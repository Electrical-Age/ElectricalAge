package mods.eln.electricasensor;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ThermalLoadInitializer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import com.google.common.base.Function;


public class ElectricalSensorDescriptor extends SixNodeDescriptor{

	public ElectricalSensorDescriptor(		
					String name,String modelName,
					boolean voltageOnly
					) {
			super(name, ElectricalSensorElement.class, ElectricalSensorRender.class);
			this.voltageOnly = voltageOnly;
			main = Eln.obj.getPart(modelName, "main");
		}
	boolean voltageOnly;
	Obj3DPart main;
	
	
	void draw()
	{
		if(main != null) main.drawList();
	}
}
