package mods.eln.client.render;

import mods.eln.Eln;
import mods.eln.compat.IItemRenderer;
import mods.eln.compat.IItemRenderer.ItemRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/**
 * IItemRenderer bridge, renderer half: the TEISR of every bridged EA item. {@link ItemBridgeModel.Stack} records the
 * render type in {@link #prepare} just before RenderItem calls us (same thread, same call chain), so a static is
 * enough.
 *
 * GL frame on entry (RenderItem.renderItem): the 1.12 transform of the type with an IDENTITY camera transform
 * (our model has none), then translate(-0.5, -0.5, -0.5). Each case below first undoes that translate, then
 * rebuilds the frame Forge 1.7.10 gave IItemRenderer.renderItem when shouldUseRenderHelper(...) is false (EA's
 * descriptors return false; SixNodeItem/TransparentNodeItem add their own "Eln" helper transforms inside renderItem).
 * The 1.7.10 numbers are from memory of Forge 1.7.10 ForgeHooksClient.renderInventoryItem/renderEntityItem/
 * renderEquippedItem, RenderPlayer.renderEquippedItems and ItemRenderer.renderItemInFirstPerson (no 1.7.10 source
 * here): tune at the client test (core-log "WP5 item transforms").
 */
@SideOnly(Side.CLIENT)
public final class ItemBridgeRenderer extends TileEntityItemStackRenderer {
    public static final ItemBridgeRenderer INSTANCE = new ItemBridgeRenderer();

    @Nullable
    private static ItemRenderType type;
    private static TransformType transform = TransformType.NONE;
    private static ItemStack stack = ItemStack.EMPTY;
    @Nullable
    private static EntityLivingBase entity;

    private ItemBridgeRenderer() {
    }

    static void prepare(@Nullable ItemRenderType t, TransformType tt, ItemStack s, @Nullable EntityLivingBase e) {
        type = t;
        transform = tt;
        stack = s;
        entity = e;
    }

    @Override
    public void renderByItem(ItemStack itemStack) {
        renderByItem(itemStack, 1.0F);
    }

