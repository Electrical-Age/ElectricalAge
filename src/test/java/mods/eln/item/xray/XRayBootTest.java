package mods.eln.item.xray;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XRayBootTest {
    @Test
    void theBarOnlyMovesForwardAndIsFullWhenReady() {
        for (long seed = 0; seed < 200; seed++) {
            double last = 0;
            for (int c = 80; c >= 1; c--) {
                double f = XRayBoot.fill(XRayBoot.time(c, 80), seed);
                assertTrue(f >= last, "seed " + seed + " goes back at " + c);
                last = f;
            }
            assertEquals(0, XRayBoot.fill(XRayBoot.time(80, 80), seed));
            assertEquals(1, last, 1e-9);
        }
    }

    @Test
    void theBarIsIrregularAndTheSameForOneBoot() {
        int steady = 0;
        for (long seed = 0; seed < 50; seed++) {
            assertEquals(XRayBoot.fill(0.37, seed), XRayBoot.fill(0.37, seed));
            if (Math.abs(XRayBoot.fill(0.5, seed) - 0.5) < 0.02) steady++;
        }
        assertTrue(steady < 10, "most boots are not a straight line: " + steady);
        assertTrue(XRayBoot.fill(0.5, 1) != XRayBoot.fill(0.5, 2), "boots differ");
    }
}
