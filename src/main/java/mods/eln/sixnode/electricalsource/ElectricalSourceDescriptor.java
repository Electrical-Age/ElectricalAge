package mods.eln.sixnode.electricalsource;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;

public class ElectricalSourceDescriptor extends SixNodeDescriptor {

	private Obj3D obj;
	private Obj3DPart main;

	public ElectricalSourceDescriptor(String name, Obj3D obj) {
		super(name, ElectricalSourceElement.class, ElectricalSourceRender.class);
		this.obj = obj;
		if (obj != null) {
			main = obj.getPart("main");
		}
	}
	
	void draw() {
		if (main != null) main.draw();
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Provides a stable voltage source");
		list.add("without energy/power limitation.");
        list.add("");
		list.add(Utils.plotOhm("Internal resistance :", Eln.instance.lowVoltageCableDescriptor.electricalRs));
        list.add("");
		list.add("Creative block.");
	}
}
