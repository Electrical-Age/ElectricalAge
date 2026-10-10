package mods.eln.sixnode.electricalalarm;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electrical alarm (P4b, 1.12 ElectricalAlarmGui): mute. */
public class ElectricalAlarmScreen extends ElnScreen {
    final ElectricalAlarmRender render;

    public ElectricalAlarmScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, 160, 48);
        this.render = (ElectricalAlarmRender) render;
    }

    @Override
    protected void init() {
        super.init();
        button(8, 20, 144, () -> render.mute ? tr("Sound: muted") : tr("Sound: on"),
            () -> render.clientSend(ElectricalAlarmElement.clientSoundToggle));
    }
}
