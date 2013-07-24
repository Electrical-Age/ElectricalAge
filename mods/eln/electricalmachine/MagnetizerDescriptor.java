package mods.eln.electricalmachine;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.item.EntityItem;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.FrameTime;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.RecipesList;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.sim.ThermalLoadInitializer;

public class MagnetizerDescriptor extends ElectricalMachineDescriptor{

	public MagnetizerDescriptor(String name, Obj3D obj,
			double nominalU, double nominalP,
			double maximalU, ThermalLoadInitializer thermal,
			ElectricalCableDescriptor cable, RecipesList recipe) {
		super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);
		this.obj = obj;
		if(obj != null)
		{
			rot = obj.getPart("rot");

			main = obj.getPart("main");
		}

	}
	Obj3D obj;
	Obj3DPart main,rot;		

	class MaceratorDescriptorHandle{
		float counter = 0;
		float itemCounter = 0;
		RcInterpolator interpolator = new RcInterpolator(0.5f);
	}
	@Override
	Object newDrawHandle() {
		// TODO Auto-generated method stub
		return new MaceratorDescriptorHandle();
	}
	@Override
	void draw(ElectricalMachineRender render,Object handleO,EntityItem inEntity, EntityItem outEntity, float powerFactor,float processState) {
		MaceratorDescriptorHandle handle = (MaceratorDescriptorHandle) handleO;
	
		main.draw();
		rot.draw(handle.counter, 0f, 0f, 1f);

		handle.interpolator.setTarget(powerFactor);
		handle.interpolator.stepGraphic();
		handle.counter += FrameTime.get() * handle.interpolator.get() *  360;
		while(handle.counter >= 360f) handle.counter -= 360;
		
	
		handle.itemCounter += FrameTime.get() *  90;
		while(handle.itemCounter >= 360f) handle.itemCounter -= 360;
				
		GL11.glScalef(0.5f, 0.5f, 0.5f);
		Utils.drawEntityItem(inEntity, 0.0, 0.25f, 0f, handle.itemCounter, 1f);
	}
	
	/*
	
	@Override
	public boolean powerLrdu(Direction side, Direction front) {
		return side != front && side != front.getInverse();
	}*/
	
	@Override
	public boolean drawCable() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	CableRenderDescriptor getPowerCableRender() {
		// TODO Auto-generated method stub
		return cable.render;
	}
}
