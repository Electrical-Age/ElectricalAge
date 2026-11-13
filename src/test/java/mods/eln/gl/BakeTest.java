package mods.eln.gl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which display-list geometry a block entity's frame leaves to the chunk mesh ({@link Bake}), with a host that records. */
class BakeTest {
    static final long MS = 1_000_000L;
    final List<Bake.Call> settled = new ArrayList<>();
    /** What the host was told: + a call joined its section, - one left, each with the number of the rendered frame. */
    final List<String> log = new ArrayList<>();
    int frames;
    boolean sprites = true;
    long t = 1_000_000_000L;
    final Object owner = new Object();
    final long pos = BlockPos.asLong(100, 15, -40);

    @BeforeEach
    void host() {
        Bake.clear();
        Bake.mode = 1;
        Bake.host = new Bake.Host() {
            public boolean hasSprite(Identifier texture) {
                return sprites;
            }

            public void settled(Bake.Call c) {
                settled.add(c);
                log.add("+" + frames);
            }

            public void unsettled(Bake.Call c) {
                assertTrue(settled.remove(c));
                log.add("-" + frames);
            }

            public void frame() {
            }

            public String stats() {
                return "";
            }
        };
    }

    @AfterEach
    void off() {
        Bake.mode = 0;
        Bake.host = null;
        Bake.clear();
    }

    /** A list with one unit quad at height y (list space), texture coordinates up to uMax. */
    static int quadList(float y, float uMax) {
        int list = GL11.glGenLists(1);
        GlFrame.open();
        GL11.glNewList(list, GL11.GL_COMPILE);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glNormal3f(0, 1, 0);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex3f(0, y, 0);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex3f(0, y, 1);
        GL11.glTexCoord2f(uMax, 1);
        GL11.glVertex3f(1, y, 1);
        GL11.glTexCoord2f(uMax, 0);
        GL11.glVertex3f(1, y, 0);
        GL11.glEnd();
        GL11.glEndList();
        GlFrame.close();
        return list;
    }

    /** One rendered frame dt milliseconds later: the device's draw; returns the vertices the renderer still draws. */
    int frame(int dtMs, Runnable draw) {
        t += dtMs * MS;
        frames++;
        Bake.frame(t);
        GlFrame f = GlFrame.openBlock(new Matrix4f().translation(3, -2, 7), null);
        f.bake(owner, pos);
        try {
            draw.run();
        } finally {
            GlFrame.close();
        }
        int n = 0;
        for (GlFrame.Batch b : f.batches().values()) n += b.vertices();
        return n;
    }

    /**
     * One rendered frame dt milliseconds later with the device in view beyond its renderer's range: it is drawn into
     * a far frame if it is due one. Returns whether it was.
     */
    boolean far(int dtMs, Runnable draw) {
        t += dtMs * MS;
        frames++;
        Bake.frame(t);
        return farFrame(owner, pos, draw);
    }

    boolean farFrame(Object owner, long pos, Runnable draw) {
        if (!Bake.farDue(owner)) return false;
        GlFrame f = GlFrame.openBlock(new Matrix4f().translation(3, -2, 7), null);
        f.bake(owner, pos, true);
        try {
            draw.run();
        } finally {
            GlFrame.close();
        }
        for (GlFrame.Batch b : f.batches().values()) assertEquals(0, b.vertices(), "a far frame draws nothing");
        return true;
    }

    /** A rendered frame dt milliseconds later in which the device is out of view. */
    void unseen(int dtMs) {
        t += dtMs * MS;
        frames++;
        Bake.frame(t);
    }

    /** The list drawn turned by angle[0] degrees. */
    static Runnable turned(int list, float[] angle) {
        return () -> {
            GL11.glPushMatrix();
            GL11.glRotatef(angle[0], 0, 1, 0);
            GL11.glCallList(list);
            GL11.glPopMatrix();
        };
    }

