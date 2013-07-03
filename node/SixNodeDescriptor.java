package mods.eln.node;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.Direction;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

public class SixNodeDescriptor extends GenericItemBlockUsingDamageDescriptor implements IItemRenderer{
	public Class ElementClass,RenderClass;
	public SixNodeDescriptor(String name,
							 Class ElementClass,Class RenderClass) {
		super(name);
		this.ElementClass = ElementClass;
		this.RenderClass = RenderClass;
		// TODO Auto-generated constructor stub
	}
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		// TODO Auto-generated method stub
		
	}
	public boolean hasVolume() {
		// TODO Auto-generated method stub
		return false;
	}

	
	public boolean canBePlacedOnSide(Direction side)
	{
		return true;
	}

}
