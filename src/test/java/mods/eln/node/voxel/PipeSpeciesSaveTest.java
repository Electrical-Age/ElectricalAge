package mods.eln.node.voxel;

import mods.eln.sim.fluid.Species;
import mods.eln.sim.fluid.SpeciesRegistry;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A pipe's other species survive its save, and an old save's volume upgrade scales them with the rest. */
public class PipeSpeciesSaveTest {
    static int oil() {
        int id = SpeciesRegistry.get().id("test:pipe_oil");
        if (id >= 0) return id;
        return SpeciesRegistry.get().define(new Species(-1, "test:pipe_oil", Species.Kind.NONE, "test_pipe", 850, 2000, 1500, 0.2,
            0, 0, 0, 1e-3, 293, 1e-3, 293, 1e-5, 0)).id;
    }

    @Test
    void speciesRoundTrip() {
        int oil = oil();
        PipePart p = new PipePart(0, new int[]{0, 8, 8, 16, 8, 8});
        p.makeNode(0, null);
        p.fluid.addMass(oil, 0.5 * p.fluid.volume * 850);
        p.fluid.energy = p.fluid.energyAt(300);
        p.fluid.update();
        CompoundTag t = new CompoundTag();
        p.write(t);
        PipePart q = PipePart.read(t, 0);
        assertEquals(p.fluid.mass(oil), q.fluid.mass(oil), 0);
        assertEquals(p.fluid.P(), q.fluid.P(), 1e-9 * p.fluid.P());
    }

    @Test
    void oldVolumeScalesEverySpecies() {
        int oil = oil();
        PipePart old = new PipePart(0, new int[]{0, 8, 8, 16, 8, 8});
        CompoundTag t = new CompoundTag();
        t.putInt("tier", 0);
        t.putIntArray("pts", old.points);
        t.putDouble("w", 0.001);
        t.putDouble("a", 1e-7);
        t.putDouble("u", 100);
        t.putDouble("v", 2 * PipePart.VOLUME_PER_BLOCK[0]);   // (saved at twice today's volume)
        CompoundTag s = new CompoundTag();
        s.putDouble("test:pipe_oil", 0.004);
        t.put("s", s);
        PipePart p = PipePart.read(t, 0);
        assertEquals(0.002, p.fluid.mass(oil), 1e-15);
        assertEquals(0.0005, p.fluid.mWater, 1e-15);
    }
}
