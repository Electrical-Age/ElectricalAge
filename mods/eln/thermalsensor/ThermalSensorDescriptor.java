package mods.eln.thermalsensor;

import java.util.List;

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
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import com.google.common.base.Function;


public class ThermalSensorDescriptor extends SixNodeDescriptor{
	public boolean temperatureOnly;
	public ThermalSensorDescriptor(		
					String name,Obj3D obj,
					boolean temperatureOnly
					) {
		super(name, ThermalSensorElement.class, ThermalSensorRender.class);
		this.temperatureOnly = temperatureOnly;
		this.obj = obj;
		if(obj != null){
			main = obj.getPart("main");
		}
	}
	
	Obj3D obj;
	Obj3DPart main;
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		if(temperatureOnly){
			list.add("Probe temperature value on a cable");
			list.add("Has a signal output");
		}
		else
		{
			list.add("Probe thermal values on cables");
			list.add("Can measure:");
			list.add("  Temperature/Power conducted");
			list.add("Has a signal output");
		}

		
	}
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addThermal(newItemStack());
		Data.addSignal(newItemStack());
	}
	
	void draw()
	{
		if(main != null) main.draw();
	}

}
