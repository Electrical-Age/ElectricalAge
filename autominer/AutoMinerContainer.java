package mods.eln.autominer;

import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.item.OreScanner;
import mods.eln.node.INodeContainer;
import mods.eln.node.Node;
import mods.eln.node.SixNodeItemSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class AutoMinerContainer extends BasicContainer implements INodeContainer {
	Node node;
	
	public static final int electricalDrillSlotId = 0;
	public static final int OreScannerSlotId = 1;
	public static final int MiningPipeSlotId = 2;
	public AutoMinerContainer(Node node,EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
		
				new GenericItemUsingDamageSlot(inventory,electricalDrillSlotId,62 + 0,17,1,ElectricalDrillDescriptor.class),
				new GenericItemUsingDamageSlot(inventory,OreScannerSlotId,62 + 18,17,1,OreScanner.class),
				new GenericItemUsingDamageSlot(inventory,MiningPipeSlotId,62 + 36,17,64,MiningPipeDescriptor.class)
			
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