    /** The device comes due between delayMs and a quarter more after its last run; its next sample is drawn. */
    void dueAfter(int delayMs, Runnable draw) {
        assertFalse(far(delayMs - 500, draw));
        assertTrue(far(500 + delayMs / 4, draw));
    }

    @Test
    void aDeviceNeverDrawnNearSettlesInTwoFarSamples() {
        int list = quadList(0.5f, 1);
        Runnable draw = () -> GL11.glCallList(list);
        assertTrue(far(0, draw));
        assertTrue(settled.isEmpty());
        assertFalse(far(16, draw), "no sample before 0.4 s have passed");
        assertFalse(far(300, draw));
        assertTrue(far(100, draw));
        assertEquals(1, settled.size());
        Bake.Device d = settled.get(0).device;
        assertFalse(d.inRun());
        assertFalse(far(16, draw));
        // near: the renderer draws it until the mesh shows it
        assertEquals(4, frame(16, draw));
        settled.get(0).shown = true;
        assertEquals(0, frame(16, draw));
        assertEquals(List.of("+4"), log);
    }

    /** What changes without a packet is found too: every run is followed by another, later each time nothing came of it. */
    @Test
    void aStillDeviceIsLookedAtAgainLessAndLessOften() {
        int list = quadList(0.5f, 1);
        float[] angle = {0};
        Runnable draw = turned(list, angle);
        far(0, draw);
        far(400, draw);
        Bake.Call old = settled.get(0);
        for (int delay = 10_000; delay <= 160_000; delay *= 2) {
            dueAfter(delay, draw);
            assertTrue(far(400, draw));
            assertFalse(old.device.inRun());
        }
        dueAfter(160_000, draw);
        assertTrue(far(400, draw));
        assertEquals(List.of("+2"), log);
        angle[0] = 10;
        dueAfter(160_000, draw);
        assertEquals(List.of(old), settled);
        assertTrue(far(400, draw));
        assertEquals(1, settled.size());
        assertFalse(settled.get(0) == old);
        // something came of that run
        dueAfter(10_000, draw);
    }

    @Test
    void aPartThatChangesFarAwayStaysUntilItsReplacementSettles() {
        int list = quadList(0.5f, 1);
        float[] angle = {0};
        Runnable draw = turned(list, angle);
        far(0, draw);
        far(400, draw);
        Bake.Call old = settled.get(0);
        Bake.Device d = old.device;
        assertFalse(far(7_000, draw));
        angle[0] = 10;
        assertFalse(far(1000, draw), "nothing said it changed");
        Bake.published(owner);
        log.clear();
        assertTrue(far(16, draw));
        assertEquals(List.of(old), settled, "the old pose stays in the chunk");
        assertEquals(1, d.held());
        assertFalse(far(16, draw));
        assertTrue(far(400, draw));
        assertEquals(1, settled.size());
        assertFalse(settled.get(0) == old);
        assertEquals(0, d.held());
        assertFalse(d.inRun());
        assertEquals(List.of("-" + frames, "+" + frames), log, "old out and new in within one frame: one re-mesh");
        assertFalse(far(1000, draw));
    }

    /** A packet whose effect shows a few frames later (an interpolator, a client tick) is still seen by the run it starts. */
    @Test
    void aChangeThatShowsAfterTheRunsFirstSampleIsFollowed() {
        int list = quadList(0.5f, 1), lamp = quadList(0.75f, 1);
        boolean[] lit = {false};
        Runnable draw = () -> {
            GL11.glCallList(list);
            if (lit[0]) GL11.glCallList(lamp);
        };
        far(0, draw);
        far(400, draw);
        Bake.Device d = settled.get(0).device;
        Bake.published(owner);
        assertTrue(far(1000, draw));
        lit[0] = true;
        assertTrue(far(400, draw));
        assertTrue(d.inRun(), "one change is not yet a part in motion");
        assertTrue(far(400, draw));
        assertFalse(d.inRun());
        assertEquals(2, settled.size());
    }

