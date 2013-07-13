package mods.eln.transformer;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.node.TransparentNodeDescriptor;

public class TransformerDescriptor extends TransparentNodeDescriptor {

	public TransformerDescriptor(String name) {
		super(name, TransformerElement.class,TransformerRender.class);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Can transform voltage");
		list.add("The transform ratio is");
		list.add("defined by cable stack size");
	}

}
