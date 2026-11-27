package mods.eln.transparentnode.groundradar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The goggles' bands (Baughn 1575645294446518445): the cycle, the edge keys, what each shows, and reading one back. */
class RadarBandTest {
    @Test
    void theKeyCyclesTheNamedBandsFromEverything() {
        RadarBand b = RadarBand.DEFAULT;
        assertEquals(RadarBand.EVERYTHING, b.kind());
        for (int k = 1; k <= RadarBand.ORES; k++) assertEquals(k, (b = b.next()).kind());
        assertEquals(RadarBand.EVERYTHING, b.next().kind(), "after Ores comes Everything again");
        assertEquals(RadarBand.EVERYTHING, new RadarBand(RadarBand.CUSTOM, 1, 2).next().kind());
    }

    @Test
    void theEdgeKeysMakeACustomBandFromANamedOne() {
        RadarBand b = RadarBand.of(RadarBand.SOIL).edges(0, 1);
        assertEquals(RadarBand.CUSTOM, b.kind());
        assertEquals(1.3, b.lo(), 1e-9);
        assertEquals(2.3, b.hi(), 1e-9);
        RadarBand t = b;
        for (int i = 0; i < 100; i++) t = t.edges(1, 0);
        assertEquals(t.hi() - RadarBand.STEP, t.lo(), 1e-9, "the low edge stops a step under the high one");
        for (int i = 0; i < 200; i++) t = t.edges(-1, 1);
        assertEquals(RadarBand.MIN, t.lo(), 1e-9);
        assertEquals(RadarBand.MAX, t.hi(), 1e-9);
    }

    @Test
    void oilShowsOnlyOilAndWaterOnlyWater() {
        RadarBand oil = RadarBand.of(RadarBand.OIL), water = RadarBand.of(RadarBand.WATER);
        assertEquals(1, oil.weight(GroundScan.OIL, 0.85, 0.85, 0), 1e-9);
        assertEquals(0, oil.weight(GroundScan.LIQUID, 1.0, 1.0, 0), 1e-9, "water isn't oil, though it is nearly as dense");
        assertEquals(0, oil.weight(0, 0.9, 0.9, 0), 1e-9, "nor is anything else at oil's density");
        assertEquals(1, water.weight(GroundScan.LIQUID, 1.0, 1.0, 0), 1e-9);
        assertEquals(0, water.weight(GroundScan.OIL, 0.85, 0.85, 0), 1e-9);
    }

    @Test
    void everythingShowsTheFeaturesOverFaintGround() {
        RadarBand e = RadarBand.DEFAULT;
        assertEquals(1, e.weight(0, 0, 0, 0), 1e-9, "a cave");
        assertEquals(1, e.weight(GroundScan.OIL, 0.85, 0.85, 0), 1e-9);
        assertEquals(1, e.weight(GroundScan.BUILT, 2.0, 2.7, 0), 1e-9, "a structure");
        assertEquals(RadarBand.FAINT, e.weight(0, 2.7, 2.7, 0), 1e-9, "plain rock is faint");
        assertEquals(0.6, e.weight(0, 2.8, 5.0, 0.2), 1e-9, "an iron vein shows by its share of the cell, ORE_GAIN times");
        assertEquals(1, e.weight(0, 2.8, 5.0, 0.8), 1e-9, "at most fully");
        assertTrue(e.byPeak(0, 2.8, 5.0, 0.8));
        assertFalse(e.byPeak(0, 2.7, 2.7, 0));
        assertEquals(RadarBand.ORE_RGB, e.colour(0x123456, true), "a vein in its own colour");
        assertEquals(0x123456, e.colour(0x123456, false));
        assertEquals(0x123456, RadarBand.of(RadarBand.ORES).colour(0x123456, true), "in the Ores band, the density ramp's");
    }

    @Test
    void aCustomBandIsTheDensityBandPass() {
        RadarBand b = new RadarBand(RadarBand.CUSTOM, 2, 3.5);
        assertEquals(RadarView.band(2.7, 2, 3.5), b.weight(0, 2.7, 2.7, 0), 1e-9);
        assertEquals(1, b.weight(0, 0.5, 2.7, 1), 1e-9, "its densest sample counts too");
        assertTrue(b.byPeak(0, 0.5, 2.7, 1));
    }

    @Test
    void readingBackKeepsItInRange() {
        assertEquals(RadarBand.of(RadarBand.OIL), RadarBand.read(RadarBand.OIL, 99, -99), "a named band by its kind alone");
        assertEquals(RadarBand.DEFAULT, RadarBand.read(42, 0, 0));
        assertEquals(RadarBand.DEFAULT, RadarBand.read(-3, 0, 0));
        assertEquals(new RadarBand(RadarBand.CUSTOM, 4.6, 6.0), RadarBand.read(RadarBand.CUSTOM, 4.6f, 6.0f), "float edges come back as tenths");
        assertEquals(RadarBand.DEFAULT, RadarBand.read(RadarBand.CUSTOM, 3, 2), "edges out of order");
        assertEquals(RadarBand.DEFAULT, RadarBand.read(RadarBand.CUSTOM, Float.NaN, 2));
        assertEquals(RadarBand.DEFAULT, RadarBand.read(RadarBand.CUSTOM, -5, 2));
    }
}
