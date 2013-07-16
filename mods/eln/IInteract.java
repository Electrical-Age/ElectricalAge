package mods.eln;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.network.Player;

public interface IInteract {
	abstract public void interact(EntityPlayerMP playerMP, ItemStack itemStack,
			byte param);
}
