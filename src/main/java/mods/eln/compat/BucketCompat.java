package mods.eln.compat;

import mods.eln.misc.Utils;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**
 * 1.7.10 FluidContainerRegistry bucket checks over the 1.12 fluid item capability (added by wp10b, fuel generator).
 * A "filled bucket" is a stack whose fluid handler yields a full bucket (1000 mB) and leaves an empty vanilla bucket,
 * as 1.7.10 isBucket(stack) && isFilledContainer(stack).
 */
public final class BucketCompat {
    private BucketCompat() {
    }

    /** Fluid in a filled bucket (FluidContainerRegistry.getFluidForFilledItem for buckets), or null. */
    public static FluidStack filledBucketFluid(ItemStack stack) {
        if (Utils.isEmpty(stack)) return null;
        ItemStack one = stack.copy();
        one.setCount(1);
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(one);
        if (handler == null) return null;
        FluidStack drained = handler.drain(Fluid.BUCKET_VOLUME, true);
        if (drained == null || drained.amount < Fluid.BUCKET_VOLUME) return null;
        ItemStack empty = handler.getContainer();
        if (empty.isEmpty() || empty.getItem() != Items.BUCKET) return null;
        return drained;
    }

    /** The empty container left by a filled bucket (FluidContainerRegistry.drainFluidContainer). */
    public static ItemStack emptyBucket() {
        return new ItemStack(Items.BUCKET);
    }
}
