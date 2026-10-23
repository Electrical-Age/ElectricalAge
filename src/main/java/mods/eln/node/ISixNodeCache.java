package mods.eln.node;

import net.minecraft.world.item.ItemStack;

public interface ISixNodeCache {
    boolean accept(ItemStack stack);

    int getMeta(ItemStack stack);
}
