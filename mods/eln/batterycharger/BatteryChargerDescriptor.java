package mods.eln.batterycharger;

import org.lwjgl.opengl.GL11;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.SixNodeDescriptor;

public class BatteryChargerDescriptor extends SixNodeDescriptor{

	private Obj3D obj;
	Obj3DPart main;
	private double nominalVoltage;
	private double nominalPower;
	private ElectricalCableDescriptor cable;
	private double Rp;


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

		}
	}
	public int range;
	
	
	public void draw()
	{
		if(main != null) main.draw();

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
		if(type == ItemRenderType.INVENTORY) {
			GL11.glScalef(1.5f, 1.5f, 1.5f);
			GL11.glTranslatef(-0.2f, 0.0f, 0f);
		}
		draw();
	}
	public void applyTo(NodeElectricalLoad powerLoad,boolean powerOn) {
		cable.applyTo(powerLoad, false);
		powerLoad.setRp(Rp);
		if(powerOn == false)
			powerLoad.setRs(1000000000.0);
	}
	
}
