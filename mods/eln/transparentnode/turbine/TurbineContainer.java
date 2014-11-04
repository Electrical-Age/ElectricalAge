package mods.eln.transparentnode.turbine;

import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class TurbineContainer extends BasicContainer implements INodeContainer{

//	public static final int turbineCoreId = 0;

	
	NodeBase node;
	public TurbineContainer(NodeBase node,EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
			//	new GenericItemUsingDamageSlot(inventory, turbineCoreId, 62 +  0,17 + 0,1, TurbineCoreDescriptor.class),
			});
		this.node = node;
		
	}
	@Override
	public NodeBase getNode() {
		
		return node;
	}
	@Override
	public int getRefreshRateDivider() {
		
		return 0;
	}

}
