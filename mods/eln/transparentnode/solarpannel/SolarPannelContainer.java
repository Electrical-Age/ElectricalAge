package mods.eln.transparentnode.solarpannel;


import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.HeatingCorpElement;
import mods.eln.item.SolarTrackerDescriptor;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorSlot;
import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import mods.eln.sim.RegulatorType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class SolarPannelContainer extends BasicContainer implements INodeContainer{

	NodeBase node = null;
	static final int trackerSlotId = 0;
	
	public SolarPannelContainer(NodeBase node,EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new GenericItemUsingDamageSlot(inventory, trackerSlotId, 176/2-20 +  0,35,1, SolarTrackerDescriptor.class,SlotSkin.medium,new String[]{"Solar Tracker Slot"})

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
/*				new SlotFilter(inventory,0,62 +  0,17,new ItemStackFilter[]{new ItemStackFilter(Block.wood,0,0)}),
new SlotFilter(inventory,1,62 + 18,17,new ItemStackFilter[]{new ItemStackFilter(Item.coal,0,0)})
*/