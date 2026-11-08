package mods.eln.transparentnode.polymeriser;

import mods.eln.oil.Butadiene;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Polymeriser's feed tank (LabAgent: it takes butadiene as liquid or gas): slot 0 the liquid, slot 1 the vapour, so
 * a pipe's flange (PortBridge.pull) can put in either. Counted together as liquid mB by mass (FluidSpecies.kgPerMb).
 */
final class ButadieneFeed extends FluidStacksResourceHandler {
    /** Each slot holds up to mB. */
    ButadieneFeed(int mB) {
        super(2, mB);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource.isEmpty()) return false;
        return index == 0 ? resource.getFluid() == Butadiene.SOURCE.get() : resource.getFluid() == Butadiene.GAS.get();
    }

    int liquid() {
        return stacks.get(0).getAmount();
    }

    int gas() {
        return stacks.get(1).getAmount();
    }

    /** Vapour mB per liquid mB of the same mass. */
    static double gasPerLiquid() {
        int id = mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:butadiene");
        return mods.eln.fluid.FluidSpecies.kgPerMb(id, false) / mods.eln.fluid.FluidSpecies.kgPerMb(id, true);
    }

    /** All of it, as liquid mB. */
    double asLiquid() {
        return liquid() + gas() / gasPerLiquid();
    }

    /** Take mB (as liquid) of it: liquid first, then vapour. Returns what it took (as liquid mB). */
    double take(double mB) {
        double took = 0;
        int l = (int) Math.min(liquid(), Math.floor(mB));
        try (Transaction tx = Transaction.openRoot()) {
            if (l > 0) took += extract(0, FluidResource.of(Butadiene.SOURCE.get()), l, tx);
            double rest = mB - took, r = gasPerLiquid();
            int g = (int) Math.min(gas(), Math.ceil(rest * r));
            if (rest > 0 && g > 0) took += extract(1, FluidResource.of(Butadiene.GAS.get()), g, tx) / r;
            tx.commit();
        }
        return took;
    }

    /** (tests) */
    int fill(FluidStack f) {
        try (Transaction tx = Transaction.openRoot()) {
            int n = insert(f.getFluid() == Butadiene.GAS.get() ? 1 : 0, FluidResource.of(f), f.getAmount(), tx);
            tx.commit();
            return n;
        }
    }

    void write(CompoundTag nbt, String key) {
        nbt.putInt(key + "Liquid", liquid());
        nbt.putInt(key + "Gas", gas());
    }

    void read(CompoundTag nbt, String key) {
        int l = nbt.getIntOr(key + "Liquid", 0), g = nbt.getIntOr(key + "Gas", 0);
        stacks.set(0, l > 0 ? new FluidStack(Butadiene.SOURCE.get(), l) : FluidStack.EMPTY);
        stacks.set(1, g > 0 ? new FluidStack(Butadiene.GAS.get(), g) : FluidStack.EMPTY);
    }
}
