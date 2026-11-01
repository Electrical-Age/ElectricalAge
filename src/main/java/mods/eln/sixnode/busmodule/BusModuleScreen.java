package mods.eln.sixnode.busmodule;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * The Bus I/O Module's screen (layout gui/bus_module.json): per terminal, its channel name and a button for its
 * direction; below, whether it is on a bus, or why the last name was refused.
 */
public class BusModuleScreen extends ElnScreen {
    final BusModuleRender render;

    public BusModuleScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (BusModuleRender) render;
    }

    @Override
    protected void init() {
        super.init();
        for (int t = 0; t < BusModuleElement.TERMINALS; t++) {
            final int i = t;
            text("l" + i, () -> String.valueOf(i + 1));
            field("n" + i, () -> render.names[i], v -> render.sendName(i, v),
                tr("The channel of terminal %1$: a-z, 0-9 and _, up to 16. Empty: unused.", i + 1));
            button("d" + i, () -> render.out[i] ? tr("Out") : tr("In"), () -> render.sendOut(i, !render.out[i]),
                tr("In: the wire's level goes onto the bus."), tr("Out: the bus drives the wire."));
        }
        text("status", () -> !render.message.isEmpty() ? render.message
                : render.busState == 2 ? tr("Bus full") : render.busState == 1 ? tr("On a bus") : tr("No bus"),
            () -> !render.message.isEmpty() || render.busState == 2 ? 0xFFA02020 : render.busState == 1 ? 0xFF207020 : LABEL);
    }
}
