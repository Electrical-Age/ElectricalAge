package mods.eln.item;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public interface IInteract {
    abstract public void interact(EntityPlayerMP playerMP, ItemStack itemStack, byte param);
}
