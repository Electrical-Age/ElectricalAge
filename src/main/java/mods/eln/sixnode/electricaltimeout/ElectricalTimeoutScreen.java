package mods.eln.sixnode.electricaltimeout;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electrical timeout (P4b; layout gui/electrical_timeout.json): the hold time, set (trigger) and reset. */
public class ElectricalTimeoutScreen extends ElnScreen {
    final ElectricalTimeoutRender render;

    public ElectricalTimeoutScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalTimeoutRender) render;
    }

    @Override
    protected void init() {
        super.init();
        text("hold", () -> tr("Hold time"));
        text("unit", () -> "s");
        field("time", () -> num(render.timeoutValue), v -> {
            float f = EaField.parse(v);
            if (!Float.isNaN(f)) render.clientSetFloat(ElectricalTimeoutElement.setTimeOutValueId, f);
        }, tr("The time interval the output is kept high."));
        button("set", () -> tr("Set"), () -> render.clientSend(ElectricalTimeoutElement.setId), tr("Start the timer: output high"));
        button("reset", () -> tr("Reset"), () -> render.clientSend(ElectricalTimeoutElement.resetId), tr("Output low now"));
    }
}
