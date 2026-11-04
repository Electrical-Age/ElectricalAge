package mods.eln.fluid;

import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.Species;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Which NeoForge fluid is which species, as a liquid or a vapour: water is minecraft:water and eln:steam; the others as
 * their datapack JSON says (SpeciesLoader). A fluid no species names is refused at EA's capabilities (Baughn 2026-11-03:
 * no guessing other mods' fluids). And how many kg one mB of each is: a liquid takes the room of as many mB of water
 * (EaUnits.KG_PER_MB_WATER at water's density), a vapour as many mB of steam (EaUnits.KG_PER_MB_STEAM, by molar mass).
 */
public final class FluidSpecies {
    private FluidSpecies() {
    }

    /** A species in a phase. */
    public record Phase(int id, boolean vapour) {
    }

    /** The species and phase a fluid is, or null if none (refused). */
    public static Phase of(Fluid f) {
        if (f == null || f == Fluids.EMPTY) return null;
        if (f.isSame(Fluids.WATER)) return new Phase(SpeciesRegistry.WATER, false);
        if (mods.eln.oil.Oil.STEAM != null && f.isSame(mods.eln.oil.Oil.STEAM.get())) return new Phase(SpeciesRegistry.WATER, true);
        String key = BuiltInRegistries.FLUID.getKey(f).toString();
        SpeciesRegistry reg = SpeciesRegistry.get();
        for (int id = 2; id < reg.size(); id++) {
            String sk = reg.byId(id).key;
            if (SpeciesLoader.liquidFluids(sk).contains(key)) return new Phase(id, false);
            if (SpeciesLoader.vapourFluids(sk).contains(key)) return new Phase(id, true);
        }
        return null;
    }

    /** The fluid species id is as a liquid (or a vapour), or null if it has none. */
    public static Fluid fluid(int id, boolean vapour) {
        if (id == SpeciesRegistry.WATER) return vapour ? mods.eln.oil.Oil.STEAM.get() : Fluids.WATER;
        if (id == SpeciesRegistry.AIR) return null;
        var names = vapour ? SpeciesLoader.vapourFluids(SpeciesRegistry.get().byId(id).key) : SpeciesLoader.liquidFluids(SpeciesRegistry.get().byId(id).key);
        for (String n : names) {
            Fluid f = BuiltInRegistries.FLUID.getValue(Identifier.parse(n));
            if (f != null && f != Fluids.EMPTY) return f;
        }
        return null;
    }

    /** kg per mB of species id as a liquid or a vapour. */
    public static double kgPerMb(int id, boolean vapour) {
        if (id == SpeciesRegistry.WATER) return vapour ? EaUnits.KG_PER_MB_STEAM : EaUnits.KG_PER_MB_WATER;
        Species s = SpeciesRegistry.get().byId(id);
        if (vapour || s.gas()) return EaUnits.KG_PER_MB_STEAM * s.molarMass / SpeciesRegistry.get().byId(SpeciesRegistry.WATER).molarMass;
        return EaUnits.KG_PER_MB_WATER * s.rhoL / Water.RHO_L;
    }

    /** The species of a node's top liquid layer with the most liquid there that is a fluid, or -1. */
    public static int topLiquid(mods.eln.sim.fluid.FluidNode n) {
        int best = -1;
        double most = 0;
        int top = -1;
        for (int id = 0; id < n.speciesBound(); id++) {
            if (n.liquid(id) <= 0 || fluid(id, false) == null) continue;
            int layer = n.layerOf(id);
            if (layer > top || layer == top && n.liquid(id) > most) {
                top = layer;
                best = id;
                most = n.liquid(id);
            }
        }
        return best;
    }

    /** The species with the most vapour in a node that is a fluid as a vapour (steam for water), or -1. */
    public static int mainVapour(mods.eln.sim.fluid.FluidNode n) {
        int best = -1;
        double most = 0;
        for (int id = 0; id < n.speciesBound(); id++) {
            if (id == mods.eln.sim.fluid.SpeciesRegistry.AIR || n.vapour(id) <= most || fluid(id, true) == null) continue;
            best = id;
            most = n.vapour(id);
        }
        return best;
    }
}
