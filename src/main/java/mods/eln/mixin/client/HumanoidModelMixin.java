package mods.eln.mixin.client;

import mods.eln.client.entity.RiderPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A bike rider's pose (Baughn: legs on the pedals): after vanilla's setupAnim, which gives a passenger its sitting
 * legs, RiderPose sets the legs to follow the pedals and the arms to the grips when the render state carries a pedal
 * (set by RiderPose's render-state modifier for players on an ElectricBike). Look only: the model is posed, nothing else.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void eln$bikeRider(HumanoidRenderState state, CallbackInfo ci) {
        RiderPose.apply((HumanoidModel<?>) (Object) this, state);
    }
}
