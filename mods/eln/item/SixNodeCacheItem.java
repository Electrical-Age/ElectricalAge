package mods.eln.item;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;

public class SixNodeCacheItem extends GenericItemUsingDamageDescriptor{
	public static SixNodeCacheItem[] map = new SixNodeCacheItem[128];
	public Obj3D obj;
	public int mapIndex;
	public SixNodeCacheItem(
			String name,
			Obj3D obj,
			int mapIndex
			) {
		super(name);
		this.obj = obj;
		this.mapIndex = mapIndex;
		map[mapIndex] = this;
		if(obj != null){
			main = obj.getPart("main");
		}
	}
	
	Obj3DPart main;
	public void draw()
	{
		if(main != null)main.draw();
		
	}
	
	
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		
		main.draw();
	}
}
