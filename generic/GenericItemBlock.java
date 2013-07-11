package mods.eln.generic;

import mods.eln.CommonProxy;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class GenericItemBlock extends ItemBlock {
	int textureIdOffset;
	public String[] subNames = {
		"Cooper", "Silver",  "Gold"
	};

	public GenericItemBlock(int id,int textureIdOffset,String ItemName,String[] subNames) {
		super(id);
		this.textureIdOffset = textureIdOffset;
		this.subNames = subNames;
		setHasSubtypes(true);
		setUnlocalizedName("wireItemBlock");
		
	}
	/*
	@Override//caca1.5.1
    public int getIconFromDamage(int par1)
    {
        return textureIdOffset + par1;
    }
	@Override
	public String getTextureFile () {
		return CommonProxy.ITEMS_PNG;
	}
	*/

	@Override
	public int getMetadata (int damageValue) {
		return damageValue;
	}
	
	
	/*
	@Override //caca1.5.1
	public String getItemNameIS(ItemStack itemstack) {
		return getItemName() + "." + subNames[itemstack.getItemDamage()];
	}*/
}