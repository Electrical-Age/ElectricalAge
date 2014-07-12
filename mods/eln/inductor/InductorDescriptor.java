package mods.eln.inductor;

import java.util.List;

import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;


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
		cable.applyTo(load);
	}
	public void applyTo(Inductor inductor)
	{
		inductor.setL(henri);
	}
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);

	}
}
