package mods.eln.item.electricalitem;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;

public class ElectricalPickaxe extends ElectricalTool {

	public ElectricalPickaxe(String name, float strengthOn, float strengthOff,
			double energyStorage, double energyPerBlock, double chargePower) {
		super(name, strengthOn, strengthOff, energyStorage, energyPerBlock, chargePower);
		// TODO Auto-generated constructor stub
	}
	
	
	@Override
	public float getStrVsBlock(ItemStack stack, Block block) {
        float value =  block != null && (block.blockMaterial == Material.iron || block.blockMaterial == Material.anvil || block.blockMaterial == Material.rock) ? getStrength(stack) : super.getStrVsBlock(stack, block);
        for(Block b : blocksEffectiveAgainst){
        	if(b == block){
        		value = getStrength(stack);
        		break;
        	}
        }
        System.out.println(value);
		return value;
	}

}
