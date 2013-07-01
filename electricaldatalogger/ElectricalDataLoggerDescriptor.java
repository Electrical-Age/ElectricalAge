package mods.eln.electricaldatalogger;

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


public class ElectricalDataLoggerDescriptor extends SixNodeDescriptor{

	public ElectricalDataLoggerDescriptor(		
					String name,
					String objName
					) {
		super(name, ElectricalDataLoggerElement.class, ElectricalDataLoggerRender.class);
		obj = Eln.obj.getObj(objName);
		if(obj != null)
		{
			main = obj.getPart("main");
		}
	}
	Obj3D obj;
	Obj3DPart main;

}
