package mods.eln.compat;

import mods.eln.misc.Utils;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * 1.7.10 FluidContainerRegistry bucket checks (added by wp10b, fuel generator). A "filled bucket" holds a full bucket
 * (1000 mB) and leaves an empty vanilla bucket, as 1.7.10 isBucket(stack) && isFilledContainer(stack). 26.x: a
 * BucketItem with a fluid (vanilla and mod buckets alike).
 */
public final class BucketCompat {
    private BucketCompat() {
    }

    /** Fluid in a filled bucket (FluidContainerRegistry.getFluidForFilledItem for buckets), or null. */
    public static FluidStack filledBucketFluid(ItemStack stack) {
        if (Utils.isEmpty(stack)) return null;
        if (!(stack.getItem() instanceof BucketItem bucket) || bucket.getContent() == Fluids.EMPTY) return null;
        return new FluidStack(bucket.getContent(), FluidType.BUCKET_VOLUME);
    }

    /** The empty container left by a filled bucket (FluidContainerRegistry.drainFluidContainer). */
    public static ItemStack emptyBucket() {
        return new ItemStack(Items.BUCKET);
    }
}
