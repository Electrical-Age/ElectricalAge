package mods.eln.compat;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The 0..15 "meta" block-state property for EA blocks that used metadata in 1.7.10/1.12 (ghost block shapes, the
 * light block's level...). WorldCompat.getMeta/setBlock(meta) read and write it when a block has it.
 */
public final class BlockMeta {
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    private BlockMeta() {
    }
}
