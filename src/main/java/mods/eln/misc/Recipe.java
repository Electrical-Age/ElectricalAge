package mods.eln.misc;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public class Recipe {

    public ItemStack input;
    public ItemStack[] output;
    public double energy;

    public Recipe(ItemStack input, ItemStack[] output, double energy) {
        this.input = input;
        this.output = output;
        this.energy = energy;
    }

    public Recipe(ItemStack input, ItemStack output, double energy) {
        this.input = input;
        this.output = new ItemStack[]{output};
        this.energy = energy;
    }

    /**
     * Extra inputs (26.x): the tags of the input's 1.12 ore dictionary names (ElnMachineRecipes), so other items under
     * the same name work, as 1.12's Utils.areSame did through the ore dictionary.
     */
    public java.util.List<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>> inputTags = java.util.List.of();

    public Recipe withTags(java.util.List<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>> tags) {
        inputTags = tags;
        return this;
    }

    public boolean canBeCraftedBy(ItemStack stack) {
        if (Utils.isEmpty(stack) || input.getCount() > stack.getCount()) return false;
        if (stack.getItem() == input.getItem()) return true;
        for (var t : inputTags) if (stack.is(t)) return true;
        return false;
    }

    public ItemStack[] getOutputCopy() {
        ItemStack[] cpy = new ItemStack[output.length];
        for (int idx = 0; idx < output.length; idx++) {
            cpy[idx] = output[idx].copy();
        }
        return cpy;
    }

    public ArrayList<ItemStack> machineList = new ArrayList<ItemStack>();

    public void setMachineList(ArrayList<ItemStack> machineList) {
        this.machineList = machineList;
    }
}
