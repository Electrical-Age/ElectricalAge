package mods.eln.sixnode.wirelesssignal.source;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Wireless signal source (P4b, 1.12 WirelessSignalSourceGui): the channel name. */
public class WirelessSignalSourceScreen extends ElnScreen {
    final WirelessSignalSourceRender render;

    public WirelessSignalSourceScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, 200, 46);
        this.render = (WirelessSignalSourceRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field(8, 20, 184, () -> render.channel, v -> render.clientSetString(WirelessSignalSourceElement.setChannelId, v),
            tr("Specify the channel"));
    }
}
