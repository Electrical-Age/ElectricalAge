package mods.eln.ore;

import mods.eln.generic.IDescriptorItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

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

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(descriptor.name);
    }
}
