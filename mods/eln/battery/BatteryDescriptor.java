package mods.eln.battery;

import java.util.List;

import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSASigner.stdDSA;

import mods.eln.Eln;
import mods.eln.client.ClientProxy;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.gui.GuiLabel;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
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
import mods.eln.wiki.Data;
import mods.eln.wiki.GuiVerticalExtender;
import mods.eln.wiki.ItemDefault.IPlugIn;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;



public class BatteryDescriptor extends TransparentNodeDescriptor implements IPlugIn{
	


	public double electricalU,electricalDischargeRate;
	public double electricalStdP,electricalStdDischargeTime,electricalStdHalfLife,electricalStdEfficiency,electricalPMax;
	
	public double electricalStdEnergy,electricalStdI;
	
	public double thermalHeatTime,thermalWarmLimit,thermalCoolLimit;
	
	
	
	public double electricalQ,electricalRs,electricalRp;
	public double thermalC,thermalRp,thermalPMax;
	public double lifeNominalCurrent,lifeNominalLost;
	public double startCharge;
	public boolean isRechargable;
	String description = "todo battery";
	
	FunctionTable UfCharge;
	String modelName;
	Obj3DPart modelPart;
	public double IMax;
	public boolean lifeEnable;
	private ElectricalCableDescriptor cable;
	public void draw()
	{
		switch (renderType) {
		case 0:
			if(modelPart == null) return;
			modelPart.draw();			
			break;
		case 1:
			if(main != null) main.draw();
			if(plugPlus != null) plugPlus.draw();
			if(plusMinus != null) plusMinus.draw();
			if(cables != null) cables.draw();
			if(battery != null) battery.draw();
			break;
		}

	}
	public BatteryDescriptor(
				String name,String modelName,
				ElectricalCableDescriptor cable,
				double startCharge,boolean isRechargable,boolean lifeEnable,
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
		this.startCharge = startCharge;
		this.isRechargable = isRechargable;
		this.lifeEnable = lifeEnable;
		this.cable = cable;
		
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
		//electricalRs =  electricalStdP*(1-electricalStdEfficiency) / electricalStdI/electricalStdI / 2;
		electricalRs = cable.electricalRs;
		electricalRp = electricalU*electricalU/electricalStdP/electricalDischargeRate;
		
		
		lifeNominalCurrent = electricalStdP / electricalU;
		lifeNominalLost = 0.5/electricalStdHalfLife;
		

		thermalPMax = electricalPMax/electricalU*electricalPMax/electricalU*electricalRs;
		thermalC = Math.pow(electricalPMax/electricalU,2)*electricalRs*thermalHeatTime/thermalWarmLimit;
		thermalRp = thermalWarmLimit/thermalPMax;
		
		IMax = electricalStdI*3;		
		
		obj = Eln.obj.getObj(modelName);
		
		if(obj != null){
			if(obj.getString("type").equals("A"))
				renderType = 0;
			if(obj.getString("type").equals("B"))
				renderType = 1;
			
			switch (renderType) {
			case 0:
				modelPart = obj.getPart("Battery");
			case 1:
				main = obj.getPart("main");
				plugPlus = obj.getPart("plugPlus");
				plusMinus = obj.getPart("plusMinus");
				cables = obj.getPart("cables");
				battery = obj.getPart("battery");
				break;

			}
			
		}
		
		
		

	}
	
	Obj3D obj;
	
	Obj3DPart main,plugPlus,plusMinus,cables,battery;
	
	int renderType;
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addEnergy(newItemStack());
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
		battery.isRechargeable = isRechargable;
		//battery.efficiency = electricalStdEfficiency;
   		
	}
	public void applyTo(ElectricalLoad load,Simulator simulator)
	{
		load.setRs(electricalRs);
		load.infinitRp();
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
		nbt.setDouble("charge",startCharge);
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
		list.add("Capacity : " + Utils.plotValue((electricalQ/3600),"Ah"));
		list.add("");
	   	list.add("Charge : " + (int)(getChargeInTag(itemStack)*100) + "%");
	   	
	   	if(lifeEnable)
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
			double voltage = UfCharge.getValue(chargeIntegrator)*electricalU;
			energy += voltage*QperStep;
			chargeIntegrator += chargeStep;

		}
		
		return energy;
	}
	
	
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw();
	}
	
	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem) {
		if(entityItem.isBurning())
		{
			entityItem.worldObj.createExplosion(entityItem, entityItem.posX, entityItem.posY, entityItem.posZ, 2,true);
			entityItem.extinguish();
			entityItem.setDead();	
		}

		return false;
	}
	@Override
	public int top(int y, GuiVerticalExtender extender, ItemStack stack) {
		extender.add(new GuiLabel(6, y, "miaouuuu"));
		y+=12;
		return y;
	}
	@Override
	public int bottom(int y, GuiVerticalExtender extender, ItemStack stack) {
		// TODO Auto-generated method stub
		return y;
	}
}
