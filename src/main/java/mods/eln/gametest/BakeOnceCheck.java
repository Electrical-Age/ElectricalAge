package mods.eln.gametest;

import mods.eln.gl.Bake;
import mods.eln.gl.BakeMesh;
import mods.eln.gl.GlFrame;
import mods.eln.misc.UtilsClient;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * A device whose unchanging parts are drawn by the chunk ({@link Bake}) must look as it does without: every quad of
 * its draw is on screen exactly once, from the block entity renderer or from the chunk's geometry, never both and
 * never neither.
 * <p>
 * The device is drawn without baking for reference, then with it on a clock of its own until its parts have
 * settled. While the chunk does not show them the renderer must still draw everything. Once it does, what the
 * renderer leaves out must be exactly what {@link BakeMesh} writes for the settled calls: same place, texture
 * coordinates, state and light, and the colour the entity shader would have given the vertex. When the chunk stops
 * showing them the renderer draws everything again, and a device that is gone leaves nothing behind. Then the same
 * device is drawn only into far frames, as from beyond the renderer's range: one run of them must settle what the
 * near frames did, and the next near frame shows every quad exactly once.
 * <p>
 * Only quads that two reference draws agree on are compared (a part moving on the real clock differs between any
 * two draws), plus the vertex count of the whole frame.
 */
final class BakeOnceCheck {
    private BakeOnceCheck() {
    }

    /** One quad (or line) of a frame: its batch's state and its vertices, compared bit for bit. */
    private static final class Q {
        final GlFrame.Key key;
        final float[] v;
        final int hash;

