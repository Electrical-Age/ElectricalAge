package mods.eln.transparentnode;

import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * Fuel heat furnace (P4b; layout gui/fuel_heat_furnace.json): control mode, main switch (needs a burner), and either
 * the manual control value (no regulator) or the temperature set point (with one).
 */
public class FuelHeatFurnaceScreen extends ElnScreen {
    final FuelHeatFurnaceRender render;
    EaSlider manual, temp;
    net.minecraft.client.gui.components.Button main;

    public FuelHeatFurnaceScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (FuelHeatFurnaceRender) render;
    }

    static final FuelHeatFurnaceElement.Companion E = FuelHeatFurnaceElement.Companion;
    static final FuelHeatFurnaceContainer.Companion C = FuelHeatFurnaceContainer.Companion;

    @Override
    protected void init() {
        super.init();
        button("external", () -> render.getExternalControlled() ? tr("External control") : tr("Internal control"),
            () -> render.clientSendId(E.getExternalControlledToggleEvent()));
        main = button("main", () -> render.getMainSwitch() ? tr("Furnace is on") : tr("Furnace is off"),
            () -> render.clientSendId(E.getMainSwitchToggleEvent()));
        manual = slider("manual", 0, 1, 0.01, render.getManualControl().getValue(),
            v -> Component.literal(tr("Control value at %1$", Utils.plotPercent("", v)) + "  ("
                + tr("Heat Power: %1$", Utils.plotPower("", render.getHeatPower())) + ")"),
            v -> render.clientSendFloat(E.getSetManualControlValueEvent(), (float) v));
        temp = slider("temp", 0, 900, 10, render.getSetTemperature().getValue(),
            v -> Component.literal(tr("Set point: %1$", Utils.plotCelsius("", v)) + "  ("
                + tr("Actual: %1$", Utils.plotCelsius("", render.getActualTemperature())) + ")"),
            v -> render.clientSendFloat(E.getSetTemperatureEvent(), (float) v));
        update();
    }

    @Override
    protected void update() {
        boolean regulator = !deviceItem(C.getRegulatorSlot()).isEmpty();
        main.active = !deviceItem(C.getFuelBurnerSlot()).isEmpty();
        manual.active = !regulator && !render.getExternalControlled();
        temp.active = regulator && !render.getExternalControlled();
        if (render.getManualControl().getPending()) manual.setFromDevice(render.getManualControl().getValue());
        if (render.getSetTemperature().getPending()) temp.setFromDevice(render.getSetTemperature().getValue());
        manual.refreshLabel();
        temp.refreshLabel();
    }
}
