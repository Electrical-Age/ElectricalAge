package mods.eln.item;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.wiki.Data;

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
			for(int idx = 0;idx < 6;idx++)
				sidePart[idx] = obj.getPart("main"+idx);		
		}
	}
	
	public void setParent(net.minecraft.item.Item item, int damage) 
	{
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}
	
	Obj3DPart main;
	Obj3DPart[] sidePart = new Obj3DPart[6];
	public void draw(World world,int x,int y,int z)
	{
		if(main != null)main.draw();
		int[] v = new int[3];
		if(world != null){
			UtilsClient.disableLight();
			for(int idx = 0;idx < 6;idx++){
				if(sidePart[idx] == null) continue;
				v[0] = x;v[1] = y;v[2] = z;
				Direction.fromInt(idx).applyTo(v, 1);
				float light = world.provider.lightBrightnessTable[UtilsClient.getLight(world,v[0],v[1],v[2])];
				
				GL11.glColor3f(light, light, light);
				sidePart[idx].draw();
			}
			UtilsClient.enableLight();
		} else {
			for(int idx = 0;idx < 6;idx++){
				if(sidePart[idx] == null) continue;
				sidePart[idx].draw();
			}			
		}

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
		GL11.glScalef(0.5f, 0.5f, 0.5f);
		draw(null, 0, 0, 0);
	}
}
