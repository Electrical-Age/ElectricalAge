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
