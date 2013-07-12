package mods.eln.thermalcable;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.NodeThermalWatchdogProcess;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadDynamicProcess;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalWatchdogProcess;
import net.minecraft.item.ItemStack;

public class ThermalCableDescriptor extends SixNodeDescriptor{

	public ThermalCableDescriptor(String name,
		 	double thermalWarmLimit,double thermalCoolLimit,
		 	double thermalStdT,double thermalStdPower,
			double thermalStdDrop,double thermalStdLost,
			double thermalTao,
			CableRenderDescriptor render,
			String description)
	{
		super(name, ThermalCableElement.class, ThermalCableRender.class);
		

		this.description = description;
		this.render = render;
		
		
		this.thermalWarmLimit = thermalWarmLimit;
		this.thermalCoolLimit = thermalCoolLimit;
		this.thermalStdT = thermalStdT;
		this.thermalStdPower = thermalStdPower;
		this.thermalStdDrop = thermalStdDrop;
		this.thermalStdLost = thermalStdLost;
		this.thermalTao = thermalTao;
		
		
		thermalRs = thermalStdDrop / 2 / thermalStdPower;
		thermalRp = thermalStdT / thermalStdLost;
		//thermalC = thermalTao / (thermalRs * 2) ;
		thermalC = Eln.simulator.getMinimalThermalC(thermalRs, thermalRp);	
		if( ! Eln.simulator.checkThermalLoad(thermalRs,thermalRp,thermalC))
		{
			System.out.println("Bad thermalCable setup");
			while(true);
		}
		
		}
		

	double thermalRp = 1, thermalRs = 1, thermalC = 1;
	
	 double thermalWarmLimit, thermalCoolLimit;
	 double thermalStdT, thermalStdPower;
	 double thermalStdDrop, thermalStdLost;
	 double thermalTao;
		  

	

	String description = "todo cable";
	
	public CableRenderDescriptor render;
	

	public static ThermalCableDescriptor[] list = new ThermalCableDescriptor[256];
	
	public static ThermalCableDescriptor getDescriptorFrom(ItemStack itemStack)
	{
		return list[(itemStack.getItemDamage()>>8) & 0xFF];
	}
	
	/*
	static void setThermalLoadFrom(ItemStack itemStack,ThermalLoad thermalLoad)
	{
		if(itemStack == null || itemStack.itemID != Eln.sixNodeBlock.blockID || (itemStack.getItemDamage() & 0xFF) != Eln.electricalCableId)
		{
			thermalLoad.setHighImpedance();
			
		}
		else
		{
			ThermalCableDescriptor cableDescriptor = ThermalCableDescriptor.list[(itemStack.getItemDamage()>>8) & 0xFF];
			thermalLoad.Rp = cableDescriptor.thermalRp;
			thermalLoad.Rs = cableDescriptor.thermalRs;
			thermalLoad.C = cableDescriptor.thermalC;
		}	
	}
	*/
	public void setThermalLoad(ThermalLoad thermalLoad)
	{
		thermalLoad.Rp = thermalRp;
		thermalLoad.Rs = thermalRs;
		thermalLoad.C = thermalC;
	}

}
