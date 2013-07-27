package mods.eln.item;

import java.util.List;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.lampsocket.LampSocketType;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;



public class FerromagneticCoreDescriptor  extends GenericItemUsingDamageDescriptor
{
	private double cableMultiplicator;
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

	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("cableMultiplicator : " + cableMultiplicator);
	}
}
