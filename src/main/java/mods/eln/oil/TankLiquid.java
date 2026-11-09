package mods.eln.oil;

import mods.eln.fluid.NodeFluidView;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A tank's liquid as a fluid capability (AdventurAgent 2026-10-29: a water bucket on a tank did nothing; Baughn: buckets
 * work on every EA fluid device, FluidBuckets): its vessel through NodeFluidView. LIQUID shows its top layer's main
 * liquid (water, oil...), to take out or add more of; INTAKE takes any liquid a species is (Baughn 2026-11-03: other
 * fluids are refused). Gases only by EA pipe. Room is the vessel's free volume up to TankGroup.FULL, and no more than
 * keeps the air over the liquid under the tank's working pressure (a tank has no vent: filled on to 100 % it burst),
 * shared by both slots.
 * <p>
 * In a box every block's view hands its calls to the box's one view (the master's), so the vessel's contents and room
 * are offered once, not once per block (two pipes on two blocks took twice the room, or drew the same liquid twice).
 * The block's own object stays the capability, so a pipe's cached handler keeps working when the box re-forms.
 */
public class TankLiquid extends NodeFluidView {
    /** The view the vessel's calls go to: this one, or the box's. */
    private final java.util.function.Supplier<NodeFluidView> primary;

    public TankLiquid(double workingPa, java.util.function.Supplier<NodeFluidView> primary) {
        super(new Spec().intake().liquidShare(TankGroup.FULL).cushion(workingPa));
        this.primary = primary;
    }

    /** The view that serves this block's calls (the box's, or this one). */
    public NodeFluidView serving() {
        NodeFluidView v = primary.get();
        return v == null ? this : v;
    }

    @Override
    public int size() {
        NodeFluidView v = serving();
        return v == this ? super.size() : v.size();
    }

    @Override
    public FluidResource getResource(int index) {
        NodeFluidView v = serving();
        return v == this ? super.getResource(index) : v.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        NodeFluidView v = serving();
        return v == this ? super.getAmountAsLong(index) : v.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        NodeFluidView v = serving();
        return v == this ? super.getCapacityAsLong(index, resource) : v.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        NodeFluidView v = serving();
        return v == this ? super.isValid(index, resource) : v.isValid(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        NodeFluidView v = serving();
        return v == this ? super.insert(index, resource, amount, tx) : v.insert(index, resource, amount, tx);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        NodeFluidView v = serving();
        return v == this ? super.extract(index, resource, amount, tx) : v.extract(index, resource, amount, tx);
    }

    /** The liquid shown (mB). */
    public int liquid() {
        return amount(LIQUID);
    }
}
