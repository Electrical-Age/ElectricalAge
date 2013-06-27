package mods.eln.lampsocket;

import mods.eln.Eln;
import mods.eln.node.SixNodeDescriptor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

public class LampSocketDescriptor extends SixNodeDescriptor{
	public LampSocketType socketType;
	public LampSocketDescriptor(String name, String modelName,
								LampSocketType socketType
			) 
	{
		super(name, LampSocketElement.class,LampSocketRender.class);
		this.socketType = socketType;
		this.modelName = modelName;
	}


	public String modelName;
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		// TODO Auto-generated method stub
		Eln.obj.draw(modelName, "socket");
	}		
}	
	

