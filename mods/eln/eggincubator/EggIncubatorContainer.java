package mods.eln.eggincubator;

import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.ItemStackFilter;
import mods.eln.SlotFilter;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.node.SixNodeItemSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;

public class EggIncubatorContainer extends BasicContainer {
	public static final int EggSlotId = 0;

	
	public EggIncubatorContainer(EntityPlayer player, IInventory inventory) {
		
		super(player, inventory,new Slot[]{
				new SlotFilter(inventory,EggSlotId,0,0,64,new ItemStackFilter[]{new ItemStackFilter(Item.egg)},SlotSkin.medium,new String[]{"Egg slot"})
				
				//	new SlotFilter(inventory,1,62 + 18,17,1,new ItemStackFilter[]{new ItemStackFilter(Eln.sixNodeBlock,0xFF,Eln.electricalCableId)})
			});
		
		// TODO Auto-generated constructor stub
	}


}
