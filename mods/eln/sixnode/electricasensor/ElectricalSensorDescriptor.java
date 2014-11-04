package mods.eln.sixnode.electricasensor;

import java.util.List;

import mods.eln.Eln;
import mods.eln.Translator;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;


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
		if(main != null) main.draw();
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
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw();
	}*/
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		if(voltageOnly){
			list.add(Translator.translate("eln.core.tile.elsensor.hint0"));
			list.add(Translator.translate("eln.core.tile.elsensor.hint1"));
		}
		else
		{
			list.add(Translator.translate("eln.core.tile.elsensor.hint0"));
			list.add(Translator.translate("eln.core.tile.elsensor.hint2")+":");
			list.add("	"+Translator.translate("eln.core.tile.elsensor.hint3"));
			list.add(Translator.translate("eln.core.tile.elsensor.hint1"));
		}
	}
}
