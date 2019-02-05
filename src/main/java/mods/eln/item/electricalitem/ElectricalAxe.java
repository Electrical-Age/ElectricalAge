package mods.eln.item.electricalitem;

import mods.eln.misc.Utils;
import mods.eln.wiki.Data;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ElectricalAxe extends ElectricalTool {

    public ElectricalAxe(String name, float strengthOn, float strengthOff,
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
        float value = state.getBlock() != null && (state.getMaterial() == Material.WOOD || state.getMaterial() == Material.PLANTS || state.getMaterial() == Material.VINE) ? getStrength(stack) : super.getStrVsBlock(stack, state);
        Utils.println(value);
        return value;
    }
}
