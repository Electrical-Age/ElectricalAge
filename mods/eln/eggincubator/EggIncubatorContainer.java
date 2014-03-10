package mods.eln.eggincubator;

import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.ItemStackFilter;
import mods.eln.SlotFilter;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.node.INodeContainer;
import mods.eln.node.Node;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeItemSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;

public class EggIncubatorContainer extends BasicContainer implements INodeContainer {
	public static final int EggSlotId = 0;
	private Node node;

	
	public EggIncubatorContainer(EntityPlayer player, IInventory inventory,Node node) {
		
		super(player, inventory,new Slot[]{
				new SlotFilter(inventory,EggSlotId,176/2-8,7,64,new ItemStackFilter[]{new ItemStackFilter(Item.egg)},SlotSkin.medium,new String[]{"Egg Slot"})
				
				//	new SlotFilter(inventory,1,62 + 18,17,1,new ItemStackFilter[]{new ItemStackFilter(Eln.sixNodeBlock,0xFF,Eln.electricalCableId)})
			});
		this.node = node;
		
		// TODO Auto-generated constructor stub
	}


	@Override
	public NodeBase getNode() {
		// TODO Auto-generated method stub
		return node;
	}

	@Override
	public int getRefreshRateDivider() {
		// TODO Auto-generated method stub
		return 1;
	}



}
