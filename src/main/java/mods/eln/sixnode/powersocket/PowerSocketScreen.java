package mods.eln.sixnode.powersocket;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Power socket (P4b; layout gui/power_socket.json): the supply channel this socket feeds from. */
public class PowerSocketScreen extends ElnScreen {
    final PowerSocketRender render;

    public PowerSocketScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (PowerSocketRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field("channel", () -> render.channel, v -> render.clientSetString(PowerSocketElement.setChannelId, v),
            tr("Specify the device to supply through this socket."));
    }
}
