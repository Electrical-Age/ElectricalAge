package mods.eln.gl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Measurement only (static-bake study): per rendered frame, how many of the vertices the block entity renderers send
 * could live in a chunk mesh. A display-list primitive counts as constant when its matrix (relative to the renderer's
 * pose), colour and state equal the previous frame's for the same device and position in the draw order. Enabled by
 * -Deln.bakeStats=true or ELN_BAKE_STATS=1; logs a summary every {@link #PERIOD} frames. With {@link Bake} on, the
 * constant vertices the renderer still draws are split by the reason the chunk mesh does not draw them.
 */
public final class BakeStats {
    private BakeStats() {
    }

    public static final boolean ON = Boolean.getBoolean("eln.bakeStats") || "1".equals(System.getenv("ELN_BAKE_STATS"));
    private static final int PERIOD = 600;

    /** Vertex categories. */
    public static final int CONST = 0, MOVING = 1, FIRST = 2, BLEND = 3, UV = 4, IMMEDIATE = 5, OVERLAY = 6, LINES = 7, SKIPPED = 8, N = 9;
    private static final String[] NAMES = {"const", "moving", "first-seen", "blended", "uv-wrap", "immediate", "soot/ghost", "lines", "in-chunk"};

    private static final class Device {
        long[] hashes = new long[16];
        int count;
        int lastFrame;
    }

    private static final Map<Long, Device> DEVICES = new HashMap<>();
    private static final Map<String, long[]> BY_LABEL = new HashMap<>();
    private static final IdentityHashMap<float[], Boolean> UV_OK = new IdentityHashMap<>();
    private static final long[] TOTAL = new long[N];
    private static final String[] WHY = {"unknown", "not attached", "burnt", "pose", "no block entity", "changed", "waiting",
        "waiting longer", "no sprite", "no whole quad", "mesh not shown yet", "other section hidden"};
    private static final Map<String, long[]> WHY_BY_LABEL = new HashMap<>();
    private static final long[] WHY_TOTAL = new long[Bake.REASONS];
    /** Per label: calls that differed from the last draw, by {@link Bake} change kind; then the settled ones among them. */
    private static final Map<String, int[]> CHANGED_BY_LABEL = new HashMap<>();
    private static final String[] CHANGE = {"new", "matrix", "light", "state"};
    private static final java.util.LinkedHashSet<String> NO_SPRITE = new java.util.LinkedHashSet<>(), TWICE = new java.util.LinkedHashSet<>();
    private static int frames, frameNo;
    private static Device device;
    private static int seq;
    private static String label = "?";
    private static boolean tracked;

    /** A new rendered frame. */
    static void frame() {
        if (!ON) return;
        frameNo++;
        if (++frames < PERIOD) return;
        StringBuilder sb = new StringBuilder("[eln bake stats] per frame over ").append(frames).append(" frames:");
        long all = 0;
        for (long t : TOTAL) all += t;
        for (int i = 0; i < N; i++)
            sb.append(' ').append(NAMES[i]).append('=').append(TOTAL[i] / frames)
                .append(" (").append(all == 0 ? 0 : Math.round(1000.0 * TOTAL[i] / all) / 10.0).append("%)");
        sb.append(" total=").append(all / frames).append(" devices=").append(DEVICES.size());
        List<Map.Entry<String, long[]>> rows = new ArrayList<>(BY_LABEL.entrySet());
        rows.sort((x, y) -> Long.compare(sum(y.getValue()), sum(x.getValue())));
        for (int i = 0; i < Math.min(40, rows.size()); i++) {
            long[] c = rows.get(i).getValue();
            sb.append("\n[eln bake stats]   ").append(rows.get(i).getKey()).append(": total=").append(sum(c) / frames);
            for (int k = 0; k < N; k++) if (c[k] != 0) sb.append(' ').append(NAMES[k]).append('=').append(c[k] / frames);
            long[] w = WHY_BY_LABEL.get(rows.get(i).getKey());
            if (w != null) reasons(sb.append("; const is"), w);
            int[] ch = CHANGED_BY_LABEL.get(rows.get(i).getKey());
            if (ch != null) {
                sb.append("; calls changed by");
                for (int k = 0; k < Bake.CHANGES; k++)
                    if (ch[k] != 0) sb.append(' ').append(CHANGE[k]).append('=').append(ch[k]).append(" (").append(ch[Bake.CHANGES + k]).append(" settled)");
            }
        }
        if (Bake.mode != 0) {
            reasons(sb.append("\n[eln bake stats]   const left to the renderer:"), WHY_TOTAL);
            if (!NO_SPRITE.isEmpty()) sb.append("\n[eln bake stats]   textures without a sprite: ").append(NO_SPRITE);
            if (!TWICE.isEmpty()) sb.append("\n[eln bake stats]   a part drawn twice in the same place: ").append(TWICE);
        }
        sb.append("\n[eln bake stats]   ").append(Bake.statsLine(frames));
        org.slf4j.LoggerFactory.getLogger("eln").info(sb.toString());
        frames = 0;
        java.util.Arrays.fill(TOTAL, 0);
        java.util.Arrays.fill(WHY_TOTAL, 0);
        BY_LABEL.clear();
        WHY_BY_LABEL.clear();
        CHANGED_BY_LABEL.clear();
        NO_SPRITE.clear();
        TWICE.clear();
        DEVICES.values().removeIf(d -> frameNo - d.lastFrame > PERIOD);
    }

    private static void reasons(StringBuilder sb, long[] w) {
        for (int k = 0; k < w.length; k++) if (w[k] != 0) sb.append(' ').append(WHY[k]).append('=').append(w[k] / frames);
    }

    /** A texture that is not in the block atlas kept a primitive out of the chunk mesh. */
    static void noSprite(net.minecraft.resources.Identifier texture) {
        if (NO_SPRITE.size() < 16) NO_SPRITE.add(String.valueOf(texture));
    }

    /** A call differed from the device's last draw of its primitive (settled = that one was in the chunk mesh). */
    static void changed(int kind, boolean settled) {
        int[] c = CHANGED_BY_LABEL.computeIfAbsent(label, k -> new int[2 * Bake.CHANGES]);
        c[kind]++;
        if (settled) c[Bake.CHANGES + kind]++;
    }

    /** The device drew a primitive at a matrix it had already drawn it at in this draw. */
    static void twiceInPlace() {
        if (TWICE.size() < 16) TWICE.add(label);
    }

    private static long sum(long[] c) {
        long s = 0;
        for (long v : c) s += v;
        return s;
    }

    /** The block entity renderer opened a frame for the device at this position. */
    public static void device(long pos, String name) {
        if (!ON) return;
        device = DEVICES.computeIfAbsent(pos, k -> new Device());
        tracked = true;
        seq = 0;
        label = name;
    }

    /** The label for what is drawn next (a six-node's slot). */
    public static void label(String name) {
        if (ON) label = name;
    }

    /** The device's frame is done: forget what it did not draw this time. */
    public static void endDevice() {
        if (!ON || device == null) return;
        device.count = seq;
        device.lastFrame = frameNo;
        device = null;
        tracked = false;
    }

    static void add(int category, int vertices) {
        if (!tracked) return;
        TOTAL[category] += vertices;
        BY_LABEL.computeIfAbsent(label, k -> new long[N])[category] += vertices;
    }

    /**
     * One replayed display-list primitive that the renderer draws; the hash covers its matrix (relative to the pose),
     * colour and state, why is the {@link Bake} reason the chunk mesh does not draw it.
     */
    static void list(float[] quads, int floats, int vertices, long hash, boolean blended, boolean overlay, int why) {
        if (!tracked) return;
        if (overlay) {
            add(OVERLAY, vertices);
            return;
        }
        if (blended) {
            add(BLEND, vertices);
            return;
        }
        Boolean ok = UV_OK.get(quads);
        if (ok == null) {
            ok = true;
            for (int i = 0; i < floats; i += GlFrame.STRIDE) {
                float u = quads[i + 3], v = quads[i + 4];
                if (u < -1e-4f || u > 1.0001f || v < -1e-4f || v > 1.0001f) {
                    ok = false;
                    break;
                }
            }
            UV_OK.put(quads, ok);
        }
        if (!ok) {
            add(UV, vertices);
            return;
        }
        Device d = device;
        int i = seq++;
        if (i >= d.hashes.length) d.hashes = java.util.Arrays.copyOf(d.hashes, d.hashes.length * 2);
        hash = hash * 31 + System.identityHashCode(quads);
        boolean seen = i < d.count && d.lastFrame == frameNo - 1;
        int category = !seen ? FIRST : d.hashes[i] == hash ? CONST : MOVING;
        add(category, vertices);
        d.hashes[i] = hash;
        if (category == CONST && Bake.mode != 0) {
            WHY_TOTAL[why] += vertices;
            WHY_BY_LABEL.computeIfAbsent(label, k -> new long[Bake.REASONS])[why] += vertices;
        }
    }
}
