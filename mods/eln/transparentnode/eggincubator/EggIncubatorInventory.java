package mods.eln.transparentnode.eggincubator;

import net.minecraft.item.ItemStack;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;

public class EggIncubatorInventory extends TransparentNodeElementInventory{
	public EggIncubatorInventory(int size, int stackLimit, TransparentNodeElement TransparentNodeElement) {
		super(size, stackLimit, TransparentNodeElement);
	}

	public EggIncubatorInventory(int size, int stackLimit, TransparentNodeElementRender TransparentnodeRender) {
		super(size, stackLimit, TransparentnodeRender);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return new int[]{EggIncubatorContainer.EggSlotId};
	}

	@Override
	public boolean canInsertItem(int var1, ItemStack var2, int var3) {
		return true;
	}

	@Override
	public boolean canExtractItem(int var1, ItemStack var2, int var3) {
		return false;
	}

}
