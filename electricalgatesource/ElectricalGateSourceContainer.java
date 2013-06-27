package mods.eln.electricalgatesource;

import mods.eln.BasicContainer;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.item.LampSlot;
import mods.eln.node.SixNodeItemSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ElectricalGateSourceContainer extends BasicContainer {


	public ElectricalGateSourceContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
			});
		
		// TODO Auto-generated constructor stub
	}

}
