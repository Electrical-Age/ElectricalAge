package mods.eln.transparentnode.computercraftio;

import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ComputerCraftIoDescriptor extends TransparentNodeDescriptor {

	public ComputerCraftIoDescriptor(String name, Obj3D obj) {
		super(name, ComputerCraftIoElement.class, ComputerCraftIoRender.class);
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw(0, 1f);
	}
	@Override
	public boolean use2DIcon() {
		return false;
	}
    
	void draw(int eggStackSize, float powerFactor) {
	}
}
