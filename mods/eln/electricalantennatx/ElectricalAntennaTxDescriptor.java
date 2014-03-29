package mods.eln.electricalantennatx;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import org.lwjgl.opengl.GL11;

import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNode.FrontType;
import mods.eln.wiki.Data;

public class ElectricalAntennaTxDescriptor extends TransparentNodeDescriptor {

	public ElectricalAntennaTxDescriptor(
			String name, Obj3D obj,
			int rangeMax,
			double electricalPowerRatioEffStart, double electricalPowerRatioEffEnd,
			double electricalNominalVoltage, double electricalNominalPower,
			double electricalMaximalVoltage, double electricalMaximalPower,
			ElectricalCableDescriptor cable) {
		super(name, ElectricalAntennaTxElement.class, ElectricalAntennaTxRender.class);
		this.rangeMax = rangeMax;
		this.electricalNominalVoltage = electricalNominalVoltage;
		this.electricalNominalPower = electricalNominalPower;
		this.electricalMaximalVoltage = electricalMaximalVoltage;
		this.electricalMaximalPower = electricalMaximalPower;
		this.electricalPowerRatioEffStart = electricalPowerRatioEffStart;
		this.electricalPowerRatioEffEnd = electricalPowerRatioEffEnd;
		this.cable = cable;
		
		electricalPowerRatioLostOffset = 1.0 - electricalPowerRatioEffStart;
		electricalPowerRatioLostPerBlock = (electricalPowerRatioEffStart - electricalPowerRatioEffEnd) / rangeMax;
		
		electricalNominalInputR = electricalNominalVoltage * electricalNominalVoltage / electricalNominalPower;
		
		this.obj = obj;
		if(obj != null) main = obj.getPart("main");
	}
	
	@Override	
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}
	
	Obj3D obj;
	Obj3DPart main;
	
	@Override
	public FrontType getFrontType() {
		return FrontType.PlayerView;
	}
	
	@Override
	public boolean mustHaveWallFrontInverse() {
		return true;
	}
	
	@Override
	public boolean mustHaveFloor() {
		return false;
	}
	
	int rangeMax;
	double electricalPowerRatioEffStart, electricalPowerRatioEffEnd;
	double electricalPowerRatioLostOffset, electricalPowerRatioLostPerBlock;
	double electricalNominalVoltage, electricalNominalPower;
	double electricalMaximalVoltage, electricalMaximalPower;
	double electricalNominalInputR;
	ElectricalCableDescriptor cable;
	
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
		
	public void draw() {
		GL11.glDisable(GL11.GL_CULL_FACE);
		if(main != null) main.draw();
		GL11.glEnable(GL11.GL_CULL_FACE);
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Wireless power transmitter");
		list.add("Nominal usage");
		list.add(Utils.plotVolt(" U :", electricalNominalVoltage));
		list.add(Utils.plotPower(" P :", electricalNominalPower));
		list.add("Range : " + rangeMax + " Blocks");
		list.add("Efficiency : " + (int)(electricalPowerRatioEffEnd * 100) + "% to " + (int)(electricalPowerRatioEffStart * 100) + "%" );
	}
}
