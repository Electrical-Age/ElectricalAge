package mods.eln.transformer;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.wiki.Data;

public class TransformerDescriptor extends TransparentNodeDescriptor {
	Obj3D obj;
	Obj3D defaultFeroObj;
	public TransformerDescriptor(
			String name,
			Obj3D obj,
			Obj3D defaultFeroObj
			) {
		super(name, TransformerElement.class,TransformerRender.class);
		this.obj = obj;
		this.defaultFeroObj = defaultFeroObj;
		
		if(obj != null){
			main = obj.getPart("main");
			sbire = obj.getPart("sbire");
		}
		if(defaultFeroObj != null){
			defaultFero = defaultFeroObj.getPart("fero");
		}
	}
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}
	
	Obj3DPart main,defaultFero,sbire;
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Can transform voltage");
		list.add("The transform ratio is");
		list.add("defined by cables stacks size ratio");
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
		draw(defaultFero, 1, 4);
	}
	
	
	void draw(Obj3DPart fero,int priCableNbr,int secCableNbr)
	{
		if(main != null) main.draw();
		if(fero != null){
			fero.draw();
			if(priCableNbr != 0){
				GL11.glPushMatrix();
					float y = (priCableNbr - 1) * 1f/16f;
					GL11.glTranslatef(0f, -y, 0f);
					for(int idx = 0;idx<priCableNbr;idx++){
						sbire.draw();
						GL11.glTranslatef(0f, 2f/16f, 0f);
					}
				GL11.glPopMatrix();
			}
			if(secCableNbr != 0){
				GL11.glPushMatrix();
					GL11.glRotatef(180, 0f, 1f, 0f);
					float y = (secCableNbr - 1) * 1f/16f;
					GL11.glTranslatef(0f, -y, 0f);
					for(int idx = 0;idx<secCableNbr;idx++){
						sbire.draw();
						GL11.glTranslatef(0f, 2f/16f, 0f);
					}
				GL11.glPopMatrix();
			}
		
		}
	}

}
