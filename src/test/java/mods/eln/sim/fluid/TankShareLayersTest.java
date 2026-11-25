package mods.eln.sim.fluid;

import mods.eln.oil.TankGroup;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TankGroup.share keeps immiscible liquids in their layers: the denser in the lower row, as a split box should hold them. */
public class TankShareLayersTest {
    static final double V1 = Double.parseDouble("0x1.bb4aa15141dd8p-11");

    @Test
    void dieselOverWaterSplitsByLayer() {
        int diesel = TestSpecies.id("diesel");
        double V = 4 * V1;
        // a box two blocks tall: 30 % water and 30 % diesel by volume, the diesel floating on the water
        FluidNode box = TestSpecies.node(V, 0, 2, 300, Water.P_ATM, SpeciesRegistry.WATER, 0.3 * V * Water.RHO_L, diesel, 0.3 * V * 840);
        assertEquals(2, box.layers());
        List<FluidNode> owns = new ArrayList<>();
        for (int i = 0; i < 4; i++) owns.add(new FluidNode(V1, 0, 1, false));
        TankGroup.share(box, 2, owns, new int[]{0, 0, 1, 1});
        double w = 0, d = 0, u = 0;
        for (FluidNode o : owns) {
            w += o.mWater;
            d += o.mass(diesel);
            u += o.energy;
            assertTrue(Double.isFinite(o.energy));
            assertEquals(box.T(), o.T(), 0.01);
        }
        assertEquals(box.mWater, w, 1e-9 * box.mWater);
        assertEquals(box.mass(diesel), d, 1e-9 * box.mass(diesel));
        assertEquals(box.energy, u, 1e-9 * Math.abs(box.energy));
        // the liquid column: 0.6 V over two rows of 0.49 V each: the bottom row holds 0.49 V (water 0.3, diesel 0.19), the
        // top row the other 0.11 V of diesel and no liquid water
        FluidNode low = owns.get(0), high = owns.get(2);
        assertEquals(box.liquid(SpeciesRegistry.WATER) / 2, low.liquid(SpeciesRegistry.WATER), 1e-3 * box.mWater);
        assertTrue(high.liquid(SpeciesRegistry.WATER) < 1e-3 * low.liquid(SpeciesRegistry.WATER), high.liquid(SpeciesRegistry.WATER) + " water up top");
        assertEquals(0.19 / 0.30, 2 * low.liquid(diesel) / box.liquid(diesel), 0.01);
    }
}
