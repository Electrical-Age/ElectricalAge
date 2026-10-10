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

    /**
     * 26.x: a random number of extra output[0] items, extraWeights[k] = weight of k extra (null: none). Ore blocks
     * macerate to raw chunks about like Fortune II, chunks to one dust with a chance of two (Baughn, 2026-10-10).
     */
    public int[] extraWeights;

    public Recipe withExtra(int... weights) {
        extraWeights = weights;
        return this;
    }

    /** The most this recipe can put out (output space check). */
    public ItemStack[] maxOutput() {
        if (extraWeights == null) return output;
        ItemStack[] o = getOutputCopy();
        o[0].grow(extraWeights.length - 1);
        return o;
    }

    /** The output of one operation, the random extra included. */
    public ItemStack[] getOutputCopy(java.util.Random random) {
        ItemStack[] o = getOutputCopy();
        if (extraWeights != null) {
            int total = 0;
            for (int w : extraWeights) total += w;
            int pick = random.nextInt(total);
            for (int k = 0; k < extraWeights.length; k++) {
                if (pick < extraWeights[k]) {
                    o[0].grow(k);
                    break;
                }
                pick -= extraWeights[k];
            }
        }
        return o;
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
