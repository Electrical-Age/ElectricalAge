package mods.eln.transparentnode.groundradar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RadarViewTest {
    /** Stone under y = 64 with a big cave (radius 12) at y 30 under the radar at (0, 64, 0). */
    static GroundScan scan() {
        GroundScan s = new GroundScan(new GroundScan.World() {
            @Override
            public GroundScan.Material material(int x, int y, int z) {
                if (y >= 64) return GroundScan.Material.AIR_;
                int dy = y - 30;
                if (x * x + dy * dy + z * z <= 144) return GroundScan.Material.AIR_;
                return GroundScanTest.STONE;
            }

            @Override
            public boolean built(int x, int y, int z) {
                return false;
            }
        }, 0, 64, 0, -64);
        while (!s.step(1 << 20)) ;
        return s;
    }

    static int brightness(int argb) {
        return ((argb >> 16) & 255) + ((argb >> 8) & 255) + (argb & 255);
    }

    @Test
    void theBandPicksWhatShows() {
        assertEquals(1, RadarView.band(2.7, 2, 3.5), 1e-9);
        assertEquals(0, RadarView.band(0, 2, 3.5), 1e-9);
        assertTrue(RadarView.band(2.0, 2, 3.5) > 0.3 && RadarView.band(2.0, 2, 3.5) < 0.7, "an edge is a haze");
    }

    @Test
    void aCaveCastShowsWhenTheBandHoldsAir() {
        RadarView v = new RadarView(scan());
        // (looking straight down from above the radar at the cave)
        int air = v.trace(0.5, 80, 0.5, 0, -1, 0, -1, 0.4), rock = v.trace(0.5, 80, 0.5, 0, -1, 0, 2.2, 3.2);
        int miss = v.trace(0.5, 80, 0.5, 0, 1, 0, -1, 0.4);
        assertTrue(brightness(air) > 60, "the cave's cast " + Integer.toHexString(air));
        assertTrue(brightness(rock) > 60, "the rock " + Integer.toHexString(rock));
        assertEquals(0xFF000000, miss, "a ray up into the sky sees nothing");
        // (only rock in the band: a ray down a side away from the cave still shows rock; only air: dark there)
        int sideAir = v.trace(40.5, 80, 40.5, 0, -1, 0, -1, 0.4);
        assertTrue(brightness(sideAir) < 40, "no cave there " + Integer.toHexString(sideAir));
    }

    @Test
    void theSameViewRendersTheSame() {
        RadarView v = new RadarView(scan());
        int[] a = new int[48 * 27], b = new int[48 * 27];
        v.render(a, 48, 27, 0, 90, -60, 0, 30, 70, 2.2, 3.2, 0, 1);
        v.render(b, 48, 27, 0, 90, -60, 0, 30, 70, 2.2, 3.2, 0, 1);
        assertArrayEquals(a, b);
        long lit = java.util.Arrays.stream(a).filter(c -> brightness(c) > 30).count();
        assertTrue(lit > a.length / 4, "the volume fills part of the view: " + lit);
    }

    @Test
    void aFrameIsCheapEnough() {
        RadarView v = new RadarView(scan());
        int w = 192, h = 108;
        int[] out = new int[w * h];
        v.render(out, w, h, 0, 90, -80, 0, 35, 70, 2.2, 3.2, 0, 1);   // (warm-up)
        long t0 = System.nanoTime();
        for (int i = 0; i < 5; i++) v.render(out, w, h, 0, 90, -80, 0, 35, 70, 2.2, 3.2, 0, 1);
        double ms = (System.nanoTime() - t0) / 5e6;
        System.out.printf("radar view: %.1f ms a %dx%d frame%n", ms, w, h);
        assertTrue(ms < 200, ms + " ms");
    }

    @Test
    void aLoneOreShowsInADenseBandThoughItsCellsMeanHidesIt() {
        GroundScan.Material copper = new GroundScan.Material(4.2, 0.006, GroundScan.Material.SOLID);
        GroundScan s = new GroundScan(new GroundScan.World() {
            @Override
            public GroundScan.Material material(int x, int y, int z) {
                if (y >= 64) return GroundScan.Material.AIR_;
                if (x == 0 && z == 0 && y == 60) return copper;   // (one block, in a 2-block cell of stone)
                return GroundScanTest.STONE;
            }

            @Override
            public boolean built(int x, int y, int z) {
                return false;
            }
        }, 0, 64, 0, -64);
        while (!s.step(1 << 20)) ;
        int k = s.layerAt(64 - 1 - 60), mid = s.n / 2;
        assertTrue(s.densityAt(k, mid, mid) < 3.2, "the mean hides it: " + s.densityAt(k, mid, mid));
        assertTrue(s.peakAt(k, mid, mid) > 4.0, "the peak has it: " + s.peakAt(k, mid, mid));
        RadarView v = new RadarView(s);
        int over = v.trace(0.5, 80, 0.5, 0, -1, 0, 4.0, 8.5), beside = v.trace(10.5, 80, 10.5, 0, -1, 0, 4.0, 8.5);
        assertTrue(brightness(over) > 40, "the ore shows " + Integer.toHexString(over));
        assertTrue(brightness(beside) < 20, "nothing beside it " + Integer.toHexString(beside));
    }

    @Test
    void aOneLayerPictureAndACameraOutsideDrawWithoutFault() {
        GroundScan s = new GroundScan(new GroundScan.World() {
            @Override
            public GroundScan.Material material(int x, int y, int z) {
                return GroundScanTest.STONE;
            }

            @Override
            public boolean built(int x, int y, int z) {
                return false;
            }
        }, 0, 64, 0, 62);   // (the radar two blocks over the floor: one layer)
        while (!s.step(1 << 20)) ;
        assertEquals(1, s.layers.length);
        RadarView v = new RadarView(s);
        int[] out = new int[32 * 18];
        v.render(out, 32, 18, 500, 300, 500, 45, -30, 70, 2.2, 3.2, 0, 1);   // (far off, looking away)
        assertTrue(java.util.Arrays.stream(out).allMatch(c -> c == 0xFF000000), "nothing drawn looking away");
        v.render(out, 32, 18, 0.5, 70, 0.5, 0, 89, 70, 2.2, 3.2, 0, 1);   // (straight down at it)
        assertTrue(java.util.Arrays.stream(out).anyMatch(c -> brightness(c) > 30), "the layer shows");
    }
}
