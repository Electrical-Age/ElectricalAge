package mods.eln.sixnode.wirelesssignal.tx;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Wireless signal tx (P4b, 1.12 WirelessSignalTxGui): the channel name. */
public class WirelessSignalTxScreen extends ElnScreen {
    final WirelessSignalTxRender render;

    public WirelessSignalTxScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, 200, 46);
        this.render = (WirelessSignalTxRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field(8, 20, 184, () -> render.channel, v -> render.clientSetString(WirelessSignalTxElement.setChannelId, v),
            tr("Specify the channel"));
    }
}
