package mods.eln.sixnode.electricalalarm;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electrical alarm (P4b; layout gui/electrical_alarm.json): mute. */
public class ElectricalAlarmScreen extends ElnScreen {
    final ElectricalAlarmRender render;

    public ElectricalAlarmScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalAlarmRender) render;
    }

    @Override
    protected void init() {
        super.init();
        button("mute", () -> render.mute ? tr("Sound: muted") : tr("Sound: on"), () -> render.clientSend(ElectricalAlarmElement.clientSoundToggle));
    }
}
