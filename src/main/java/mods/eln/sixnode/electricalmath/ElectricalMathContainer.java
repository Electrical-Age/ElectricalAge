package mods.eln.sixnode.electricalmath;

import mods.eln.gui.ItemStackFilter;
import mods.eln.gui.SlotFilter;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.misc.BasicContainer;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ElectricalMathContainer extends BasicContainer {

	NodeBase node = null;
	public static final int restoneSlotId = 0;
	
	public ElectricalMathContainer(NodeBase node, EntityPlayer player, IInventory inventory) {
		super(player, inventory, new Slot[]{
				new SlotFilter(inventory, restoneSlotId, 125+27+44/2, 25, 64, new ItemStackFilter[]{new ItemStackFilter(Items.redstone)}, SlotSkin.medium, new String[]{"Redstone Slot"})
		});
		this.node = node;
	}
}
