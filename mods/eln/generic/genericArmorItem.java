package mods.eln.generic;

import mods.eln.Eln;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnumArmorMaterial;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class genericArmorItem  extends ItemArmor{

	public genericArmorItem(int par1, EnumArmorMaterial par2EnumArmorMaterial,
			int par3, int par4,String t1,String t2,int legs) {
		super(par1, par2EnumArmorMaterial, par3, par4);
		this.t1 = t1;
		this.t2 = t2;
		this.legs = legs;
	}
	String t1,t2;
	int legs;
	
	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, int slot, int layer)

	{

		if(stack.itemID == legs)

		{

			return t2;

		}

		else

		{

			return t1;

		}

	}

}
