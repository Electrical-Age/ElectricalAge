package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fuel quality (refinery M1): the rating of a liquid layer for an engine's boiling window, the derating, the fouling law. */
class FuelQualityTest {
    static final double LO = FuelQuality.TURBINE_LO, HI = FuelQuality.TURBINE_HI;

    static FuelQuality.Rating rate(double lo, double hi, Object... nameKg) {
        double[] idKg = new double[nameKg.length];
        for (int i = 0; i < nameKg.length; i += 2) {
            idKg[i] = TestSpecies.id((String) nameKg[i]);
            idKg[i + 1] = (Double) nameKg[i + 1];
        }
        FluidNode n = TestSpecies.node(0.1, 0, 1, 293.15, 1e5, idKg);
        return FuelQuality.rate(n, (int) idKg[0], lo, hi);
    }

    @Test
    void todaysFuelsOnTheGasTurbine() {
        for (String f : new String[]{"gasoline", "diesel", "heavy_oil"}) {
            var r = rate(LO, HI, f, 10.0);
            assertEquals(0, r.off(), 1e-12, f);
            assertEquals(1, r.power(), 1e-12, f);
        }
        assertEquals(0, rate(LO, HI, "gasoline", 10.0).coking(), 1e-12);
        assertEquals(0.01, rate(LO, HI, "diesel", 10.0).coking(), 1e-12);
        assertEquals(0.15, rate(LO, HI, "heavy_oil", 10.0).coking(), 1e-12);
        // gasoline and diesel clear an engine, heavy oil fouls it: 0.39 an hour at full power
        assertTrue(FuelQuality.foulRate(0, FuelQuality.ENGINE_BALANCE) < 0);
        assertTrue(FuelQuality.foulRate(0.01, FuelQuality.ENGINE_BALANCE) < 0);
        assertEquals(0.39, FuelQuality.foul(0, 0.15, FuelQuality.ENGINE_BALANCE, 1, 3600), 0.005);
    }

    @Test
    void residueIsTooHeavyAndDerates2PerCent() {
        var r = rate(LO, HI, "diesel", 9.0, "residue", 1.0);
        assertEquals(0.1, r.heavy(), 2e-3);
        assertEquals(0, r.light(), 1e-12);
        assertEquals(0.8, r.power(), 2e-3);
        assertEquals(0.9 * 0.01 + 0.1, r.coking(), 2e-3);
        assertEquals(0, rate(LO, HI, "diesel", 4.0, "residue", 6.0).power(), 1e-12);   // (half outside: nothing)
        // dead crude's liquid cuts: 15.8 % residue -> 68 % power, coking 0.2159
        var crude = rate(LO, HI, "heavy_oil", 17.5, "diesel", 12.5, "gasoline", 10.0, "residue", 7.5);
        assertEquals(0.15 / 0.95, crude.heavy(), 2e-3);
        assertEquals(1 - 2 * 0.15 / 0.95, crude.power(), 2e-3);
        assertEquals(0.2159, crude.coking(), 1e-3);
    }

    @Test
    void aNarrowWindowCountsLightAndHeavy() {
        // the note's first gas turbine window, 330-560 K: heavy oil (700 K) outside it; a 450-600 K one: gasoline too light
        assertEquals(1, rate(330, 560, "heavy_oil", 10.0).heavy(), 1e-12);
        var r = rate(450, 600, "diesel", 8.0, "gasoline", 1.0, "heavy_oil", 1.0);
        assertEquals(0.1, r.light(), 2e-3);
        assertEquals(0.1, r.heavy(), 2e-3);
        assertEquals(0.6, r.power(), 2e-3);
    }

    @Test
    void theEnginesBalance() {
        // diesel with 7 % heavy oil just clears an engine, with 8 % fouls it; 1 % residue is the edge
        assertTrue(FuelQuality.foulRate(rate(LO, HI, "diesel", 93.0, "heavy_oil", 7.0).coking(), FuelQuality.ENGINE_BALANCE) < 0);
        assertTrue(FuelQuality.foulRate(rate(LO, HI, "diesel", 92.0, "heavy_oil", 8.0).coking(), FuelQuality.ENGINE_BALANCE) > 0);
        // a pot still's one-pass diesel (78-88 %, the rest heavy oil) fouls; a tray column's (97 %) doesn't
        assertTrue(rate(LO, HI, "diesel", 88.0, "heavy_oil", 12.0).coking() > FuelQuality.ENGINE_BALANCE);
        assertTrue(rate(LO, HI, "diesel", 97.0, "heavy_oil", 3.0).coking() < FuelQuality.ENGINE_BALANCE);
    }

    @Test
    void foulingIsBoundedAndTheFurnaceLawIsUnchanged() {
        assertEquals(FuelQuality.FOUL_MAX, FuelQuality.foul(0.89, 1, FuelQuality.ENGINE_BALANCE, 1, 1e6), 1e-12);
        assertEquals(0, FuelQuality.foul(0.01, 0, FuelQuality.ENGINE_BALANCE, 1, 1e6), 1e-12);
        // dead crude on a furnace burner: half coked after an hour at full power
        assertEquals(0.5, FuelQuality.foul(0, 0.2159, FuelQuality.FURNACE_BALANCE, 1, 3600), 1e-9);
        assertEquals(0.2, FuelQuality.foul(0.2, 0.15, FuelQuality.ENGINE_BALANCE, 0, 3600), 1e-12);   // (not running: no change)
    }

    @Test
    void viscosityOfAMix() {
        FluidNode n = TestSpecies.node(0.1, 0, 1, 293.15, 1e5, TestSpecies.id("diesel"), 10.0);
        assertEquals(0.0035, FuelQuality.layerViscosity(n, TestSpecies.id("diesel")), 5e-5);
        FluidNode m = TestSpecies.node(0.1, 0, 1, 293.15, 1e5, TestSpecies.id("diesel"), 5.0, TestSpecies.id("heavy_oil"), 5.0);
        assertEquals(Math.sqrt(0.0035 * 0.2), FuelQuality.layerViscosity(m, TestSpecies.id("diesel")), 1e-3);
    }
}
