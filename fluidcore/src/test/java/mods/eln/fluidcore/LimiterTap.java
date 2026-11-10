package mods.eln.fluidcore;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Watches every Limiter call of a test: checks what spec/'s theorems assume of the arguments, and, with
 * FLUIDCORE_VECTORS=file, appends a sample of the calls (arguments and results as raw double bits) for spec/'s
 * comparison of its Lean limiter with this one.
 *
 * A line: V source C cells DOWN_PASSES UP_PASSES SLACK passes moves, then the cells' y0, each move's from, to,
 * component, amount and edge, the cells' scales and the cells' results (doubles as 16 hex digits).
 */
final class LimiterTap implements Limiter.Tap, AutoCloseable {
    private final String source;
    private final Writer out;
    private String broken;
    private long plain, cut, silenced;

    private LimiterTap(String source) throws IOException {
        this.source = source;
        String file = System.getenv("FLUIDCORE_VECTORS");
        out = file == null ? null : Files.newBufferedWriter(Path.of(file), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        Limiter.TAP = this;
    }

    static LimiterTap open(String source) throws IOException {
        return new LimiterTap(source);
    }

    @Override
    public synchronized void call(int C, double[] y0, List<double[]> moves, int passes, double[] scale, double[] y) {
        int n = y0.length / C;
        for (double v : y0) if (!Double.isFinite(v)) broken = "y0 " + v;
        for (double[] mv : moves) {
            boolean ends = mv[0] >= -1 && mv[0] < n && mv[1] >= -1 && mv[1] < n && (mv[0] != mv[1] || mv[0] == -1);
            if (!(ends && mv[2] >= 0 && mv[2] < C && mv[3] > 0 && Double.isFinite(mv[3]) && mv[4] >= -1)) broken = "move " + java.util.Arrays.toString(mv);
        }
        if (out == null) return;
        // a sample: the first of each kind, then now and then
        long seen = passes == 0 ? plain++ : passes <= Limiter.DOWN_PASSES ? cut++ : silenced++;
        long first = passes == 0 ? 30 : 150, every = passes == 0 ? 20000 : passes <= Limiter.DOWN_PASSES ? 500 : 20;
        if (seen >= first && seen % every != 0) return;
        StringBuilder o = new StringBuilder("V ").append(source).append(' ').append(C).append(' ').append(y0.length).append(' ')
            .append(Limiter.DOWN_PASSES).append(' ').append(Limiter.UP_PASSES).append(' ').append(hex(Limiter.SLACK)).append(' ')
            .append(passes).append(' ').append(moves.size());
        for (double v : y0) o.append(' ').append(hex(v));
        for (double[] mv : moves) o.append(' ').append((int) mv[0]).append(' ').append((int) mv[1]).append(' ').append((int) mv[2]).append(' ').append(hex(mv[3])).append(' ').append((int) mv[4]);
        for (double v : scale) o.append(' ').append(hex(v));
        for (double v : y) o.append(' ').append(hex(v));
        try {
            out.append(o).append('\n');
        } catch (IOException e) {
            broken = e.toString();
        }
    }

    static String hex(double v) {
        return String.format("%016x", Double.doubleToRawLongBits(v));
    }

    /** Stops watching; fails if any call broke the assumptions. */
    @Override
    public void close() throws IOException {
        Limiter.TAP = null;
        if (out != null) out.close();
        if (broken != null) throw new AssertionError("Limiter called outside what its proofs assume: " + broken);
    }
}
