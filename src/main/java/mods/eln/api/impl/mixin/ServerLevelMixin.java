package mods.eln.api.impl.mixin;

import mods.eln.api.impl.HostedMachines;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every block entity joining a server level (placed, or its chunk loaded) is offered to the Machine API's host,
 * which queues a registered type's for the next tick. NeoForge has no block entity load event.
 */
@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(method = "onBlockEntityAdded", at = @At("TAIL"))
    private void eln$hostMachine(BlockEntity blockEntity, CallbackInfo ci) {
        HostedMachines.added(blockEntity);
    }
}
