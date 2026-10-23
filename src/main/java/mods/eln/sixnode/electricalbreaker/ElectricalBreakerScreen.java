package mods.eln.sixnode.electricalbreaker;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electrical breaker (P4b; layout gui/electrical_breaker.json): cut-off voltages and the switch. */
public class ElectricalBreakerScreen extends ElnScreen {
    final ElectricalBreakerRender render;

    public ElectricalBreakerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalBreakerRender) render;
    }

    @Override
    protected void init() {
        super.init();
        text("maxl", () -> "max");
        text("minl", () -> "min");
        text("maxu", () -> "V");
        text("minu", () -> "V");
        field("max", () -> num(render.uMax), v -> {
            float f = EaField.parse(v);
            if (!Float.isNaN(f)) render.clientSetVoltageMax(f);
        }, tr("Maximum voltage before cutting off"));
        field("min", () -> num(render.uMin), v -> {
            float f = EaField.parse(v);
            if (!Float.isNaN(f)) render.clientSetVoltageMin(f);
        }, tr("Minimum voltage before cutting off"));
        button("switch", () -> render.switchState ? tr("On") : tr("Off"), render::clientToogleSwitch,
            tr("Toggle switch (it cuts off again while the voltage is outside min..max)"));
    }
}
