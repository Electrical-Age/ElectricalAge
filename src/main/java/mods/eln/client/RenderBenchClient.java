package mods.eln.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mods.eln.gl.Bake;
import mods.eln.gl.BenchRun;
import mods.eln.gl.RenderProbe;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

/**
 * The render benchmark's client tier (todo #424): frame times and EA's share of them at fixed views, written as JSON.
 * <p>
 * Started by launching with `-Deln.renderBench=FILE.json` (or ELN_RENDER_BENCH=FILE.json; relative to the game
 * directory), which runs it once a world is joined, or at any time by the client command `/elnbench` (then into
 * eln-render-bench.json unless the property names a file). Options, all as `-Deln.renderBench.X=`:
 * views = "name=x,y,z,yaw,pitch[,spin];..." (see {@link BenchRun#parseViews}; default: where the player stands; a view
 * with coordinates is reached with /tp, which needs the permission), settle = seconds to wait at a view before
 * measuring (40), measure = seconds measured (30), label = a name for the run, quit = true to close the game after.
 * <p>
 * Per view: every frame's length (the time between two RenderFrameEvent.Pre) and the {@link RenderProbe} counters of
 * it. Run it with the frame rate uncapped and vsync off, or the frame lengths only show the cap (EA's own times stay
 * valid); the result says which it was.
 */
