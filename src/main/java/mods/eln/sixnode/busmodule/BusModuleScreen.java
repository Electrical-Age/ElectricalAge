package mods.eln.sixnode.busmodule;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * The Bus I/O Module's screen (layout gui/bus_module.json): per terminal, its number, its channel as a swatch and a
 * button with the channel's number and name that steps through the colours, and a button for its direction; below,
 * whether it is on a bus.
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
            text("l" + i, () -> "T" + (i + 1));   // (AdventurAgent: "1" beside channel "14" read as one number)
            busChannel("s" + i, "c" + i, () -> render.channels[i], c -> render.sendChannel(i, c),
                tr("Terminal %1$ bus channel", i + 1));
            button("d" + i, () -> render.out[i] ? tr("Out") : tr("In"), () -> render.sendOut(i, !render.out[i]),
                tr("In: wire -> bus"), tr("Out: bus -> wire"));
        }
        text("status", () -> render.onBus ? tr("On a bus") : tr("No bus"), () -> render.onBus ? 0xFF207020 : LABEL);
    }
}
