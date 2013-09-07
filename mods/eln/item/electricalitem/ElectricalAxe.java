package mods.eln.item.electricalitem;

import mods.eln.wiki.Data;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ElectricalAxe extends ElectricalTool {

	public ElectricalAxe(String name, float strengthOn, float strengthOff,
			double energyStorage, double energyPerBlock, double chargePower) {
		super(name, strengthOn, strengthOff, energyStorage, energyPerBlock, chargePower);
		// TODO Auto-generated constructor stub
		
		
	}
	
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addPortable(newItemStack());
	}
	
	@Override
	public float getStrVsBlock(ItemStack stack, Block block) {
        float value =   block != null && (block.blockMaterial == Material.wood || block.blockMaterial == Material.plants || block.blockMaterial == Material.vine) ? getStrength(stack) : super.getStrVsBlock(stack, block);
		System.out.println(value);
        return value;
	}

}
