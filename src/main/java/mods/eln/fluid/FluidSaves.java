package mods.eln.fluid;

import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;
import net.minecraft.nbt.CompoundTag;

/**
 * A fluid node's contents in a save, under a key prefix: water, air and energy as they always were (key + "W", "A",
 * "U"), and the other species by their keys in a compound (key + "S": {"eln:crude_oil": kg, ...}), only when it holds
 * any. Saves from before species load as water and air; a species a save holds that isn't defined now is kept under
 * its key (SpeciesRegistry.forSave), never dropped.
 *
 * Format marker (key + "Fv", FORMAT): saves without it hold the legacy solver's energy. Its liquid and air energies match
 * the fluid core's, but its vapour's are up to ~40 kJ/kg lower, so on load the energy is taken at the temperature the
 * legacy solver found for it (Flash), and recomputed by the core at that temperature: nothing jumps on the upgrade.
 * The upgrade is one-way: a build without the core reads these saves' energies as its own.
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

    /** The fluid core's save format (the legacy solver's saves have no marker). */
    public static final int FORMAT = 2;

    /** Arrays from array() that hold a legacy energy (restore converts them). */
    private static final java.util.Set<double[]> LEGACY = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    public static void put(CompoundTag nbt, String key, FluidNode n) {
        nbt.putInt(key + "Fv", FORMAT);
        nbt.putDouble(key + "W", n.mWater);
        nbt.putDouble(key + "A", n.mAir);
        nbt.putDouble(key + "U", n.energy);
        putExtra(nbt, key + "S", n);
    }

    /** Only the other species, under tag (nothing if it holds none). */
    public static void putExtra(CompoundTag nbt, String tag, FluidNode n) {
        if (n.extra == null) return;
        CompoundTag s = null;
        SpeciesRegistry reg = SpeciesRegistry.get();
        for (int i = 0; i < n.extra.length; i++) {
            if (n.extra[i] == 0) continue;
            if (s == null) s = new CompoundTag();
            s.putDouble(reg.byId(i + 2).key, n.extra[i]);
        }
        if (s != null) nbt.put(tag, s);
    }

    /** The contents saved under key, or null if there are none. */
    public static Saved get(CompoundTag nbt, String key) {
        if (!nbt.contains(key + "W")) return null;
        return new Saved(nbt.getDoubleOr(key + "W", 0), nbt.getDoubleOr(key + "A", 0), nbt.getDoubleOr(key + "U", 0), extra(nbt, key + "S"));
    }

    /**
     * The contents saved under key as the devices keep them until their nodes exist: {water, air, energy} followed by
     * the other species (index 3 + i is species i + 2) if it held any; null if nothing is saved there.
     */
    public static double[] array(CompoundTag nbt, String key) {
        Saved s = get(nbt, key);
        if (s == null) return null;
        double[] out = s.wau();
        if (s.extra() != null) {
            out = java.util.Arrays.copyOf(out, 3 + s.extra().length);
            System.arraycopy(s.extra(), 0, out, 3, s.extra().length);
        }
        if (nbt.getIntOr(key + "Fv", 0) < FORMAT) LEGACY.add(out);
        return out;
    }

    /** Set n's contents to what array() gave (no update: the caller does it). */
    public static void restore(FluidNode n, double[] saved) {
        n.mWater = saved[0];
        n.mAir = saved[1];
        n.energy = saved[2];
        n.extra = saved.length > 3 ? java.util.Arrays.copyOfRange(saved, 3, saved.length) : null;
        if (LEGACY.contains(saved)) fromLegacy(n);
    }

    /**
     * A node just loaded with a legacy energy: that energy taken at the legacy solver's temperature for these contents,
     * the energy the fluid core gives them there instead (no update: the caller does it).
     */
    public static void fromLegacy(FluidNode n) {
        if (n.mass() <= 0) return;
        double T = new mods.eln.sim.fluid.Flash().solve(n.mWater, n.mAir, n.extra, n.energy, n.volume, 300).T;
        if (!(T > 0) || !Double.isFinite(T)) return;
        n.energy = n.energyAt(T);
    }

    /** The other species saved under tag (extra[i] is species i + 2), or null. */
    public static double[] extra(CompoundTag nbt, String tag) {
        double[] extra = null;
        var s = nbt.getCompound(tag);
        if (s.isPresent()) {
            SpeciesRegistry reg = SpeciesRegistry.get();
            for (String k : s.get().keySet()) {
                int id = reg.forSave(k);
                if (id < 2) continue;   // (the built-ins have their own keys)
                if (extra == null || id - 2 >= extra.length) extra = extra == null ? new double[id - 1] : java.util.Arrays.copyOf(extra, id - 1);
                extra[id - 2] += s.get().getDoubleOr(k, 0);
            }
        }
        return extra;
    }
}
