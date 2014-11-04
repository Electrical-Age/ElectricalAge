package mods.eln.transparentnode.transformer;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import mods.eln.Translator;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sound.SoundCommand;
import mods.eln.wiki.Data;

public class TransformerDescriptor extends TransparentNodeDescriptor {
	Obj3D obj;
	Obj3D defaultFeroObj;
	
	public float minimalLoadToHum;
	public SoundCommand highLoadSound;
	
	public TransformerDescriptor(
			String name,
			Obj3D obj,
			Obj3D defaultFeroObj,
			float minimalLoadToHum
			) {
		super(name, TransformerElement.class,TransformerRender.class);
		this.obj = obj;
		this.defaultFeroObj = defaultFeroObj;
		this.minimalLoadToHum = minimalLoadToHum;
		
		if(obj != null){
			main = obj.getPart("main");
			sbire = obj.getPart("sbire");
		}
		if(defaultFeroObj != null){
			defaultFero = defaultFeroObj.getPart("fero");
		}
		
		highLoadSound = new SoundCommand("eln:Transformer", 1.6f);
	}
	@Override
	public void setParent(Item item, int damage) {
		
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}
	
	Obj3DPart main,defaultFero,sbire;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add(Translator.translate("eln.core.tile.transformer.hint0"));
		list.add(Translator.translate("eln.core.tile.transformer.hint1"));
		list.add(Translator.translate("eln.core.tile.transformer.hint2"));
		list.add(Translator.translate("eln.core.tile.transformer.hint3"));
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		
		return true;
	}
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		
		return true;
	}
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		
		draw(defaultFero, 1, 4);
	}
	
	@Override
	public boolean use2DIcon() {
		return false;
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
