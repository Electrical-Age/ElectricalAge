package mods.eln.electricalmachine;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.item.EntityItem;
import mods.eln.Eln;
import mods.eln.client.FrameTime;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.RecipesList;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sim.ThermalLoadInitializer;

public class MaceratorDescriptor extends ElectricalMachineDescriptor{

	public MaceratorDescriptor(String name, String modelName,
			double nominalU, double nominalP,
			double maximalU, ThermalLoadInitializer thermal,
			ElectricalCableDescriptor cable, RecipesList recipe) {
		super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);
		obj = Eln.obj.getObj(modelName);
		if(obj != null)
		{
			rouleau1 = obj.getPart("rouleau1");
			rouleau2 = obj.getPart("rouleau2");
			main = obj.getPart("main");
		}

	}
	Obj3D obj;
	Obj3DPart main,rouleau1,rouleau2;		

	class MaceratorDescriptorHandle{
		float counter = 0;
		RcInterpolator interpolator = new RcInterpolator(0.5f);
	}
	@Override
	Object newDrawHandle() {
		// TODO Auto-generated method stub
		return new MaceratorDescriptorHandle();
	}
	@Override
	void draw(ElectricalMachineRender render,Object handleO,EntityItem inEntity, EntityItem outEntity, float powerFactor) {
		MaceratorDescriptorHandle handle = (MaceratorDescriptorHandle) handleO;
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		
		main.drawList();
		rouleau1.draw(handle.counter, 1f, 0f, 0f);
		rouleau2.draw(handle.counter, -1f, 0f, 0f);
		
		handle.interpolator.setTarget(powerFactor);
		handle.interpolator.stepGraphic();
		handle.counter += FrameTime.get() * handle.interpolator.get() *  360;
		while(handle.counter >= 360f) handle.counter -= 360;
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		
	}
}
