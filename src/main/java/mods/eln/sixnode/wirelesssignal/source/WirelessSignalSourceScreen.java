package mods.eln.sixnode.wirelesssignal.source;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Wireless signal source (P4b; layout gui/wireless_source.json): the channel. */
public class WirelessSignalSourceScreen extends ElnScreen {
    final WirelessSignalSourceRender render;

    public WirelessSignalSourceScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (WirelessSignalSourceRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field("channel", () -> render.channel, v -> render.clientSetString(WirelessSignalSourceElement.setChannelId, v),
            tr("Specify the channel"));
    }
}
