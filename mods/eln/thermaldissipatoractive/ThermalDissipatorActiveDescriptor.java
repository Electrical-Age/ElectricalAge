package mods.eln.thermaldissipatoractive;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor;
import mods.eln.thermaldissipatorpassive.ThermalDissipatorPassiveElement;
import mods.eln.thermaldissipatorpassive.ThermalDissipatorPassiveRender;

public class ThermalDissipatorActiveDescriptor extends TransparentNodeDescriptor  implements ITemperatureWatchdogDescriptor ,IThermalDestructorDescriptor{
	
	double nominalP, nominalT;
	
	public ThermalDissipatorActiveDescriptor(
			String name, 
			double nominalElectricalU,double electricalNominalP,
			double nominalElectricalCoolingPower,
			ElectricalCableDescriptor cableDescriptor,
			double warmLimit,double coolLimit, 
			double nominalP, double nominalT,
			double nominalTao, double nominalConnectionDrop
			) {
		super(name, ThermalDissipatorActiveElement.class, ThermalDissipatorActiveRender.class);
		this.cableDescriptor = cableDescriptor;
		this.electricalNominalP = electricalNominalP;
		this.nominalElectricalU = nominalElectricalU;
		this.nominalElectricalCoolingPower = nominalElectricalCoolingPower;
		electricalRp = nominalElectricalU*nominalElectricalU / electricalNominalP;
		electricalToThermalRp = nominalT / nominalElectricalCoolingPower;
		thermalC = (nominalP + nominalElectricalCoolingPower) * nominalTao / nominalT;
		thermalRp = nominalT / nominalP;
		thermalRs = nominalConnectionDrop / (nominalP + nominalElectricalCoolingPower);
		Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC);
		this.coolLimit = coolLimit;
		this.warmLimit = warmLimit;
		this.nominalP = nominalP;
		this.nominalT = nominalT;
	}
	double warmLimit, coolLimit;
	double nominalElectricalU;
	double nominalElectricalCoolingPower;
	public void applyTo(ThermalLoad load)
	{
		load.set(thermalRs, thermalRp, thermalC);
	}
	
	
	public double thermalRs,thermalRp,thermalC;
	double electricalRp;
	double electricalToThermalRp;
	public double electricalNominalP;
	ElectricalCableDescriptor cableDescriptor;
	public void applyTo(ElectricalLoad load)
	{
		cableDescriptor.applyTo(load, false);
		load.setRp(electricalRp);
	}
	
	

	@Override
	public double getThermalDestructionMax() {
		// TODO Auto-generated method stub
		return 2;
	}


	@Override
	public double getThermalDestructionStart() {
		// TODO Auto-generated method stub
		return 1;
	}


	@Override
	public double getThermalDestructionPerOverflow() {
		// TODO Auto-generated method stub
		return 0.1;
	}


	@Override
	public double getThermalDestructionProbabilityPerOverflow() {
		// TODO Auto-generated method stub
		return 0.05;
	}


	@Override
	public double getTmax() {
		// TODO Auto-generated method stub
		return warmLimit;
	}


	@Override
	public double getTmin() {
		// TODO Auto-generated method stub
		return coolLimit;
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Dissipates heat in air");
		list.add("Useful for cooling turbine");
		list.add(Utils.plotCelsius("Tmax :", warmLimit));
		list.add("Nominal usage ->");
		list.add(Utils.plotCelsius("  Temperature :", nominalT));
		list.add(Utils.plotPower("  Cooling :", nominalP));
		list.add(Utils.plotVolt("  Fan voltage :", nominalElectricalU));
		list.add(Utils.plotPower("  Fan electrical power :", electricalNominalP));
		list.add(Utils.plotPower("  Fan cooling power :", nominalElectricalCoolingPower));

	}
}
