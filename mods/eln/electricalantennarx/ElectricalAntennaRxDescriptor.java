package mods.eln.electricalantennarx;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import org.lwjgl.opengl.GL11;

import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNode.FrontType;

public class ElectricalAntennaRxDescriptor extends TransparentNodeDescriptor{

	public ElectricalAntennaRxDescriptor(
			String name,Obj3D obj,
			double electricalNominalVoltage,double electricalNominalPower,
			double electricalMaximalVoltage,double electricalMaximalPower,
			ElectricalCableDescriptor cable
			) {
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
	public FrontType getFrontType() {
		// TODO Auto-generated method stub
		return FrontType.PlayerView;
	}
	
	@Override
	public boolean mustHaveWallFrontInverse() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean mustHaveFloor() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void draw()
	{
		GL11.glDisable(GL11.GL_CULL_FACE);
		if(main != null) main.drawList();
		GL11.glEnable(GL11.GL_CULL_FACE);
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
		draw();
	}	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
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
