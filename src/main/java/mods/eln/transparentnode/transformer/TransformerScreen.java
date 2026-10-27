package mods.eln.transparentnode.transformer;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

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
        button("isolator", () -> render.isIsolator ? tr("Isolated") : tr("Not isolated"),
            () -> render.clientSendId(TransformerElement.toogleIsIsolator),
            tr("Isolated: the networks on both sides are simulated apart. Faster for circuits that switch very often, at the cost of a little capacitance."));
    }
}
