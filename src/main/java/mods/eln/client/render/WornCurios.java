package mods.eln.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * EA's Curios accessories drawn on the wearer (Baughn: the curios show in third person, as other mods' do): the battery
 * belt round the waist, the headlamp on the forehead. Curios draws them only while the slot's render toggle is on. No
 * hard dependency: Curios classes are touched only after {@link mods.eln.item.belt.CuriosCompat#loaded}.
 */
public final class WornCurios {
    private WornCurios() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        if (mods.eln.item.belt.CuriosCompat.loaded())
            modBus.addListener(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent.class, e -> e.enqueueWork(Impl::register));
    }

    /** The belt's item texture: its 16x16 icon is the belt seen from the front (pouches with cells, the buckle in the middle). */
    static final Identifier BELT = Identifier.fromNamespaceAndPath("eln", "textures/item/battery_belt.png");

    /** How far the belt band stands off the body, in model pixels: just over a chestplate's 1 px. */
    static final float BELT_GAP = 1.2f;

    /**
     * The band, in the body's space (pixels, y down, the front at -z): 3 px tall at the waist, its faces cut from the icon
     * (the front: pouches and buckle; back and sides: a pouch each; top and bottom: the dark strap).
     */
    static void belt(PoseStack.Pose p, VertexConsumer vc, int light) {
        float x = 4 + BELT_GAP, z = 2 + BELT_GAP, y0 = 9.5f, y1 = 12.5f;
        // u0, u1, v0, v1 in icon pixels
        float[] front = {2.5f, 12.5f, 7, 10}, back = {9, 15, 7, 10}, side = {1, 5, 7, 10}, strap = {1, 2, 11, 12};
        quad(p, vc, light, front, 0, 0, -1, x, y0, -z, -x, y0, -z, -x, y1, -z, x, y1, -z);
        quad(p, vc, light, back, 0, 0, 1, -x, y0, z, x, y0, z, x, y1, z, -x, y1, z);
        quad(p, vc, light, side, -1, 0, 0, -x, y0, -z, -x, y0, z, -x, y1, z, -x, y1, -z);
        quad(p, vc, light, side, 1, 0, 0, x, y0, z, x, y0, -z, x, y1, -z, x, y1, z);
        quad(p, vc, light, strap, 0, -1, 0, -x, y0, -z, x, y0, -z, x, y0, z, -x, y0, z);
        quad(p, vc, light, strap, 0, 1, 0, -x, y1, z, x, y1, z, x, y1, -z, -x, y1, -z);
    }

    /** A quad (corners in pixels: top-left, top-right, bottom-right, bottom-left of the uv rectangle). */
    static void quad(PoseStack.Pose p, VertexConsumer vc, int light, float[] uv, float nx, float ny, float nz, float... c) {
        float[][] corner = {{uv[0], uv[2]}, {uv[1], uv[2]}, {uv[1], uv[3]}, {uv[0], uv[3]}};
        for (int i = 0; i < 4; i++)
            vc.addVertex(p, c[3 * i] / 16f, c[3 * i + 1] / 16f, c[3 * i + 2] / 16f).setColor(-1)
                .setUv(corner[i][0] / 16f, corner[i][1] / 16f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    private static final class Impl {
        static void register() {
            top.theillusivec4.curios.api.client.ICurioRenderer.register(mods.eln.registry.ElnRegistry.BATTERY_BELT.get(), () -> new Belt());
            top.theillusivec4.curios.api.client.ICurioRenderer.register(mods.eln.registry.ElnRegistry.HEADLAMP.get(), () -> new Headlamp());
        }
    }

    /** The battery belt: a band round the waist, following the body (sneaking leans it). */
    static final class Belt implements top.theillusivec4.curios.api.client.ICurioRenderer {
        @Override
        public <S extends LivingEntityRenderState, M extends net.minecraft.client.model.EntityModel<? super S>> void render(
            ItemStack stack, top.theillusivec4.curios.api.SlotContext ctx, PoseStack pose, SubmitNodeCollector out, int light, S state,
            net.minecraft.client.renderer.entity.RenderLayerParent<S, M> parent,
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context, float yRot, float xRot) {
            if (!(parent.getModel() instanceof HumanoidModel<?> m)) return;
            pose.pushPose();
            m.root().translateAndRotate(pose);
            m.body.translateAndRotate(pose);
            out.submitCustomGeometry(pose, net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(BELT), (p, vc) -> belt(p, vc, light));
            pose.popPose();
        }
    }

    /**
     * The headlamp: its item model (a band round a head, the lamp at its front) on the forehead, following the head. The
     * model's band is 10 px across; 1.15 times that clears the head's corners.
     */
    static final class Headlamp implements top.theillusivec4.curios.api.client.ICurioRenderer {
        static final float SCALE = 1.15f;

        @Override
        public <S extends LivingEntityRenderState, M extends net.minecraft.client.model.EntityModel<? super S>> void render(
            ItemStack stack, top.theillusivec4.curios.api.SlotContext ctx, PoseStack pose, SubmitNodeCollector out, int light, S state,
            net.minecraft.client.renderer.entity.RenderLayerParent<S, M> parent,
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context, float yRot, float xRot) {
            if (!(parent.getModel() instanceof HumanoidModel<?> m)) return;
            pose.pushPose();
            m.root().translateAndRotate(pose);
            m.head.translateAndRotate(pose);
            // the band's middle (1 px below the model's centre) at 5.5 px above the neck, just over the eyes; the model
            // upright (the head's space is y down) with its lamp to the front (-z in both)
            pose.translate(0, -(5.5f + SCALE) / 16f, 0);
            pose.rotateDegrees(Axis.ZP, 180);
            pose.scale(SCALE, SCALE, SCALE);
            item(stack, ctx, pose, out, light, state);
            pose.popPose();
        }
    }

    /** The item's own model, centred at the pose's origin (no display transform). */
    static void item(ItemStack stack, top.theillusivec4.curios.api.SlotContext ctx, PoseStack pose, SubmitNodeCollector out, int light,
                     LivingEntityRenderState state) {
        var r = new net.minecraft.client.renderer.item.ItemStackRenderState();
        net.minecraft.client.Minecraft.getInstance().getItemModelResolver()
            .updateForLiving(r, stack, net.minecraft.world.item.ItemDisplayContext.NONE, ctx.entity());
        r.submit(pose, out, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }
}
