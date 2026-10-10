package mods.eln.node.six;

import com.mojang.blaze3d.vertex.PoseStack;
import mods.eln.client.render.ElnRenderErrors;
import mods.eln.client.render.GlSubmit;
import mods.eln.gl.GL11;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Direction;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a six-node block's elements (1.12 SixNodeRender TESR). Each element render runs its 1.12 draw() against the
 * GL11 shim inside a GlFrame; the frame's batches are submitted as custom geometry. The render objects are read in
 * submit (not copied in extract): extract and submit run on the render thread in the same frame (P3 plan, point 5).
 */
public class SixNodeRender implements BlockEntityRenderer<SixNodeEntity, SixNodeRender.State> {
    public static class State extends BlockEntityRenderState {
        SixNodeElementRender[] renders = new SixNodeElementRender[6];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SixNodeEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        System.arraycopy(be.elementRenderList, 0, state.renders, 0, 6);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        GlFrame frame = GlFrame.open();
        try {
            GL11.glTranslatef(.5f, .5f, .5f);
            for (int idx = 0; idx < 6; idx++) {
                SixNodeElementRender render = state.renders[idx];
                if (render == null) continue;
                UtilsClient.glDefaultColor();
                GL11.glPushMatrix();
                Direction.fromInt(idx).glRotateXnRef();
                GL11.glTranslatef(-0.5f, 0f, 0f);
                render.draw();
                GL11.glPopMatrix();
            }
        } catch (RuntimeException e) {
            ElnRenderErrors.report("six-node", e);
        } finally {
            GlFrame.close();
        }
        GlSubmit.submit(frame, poseStack, collector, state.lightCoords);
    }
}
