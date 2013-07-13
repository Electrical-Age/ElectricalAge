package mods.eln.electricalredstoneinput;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;

import com.google.common.base.Function;


public class ElectricalRedstoneInputDescriptor extends SixNodeDescriptor{


	public ElectricalRedstoneInputDescriptor(
			String name,
			String objName
			) {
		super(name, ElectricalRedstoneInputElement.class,ElectricalRedstoneInputRender.class);
		//obj = Eln.instance.obj.getObj(objName);

	}

	Obj3D obj;

	void draw(float factor)
	{
		
	}
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Convert redstone signal");
		list.add("to voltage signal");
	}
}
