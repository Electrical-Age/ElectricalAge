package mods.eln.hub;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.wiki.Data;

public class HubDescriptor extends SixNodeDescriptor{

	Obj3D obj;
	Obj3DPart main;
	Obj3DPart[] connection = new Obj3DPart[6];
	
	public HubDescriptor(String name,Obj3D obj) {
		super(name, HubElement.class, HubRender.class);
		this.obj = obj;
		if(obj != null){
			main = obj.getPart("main");
			for(int idx = 0;idx < 6;idx++){
				connection[idx] = obj.getPart("con" + idx);
			}
		}
	}
	
	void draw(boolean[] connectionGrid)
	{
		if(main != null) main.draw();
		for(int idx = 0;idx < 6;idx++){
			if(connectionGrid[idx])
				GL11.glColor3f(0, 0, 0);
			else
				GL11.glColor3f(130/255f, 67/255f, 18/255f);
			if(connection[idx] != null) connection[idx].draw();
		}
		GL11.glColor3f(1, 1, 1);
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
		// TODO Auto-generated method stub
		//GL11.glTranslatef(-0.3f, -0.1f, 0f);
		draw(new boolean[]{true,true,true,true,true,true});
	}
	
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Provides a zero volt reference.");
		list.add("Can be used to ground negative");
		list.add("battery pins.");
	}
	
	
	
	
}
