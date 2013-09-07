package mods.eln.batterycharger;

import org.lwjgl.opengl.GL11;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.wiki.Data;

public class BatteryChargerDescriptor extends SixNodeDescriptor{

	private Obj3D obj;
	Obj3DPart main;
	public double nominalVoltage;
	public double nominalPower;
	public ElectricalCableDescriptor cable;
	double Rp;


	public BatteryChargerDescriptor(
			String name,
			Obj3D obj,
			ElectricalCableDescriptor cable,
			double nominalVoltage,double nominalPower
			) {
		super(name, BatteryChargerElement.class, BatteryChargerRender.class);
		this.nominalVoltage = nominalVoltage;
		this.nominalPower = nominalPower;
		this.Rp = nominalVoltage*nominalVoltage/nominalPower;
		this.obj = obj;
		this.cable = cable;
		if(obj != null) {
			main = obj.getPart("main");
			for(int idx = 0;idx < 4;idx++){
				leds[idx] = obj.getPart("led" + idx);
			}
		}
	}
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addEnergy(newItemStack());
		Data.addUtilities(newItemStack());
	}
	
	Obj3DPart [] leds = new Obj3DPart[4];
	
	public void draw(boolean [] presence,boolean [] charged)
	{
		if(main != null) main.draw();
		
		int idx = 0;
		for(Obj3DPart led : leds){
			if(presence != null && presence[idx]){
				Utils.ledOnOffColor(charged[idx]);
				Utils.drawLight(led);
			}
			else{
				GL11.glColor3f(0.2f,0.2f,0.2f);
				led.draw();	
			}
			idx++;
		}

	
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
	//boolean[] defaultCharged = new boolean[]{true,true,true,true};
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if(type == ItemRenderType.INVENTORY) {
			GL11.glScalef(1.5f, 1.5f, 1.5f);
			GL11.glTranslatef(-0.2f, 0.0f, 0f);
		}
		draw(null,null);
	}
	public void applyTo(NodeElectricalLoad powerLoad) {
		cable.applyTo(powerLoad, false);
	}
	
	public void setRp(NodeElectricalLoad powerload,boolean powerOn)
	{
		powerload.setRp(Rp);
		if(powerOn == false)
			powerload.setRp(1000000000000000000000.0);
	}
}
