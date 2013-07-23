package mods.eln.electricalmachine;

import mods.eln.client.FrameTime;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.electricalmachine.MaceratorDescriptor.MaceratorDescriptorHandle;
import mods.eln.misc.Obj3D;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.RecipesList;
import mods.eln.sim.ThermalLoadInitializer;
import net.minecraft.entity.item.EntityItem;

public class PlateMachineDescriptor extends ElectricalMachineDescriptor{

	public PlateMachineDescriptor(String name, 
			Obj3D obj,			
			double nominalU, double nominalP,
			double maximalU, ThermalLoadInitializer thermal,
			ElectricalCableDescriptor cable, RecipesList recipe) {
		super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);
		
		
		this.obj = obj;
		if(obj != null){
			main = obj.getPart("main");
			rot1 = obj.getPart("rot1");
			rot2 = obj.getPart("rot2");
		}		
	}


	Obj3D obj;
	Obj3DPart main,rot1,rot2;
	
	class CompressorDescriptorHandle{
		float counter = 0;
		RcInterpolator interpolator = new RcInterpolator(0.5f);
		float itemCounter = 0;
	}
	
	@Override
	Object newDrawHandle() {
		// TODO Auto-generated method stub
		return new CompressorDescriptorHandle();
	}


	@Override
	void draw(ElectricalMachineRender render,Object handleO,EntityItem inEntity, EntityItem outEntity, float powerFactor)
	{
		CompressorDescriptorHandle handle = (CompressorDescriptorHandle) handleO;
		
		main.draw();
		rot1.draw(handle.counter, 0f, 0f, -1f);
		rot2.draw(handle.counter, 0f, 0f, 1f);
		
		handle.interpolator.setTarget(powerFactor);
		handle.interpolator.stepGraphic();
		handle.counter += FrameTime.get() * handle.interpolator.get() *  360;
		while(handle.counter >= 360f) handle.counter -= 360;
		
		handle.itemCounter += FrameTime.get() *  90;
		while(handle.itemCounter >= 360f) handle.itemCounter -= 360;
				
		Utils.drawEntityItem(inEntity, -0.35f, 0.1f, 0f, handle.itemCounter, 1f);
		Utils.drawEntityItem(outEntity, 0.35f, 0.1f, 0f, -handle.itemCounter + 139f, 1f);
	
	}
}
