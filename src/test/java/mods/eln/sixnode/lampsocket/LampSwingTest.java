package mods.eln.sixnode.lampsocket;

import mods.eln.gl.Bake;
import mods.eln.gl.GlFrame;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hanging lamp indoors hangs still and is drawn by the chunk; outdoors the wind swings it in all weather and the
 * renderer draws it every frame (todo #473). Frames are 50 ms; the lamp is seen from 10 blocks (no glow plane).
 */
class LampSwingTest {
    final LinkedHashSet<Bake.Call> settled = new LinkedHashSet<>();
    int left;
    long clock = 2_000_000_000_000L;
    final Object owner = new Object();
    final BlockPos pos = new BlockPos(100, 15, -40);
    Bake.Host oldHost;
    int oldMode;
    LampSocketSuspendedObjRender render;
    final LampSwing swing = new LampSwing();
    boolean outdoors;
    float weather = 0.1f;
    int light = 0;
    static final float DT = 0.05f;

    @BeforeEach
    void host() {
        oldHost = Bake.host;
        oldMode = Bake.mode;
        Bake.clear();
        Bake.mode = 1;
        Bake.host = new Bake.Host() {
            public boolean hasSprite(Identifier texture) {
                return true;
            }

            public void settled(Bake.Call c) {
                settled.add(c);
            }

            public void unsettled(Bake.Call c) {
                assertTrue(settled.remove(c));
                left++;
            }

            public void frame() {
            }

            public String stats() {
                return "";
            }
        };
        Obj3D obj = new Obj3D();
        assertTrue(obj.loadFile("RobustLampSuspended/RobustLampSuspended.obj"));
        render = new LampSocketSuspendedObjRender(obj, true, 3);
    }

    @AfterEach
    void off() {
        Bake.sweep(o -> true);
        Bake.clear();
        Bake.mode = oldMode;
        Bake.host = oldHost;
        GlFrame.beginRenderFrame();
    }

    /** One rendered frame 50 ms later, as LampSocketRender.refresh and draw do it; the vertices the renderer still sends. */
    int frame() {
        if (outdoors) swing.wind(weather, DT);
        swing.step(DT, outdoors);
        clock += 50_000_000L;
        GlFrame.beginRenderFrame(clock);
        GlFrame f = GlFrame.openBlock(new Matrix4f(), new Matrix3f());
        try {
            f.bake(owner, pos.asLong());
            render.draw(LRDU.Up, 0, (byte) light, swing.py, swing.pz, 10);
        } finally {
            GlFrame.close();
        }
        for (Bake.Call c : settled) c.shown = true;   // (the chunk shows a settled part from the next frame on)
        int n = 0;
        for (GlFrame.Batch b : f.batches().values()) n += b.vertices();
        return n;
    }

    /** Frames until the renderer sends nothing; -1 if it still does after max frames. */
    int framesUntilBaked(int max) {
        for (int i = 1; i <= max; i++) if (frame() == 0) return i;
        return -1;
    }

    @Test
    void indoorsALampAtRestLeavesTheRenderer() {
        outdoors = false;
        int all = frame();
        assertTrue(all > 0);
        int n = framesUntilBaked(40);
        assertTrue(n > 0 && n <= 20, "a lamp at rest settles within a second: " + n);
        for (int i = 0; i < 100; i++) assertEquals(0, frame(), "nothing left with the renderer");
        assertTrue(swing.atRest());
        System.out.println("[lamp rest] hanging lamp, 3 links, unlit: " + all + " vertices a frame until settled, 0 after " + n + " frames of 50 ms; " + settled.size() + " parts in the chunk");

        // lit (the head at full light): the same
        light = 15;
        assertTrue(frame() > 0, "switched on, the head is the renderer's until the chunk shows the lit one");
        n = framesUntilBaked(60);
        assertTrue(n > 0, "the lit lamp settles too");
        for (int i = 0; i < 20; i++) assertEquals(0, frame());
        System.out.println("[lamp rest] lit: 0 vertices a frame after " + n + " more frames");
    }

    @Test
    void outdoorsTheLampSwingsInAllWeatherAndStaysWithTheRenderer() {
        outdoors = true;
        for (float w : new float[]{0.1f, 0.55f, 1f}) {   // clear, rain, thunder
            weather = w;
            int moving = 0, least = Integer.MAX_VALUE, most = 0;
            float widest = 0;
            for (int i = 0; i < 1200; i++) {   // one minute
                float py = swing.py, pz = swing.pz;
                int v = frame();
                least = Math.min(least, v);
                most = Math.max(most, v);
                if (py != swing.py || pz != swing.pz) moving++;
                widest = Math.max(widest, Math.max(Math.abs(swing.py), Math.abs(swing.pz)));
            }
            assertEquals(1200, moving, "it moves in every frame, weather " + w);
            assertFalse(swing.atRest());
            assertTrue(least > 0, "chain and head are never baked outdoors, weather " + w);
            System.out.println("[lamp rest] outdoors, weather " + w + ": moved in " + moving + " of 1200 frames, widest swing " + widest + " degrees, renderer " + least + " to " + most + " vertices a frame");
        }
    }

    @Test
    void aBumpIndoorsSwingsDiesOutAndBakesAgain() {
        outdoors = false;
        framesUntilBaked(40);
        assertEquals(0, frame());
        // a player walks under it for a second (0.2 blocks a tick)
        left = 0;
        for (int i = 0; i < 20; i++) {
            swing.push(0.2, 0.1, 4f, DT);
            assertTrue(frame() > 0, "a pushed lamp is the renderer's again");
        }
        assertTrue(left > 0, "the chunk was told to drop the resting chain and head");
        float widest = 0;
        int n = 0;
        while (!swing.atRest() && n < 20 * 600) {
            assertTrue(frame() > 0, "swinging: with the renderer");
            widest = Math.max(widest, Math.max(Math.abs(swing.py), Math.abs(swing.pz)));
            n++;
        }
        assertTrue(swing.atRest(), "the swing dies out");
        int b = framesUntilBaked(200);
        assertTrue(b > 0, "and the lamp bakes again");
        for (int i = 0; i < 20; i++) assertEquals(0, frame());
        // somebody standing under it does not wake it
        for (int i = 0; i < 20; i++) {
            swing.push(0, 0, 4f, DT);
            assertEquals(0, frame());
        }
        System.out.println("[lamp rest] bump indoors: widest swing " + widest + " degrees, at rest after " + n * DT + " s, baked again " + b * DT + " s later");
    }

    /** The rest rule is about the swing's amplitude, so it is the same swing at 20, 60 and 300 frames a second. */
    @Test
    void theSwingEndsAtTheSameSizeAtAnyFrameRate() {
        for (int fps : new int[]{20, 60, 300}) {
            LampSwing s = new LampSwing();
            float dt = 1f / fps;
            for (int i = 0; i < fps; i++) s.push(0.2, 0.1, 4f, dt);
            float peak = 0, lastPeak = 0;
            int n = 0;
            while (!s.atRest() && n < fps * 600) {
                float before = s.angle();
                s.step(dt, false);
                // (the widest angle of the last second before rest)
                peak = Math.max(peak, before);
                if (++n % fps == 0) {
                    lastPeak = peak;
                    peak = 0;
                }
            }
            assertTrue(s.atRest(), "at rest at " + fps + " fps");
            assertTrue(Math.max(peak, lastPeak) < 2 * LampSwing.REST_ANGLE, "no visible jump to rest at " + fps + " fps: " + Math.max(peak, lastPeak));
            System.out.println("[lamp rest] " + fps + " fps: at rest after " + n * dt + " s, swing in the last second " + Math.max(peak, lastPeak) + " degrees");
        }
    }

    @Test
    void aRoofStopsTheSwingAndRemovingItStartsItAgain() {
        outdoors = true;
        weather = 1f;
        for (int i = 0; i < 600; i++) assertTrue(frame() > 0);
        // roof built: LampSocketRender sees the sky light fall at its next look (1 to 1.25 s), then no more wind
        outdoors = false;
        int n = 0;
        while (!swing.atRest() && n < 20 * 600) {
            frame();
            n++;
        }
        assertTrue(swing.atRest(), "under a roof the swing dies out");
        int b = framesUntilBaked(200);
        assertTrue(b > 0);
        for (int i = 0; i < 20; i++) assertEquals(0, frame());
        System.out.println("[lamp rest] roof built in a thunderstorm: at rest after " + n * DT + " s, baked " + b * DT + " s later");
        // roof removed
        outdoors = true;
        weather = 0.1f;
        left = 0;
        assertTrue(frame() > 0, "the wind takes it back to the renderer in the first frame");
        for (int i = 0; i < 600; i++) assertTrue(frame() > 0);
        assertTrue(left > 0);
    }
}
