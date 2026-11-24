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
        // gasoline and diesel clear an engine, heavy oil fouls it: ~0.10 an hour at full power (4x slower than a burner
        // at the same index, Baughn 2026-11-15)
        assertTrue(FuelQuality.foulRate(0, FuelQuality.ENGINE_BALANCE) < 0);
        assertTrue(FuelQuality.foulRate(0.01, FuelQuality.ENGINE_BALANCE) < 0);
        assertEquals(0.098, FuelQuality.foulEngine(0, 0.15, 1, 3600), 0.002);
        assertEquals(FuelQuality.foulEngine(0, 0.15, 1, 3600), FuelQuality.engineFoulPerHour(0.15), 1e-12);
        assertEquals(FuelQuality.foul(0, 0.15, FuelQuality.ENGINE_BALANCE, 1, 900), FuelQuality.foulEngine(0, 0.15, 1, 3600), 1e-12);
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

    /**
     * A piston engine's choke (Baughn 2026-11-24: no heavy oil at all; PISTON_CHOKE), on the Fuel Generator's window, by
     * the Still Pot's products (StillPotCases, StillSketchTest): a sensible still's cuts don't choke one, a badly run
     * one's (overfired at 25 kW) do; a 15 kW batch's product only fouls one.
     */
    @Test
    void thePistonChoke() {
        double lo = FuelQuality.GENERATOR_LO, hi = FuelQuality.GENERATOR_HI, c = FuelQuality.PISTON_CHOKE;
        assertTrue(c > FuelQuality.ENGINE_BALANCE && c < FuelQuality.TURBINE_CHOKE);
        // a sensible still: its gasoline cut (98 %), its diesel cut (90 % diesel, 9 % heavy oil, 1 % gasoline)
        assertTrue(!rate(lo, hi, "gasoline", 98.0, "diesel", 2.0).choked(c));
        assertTrue(!rate(lo, hi, "diesel", 90.0, "heavy_oil", 9.0, "gasoline", 1.0).choked(c));
        // a 15 kW batch's product (still_pot_overfired_batch): fouls, doesn't choke
        var mild = rate(lo, hi, "gasoline", 60.0, "diesel", 30.0, "heavy_oil", 8.5, "residue", 1.5);
        assertTrue(!mild.choked(c) && mild.coking() > FuelQuality.ENGINE_BALANCE, "" + mild);
        // overfired at 25 kW (still_pot_overfire, StillSketchTest), straight heavy oil and a heavy cut: choke
        assertTrue(rate(lo, hi, "diesel", 33.0, "heavy_oil", 48.0, "residue", 19.0).choked(c));
        assertTrue(rate(lo, hi, "gasoline", 60.0, "diesel", 20.0, "heavy_oil", 14.0, "residue", 6.0).choked(c));
        assertTrue(rate(lo, hi, "heavy_oil", 10.0).choked(c));
        assertTrue(rate(lo, hi, "diesel", 44.0, "heavy_oil", 56.0).choked(c));
        // and the turbine keeps heavy oil
        assertTrue(!rate(FuelQuality.TURBINE_LO, FuelQuality.TURBINE_HI, "heavy_oil", 10.0).choked(FuelQuality.TURBINE_CHOKE));
    }

    @Test
    void viscosityOfAMix() {
        FluidNode n = TestSpecies.node(0.1, 0, 1, 293.15, 1e5, TestSpecies.id("diesel"), 10.0);
        assertEquals(0.0035, FuelQuality.layerViscosity(n, TestSpecies.id("diesel")), 5e-5);
        FluidNode m = TestSpecies.node(0.1, 0, 1, 293.15, 1e5, TestSpecies.id("diesel"), 5.0, TestSpecies.id("heavy_oil"), 5.0);
        assertEquals(Math.sqrt(0.0035 * 0.2), FuelQuality.layerViscosity(m, TestSpecies.id("diesel")), 1e-3);
    }

    /** The viscosity players read (design-398 6 A): mPa s under 1 Pa s, Neon's bands by the ratio to the game's water. */
    @Test
    void viscosityForPlayers() {
        assertEquals("0.50 mPa s", EaUnits.viscosity(5e-4));
        assertEquals("200 mPa s", EaUnits.viscosity(0.2));
        assertEquals("50 Pa s", EaUnits.viscosity(50));
        assertEquals("thin, 0.50 mPa s, 2.0x water", EaUnits.viscosityText(5e-4));
        assertEquals("syrupy, 10 mPa s, 40x water", EaUnits.viscosityText(0.01));
        assertEquals("thick, 200 mPa s, 800x water", EaUnits.viscosityText(0.2));
        assertEquals("tarry, 50 Pa s, 200,000x water", EaUnits.viscosityText(50));
        // the band goes by the ratio as shown: 199.5x reads 200x, so thick; 14.96x reads 15x, so syrupy
        assertTrue(EaUnits.viscosityText(199.5 * Water.MU_L).startsWith("thick, "), EaUnits.viscosityText(199.5 * Water.MU_L));
        assertTrue(EaUnits.viscosityText(14.96 * Water.MU_L).startsWith("syrupy, "), EaUnits.viscosityText(14.96 * Water.MU_L));
        // a node: its liquid but water; none for water only (water's is held constant: it reads as the reference)
        FluidNode n = TestSpecies.node(0.1, 0, 1, 293.15, 1e5, TestSpecies.id("diesel"), 10.0);
        String name = SpeciesRegistry.get().byId(TestSpecies.id("diesel")).displayName();
        assertTrue(EaUnits.viscosity(n).startsWith("thin " + name + ", 3.5 mPa s, 14x water"), EaUnits.viscosity(n));
        FluidNode w = TestSpecies.node(0.1, 0, 1, 293.15, 1e5, SpeciesRegistry.WATER, 10.0);
        assertEquals(null, EaUnits.viscosity(w));
        // oil over water: the oil only
        FluidNode ow = TestSpecies.node(0.1, 0, 1, 293.15, 1e5, SpeciesRegistry.WATER, 10.0, TestSpecies.id("diesel"), 10.0);
        assertEquals(EaUnits.viscosity(n), EaUnits.viscosity(ow));
        // thick: third in the Jade lines, so it shows without sneaking; else last
        FluidNode cold = TestSpecies.node(0.1, 0, 1, 293.15, 1e5, TestSpecies.id("heavy_oil"), 10.0);
        assertTrue(EaUnits.viscosity(cold).startsWith("thick ") && EaUnits.viscosity(cold).endsWith("; heat or dilute"), EaUnits.viscosity(cold));
        var info = new java.util.LinkedHashMap<String, String>();
        info.put("Pressure", "p");
        info.put("Contents", "c");
        info.put("Liquid", "l");
        EaUnits.putViscosity(info, cold);
        assertEquals(java.util.List.of("Pressure", "Contents", "Viscosity", "Liquid"), new java.util.ArrayList<>(info.keySet()));
        info.remove("Viscosity");
        EaUnits.putViscosity(info, n);
        assertEquals(java.util.List.of("Pressure", "Contents", "Liquid", "Viscosity"), new java.util.ArrayList<>(info.keySet()));
    }

    /**
     * The choke (Baughn 2026-11-23): today's fuels and a sensible still's never choke an engine; crude and residue-laden
     * fuel do (the gas turbine's limit, TURBINE_CHOKE).
     */
    @Test
    void theChoke() {
        assertTrue(FuelQuality.TURBINE_CHOKE >= 3 * FuelQuality.ENGINE_BALANCE);
        for (String f : new String[]{"gasoline", "diesel", "heavy_oil"}) assertTrue(!rate(LO, HI, f, 10.0).choked(FuelQuality.TURBINE_CHOKE), f);
        // a pot still's one-pass diesel, with heavy oil and some residue carried over: fouls, doesn't choke
        assertTrue(!rate(LO, HI, "diesel", 80.0, "heavy_oil", 15.0, "residue", 5.0).choked(FuelQuality.TURBINE_CHOKE));
        assertTrue(!rate(LO, HI, "diesel", 9.0, "residue", 1.0).choked(FuelQuality.TURBINE_CHOKE));
        // dead crude, and diesel with a fifth residue: choke
        assertTrue(rate(LO, HI, "heavy_oil", 17.5, "diesel", 12.5, "gasoline", 10.0, "residue", 7.5).choked(FuelQuality.TURBINE_CHOKE));
        assertTrue(rate(LO, HI, "diesel", 8.0, "residue", 2.0).choked(FuelQuality.TURBINE_CHOKE));
        assertTrue(FuelQuality.describe(rate(LO, HI, "diesel", 8.0, "residue", 2.0), FuelQuality.TURBINE_CHOKE).startsWith("chokes engine"));
    }
}
