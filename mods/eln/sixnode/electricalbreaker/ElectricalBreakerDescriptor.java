package mods.eln.sixnode.electricalbreaker;

import java.util.List;

import mods.eln.Translator;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class ElectricalBreakerDescriptor extends SixNodeDescriptor {

	private Obj3DPart main;
	private Obj3DPart lever;
	public ElectricalBreakerDescriptor(String name, Obj3D obj) {
		super(name, ElectricalBreakerElement.class, ElectricalBreakerRender.class);
		if(obj != null) {
			main = obj.getPart("case");
			lever = obj.getPart("lever");

			if(lever != null) {
				speed = lever.getFloat("speed");
				alphaOff = lever.getFloat("alphaOff");
				alphaOn = lever.getFloat("alphaOn");
			}
		}			
	}
	float alphaOff,alphaOn,speed;
	
	@Override	
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}
	@Override
	public boolean use2DIcon() {
		return false;
	}
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	@Override
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if(type == ItemRenderType.INVENTORY) GL11.glScalef(1.8f, 1.8f, 1.8f);
		draw(0f,0f);
	}
	
	public void draw(float on, float distance) {
		if(main != null)main.draw();
		if(lever != null) {
			lever.draw(on * (alphaOn - alphaOff) + alphaOff, 0, 1, 0);
		}		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add(Translator.translate("eln.core.tile.elbreaker.hint0"));
		list.add(Translator.translate("eln.core.tile.elbreaker.hint1")+":");
		list.add("- "+Translator.translate("eln.core.tile.elbreaker.hint2"));
		list.add("- "+Translator.translate("eln.core.tile.elbreaker.hint3"));
	}
}