    @Override
    public void renderByItem(ItemStack itemStack, float partialTicks) {
        ItemRenderType t = type;
        type = null;
        if (t == null || stack.getItem() != itemStack.getItem() || stack.getItemDamage() != itemStack.getItemDamage()
            || !(itemStack.getItem() instanceof IItemRenderer)) return;
        IItemRenderer renderer = (IItemRenderer) itemStack.getItem();

        // EA draws with raw GL11; 1.12 caches GL state in GlStateManager. Restore everything except the texture
        // binding (EA binds through TextureManager -> GlStateManager, so that cache stays right).
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT
            | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_POLYGON_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0.5F, 0.5F, 0.5F); // undo RenderItem.renderItem's translate(-0.5, -0.5, -0.5)
            Object[] data;
            // The per-type ops live in config/eln-render.cfg (ItemTransforms; /elnclient reloadrender), defaults =
            // the 1.7.10 frames described in core-log "WP5 item transforms".
            boolean six = itemStack.getItem() == Eln.sixNodeItem; // SixNodeBlock had render type 0 (3D item paths)
            Held held = t == ItemRenderType.EQUIPPED || t == ItemRenderType.EQUIPPED_FIRST_PERSON
                ? held(renderer, t, itemStack, six) : Held.PLAIN;
            switch (t) {
                case INVENTORY:
                    ItemTransforms.apply("inventory");
                    GL11.glDisable(GL11.GL_LIGHTING); // 1.7.10 non-helper inventory path
                    data = new Object[]{null};
                    break;
                case EQUIPPED_FIRST_PERSON:
                    if (transform == TransformType.FIRST_PERSON_LEFT_HAND) ItemTransforms.apply("first_person_left");
                    switch (held) {
                        case TOOL:
                            ItemTransforms.apply("equipped_helper_first_person");
                            break;
                        case MODEL:
                            ItemTransforms.apply("equipped_model_first_person");
                            break;
                        case NODE:
                            ItemTransforms.apply("node_first_person");
                            break;
                        default:
                            ItemTransforms.apply("first_person");
                            ItemTransforms.apply("equipped_tail");
                    }
                    data = new Object[]{null, entity};
                    break;
                case EQUIPPED:
                    boolean left = transform == TransformType.THIRD_PERSON_LEFT_HAND;
                    if (held == Held.TOOL || held == Held.NODE) {
                        // keys in the 1.12 LayerHeldItem frame (vanilla item/handheld, block/block poses); left hand =
                        // mirrored like ForgeHooksClient.handleCameraTransforms' leftHandHackery
                        if (left) GL11.glScalef(-1F, 1F, 1F);
                        ItemTransforms.apply(held == Held.TOOL ? "equipped_helper_third_person" : "node_third_person");
                        if (left) GL11.glScalef(-1F, 1F, 1F);
                    } else {
                        ItemTransforms.apply(left ? "third_person_left_undo" : "third_person_right_undo");
                        ItemTransforms.apply("third_person_arm");
                        if (held == Held.MODEL) {
                            ItemTransforms.apply("equipped_model_third_person");
                        } else {
                            ItemTransforms.apply(six ? "third_person_six_node" : "third_person_item");
                            ItemTransforms.apply("equipped_tail");
                        }
                    }
                    data = new Object[]{null, entity};
                    break;
                case ENTITY:
                default:
                    ItemTransforms.apply(transform == TransformType.FIXED ? "fixed"
                        : transform == TransformType.HEAD ? "head" : "ground");
                    ItemTransforms.apply(six ? "entity_six_node" : "entity_item");
                    data = new Object[]{null, null};
                    break;
            }
            GL11.glColor4f(1F, 1F, 1F, 1F);
            renderer.renderItem(t, itemStack, data);
        } catch (RuntimeException e) {
            // never take the frame down for one item
            if (!warned) {
                warned = true;
                e.printStackTrace();
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        }
    }

    private static boolean warned = false;

    /**
     * How a held item is framed (eln-render.cfg keys; math in core-log "Client test 3 fixes").
     * PLAIN: no render helper (equipped_tail after first_person / third_person_item).
     * SIX_NODE: six-node items, helper or not: first_person / third_person_six_node + equipped_tail, as tuned at client
     * test 1 (c5d81cee had moved the helper ones, most of them, to the helper frame untested).
     * TOOL: helper items that draw their flat icon (UtilsClient.drawIcon: tools, flashlight, portable battery, brush).
     * MODEL: helper items that draw an OBJ model tuned against the 1.7.10 helper frame (X-ray scanner, fuse).
     * NODE: transparent-node items (block-sized OBJ models centred on the origin).
     */
    enum Held {PLAIN, SIX_NODE, TOOL, MODEL, NODE}

    /**
     * Forge 1.7.10 renderEquippedItem: shouldUseRenderHelper(type, stack, EQUIPPED_BLOCK) chose translate(-0.5)^3
     * instead of the item transform (equipped_tail); RenderPlayer's BLOCK_3D query (same answer for EA's items) chose
     * the block branch in third person.
     */
    private static Held held(IItemRenderer renderer, ItemRenderType t, ItemStack s, boolean six) {
        if (six) return Held.SIX_NODE;
        boolean helper;
        try {
            helper = renderer.shouldUseRenderHelper(t, s, IItemRenderer.ItemRendererHelper.EQUIPPED_BLOCK);
        } catch (RuntimeException e) {
            helper = false;
        }
        if (!helper) return Held.PLAIN;
        if (s.getItem() instanceof mods.eln.node.transparent.TransparentNodeItem) return Held.NODE;
        Object d = s.getItem() instanceof mods.eln.generic.GenericItemUsingDamage
            ? ((mods.eln.generic.GenericItemUsingDamage<?>) s.getItem()).getDescriptor(s) : null;
        if (d instanceof mods.eln.item.electricalitem.PortableOreScannerItem
            || d instanceof mods.eln.item.ElectricalFuseDescriptor) return Held.MODEL;
        return Held.TOOL;
    }

}
