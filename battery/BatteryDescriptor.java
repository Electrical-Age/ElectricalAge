package mods.eln.battery;

import java.util.List;

import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSASigner.stdDSA;


import mods.eln.Eln;
import mods.eln.client.ClientProxy;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.NodeThermalWatchdogProcess;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.BatteryProcess;
import mods.eln.sim.BatterySlowProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadDynamicProcess;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;



public class BatteryDescriptor extends TransparentNodeDescriptor{
	


	public double electricalU,electricalDischargeRate;
	public double electricalStdP,electricalStdDischargeTime,electricalStdHalfLife,electricalStdEfficiency,electricalPMax;
	
	public double electricalStdEnergy,electricalStdI;
	
	public double thermalHeatTime,thermalWarmLimit,thermalCoolLimit;
	
	
	
	public double electricalQ,electricalRs,electricalRp;
	public double thermalC,thermalRp,thermalPMax;
	public double lifeNominalCurrent,lifeNominalLost;
	
	String description = "todo battery";
	
	FunctionTable UfCharge;
	String modelName;
	Obj3DPart modelPart;
	public void draw()
	{
		if(modelPart == null) return;
		modelPart.drawList();
	}
	public BatteryDescriptor(
				String name,String modelName,
				FunctionTable UfCharge,
				double electricalU,double electricalPMax,double electricalDischargeRate,
				double electricalStdP,double electricalStdDischargeTime,double electricalStdEfficiency,double electricalStdHalfLife,
				double thermalHeatTime,double thermalWarmLimit,double thermalCoolLimit,
			  	String description)
	{
		super(name, BatteryElement.class, BatteryRender.class);
		this.electricalU = electricalU;
		this.electricalDischargeRate = electricalDischargeRate;
		this.electricalStdEfficiency = electricalStdEfficiency;
		this.electricalStdP = electricalStdP;
		this.electricalStdHalfLife = electricalStdHalfLife;
		this.electricalStdDischargeTime = electricalStdDischargeTime;

		
		this.thermalHeatTime = thermalHeatTime;
		this.thermalWarmLimit = thermalWarmLimit;
		this.thermalCoolLimit = thermalCoolLimit;
		this.electricalPMax = electricalPMax;
		
		this.UfCharge = UfCharge;
		this.description = description;
		
		electricalStdI = electricalStdP/ electricalU;
		electricalStdEnergy = electricalStdDischargeTime * electricalStdP;
		
		
		electricalQ = electricalStdP*electricalStdDischargeTime/electricalU;
		electricalQ = 1;
		double energy = getEnergy(1.0, 1.0);
		electricalQ *= electricalStdEnergy/energy;
		electricalRs =  electricalStdP*(1-electricalStdEfficiency) / electricalStdI/electricalStdI / 2;
		electricalRp = electricalU*electricalU/electricalStdP/electricalDischargeRate;
		
		
		lifeNominalCurrent = electricalStdP / electricalU;
		lifeNominalLost = 0.5/electricalStdHalfLife;
		

		thermalPMax = electricalPMax/electricalU*electricalPMax/electricalU*electricalRs;
		thermalC = electricalPMax/electricalU*thermalHeatTime/thermalWarmLimit;
		thermalRp = thermalWarmLimit/thermalPMax;
		
		modelPart = Eln.obj.getPart(modelName, "Battery");
		
	}
	public void applyTo(ElectricalResistor resistor)
	{
		resistor.setR(electricalRp);
   		
	}	
	public void applyTo(BatteryProcess battery)
	{
		battery.QNominal = electricalQ;
		battery.uNominal = electricalU;
		battery.voltageFunction = UfCharge;
		//battery.efficiency = electricalStdEfficiency;
   		
	}
	public void applyTo(ElectricalLoad load,Simulator simulator)
	{
		load.setRs(electricalRs);
		load.setRp(1000000000.0);
		load.setMinimalC(simulator);
	}
	
	public void applyTo(ThermalLoad load)
	{
		load.Rp = thermalRp;
		load.C = thermalC;
		//load.setRsByTao(2);
	}

	public void applyTo(BatterySlowProcess process)
	{
		process.lifeNominalCurrent = lifeNominalCurrent;
		process.lifeNominalLost = lifeNominalLost;
	}
	
	public static BatteryDescriptor[] list = new BatteryDescriptor[8];
	
	public static BatteryDescriptor getDescriptorFrom(ItemStack itemStack)
	{
		return list[(itemStack.getItemDamage()) & 0x7];
	}
	
	
	@Override
	public NBTTagCompound getDefaultNBT() {
		// TODO Auto-generated method stub
		NBTTagCompound nbt = new NBTTagCompound("itemStackNBT");
		nbt.setDouble("charge",0.5);
		nbt.setDouble("life",1.0);
		return nbt;
	}
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Nominal voltage : " + (int)(electricalU) + "V");
		list.add("Nominal power : " + (int)(electricalStdP) + "W");
		list.add("Full charge energy : " + (int)(electricalStdDischargeTime*electricalStdP/1000) + "KJ");
		list.add("");
	   	list.add("Charge : " + (int)(getChargeInTag(itemStack)*100) + "%");
    	list.add("Life : " + (int)(getLifeInTag(itemStack)*100) + "%");
 
	}

	
	@Override
	public String getName(ItemStack stack) {
		return super.getName(stack) + " charged at " + (int)(getChargeInTag(stack)*100) + "%";
	}
	
	
	double getChargeInTag(ItemStack stack)
	{
		if(stack.hasTagCompound() == false)
			stack.setTagCompound(getDefaultNBT());
		return stack.getTagCompound().getDouble("charge");
	}
	double getLifeInTag(ItemStack stack)
	{
		if(stack.hasTagCompound() == false)
			stack.setTagCompound(getDefaultNBT());
		return stack.getTagCompound().getDouble("life");
	}
	public double getEnergy(double charge,double life)
	{
		int stepNbr = 50;
		double chargeStep = charge / stepNbr;
		double chargeIntegrator = 0;
		double energy = 0;
		double QperStep = electricalQ*life*charge / stepNbr;
		
		for(int step = 0; step < stepNbr;step++)
		{	
			chargeIntegrator += chargeStep;
			double voltage = UfCharge.getValue(chargeIntegrator)*electricalU;
			energy += voltage*QperStep;
		}
		
		return energy;
	}
}
