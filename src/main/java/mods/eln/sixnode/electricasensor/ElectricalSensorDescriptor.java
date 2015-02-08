package mods.eln.sixnode.electricasensor;

import mods.eln.Eln;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ElectricalSensorDescriptor extends SixNodeDescriptor {

    boolean voltageOnly;
    Obj3DPart main;

	public ElectricalSensorDescriptor(		
					String name,String modelName,
					boolean voltageOnly) {
        super(name, ElectricalSensorElement.class, ElectricalSensorRender.class);
        this.voltageOnly = voltageOnly;
        main = Eln.obj.getPart(modelName, "main");
	}

	void draw() {
		if (main != null) main.draw();
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
	}
	/*
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw();
	}*/

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		if (voltageOnly) {
			list.add("Probe voltage value on a cable.");
			list.add("Has a signal output.");
		} else {
			list.add("Probe electrical values on cables.");
			list.add("Can measure:");
			list.add("  Voltage/Power/Current");
			list.add("Has a signal output.");
		}
	}
}
