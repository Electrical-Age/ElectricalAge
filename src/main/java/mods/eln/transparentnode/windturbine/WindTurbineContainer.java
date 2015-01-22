package mods.eln.transparentnode.windturbine;


import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class WindTurbineContainer extends BasicContainer implements INodeContainer{

	NodeBase node = null;

	public WindTurbineContainer(NodeBase node,EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				//new GenericItemUsingDamageSlot(inventory, windRotorSlotId, 62 +  0,17 + 0,1, WindRotorDescriptor.class,SlotSkin.medium,new String[]{"Wind rotor slot"}),
				//new GenericItemUsingDamageSlot(inventory, dynamoSlotId, 62 +  18,17 + 0,1, DynamoDescriptor.class,SlotSkin.medium,new String[]{"Dynamo slot"})

			});
		this.node = node;
		
	}

	@Override
	public NodeBase getNode() {
		
		return node;
	}

	@Override
	public int getRefreshRateDivider() {
		
		return 4;
	}



}
/*				new SlotFilter(inventory,0,62 +  0,17,new ItemStackFilter[]{new ItemStackFilter(Block.wood,0,0)}),
new SlotFilter(inventory,1,62 + 18,17,new ItemStackFilter[]{new ItemStackFilter(Item.coal,0,0)})
*/