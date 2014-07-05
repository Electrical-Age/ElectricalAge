package mods.eln.sixnode.diode;

import java.util.List;

import mods.eln.misc.IFunction;
import mods.eln.node.NodeResistorSwitch;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class DiodeDescriptor extends SixNodeDescriptor {

	public DiodeDescriptor(
			String name,
			IFunction IfU,
			double Imax,
			double stdU,double stdI,
			ThermalLoadInitializer thermal,
			ElectricalCableDescriptor cable) {
		super(name, DiodeElement.class, DiodeRender.class);
		this.IfU = IfU;
		
		double Umax = 0;
		while(IfU.getValue(Umax) < Imax) Umax += 0.01;
		double Pmax = Umax * IfU.getValue(Umax);
		this.cable = cable;
		this.thermal = thermal;
		thermal.setMaximalPower(Pmax);
		this.stdI = stdI;
		this.stdU = stdU;
	}
	double stdI, stdU;
	ElectricalCableDescriptor cable;
	String descriptor;
	IFunction IfU;
	
	ThermalLoadInitializer thermal;
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addEnergy(newItemStack());
	}
	
/*	public void applyTo(DiodeProcess diode) {
		diode.IfU = IfU;
	}
	*/
	public void applyTo(ThermalLoad load) {
		thermal.applyTo(load);
	}
	
	public void applyTo(ElectricalLoad load) {
		cable.applyTo(load);
	}
	
	public void applyTo(NodeResistorSwitch resistorSwitch) {
		resistorSwitch.setR(stdU/stdI);
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("The current can only run");
		list.add("one way");
	}


}
