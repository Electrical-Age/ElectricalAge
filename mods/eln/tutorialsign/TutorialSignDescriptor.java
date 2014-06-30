package mods.eln.tutorialsign;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;

public class TutorialSignDescriptor extends SixNodeDescriptor {

	private Obj3D obj;
	private Obj3DPart main;

	public TutorialSignDescriptor(String name, Obj3D obj) {
		super(name, TutorialSignElement.class, TutorialSignRender.class);
		this.obj = obj;
		if(obj != null) {
			main = obj.getPart("main");
		}
	}
	
	void draw() {
		if(main != null) main.draw();
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		//list.add("");
	}
}
