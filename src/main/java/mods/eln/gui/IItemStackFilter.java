package mods.eln.gui;

import net.minecraft.item.ItemStack;

public interface IItemStackFilter {
    boolean tryItemStack(ItemStack itemStack);
}
