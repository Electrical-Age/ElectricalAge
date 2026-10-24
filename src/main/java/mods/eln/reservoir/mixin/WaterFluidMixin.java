package mods.eln.reservoir.mixin;

import mods.eln.reservoir.ReservoirWaterFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla water and a reservoir's partial surface are one body of water: no faces between them, no flow into it
 *  (AdventurAgent: both drew faces where they met). */
@Mixin(WaterFluid.class)
public class WaterFluidMixin {
    @Inject(method = "isSame", at = @At("HEAD"), cancellable = true)
    private void eln$reservoirWater(Fluid other, CallbackInfoReturnable<Boolean> cir) {
        if (other instanceof ReservoirWaterFluid) cir.setReturnValue(true);
    }
}