    @Test
    void aPartFlippedBackWithinTheRunIsTakenBack() {
        int list = quadList(0.5f, 1);
        float[] angle = {0};
        Runnable draw = turned(list, angle);
        far(0, draw);
        far(400, draw);
        Bake.Call old = settled.get(0);
        Bake.Device d = old.device;
        far(7_000, draw);
        log.clear();
        angle[0] = 10;
        Bake.published(owner);
        assertTrue(far(16, draw));
        assertEquals(1, d.held());
        angle[0] = 0;
        assertTrue(far(400, draw));
        assertEquals(0, d.held());
        while (d.inRun()) assertTrue(far(400, draw));
        assertEquals(List.of(old), settled);
        assertTrue(log.isEmpty(), "the section was not touched");
    }

    @Test
    void aPartThatKeepsMovingIsGivenUpAndLookedAtAgainLater() {
        int body = quadList(0.25f, 1), rotor = quadList(0.5f, 1);
        float[] angle = {0};
        boolean[] turning = {true};
        Runnable draw = () -> {
            GL11.glCallList(body);
            if (turning[0]) angle[0] += 7;
            turned(rotor, angle).run();
        };
        assertTrue(far(0, draw));
        assertTrue(far(400, draw));
        assertEquals(1, settled.size(), "the still part does not wait for the moving one");
        Bake.Device d = settled.get(0).device;
        assertFalse(d.inRun(), "two samples differed: it moves");
        for (int delay = 10_000; delay <= 160_000; delay *= 2) {
            dueAfter(delay, draw);
            assertTrue(far(400, draw), "the first sample of a run says nothing: it differs from long ago");
            assertFalse(d.inRun());
        }
        dueAfter(160_000, draw);
        turning[0] = false;
        assertTrue(far(400, draw));
        assertEquals(2, settled.size(), "the rotor stopped");
        assertFalse(d.inRun());
    }

    /** A lever is thrown and a rotor starts: the lever's old pose stays until the run is over, then the new one replaces it. */
    @Test
    void whileACallIsHeldNothingJoins() {
        int lever = quadList(0.25f, 1), rotor = quadList(0.5f, 1);
        float[] at = {0}, angle = {0};
        boolean[] turning = {false};
        Runnable draw = () -> {
            turned(lever, at).run();
            if (turning[0]) angle[0] += 7;
            turned(rotor, angle).run();
        };
        far(0, draw);
        far(400, draw);
        assertEquals(2, settled.size());
        Bake.Device d = settled.get(0).device;
        far(7_000, draw);
        log.clear();
        at[0] = 30;
        turning[0] = true;
        Bake.published(owner);
        assertTrue(far(16, draw));
        assertEquals(2, d.held());
        for (int k = 0; k < 7; k++) {
            assertTrue(far(400, draw));
            assertEquals(2, settled.size());
            assertTrue(log.isEmpty());
        }
        assertTrue(far(400, draw), "3 s into the run");
        assertFalse(d.inRun());
        assertEquals(List.of("-" + frames, "-" + frames, "+" + frames), log);
        assertEquals(1, settled.size());
        assertEquals(0, d.held());
        // the next packet is looked at at once: the last run changed the chunk
        Bake.published(owner);
        assertTrue(far(16, draw));
    }

    /** Devices that report a voltage every second or two must not be drawn all the time for it. */
    @Test
    void packetsThatChangeNothingStartARunEveryTenSecondsAtMost() {
        int list = quadList(0.5f, 1);
        Runnable draw = () -> GL11.glCallList(list);
        far(0, draw);
        far(400, draw);
        log.clear();
        int drawn = 0;
        for (int k = 0; k < 300; k++) {
            Bake.published(owner);
            if (far(200, draw)) drawn++;
        }
        assertTrue(drawn >= 8 && drawn <= 12, drawn + " far frames in a minute");
        assertTrue(log.isEmpty());
        assertEquals(1, settled.size());
    }

