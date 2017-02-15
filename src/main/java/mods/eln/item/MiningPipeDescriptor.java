package mods.eln.item;

import mods.eln.misc.VoltageLevelColor;
import net.minecraft.item.Item;

public class MiningPipeDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

    public MiningPipeDescriptor(String name) {
        super(name);
        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
    }
}
