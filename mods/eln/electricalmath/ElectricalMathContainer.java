package mods.eln.electricalmath;



import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.ItemStackFilter;
import mods.eln.SlotFilter;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.SlotWithSkin;
import mods.eln.gui.ISlotSkin.SlotSkin;

import mods.eln.item.HeatingCorpElement;
import mods.eln.item.MaceratorSorterDescriptor;
import mods.eln.item.MachineBoosterDescriptor;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorSlot;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import mods.eln.sim.RegulatorType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;

public class ElectricalMathContainer extends BasicContainer{

	NodeBase node = null;
	public static final int restoneSlotId = 0;
	public ElectricalMathContainer(NodeBase node,EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new SlotFilter(inventory,restoneSlotId,125,25,64,new ItemStackFilter[]{new ItemStackFilter(Item.redstone)},SlotSkin.medium,new String[]{"Redstone Slot"})
				
				
			});
		this.node = node;
		// TODO Auto-generated constructor stub
	}




}