public final class RenderBenchClient {
    private RenderBenchClient() {
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("eln");
    private static final String FILE = System.getProperty("eln.renderBench", System.getenv("ELN_RENDER_BENCH"));
    private static final int IDLE = 0, WAIT = 1, SETTLE = 2, MEASURE = 3;

    private static int phase = FILE != null ? WAIT : IDLE;
    private static List<BenchRun.View> views;
    private static int index;
    private static long phaseStart, last;
    private static BenchRun run;
    private static final long[] counters = new long[RenderProbe.N];
    private static JsonArray results;
    private static float yaw0;
    private static int pausedFrames;
    private static long[] stateBefore;

    private static double seconds(String name, double fallback) {
        try {
            return Double.parseDouble(System.getProperty("eln.renderBench." + name, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Run the benchmark from its first view (the client command /elnbench); a run under way starts again. */
    public static void start() {
        RenderProbe.on = false;
        phase = WAIT;
    }

    /** Once per rendered frame (FrameTime), at the frame's time in nanoseconds. */
    static void onFrame(long now) {
        if (phase == IDLE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            // not in a world (yet, or any more): begin again when there is one
            if (phase != WAIT) {
                LOG.warn("[eln render bench] the world went away; the run starts again in the next one");
                RenderProbe.on = false;
                phase = WAIT;
            }
            return;
        }
        try {
            step(mc, now);
        } catch (RuntimeException e) {
            LOG.error("[eln render bench] stopped", e);
            RenderProbe.on = false;
            phase = IDLE;
        }
    }

    private static void step(Minecraft mc, long now) {
        if (phase == WAIT) {
            views = BenchRun.parseViews(System.getProperty("eln.renderBench.views"));
            results = new JsonArray();
            index = 0;
            begin(mc, now);
            return;
        }
        BenchRun.View v = views.get(index);
        if (phase == SETTLE) {
            if (now - phaseStart < (long) (seconds("settle", 40) * 1e9)) return;
            RenderProbe.take(counters);   // (what the settling counted is not the measurement's)
            run = new BenchRun();
            pausedFrames = 0;
            yaw0 = mc.player.getYRot();
            stateBefore = Bake.benchState();
            phase = MEASURE;
            phaseStart = last = now;
            return;
        }
        // MEASURE: the frame that just ended
        java.util.Arrays.fill(counters, 0);
        RenderProbe.take(counters);
        run.frame(now - last, counters);
        last = now;
        if (mc.isPaused()) pausedFrames++;
        if (v.spin() != 0) {
            float yaw = yaw0 + v.spin() * (now - phaseStart) * 1e-9f;
            mc.player.setYRot(yaw);
            mc.player.yRotO = yaw;
        }
        if (now - phaseStart < (long) (seconds("measure", 30) * 1e9)) return;
        JsonObject r = run.json();
        JsonObject o = new JsonObject();
        o.addProperty("name", v.name());
        o.addProperty("x", mc.player.getX());
        o.addProperty("y", mc.player.getY());
        o.addProperty("z", mc.player.getZ());
        o.addProperty("yaw", v.spin() != 0 ? yaw0 : mc.player.getYRot());
        o.addProperty("pitch", mc.player.getXRot());
        o.addProperty("spin", v.spin());
        o.addProperty("pausedFrames", pausedFrames);
        for (var e : r.entrySet()) o.add(e.getKey(), e.getValue());
        long[] s = Bake.benchState();
        JsonObject bake = new JsonObject();
        bake.addProperty("devices", s[0]);
        bake.addProperty("settledCalls", s[1]);
        bake.addProperty("settledVertices", s[2]);
        bake.addProperty("heldCalls", s[3]);
        bake.addProperty("settledVerticesBefore", stateBefore[2]);
        o.add("bake", bake);
        results.add(o);
        LOG.info("[eln render bench] {}: {} frames, {} fps, frame ms {}, EA ms {}, EA share {}", v.name(), r.get("frames"), r.get("fps"),
            r.get("frameMs"), r.get("eaMs"), r.get("eaShare"));
        index++;
        if (index < views.size()) begin(mc, now);
        else finish(mc);
    }

    private static void begin(Minecraft mc, long now) {
        BenchRun.View v = views.get(index);
        if (!v.here()) mc.player.connection.sendCommand(BenchRun.teleport(v));
        RenderProbe.on = true;
        phase = SETTLE;
        phaseStart = now;
        LOG.info("[eln render bench] view {} of {}: {}", index + 1, views.size(), v);
    }

    private static void finish(Minecraft mc) {
        RenderProbe.on = false;
        phase = IDLE;
        JsonObject out = new JsonObject();
        out.addProperty("bench", "eln-render-client");
        out.addProperty("version", 1);
        out.addProperty("label", System.getProperty("eln.renderBench.label", "unnamed"));
        out.addProperty("commit", mods.eln.network.BuildInfo.COMMIT);
        out.addProperty("time", java.time.Instant.now().toString());
        out.addProperty("settleSeconds", seconds("settle", 40));
        out.addProperty("measureSeconds", seconds("measure", 30));
        out.addProperty("bakeMode", Bake.mode);
        out.addProperty("bakeStats", mods.eln.gl.BakeStats.ON);
        out.addProperty("java", System.getProperty("java.version"));
        out.addProperty("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        out.addProperty("cpus", Runtime.getRuntime().availableProcessors());
        try {
            out.addProperty("width", mc.getWindow().getWidth());
            out.addProperty("height", mc.getWindow().getHeight());
            int limit = mc.options.framerateLimit().get();
            boolean vsync = mc.options.enableVsync().get();
            out.addProperty("renderDistance", mc.options.renderDistance().get());
            out.addProperty("framerateLimit", limit);
            out.addProperty("vsync", vsync);
            // (the slider's top, 260, means no limit)
            out.addProperty("capped", vsync || limit < 260);
            var info = com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo();
            out.addProperty("gpu", info.name());
            out.addProperty("gpuVendor", info.vendorName());
            out.addProperty("gpuDriver", info.driverInfo());
            out.addProperty("backend", info.backendName());
        } catch (RuntimeException e) {
            out.addProperty("settingsError", e.toString());
        }
        out.add("views", results);
        File file = new File(FILE != null ? FILE : "eln-render-bench.json");
        if (!file.isAbsolute()) file = new File(mc.gameDirectory, file.getPath());
        try (Writer w = new FileWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(out, w);
            LOG.info("[eln render bench] written {}", file);
        } catch (java.io.IOException e) {
            LOG.error("[eln render bench] not written: {}", file, e);
        }
        if (Boolean.getBoolean("eln.renderBench.quit")) mc.stop();
    }
}
