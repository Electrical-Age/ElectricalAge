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
            switch (t) {
                case INVENTORY:
                    // 1.12 GUI: unit = 16 px, y up, origin at the slot centre. 1.7.10 (non-helper): pixels, y down,
                    // origin at the slot's top-left corner, lighting off.
                    GL11.glTranslatef(-0.5F, 0.5F, 0F);
                    GL11.glScalef(1F / 16F, -1F / 16F, 1F / 16F);
                    GL11.glDisable(GL11.GL_LIGHTING);
                    data = new Object[]{null};
                    break;
                case EQUIPPED_FIRST_PERSON:
                    // 1.12 ItemRenderer.transformSideFirstPerson == 1.7.10's translate(0.7*0.8, -0.65*0.8, -0.9*0.8);
                    // then 1.7.10: rotate 45 Y, scale 0.4, renderEquippedItem.
                    if (transform == TransformType.FIRST_PERSON_LEFT_HAND) GL11.glScalef(-1F, 1F, 1F); // TODO(1.12 M2): left hand
                    GL11.glRotatef(45F, 0F, 1F, 0F);
                    GL11.glScalef(0.4F, 0.4F, 0.4F);
                    equippedItem();
                    data = new Object[]{null, entity};
                    break;
                case EQUIPPED: {
                    // undo 1.12 LayerHeldItem (after the arm's postRender): rot -90 X, rot 180 Y, translate(+-1/16, 0.125, -0.625)
                    boolean left = transform == TransformType.THIRD_PERSON_LEFT_HAND;
                    GL11.glTranslatef(left ? 1F / 16F : -1F / 16F, -0.125F, 0.625F);
                    GL11.glRotatef(-180F, 0F, 1F, 0F);
                    GL11.glRotatef(90F, 1F, 0F, 0F);
                    // 1.7.10 RenderPlayer.renderEquippedItems after bipedRightArm.postRender
                    GL11.glTranslatef(-0.0625F, 0.4375F, 0.0625F);
                    if (itemStack.getItem() == Eln.sixNodeItem) {
                        // SixNodeBlock had render type 0: RenderBlocks.renderItemIn3d -> block branch
                        float f = 0.5F * 0.75F;
                        GL11.glTranslatef(0.0F, 0.1875F, -0.3125F);
                        GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
                        GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
                        GL11.glScalef(-f, -f, f);
                    } else {
                        float f = 0.375F;
                        GL11.glTranslatef(0.25F, 0.1875F, -0.1875F);
                        GL11.glScalef(f, f, f);
                        GL11.glRotatef(60.0F, 0.0F, 0.0F, 1.0F);
                        GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                        GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);
                    }
                    equippedItem();
                    data = new Object[]{null, entity};
                    break;
                }
                case ENTITY:
                default:
                    // 1.12 RenderEntityItem: entity pos + bob + 0.25, spinning (1.7.10 EA items neither bobbed nor
                    // spun: kept the 1.12 motion). 1.7.10 then: SixNode item (render type 0) 3D path scale 0.25,
                    // others scale 0.5 (+ a camera-facing turn we don't reproduce).
                    GL11.glTranslatef(0F, -0.25F, 0F);
                    if (transform == TransformType.FIXED) GL11.glTranslatef(0F, 0.25F, 0F); // item frame: centred
                    float s = itemStack.getItem() == Eln.sixNodeItem ? 0.25F : 0.5F;
                    GL11.glScalef(s, s, s);
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

    /** Forge 1.7.10 ForgeHooksClient.renderEquippedItem, non-helper branch. */
    private static void equippedItem() {
        GL11.glTranslatef(0.0F, -0.3F, 0.0F);
        GL11.glScalef(1.5F, 1.5F, 1.5F);
        GL11.glRotatef(50.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(335.0F, 0.0F, 0.0F, 1.0F);
        GL11.glTranslatef(-0.9375F, -0.0625F, 0.0F);
    }
}
