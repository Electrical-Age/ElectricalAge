package mods.eln.sixnode.wirelesssignal.rx;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Wireless signal receiver (P4b, 1.12 WirelessSignalRxGui): channel, connection state, how emitters combine. */
public class WirelessSignalRxScreen extends ElnScreen {
    final WirelessSignalRxRender render;

    public WirelessSignalRxScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, 232, 72);
        this.render = (WirelessSignalRxRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field(8, 20, 216, () -> render.channel, v -> render.clientSetString(WirelessSignalRxElement.setChannelId, v),
            tr("Specify the channel"));
        String[] names = {tr("Biggest"), tr("Smallest"), tr("Toggle")};
        String[] tips = {tr("Uses the biggest value on the channel."), tr("Uses the smallest value on the channel."),
            tr("Toggles the output each time an emitter's value rises. Useful to allow multiple buttons to control the same light.")};
        for (int i = 0; i < 3; i++) {
            final byte id = (byte) i;
            var b = button(8 + i * 73, 44, 70, () -> (render.selectedAggregator == id ? "> " : "") + names[id],
                () -> render.clientSetByte(WirelessSignalRxElement.setSelectedAggregator, id), tips[i]);
        }
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        String c = render.connection ? tr("Connected") : tr("Not connected");
        g.text(font, c, leftPos + imageWidth - 8 - font.width(c), topPos + 6, render.connection ? 0xFF207020 : 0xFFA02020, false);
    }
}
