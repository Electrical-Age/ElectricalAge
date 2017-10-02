package mods.eln.sixnode.electricalcable;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalCableDescriptor extends SixNodeDescriptor {

    double electricalNominalRs;
    public double electricalNominalVoltage, electricalNominalPower, electricalNominalPowerDropFactor;
    public boolean signalWire;

    public double electricalMaximalVoltage, electricalMaximalCurrent;
    public double electricalRp = Double.POSITIVE_INFINITY, electricalRs = Double.POSITIVE_INFINITY, electricalC = 1;
    public double thermalRp = 1, thermalRs = 1, thermalC = 1;
    public double thermalWarmLimit = 100, thermalCoolLimit = -100;
    double electricalMaximalI;
    public double electricalRsMin = 0;
    public double electricalRsPerCelcius = 0;

    public double dielectricBreakOhmPerVolt = 0;
    public double dielectricBreakOhm = Double.POSITIVE_INFINITY;
    public double dielectricVoltage = Double.POSITIVE_INFINITY;
    public double dielectricBreakOhmMin = Double.POSITIVE_INFINITY;

    String description = "todo cable";

    public CableRenderDescriptor render;

    public ElectricalCableDescriptor(String name, CableRenderDescriptor render, String description, boolean signalWire) {
        super(name, ElectricalCableElement.class, ElectricalCableRender.class);

        this.description = description;
        this.render = render;
        this.signalWire = signalWire;

    }

    public void setPhysicalConstantLikeNormalCable(
        double electricalNominalVoltage, double electricalNominalPower, double electricalNominalPowerDropFactor,
        double electricalMaximalVoltage, double electricalMaximalPower,
        double electricalOverVoltageStartPowerLost,
        double thermalWarmLimit, double thermalCoolLimit,
        double thermalNominalHeatTime, double thermalConductivityTao) {
        this.electricalNominalVoltage = electricalNominalVoltage;
        this.electricalNominalPower = electricalNominalPower;
        this.electricalNominalPowerDropFactor = electricalNominalPowerDropFactor;

        this.thermalWarmLimit = thermalWarmLimit;
        this.thermalCoolLimit = thermalCoolLimit;
        this.electricalMaximalVoltage = electricalMaximalVoltage;

        electricalRp = MnaConst.highImpedance;
        double electricalNorminalI = electricalNominalPower / electricalNominalVoltage;
        electricalNominalRs = (electricalNominalPower * electricalNominalPowerDropFactor) / electricalNorminalI / electricalNorminalI / 2;
        electricalRs = electricalNominalRs;
        //electricalC = Eln.simulator.getMinimalElectricalC(electricalNominalRs, electricalRp);

        electricalMaximalI = electricalMaximalPower / electricalNominalVoltage;
        double thermalMaximalPowerDissipated = electricalMaximalI * electricalMaximalI * electricalRs * 2;
        thermalC = thermalMaximalPowerDissipated * thermalNominalHeatTime / (thermalWarmLimit);
        thermalRp = thermalWarmLimit / thermalMaximalPowerDissipated;
        thermalRs = thermalConductivityTao / thermalC / 2;

        Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC);

        electricalRsMin = electricalNominalRs;
        electricalRsPerCelcius = 0;

        dielectricBreakOhmPerVolt = 0.95;
        dielectricBreakOhm = electricalMaximalVoltage * electricalMaximalVoltage / electricalOverVoltageStartPowerLost;
        dielectricVoltage = electricalMaximalVoltage;
        dielectricBreakOhmMin = dielectricBreakOhm;

        this.electricalMaximalCurrent = electricalMaximalPower / electricalNominalVoltage;

        voltageLevelColor = VoltageLevelColor.fromCable(this);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());

        if (signalWire) {
            Data.addSignal(newItemStack());
        }
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
        if (signalWire) {
            Collections.addAll(list, tr("Cable is adapted to conduct\nelectrical signals.").split("\n"));
            Collections.addAll(list, tr("A signal is electrical information\nwhich must be between 0V and %1$", Utils.plotVolt(Eln.SVU)).split("\n"));
            list.add(tr("Not adapted to transport power."));

			/*String lol = "";
			for (int idx = 0; idx < 15; idx++) {
				if (idx < 10) {
					lol += "\u00a7" + idx + "" +  idx;
				} else {
					lol += "\u00a7" + "abcdef".charAt(idx - 10) + "abcdef".charAt(idx - 10);
				}
			}
			list.add(lol);*/
        } else {
            //list.add("Low resistor => low power lost");
            list.add(tr("Save usage:"));
            list.add("  " + tr("Voltage: %1$V", Utils.plotValue(electricalNominalVoltage)));
            list.add("  " + tr("Current: %1$A", Utils.plotValue(electricalNominalPower / electricalNominalVoltage)));
            list.add("  " + tr("Power: %1$W", Utils.plotValue(electricalNominalPower)));
            list.add("  " + tr("Serial resistance: %1$Ω", Utils.plotValue(electricalNominalRs * 2)));
        }
    }

    public int getNodeMask() {
        if (signalWire)
            return NodeBase.maskElectricalGate;
        else
            return NodeBase.maskElectricalPower;
    }

    public static CableRenderDescriptor getCableRender(ItemStack cable) {
        if (cable == null) return null;
        GenericItemBlockUsingDamageDescriptor desc = ElectricalCableDescriptor.getDescriptor(cable);
        if (desc instanceof ElectricalCableDescriptor)
            return ((ElectricalCableDescriptor) desc).render;
        else
            return null;
    }

    public void bindCableTexture() {
        this.render.bindCableTexture();
    }
}
