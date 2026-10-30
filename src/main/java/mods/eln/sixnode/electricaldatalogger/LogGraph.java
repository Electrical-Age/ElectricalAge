package mods.eln.sixnode.electricaldatalogger;

import mods.eln.misc.Utils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * A logger's graph on a screen (board #45), newest point at the right, in one of three modes (Logger.LINES, STACKED,
 * LANES). Lines: each channel's range over each period as a faint band, its mean as a line; an axis per scale, the first
 * on the left with a grid, the others on the right in columns. Stacked: the channels' means as areas piled up in their
 * units (the axis reads the total, in the first channel's unit). Lanes: each channel in a band of its own, on its own
 * scale. Compact (a small pane): an axis shows only its ends.
 */
public final class LogGraph {
    static final int BACKGROUND = 0xFF181C1A, GRID = 0xFF323834, AXIS = 0xFF606660, TEXT = 0xFFA0A8A0;

    private LogGraph() {
    }

    /** The graph in (x, y, w, h); colour(c) is channel c's colour. */
    public static void draw(GuiGraphicsExtractor g, Font f, int x, int y, int w, int h, DataLogs log, IntUnaryOperator colour, boolean compact) {
        g.fill(x, y, x + w, y + h, BACKGROUND);
        int gy = y + 5, gh = h - 5 - 11;
        if (gh < 8 || w < 20) return;
        switch (log.mode) {
            case Logger.STACKED -> stacked(g, f, x, gy, w, gh, log, colour, compact);
            case Logger.LANES -> lanes(g, f, x, gy, w, gh, log, colour);
            default -> lines(g, f, x, gy, w, gh, log, colour, compact);
        }
        int n = log.size();
        String span = "-" + Utils.plotTime(n * log.samplingPeriod).trim(), now = "0";
        g.text(f, span, x + 2, gy + gh + 2, TEXT, false);
        g.text(f, now, x + w - 2 - f.width(now), gy + gh + 2, TEXT, false);
    }

    static String label(double v, LogChannel s) {
        return DataLogs.format(v, s.unit).trim();
    }

    static void lines(GuiGraphicsExtractor g, Font f, int x, int gy, int w, int gh, DataLogs log, IntUnaryOperator colour, boolean compact) {
        var axes = LogAxes.layout(log.channelList());
        int ticksMax = compact ? 2 : Math.max(2, gh / 12 + 1);
        String[][] labels = new String[axes.size()][];
        double[][] ticks = new double[axes.size()][];
        int[] widths = new int[axes.size()];
        for (int i = 0; i < axes.size(); i++) {
            LogChannel s = axes.get(i).scale();
            ticks[i] = compact ? new double[]{s.min, s.max} : LogAxes.ticks(s.min, s.max, ticksMax);
            labels[i] = new String[ticks[i].length];
            for (int t = 0; t < ticks[i].length; t++) {
                labels[i][t] = label(ticks[i][t], s);
                widths[i] = Math.max(widths[i], f.width(labels[i][t]));
            }
        }
        // a compact graph keeps its width for the data: the labels sit inside it
        int left = compact || axes.isEmpty() ? 2 : widths[0] + 4, right = 2;
        int[] colX = new int[axes.size()];
        for (int i = 1; i < axes.size(); i++) {
            colX[i] = right;
            if (!compact) right += widths[i] + 4;
        }
        int gx = x + left, gw = w - left - right;
        // the data first, the axes' labels over it
        g.fill(gx, gy + gh, gx + gw, gy + gh + 1, AXIS);
        for (int c = 0; c < log.channelCount(); c++) series(g, gx, gy, gw, gh, log, c, colour.applyAsInt(c), 0, 1);
        for (int i = 0; i < axes.size(); i++) {
            LogChannel s = axes.get(i).scale();
            int textColour = axes.size() == 1 ? TEXT : colour.applyAsInt(axes.get(i).channels().get(0));
            for (int t = 0; t < ticks[i].length; t++) {
                float frac = s.max == s.min ? 0 : (float) ((ticks[i][t] - s.min) / (s.max - s.min));
                int ty = gy + gh - Math.round(frac * gh);
                if (i == 0) g.fill(gx, ty, gx + gw, ty + 1, ticks[i][t] == 0 ? AXIS : GRID);
                // compact: the ends of the first two axes, inside the graph (lanes show every channel's scale)
                if (compact && i > 1) continue;
                int lw = f.width(labels[i][t]);
                int tx = compact ? (i == 0 ? gx + 1 : gx + gw - 1 - lw) : i == 0 ? gx - 2 - lw : gx + gw + colX[i] + 2;
                int ly = compact ? (t == 0 ? gy + gh - 9 : gy + 1) : ty - 4;
                g.text(f, labels[i][t], tx, Math.max(gy - 4, Math.min(gy + gh - 4, ly)), textColour, false);
            }
        }
    }

    /** Channel c's band and line over (gx, gy, gw, gh), signal lo..hi mapped to the bottom..top. */
    static void series(GuiGraphicsExtractor g, int gx, int gy, int gw, int gh, DataLogs log, int c, int ink, float lo, float hi) {
        int n = log.size();
        if (n < 1 || gw < 2) return;
        int band = ink & 0x00FFFFFF | 0x50000000;
        int prevY = Integer.MIN_VALUE;
        for (int px = 0; px < gw; px++) {
            float[] v = column(log, c, px, gw, n);
            if (v == null) {
                prevY = Integer.MIN_VALUE;
                continue;
            }
            int yHi = gy + gh - Math.round(clamp((v[2] - lo) / (hi - lo)) * gh), yLo = gy + gh - Math.round(clamp((v[1] - lo) / (hi - lo)) * gh);
            if (yLo > yHi) g.fill(gx + px, yHi, gx + px + 1, yLo + 1, band);
            int yy = gy + gh - Math.round(clamp((v[0] - lo) / (hi - lo)) * gh);
            int y0 = prevY == Integer.MIN_VALUE ? yy : Math.min(prevY, yy), y1 = prevY == Integer.MIN_VALUE ? yy : Math.max(prevY, yy);
            g.fill(gx + px, y0, gx + px + 1, y1 + 1, ink);
            prevY = yy;
        }
    }

    /** The points a pixel column covers (age 0, the newest, at the right): {mean, min, max}, or null. */
    static float[] column(DataLogs log, int c, int px, int gw, int n) {
        float a0 = (gw - 1 - px) / (float) gw * n, a1 = (gw - px) / (float) gw * n;
        float lo = Float.POSITIVE_INFINITY, hi = Float.NEGATIVE_INFINITY, sum = 0;
        int cnt = 0;
        for (int age = (int) a0; age < Math.max((int) a0 + 1, (int) Math.ceil(a1)) && age < n; age++) {
            float m = log.mean(c, age);
            if (Float.isNaN(m)) continue;
            lo = Math.min(lo, log.min(c, age));
            hi = Math.max(hi, log.max(c, age));
            sum += m;
            cnt++;
        }
        return cnt == 0 ? null : new float[]{sum / cnt, lo, hi};
    }

    static void stacked(GuiGraphicsExtractor g, Font f, int x, int gy, int w, int gh, DataLogs log, IntUnaryOperator colour, boolean compact) {
        int n = log.size(), nc = log.channelCount();
        double top = 0;
        for (LogChannel c : log.channels) top += Math.max(0, Math.max(c.max, c.min));
        if (top <= 0) top = 1;
        LogChannel first = log.channels[0];
        String hi = label(top, first), mid = label(top / 2, first);
        int gx = x + 2, gw = w - 4;
        g.fill(gx, gy, gx + gw, gy + 1, GRID);
        g.fill(gx, gy + gh / 2, gx + gw, gy + gh / 2 + 1, GRID);
        g.fill(gx, gy + gh, gx + gw, gy + gh + 1, AXIS);
        for (int px = 0; px < gw && n > 0; px++) {
            double sum = 0;
            for (int c = 0; c < nc; c++) {
                float[] v = column(log, c, px, gw, n);
                if (v == null) continue;
                double value = Math.max(0, log.channels[c].value(v[0]));
                int y0 = gy + gh - (int) Math.round(Math.min(1, sum / top) * gh);
                sum += value;
                int y1 = gy + gh - (int) Math.round(Math.min(1, sum / top) * gh);
                if (y1 < y0) g.fill(gx + px, y1, gx + px + 1, y0, colour.applyAsInt(c) & 0x00FFFFFF | 0xC0000000);
            }
        }
        g.text(f, hi, gx + 1, gy + 1, TEXT, false);
        if (!compact) g.text(f, mid, gx + 1, gy + gh / 2 + 1, TEXT, false);
    }

    static void lanes(GuiGraphicsExtractor g, Font f, int x, int gy, int w, int gh, DataLogs log, IntUnaryOperator colour) {
        int nc = Math.max(1, log.channelCount());
        int gx = x + 2, gw = w - 4;
        for (int c = 0; c < log.channelCount(); c++) {
            int ly = gy + c * gh / nc, lh = (c + 1) * gh / nc - c * gh / nc - 1;
            g.fill(gx, ly + lh, gx + gw, ly + lh + 1, c == nc - 1 ? AXIS : GRID);
            series(g, gx, ly + 1, gw, lh - 1, log, c, colour.applyAsInt(c), 0, 1);
            LogChannel ch = log.channels[c];
            String name = ch.name.isEmpty() ? "" : ch.name + " ";
            String text = name + label(ch.max, ch);
            g.fill(gx, ly, gx + f.width(text) + 2, ly + 9, 0xB0181C1A);   // over the data, on a dark backing
            g.text(f, text, gx + 1, ly + 1, colour.applyAsInt(c), false);
        }
    }

    static float clamp(float v) {
        return Math.max(0, Math.min(1, v));
    }

    /** The modes' names, for a button. */
    public static final List<String> MODE_NAMES = List.of("Lines", "Stacked", "Lanes");
}
