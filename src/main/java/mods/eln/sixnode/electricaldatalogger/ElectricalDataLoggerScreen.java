package mods.eln.sixnode.electricaldatalogger;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * Data logger (P4b; layout gui/data_logger.json). 1.12 switched between a graph page and a config page; here both fit
 * on one screen: the graph (newest sample at the right, in the logger's ink colour), reset/pause/print, the sampling
 * period and Y range, and a button cycling the unit.
 */
public class ElectricalDataLoggerScreen extends ElnScreen {
    final ElectricalDataLoggerRender render;
    Button print;

    public ElectricalDataLoggerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalDataLoggerRender) render;
    }

    /** Channel colours after the first (which takes the logger's ink): blue, green, white (board #45). */
    static final int[] CHANNEL_COLOURS = {0, 0xFF5A9CFF, 0xFF5AD85A, 0xFFE8E8E8};
    static final int GRID = 0xFF3A3A3A, AXIS_TEXT = 0xFFA0A0A0;

    int colour(int c) {
        if (c == 0 || c >= CHANNEL_COLOURS.length)
            return 0xFF000000 | (int) (render.descriptor.cr * 255) << 16 | (int) (render.descriptor.cg * 255) << 8 | (int) (render.descriptor.cb * 255);
        return CHANNEL_COLOURS[c];
    }

    /**
     * The graph: per channel its range over each period as a faint band and its mean as a line, newest at the right;
     * an axis per scale (the first on the left, the others on the right, one tick column each) with nice ticks and a
     * grid for the first; the time span below.
     */
    void graph(net.minecraft.client.gui.GuiGraphicsExtractor g, mods.eln.gui.layout.Layout.Rect r) {
        DataLogs log = render.log;
        var f = font;
        g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), 0xFF262626);
        var axes = LogAxes.layout(log.channelList());
        int gy = r.y() + 5, gh = r.h() - 5 - 12;
        int ticksMax = Math.max(2, gh / 12 + 1);
        // tick columns: labels and widths per axis
        String[][] labels = new String[axes.size()][];
        double[][] ticks = new double[axes.size()][];
        int[] widths = new int[axes.size()];
        for (int i = 0; i < axes.size(); i++) {
            LogChannel s = axes.get(i).scale();
            ticks[i] = LogAxes.ticks(s.min, s.max, ticksMax);
            labels[i] = new String[ticks[i].length];
            for (int t = 0; t < ticks[i].length; t++) {
                labels[i][t] = DataLogs.format(ticks[i][t], s.unit).trim();
                widths[i] = Math.max(widths[i], f.width(labels[i][t]));
            }
        }
        int left = axes.isEmpty() ? 2 : widths[0] + 4, right = 2;
        int[] colX = new int[axes.size()];
        for (int i = 1; i < axes.size(); i++) {
            colX[i] = right;
            right += widths[i] + 4;
        }
        int gx = r.x() + left, gw = r.w() - left - right;
        // axes: grid lines for the first, tick labels for all
        for (int i = 0; i < axes.size(); i++) {
            LogChannel s = axes.get(i).scale();
            int textColour = axes.size() == 1 ? AXIS_TEXT : colour(axes.get(i).channels().get(0));
            for (int t = 0; t < ticks[i].length; t++) {
                float frac = s.max == s.min ? 0 : (float) ((ticks[i][t] - s.min) / (s.max - s.min));
                int y = gy + gh - Math.round(frac * gh);
                if (i == 0) g.fill(gx, y, gx + gw, y + 1, ticks[i][t] == 0 ? 0xFF505050 : GRID);
                int tx = i == 0 ? gx - 2 - f.width(labels[i][t]) : gx + gw + colX[i] + 2;
                g.text(f, labels[i][t], tx, Math.max(r.y() + 1, Math.min(gy + gh - 4, y - 4)), textColour, false);
            }
        }
        g.fill(gx, gy + gh, gx + gw, gy + gh + 1, 0xFF606060);
        int n = log.size();
        if (n >= 1 && gw > 1) {
            for (int c = 0; c < log.channelCount(); c++) {
                int ink = colour(c), band = ink & 0x00FFFFFF | 0x50000000;
                int prevY = Integer.MIN_VALUE;
                for (int px = 0; px < gw; px++) {
                    // the points this column covers (age 0 is the newest, at the right edge)
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
                    if (cnt == 0) {
                        prevY = Integer.MIN_VALUE;
                        continue;
                    }
                    int yHi = gy + gh - Math.round(clamp(hi) * gh), yLo = gy + gh - Math.round(clamp(lo) * gh);
                    if (yLo > yHi) g.fill(gx + px, yHi, gx + px + 1, yLo + 1, band);
                    int y = gy + gh - Math.round(clamp(sum / cnt) * gh);
                    int y0 = prevY == Integer.MIN_VALUE ? y : Math.min(prevY, y), y1 = prevY == Integer.MIN_VALUE ? y : Math.max(prevY, y);
                    g.fill(gx + px, y0, gx + px + 1, y1 + 1, ink);
                    prevY = y;
                }
            }
        }
        String span = "-" + Utils.plotTime(n * log.samplingPeriod).trim(), now = Utils.plotTime(0).trim();
        g.text(f, span, gx, gy + gh + 3, AXIS_TEXT, false);
        g.text(f, now, gx + gw - f.width(now), gy + gh + 3, AXIS_TEXT, false);
    }

    static float clamp(float v) {
        return Math.max(0, Math.min(1, v));
    }

    @Override
    protected void init() {
        super.init();
        canvas("graph", this::graph);
        button("reset", () -> tr("Reset"), () -> render.clientSend(ElectricalDataLoggerElement.resetId));
        button("pause", () -> render.pause ? "§6" + tr("Paused") : "§a" + tr("Running"),
            () -> render.clientSend(ElectricalDataLoggerElement.tooglePauseId));
        print = button("print", () -> tr("Print"), () -> render.clientSend(ElectricalDataLoggerElement.printId),
            tr("Prints the graph on the paper"));
        text("periodl", () -> tr("Period"));
        text("maxl", () -> tr("Max"));
        text("minl", () -> tr("Min"));
        field("period", () -> num(render.log.samplingPeriod), v -> {
            float p = EaField.parse(v);
            if (!Float.isNaN(p)) render.clientSetFloat(ElectricalDataLoggerElement.setSamplingPeriodeId, Math.max(0.05f, p));
        }, tr("Sampling period"), tr("in seconds, at least 0.05; changing it keeps the history"));
        field("max", () -> num(render.log.channels[0].max), v -> {
            float p = EaField.parse(v);
            if (!Float.isNaN(p)) render.sendChannel(ElectricalDataLoggerElement.setMaxValue, 0, p);
        }, tr("Y-axis max"), tr("what a full signal reads as"));
        field("min", () -> num(render.log.channels[0].min), v -> {
            float p = EaField.parse(v);
            if (!Float.isNaN(p)) render.sendChannel(ElectricalDataLoggerElement.setMinValue, 0, p);
        }, tr("Y-axis min"), tr("what a zero signal reads as"));
        text("unitl", () -> tr("Unit"));
        field("unit", () -> render.log.channels[0].unit, v -> render.sendUnit(0, v), tr("Unit"), tr("any text, up to 10 characters"));
        update();
    }

    @Override
    protected void update() {
        print.active = !deviceItem(ElectricalDataLoggerContainer.paperSlotId).isEmpty()
            && deviceItem(ElectricalDataLoggerContainer.printSlotId).isEmpty();
    }
}
