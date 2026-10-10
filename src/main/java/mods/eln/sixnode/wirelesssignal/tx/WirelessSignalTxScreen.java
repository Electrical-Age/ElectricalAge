package mods.eln.sixnode.wirelesssignal.tx;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Wireless signal tx (P4b; layout gui/wireless_tx.json): the channel. */
public class WirelessSignalTxScreen extends ElnScreen {
    final WirelessSignalTxRender render;

    public WirelessSignalTxScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (WirelessSignalTxRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field("channel", () -> render.channel, v -> render.clientSetString(WirelessSignalTxElement.setChannelId, v),
            tr("Specify the channel"));
    }
}
