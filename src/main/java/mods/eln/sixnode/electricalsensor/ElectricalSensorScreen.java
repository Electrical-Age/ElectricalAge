package mods.eln.sixnode.electricalsensor;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electrical probe (P4b; layouts gui/electrical_sensor.json, voltage_probe.json): what it measures and the range. */
public class ElectricalSensorScreen extends ElnScreen {
    final ElectricalSensorRender render;
    EaField low, high;
    final java.util.List<Runnable> enabling = new java.util.ArrayList<>();

    public ElectricalSensorScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalSensorRender) render;
    }

    @Override
    protected void init() {
        super.init();
        enabling.clear();
        boolean v = render.descriptor.voltageOnly;
        text("hl", () -> "100%");
        text("ll", () -> "0%");
        high = field("high", () -> num(render.highValue), s -> validate(),
            (v ? tr("Measured voltage\ncorresponding\nto 100% output") : tr("Measured value\ncorresponding\nto 100% output")).split("\n"));
        low = field("low", () -> num(render.lowValue), s -> validate(),
            (v ? tr("Measured voltage\ncorresponding\nto 0% output") : tr("Measured value\ncorresponding\nto 0% output")).split("\n"));
        button("validate", () -> tr("Validate"), this::validate);
        if (!v) {
            type("voltage", tr("Voltage"), ElectricalSensorElement.voltageType);
            type("current", tr("Current"), ElectricalSensorElement.currantType);
            type("power", tr("Power"), ElectricalSensorElement.powerType);
            button("dir", () -> switch (render.dirType) {
                case ElectricalSensorElement.dirAB -> "§2●§r => §1●";
                case ElectricalSensorElement.dirBA -> "§2●§r <= §1●";
                default -> "§2●§r <=> §1●";
            }, () -> {
                render.dirType = (byte) ((render.dirType + 1) % 3);
                render.clientSetByte(ElectricalSensorElement.setDirType, render.dirType);
            }, tr("Current direction measured"));
        }
        update();
    }

    void type(String id, String label, byte type) {
        var b = button(id, () -> label, () -> render.clientSetByte(ElectricalSensorElement.setTypeOfSensorId, type));
        enabling.add(() -> b.active = render.typeOfSensor != type);
    }

    void validate() {
        float l = EaField.parse(low.getValue()), h = EaField.parse(high.getValue());
        if (!Float.isNaN(l) && !Float.isNaN(h)) render.clientSetFloat(ElectricalSensorElement.setValueId, l, h);
    }

    @Override
    protected void update() {
        for (Runnable r : enabling) r.run();
    }
}
