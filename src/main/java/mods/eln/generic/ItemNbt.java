package mods.eln.generic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;

/**
 * 1.12 ItemStack.getTagCompound/setTagCompound on 26.x: EA's item NBT lives in the CUSTOM_DATA component, unchanged
 * in content (battery charge, cable color, chip programs...).
 */
public final class ItemNbt {
    private ItemNbt() {
    }

    /** A copy of the stack's EA tag, or null if it has none (like 1.12 getTagCompound). */
    @Nullable
    public static CompoundTag get(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? null : d.copyTag();
    }

    public static boolean has(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    public static void set(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag == null) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Change the stack's EA tag: 1.12 code mutated getTagCompound() in place; on 26.x the tag is a copy of the
     * immutable CUSTOM_DATA, so edits must be written back. Starts from an empty tag when the stack has none.
     */
    public static void edit(ItemStack stack, java.util.function.Consumer<CompoundTag> edit) {
        CompoundTag tag = get(stack);
        if (tag == null) tag = new CompoundTag();
        edit.accept(tag);
        set(stack, tag);
    }
}
