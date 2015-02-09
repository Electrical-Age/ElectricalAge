package mods.eln.sixnode.rs485cable;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class Rs485CableDescriptor extends SixNodeDescriptor  {

    public CableRenderDescriptor render;

    public Rs485CableDescriptor(String name, CableRenderDescriptor render) {
		super(name, Rs485CableElement.class, Rs485CableRender.class);
		this.render = render;
	}
    
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
		Data.addSignal(newItemStack());	
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
	}
}
