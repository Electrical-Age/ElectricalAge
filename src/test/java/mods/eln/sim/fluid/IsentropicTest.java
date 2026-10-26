package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Water.isentropicDrop against steam tables (saturated inlet), within the model's ~5% (Baughn 2026-10-26: turbines). */
public class IsentropicTest {
    static double drop(double pIn, double pOut) {
        return Water.isentropicDrop(Water.tsat(pIn), pIn, pOut) / 1e3;
    }

    @Test
    public void matchesSteamTables() {
        assertEquals(116, drop(2e5, Water.P_ATM), 116 * 0.05);
        assertEquals(389, drop(10e5, Water.P_ATM), 389 * 0.06);
        assertEquals(449, drop(2e5, 0.1e5), 449 * 0.05);
        assertEquals(692, drop(10e5, 0.1e5), 692 * 0.05);
        assertEquals(822, drop(25e5, 0.1e5), 822 * 0.08);   // (the model runs high at higher pressures)
    }

    @Test
    public void noDropWithoutAPressureFall() {
        assertEquals(0, drop(2e5, 2e5));
        assertEquals(0, drop(2e5, 3e5));
        assertTrue(drop(20e5, Water.P_ATM) > drop(10e5, Water.P_ATM));
    }

    @Test
    public void superheatedInletGivesMore() {
        double sat = Water.isentropicDrop(Water.tsat(10e5), 10e5, Water.P_ATM);
        double sup = Water.isentropicDrop(Water.tsat(10e5) + 100, 10e5, Water.P_ATM);
        assertTrue(sup > sat);
    }
}
