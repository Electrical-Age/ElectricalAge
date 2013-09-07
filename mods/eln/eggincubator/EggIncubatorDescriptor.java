package mods.eln.eggincubator;

import java.util.List;

import javax.rmi.CORBA.Util;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeEntity;
import mods.eln.wiki.Data;

public class EggIncubatorDescriptor extends TransparentNodeDescriptor {
	Obj3D obj;
	Obj3D defaultFeroObj;
	public ElectricalCableDescriptor cable;
	private Obj3DPart lamp;
	private EntityItem eggEntity;
	private Obj3DPart lampf;
	public EggIncubatorDescriptor(
			String name,
			Obj3D obj,
			ElectricalCableDescriptor cable,
			double nominalVoltage,double nominalPower
			) {
		super(name, EggIncubatorElement.class,EggIncubatorRender.class);
		this.obj = obj;
		this.cable = cable;
		this.nominalVoltage = nominalVoltage;
		this.nominalPower = nominalPower;
		Rp = nominalVoltage*nominalVoltage/nominalPower;
		
		if(obj != null){
			main = obj.getPart("main");
			lamp = obj.getPart("lamp");
			lampf  = obj.getPart("lampf");
		}

		

	}
	
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addMachine(newItemStack());
	}
	
	Obj3DPart main;
	
	double nominalVoltage,nominalPower;
	double Rp;
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);

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
		// TODO Auto-generated method stub
		draw(0,1f);
	}
	
	
	void draw(int eggStackSize,float powerFactor)
	{
		if(eggStackSize == 0) powerFactor = 0f;
		Utils.disableCulling();
		if(main != null) main.draw();
		if(lampf != null){
			GL11.glColor3f(0.1f, 0.1f, 0.1f);
			lampf.draw();
		}
		if(lamp != null){

			Utils.disableLight();
			Utils.enableBlend();
			GL11.glColor4f(1f , 0.2f, 0.0f,powerFactor*powerFactor*0.5f);
			lamp.draw();
			Utils.disableBlend();
			Utils.enableLight();
		}
		Utils.enableCulling();
		

	}

	public void applyTo(NodeElectricalLoad powerLoad) {
		cable.applyTo(powerLoad, false);
	}
	
	public void setState(NodeElectricalLoad powerLoad,boolean enable)
	{
		if(enable)
			powerLoad.setRp(Rp);
		else
			powerLoad.setRp(1000000000.0);
		
	}

	
	
	@Override
	public void addCollisionBoxesToList(AxisAlignedBB par5AxisAlignedBB,
			List list, TransparentNodeEntity entity) {
		AxisAlignedBB bb = Block.stone.getCollisionBoundingBoxFromPool(entity.worldObj,entity.xCoord,entity.yCoord,entity.zCoord);
		bb.maxY-=0.5;
		if(par5AxisAlignedBB.intersectsWith(bb)) list.add(bb);
	}
}
