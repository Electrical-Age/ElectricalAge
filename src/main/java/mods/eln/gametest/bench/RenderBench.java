package mods.eln.gametest.bench;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mods.eln.gametest.PublishRenderCase;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gl.Bake;
import mods.eln.gl.BakeMesh;
import mods.eln.gl.FrameStats;
import mods.eln.gl.GlFrame;
import mods.eln.misc.UtilsClient;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * The render benchmark's CPU tier (todo #424; Baughn 2026-11-12: "benchmarks as part of the test suite"). Run alone:
 * `gradlew runGameTestServer -PelnRenderBench=/path/result.json [-PelnBenchLabel=...]`; then no other test is
 * registered. No GPU and no client: every device with a render is placed and published as in
 * {@link PublishRenderCase}, and its draw code is timed into a {@link GlFrame}:
 * - vertices and ns per draw as the block entity renderer draws it without baking (record), and the ns to copy the
 *   frame's batches into a buffer in the entity vertex layout (pack: a stand-in for the write loop, not the game's
 *   BufferBuilder);
 * - with its unchanging parts in the chunk ({@link Bake}, frames 50 ms apart on a clock of the benchmark's own):
 *   when the last part settled, the vertices the renderer still sends and the ns per draw that are left, and the
 *   ns for {@link BakeMesh#write} of the settled parts (the device's share of a section compile).
 * Vertex and call counts are exact and repeat from run to run (but for draw code that reads the wall clock); times
 * are for comparing two builds on the same idle machine, and say nothing about frames per second (a client's cost is
 * mostly the vertex write and the GPU: measure that with the client tier, mods.eln.client.RenderBenchClient).
 */
public final class RenderBench implements TestCase {
    static final int WARMUP = 300, BATCHES = 7, PER_BATCH = 200, SETTLE_FRAMES = 40, MESH_PER_BATCH = 20;
    static final long STEP = 50_000_000L;
    /** Bytes of a vertex in the entity format: position, colour, uv, overlay, light, normal and a pad byte. */
    static final int VERTEX_BYTES = 36;

    public static boolean enabled() {
        return System.getProperty("eln.bench.render") != null;
    }

    @Override
    public String id() {
        return "render_bench";
    }

    @Override
    public int width() {
        return 9;
    }

    @Override
    public int height() {
        return 7;
    }

    @Override
    public int depth() {
        return 7;
    }

    @Override
    public int ticks() {
        return 1;
    }

    private static final class Host implements Bake.Host {
        final LinkedHashSet<Bake.Call> settled = new LinkedHashSet<>();

        @Override
        public boolean hasSprite(Identifier texture) {
            return texture == null || !texture.getPath().startsWith("dynamic/");
        }

        @Override
        public void settled(Bake.Call c) {
            settled.add(c);
        }

        @Override
        public void unsettled(Bake.Call c) {
            settled.remove(c);
        }

        @Override
        public void frame() {
        }

        @Override
        public String stats() {
            return "";
        }
    }

    private long clock = 2_000_000_000_000L;
    private final JsonArray devices = new JsonArray();
    private final long[] totals = new long[8];
    private static final String[] TOTAL_NAMES = {"vertices", "rendererVertices", "chunkVertices", "meshVertices", "recordNs", "packNs",
        "bakedRecordNs", "meshNs"};
    private int errors;
    private ByteBuffer pack = ByteBuffer.allocateDirect(VERTEX_BYTES * 4096).order(ByteOrder.nativeOrder());
    /** Kept from being optimised away. */
    private long sink;

    @Override
    public void build(TestContext ctx) {
    }

    @Override
    public void measure(TestContext ctx) {
        long t0 = System.nanoTime();
        PublishRenderCase.probe = (name, six, owner, pos, draw) -> {
            try {
                devices.add(device(name, six, owner, pos, draw));
            } catch (RuntimeException ex) {
                errors++;
                GlFrame.close();
                ctx.check("render_bench " + name, false, ex.toString());
            }
        };
        try {
            new PublishRenderCase().measure(ctx);
        } finally {
            PublishRenderCase.probe = null;
        }
        JsonObject out = new JsonObject();
        out.addProperty("bench", "eln-render-cpu");
        out.addProperty("version", 1);
        out.addProperty("label", System.getProperty("eln.bench.label", "unnamed"));
        out.addProperty("commit", mods.eln.network.BuildInfo.COMMIT);
        out.addProperty("time", java.time.Instant.now().toString());
        out.addProperty("java", System.getProperty("java.version"));
        out.addProperty("cpus", Runtime.getRuntime().availableProcessors());
        out.addProperty("warmupDraws", WARMUP);
        out.addProperty("batches", BATCHES);
        out.addProperty("drawsPerBatch", PER_BATCH);
        out.addProperty("seconds", (System.nanoTime() - t0) / 1e9);
        JsonObject tot = new JsonObject();
        tot.addProperty("devices", devices.size());
        for (int i = 0; i < totals.length; i++) tot.addProperty(TOTAL_NAMES[i], totals[i]);
        tot.addProperty("chunkShare", totals[0] == 0 ? 0 : (double) totals[2] / totals[0]);
        out.add("totals", tot);
        out.add("devices", devices);
        String file = System.getProperty("eln.bench.render");
        boolean written = true;
        try (Writer w = new FileWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(out, w);
        } catch (IOException e) {
            written = false;
            ctx.line("render_bench: " + e);
        }
        ctx.check("render_bench: result written to " + file, written, "");
        ctx.check("render_bench: devices measured", devices.size() > 100 && errors == 0, devices.size() + " devices, " + errors + " errors");
        ctx.line(String.format("INFO render_bench: %d devices; one draw of each: %d vertices, %.2f ms recorded, %.2f ms packed; with the bake: "
                + "%d vertices left with the renderer (%.1f%% in the chunk), %.2f ms recorded; meshing all: %.2f ms", devices.size(), totals[0],
            totals[4] / 1e6, totals[5] / 1e6, totals[1], totals[0] == 0 ? 0 : 100.0 * totals[2] / totals[0], totals[6] / 1e6, totals[7] / 1e6));
    }

    private GlFrame frame(Object owner, BlockPos pos, Runnable draw) {
        clock += STEP;
        GlFrame.beginRenderFrame(clock);
        GlFrame f = GlFrame.openBlock(new Matrix4f(), new Matrix3f());
        try {
            if (owner != null) f.bake(owner, pos.asLong());
            draw.run();
        } finally {
            GlFrame.close();
        }
        return f;
    }

    private static int vertices(GlFrame f) {
        int n = 0;
        for (GlFrame.Batch b : f.batches().values()) n += b.vertices();
        return n;
    }

    /** ns per run: the median and the fastest of BATCHES batches of per runs each; between() runs between batches. */
    private static FrameStats.Summary time(int per, Runnable run, Runnable between) {
        long[] ns = new long[BATCHES];
        for (int b = 0; b < BATCHES; b++) {
            between.run();
            long t0 = System.nanoTime();
            for (int i = 0; i < per; i++) run.run();
            ns[b] = (System.nanoTime() - t0) / per;
        }
        return FrameStats.summary(ns, BATCHES);
    }

    /** The frame's batches as the write loop would send them: 36 bytes a vertex into a buffer. */
    private void pack(GlFrame f) {
        ByteBuffer out = pack;
        out.clear();
        final int S = GlFrame.STRIDE;
        for (GlFrame.Batch b : f.batches().values()) {
            if (out.remaining() < b.vertices() * VERTEX_BYTES) {
                ByteBuffer bigger = ByteBuffer.allocateDirect(Math.max(out.capacity() * 2, out.position() + b.vertices() * VERTEX_BYTES)).order(ByteOrder.nativeOrder());
                out.flip();
                bigger.put(out);
                pack = out = bigger;
            }
            float[] d = b.data;
            for (int i = 0; i < b.floats; i += S) {
                out.putFloat(d[i]).putFloat(d[i + 1]).putFloat(d[i + 2]);
                out.putInt(Float.floatToRawIntBits(d[i + 5]));
                out.putFloat(d[i + 3]).putFloat(d[i + 4]);
                out.putInt(0x000A0000).putInt(0x00F000F0);
                out.put((byte) (d[i + 6] * 127f)).put((byte) (d[i + 7] * 127f)).put((byte) (d[i + 8] * 127f)).put((byte) 0);
            }
        }
        sink += out.position();
    }

    private static void add(JsonObject o, String name, FrameStats.Summary s) {
        o.addProperty(name, s.p50());
        o.addProperty(name + "Min", s.min());
    }

    private JsonObject device(String name, boolean six, Object owner, BlockPos pos, Runnable draw) {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("kind", six ? "six" : "transparent");
        Bake.Host oldHost = Bake.host;
        int oldMode = Bake.mode;
        Host host = new Host();
        Bake.host = host;
        Bake.mode = 1;
        try {
            // as the renderer draws it with nothing in the chunk
            int total = vertices(frame(null, pos, draw));
            for (int i = 0; i < WARMUP; i++) frame(null, pos, draw);
            FrameStats.Summary record = time(PER_BATCH, () -> frame(null, pos, draw), () -> {
            });
            GlFrame last = frame(null, pos, draw);
            boolean steady = vertices(last) == total;
            for (int i = 0; i < WARMUP; i++) pack(last);
            FrameStats.Summary packed = time(PER_BATCH, () -> pack(last), () -> {
            });

            // its unchanging parts settle into the chunk
            int settleFrame = 0, before = 0;
            for (int i = 1; i <= SETTLE_FRAMES; i++) {
                frame(owner, pos, draw);
                if (host.settled.size() != before) {
                    before = host.settled.size();
                    settleFrame = i;
                }
            }
            Runnable show = () -> {
                for (Bake.Call c : host.settled) c.shown = true;
            };
            for (int i = 0; i < WARMUP; i++) {
                show.run();
                frame(owner, pos, draw);
            }
            show.run();
            int left = vertices(frame(owner, pos, draw));
            FrameStats.Summary baked = time(PER_BATCH, () -> frame(owner, pos, draw), show);

            // the settled parts as chunk geometry
            Bake.Call[] calls = Bake.inDrawOrder(host.settled);
            Map<Identifier, BakeMesh.Spr> sprites = new HashMap<>();
            for (Bake.Call c : calls) sprites.put(c.texture == null ? UtilsClient.WHITE : c.texture, new BakeMesh.Spr(0, 0, 1, 1, false));
            int[] lights = new int[calls.length];
            long[] count = {0};
            BakeMesh.Sink meshSink = (cutout, x, y, z, color, u, v, light) -> count[0] += color;
            Runnable mesh = () -> BakeMesh.write(meshSink, calls, lights, sprites, pos.getX(), pos.getY(), pos.getZ(), false);
            int meshVertices = BakeMesh.write(meshSink, calls, lights, sprites, pos.getX(), pos.getY(), pos.getZ(), false);
            for (int i = 0; i < 3 * MESH_PER_BATCH; i++) mesh.run();
            FrameStats.Summary meshed = time(MESH_PER_BATCH, mesh, () -> {
            });
            sink += count[0];

            int chunk = Math.max(0, total - left);
            o.addProperty("vertices", total);
            o.addProperty("steady", steady);
            add(o, "recordNs", record);
            add(o, "packNs", packed);
            o.addProperty("settledCalls", calls.length);
            o.addProperty("settleFrame", settleFrame);
            o.addProperty("rendererVertices", left);
            o.addProperty("chunkVertices", chunk);
            o.addProperty("meshVertices", meshVertices);
            add(o, "bakedRecordNs", baked);
            add(o, "meshNs", meshed);
            long[] t = {total, left, chunk, meshVertices, record.p50(), packed.p50(), baked.p50(), meshed.p50()};
            for (int i = 0; i < t.length; i++) totals[i] += t[i];
        } finally {
            Bake.sweep(x -> x == owner);
            Bake.host = oldHost;
            Bake.mode = oldMode;
        }
        return o;
    }
}
