package mods.eln.sixnode.thermalsensor;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Thermal probe (P4b; layouts gui/thermal_sensor.json, temperature_probe.json): what it measures and the range. */
public class ThermalSensorScreen extends ElnScreen {
    final ThermalSensorRender render;
    EaField low, high;
    final java.util.List<Runnable> enabling = new java.util.ArrayList<>();

    public ThermalSensorScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ThermalSensorRender) render;
    }

    @Override
    protected void init() {
        super.init();
        enabling.clear();
        boolean t = render.descriptor.temperatureOnly;
        text("hl", () -> "100%");
        text("ll", () -> "0%");
        high = field("high", () -> num(render.highValue), s -> validate(),
            (t ? tr("Measured temperature\ncorresponding\nto 100% output") : tr("Measured value\ncorresponding\nto 100% output")).split("\n"));
        low = field("low", () -> num(render.lowValue), s -> validate(),
            (t ? tr("Measured temperature\ncorresponding\nto 0% output") : tr("Measured value\ncorresponding\nto 0% output")).split("\n"));
        button("validate", () -> tr("Validate"), this::validate);
        if (!t) {
            type("power", tr("Power"), ThermalSensorElement.powerType);
            type("temperature", tr("Temperature"), ThermalSensorElement.temperatureType);
        }
        update();
    }

    void type(String id, String label, byte type) {
        var b = button(id, () -> label, () -> render.clientSetByte(ThermalSensorElement.setTypeOfSensorId, type));
        enabling.add(() -> b.active = render.typeOfSensor != type);
    }

    /** The render shows the values plus ambient (1.12); they go back without it. */
    void validate() {
        float l = EaField.parse(low.getValue()), h = EaField.parse(high.getValue());
        if (!Float.isNaN(l) && !Float.isNaN(h))
            render.clientSetFloat(ThermalSensorElement.setValueId, l - (float) PhysicalConstant.Tamb, h - (float) PhysicalConstant.Tamb);
    }

    @Override
    protected void update() {
        for (Runnable r : enabling) r.run();
    }
}
