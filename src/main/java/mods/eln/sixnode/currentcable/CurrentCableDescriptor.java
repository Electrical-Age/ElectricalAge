package mods.eln.sixnode.currentcable;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.debug.DebugType;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.misc.materials.MaterialType;
import mods.eln.node.NodeBase;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.genericcable.GenericCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class CurrentCableDescriptor extends GenericCableDescriptor {

    public CableRenderDescriptor render;

    double thermalC;
    double thermalRp;
    double thermalRs;
    double thermalWarmLimit;
    double thermalCoolLimit;

    public CurrentCableDescriptor(String name, CableRenderDescriptor render) {
        super(name, CurrentCableElement.class, CurrentCableRender.class);
        this.render = render;
        electricalNominalVoltage = 1000000000;
    }

    /**
     *
     * @param conductorArea size of conductor area (mm^2)
     * @param type material type (Copper, Iron, etc.)
     * @param insulationThickness thickness of insulation (mm)
     * @param thermalWarmLimit maximum temperature (C)
     * @param thermalCoolLimit minimum temperature (C)
     * @param thermalNominalHeatTime
     */
    public void setCableProperties(double conductorArea, MaterialType type, double insulationThickness, double thermalWarmLimit, double thermalCoolLimit, double thermalNominalHeatTime) {

        this.thermalWarmLimit = thermalWarmLimit;
        this.thermalCoolLimit = thermalCoolLimit;

        this.electricalMaximalCurrent = 0.355 * conductorArea; // roughly (mm^2 / I) that is suggested by https://www.powerstream.com/Wire_Size.htm for power transmission lines

        electricalRs = Eln.mp.getElectricalResistivity(type) * (conductorArea / 1000000 / 1.0); // resistivity (ohms/meter)* (cross sectional area (m) / length (m))
        Eln.dp.println(DebugType.SIX_NODE, "Cable Resistance: " + electricalRs);

        // begin odd thermal system code
        double thermalMaximalPowerDissipated = electricalMaximalCurrent * electricalMaximalCurrent * electricalRs * 2;
        thermalC = (thermalMaximalPowerDissipated * thermalNominalHeatTime) / thermalWarmLimit;
        thermalRp = thermalWarmLimit / thermalMaximalPowerDissipated;
        thermalRs = (Eln.mp.getThermalConductivity(type) / 385.0) / thermalC / 2;
        // TODO: FIX WHEN REDOING THERMAL SYSTEM
        // I replaced thermalConductivityTao with (material.getThermalConductivity() / 385.0)
        // Since thermalConductivityTao is typically 1, I'm going to use Copper's thermal conductivity constant as a baseline.
        // When someone redoes the thermal system, please remove this shim and do it correctly.

        Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC);

        voltageLevelColor = VoltageLevelColor.None;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    public void applyTo(ElectricalLoad electricalLoad, double rsFactor) {
        electricalLoad.setRs(electricalRs * rsFactor);
    }

    public void applyTo(ElectricalLoad electricalLoad) {
        applyTo(electricalLoad, 1);
    }

    public void applyTo(Resistor resistor) {
        applyTo(resistor, 1);
    }

    public void applyTo(Resistor resistor, double factor) {
        resistor.setR(electricalRs * factor);
    }

    public void applyTo(ThermalLoad thermalLoad) {
        thermalLoad.Rs = this.thermalRs;
        thermalLoad.C = this.thermalC;
        thermalLoad.Rp = this.thermalRp;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(tr("Nominal Ratings:"));
        list.add("  " + tr("Current: %1$A", Utils.plotValue(electricalMaximalCurrent)));
        list.add("  " + tr("Serial resistance: %1$\u2126", Utils.plotValue(electricalRs * 2)));
    }

    public int getNodeMask() {
        return NodeBase.MASK_ELECTRICAL_POWER;
    }

    public static CableRenderDescriptor getCableRender(ItemStack cable) {
        if (cable == null) return null;
        GenericItemBlockUsingDamageDescriptor desc = CurrentCableDescriptor.getDescriptor(cable);
        if (desc instanceof CurrentCableDescriptor)
            return ((CurrentCableDescriptor) desc).render;
        else
            return null;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return false;
    }

}
