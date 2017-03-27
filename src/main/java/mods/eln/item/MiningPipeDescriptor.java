package mods.eln.item;

import net.minecraft.item.Item;

public class MiningPipeDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

    public MiningPipeDescriptor(String name) {
        super(name);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
    }
}
