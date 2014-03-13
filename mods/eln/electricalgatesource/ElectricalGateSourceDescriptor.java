package mods.eln.electricalgatesource;

import java.awt.Color;
import java.util.List;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;

import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import com.google.common.base.Function;


public class ElectricalGateSourceDescriptor extends SixNodeDescriptor{

	private Obj3DPart rot;
	private Obj3DPart main;
	private Obj3D obj;
	private float rotAlphaOn,rotAlphaOff;
	public boolean onOffOnly;
	private Obj3DPart lever;
	private Obj3DPart led;
	private Obj3DPart halo;
	public float speed;
	public ElectricalGateSourceDescriptor(		
					String name,Obj3D obj,
					boolean onOffOnly
					) {
			super(name, ElectricalGateSourceElement.class, ElectricalGateSourceRender.class);
			this.obj = obj;
			if(obj != null){
				main = obj.getPart("main");


				if(obj.getString("type").equals("pot")){
					objType = ObjType.Pot;
					rot = obj.getPart("rot");
					if(rot != null){
						rotAlphaOff = rot.getFloat("alphaOff");
						rotAlphaOn = rot.getFloat("alphaOn");
						speed = rot.getFloat("speed");
					}
										
				}
				
				if(obj.getString("type").equals("button")){
					lever = obj.getPart("button");
					led = obj.getPart("led");
					halo = obj.getPart("halo");	

					objType = ObjType.Button;
					if(lever != null){
						speed = lever.getFloat("speed");
						leverTx = lever.getFloat("tx");
					}
				}
			}
			this.onOffOnly = onOffOnly;
		}
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("A weak adjustable voltage source.");
		list.add("Provides signal voltage.");
	}
	enum ObjType {Pot,Button};
	ObjType objType;
	float leverTx;
	void draw(float factor,float distance,TileEntity e)
	{
		switch (objType) {
		case Button:
			
			
			if(main != null)main.draw();
			
			GL11.glTranslatef(leverTx*factor, 0f, 0f);
			if(lever != null) lever.draw();
			
		//	if(factor < 0.5f){
			//	GL11.glColor3f(234f/255f, 80/255f, 0f);
			Utils.ledOnOffColor(factor > 0.5f);
				Utils.disableLight();
				if(led != null) led.draw();
				Utils.enableBlend();
				
				if(halo != null){
	
					if(e == null)
						Utils.drawLight(halo);
					else{
						Color c = Utils.ledOnOffColorC(factor > 0.5f);
						Utils.drawHaloNoLightSetup(halo,c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,e,false);
					}
				}
			
				Utils.disableBlend();
				Utils.enableLight();
		//	}
		/*	else
			{
				if(led != null) led.draw();
			}*/

			break;
		case Pot:
			if(main != null) main.draw();
			if(rot != null) rot.draw(factor * (rotAlphaOn - rotAlphaOff) + rotAlphaOff,1f,0f,0f);
			break;
		}
	}
	
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
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
		GL11.glScalef(1.5f, 1.5f, 1.5f);
		if(type == ItemRenderType.INVENTORY) GL11.glScalef(1.5f, 1.5f, 1.5f);
		draw(0f,1f,null);
	}
}
