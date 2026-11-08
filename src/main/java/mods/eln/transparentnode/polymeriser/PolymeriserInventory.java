package mods.eln.transparentnode.polymeriser;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.world.item.ItemStack;

/** The Polymeriser's slots for hoppers and pipes: Soul Dust in from above and the sides, Raw Rubber out from below. */
public class PolymeriserInventory extends TransparentNodeElementInventory {
    public PolymeriserInventory(TransparentNodeElement element) {
        super(2, 64, element);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return Direction.fromIntMinecraftSide(side) == Direction.YN ? new int[]{PolymeriserElement.RUBBER_SLOT} : new int[]{PolymeriserElement.SOUL_SLOT};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return slot == PolymeriserElement.SOUL_SLOT && Direction.fromIntMinecraftSide(side) != Direction.YN
            && stack.is(mods.eln.registry.ElnRegistry.SOUL_DUST.get());
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot == PolymeriserElement.RUBBER_SLOT && Direction.fromIntMinecraftSide(side) == Direction.YN;
    }
}