    @Test
    void packetsOfAMovingDeviceStartARunEveryTenSecondsAtMost() {
        int body = quadList(0.25f, 1), rotor = quadList(0.5f, 1);
        float[] angle = {0};
        Runnable draw = () -> {
            GL11.glCallList(body);
            angle[0] += 7;
            turned(rotor, angle).run();
        };
        far(0, draw);
        far(400, draw);
        log.clear();
        int drawn = 0;
        for (int k = 0; k < 300; k++) {
            Bake.published(owner);
            if (far(200, draw)) drawn++;
        }
        assertTrue(drawn >= 8 && drawn <= 12, drawn + " far frames in a minute");
        assertTrue(log.isEmpty());
    }

    @Test
    void aNearFrameEndsTheRunAndDropsWhatWasHeld() {
        int list = quadList(0.5f, 1);
        float[] angle = {0};
        Runnable draw = turned(list, angle);
        far(0, draw);
        far(400, draw);
        Bake.Device d = settled.get(0).device;
        settled.get(0).shown = true;
        far(7_000, draw);
        angle[0] = 10;
        Bake.published(owner);
        assertTrue(far(16, draw));
        assertEquals(1, d.held());
        assertEquals(4, frame(16, draw), "the renderer draws the device as it is");
        assertTrue(settled.isEmpty());
        assertEquals(0, d.held());
        assertFalse(d.inRun());
        // a sample is less proof than a frame: the wait starts again with the first near frame
        assertEquals(4, frame(390, draw));
        assertTrue(settled.isEmpty());
        frame(20, draw);
        assertEquals(1, settled.size());
    }

    @Test
    void walkingAwayStartsARunThatFindsNothingNew() {
        int list = quadList(0.5f, 1);
        Runnable draw = () -> GL11.glCallList(list);
        frame(0, draw);
        frame(500, draw);
        Bake.Call c = settled.get(0);
        c.shown = true;
        assertEquals(0, frame(16, draw));
        log.clear();
        assertTrue(far(16, draw));
        assertTrue(c.device.inRun());
        assertFalse(far(16, draw));
        assertTrue(far(400, draw));
        assertFalse(c.device.inRun());
        assertTrue(log.isEmpty());
        assertEquals(0, frame(16, draw));
        assertEquals(List.of(c), settled);
    }

    /** Near, a call that waits is watched in every frame; the samples after it carry the wait on. */
    @Test
    void aCallStillWaitingWhenWalkingAwaySettlesFar() {
        int list = quadList(0.5f, 1);
        Runnable draw = () -> GL11.glCallList(list);
        frame(0, draw);
        frame(100, draw);
        assertTrue(far(16, draw));
        assertTrue(settled.isEmpty());
        assertTrue(far(400, draw));
        assertEquals(1, settled.size());
    }

    /** A far device is only asked about while it is in view. Out of view its run waits; the next sample decides it. */
    @Test
    void aRunInterruptedByLookingAwayEndsWithItsNextSample() {
        int list = quadList(0.5f, 1);
        float[] angle = {0};
        Runnable draw = turned(list, angle);
        far(0, draw);
        far(400, draw);
        Bake.Call old = settled.get(0);
        far(7_000, draw);
        angle[0] = 10;
        Bake.published(owner);
        assertTrue(far(16, draw));
        assertEquals(1, old.device.held());
        for (int k = 0; k < 20; k++) unseen(250);
        log.clear();
        assertTrue(far(16, draw));
        assertEquals(List.of("-" + frames, "+" + frames), log);
        assertEquals(0, old.device.held());
        assertFalse(old.device.inRun());
    }

