package mods.eln.electricalrelay;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.electricalcable.ElectricalCableDescriptor;
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


public class ElectricalRelayDescriptor extends SixNodeDescriptor{

	public ElectricalRelayDescriptor(		
					String name,
					ElectricalCableDescriptor cable
					) {
			super(name, ElectricalRelayElement.class, ElectricalRelayRender.class);
			this.cable = cable;

		}
	ElectricalCableDescriptor cable;
	
	
	void applyTo(ElectricalLoad load)
	{
		cable.applyTo(load,false);
	}
	void applyTo(ElectricalResistor load)
	{
		cable.applyTo(load);
	}
}
