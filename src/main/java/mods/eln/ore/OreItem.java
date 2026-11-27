package mods.eln.ore;

import mods.eln.generic.IDescriptorItem;
import net.minecraft.world.item.BlockItem;

/** The item of an EA ore block (1.12 OreItem, a damage sub-item of eln:ore). */
public class OreItem extends BlockItem implements IDescriptorItem {
    public final OreDescriptor descriptor;

    public OreItem(OreBlock block, Properties properties) {
        super(block, properties);
        this.descriptor = block.descriptor;
    }

    @Override
    public Object getEaDescriptor() {
        return descriptor;
    }
}
