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

    int colour(int c) {
        if (c == 0 || c >= CHANNEL_COLOURS.length)
            return 0xFF000000 | (int) (render.descriptor.cr * 255) << 16 | (int) (render.descriptor.cg * 255) << 8 | (int) (render.descriptor.cb * 255);
        return CHANNEL_COLOURS[c];
    }

    void graph(net.minecraft.client.gui.GuiGraphicsExtractor g, mods.eln.gui.layout.Layout.Rect r) {
        LogGraph.draw(g, font, r.x(), r.y(), r.w(), r.h(), render.log, this::colour, false);
    }

    @Override
    protected void init() {
        super.init();
        canvas("graph", this::graph);
        button("reset", () -> tr("Reset"), () -> render.logClient.reset());
        button("pause", () -> render.pause ? "§6" + tr("Paused") : "§a" + tr("Running"),
            () -> render.clientSend(ElectricalDataLoggerElement.tooglePauseId));
        print = button("print", () -> tr("Print"), () -> render.clientSend(ElectricalDataLoggerElement.printId),
            tr("Prints the graph on the paper"));
        text("periodl", () -> tr("Period"));
        text("maxl", () -> tr("Max"));
        text("minl", () -> tr("Min"));
        field("period", () -> num(render.log.samplingPeriod), v -> {
            float p = EaField.parse(v);
            if (!Float.isNaN(p)) render.logClient.period(Math.max(0.05f, p));
        }, tr("Sampling period"), tr("in seconds, at least 0.05; changing it keeps the history"));
        field("max", () -> num(render.log.channels[0].max), v -> {
            float p = EaField.parse(v);
            if (!Float.isNaN(p)) render.logClient.max(0, p);
        }, tr("Y-axis max"), tr("what a full signal reads as"));
        field("min", () -> num(render.log.channels[0].min), v -> {
            float p = EaField.parse(v);
            if (!Float.isNaN(p)) render.logClient.min(0, p);
        }, tr("Y-axis min"), tr("what a zero signal reads as"));
        text("unitl", () -> tr("Unit"));
        field("unit", () -> render.log.channels[0].unit, v -> render.logClient.unit(0, v), tr("Unit"), tr("any text, up to 10 characters"));
        update();
    }

    @Override
    protected void update() {
        render.watch();
        print.active = !deviceItem(ElectricalDataLoggerContainer.paperSlotId).isEmpty()
            && deviceItem(ElectricalDataLoggerContainer.printSlotId).isEmpty();
    }
}
