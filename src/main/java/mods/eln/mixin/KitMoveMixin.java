package mods.eln.mixin;

import mods.eln.node.voxel.KitItem;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A kit shift-clicked toward slots holding kits of its kind pours into them (Baughn 2026-11-28: kits merge wherever
 * they land): a kit is a one-item stack holding a length, which vanilla's stacking can't add up.
 */
@Mixin(AbstractContainerMenu.class)
public class KitMoveMixin {
    @Inject(method = "moveItemStackTo", at = @At("HEAD"), cancellable = true)
    private void eln$pourKit(ItemStack stack, int start, int end, boolean reverse, CallbackInfoReturnable<Boolean> cir) {
        if (KitItem.pourIntoSlotsOf((AbstractContainerMenu) (Object) this, stack, start, end, reverse)) cir.setReturnValue(true);
    }
}
