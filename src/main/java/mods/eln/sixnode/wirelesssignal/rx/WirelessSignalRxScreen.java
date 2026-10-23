package mods.eln.sixnode.wirelesssignal.rx;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Wireless signal receiver (P4b; layout gui/wireless_rx.json): channel, connection, how emitters combine. */
public class WirelessSignalRxScreen extends ElnScreen {
    final WirelessSignalRxRender render;

    public WirelessSignalRxScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (WirelessSignalRxRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field("channel", () -> render.channel, v -> render.clientSetString(WirelessSignalRxElement.setChannelId, v),
            tr("Specify the channel"));
        text("connection", () -> render.connection ? tr("Connected") : tr("Not connected"), () -> render.connection ? 0xFF207020 : 0xFFA02020);
        String[] names = {tr("Biggest"), tr("Smallest"), tr("Toggle")};
        String[] tips = {tr("Uses the biggest value on the channel."), tr("Uses the smallest value on the channel."),
            tr("Toggles the output each time an emitter's value rises. Useful to allow multiple buttons to control the same light.")};
        for (int i = 0; i < 3; i++) {
            final byte id = (byte) i;
            button(String.valueOf(i), () -> (render.selectedAggregator == id ? "> " : "") + names[id],
                () -> render.clientSetByte(WirelessSignalRxElement.setSelectedAggregator, id), tips[i]);
        }
    }
}
