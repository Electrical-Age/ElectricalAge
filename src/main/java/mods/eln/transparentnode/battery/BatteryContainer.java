package mods.eln.transparentnode.battery;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.OverHeatingProtectionDescriptor;
import mods.eln.item.OverVoltageProtectionDescriptor;
import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class BatteryContainer extends BasicContainer implements INodeContainer {
	
	NodeBase node;
	
	public BatteryContainer(NodeBase node, EntityPlayer player, IInventory inventory) {
		super(player, inventory, new Slot[]{
				new GenericItemUsingDamageSlot(inventory, 0, 130, 40, 1, OverVoltageProtectionDescriptor.class,SlotSkin.medium, new String[]{"Overvoltage Protection"}),
				new GenericItemUsingDamageSlot(inventory, 1, 130, 60, 1, OverHeatingProtectionDescriptor.class,SlotSkin.medium, new String[]{"Overheating Protection"}),
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