        Q(GlFrame.Key key, float[] v) {
            this.key = key;
            this.v = v;
            int h = key.hashCode();
            for (float f : v) h = h * 31 + Float.floatToRawIntBits(f);
            hash = h;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Q q) || hash != q.hash || !key.equals(q.key) || v.length != q.v.length) return false;
            for (int i = 0; i < v.length; i++) if (Float.floatToRawIntBits(v[i]) != Float.floatToRawIntBits(q.v[i])) return false;
            return true;
        }
    }

    private static final class Host implements Bake.Host {
        final LinkedHashSet<Bake.Call> settled = new LinkedHashSet<>();

        /** As on a client: every texture a renderer binds is in the atlas but the ones made at run time. */
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

    /** The clock the frames of this check are drawn on: 50 ms a frame. */
    private static long clock = 1_000_000_000_000L;
    private static final long STEP = 50_000_000L;
    /** Frames drawn while the parts settle (well over the 0.4 s a part must stay the same). */
    private static final int SETTLE_FRAMES = 12;
    /** Far frames are asked for over this many frames: one run at most (3 s), and not the next (10 s on). */
    private static final int FAR_FRAMES = 90;
    /** The light the calls are written with, to tell it from full light. */
    private static final int LIGHT = 0x00A000B0;
    private static final int S = GlFrame.STRIDE;

    /** Over all devices checked: vertices of a whole draw, those the chunk took, devices, devices the chunk took none of. */
    private static long allVertices, chunkVertices;
    private static int devices;
    private static final List<String> nothingBaked = new ArrayList<>();

    static void reset() {
        allVertices = chunkVertices = 0;
        devices = 0;
        nothingBaked.clear();
    }

    private static Map<Q, Integer> frame(Object owner, BlockPos pos, Runnable draw) {
        clock += STEP;
        GlFrame.beginRenderFrame(clock);
        GlFrame f = GlFrame.openBlock(new Matrix4f(), new Matrix3f());
        try {
            if (owner != null) f.bake(owner, pos.asLong());
            draw.run();
        } finally {
            GlFrame.close();
        }
        Map<Q, Integer> out = new HashMap<>();
        for (Map.Entry<GlFrame.Key, GlFrame.Batch> e : f.batches().entrySet()) {
            GlFrame.Batch b = e.getValue();
            int size = (e.getKey().blend() == GlFrame.Blend.LINES ? 2 : 4) * S;
            for (int i = 0; i < b.floats; i += size)
                out.merge(new Q(e.getKey(), Arrays.copyOfRange(b.data, i, Math.min(b.floats, i + size))), 1, Integer::sum);
        }
        return out;
    }

    private static int vertices(Map<Q, Integer> frame) {
        int n = 0;
        for (Map.Entry<Q, Integer> e : frame.entrySet()) n += e.getKey().v.length / S * e.getValue();
        return n;
    }

    /** As the entity shader lights a vertex: two lights of 0.6 over 0.4 ambient, the normal a signed byte per axis. */
    private static float shade(float nx, float ny, float nz) {
        double n = 1 / Math.sqrt(0.2 * 0.2 + 1 + 0.7 * 0.7);
        nx = (int) (nx * 127f) / 127f;
        ny = (int) (ny * 127f) / 127f;
        nz = (int) (nz * 127f) / 127f;
        double a = Math.max(0, (0.2 * nx + ny - 0.7 * nz) * n), b = Math.max(0, (-0.2 * nx + ny + 0.7 * nz) * n);
        return (float) Math.min(1, (a + b) * 0.6 + 0.4);
    }

    /**
     * Draw the device (owner = its block entity, at pos) through the states of a baked device and compare. Adds one
     * check and one INFO line to the context.
     */
    static void check(TestContext ctx, String name, Object owner, BlockPos pos, Runnable draw) {
        Bake.Host oldHost = Bake.host;
        int oldMode = Bake.mode;
        Host host = new Host();
        Bake.host = host;
        Bake.mode = 1;
        List<String> errors = new ArrayList<>();
        int total = 0, fixed = 0, inChunk = 0, farInChunk = 0;
        try {
            Map<Q, Integer> before = frame(null, pos, draw);
            Map<Q, Integer> waiting = null;
            for (int i = 0; i < SETTLE_FRAMES; i++) waiting = frame(owner, pos, draw);
            for (Bake.Call c : host.settled) c.shown = true;
            Map<Q, Integer> shown = frame(owner, pos, draw);
            // (a part that changed in that very frame was drawn by the renderer and has left)
            Bake.Call[] calls = Bake.inDrawOrder(host.settled);
            for (Bake.Call c : host.settled) c.shown = false;
            Map<Q, Integer> hidden = frame(owner, pos, draw);
            Map<Q, Integer> after = frame(null, pos, draw);

            // what any draw of the device has: the quads both references agree on
            Map<Q, Integer> same = new HashMap<>();
            for (Map.Entry<Q, Integer> e : before.entrySet()) {
                Integer n = after.get(e.getKey());
                if (n != null) same.put(e.getKey(), Math.min(n, e.getValue()));
            }
            total = vertices(after);
            fixed = vertices(same);
            boolean steady = vertices(before) == total;

            Map<GlFrame.Key, List<Q>> byKey = new HashMap<>();
            for (Q q : same.keySet()) byKey.computeIfAbsent(q.key, k -> new ArrayList<>()).add(q);
            Map<Q, Integer> chunk = new HashMap<>();
            inChunk = chunk(calls, byKey, pos, chunk, errors);

            for (Map.Entry<Q, Integer> e : same.entrySet()) {
                Q q = e.getKey();
                int want = e.getValue();
                int w = waiting.getOrDefault(q, 0), r = shown.getOrDefault(q, 0), c = chunk.getOrDefault(q, 0), h = hidden.getOrDefault(q, 0);
                if (w != want) errors.add(where(q) + " is drawn " + w + " times, not " + want + ", before the chunk shows it");
                if (r + c != want) errors.add(where(q) + " is drawn " + r + " times by the renderer and " + c + " by the chunk, not " + want + " in all");
                if (h != want) errors.add(where(q) + " is drawn " + h + " times, not " + want + ", after the chunk stopped showing it");
            }
            if (steady) {
                if (vertices(waiting) != total) errors.add(vertices(waiting) + " vertices, not " + total + ", before the chunk shows the parts");
                if (vertices(shown) + inChunk != total) errors.add(vertices(shown) + " vertices from the renderer and " + inChunk + " from the chunk, not " + total + " in all");
                if (vertices(hidden) != total) errors.add(vertices(hidden) + " vertices, not " + total + ", after the chunk stopped showing the parts");
            }
            Bake.sweep(o -> o == owner);
            if (!host.settled.isEmpty()) errors.add(host.settled.size() + " parts are left in the chunk when the device is gone");

            // the same device seen only from beyond the renderer's range: far frames settle what the near ones did
            int looks = 0;
            for (int i = 0; i < FAR_FRAMES; i++) {
                clock += STEP;
                GlFrame.beginRenderFrame(clock);
                if (!Bake.farDue(owner, pos.asLong())) continue;
                looks++;
                GlFrame f = GlFrame.openBlock(new Matrix4f(), new Matrix3f());
                try {
                    f.bake(owner, pos.asLong(), true);
                    draw.run();
                } finally {
                    GlFrame.close();
                }
            }
            if (looks < 2 || looks > 9) errors.add(looks + " far frames in " + FAR_FRAMES * STEP / 1_000_000 + " ms, not 2 to 9 (one run)");
            Map<Q, Integer> farChunk = new HashMap<>();
            farInChunk = chunk(Bake.inDrawOrder(host.settled), byKey, pos, farChunk, errors);
            if (steady && farInChunk != inChunk) errors.add("far frames settled " + farInChunk + " vertices, the near frames " + inChunk);
            for (Bake.Call c : host.settled) c.shown = true;
            Map<Q, Integer> farShown = frame(owner, pos, draw);
            for (Map.Entry<Q, Integer> e : same.entrySet()) {
                Q q = e.getKey();
                int r = farShown.getOrDefault(q, 0), c = farChunk.getOrDefault(q, 0);
                if (r + c != e.getValue()) errors.add(where(q) + " is drawn " + r + " times by the renderer and " + c + " by the chunk after far frames, not " + e.getValue());
            }
            Bake.sweep(o -> o == owner);
            if (!host.settled.isEmpty()) errors.add(host.settled.size() + " parts are left in the chunk when the device is gone (far)");
        } catch (RuntimeException ex) {
            errors.add(ex.toString());
        } finally {
            Bake.sweep(o -> o == owner);
            Bake.host = oldHost;
            Bake.mode = oldMode;
        }
        devices++;
        allVertices += total;
        chunkVertices += inChunk;
        if (inChunk == 0) nothingBaked.add(name);
        ctx.check("bake " + name + ": every quad drawn exactly once, near and far", errors.isEmpty(),
            errors.size() + " differences, the first: " + errors.subList(0, Math.min(3, errors.size())));
        ctx.line(String.format("INFO bake %s: %d of %d vertices from the chunk (%d the same in every draw; far frames: %d)", name, inChunk, total, fixed, farInChunk));
    }

    /**
     * The chunk's geometry for the settled calls, each quad matched to one of the reference quads (byKey) and counted
     * in chunk; returns its vertices. What does not match goes to errors.
     */
    private static int chunk(Bake.Call[] calls, Map<GlFrame.Key, List<Q>> byKey, BlockPos pos, Map<Q, Integer> chunk, List<String> errors) {
        int inChunk = 0;
        Map<Identifier, BakeMesh.Spr> sprites = new HashMap<>();
        float[][] buf = {new float[7 * 64]};
        int[] n = {0};
        BakeMesh.Sink sink = (cutout, x, y, z, color, u, v, light) -> {
            if (n[0] + 7 > buf[0].length) buf[0] = Arrays.copyOf(buf[0], buf[0].length * 2);
            float[] d = buf[0];
            int o = n[0];
            d[o] = x;
            d[o + 1] = y;
            d[o + 2] = z;
            d[o + 3] = u;
            d[o + 4] = v;
            d[o + 5] = Float.intBitsToFloat(color);
            d[o + 6] = Float.intBitsToFloat(light);
            n[0] = o + 7;
        };
        for (Bake.Call c : calls) {
            sprites.put(c.texture == null ? UtilsClient.WHITE : c.texture, new BakeMesh.Spr(0, 0, 1, 1, false));
            n[0] = 0;
            BakeMesh.write(sink, new Bake.Call[]{c}, new int[]{LIGHT}, sprites, pos.getX(), pos.getY(), pos.getZ(), false);
            List<Q> candidates = byKey.get(new GlFrame.Key(c.texture, GlFrame.Blend.NONE, c.cull, c.fullBright, c.lit));
            int per = c.cull ? 28 : 56;
            if (n[0] % per != 0) errors.add("the chunk's part of " + c.texture + " is not whole quads");
            int at = 0;
            for (int i = 0; i + per <= n[0]; i += per) {
                float[] d = buf[0];
                if (!c.cull) {
                    // seen from both sides: the same quad again, wound the other way
                    for (int k = 0; k < 4; k++)
                        for (int j = 0; j < 7; j++)
                            if (Float.floatToRawIntBits(d[i + k * 7 + j]) != Float.floatToRawIntBits(d[i + 28 + (3 - k) * 7 + j])) {
                                errors.add("the back of a two-sided quad of " + c.texture + " is not its front reversed");
                                k = 4;
                                break;
                            }
                }
                Q hit = null;
                String wrong = null;
                int size = candidates == null ? 0 : candidates.size();
                for (int t = 0; t < size; t++) {
                    // (the chunk writes in the order of the draw, so the next one is usually right after the last)
                    Q q = candidates.get((at + t) % size);
                    if (!matches(q.v, d, i)) continue;
                    // (a device may draw two quads in one place, in different colours)
                    String w = colours(q, d, i, c.fullBright);
                    if (hit == null || w == null) {
                        hit = q;
                        wrong = w;
                    }
                    if (w == null) {
                        at = (at + t + 1) % size;
                        break;
                    }
                }
                if (hit == null) {
                    errors.add(String.format("the chunk draws a quad of %s at %.3f %.3f %.3f that the device does not", c.texture, d[i], d[i + 1], d[i + 2]));
                    continue;
                }
                chunk.merge(hit, 1, Integer::sum);
                inChunk += 4;
                if (wrong != null) errors.add(wrong);
            }
        }
        return inChunk;
    }

    /** Whether the chunk's quad at d[i] (7 floats a vertex) is where the reference quad is, with its texture coordinates. */
    private static boolean matches(float[] q, float[] d, int i) {
        for (int k = 0; k < 4; k++) {
            int a = k * S, b = i + k * 7;
            if (Math.abs(q[a] - d[b]) > 1e-4f || Math.abs(q[a + 1] - d[b + 1]) > 1e-4f || Math.abs(q[a + 2] - d[b + 2]) > 1e-4f
                || Math.abs(q[a + 3] - d[b + 3]) > 1e-6f || Math.abs(q[a + 4] - d[b + 4]) > 1e-6f) return false;
        }
        return true;
    }

    /** The chunk's colours and light against the reference quad's as the entity shader shows it; null = the same. */
    private static String colours(Q q, float[] d, int i, boolean fullBright) {
        for (int k = 0; k < 4; k++) {
            int a = k * S, b = i + k * 7;
            int col = Float.floatToRawIntBits(q.v[a + 5]), got = Float.floatToRawIntBits(d[b + 5]);
            float shade = q.key.lit() ? shade(q.v[a + 6], q.v[a + 7], q.v[a + 8]) : 1;
            for (int shift = 0; shift <= 16; shift += 8) {
                int want = Math.round(((col >> shift) & 255) * shade);
                if (Math.abs(want - ((got >> shift) & 255)) > 3)
                    return String.format("%s is %06x in the chunk, %06x from the renderer (%06x shaded %.3f)", where(q), got & 0xFFFFFF,
                        (Math.round(((col >> 16) & 255) * shade) << 16) | (Math.round(((col >> 8) & 255) * shade) << 8) | Math.round((col & 255) * shade),
                        col & 0xFFFFFF, shade);
            }
            int light = Float.floatToRawIntBits(d[b + 6]);
            if (light != (fullBright ? LightCoordsUtil.FULL_BRIGHT : LIGHT))
                return where(q) + " has light " + Integer.toHexString(light) + " in the chunk" + (fullBright ? ", not full light" : ", not its block's");
        }
        return null;
    }

    private static String where(Q q) {
        return String.format("the quad at %.3f %.3f %.3f of %s", q.v[0], q.v[1], q.v[2], q.key.texture());
    }

    /** The share of all checked devices' geometry the chunk took, with a floor against it quietly falling to nothing. */
    static void summary(TestContext ctx, double floor) {
        double share = allVertices == 0 ? 0 : (double) chunkVertices / allVertices;
        ctx.line(String.format("INFO bake: the chunk draws %d of %d vertices (%.1f%%) of %d devices; none of %d: %s", chunkVertices, allVertices,
            share * 100, devices, nothingBaked.size(), nothingBaked));
        ctx.check("bake: the chunk draws most of the devices' geometry", share >= floor,
            String.format("%.1f%% of the vertices, at least %.0f%% wanted", share * 100, floor * 100));
    }
}
