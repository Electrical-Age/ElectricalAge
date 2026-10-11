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

    // DataLogs unit order: voltage, current, power, celsius, percent, energy (do not translate, as 1.12)
    static final String[] UNITS = {"Voltage [V]", "Current [A]", "Power [W]", "Temp. [*C]", "Percent [-]%", "Energy [J]"};

    @Override
    protected void init() {
        super.init();
        DataLogs log = render.log;
        canvas("graph", (g, r) -> {
            g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), 0xFF262626);
            int ink = 0xFF000000 | (int) (render.descriptor.cr * 255) << 16 | (int) (render.descriptor.cg * 255) << 8
                | (int) (render.descriptor.cb * 255);
            int gx = r.x() + 2, gy = r.y() + 2, gw = r.w() - 4, gh = r.h() - 4 - 10;
            // zero line when the range crosses it
            if (log.minValue < 0 && log.maxValue > 0 || log.minValue > 0 && log.maxValue < 0) {
                int zy = gy + Math.round(log.maxValue / (log.maxValue - log.minValue) * gh);
                g.fill(gx, zy, gx + gw, zy + 1, 0xFF505050);
            }
            int n = log.size();
            if (n >= 2) {
                int prevY = -1;
                for (int px = 0; px < gw; px++) {
                    // sample 0 is the newest, at the right edge
                    int idx = Math.round((gw - 1 - px) / (float) (gw - 1) * (n - 1));
                    int y = gy + gh - Math.round(((int) log.read(idx) + 128) / 255f * gh);
                    int y0 = prevY < 0 ? y : Math.min(prevY, y), y1 = prevY < 0 ? y : Math.max(prevY, y);
                    g.fill(gx + px, y0, gx + px + 1, y1 + 1, ink);
                    prevY = y;
                }
            }
            var f = font;
            String hi = DataLogs.getYstring(1f, log.maxValue, log.minValue, log.unitType).trim();
            String lo = DataLogs.getYstring(0f, log.maxValue, log.minValue, log.unitType).trim();
            g.text(f, hi, gx + gw - f.width(hi), gy, 0xFFA0A0A0, false);
            g.text(f, lo, gx + gw - f.width(lo), gy + gh - 8, 0xFFA0A0A0, false);
            String span = "-" + Utils.plotTime(n * log.samplingPeriod).trim(), now = Utils.plotTime(0).trim();
            g.text(f, span, gx, gy + gh + 2, 0xFFA0A0A0, false);
            g.text(f, now, gx + gw - f.width(now), gy + gh + 2, 0xFFA0A0A0, false);
        });
        button("reset", () -> tr("Reset"), () -> render.clientSend(ElectricalDataLoggerElement.resetId));
        button("pause", () -> render.pause ? "§6" + tr("Paused") : "§a" + tr("Running"),
            () -> render.clientSend(ElectricalDataLoggerElement.tooglePauseId));
        print = button("print", () -> tr("Print"), () -> render.clientSend(ElectricalDataLoggerElement.printId),
            tr("Prints the graph on the paper"));
        text("periodl", () -> tr("Period"));
        text("maxl", () -> tr("Max"));
        text("minl", () -> tr("Min"));
        field("period", () -> num(log.samplingPeriod), v -> {
            float p = EaField.parse(v);
            if (!Float.isNaN(p)) render.clientSetFloat(ElectricalDataLoggerElement.setSamplingPeriodeId, Math.max(0.05f, p));
        }, tr("Sampling period"), tr("in seconds, at least 0.05"));
        field("max", () -> num(log.maxValue), v -> {
            float p = EaField.parse(v);
            if (!Float.isNaN(p)) render.clientSetFloat(ElectricalDataLoggerElement.setMaxValue, p);
        }, tr("Y-axis max"));
        field("min", () -> num(log.minValue), v -> {
            float p = EaField.parse(v);
            if (!Float.isNaN(p)) render.clientSetFloat(ElectricalDataLoggerElement.setMinValue, p);
        }, tr("Y-axis min"));
        button("unit", () -> tr("Unit: %1$", log.unitType >= 0 && log.unitType < 6 ? tr(UNITS[log.unitType]) : "?"),
            () -> render.clientSetByte(ElectricalDataLoggerElement.setUnitId, (byte) ((log.unitType + 1) % 6)), tr("Click to change"));
        update();
    }

    @Override
    protected void update() {
        print.active = !deviceItem(ElectricalDataLoggerContainer.paperSlotId).isEmpty()
            && deviceItem(ElectricalDataLoggerContainer.printSlotId).isEmpty();
    }
}
