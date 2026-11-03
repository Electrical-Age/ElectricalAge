package mods.eln.fluid;

import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Species;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** A node's contents in a save: water, air and energy as always, other species by key, unknown ones kept. */
public class FluidSavesTest {
    static int define(String key) {
        int id = SpeciesRegistry.get().id(key);
        if (id >= 0) return id;
        return SpeciesRegistry.get().define(new Species(-1, key, Species.Kind.NONE, "test", 900, 2000, 1500, 0.3,
            0, 0, 0, 1e-3, 293, 1e-3, 293, 1e-5, 0)).id;
    }

    @Test
    void waterAndAirOnlyWriteTheOldKeys() {
        FluidNode n = new FluidNode(0.01, 0, 1, false).fill(3, 2e5, 350);
        CompoundTag t = new CompoundTag();
        FluidSaves.put(t, "vessel", n);
        assertFalse(t.contains("vesselS"), "no species compound for water and air");
        FluidSaves.Saved s = FluidSaves.get(t, "vessel");
        assertNotNull(s);
        assertEquals(n.mWater, s.water(), 0);
        assertEquals(n.mAir, s.air(), 0);
        assertEquals(n.energy, s.energy(), 0);
        assertNull(s.extra());
        assertNull(FluidSaves.get(t, "outlet"), "nothing saved there");
    }

    @Test
    void otherSpeciesRoundTripByKey() {
        int oil = define("test:oil");
        FluidNode n = new FluidNode(0.01, 0, 1, false).fill(1, Water.P_ATM, 300);
        n.addMass(oil, 2.5);
        n.energy = n.energyAt(300);
        n.update();
        CompoundTag t = new CompoundTag();
        FluidSaves.put(t, "p", n);
        FluidNode m = new FluidNode(0.01, 0, 1, false);
        FluidSaves.get(t, "p").applyTo(m);
        m.update();
        assertEquals(2.5, m.mass(oil), 0);
        assertEquals(n.P(), m.P(), 1e-9 * n.P());
    }

    @Test
    void anUnknownSpeciesIsKeptNotDropped() {
        CompoundTag t = new CompoundTag();
        t.putDouble("pW", 1);
        t.putDouble("pA", 0.01);
        t.putDouble("pU", 1e5);
        CompoundTag s = new CompoundTag();
        s.putDouble("othermod:mystery_goo", 0.75);
        t.put("pS", s);
        FluidNode m = new FluidNode(0.01, 0, 1, false);
        FluidSaves.get(t, "p").applyTo(m);
        m.update();
        int id = SpeciesRegistry.get().id("othermod:mystery_goo");
        assertEquals(0.75, m.mass(id), 0);
        // and it is written back under the same key
        CompoundTag t2 = new CompoundTag();
        FluidSaves.put(t2, "p", m);
        assertEquals(0.75, t2.getCompound("pS").get().getDoubleOr("othermod:mystery_goo", 0), 0);
    }
}
