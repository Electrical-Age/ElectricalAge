package mods.eln.robotics.mixin;

import mods.eln.robotics.servo.ElevatorCar;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player riding an elevator car stands on something: the server's floating kick (allow-flight off) counts only
 * blocks, and the car is not solid to players on the server (ElevatorCar.canBeCollidedWith).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class FloatingKickMixin {
    @Inject(method = "noBlocksAround", at = @At("RETURN"), cancellable = true)
    private void eln$onElevatorCar(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && ElevatorCar.carries(entity)) cir.setReturnValue(false);
    }
}
