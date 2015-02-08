package mods.eln.sixnode.lampsocket;

import net.minecraftforge.client.IItemRenderer.ItemRenderType;

public interface LampSocketObjRender {
	
	void draw(LampSocketDescriptor descriptor, ItemRenderType type);
	
	void draw(LampSocketRender render);
}