    @Test
    void aPartThatChangesSoonAfterSettlingFarWaitsLonger() {
        int list = quadList(0.5f, 1);
        float[] angle = {0};
        Runnable draw = turned(list, angle);
        far(0, draw);
        far(400, draw);
        Bake.Call old = settled.get(0);
        angle[0] = 10;
        Bake.published(owner);
        assertTrue(far(100, draw));
        assertTrue(far(400, draw));
        assertEquals(List.of(old), settled, "0.8 s now");
        assertTrue(far(400, draw));
        assertEquals(1, settled.size());
        assertFalse(settled.get(0) == old);
    }

    @Test
    void farFramesAreCappedInARenderedFrame() {
        int list = quadList(0.5f, 1);
        Runnable draw = () -> GL11.glCallList(list);
        Object[] owners = new Object[100];
        for (int i = 0; i < owners.length; i++) owners[i] = new Object();
        int[] want = {Bake.FAR_PER_FRAME, owners.length - Bake.FAR_PER_FRAME, 0};
        for (int round = 0; round < 2; round++) {
            for (int k = 0; k < 3; k++) {
                t += (k == 0 ? 400 : 16) * MS;
                frames++;
                Bake.frame(t);
                int drawn = 0;
                for (int i = 0; i < owners.length; i++) if (farFrame(owners[i], BlockPos.asLong(i, 15, -40), draw)) drawn++;
                assertEquals(want[k], drawn);
            }
        }
        assertEquals(owners.length, settled.size());
    }

    /** The game may ask twice in a frame (another pass), or not draw what it asked about. */
    @Test
    void aDeviceGetsOneFarFrameInARenderedFrameAndStaysDueIfItIsNotDrawn() {
        int list = quadList(0.5f, 1);
        Runnable draw = () -> GL11.glCallList(list);
        frame(0, draw);
        frame(500, draw);
        unseen(16);
        assertTrue(Bake.farDue(owner));
        assertFalse(Bake.farDue(owner), "asked twice before it was drawn");
        assertTrue(far(16, draw), "not drawn in that frame: still due");
        assertTrue(far(400, draw));
        assertFalse(settled.get(0).device.inRun());
    }

    @Test
    void forgettingADeviceTakesItsHeldCallsOutOfTheChunk() {
        int a = quadList(0.25f, 1), b = quadList(0.5f, 1);
        float[] angle = {0};
        Runnable draw = () -> {
            GL11.glCallList(a);
            turned(b, angle).run();
        };
        Bake.published(owner);   // a packet for a device never drawn: nothing to do
        far(0, draw);
        far(400, draw);
        assertEquals(2, settled.size());
        far(7_000, draw);
        angle[0] = 10;
        Bake.published(owner);
        assertTrue(far(16, draw));
        assertEquals(1, settled.get(0).device.held());
        Bake.sweep(o -> o == owner);
        assertTrue(settled.isEmpty());
        Bake.published(owner);
        assertTrue(far(16, draw), "a new device");
        assertTrue(far(400, draw));
        assertEquals(2, settled.size());
    }

    @Test
    void aCallSettlesAfterHoldingStillAndIsSkippedOnceShown() {
        int list = quadList(0.5f, 1);
        Runnable draw = () -> GL11.glCallList(list);
        assertEquals(4, frame(0, draw));
        assertEquals(4, frame(300, draw));
        assertTrue(settled.isEmpty());
        assertEquals(4, frame(150, draw), "settled, but the mesh does not show it yet");
        assertEquals(1, settled.size());
        Bake.Call c = settled.get(0);
        assertEquals(SectionPos.asLong(100 >> 4, 15 >> 4, -40 >> 4), c.device.home);
        c.shown = true;
        assertEquals(0, frame(16, draw));
        assertEquals(0, frame(16, draw));
        assertEquals(1, settled.size());
    }

    @Test
    void aChangeBringsTheRendererBackAtOnce() {
        int list = quadList(0.5f, 1);
        float[] angle = {0};
        Runnable draw = () -> {
            GL11.glPushMatrix();
            GL11.glRotatef(angle[0], 0, 1, 0);
            GL11.glCallList(list);
            GL11.glPopMatrix();
        };
        frame(0, draw);
        frame(500, draw);
        settled.get(0).shown = true;
        assertEquals(0, frame(16, draw));
        angle[0] = 10;
        assertEquals(4, frame(16, draw));
        assertTrue(settled.isEmpty(), "the old pose left the chunk");
    }

