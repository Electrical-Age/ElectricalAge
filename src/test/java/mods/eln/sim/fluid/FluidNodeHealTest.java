package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FluidNode.heal and guardFinite: corrupt contents put right instead of cooling 1 K per update to 120 K. */
public class FluidNodeHealTest {
    /** One iron tank's volume (m^3). */
    static final double V1 = Double.parseDouble("0x1.bb4aa15141dd8p-11");

    /** Incognito 2026-11-25: a tank loaded with NaN energy; before the guard, T read 26.85, 25.85, ... down to -153.15 C. */
    @Test
    void nanEnergyHealsAtTwentyDegreesOnUpdate() {
        FluidNode n = new FluidNode(V1, 0, 1, false);
        n.mWater = 0.8100512623786926;
        n.mAir = 0;
        n.energy = Double.NaN;
        n.update();
        assertTrue(Double.isFinite(n.energy), "energy " + n.energy);
        assertEquals(Water.T_AMBIENT, n.T(), 0.01);
        double t = n.T();
        for (int i = 0; i < 10; i++) n.update();
        assertEquals(t, n.T(), 1e-9);
        assertEquals(0.8100512623786926, n.mWater, 0);
    }

    @Test
    void negativeAirHealsAtTheGivenAmbient() {
        FluidNode n = new FluidNode(V1, 0, 1, false).fill(0.4, Water.P_ATM, 330);
        double w = n.mWater;
        n.mAir = -0.16;
        assertTrue(n.heal(280));
        n.update();
        assertEquals(0, n.mAir, 0);
        assertEquals(w, n.mWater, 0);
        assertEquals(280, n.T(), 0.01);
    }

    /** A sound node, or one with a rounding trace of negative air, is left as it is. */
    @Test
    void soundContentsUntouched() {
        FluidNode n = new FluidNode(V1, 0, 1, false).fill(0.4, Water.P_ATM, 330);
        double u = n.energy, a = n.mAir;
        assertFalse(n.heal(280));
        assertFalse(n.guardFinite());
        assertEquals(u, n.energy, 0);
        assertEquals(a, n.mAir, 0);
        n.mAir = -1e-12;
        assertFalse(n.heal(280));
        assertEquals(u, n.energy, 0);
    }

    /** At runtime a hot node heals at its last temperature, not at 20 C (Gemini's review: a 550 K boiler reset to 20 C). */
    @Test
    void runtimeGuardKeepsTheLastTemperature() {
        FluidNode n = new FluidNode(V1, 0, 1, false).fill(0.3, 20e5, 480);
        n.energy = Double.NaN;
        assertTrue(n.guardFinite());
        n.update();
        assertEquals(480, n.T(), 0.5);
    }

    /** The guard takes a negative mass as heal does. */
    @Test
    void runtimeGuardCatchesNegativeMass() {
        FluidNode n = new FluidNode(V1, 0, 1, false).fill(0.3, Water.P_ATM, 300);
        n.mAir = -0.05;
        n.update();
        assertEquals(0, n.mAir, 0);
        assertEquals(300, n.T(), 0.5);
    }
}
