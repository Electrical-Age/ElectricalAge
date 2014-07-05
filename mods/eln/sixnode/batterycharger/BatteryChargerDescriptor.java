package mods.eln.sixnode.batterycharger;

import java.util.List;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class BatteryChargerDescriptor extends SixNodeDescriptor {

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
			double nominalVoltage, double nominalPower) {
		super(name, BatteryChargerElement.class, BatteryChargerRender.class);
		this.nominalVoltage = nominalVoltage;
		this.nominalPower = nominalPower;
		this.Rp = nominalVoltage * nominalVoltage / nominalPower;
		this.obj = obj;
		this.cable = cable;
		if (obj != null) {
			main = obj.getPart("main");
			for (int idx = 0; idx < 4; idx++) {
				leds[idx] = obj.getPart("led" + idx);
			}
		}
	}

	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addEnergy(newItemStack());
		Data.addUtilities(newItemStack());
	}

	Obj3DPart[] leds = new Obj3DPart[4];

	public void draw(boolean[] presence, boolean[] charged) {
		if (main != null)
			main.draw();

		int idx = 0;
		for (Obj3DPart led : leds) {
			if (presence != null && presence[idx]) {
				UtilsClient.ledOnOffColor(charged[idx]);
				UtilsClient.drawLight(led);
			}
			else {
				GL11.glColor3f(0.2f, 0.2f, 0.2f);
				led.draw();
			}
			idx++;
		}
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

	//boolean[] defaultCharged = new boolean[]{true, true, true, true};

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if (type == ItemRenderType.INVENTORY) {
			GL11.glScalef(1.5f, 1.5f, 1.5f);
			GL11.glTranslatef(-0.2f, 0.0f, 0f);
		}
		draw(null, null);
	}

	public void applyTo(NodeElectricalLoad powerLoad) {
		cable.applyTo(powerLoad);
	}

	public void setRp(Resistor powerload, boolean powerOn)
	{
		
		if (powerOn == false)
			powerload.highImpedance();
		else
			powerload.setR(Rp);
		
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add("Can be used to recharge");
		list.add("some electrical items like");
		list.add("Flash Light, Xray scanner,");
		list.add("Portable Pattery ..");
	}
}