    @Test
    void aPartThatKeepsChangingWaitsLonger() {
        int list = quadList(0.5f, 1);
        float[] angle = {0};
        Runnable draw = () -> {
            GL11.glPushMatrix();
            GL11.glRotatef(angle[0], 0, 1, 0);
            GL11.glCallList(list);
            GL11.glPopMatrix();
        };
        frame(0, draw);
        frame(450, draw);
        assertEquals(1, settled.size());
        angle[0] = 10;   // moved soon after settling: 0.8 s now
        frame(100, draw);
        assertTrue(settled.isEmpty());
        frame(450, draw);
        assertTrue(settled.isEmpty());
        frame(400, draw);
        assertEquals(1, settled.size());
        angle[0] = 20;   // and again: 1.6 s
        frame(100, draw);
        frame(900, draw);
        assertTrue(settled.isEmpty());
        frame(800, draw);
        assertEquals(1, settled.size());
        // held for long (16 times its wait), then moved: back to 0.4 s
        angle[0] = 30;
        frame(30_000, draw);
        assertTrue(settled.isEmpty());
        frame(450, draw);
        assertEquals(1, settled.size());
    }

    @Test
    void aPartNoLongerDrawnLeavesTheChunk() {
        int a = quadList(0.5f, 1), b = quadList(0.25f, 1);
        boolean[] both = {true};
        Runnable draw = () -> {
            GL11.glCallList(a);
            if (both[0]) GL11.glCallList(b);
        };
        frame(0, draw);
        frame(500, draw);
        assertEquals(2, settled.size());
        for (Bake.Call c : settled) c.shown = true;
        assertEquals(0, frame(16, draw));
        both[0] = false;
        assertEquals(0, frame(16, draw));
        assertEquals(1, settled.size());
        // two equal calls are two calls
        both[0] = true;
        Runnable twice = () -> {
            GL11.glCallList(a);
            GL11.glCallList(a);
        };
        frame(16, twice);
        frame(500, twice);
        assertEquals(2, settled.size());
    }

    @Test
    void whatTheChunkCannotDrawStaysWithTheRenderer() {
        int wraps = quadList(0.5f, 3), plain = quadList(0.5f, 1);
        frame(0, () -> GL11.glCallList(wraps));
        assertEquals(4, frame(500, () -> GL11.glCallList(wraps)));
        assertTrue(settled.isEmpty(), "texture coordinates beyond the texture");
        Runnable blended = () -> {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glCallList(plain);
            GL11.glDisable(GL11.GL_BLEND);
        };
        frame(16, blended);
        assertEquals(4, frame(500, blended));
        assertTrue(settled.isEmpty(), "blended");
        sprites = false;
        frame(16, () -> GL11.glCallList(plain));
        assertEquals(4, frame(500, () -> GL11.glCallList(plain)));
        assertTrue(settled.isEmpty(), "texture not in the atlas");
    }

    /** A section of nothing but air is never compiled, and the game draws a block entity by its block's section. */
    @Test
    void quadsOutsideTheBlocksSectionStillGoToIt() {
        // the block is at y 15: a quad raised by 1.5 lies in the section above
        int low = quadList(0.5f, 1), high = quadList(1.5f, 1);
        Runnable draw = () -> {
            GL11.glCallList(low);
            GL11.glCallList(high);
        };
        frame(0, draw);
        frame(500, draw);
        assertEquals(2, settled.size());
        long home = SectionPos.asLong(100 >> 4, 0, -40 >> 4);
        for (Bake.Call c : settled) {
            assertEquals(home, c.device.home);
            c.shown = true;
        }
        assertEquals(0, frame(16, draw));
        var sprites = java.util.Map.of(mods.eln.misc.UtilsClient.WHITE, new BakeMesh.Spr(0, 0, 1, 1, false));
        List<Float> ys = new ArrayList<>();
        assertEquals(8, BakeMesh.write((cutout, x, y, z, color, u, v, l) -> ys.add(y),
            Bake.inDrawOrder(settled), new int[2], sprites, 96, 0, -48, false));
        assertEquals(15.5f, ys.get(0), 1e-5f);
        assertEquals(16.5f, ys.get(4), 1e-5f, "above the section's 16 blocks");
    }

