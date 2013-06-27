package mods.eln.battery;

import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.item.OverHeatingProtectionDescriptor;
import mods.eln.item.OverVoltageProtectionDescriptor;
import mods.eln.node.INodeContainer;
import mods.eln.node.Node;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class BatteryContainer extends BasicContainer implements INodeContainer{
	Node node;
	public BatteryContainer(Node node,EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new GenericItemUsingDamageSlot(inventory,0,62 +  0,17,1,OverVoltageProtectionDescriptor.class),
				new GenericItemUsingDamageSlot(inventory,1,62 +  18,17,1,OverHeatingProtectionDescriptor.class),

			});
		this.node = node;
		// TODO Auto-generated constructor stub
	}

	@Override
	public Node getNode() {
		// TODO Auto-generated method stub
		return node;
	}

	@Override
	public int getRefreshRateDivider() {
		// TODO Auto-generated method stub
		return 0;
	}

}
