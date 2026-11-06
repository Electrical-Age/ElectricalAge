package mods.eln.sixnode;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * Fuse holder (layout gui/fuse_holder.json; Baughn 2026-11-06): the fuse's rating in amps, set here (one fuse item for
 * every cable), and taking the fuse out. A fuse goes in by right-clicking the holder with it.
 */
public class ElectricalFuseHolderScreen extends ElnScreen {
    final ElectricalFuseHolderRender render;

    public ElectricalFuseHolderScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalFuseHolderRender) render;
    }

    @Override
    protected void init() {
        super.init();
        text("ratingl", () -> tr("rating"));
        text("ratingu", () -> "A");
        field("rating", () -> num(render.getRating()), v -> {
            float f = EaField.parse(v);
            if (!Float.isNaN(f)) render.clientSetFloat(ElectricalFuseHolderElement.SET_RATING, f);
        }, tr("It blows at about 1.08 times this"));
        text("state", () -> render.getInstalledFuse() == null ? tr("No fuse: right-click the holder with one")
            : render.getInstalledFuse().getCableDescriptor() == null ? tr("The fuse has blown") : tr("Fuse in"));
        button("out", () -> tr("Take the fuse out"), () -> render.clientSend(ElectricalFuseHolderElement.TAKE_OUT),
            tr("Drops the fuse (a blown one too)"));
    }
}
