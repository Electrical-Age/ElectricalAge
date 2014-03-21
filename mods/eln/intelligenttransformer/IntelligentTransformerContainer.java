package mods.eln.intelligenttransformer;

import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.node.SixNodeItemSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class IntelligentTransformerContainer extends BasicContainer {

	public IntelligentTransformerContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new SixNodeItemSlot(inventory,0,62 +  0,17,1,new Class[]{ElectricalCableDescriptor.class},SlotSkin.medium,new String[]{"Electrical Cable Slot"}),
				new SixNodeItemSlot(inventory,1,62 + 18,17,1,new Class[]{ElectricalCableDescriptor.class},SlotSkin.medium,new String[]{"Electrical Cable Slot"}),
				new GenericItemUsingDamageSlot(inventory,2,62 + 0,17 + 18,1,new Class[]{FerromagneticCoreDescriptor.class},SlotSkin.medium,new String[]{"Ferromagnetic Core Slot"})
			
				//	new SlotFilter(inventory,1,62 + 18,17,1,new ItemStackFilter[]{new ItemStackFilter(Eln.sixNodeBlock,0xFF,Eln.electricalCableId)})
			});
		
		// TODO Auto-generated constructor stub
	}

}
