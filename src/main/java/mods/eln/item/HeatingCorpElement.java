package mods.eln.item;

import mods.eln.misc.VoltageLevelColor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.RegulatorThermalLoadToElectricalResistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class HeatingCorpElement extends GenericItemUsingDamageDescriptorUpgrade {

    public double electricalNominalU;
    double electricalNominalP;
    double electricalMaximalP;
    public ElectricalCableDescriptor cableDescriptor;

    double electricalR;

    double Umax;

    public HeatingCorpElement(String name,
                              double electricalNominalU, double electricalNominalP,
                              double electricalMaximalP,
                              ElectricalCableDescriptor cableDescriptor) {
        super(name);

        this.electricalNominalU = electricalNominalU;
        this.electricalNominalP = electricalNominalP;
        this.electricalMaximalP = electricalMaximalP;
        this.cableDescriptor = cableDescriptor;

        electricalR = electricalNominalU * electricalNominalU / electricalNominalP;

        Umax = Math.sqrt(electricalMaximalP * electricalR);

        voltageLevelColor = VoltageLevelColor.fromVoltage(electricalNominalU);
    }
/*
    public void applyTo(ElectricalResistor resistor) {
		resistor.setR(electricalR);
	}*/

    public void applyTo(ElectricalLoad load) {
        cableDescriptor.applyTo(load);
    }

    public void applyTo(RegulatorThermalLoadToElectricalResistor regulator) {
        regulator.setRmin(electricalR);
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(tr("Nominal:"));
        list.add("  " + tr("Voltage: %1$V", electricalNominalU));
        list.add("  " + tr("Power: %1$W", electricalNominalP));
        list.add("  " + tr("Resistance: %1$Ω", electricalR));
    }
}
