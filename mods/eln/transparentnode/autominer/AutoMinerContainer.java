package mods.eln.transparentnode.autominer;

import mods.eln.BasicContainer;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.SlotWithSkin;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.item.OreScanner;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class AutoMinerContainer extends BasicContainer /*implements INodeContainer*/ {
	NodeBase node;

	public static final int electricalDrillSlotId = 0;
	//public static final int OreScannerSlotId = 1;
	public static final int MiningPipeSlotId = 2;
	public static final int StorageStartId = 3;
	public static final int StorageSize = 21;
	public static final int inventorySize = StorageStartId + StorageSize;
	public AutoMinerContainer(NodeBase node, EntityPlayer player, IInventory inventory) {
		super(player, inventory, newSlots(inventory));
		this.node = node;
	}

	public static Slot[] newSlots(IInventory inventory) {
		Slot[] slots = new Slot[StorageStartId + StorageSize];
		slots[0] = new GenericItemUsingDamageSlot(inventory, electricalDrillSlotId, 62 + 0, 17, 1, ElectricalDrillDescriptor.class, SlotSkin.medium, new String[] { "Drill Slot" });
		slots[1] = new GenericItemUsingDamageSlot(inventory, 1, 3000, 3000, 1, OreScanner.class, SlotSkin.medium, new String[] { "Ore Scanner Slot" });
		slots[2] = new GenericItemUsingDamageSlot(inventory, MiningPipeSlotId, 62 + 36, 17, 64, MiningPipeDescriptor.class, SlotSkin.medium, new String[] { "Mining Pipe Slot" });

		int x = 0,y = 2;
		for(int idx = 0;idx < StorageSize;idx++){
			slots[idx + StorageStartId] = new SlotWithSkin(inventory,StorageStartId + idx,62-18*2 + x*18, 17 + y*18,SlotSkin.medium);
			x++;
			if(x == 7){
				x = 0;
				y++;
			}
		}
		return slots;
	}

	/*@Override
	public NodeBase getNode() {
		return node;
	}

	@Override
	public int getRefreshRateDivider() {
		return 0;
	}*/
}
