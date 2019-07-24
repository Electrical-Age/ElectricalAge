package mods.eln.node.transparent;

/**
 * Used to differentiate between subclasses of TransparentNodeEntity, so that
 * our TEs can implement different interfaces depending on what functionality
 * they have.
 */
public enum EntityMetaTag {
    Fluid(1, TransparentNodeEntityWithFluid.class),
    // 3, because this is the default value used in pre-metatag worlds.
    Basic(3, TransparentNodeEntity.class);

    public final int meta;
    public final Class cls;

    EntityMetaTag(int meta, Class cls) {
        this.meta = meta;
        this.cls = cls;
    }
}
