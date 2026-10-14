package mods.eln.oil;

import mods.eln.fluid.ElementFluidHandler;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** A machine's output tank: pipes and buckets take from it, but only the machine fills it ({@link #put}). */
public class OutputTank extends ElementFluidHandler {
    private boolean filling;

    public OutputTank(int mB) {
        super(mB);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        return filling ? super.insert(index, resource, amount, tx) : 0;
    }

    /** Room for this fluid (0 if the tank holds another). */
    public int room(Fluid fluid) {
        if (getFluidAmount() > 0 && !getFluid().getFluid().isSame(fluid)) return 0;
        return getCapacity() - getFluidAmount();
    }

    /** The machine fills it: what went in. */
    public int put(Fluid fluid, int mB) {
        if (mB <= 0) return 0;
        filling = true;
        try {
            return fill(new FluidStack(fluid, mB), true);
        } finally {
            filling = false;
        }
    }
}
