package mods.eln.electricalsource;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.node.SixNodeDescriptor;

public class ElectricalSourceDescriptor extends SixNodeDescriptor{

	public ElectricalSourceDescriptor(String name) {
		super(name, ElectricalSourceElement.class,ElectricalSourceRender.class);
		// TODO Auto-generated constructor stub
	}
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Provide a stable voltage source");
		list.add("without energy/power limitation");
		list.add("This bloc is a cheat");
	}

}
