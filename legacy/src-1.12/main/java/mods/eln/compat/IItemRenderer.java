package mods.eln.compat;

import net.minecraft.item.ItemStack;

/**
 * Replica of Forge 1.7.10's {@code mods.eln.compat.IItemRenderer} (same shape, nested enums), so EA's
 * descriptor render code keeps its {@code handleRenderType/shouldUseRenderHelper/renderItem} methods.
 * No client types in the signatures, so common classes (descriptors) may implement it; the bodies that draw must
 * only run on the client.
 *
 * TODO(1.12 WP5): client bridge. Item JSON with parent {@code builtin/entity}; a wrapping IBakedModel whose
 * handlePerspective(TransformType) stashes the type and returns itself; a TileEntityItemStackRenderer that reads it
 * and calls {@link #renderItem} with GUI->INVENTORY, FIRST_PERSON_*->EQUIPPED_FIRST_PERSON, THIRD_PERSON_*->EQUIPPED,
 * GROUND/FIXED->ENTITY (see PORTING-GUIDE rule 2).
 */
public interface IItemRenderer {
    enum ItemRenderType {
        /** Render an EntityItem (dropped item, item frame). */
        ENTITY,
        /** Held in a third-person hand. */
        EQUIPPED,
        /** Held in the first-person hand. */
        EQUIPPED_FIRST_PERSON,
        /** In a GUI slot / inventory. */
        INVENTORY,
        /** A held map in first person. */
        FIRST_PERSON_MAP
    }

    enum ItemRendererHelper {
        /** Spin like a dropped item (ENTITY only). */
        ENTITY_ROTATION,
        /** Bob up and down like a dropped item (ENTITY only). */
        ENTITY_BOBBING,
        /** Third-person held-block transform (EQUIPPED only). */
        EQUIPPED_BLOCK,
        /** Bob/rotate as a 3D block (ENTITY only). */
        BLOCK_3D,
        /** Inventory block transform (INVENTORY only). */
        INVENTORY_BLOCK
    }

    /** Whether this renderer handles the given render type for this stack. */
    boolean handleRenderType(ItemStack item, ItemRenderType type);

    /** Whether the bridge should apply the given helper transform before calling {@link #renderItem}. */
    boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper);

    /**
     * Render the item. {@code data} as in 1.7.10: ENTITY {RenderBlocks, EntityItem}; EQUIPPED* {RenderBlocks,
     * EntityLivingBase}; INVENTORY {RenderBlocks}. RenderBlocks no longer exists: the bridge passes null in its slot.
     */
    void renderItem(ItemRenderType type, ItemStack item, Object... data);
}
