package mods.eln.node.voxel;

import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A pipe saved when iron pipe held 0.15 L per block loads at the same pressure in today's 0.03 L (not at 5x). */
public class PipeVolumeUpgradeTest {
    @Test
    void oldSaveKeepsItsPressure() {
        PipePart old = new PipePart(0, new int[]{0, 8, 8, 16, 8, 8});   // one block, straight
        double vOld = 16 / 16.0 * PipePart.OLD_VOLUME_PER_BLOCK[0];
        var n = new mods.eln.sim.fluid.FluidNode(vOld, 0, 0.25, false).fill(0.2 * vOld * Water.RHO_L, 8e5, 420);
        CompoundTag t = new CompoundTag();
        t.putInt("tier", 0);
        t.putIntArray("pts", old.points);
        t.putDouble("w", n.mWater);
        t.putDouble("a", n.mAir);
        t.putDouble("u", n.energy);   // (no "v": an old save)
        PipePart p = PipePart.read(t, 0);
        assertEquals(PipePart.VOLUME_PER_BLOCK[0], p.fluid.volume, 1e-12);
        assertEquals(n.P(), p.fluid.P(), 1e-3 * n.P(), "pressure");
        assertEquals(n.T(), p.fluid.T(), 0.5, "temperature");
        // and a new save round-trips unscaled
        CompoundTag t2 = new CompoundTag();
        p.write(t2);
        PipePart q = PipePart.read(t2, 0);
        assertEquals(p.fluid.mWater, q.fluid.mWater, 1e-15);
    }
}
