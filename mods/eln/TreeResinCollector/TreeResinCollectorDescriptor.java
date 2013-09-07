package mods.eln.TreeResinCollector;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.wiki.Data;

public class TreeResinCollectorDescriptor extends SixNodeDescriptor {

	private Obj3D obj;
	private Obj3DPart main,fill;

	public TreeResinCollectorDescriptor(String name,Obj3D obj) {
		super(name, TreeResinCollectorElement.class, TreeResinCollectorRender.class);
		this.obj = obj;
		if(obj != null){
			main = obj.getPart("main");
			fill = obj.getPart("fill");
			if(fill != null){
				emptyT = fill.getFloat("emptyT");
				emptyS = fill.getFloat("emptyS");
			}
		}
	}

	float emptyS,emptyT;
	
	
	void draw(float factor)
	{
		if(main != null) main.draw();
		if(fill != null){
			if(factor>1f)factor = 1f;
			factor = (1f-factor);
			GL11.glTranslatef(0f,0f,factor*emptyT);
			GL11.glScalef(1f - factor*(1f-emptyS),1f - factor*(1f-emptyS), 1f);
			fill.draw();
		}
	}
	
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addMachine(newItemStack());
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
		if(type == ItemRenderType.INVENTORY){
			GL11.glScalef(2f, 2f, 2f);
		}
		draw(0.0f);
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Product tree resin over time");
		list.add("when placed on tree");
		list.add("The production is slow");
	}
}
