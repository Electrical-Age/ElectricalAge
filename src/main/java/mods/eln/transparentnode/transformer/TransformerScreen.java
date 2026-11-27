package mods.eln.transparentnode.transformer;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.api.Meters.volts;
import static mods.eln.i18n.I18N.tr;

/** DC-DC converter / transformer (P4b; layout gui/transformer.json): windings, core, casing; network isolation. */
public class TransformerScreen extends ElnScreen {
    final TransformerRender render;

    public TransformerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (TransformerRender) render;
    }

    /** The windings' slots in the model's colours (Baughn 2026-10-27: primary green, secondary yellow, as its label and
     *  the 1.12 screen's marks). */
    @Override
    protected int slotFrame(int index) {
        if (index == TransformerContainer.primaryCableSlotId) return 0xFF09B700;
        if (index == TransformerContainer.secondaryCableSlotId) return 0xFFEDEA00;
        return 0;
    }

    @Override
    protected void init() {
        super.init();
        text("pl", () -> tr("Primary"));
        text("cl", () -> tr("Core"));
        text("sl", () -> tr("Secondary"));
        text("casl", () -> tr("Casing"));
        TransformerRender r = render;
        button("on", () -> r.switchedOn ? tr("Switched on") : tr("Switched off"), () -> r.clientSendId(TransformerElement.toggleSwitchedOn),
            tr("Connects windings to terminals (resets off on changes)"));
        text("onl", () -> "");
        text("ratio", () -> r.primaryTurns <= 0 || r.secondaryTurns <= 0 ? tr("Needs windings and a core")
            : tr("Turns %1$ : %2$ (x%3$)", r.primaryTurns, r.secondaryTurns, String.format("%.2f", (double) r.secondaryTurns / r.primaryTurns))
                + (r.coreVoltsPerTurn > 0 ? tr(", core %1$/turn", volts(r.coreVoltsPerTurn)) : tr(", no core")));
        text("expect", () -> expectText(r), () -> ratioOff(r) > 0.05 ? ERROR_TEXT : LABEL);
        text("vpt", () -> {
            double v = voltsPerTurn(r);
            if (r.coreVoltsPerTurn <= 0 || v <= 0) return "";
            return tr("Volts/turn: %1$ / %2$", volts(v), volts(r.coreVoltsPerTurn))
                + (r.saturated ? tr(" (saturated!)") : "");
        }, () -> r.saturated || voltsPerTurn(r) > 1.1 * r.coreVoltsPerTurn && r.coreVoltsPerTurn > 0 ? ERROR_TEXT : LABEL);
        text("window", () -> r.window <= 0 ? "" : tr("Window: %1$ / %2$ mm² turns", String.format("%.0f", r.windowUsed), String.format("%.0f", r.window))
            + (r.windowUsed > r.window ? tr(" (full)") : ""), () -> r.windowUsed > r.window ? ERROR_TEXT : LABEL);
        button("isolator", () -> render.isIsolator ? tr("Isolated") : tr("Not isolated"),
            () -> render.clientSendId(TransformerElement.toogleIsIsolator),
            tr("Simulates both sides separately (faster switching, slight capacitance)"));
    }

    static final double LIVE = 1;

    /** The volts a turn the core sees now, from whichever side is live. */
    static double voltsPerTurn(TransformerRender r) {
        if (r.primaryTurns <= 0 || r.secondaryTurns <= 0) return 0;
        return Math.max(Math.abs(r.primaryU) / r.primaryTurns, Math.abs(r.secondaryU) / r.secondaryTurns);
    }

    /** With both sides live and it switched off: how far the turns ratio is from the voltages' ratio (0.03: 3 %); else 0. */
    static double ratioOff(TransformerRender r) {
        if (r.switchedOn || r.primaryTurns <= 0 || r.secondaryTurns <= 0) return 0;
        if (Math.abs(r.primaryU) < LIVE || Math.abs(r.secondaryU) < LIVE) return 0;
        double want = Math.abs(r.secondaryU / r.primaryU), have = (double) r.secondaryTurns / r.primaryTurns;
        return Math.abs(have / want - 1);
    }

    static String expectText(TransformerRender r) {
        if (r.primaryTurns <= 0 || r.secondaryTurns <= 0) return "";
        double k = (double) r.secondaryTurns / r.primaryTurns, up = Math.abs(r.primaryU), us = Math.abs(r.secondaryU);
        if (r.switchedOn) return tr("Primary %1$, secondary %2$", volts(up), volts(us));
        if (up >= LIVE && us >= LIVE) {
            double off = (double) r.secondaryTurns / r.primaryTurns / (us / up) - 1;
            return tr("Both sides live (%1$, %2$): ratio %3$% off", volts(up), volts(us), String.format("%+.1f", off * 100));
        }
        if (up >= LIVE) return tr("Off: %1$ primary -> %2$ secondary", volts(up), volts(up * k));
        if (us >= LIVE) return tr("Off: %1$ secondary -> %2$ primary", volts(us), volts(us / k));
        return tr("Off: neither side live");
    }
}
