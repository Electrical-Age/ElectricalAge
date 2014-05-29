package mods.eln.generic;

import mods.eln.Eln;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;

//1.7.2
public class genericArmorItem  extends ItemArmor{

	public genericArmorItem(ArmorMaterial par2EnumArmorMaterial,
			int par3, int par4,String t1,String t2) {
		super(par2EnumArmorMaterial, par3, par4);
		this.t1 = t1;
		this.t2 = t2;

	}
	String t1,t2;

	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, int slot, String layer)
	{
		if(this.armorType == 2){
			return t2;
		}
		else{
			return t1;
		}
	}

}
