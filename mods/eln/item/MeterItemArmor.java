package mods.eln.item;

import mods.eln.IInteract;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumArmorMaterial;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class MeterItemArmor extends ItemArmor implements IInteract {

	public MeterItemArmor(int par1, EnumArmorMaterial par2EnumArmorMaterial,
			int par3, int par4) {
		super(par1, par2EnumArmorMaterial, par3, par4);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void interact(EntityPlayerMP playerMP,ItemStack itemStack, byte param) {
		// TODO Auto-generated method stub
		int data = itemStack.getItemDamage();
		switch (param) {
		case 0:
			itemStack.setItemDamage((data + 1)&0x7);
			break;
		case 1:
			
			break;

		default:
			break;
		}
	}
	
	
	public static float getBlockRenderColorFactor(ItemStack itemStack)
	{
		return 1.0f/(1<<(itemStack.getItemDamage()&7));
	}

}
