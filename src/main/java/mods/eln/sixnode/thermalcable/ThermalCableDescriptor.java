package mods.eln.sixnode.thermalcable;

import java.util.List;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ThermalLoad;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ThermalCableDescriptor extends SixNodeDescriptor {

    boolean addToDataEnabled = true;

    double thermalRp = 1, thermalRs = 1, thermalC = 1;

    double thermalWarmLimit, thermalCoolLimit;
    double thermalStdT, thermalStdPower;
    double thermalStdDrop, thermalStdLost;
    double thermalTao;

    String description = "todo cable";

    public CableRenderDescriptor render;

    public static ThermalCableDescriptor[] list = new ThermalCableDescriptor[256];
    
	public ThermalCableDescriptor(String name,
		 	double thermalWarmLimit, double thermalCoolLimit,
		 	double thermalStdT, double thermalStdPower,
			double thermalStdDrop, double thermalStdLost,
			double thermalTao,
			CableRenderDescriptor render,
			String description) {
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
		if ( ! Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC)) {
			Utils.println("Bad thermalCable setup");
			while(true);
		}
	}
	
	public void addToData(boolean enable) {
		this.addToDataEnabled = enable;
	}
    
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		if (addToDataEnabled) {
			Data.addWiring(newItemStack());
			Data.addThermal(newItemStack());
		}
	}

	public static ThermalCableDescriptor getDescriptorFrom(ItemStack itemStack) {
		return list[(itemStack.getItemDamage() >> 8) & 0xFF];
	}
	
	/*
	static void setThermalLoadFrom(ItemStack itemStack, ThermalLoad thermalLoad) {
		if (itemStack == null || itemStack.itemID != Eln.sixNodeBlock.blockID || (itemStack.getItemDamage() & 0xFF) != Eln.electricalCableId) {
			thermalLoad.setHighImpedance();
		} else {
			ThermalCableDescriptor cableDescriptor = ThermalCableDescriptor.list[(itemStack.getItemDamage() >> 8) & 0xFF];
			thermalLoad.Rp = cableDescriptor.thermalRp;
			thermalLoad.Rs = cableDescriptor.thermalRs;
			thermalLoad.C = cableDescriptor.thermalC;
		}	
	}
	*/
	public void setThermalLoad(ThermalLoad thermalLoad) {
		thermalLoad.Rp = thermalRp;
		thermalLoad.Rs = thermalRs;
		thermalLoad.C = thermalC;
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add(Utils.plotCelsius("Tmax:", thermalWarmLimit));
		list.add(Utils.plotOhm("Serial Resistance:", thermalRs * 2));
		list.add(Utils.plotOhm("Parallel Resistance:", thermalRp));
		list.add("");
		list.add("Low Serial Resistance");
		list.add(" => High conductivity");
		list.add("High Parallel Resistance");
		list.add(" => Low power dissipation");
	}
}
