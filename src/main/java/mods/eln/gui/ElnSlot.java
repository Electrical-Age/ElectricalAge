package mods.eln.gui;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A device slot (1.12 SlotWithSkin and its subclasses), its rule from a {@link SlotSpec}. */
public class ElnSlot extends Slot {
    public final SlotSpec spec;

    /** The level the menu is open in (for a furnace input's recipe test), or null. */
    final net.minecraft.world.level.Level level;

    public ElnSlot(Container container, SlotSpec spec) {
        this(container, spec, null);
    }

    public ElnSlot(Container container, SlotSpec spec, net.minecraft.world.level.Level level) {
        super(container, spec.index(), spec.x(), spec.y());
        this.spec = spec;
        this.level = level;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if ("Smeltable".equals(spec.kind()))
            return level == null || spec.filter().test(stack)
                || level.recipeAccess().propertySet(net.minecraft.world.item.crafting.RecipePropertySet.FURNACE_INPUT).test(stack);
        return spec.filter().test(stack);
    }

    /** A cable-piece slot takes a block (16 voxels) of a cable kit, leaving the rest with the player (Baughn 2026-11-07). */
    public boolean takesPiece(ItemStack stack) {
        return SlotSpec.CABLE_PIECE.equals(spec.kind()) && mods.eln.node.voxel.CableItems.kitTier(stack) >= 0;
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int increment) {
        if (!stack.isEmpty() && takesPiece(stack) && !hasItem() && mayPlace(stack)) {
            setByPlayer(mods.eln.node.voxel.CableItems.take(stack, 16));
            return stack;
        }
        return super.safeInsert(stack, increment);
    }

    /** 1.12 getSlotStackLimit (the slot's own limit; 26.x also caps by the item's max stack size). */
    @Override
    public int getMaxStackSize() {
        return spec.limit();
    }

    /** ... and the device's own cap for that item (Container.getMaxStackSize(ItemStack): the signal processor's advanced chips). */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(super.getMaxStackSize(stack), container.getMaxStackSize(stack));
    }
}
