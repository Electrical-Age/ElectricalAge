package mods.eln.item.electricalitem;

import mods.eln.Eln;
import mods.eln.debug.DebugType;
import mods.eln.misc.Utils;
import mods.eln.wiki.Data;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
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
    public float getStrVsBlock(ItemStack stack, Block block) {
        float value = block != null && (block.getMaterial() == Material.wood || block.getMaterial() == Material.plants || block.getMaterial() == Material.vine) ? getStrength(stack) : super.getStrVsBlock(stack, block);
        Eln.dp.println(DebugType.OTHER, Float.toString(value));
        return value;
    }
}
