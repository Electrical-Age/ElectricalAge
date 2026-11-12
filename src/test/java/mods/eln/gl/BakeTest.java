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
    final List<Long> hidden = new ArrayList<>();
    boolean sprites = true;
    long t = 1_000_000_000L;
    final Object owner = new Object();
    final long pos = BlockPos.asLong(100, 15, -40);
    int light = 0xF000F0;

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
            }

            public void unsettled(Bake.Call c) {
                assertTrue(settled.remove(c));
            }

            public boolean visible(long section) {
                return !hidden.contains(section);
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
        Bake.frame(t);
        GlFrame f = GlFrame.openBlock(new Matrix4f().translation(3, -2, 7), null);
        f.bake(owner, pos, light);
        try {
            draw.run();
        } finally {
            GlFrame.close();
        }
        int n = 0;
        for (GlFrame.Batch b : f.batches().values()) n += b.vertices();
        return n;
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
        assertArrayEquals(new long[]{SectionPos.asLong(100 >> 4, 15 >> 4, -40 >> 4)}, c.sections);
        assertFalse(c.foreign);
        c.activeIn = 1;
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
        settled.get(0).activeIn = 1;
        assertEquals(0, frame(16, draw));
        angle[0] = 10;
        assertEquals(4, frame(16, draw));
        assertTrue(settled.isEmpty(), "the old pose left the chunk");
        // the light is part of what a call is (the wait is 0.8 s now: the part moved soon after settling)
        frame(900, draw);
        settled.get(0).activeIn = 1;
        assertEquals(0, frame(16, draw));
        light = 0xE000F0;
        assertEquals(4, frame(16, draw));
        assertTrue(settled.isEmpty());
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
        for (Bake.Call c : settled) c.activeIn = 1;
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

    @Test
    void quadsGoToTheSectionOfTheirCentreAndNeedItVisible() {
        // the block is at y 15: a quad raised by 1.5 lies in the section above
        int low = quadList(0.5f, 1), high = quadList(1.5f, 1);
        Runnable draw = () -> {
            GL11.glCallList(low);
            GL11.glCallList(high);
        };
        frame(0, draw);
        frame(500, draw);
        assertEquals(2, settled.size());
        long home = SectionPos.asLong(100 >> 4, 0, -40 >> 4), above = SectionPos.asLong(100 >> 4, 1, -40 >> 4);
        assertArrayEquals(new long[]{home}, settled.get(0).sections);
        assertArrayEquals(new long[]{above}, settled.get(1).sections);
        assertTrue(settled.get(1).foreign);
        for (Bake.Call c : settled) c.activeIn = 1;
        assertEquals(0, frame(16, draw));
        hidden.add(above);
        assertEquals(4, frame(16, draw), "the section above is not drawn: the renderer draws that quad");
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
        var sprites = java.util.Map.of(white, new mods.eln.client.render.bake.ModelSprites.Spr(0.25f, 0.5f, 0.125f, 0.0625f, false));
        List<float[]> out = new ArrayList<>();
        int n = mods.eln.client.render.bake.ChunkBake.write((cutout, x, y, z, color, u, v, l) -> {
            assertFalse(cutout);
            assertEquals(light, l);
            out.add(new float[]{x, y, z, u, v, color});
        }, settled.toArray(new Bake.Call[0]), sprites, settled.get(0).sections[0], 96, 0, -48, false);
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

    /** With a pose that is more than a translation the matrices hold the camera: nothing of such a frame is baked. */
    @Test
    void aFrameWithARotatedPoseIsNotBaked() {
        int list = quadList(0.5f, 1);
        for (int k = 0; k < 3; k++) {
            t += 500 * MS;
            Bake.frame(t);
            GlFrame f = GlFrame.openBlock(new Matrix4f().translation(3, -2, 7).rotateY(0.3f), null);
            f.bake(owner, pos, light);
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
