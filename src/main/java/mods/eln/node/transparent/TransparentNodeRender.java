package mods.eln.node.transparent;

import com.mojang.blaze3d.vertex.PoseStack;
import mods.eln.client.render.ElnRenderErrors;
import mods.eln.client.render.GlSubmit;
import mods.eln.gl.GL11;
import mods.eln.gl.GlFrame;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws a transparent-node device (1.12 TransparentNodeRender TESR) through the GL11 shim; see SixNodeRender. */
public class TransparentNodeRender implements BlockEntityRenderer<TransparentNodeEntity, TransparentNodeRender.State> {
    public static class State extends BlockEntityRenderState {
        TransparentNodeElementRender render;
        TransparentNodeEntity entity;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TransparentNodeEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.render = be.elementRender;
        state.entity = be;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.render == null) return;
        GlFrame frame = GlSubmit.openBlock(poseStack);
        // (a burnt device draws twice, the second time as a soot overlay that needs the first one's depth)
        if (!state.render.burnt) mods.eln.client.render.bake.ChunkBake.attach(frame, state.entity, state.lightCoords);
        if (mods.eln.gl.BakeStats.ON) mods.eln.gl.BakeStats.device(state.blockPos.asLong(), state.render.getClass().getSimpleName());
        try {
            GL11.glTranslatef(.5f, .5f, .5f);
            UtilsClient.glDefaultColor();
            GL11.glPushMatrix();
            state.render.draw();
            GL11.glPopMatrix();
            if (state.render.burnt) {
                frame.soot = true;
                UtilsClient.glDefaultColor();
                state.render.draw();
                frame.soot = false;
            }
        } catch (RuntimeException e) {
            ElnRenderErrors.report("transparent-node", e);
        } finally {
            mods.eln.gl.BakeStats.endDevice();
            GlFrame.close();
        }
        GlSubmit.submit(frame, poseStack, collector, state.lightCoords);
    }

    /** Multiblock devices (turbines, solar panels, poles) reach past their block: don't cull them by it. */
    @Override
    public AABB getRenderBoundingBox(TransparentNodeEntity be) {
        var r = be.elementRender == null ? null : be.elementRender.renderBounds();
        if (r != null) return r;
        return new AABB(be.getBlockPos()).inflate(3);
    }
}