    /** The chunk mesh gets a settled call's quads where the renderer drew them, shaded as the entity shader shades. */
    @Test
    void theChunkGetsTheQuadsTheRendererDrew() {
        int list = quadList(0.5f, 1);
        Runnable draw = () -> {
            GL11.glPushMatrix();
            GL11.glTranslatef(0.5f, 0.25f, 0.5f);
            GL11.glRotatef(90, 0, 0, 1);   // the quad's up normal now points to -x
            GL11.glColor3f(1, 0.5f, 0.25f);
            GL11.glCallList(list);
            GL11.glColor3f(1, 1, 1);
            GL11.glPopMatrix();
        };
        Bake.mode = 0;
        t += 16 * MS;
        Bake.frame(t);
        GlFrame f = GlFrame.openBlock(new Matrix4f(), null);
        draw.run();
        GlFrame.close();
        GlFrame.Batch b = f.batches().values().iterator().next();
        float[] drawn = java.util.Arrays.copyOf(b.data, b.floats);
        Bake.mode = 1;
        frame(16, draw);
        frame(500, draw);
        assertEquals(1, settled.size());
        Identifier white = mods.eln.misc.UtilsClient.WHITE;
        var sprites = java.util.Map.of(white, new BakeMesh.Spr(0.25f, 0.5f, 0.125f, 0.0625f, false));
        List<float[]> out = new ArrayList<>();
        int n = BakeMesh.write((cutout, x, y, z, color, u, v, l) -> {
            assertFalse(cutout);
            assertEquals(0xE000B0, l);
            out.add(new float[]{x, y, z, u, v, color});
        }, settled.toArray(new Bake.Call[0]), new int[]{0xE000B0}, sprites, 96, 0, -48, false);
        assertEquals(4, n);
        // -x: only the second light reaches it, 0.2/sqrt(1.53) of it
        float shade = (float) (0.2 / Math.sqrt(1.53) * 0.6 + 0.4);
        for (int k = 0; k < 4; k++) {
            float[] v = out.get(k);
            int i = k * GlFrame.STRIDE;
            assertEquals(drawn[i] + 4, v[0], 1e-5f);
            assertEquals(drawn[i + 1] + 15, v[1], 1e-5f);
            assertEquals(drawn[i + 2] + 8, v[2], 1e-5f);
            assertEquals(0.25f + drawn[i + 3] * 0.125f, v[3], 1e-6f);
            assertEquals(0.5f + drawn[i + 4] * 0.0625f, v[4], 1e-6f);
            assertEquals(-1, drawn[i + 6], 1e-5f);
            int c = (int) v[5];
            assertEquals(255, c >>> 24);
            assertEquals(255 * shade, (c >> 16) & 255, 0.51f);
            assertEquals(128 * shade, (c >> 8) & 255, 1.01f);
            assertEquals(64 * shade, c & 255, 1.01f);
        }
    }

