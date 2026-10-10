package mods.eln.client.render;

import mods.eln.compat.IItemRenderer;
import mods.eln.compat.IItemRenderer.ItemRenderType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * IItemRenderer bridge, model half (porting guide rule 2). One instance per bridged item. handleItemState gives a
 * per-stack {@link Stack} model; its handlePerspective(TransformType) decides, like Forge 1.7.10's
 * MinecraftForgeClient.getItemRenderer(stack, type), whether the item's IItemRenderer handles the render type:
 * yes -> remember (type, stack, entity) for {@link ItemBridgeRenderer} and return itself (isBuiltInRenderer, identity
 * matrix: the 1.7.10 GL conventions are applied in the TEISR); no -> the descriptor's flat icon model (vanilla
 * item/generated transforms), or nothing when there is no icon.
 */
@SideOnly(Side.CLIENT)
public class ItemBridgeModel implements IBakedModel {
    private final IBakedModel base;
    private final Map<Integer, IBakedModel> icons;
    private final ItemOverrideList overrides = new ItemOverrideList(Collections.emptyList()) {
        @Override
        public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
            return new Stack(stack, entity);
        }
    };

    public ItemBridgeModel(@Nullable IBakedModel base, Map<Integer, IBakedModel> icons) {
        this.base = base;
        this.icons = icons;
    }

    /** Forge 1.7.10 ItemRenderType for a 1.12 transform (null: not drawn by EA's renderers). */
    @Nullable
    static ItemRenderType renderTypeOf(TransformType t) {
        switch (t) {
            case GUI:
                return ItemRenderType.INVENTORY;
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND:
                return ItemRenderType.EQUIPPED_FIRST_PERSON;
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
                return ItemRenderType.EQUIPPED;
            case GROUND:
            case FIXED:
            case HEAD:
                return ItemRenderType.ENTITY;
            default:
                return null;
        }
    }

    /** Base-class renderItem implementations that only draw the descriptor's icon (UtilsClient.drawIcon). */
    private static final List<Class<?>> ICON_ONLY_RENDERERS = java.util.Arrays.asList(
        mods.eln.node.six.SixNodeDescriptor.class, mods.eln.node.transparent.TransparentNodeDescriptor.class,
        mods.eln.generic.GenericItemUsingDamageDescriptor.class);
    private static final Map<Class<?>, Boolean> iconOnlyCache = new java.util.HashMap<>();

    /**
     * True when the stack's descriptor does not override the base icon-only renderItem: 1.7.10 drew those in hand and
     * on the ground as one single-sided quad (invisible edge-on, e.g. cables); 1.12 uses the vanilla generated
     * (extruded) item model of the same icon there, and the bridge only for the GUI (voltage-level background).
     */
    static boolean isIconOnly(ItemStack stack) {
        Object d = null;
        if (stack.getItem() instanceof mods.eln.generic.GenericItemBlockUsingDamage)
            d = ((mods.eln.generic.GenericItemBlockUsingDamage<?>) stack.getItem()).getDescriptor(stack);
        else if (stack.getItem() instanceof mods.eln.generic.GenericItemUsingDamage)
            d = ((mods.eln.generic.GenericItemUsingDamage<?>) stack.getItem()).getDescriptor(stack);
        if (d == null) return false;
        return iconOnlyCache.computeIfAbsent(d.getClass(), c -> {
            try {
                Class<?> owner = c.getMethod("renderItem", ItemRenderType.class, ItemStack.class, Object[].class)
                    .getDeclaringClass();
                return ICON_ONLY_RENDERERS.contains(owner);
            } catch (NoSuchMethodException e) {
                return false;
            }
        });
    }

    /** ItemTransforms icon_* key matrix for icon-only six-node items (cables), null = vanilla item/generated. */
    @Nullable
    static Matrix4f iconMatrix(TransformType t) {
        switch (t) {
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND:
                return ItemTransforms.matrix("icon_first_person");
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
                return ItemTransforms.matrix("icon_third_person");
            case GROUND:
                return ItemTransforms.matrix("icon_ground");
            default:
                return null;
        }
    }

    // --- the item-level model: only used to reach the overrides ---

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return Collections.emptyList();
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        if (base != null) return base.getParticleTexture();
        return Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrides;
    }

    /** The per-stack model. */
    class Stack implements IBakedModel {
        final ItemStack stack;
        @Nullable
        final EntityLivingBase entity;
        @Nullable
        final IBakedModel icon;
        final boolean guiHandled;
        /** Descriptor only draws its flat icon (1.7.10 single quad): outside the GUI use the icon's item model. */
        final boolean iconOnly;

        Stack(ItemStack stack, @Nullable EntityLivingBase entity) {
            this.stack = stack;
            this.entity = entity;
            this.icon = icons.get(stack.getItemDamage());
            this.iconOnly = icon != null && isIconOnly(stack);
            this.guiHandled = handles(ItemRenderType.INVENTORY);
        }

        boolean handles(@Nullable ItemRenderType type) {
            if (type == null || !(stack.getItem() instanceof IItemRenderer)) return false;
            if (iconOnly && type != ItemRenderType.INVENTORY) return false;
            try {
                return ((IItemRenderer) stack.getItem()).handleRenderType(stack, type);
            } catch (RuntimeException e) {
                return false;
            }
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
            ItemRenderType type = renderTypeOf(cameraTransformType);
            if (handles(type)) {
                ItemBridgeRenderer.prepare(type, cameraTransformType, stack, entity);
                return Pair.of(this, null);
            }
            if (icon != null) {
                Matrix4f m = iconOnly && stack.getItem() == mods.eln.Eln.sixNodeItem ? iconMatrix(cameraTransformType) : null;
                if (m != null) return Pair.of(icon, m);
                return icon.handlePerspective(cameraTransformType);
            }
            ItemBridgeRenderer.prepare(null, cameraTransformType, stack, entity); // nothing to draw
            return Pair.of(this, null);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return Collections.emptyList();
        }

        @Override
        public boolean isAmbientOcclusion() {
            return false;
        }

        /** GUI lighting (RenderItem.setupGuiTransform) and ground-item style: the icon's when the icon is drawn. */
        @Override
        public boolean isGui3d() {
            if (iconOnly) return icon.isGui3d();
            return guiHandled || icon == null || icon.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return true;
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return icon != null ? icon.getParticleTexture() : ItemBridgeModel.this.getParticleTexture();
        }

        /**
         * RenderEntityItem lifts a dropped item by 0.25 * this GROUND scale.y before the perspective transform: when the
         * icon model draws it, use the icon's (vanilla 0.5) so it does not float; the bridge's own "ground" key cancels
         * the full 0.25 of DEFAULT.
         */
        @Override
        public ItemCameraTransforms getItemCameraTransforms() {
            if (icon != null && !handles(ItemRenderType.ENTITY)) return icon.getItemCameraTransforms();
            return ItemCameraTransforms.DEFAULT;
        }

        @Override
        public ItemOverrideList getOverrides() {
            return ItemOverrideList.NONE;
        }
    }
}
