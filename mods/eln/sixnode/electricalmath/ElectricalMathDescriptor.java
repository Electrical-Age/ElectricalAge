package mods.eln.sixnode.electricalmath;

import java.util.List;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class ElectricalMathDescriptor extends SixNodeDescriptor {

	public float[] pinDistance;

	public ElectricalMathDescriptor(String name, Obj3D obj) {
		super(name, ElectricalMathElement.class, ElectricalMathRender.class);
		this.obj = obj;
		if(obj != null) {
			main = obj.getPart("main");
			door = obj.getPart("door");
			if(door != null) {
				alphaOff = door.getFloat("alphaOff");
			}
			for(int idx = 0; idx < 8; idx++) {
				led[idx] = obj.getPart("led" + idx);
			}
			

			pinDistance = Utils.getSixNodePinDistance(main);
		}
	}

	Obj3D obj;
	Obj3DPart main,door;
	Obj3DPart led[] = new Obj3DPart[8];
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
	}
	
	float alphaOff;
	@Override
	public boolean use2DIcon() {
		return false;
	}
	void draw(float open,boolean ledOn[]) {
		if(main != null) main.draw();
		if(door != null) door.draw((1f - open) * alphaOff, 0f, 1f, 0f);
		
		for(int idx = 0; idx < 8; idx++) {
			if(ledOn[idx]) {
				if((idx & 3) == 0)
					GL11.glColor3f(0.8f, 0f, 0f);
				else
					GL11.glColor3f(0f, 0.8f, 0f);
				UtilsClient.drawLight(led[idx]);		
			}
			else {
				GL11.glColor3f(0.3f, 0.3f, 0.3f);
				led[idx].draw();
			}
		}
	}
	
	static boolean[] ledDefault = {true,false,true,false,true,true,true,false};
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	@Override
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		GL11.glTranslatef(-0.3f, -0.1f, 0f);
		GL11.glRotatef(90, 1, 0, 0);
		draw(0.7f, ledDefault);
	}
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add("Calculate a output signal from");
		list.add("3 inputs (A,B,C) and one equation");
	}
}
