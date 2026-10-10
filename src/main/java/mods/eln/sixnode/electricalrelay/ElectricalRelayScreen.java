package mods.eln.sixnode.electricalrelay;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electrical relay (P4b; layout gui/electrical_relay.json): normally open or closed. */
public class ElectricalRelayScreen extends ElnScreen {
    final ElectricalRelayRender render;

    public ElectricalRelayScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalRelayRender) render;
    }

    @Override
    protected void init() {
        super.init();
        button("default", () -> render.defaultOutput ? tr("Normally closed") : tr("Normally open"), render::clientToogleDefaultOutput,
            tr("The contact's state when the control signal is low"));
    }
}
