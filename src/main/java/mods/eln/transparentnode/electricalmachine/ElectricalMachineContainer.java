package mods.eln.transparentnode.electricalmachine;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.SlotWithSkin;
import mods.eln.item.MachineBoosterDescriptor;
import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ElectricalMachineContainer extends BasicContainer implements INodeContainer {

	NodeBase node = null;
	
	public ElectricalMachineContainer(NodeBase node, EntityPlayer player, IInventory inventory,ElectricalMachineDescriptor descriptor) {
		super(player, inventory, getSlot(inventory, descriptor));
		
		/*super(player, inventory, new Slot[]{
				new SlotWithSkin(inventory, outSlotId, 130, 12, SlotSkin.big),
				new SlotWithSkin(inventory, inSlotId, 70, 12, SlotSkin.medium),
				new GenericItemUsingDamageSlot(inventory, boosterSlotId, 20, 12, 5,
												MachineBoosterDescriptor.class,
												SlotSkin.medium,
												new String[]{"Booster Slot"}),
			});*/
		

		this.node = node;
	}

	static Slot[] getSlot(IInventory inventory,ElectricalMachineDescriptor descriptor){
		Slot[] slots = new Slot[2 + descriptor.outStackCount];
		for(int idx = 0;idx < descriptor.outStackCount;idx++){
			slots[0 + idx]  = new SlotWithSkin(inventory, 0 + idx, 130-32+idx*18, 12, SlotSkin.medium);
		}
		slots[descriptor.outStackCount + 0] = new SlotWithSkin(inventory, descriptor.outStackCount + 0, 8+36, 12, SlotSkin.medium);
		slots[descriptor.outStackCount + 1] = new GenericItemUsingDamageSlot(inventory, descriptor.outStackCount + 1, 8, 12, 5,
				MachineBoosterDescriptor.class,
				SlotSkin.medium,
				new String[]{"Booster Slot"});
		
		return slots;
	}
	
	@Override
	public NodeBase getNode() {
		return node;
	}

	@Override
	public int getRefreshRateDivider() {
		return 1;
	}
}
/*				new SlotFilter(inventory, 0, 62 +  0, 17, new ItemStackFilter[]{new ItemStackFilter(Block.wood, 0, 0)}),
				new SlotFilter(inventory, 1, 62 + 18, 17, new ItemStackFilter[]{new ItemStackFilter(Item.coal, 0, 0)})
*/