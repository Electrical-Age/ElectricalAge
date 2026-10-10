package mods.eln.node.transparent;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Used to differentiate between subclasses of TransparentNodeEntity, so that
 * our TEs can implement different interfaces depending on what functionality
 * they have.
 */
public enum EntityMetaTag {
    // 1.12 picked the BlockEntity subclass from this; 26.x has one block-entity type per kind (registry, P1 step 2)
    Fluid(1),

    // 3, because this is the default value used in pre-metatag worlds.
    Basic(3);

    public final int meta;

    EntityMetaTag(int meta) {
        this.meta = meta;
    }
}
