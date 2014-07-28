package mods.eln.transparentnode.electricalantennarx;

import java.util.List;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNode.FrontType;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class ElectricalAntennaRxDescriptor extends TransparentNodeDescriptor {

	public ElectricalAntennaRxDescriptor(
			String name, Obj3D obj,
			double electricalNominalVoltage, double electricalNominalPower,
			double electricalMaximalVoltage, double electricalMaximalPower,
			ElectricalCableDescriptor cable) {
		super(name, ElectricalAntennaRxElement.class, ElectricalAntennaRxRender.class);
		this.electricalNominalVoltage = electricalNominalVoltage;
		this.electricalNominalPower = electricalNominalPower;
		this.electricalMaximalVoltage = electricalMaximalVoltage;
		this.electricalMaximalPower = electricalMaximalPower;
		this.cable = cable;
		
		this.obj = obj;
		if(obj != null) main = obj.getPart("main");
	}
	
	Obj3D obj;
	Obj3DPart main;
	
	@Override	
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}
	
	@Override
	public FrontType getFrontType() {
		return FrontType.PlayerView;
	}
	
	@Override
	public boolean mustHaveWallFrontInverse() {
		return true;
	}
	@Override
	public boolean use2DIcon() {
		return false;
	}
	@Override
	public boolean mustHaveFloor() {
		return false;
	}
	
	public void draw() {
		GL11.glDisable(GL11.GL_CULL_FACE);
		if(main != null) main.draw();
		GL11.glEnable(GL11.GL_CULL_FACE);
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
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw();
	}	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Wireless power receiver");
		list.add("Nominal usage");
		list.add(Utils.plotVolt(" U :", electricalNominalVoltage));
		list.add(Utils.plotPower(" P :", electricalNominalPower));
	}
	double electricalNominalVoltage, electricalNominalPower;
	double electricalMaximalVoltage, electricalMaximalPower;
	double electricalNominalInputR;
	ElectricalCableDescriptor cable;
}
