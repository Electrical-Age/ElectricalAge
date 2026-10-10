package mods.eln.sixnode.electricaltimeout;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electrical timeout (P4b, 1.12 ElectricalTimeoutGui): the hold time, set (trigger) and reset. */
public class ElectricalTimeoutScreen extends ElnScreen {
    final ElectricalTimeoutRender render;

    public ElectricalTimeoutScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, 176, 74);
        this.render = (ElectricalTimeoutRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field(70, 20, 60, () -> num(render.timeoutValue), v -> {
            float f = EaField.parse(v);
            if (!Float.isNaN(f)) render.clientSetFloat(ElectricalTimeoutElement.setTimeOutValueId, f);
        }, tr("The time interval the output is kept high."));
        button(8, 46, 78, () -> tr("Set"), () -> render.clientSend(ElectricalTimeoutElement.setId), tr("Start the timer: output high"));
        button(90, 46, 78, () -> tr("Reset"), () -> render.clientSend(ElectricalTimeoutElement.resetId), tr("Output low now"));
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        label(g, 8, 25, tr("Hold time"));
        label(g, 134, 25, "s");
    }
}
