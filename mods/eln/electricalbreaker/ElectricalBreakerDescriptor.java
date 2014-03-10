package mods.eln.electricalbreaker;

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
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import com.google.common.base.Function;


public class ElectricalBreakerDescriptor extends SixNodeDescriptor{

	private Obj3D obj;
	private Obj3DPart main;
	private Obj3DPart lever;
	private Obj3DPart led;

	public ElectricalBreakerDescriptor(		
					String name,Obj3D obj
					) 
	{
		super(name, ElectricalBreakerElement.class, ElectricalBreakerRender.class);
		this.obj = obj;
		if(obj != null){
			main = obj.getPart("case");
			lever = obj.getPart("lever");

	
			if(lever != null){
				speed = lever.getFloat("speed");
				alphaOff = lever.getFloat("alphaOff");
				alphaOn = lever.getFloat("alphaOn");
			}

		}			
	}
	float alphaOff,alphaOn,speed;
	
	

	@Override	
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return true;
	}	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		// TODO Auto-generated method stub
		
		if(type == ItemRenderType.INVENTORY) GL11.glScalef(1.8f, 1.8f, 1.8f);
		draw(0f,0f);
	}	
	public void draw(float on,float distance) {
		if(main != null)main.draw();

		if(lever != null) {
			float switchDelta;			
			lever.draw(on*(alphaOn - alphaOff) + alphaOff, 0, 1, 0);
		}		
	
	}
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Protects other electrical components");
		list.add("Cut off when:");
		list.add("- Voltage reach a defined level");
		list.add("- Current reach the cable limit");
	}
}

