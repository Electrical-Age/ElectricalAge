package mods.eln.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mods.eln.gl.GlFrame;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

import java.util.List;
import java.util.Map;

/** Submits a closed {@link GlFrame}'s vertex batches as custom geometry, one RenderType per batch key (P3). */
public final class GlSubmit {
    private GlSubmit() {
    }

    public static RenderType renderType(GlFrame.Key key) {
        if (key.blend() == GlFrame.Blend.LINES) return RenderTypes.lines();
        return ElnPipelines.type(key, key.texture() != null ? key.texture() : UtilsClient.WHITE);
    }

    /** Open a frame based on the renderer's pose (vertices come out in pose space). */
    public static GlFrame open(PoseStack poseStack) {
        PoseStack.Pose p = poseStack.last();
        return GlFrame.open(p.pose(), p.normal());
    }

    /** Submit every batch of the frame (call after GlFrame.close()); light = the block's packed light coords. */
    public static void submit(GlFrame frame, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        // extras last, from a copy: an item drawn by EA's own item renderer (an x-ray scanner in a battery charger)
        // reopens the shared frame, which clears its batches and extras (that crashed the game)
        List<GlFrame.Extra> extras = frame.extras().isEmpty() ? List.of() : new java.util.ArrayList<>(frame.extras());
        for (Map.Entry<GlFrame.Key, GlFrame.Batch> e : frame.batches().entrySet()) {
            GlFrame.Batch b = e.getValue();
            if (b.floats == 0) continue;
            int l = e.getKey().fullBright() ? LightCoordsUtil.FULL_BRIGHT : light;
            float[] d = b.data;
            int n = b.floats;
            // the frame was opened with this pose: vertices and normals are already in its space
            if (e.getKey().blend() == GlFrame.Blend.LINES) {
                collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (PoseStack.Pose pose, VertexConsumer vc) -> {
                    for (int i = 0; i < n; i += GlFrame.STRIDE)
                        vc.addVertex(d[i], d[i + 1], d[i + 2]).setColor(Float.floatToRawIntBits(d[i + 5]))
                            .setNormal(d[i + 6], d[i + 7], d[i + 8]).setLineWidth(d[i + 3]);
                });
                continue;
            }
            collector.submitCustomGeometry(poseStack, renderType(e.getKey()), (PoseStack.Pose pose, VertexConsumer vc) -> {
                for (int i = 0; i < n; i += GlFrame.STRIDE) {
                    vc.addVertex(d[i], d[i + 1], d[i + 2])
                        .setColor(Float.floatToRawIntBits(d[i + 5]))
                        .setUv(d[i + 3], d[i + 4])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(l)
                        .setNormal(d[i + 6], d[i + 7], d[i + 8]);
                }
            });
        }
        submitExtras(extras, collector, light);
    }

    /** Text and items the frame recorded, each at its own matrix (already in pose space). */
    static void submitExtras(List<GlFrame.Extra> extras, SubmitNodeCollector collector, int light) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        for (GlFrame.Extra e : extras) {
            PoseStack ps = new PoseStack();
            ps.last().pose().set(e.matrix());
            ps.last().normal().set(new org.joml.Matrix3f(e.matrix()).invert().transpose());
            if (e.payload() instanceof GlFrame.Text t) {
                int argb = (t.color() & 0xFF000000) == 0 ? t.color() | 0xFF000000 : t.color();
                collector.submitText(ps, t.x(), t.y(), net.minecraft.network.chat.Component.literal(t.text()).getVisualOrderText(), false,
                    net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET, t.fullBright() ? LightCoordsUtil.FULL_BRIGHT : light, argb, 0, 0);
            } else if (e.payload() instanceof net.minecraft.world.item.ItemStack stack) {
                // 1.12 drew an EntityItem: half a block, bottom at the origin
                ps.translate(0, 0.125f, 0);
                ps.scale(0.5f, 0.5f, 0.5f);
                var state = new net.minecraft.client.renderer.item.ItemStackRenderState();
                mc.getItemModelResolver().updateForTopItem(state, stack, net.minecraft.world.item.ItemDisplayContext.GROUND, mc.level, null, 0);
                state.submit(ps, collector, light, OverlayTexture.NO_OVERLAY, 0);
            }
        }
    }
}
