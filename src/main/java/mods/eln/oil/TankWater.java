package mods.eln.oil;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/**
 * A tank's water as a fluid capability (AdventurAgent 2026-10-29: a water bucket on a tank did nothing; Baughn: buckets
 * work on every EA fluid device, FluidBuckets): a view of its vessel's liquid water, as BoilerTanks is of the boiler's.
 * Water goes in and comes out (a bucket, another mod's pipe); steam only by EA pipe. Each fluid step the vessel takes
 * what was put in or taken out since the last one, then it shows the vessel's water again.
 */
public class TankWater extends FluidStacksResourceHandler {
    private int capacity = 1, shown;

    public TankWater() {
        super(1, Integer.MAX_VALUE);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && resource.getFluid().isSame(Fluids.WATER);
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return capacity;
    }

    public int water() {
        return stacks.get(0).getAmount();
    }

    /** Show the vessel: its liquid water and the most it holds (mB). */
    void show(int water, int capacity) {
        this.capacity = Math.max(1, capacity);
        int w = Math.max(0, Math.min(water, this.capacity));
        set(0, w > 0 ? FluidResource.of(Fluids.WATER) : FluidResource.EMPTY, w);
        shown = w;
    }

    /** What was put in (positive) or taken out (negative) since it was last shown (mB). */
    int change() {
        int d = water() - shown;
        shown = water();
        return d;
    }
}
