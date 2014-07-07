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
	public static final int inSlotId = 0, outSlotId = 1, boosterSlotId = 2;
	public ElectricalMachineContainer(NodeBase node, EntityPlayer player, IInventory inventory) {
		super(player, inventory, new Slot[]{
				new SlotWithSkin(inventory, inSlotId, 70, 12, SlotSkin.medium),
				new SlotWithSkin(inventory, outSlotId, 130, 12, SlotSkin.big),
				new GenericItemUsingDamageSlot(inventory, boosterSlotId, 20, 12, 5,
												MachineBoosterDescriptor.class,
												SlotSkin.medium,
												new String[]{"Booster Slot"}),
			});
		this.node = node;
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