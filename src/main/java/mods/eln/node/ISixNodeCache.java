package mods.eln.node;

import net.minecraft.item.ItemStack;

public interface ISixNodeCache {
    boolean accept(ItemStack stack);

    int getMeta(ItemStack stack);
}
