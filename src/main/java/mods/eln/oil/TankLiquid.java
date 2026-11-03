package mods.eln.oil;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A tank's liquid as a fluid capability (AdventurAgent 2026-10-29: a water bucket on a tank did nothing; Baughn: buckets
 * work on every EA fluid device, FluidBuckets): a view of its vessel. Tank 0 shows its top layer's main liquid (water,
 * oil...), to take out or add more of; tank 1 is an intake any liquid a species is can be put into (Baughn 2026-11-03:
 * other fluids are refused). Gases only by EA pipe. Each fluid step the vessel takes what was put in or taken out since
 * the last one, then it shows its liquid again.
 */
public class TankLiquid extends FluidStacksResourceHandler {
    public static final int SHOWN = 0, INTAKE = 1;
    private int capacity = 1, room, shown;
    private Fluid shownFluid;

    public TankLiquid() {
        super(2, Integer.MAX_VALUE);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource.isEmpty()) return false;
        var p = mods.eln.fluid.FluidSpecies.of(resource.getFluid());
        return p != null && !p.vapour();
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return index == SHOWN ? capacity : Math.max(0, room);
    }

    /** The liquid shown (mB). */
    public int liquid() {
        return stacks.get(SHOWN).getAmount();
    }

    /** Show the vessel: its top liquid (fluid, mB), the most it holds and its room left (mB of liquid). */
    void show(Fluid fluid, int amount, int capacity, int room) {
        this.capacity = Math.max(1, capacity);
        this.room = room;
        int w = fluid == null ? 0 : Math.max(0, Math.min(amount, this.capacity));
        set(SHOWN, w > 0 ? FluidResource.of(fluid) : FluidResource.EMPTY, w);
        set(INTAKE, FluidResource.EMPTY, 0);
        shown = w;
        shownFluid = fluid;
    }

    /** What was put in (positive) or taken out (negative) since it was last shown, per fluid (mB). */
    List<Map.Entry<Fluid, Integer>> change() {
        List<Map.Entry<Fluid, Integer>> out = new ArrayList<>();
        var st = stacks.get(SHOWN);
        Fluid f = st.isEmpty() ? shownFluid : st.getFluid();
        int d = st.getAmount() - shown;
        if (d != 0 && f != null) out.add(Map.entry(f, d));
        shown = st.getAmount();
        var in = stacks.get(INTAKE);
        if (!in.isEmpty() && in.getAmount() > 0) out.add(Map.entry(in.getFluid(), in.getAmount()));
        set(INTAKE, FluidResource.EMPTY, 0);
        return out;
    }
}
