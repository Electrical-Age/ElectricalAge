package mods.eln.turbine;

import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.item.CombustionChamber;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.regulator.RegulatorSlot;
import mods.eln.node.INodeContainer;
import mods.eln.node.Node;
import mods.eln.sim.RegulatorType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class TurbineContainer extends BasicContainer implements INodeContainer{

//	public static final int turbineCoreId = 0;

	
	Node node;
	public TurbineContainer(Node node,EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
			//	new GenericItemUsingDamageSlot(inventory, turbineCoreId, 62 +  0,17 + 0,1, TurbineCoreDescriptor.class),
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
