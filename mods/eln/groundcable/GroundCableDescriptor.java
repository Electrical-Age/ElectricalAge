package mods.eln.groundcable;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.node.SixNodeDescriptor;

public class GroundCableDescriptor extends SixNodeDescriptor{

	public GroundCableDescriptor(String name) {
		super(name, GroundCableElement.class, GroundCableRender.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Provide a zero volt reference");
		list.add("Can be used to put negative");
		list.add("battery pin to the ground");
	}
	
}
