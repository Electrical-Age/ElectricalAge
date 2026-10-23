package mods.eln.oil;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.world.item.ItemStack;

/** Hoppers: the sides feed the input (only food it can cook; the top is the steam pipe's), the bottom takes the output. */
public class SteamOvenInventory extends TransparentNodeElementInventory {
    public SteamOvenInventory(int size, int stackLimit, TransparentNodeElement element) {
        super(size, stackLimit, element);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return Direction.fromIntMinecraftSide(side) == Direction.YN ? new int[]{SteamOvenElement.outSlotId} : new int[]{SteamOvenElement.inSlotId};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return Direction.fromIntMinecraftSide(side) != Direction.YN && slot == SteamOvenElement.inSlotId && transparentNodeElement != null
            && mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess.charrable(stack)
            && !mods.eln.misc.Utils.smeltResult(transparentNodeElement.node.coordonate.world(), stack).isEmpty();
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return Direction.fromIntMinecraftSide(side) == Direction.YN && slot == SteamOvenElement.outSlotId;
    }
}
