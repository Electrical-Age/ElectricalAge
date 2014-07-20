package mods.eln.transparentnode.electricalmachine;

import net.minecraft.item.ItemStack;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;

public class ElectricalMachineInventory extends TransparentNodeElementInventory {
	public ElectricalMachineInventory(int size, int stackLimit, TransparentNodeElement TransparentNodeElement) {
		super(size, stackLimit, TransparentNodeElement);
	}

	public ElectricalMachineInventory(int size, int stackLimit, TransparentNodeElementRender TransparentnodeRender) {
		super(size, stackLimit, TransparentnodeRender);
	}

	ElectricalMachineDescriptor getDescriptor() {
		if (transparentNodeRender != null) return ((ElectricalMachineRender) transparentNodeRender).descriptor;
		if (transparentNodeElement != null) return ((ElectricalMachineElement) transparentNodeElement).descriptor;
		return null;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		// ElectricalMachineDescriptor d = getDescriptor();

		switch (Direction.fromIntMinecraftSide(side)) {
		case YP:
			return new int[] { ElectricalMachineContainer.inSlotId };
		default:
			return new int[] { ElectricalMachineContainer.outSlotId };
		}

	}

	@Override
	public boolean canInsertItem(int var1, ItemStack var2, int side) {
		switch (Direction.fromIntMinecraftSide(side)) {
		case YP:
			return true;
		default:
			return false;
		}
	}

	@Override
	public boolean canExtractItem(int var1, ItemStack var2, int side) {

		switch (Direction.fromIntMinecraftSide(side)) {
		case YP:
			return false;
		default:
			return true;
		}
	}

}
