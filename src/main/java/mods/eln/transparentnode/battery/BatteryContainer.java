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

import static mods.eln.i18n.I18N.tr;

public class BatteryContainer extends BasicContainer implements INodeContainer {
	
	NodeBase node;
	
	public BatteryContainer(NodeBase node, EntityPlayer player, IInventory inventory) {
		super(player, inventory, new Slot[]{
				new GenericItemUsingDamageSlot(inventory, 0, 130, 40, 1,
					OverVoltageProtectionDescriptor.class, SlotSkin.medium,
					new String[]{tr("Overvoltage protection")}),
				new GenericItemUsingDamageSlot(inventory, 1, 130, 60, 1,
					OverHeatingProtectionDescriptor.class,SlotSkin.medium,
					new String[]{tr("Overheating protection")}),
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
