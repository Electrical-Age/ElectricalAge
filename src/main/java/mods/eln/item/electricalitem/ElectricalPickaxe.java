package mods.eln.item.electricalitem;

import mods.eln.wiki.Data;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
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
    public float getStrVsBlock(ItemStack stack, IBlockState state) {
        Block bl = state.getBlock();
        float value = bl != null && (state.getMaterial() == Material.IRON || state.getMaterial() == Material.GLASS || state.getMaterial() == Material.ANVIL || state.getMaterial() == Material.ROCK) ? getStrength(stack) : super.getStrVsBlock(stack, state);
        for (Block b : blocksEffectiveAgainst) {
            if (b == bl) {
                value = getStrength(stack);
                break;
            }
        }
        // Utils.println("****" + value);
        return value;
    }
}
