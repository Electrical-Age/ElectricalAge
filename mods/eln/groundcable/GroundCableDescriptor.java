package mods.eln.groundcable;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;

public class GroundCableDescriptor extends SixNodeDescriptor{

	Obj3D obj;
	Obj3DPart main;
	
	public GroundCableDescriptor(String name,Obj3D obj) {
		super(name, GroundCableElement.class, GroundCableRender.class);
		this.obj = obj;
		if(obj != null){
			main = obj.getPart("main");
		}
	}
	
	void draw()
	{
		if(main != null) main.draw();
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Provide a zero volt reference");
		list.add("Can be used to put negative");
		list.add("battery pin to the ground");
	}
	
}
