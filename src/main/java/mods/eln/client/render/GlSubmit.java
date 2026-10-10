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

import java.util.Map;

/** Submits a closed {@link GlFrame}'s vertex batches as custom geometry, one RenderType per batch key (P3). */
public final class GlSubmit {
    private GlSubmit() {
    }

    public static RenderType renderType(GlFrame.Key key) {
        Identifier tex = key.texture() != null ? key.texture() : UtilsClient.WHITE;
        return switch (key.blend()) {
            case NONE -> key.cull() ? RenderTypes.entityCutoutCull(tex) : RenderTypes.entityCutout(tex);
            case ALPHA, MULTIPLY -> key.cull() ? RenderTypes.entityTranslucentCull(tex) : RenderTypes.entityTranslucent(tex);
            case ADDITIVE -> RenderTypes.entityTranslucentEmissive(tex); // R4: a real additive pipeline
        };
    }

    /** Open a frame based on the renderer's pose (vertices come out in pose space). */
    public static GlFrame open(PoseStack poseStack) {
        PoseStack.Pose p = poseStack.last();
        return GlFrame.open(p.pose(), p.normal());
    }

    /** Submit every batch of the frame (call after GlFrame.close()); light = the block's packed light coords. */
    public static void submit(GlFrame frame, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        for (Map.Entry<GlFrame.Key, GlFrame.Batch> e : frame.batches().entrySet()) {
            GlFrame.Batch b = e.getValue();
            if (b.floats == 0) continue;
            int l = e.getKey().fullBright() ? LightCoordsUtil.FULL_BRIGHT : light;
            float[] d = b.data;
            int n = b.floats;
            // the frame was opened with this pose: vertices and normals are already in its space
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
    }
}
