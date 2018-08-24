package mods.eln.generic;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class GenericCreativeTab extends CreativeTabs {

    public Item item;

    public GenericCreativeTab(String label, Item item) {
        super(label);
        this.item = item;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {
        return (item);
    }
}
