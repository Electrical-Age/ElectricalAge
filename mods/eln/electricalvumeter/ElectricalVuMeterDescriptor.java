package mods.eln.electricalvumeter;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.Eln;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;

import com.google.common.base.Function;


public class ElectricalVuMeterDescriptor extends SixNodeDescriptor{


	public ElectricalVuMeterDescriptor(
			String name,
			String objName
			) {
		super(name, ElectricalVuMeterElement.class,ElectricalVuMeterRender.class);
		obj = Eln.instance.obj.getObj(objName);
		if(obj.getString("type").toLowerCase().equals("rot")){
			objType = ObjType.Rot;
			vumeter = obj.getPart("Vumeter");
			pointer = obj.getPart("Pointer");
		}
		if(obj.getString("type").equals("LedOnOff")){
			objType = ObjType.LedOnOff;
			main = obj.getPart("main");
			halo = obj.getPart("halo");
		}
	}

	Obj3D obj;
	
	enum ObjType{Rot,LedOnOff};
	ObjType objType;

	Obj3DPart vumeter,pointer,led,halo,main;
	void draw(float factor,float distance)
	{
		if(factor < 0.0) factor = 0.0f;
		if(factor > 1.0) factor = 1.0f;
		switch(objType)
		{
		case LedOnOff:
			
			main.draw();
			Utils.ledOnOffColor(factor > 0.5);
			//Utils.enableBilinear();
			Utils.drawHalo(halo,distance);
			//Utils.disableBilinear();

			break;
		case Rot:
			vumeter.draw();
			float alphaOff,alphaOn;
			alphaOff = pointer.getFloat("alphaOff");
			alphaOn = pointer.getFloat("alphaOn");
			pointer.draw((factor*(alphaOn-alphaOff) + alphaOff), 1.0f, 0, 0);
			break;
		default:
			break;

		}
	}
	
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Display the value of a signal");
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
		if(type == ItemRenderType.INVENTORY) GL11.glRotatef(90, 1f, 0f, 0f);
		draw(0.0f,1f);
	}
	
}