    /**
     * A part drawn twice in the same place, shaded and then at full light: the chunk writes the two in the order of
     * the draw, so the second covers the first, also when the second settled first.
     */
    @Test
    void aPartDrawnTwiceInPlaceKeepsItsOrder() {
        int list = quadList(0.5f, 1);
        Runnable bright = () -> {
            mods.eln.misc.UtilsClient.disableLight();
            GL11.glCallList(list);
            mods.eln.misc.UtilsClient.enableLight();
        };
        Runnable both = () -> {
            GL11.glCallList(list);
            bright.run();
        };
        frame(0, bright);
        frame(500, bright);
        assertEquals(1, settled.size());
        frame(100, both);
        frame(500, both);
        assertEquals(2, settled.size());
        assertTrue(settled.get(0).fullBright, "the full-light draw settled first");
        assertFalse(settled.get(1).fullBright);
        Bake.Call[] calls = Bake.inDrawOrder(settled);
        assertFalse(calls[0].fullBright);
        assertTrue(calls[1].fullBright);
        var sprites = java.util.Map.of(mods.eln.misc.UtilsClient.WHITE, new BakeMesh.Spr(0, 0, 1, 1, false));
        List<Integer> lights = new ArrayList<>();
        BakeMesh.write((cutout, x, y, z, color, u, v, l) -> lights.add(l),
            calls, new int[]{0xE000B0, 0xE000B0}, sprites, 96, 0, -48, false);
        assertEquals(8, lights.size());
        assertEquals(0xE000B0, lights.get(0));
        assertEquals(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT, lights.get(7));
    }

    /** Calls of several devices stay together, each device's in the order of its draw. */
    @Test
    void theChunkWritesEachDevicesCallsInDrawOrder() {
        int a = quadList(0.25f, 1), b = quadList(0.5f, 1), c = quadList(0.75f, 1);
        Runnable draw = () -> {
            GL11.glCallList(a);
            GL11.glCallList(b);
            GL11.glCallList(c);
        };
        frame(0, draw);
        frame(500, draw);
        assertEquals(3, settled.size());
        List<Bake.Call> shuffled = List.of(settled.get(2), settled.get(0), settled.get(1));
        assertArrayEquals(settled.toArray(), Bake.inDrawOrder(shuffled));
    }

    @Test
    void theOverrideReadsOffOnAndForce() {
        assertEquals(-1, Bake.override(null));
        assertEquals(0, Bake.override("off"));
        assertEquals(1, Bake.override("on"));
        assertEquals(1, Bake.override("1"));
        assertEquals(2, Bake.override("force"));
        assertEquals(-1, Bake.override("maybe"));
    }

    /** With a pose that is more than a translation the matrices hold the camera: nothing of such a frame is baked. */
    @Test
    void aFrameWithARotatedPoseIsNotBaked() {
        int list = quadList(0.5f, 1);
        for (int k = 0; k < 3; k++) {
            t += 500 * MS;
            Bake.frame(t);
            GlFrame f = GlFrame.openBlock(new Matrix4f().translation(3, -2, 7).rotateY(0.3f), null);
            f.bake(owner, pos);
            GL11.glCallList(list);
            GlFrame.close();
            assertEquals(4, f.batches().values().iterator().next().vertices());
        }
        assertTrue(settled.isEmpty());
    }

    @Test
    void aBlockFrameGivesTheSameVerticesAsThePoseInTheMatrix() {
        Bake.mode = 0;
        int list = quadList(0.5f, 1);
        Runnable draw = () -> {
            GL11.glTranslatef(.5f, .5f, .5f);
            GL11.glRotatef(33, 0.2f, 1, 0.1f);
            GL11.glCallList(list);
            GL11.glLoadIdentity();
            GL11.glCallList(list);
        };
        Matrix4f pose = new Matrix4f().translation(12.25f, -3.5f, 40.125f);
        GlFrame f = GlFrame.open(pose, null);
        draw.run();
        GlFrame.close();
        GlFrame.Batch b = f.batches().values().iterator().next();
        float[] want = java.util.Arrays.copyOf(b.data, b.floats);
        f = GlFrame.openBlock(pose, null);
        draw.run();
        GlFrame.close();
        b = f.batches().values().iterator().next();
        assertArrayEquals(want, java.util.Arrays.copyOf(b.data, b.floats), 1e-5f);
    }
}
