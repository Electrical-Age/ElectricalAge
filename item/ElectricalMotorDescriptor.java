package mods.eln.item;

import java.util.List;

import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.lampsocket.LampSocketType;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ElectricalResistorGrounded;
import mods.eln.sim.ElectricalStackMachineProcess;
import mods.eln.sim.IVoltageWatchdogDescriptorForInventory;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;



public class ElectricalMotorDescriptor  extends GenericItemUsingDamageDescriptor implements IVoltageWatchdogDescriptorForInventory
{
	double nominalU;
	double nominalP;
	double maximalP;
	double thermalHeatTime;
	double thermalConductivityTao,thermalWarmLimit;
	ElectricalCableDescriptor cable;
	
	double resistorR;
	double thermalRp, thermalRs, thermalC;
	
	public ElectricalMotorDescriptor(
				String name,
				double  nominalU,double nominalP,
				double  maximalU,
				double thermalConductivityTao,double thermalWarmLimit,double thermalHeatTime,
				ElectricalCableDescriptor cable
				
			) {
		super(name);
		this.nominalP = nominalP;
		this.nominalU = nominalU;
		this.maximalU = maximalU;
		this.cable = cable;
		this.thermalConductivityTao = thermalConductivityTao;
		this.thermalHeatTime = thermalHeatTime;
		this.thermalWarmLimit = thermalWarmLimit;
		
		resistorR = nominalU*nominalU/nominalP;
		this.maximalP = maximalU*maximalU/resistorR;
		thermalC = maximalP * thermalHeatTime / (thermalWarmLimit);
		thermalRp = thermalWarmLimit / maximalP;
		thermalRs = thermalConductivityTao / thermalC / 2;
		
	}

	double maximalU;
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Nominal U : " + nominalU);
		list.add("Nominal P : " + nominalP);
		
	}
	
	
	public void applyTo(ElectricalLoad load)
	{
		cable.applyTo(load, false);
	}
	
	public void applyTo(ElectricalResistor resistor)
	{
		resistor.setR(resistorR);
	}
	public void applyTo(ElectricalStackMachineProcess machine)
	{
		machine.setResistorValue(resistorR);
	}
	
	public void applyTo(ThermalLoad load)
	{
		load.set(this.thermalRs, this.thermalRp,this.thermalC);
	}



	@Override
	public double getUmax() {
		// TODO Auto-generated method stub
		return maximalU;
	}



	@Override
	public double getUmin() {
		// TODO Auto-generated method stub
		return -0.1 * maximalU;
	}



	@Override
	public double getBreakPropPerVoltOverflow() {
		// TODO Auto-generated method stub
		return 0.2;
	}
}
