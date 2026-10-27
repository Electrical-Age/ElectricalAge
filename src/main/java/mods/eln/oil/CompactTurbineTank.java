package mods.eln.oil;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The compact turbine's water tank as a fluid capability: water goes in, nothing comes out. The element keeps the
 * water in mB as a double (it uses fractions of a mB a tick); this shows it, and takes in what pipes put in since.
 */
public class CompactTurbineTank extends FluidStacksResourceHandler {
    private int shown;

    public CompactTurbineTank() {
        super(1, CompactTurbineDescriptor.WATER_MB);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && resource.getFluid().isSame(Fluids.WATER);
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return CompactTurbineDescriptor.WATER_MB;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        return 0;   // (water stays in)
    }

    public int water() {
        return stacks.get(0).getAmount();
    }

    /** Water pipes put in since the last show (mB). */
    int takeIn() {
        int d = Math.max(0, water() - shown);
        shown = water();
        return d;
    }

    /** Show the element's water (mB, rounded down). */
    void show(double mB) {
        int n = (int) Math.max(0, Math.min(CompactTurbineDescriptor.WATER_MB, Math.floor(mB)));
        set(0, n > 0 ? FluidResource.of(Fluids.WATER) : FluidResource.EMPTY, n);
        shown = n;
    }
}
