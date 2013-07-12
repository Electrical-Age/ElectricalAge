package mods.eln.misc;

import net.minecraft.item.ItemStack;

public class Recipe {

	public Recipe(ItemStack input,ItemStack[] output,double energy) {
		this.input = input;
		this.output = output;
		this.energy = energy;
	}
	public Recipe(ItemStack input,ItemStack output,double energy) {
		this.input = input;
		this.output = new ItemStack[]{output};
		this.energy = energy;
	}
	
	public ItemStack input;
	public ItemStack[] output;
	public double energy;
	
	
	
	public boolean canBeCraftedBy(ItemStack stack)
	{
		if(stack == null) return false;
		return input.stackSize <= stack.stackSize && input.getItem() == stack.getItem() && input.getItemDamage() == stack.getItemDamage();
	}
	
	public ItemStack[] getOutputCopy()
	{
		ItemStack[] cpy = new ItemStack[output.length];
		for(int idx = 0;idx<output.length;idx++)
		{
			cpy[idx] = output[idx].copy();
		}
		return cpy;
	}
}
