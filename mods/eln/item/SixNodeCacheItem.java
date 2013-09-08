package mods.eln.item;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Obj3D;
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
			xn = obj.getPart("xn");
			xp = obj.getPart("xp");
			yn = obj.getPart("yn");
			yp = obj.getPart("yp");
			zn = obj.getPart("zn");
			zp = obj.getPart("zp");
			
		}
	}
	
	public void setParent(net.minecraft.item.Item item, int damage) 
	{
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}
	
	Obj3DPart main,xp,xn,yp,yn,zp,zn;
	public void draw(World world,int x,int y,int z)
	{
		if(main != null)main.draw();
	/*	//Utils.disableLight();
		if(xn != null){
			float light = world.getLightBrightness(x-1, y, z)*0.97f+0.03f;
		//	float light = world.getBrightness(x-1, y, z,0);
			//light = 1f;
		//	GL11.glColor3f(light, light, light);
			xn.draw();
		}
		Utils.enableLight();*/
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
		
		if(main != null)main.draw();
	}
}
