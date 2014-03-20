package mods.eln.inductor;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.IFunction;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalInductor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.wiki.Data;

import com.google.common.base.Function;


public class InductorDescriptor extends SixNodeDescriptor{

	

	public InductorDescriptor(
			String name,
			double henri,
			ElectricalCableDescriptor cable
			) {
		super(name, InductorElement.class,InductorRender.class);
		this.henri = henri;
		this.cable = cable;
	}
	ElectricalCableDescriptor cable;
	String descriptor;
	public double henri;

	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addEnergy(newItemStack());
	}

	public void applyTo(ElectricalLoad load)
	{
		cable.applyTo(load,false);
	}
	public void applyTo(ElectricalInductor inductor)
	{
		inductor.setHenri(henri);
	}
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);

	}
}
