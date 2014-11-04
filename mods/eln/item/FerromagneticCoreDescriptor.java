package mods.eln.item;

import java.util.List;

import mods.eln.Translator;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sim.ElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;



public class FerromagneticCoreDescriptor  extends GenericItemUsingDamageDescriptorUpgrade
{
	public double cableMultiplicator;
	public Obj3DPart feroPart;
	Obj3D obj;
	public FerromagneticCoreDescriptor(
			String name,
			Obj3D obj,
			double cableMultiplicator
			) {
		super(name);
		this.obj = obj;
		if(obj != null){
			feroPart = obj.getPart("fero");
		}
		this.cableMultiplicator = cableMultiplicator;
	}

	
	public void applyTo(ElectricalLoad load)
	{
		load.setRs(load.getRs()*cableMultiplicator);
	}

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add(Translator.translate("eln.core.cableMultiplicator")+": " + cableMultiplicator);
	}
}
