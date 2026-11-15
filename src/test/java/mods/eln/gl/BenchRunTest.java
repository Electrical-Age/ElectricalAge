package mods.eln.gl;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchRunTest {
    @AfterEach
    void off() {
        RenderProbe.on = false;
        RenderProbe.take(new long[RenderProbe.N]);
    }

    @Test
    void viewsParse() {
        List<BenchRun.View> v = BenchRun.parseViews("workshop=727.5,146,68.5,-40.5,18; here,30 ;10,64,-3,90,0,12.5");
        assertEquals(3, v.size());
        assertEquals(new BenchRun.View("workshop", false, 727.5, 146, 68.5, -40.5f, 18f, 0f), v.get(0));
        assertEquals(new BenchRun.View("view2", true, 0, 0, 0, 0f, 0f, 30f), v.get(1));
        assertEquals(new BenchRun.View("view3", false, 10, 64, -3, 90f, 0f, 12.5f), v.get(2));
        assertEquals("tp @s 727.500 146.000 68.500 -40.50 18.00", BenchRun.teleport(v.get(0)));
    }

    @Test
    void noViewsMeansHere() {
        for (String s : new String[]{null, "", " ; "}) {
            List<BenchRun.View> v = BenchRun.parseViews(s);
            assertEquals(1, v.size());
            assertTrue(v.get(0).here());
            assertEquals(0f, v.get(0).spin());
        }
    }

    @Test
    void badViewsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> BenchRun.parseViews("1,2,3"));
        assertThrows(IllegalArgumentException.class, () -> BenchRun.parseViews("a=1,2,3,x,5"));
        assertThrows(IllegalArgumentException.class, () -> BenchRun.parseViews("here,1,2"));
    }

    @Test
    void framesAddUp() {
        BenchRun run = new BenchRun();
        long[] c = new long[RenderProbe.N];
        // 10000 frames (the arrays grow) of 4 ms, EA 1 ms of each: 0.6 record, 0.3 write, 0.1 refresh
        for (int i = 0; i < 10000; i++) {
            java.util.Arrays.fill(c, 0);
            c[RenderProbe.RECORD_NS] = 600_000;
            c[RenderProbe.WRITE_NS] = 300_000;
            c[RenderProbe.REFRESH_NS] = 100_000;
            c[RenderProbe.WRITE_VERTICES] = 20_000;
            c[RenderProbe.DEVICES] = 70;
            c[RenderProbe.MESH_NS] = i % 100 == 0 ? 2_000_000 : 0;   // (workers: not part of the frame's share)
            c[RenderProbe.MESH_SECTIONS] = i % 100 == 0 ? 1 : 0;
            run.frame(i == 5000 ? 40_000_000 : 4_000_000, c);
        }
        JsonObject o = run.json();
        assertEquals(10000, o.get("frames").getAsInt());
        assertEquals(40.036, o.get("seconds").getAsDouble(), 1e-9);
        assertEquals(10000 / 40.036, o.get("fps").getAsDouble(), 1e-3);
        JsonObject f = o.getAsJsonObject("frameMs");
        assertEquals(4.0, f.get("p50").getAsDouble(), 1e-9);
        assertEquals(4.0, f.get("p99").getAsDouble(), 1e-9);
        assertEquals(40.0, f.get("max").getAsDouble(), 1e-9);
        assertEquals(1.0, o.getAsJsonObject("eaMs").get("p50").getAsDouble(), 1e-9);
        assertEquals(10000 * 1.0 / 40036, o.get("eaShare").getAsDouble(), 1e-4);
        JsonObject per = o.getAsJsonObject("perFrame");
        assertEquals(20000, per.get("writeVertices").getAsDouble(), 1e-9);
        assertEquals(70, per.get("devices").getAsDouble(), 1e-9);
        assertEquals(600000, per.get("recordNs").getAsDouble(), 1e-9);
        assertFalse(per.has("meshNs"));
        assertEquals(100 / 40.036, o.getAsJsonObject("perSecond").get("meshSections").getAsDouble(), 1e-3);
    }

    @Test
    void probeCountsOnlyWhileOn() throws Exception {
        assertEquals(0, RenderProbe.start(), "off: the hooks get 0 and skip their end");
        RenderProbe.on = true;
        long t = RenderProbe.start();
        assertTrue(t != 0);
        RenderProbe.device(t, false);
        RenderProbe.device(t, false);
        RenderProbe.device(t, true);
        RenderProbe.wrote(t, 400);
        RenderProbe.wrote(t, 200);
        RenderProbe.refreshed(t, 68);
        RenderProbe.count(RenderProbe.SETTLES);
        Thread worker = new Thread(() -> RenderProbe.meshed(RenderProbe.start(), 1200));
        worker.start();
        worker.join();
        long[] c = new long[RenderProbe.N];
        RenderProbe.take(c);
        assertEquals(2, c[RenderProbe.DEVICES]);
        assertEquals(1, c[RenderProbe.FAR_FRAMES]);
        assertEquals(600, c[RenderProbe.WRITE_VERTICES]);
        assertEquals(2, c[RenderProbe.BATCHES]);
        assertEquals(68, c[RenderProbe.REFRESHED]);
        assertEquals(1, c[RenderProbe.SETTLES]);
        assertEquals(1200, c[RenderProbe.MESH_VERTICES]);
        assertEquals(1, c[RenderProbe.MESH_SECTIONS]);
        assertTrue(c[RenderProbe.RECORD_NS] >= 0 && c[RenderProbe.WRITE_NS] >= 0);
        long[] again = new long[RenderProbe.N];
        RenderProbe.take(again);
        for (long v : again) assertEquals(0, v, "taking starts the counters again from zero");
        assertEquals(RenderProbe.N, RenderProbe.NAMES.length);
    }
}
