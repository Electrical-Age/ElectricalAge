package mods.eln.sixnode.groundcable;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.Translator;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;

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
	public void setParent(Item item, int damage) {
		
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		
		super.addInformation(itemStack, entityPlayer, list, par4);

		list.add (Translator.translate("eln.core.tile.grndcable.hint0")); 
		list.add (Translator.translate("eln.core.tile.grndcable.hint1")); 
		list.add (Translator.translate("eln.core.tile.grndcable.hint2")); 
		list.add (Utils.plotOhm (Translator.translate("eln.core.tile.grndcable.hint3"), Eln.getSmallRs ()));
	}
	
}
