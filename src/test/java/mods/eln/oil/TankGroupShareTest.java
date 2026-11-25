package mods.eln.oil;

import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TankGroup.share: a box's contents into its blocks' shares, and back (the box re-formed from them on load). */
public class TankGroupShareTest {
    /** One iron tank's volume (m^3), as the live box's dump had it. */
    static final double V1 = Double.parseDouble("0x1.bb4aa15141dd8p-11");

    static List<FluidNode> owns(int count) {
        List<FluidNode> out = new ArrayList<>();
        for (int i = 0; i < count; i++) out.add(new FluidNode(V1, 0, 1, false));
        return out;
    }

    /** The box TankGroup's constructor forms from the shares. */
    static FluidNode reformed(List<FluidNode> owns, int sy) {
        FluidNode n = new FluidNode(owns.size() * V1, 0, sy, false);
        for (FluidNode o : owns) n.addShareOf(o, 1);
        n.update();
        return n;
    }

    static void assertSane(FluidNode box, List<FluidNode> owns, int sy) {
        double u = 0, w = 0, a = 0;
        for (FluidNode o : owns) {
            assertTrue(Double.isFinite(o.energy), "share energy " + o.energy);
            assertTrue(o.mWater >= 0 && o.mAir >= 0, "share masses " + o.mWater + " " + o.mAir);
            u += o.energy;
            w += o.mWater;
            a += o.mAir;
        }
        assertEquals(box.energy, u, 1e-9 * Math.abs(box.energy));
        assertEquals(box.mWater, w, 1e-9 * box.mWater);
        assertEquals(box.mAir, a, 1e-9 * box.mAir + 1e-18);
        FluidNode back = reformed(owns, sy);
        assertEquals(box.T(), back.T(), 1e-6);
        assertEquals(box.P(), back.P(), 1e-6 * box.P());
    }

    /**
     * Incognito 2026-11-25: a 2 x 1 x 2 iron tank box pumped full by a submersible pump (6.3 bar), its liquid 1.000047 of
     * its volume at nominal density. The layered split gave each block -0.22 kg of air and NaN energy; the box reloaded
     * from them fell 1 K per update to the EOS floor (-153 C). Now each block takes a quarter of everything.
     */
    @Test
    void overFullBoxSharesByVolume() {
        FluidNode box = new FluidNode(4 * V1, 0, 1, false);
        box.mWater = Double.parseDouble("0x1.9ebd4d449c218p1");   // 3.2402 kg
        box.mAir = Double.parseDouble("0x1.7222fdb0c384fp-18");   // 5.5e-6 kg
        box.energy = Double.parseDouble("0x1.e5488df4bde1p17");   // 248465 J
        box.update();
        assertTrue(box.liquidVolume() > box.volume, "over-full: " + box.liquidVolume() / box.volume);
        List<FluidNode> owns = owns(4);
        TankGroup.share(box, 1, owns, new int[4]);
        assertSane(box, owns, 1);
        for (FluidNode o : owns) assertEquals(box.mWater / 4, o.mWater, 1e-12);
        assertEquals(291.47, owns.get(0).T(), 0.1);
    }

    /** A box two blocks tall, 98.5 % full (over FULL, under its volume): the same volume split, nothing lost. */
    @Test
    void nearlyFullTallBox() {
        FluidNode box = new FluidNode(4 * V1, 0, 2, false).fill(0.985 * 4 * V1 * Water.RHO_L, 3e5, 300);
        List<FluidNode> owns = owns(4);
        TankGroup.share(box, 2, owns, new int[]{0, 0, 1, 1});
        assertSane(box, owns, 2);
    }

    /** Half full, two layers: the liquid in the bottom layer (98 % of it, the 2 % over FULL in the top one), gas over it, as before. */
    @Test
    void halfFullBoxLayers() {
        FluidNode box = new FluidNode(4 * V1, 0, 2, false).fill(0.5 * 4 * V1 * Water.RHO_L, Water.P_ATM, 293.15);
        List<FluidNode> owns = owns(4);
        TankGroup.share(box, 2, owns, new int[]{0, 0, 1, 1});
        assertSane(box, owns, 2);
        assertTrue(owns.get(0).mLiquid() > 30 * owns.get(2).mLiquid(), owns.get(0).mLiquid() + " under " + owns.get(2).mLiquid());
    }
}
