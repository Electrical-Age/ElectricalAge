package mods.eln.reservoir.mixin;

import mods.eln.reservoir.Reservoirs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A bucket of water from a reservoir's cell takes 1000 mB from its record (Baughn: no infinite water); from an empty
 *  reservoir, none. The sweep then puts the cells right. */
@Mixin(LiquidBlock.class)
public class LiquidBlockMixin {
    @Inject(method = "pickupBlock", at = @At("HEAD"), cancellable = true)
    private void eln$reservoir(LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state, CallbackInfoReturnable<ItemStack> cir) {
        if (level instanceof ServerLevel sl && state.getFluidState().isSource() && !Reservoirs.bucket(sl, pos)) cir.setReturnValue(ItemStack.EMPTY);
    }
}
