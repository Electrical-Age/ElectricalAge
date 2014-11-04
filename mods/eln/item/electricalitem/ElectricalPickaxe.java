package mods.eln.item.electricalitem;

import mods.eln.wiki.Data;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ElectricalPickaxe extends ElectricalTool {

	public ElectricalPickaxe(String name, float strengthOn, float strengthOff,
			double energyStorage, double energyPerBlock, double chargePower) {
		super(name, strengthOn, strengthOff, energyStorage, energyPerBlock, chargePower);
		
	}
	
	@Override
	public void setParent(Item item, int damage) {
		
		super.setParent(item, damage);
		Data.addPortable(newItemStack());
	}
	@Override
	public float getStrVsBlock(ItemStack stack, Block block) {
        float value =  block != null && (block.getMaterial() == Material.iron || block.getMaterial() == Material.glass || block.getMaterial() == Material.anvil || block.getMaterial() == Material.rock) ? getStrength(stack) : super.getStrVsBlock(stack, block);
        for(Block b : blocksEffectiveAgainst){
        	if(b == block){
        		value = getStrength(stack);
        		break;
        	}
        }
       // Utils.println("****" + value);
		return value;
	}

}
