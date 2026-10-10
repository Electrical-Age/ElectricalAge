package mods.eln.sixnode.electricalbreaker;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaGauge;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electrical breaker (P4b, 1.12 ElectricalBreakerGui): cut-off voltages and the switch. */
public class ElectricalBreakerScreen extends ElnScreen {
    final ElectricalBreakerRender render;

    public ElectricalBreakerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalBreakerRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field(30, 10, 50, () -> num(render.uMax), v -> {
            float f = EaField.parse(v);
            if (!Float.isNaN(f)) render.clientSetVoltageMax(f);
        }, tr("Maximum voltage before cutting off"));
        field(30, 32, 50, () -> num(render.uMin), v -> {
            float f = EaField.parse(v);
            if (!Float.isNaN(f)) render.clientSetVoltageMin(f);
        }, tr("Minimum voltage before cutting off"));
        button(88, 20, 56, () -> render.switchState ? tr("On") : tr("Off"), render::clientToogleSwitch, tr("Toggle switch"));
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        label(g, 8, 15, "max");
        label(g, 8, 37, "min");
        label(g, 82, 15, "V");
        label(g, 82, 37, "V");
    }
}
