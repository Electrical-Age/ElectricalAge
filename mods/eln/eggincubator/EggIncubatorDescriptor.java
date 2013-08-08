package mods.eln.eggincubator;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.TransparentNodeDescriptor;

public class EggIncubatorDescriptor extends TransparentNodeDescriptor {
	Obj3D obj;
	Obj3D defaultFeroObj;
	public ElectricalCableDescriptor cable;
	private Obj3DPart lamp;
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
		
		}

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
		draw(0);
	}
	
	
	void draw(int eggStackSize)
	{
		Utils.disableCulling();
		if(main != null) main.draw();
		if(lamp != null) lamp.draw();
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

}
