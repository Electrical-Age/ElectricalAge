package mods.eln.mixin.client;

import mods.eln.client.LampBeam;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands LampBeam the projection the level is really drawn with: renderLevel multiplies the view bob, the hurt tilt and
 * the nausea warp into it, and CameraRenderState.projectionMatrix has none of them, so the beam shader would rebuild
 * world positions from depth with the wrong matrix and the beam would sway as the camera bobs. Optional: without it
 * LampBeam uses the camera's own projection.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /** No level projection outside renderLevel. */
    @Inject(method = "renderLevel", at = {@At("HEAD"), @At("RETURN")})
    private void eln$clearProjection(CallbackInfo ci) {
        LampBeam.levelProjection(null);
    }

    /** The level's projection, bob and all, as it goes to the GPU (before FrameGraphSetupEvent packs the beam data). */
    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
    private Matrix4f eln$levelProjection(Matrix4f projection) {
        LampBeam.levelProjection(projection);
        return projection;
    }
}
