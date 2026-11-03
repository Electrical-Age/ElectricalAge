package mods.eln.fluid;

import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;
import net.minecraft.nbt.CompoundTag;

/**
 * A fluid node's contents in a save, under a key prefix: water, air and energy as they always were (key + "W", "A",
 * "U"), and the other species by their keys in a compound (key + "S": {"eln:crude_oil": kg, ...}), only when it holds
 * any. Saves from before species load as water and air; a species a save holds that isn't defined now is kept under
 * its key (SpeciesRegistry.forSave), never dropped.
 */
public final class FluidSaves {
    private FluidSaves() {
    }

    /** What a save holds for one node. */
    public record Saved(double water, double air, double energy, double[] extra) {
        /** The old three-number form (water, air, energy) the devices restore from. */
        public double[] wau() {
            return new double[]{water, air, energy};
        }

        /** Set n's contents to these (no update: the caller does it). */
        public void applyTo(FluidNode n) {
            n.mWater = water;
            n.mAir = air;
            n.energy = energy;
            n.extra = extra == null ? null : extra.clone();
        }
    }

    public static void put(CompoundTag nbt, String key, FluidNode n) {
        nbt.putDouble(key + "W", n.mWater);
        nbt.putDouble(key + "A", n.mAir);
        nbt.putDouble(key + "U", n.energy);
        if (n.extra == null) return;
        CompoundTag s = null;
        SpeciesRegistry reg = SpeciesRegistry.get();
        for (int i = 0; i < n.extra.length; i++) {
            if (n.extra[i] == 0) continue;
            if (s == null) s = new CompoundTag();
            s.putDouble(reg.byId(i + 2).key, n.extra[i]);
        }
        if (s != null) nbt.put(key + "S", s);
    }

    /** The contents saved under key, or null if there are none. */
    public static Saved get(CompoundTag nbt, String key) {
        if (!nbt.contains(key + "W")) return null;
        double[] extra = null;
        var s = nbt.getCompound(key + "S");
        if (s.isPresent()) {
            SpeciesRegistry reg = SpeciesRegistry.get();
            for (String k : s.get().keySet()) {
                int id = reg.forSave(k);
                if (id < 2) continue;   // (the built-ins have their own keys)
                if (extra == null || id - 2 >= extra.length) extra = extra == null ? new double[id - 1] : java.util.Arrays.copyOf(extra, id - 1);
                extra[id - 2] += s.get().getDoubleOr(k, 0);
            }
        }
        return new Saved(nbt.getDoubleOr(key + "W", 0), nbt.getDoubleOr(key + "A", 0), nbt.getDoubleOr(key + "U", 0), extra);
    }
}
