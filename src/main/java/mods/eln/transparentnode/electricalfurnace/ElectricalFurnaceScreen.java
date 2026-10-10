package mods.eln.transparentnode.electricalfurnace;

import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.item.HeatingCorpElement;
import mods.eln.misc.Utils;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import static mods.eln.i18n.I18N.tr;

/**
 * Electrical furnace (P4b, 1.12 ElectricalFurnaceGuiDraw restyled; layout gui/electrical_furnace.json): input ->
 * progress -> output, heater and regulator slots, target temperature with the actual one, the heater's supply, power
 * and auto-stop. Same EA packets as 1.12.
 */
public class ElectricalFurnaceScreen extends ElnScreen {
    final ElectricalFurnaceRender render;
    EaSlider target;

    public ElectricalFurnaceScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalFurnaceRender) render;
    }

    static String celsius(double relative) {
        return Utils.plotValue(relative + PhysicalConstant.Tamb) + "°C";
    }

    @Override
    protected void init() {
        super.init();
        progress("process", () -> render.processState);
        target = slider("target", 0, 800, 10, render.temperatureTargetSyncValue < -1000 ? 0 : render.temperatureTargetSyncValue,
            v -> Component.literal(tr("Target %1$  (now %2$)", celsius(v), celsius(render.temperature))),
            v -> render.clientSetTemperatureTarget((float) v));
        gauge("supply", this::supplyFraction, this::supplyText, 1.0, 1.2);
        var power = button("heater", () -> render.getPowerOn() ? tr("Heater: on") : tr("Heater: off"),
            () -> render.clientSetPowerOn(!render.getPowerOn()));
        button("autostop", () -> render.autoShutDown ? tr("Auto stop: on") : tr("Auto stop: off"),
            () -> render.clientSendId(ElectricalFurnaceElement.unserializeAutoShutDownId),
            tr("Auto: the heater stops when nothing is left to smelt"));
        this.power = power;
    }

    net.minecraft.client.gui.components.Button power;

    ItemStack heater() {
        return menu.deviceInventory.getItem(ElectricalFurnaceElement.heatingCorpSlotId);
    }

    double nominalU() {
        ItemStack h = heater();
        if (Utils.isEmpty(h)) return 0;
        Object d = HeatingCorpElement.getDescriptor(h);
        return d instanceof HeatingCorpElement hc ? hc.electricalNominalU : 0;
    }

    double supplyFraction() {
        double u = nominalU();
        return u <= 0 ? 0 : render.voltage / u;
    }

    Component supplyText() {
        double u = nominalU();
        if (u <= 0) return Component.literal(tr("No heating element"));
        return Component.literal(Utils.plotVolt("", render.voltage) + " / " + Utils.plotVolt("", u) + "  " + Utils.plotPower("", render.heatingCorpResistorP));
    }

    @Override
    protected void update() {
        if (render.temperatureTargetSyncNew) {
            target.setFromDevice(render.temperatureTargetSyncValue);
            render.temperatureTargetSyncNew = false;
        }
        target.refreshLabel();
        power.active = !render.autoShutDown;
    }
}
