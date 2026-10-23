package mods.eln.transparentnode.smelter;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.world.item.ItemStack;

/** Hoppers: the top and the sides feed the input (only what smelts), the bottom takes the output (Baughn: hopper-friendly). */
public class SmelterInventory extends TransparentNodeElementInventory {
    public SmelterInventory(int size, int stackLimit, TransparentNodeElement element) {
        super(size, stackLimit, element);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return Direction.fromIntMinecraftSide(side) == Direction.YN ? new int[]{SmelterElement.outSlotId} : new int[]{SmelterElement.inSlotId};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return Direction.fromIntMinecraftSide(side) != Direction.YN && slot == SmelterElement.inSlotId && transparentNodeElement != null
            && !mods.eln.misc.Utils.smeltResult(transparentNodeElement.node.coordonate.world(), stack).isEmpty();
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return Direction.fromIntMinecraftSide(side) == Direction.YN && slot == SmelterElement.outSlotId;
    }
}
