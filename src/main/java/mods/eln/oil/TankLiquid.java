package mods.eln.oil;

import mods.eln.fluid.NodeFluidView;

/**
 * A tank's liquid as a fluid capability (AdventurAgent 2026-10-29: a water bucket on a tank did nothing; Baughn: buckets
 * work on every EA fluid device, FluidBuckets): its vessel through NodeFluidView. LIQUID shows its top layer's main
 * liquid (water, oil...), to take out or add more of; INTAKE takes any liquid a species is (Baughn 2026-11-03: other
 * fluids are refused). Gases only by EA pipe. Room is the vessel's real free volume, shared by both slots.
 */
public class TankLiquid extends NodeFluidView {
    public TankLiquid() {
        super(new Spec().intake());
    }

    /** The liquid shown (mB). */
    public int liquid() {
        return amount(LIQUID);
    }
}
