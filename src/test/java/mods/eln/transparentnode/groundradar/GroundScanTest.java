package mods.eln.transparentnode.groundradar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroundScanTest {
    static final GroundScan.Material STONE = new GroundScan.Material(2.7, 0.004, GroundScan.Material.SOLID);
    static final GroundScan.Material WATER = new GroundScan.Material(1.0, 0.03, GroundScan.Material.WATER);
    static final GroundScan.Material OIL = new GroundScan.Material(0.85, 0.002, GroundScan.Material.OIL);

    /** Stone everywhere under y = 64, with an optional cave (a sphere of air), sea on top or an oil pocket. */
    static GroundScan.World world(int caveX, int caveY, int caveZ, int caveR, int seaDepth, boolean oilPocket) {
        return new GroundScan.World() {
            @Override
            public GroundScan.Material material(int x, int y, int z) {
                if (y >= 64) return GroundScan.Material.AIR_;
                if (y >= 64 - seaDepth) return WATER;
                int dx = x - caveX, dy = y - caveY, dz = z - caveZ;
                if (dx * dx + dy * dy + dz * dz <= caveR * caveR) return GroundScan.Material.AIR_;
                if (oilPocket && y < 30 && y >= 20 && Math.abs(x) < 10 && Math.abs(z) < 10) return OIL;
                return STONE;
            }

            @Override
            public boolean built(int x, int y, int z) {
                return false;
            }
        };
    }

    static GroundScan run(GroundScan.World w) {
        GroundScan s = new GroundScan(w, 0, 64, 0, -64);
        int guard = 0;
        while (!s.step(500)) assertTrue(++guard < 100000);
        return s;
    }

    @Test
    void layersGrowWithDepthAndReachTheFloor() {
        GroundScan.Layer[] l = GroundScan.layers(128);
        assertEquals(2, l[0].cell());
        assertEquals(0, l[0].depth());
        for (int k = 1; k < l.length; k++) {
            assertEquals(l[k - 1].depth() + l[k - 1].cell(), l[k].depth());
            assertTrue(l[k].cell() >= l[k - 1].cell());
        }
        GroundScan.Layer last = l[l.length - 1];
        assertTrue(last.depth() + last.cell() >= 128);
        // (Baughn: a 50-wide feature still shows at bedrock: there, it spans a few cells)
        assertTrue(last.cell() <= 25, "bottom cell " + last.cell());
        assertTrue(l.length < 60, "layers " + l.length);
    }

    @Test
    void aCaveShowsAsAVoidUnderSolidRock() {
        GroundScan s = run(world(0, 30, 0, 6, 0, false));
        int k = s.layerAt(64 - 1 - 30);
        int mid = GroundScan.N / 2;
        double cave = s.densityAt(k, mid, mid), rock = s.densityAt(k, 2, 2);
        assertTrue(cave < 0.9, "cave " + cave);
        assertTrue(rock > 2.2, "rock " + rock);
    }

    @Test
    void theSameGroundScansTheSame() {
        GroundScan a = run(world(5, 20, -3, 5, 0, false)), b = run(world(5, 20, -3, 5, 0, false));
        assertArrayEquals(a.density, b.density);
        assertArrayEquals(a.flags, b.flags);
    }

    @Test
    void theSeaFadesTheSignal() {
        // (a cave at the same depth under the rock: plain to see on land, lost in static under 40 blocks of sea)
        GroundScan land = run(world(0, -40, 0, 20, 0, false)), sea = run(world(0, -40, 0, 20, 40, false));
        int k = land.layerAt(64 - 1 + 40), mid = GroundScan.N / 2;
        double contrastLand = land.densityAt(k, 1, 1) - land.densityAt(k, mid, mid);
        assertTrue(contrastLand > 1.5, "land contrast " + contrastLand);
        double sigLand = land.signalAt(k, mid, mid), sigSea = sea.signalAt(k, mid, mid);
        assertTrue(sigLand > 0.5, "signal on land " + sigLand);
        assertTrue(sigSea < sigLand * 0.6, "signal under the sea " + sigSea + " vs land " + sigLand);
        assertTrue((sea.flagsAt(0, mid, mid) & GroundScan.LIQUID) != 0, "the sea itself is liquid");
    }

    @Test
    void oilIsFlagged() {
        GroundScan s = run(world(0, -200, 0, 1, 0, true));
        int k = s.layerAt(64 - 1 - 25), mid = GroundScan.N / 2;
        assertTrue((s.flagsAt(k, mid, mid) & GroundScan.OIL) != 0);
        assertEquals(0, s.flagsAt(k, 0, 0) & GroundScan.OIL);
    }

    @Test
    void unloadedGroundIsUnknown() {
        GroundScan s = run(new GroundScan.World() {
            @Override
            public GroundScan.Material material(int x, int y, int z) {
                return x < 0 ? null : STONE;
            }

            @Override
            public boolean built(int x, int y, int z) {
                return false;
            }
        });
        assertTrue((s.flagsAt(0, 0, 5) & GroundScan.UNKNOWN) != 0);
        assertEquals(0, s.flagsAt(0, GroundScan.N - 1, 5) & GroundScan.UNKNOWN);
    }

    @Test
    void theBottomLayerReadsOnlyWhatIsAboveTheFloor() {
        // (as a level does: nothing under its floor; the last layer, cut off there, still reads as rock, not a void)
        GroundScan s = run(new GroundScan.World() {
            @Override
            public GroundScan.Material material(int x, int y, int z) {
                return y < -64 ? GroundScan.Material.AIR_ : y >= 64 ? GroundScan.Material.AIR_ : STONE;
            }

            @Override
            public boolean built(int x, int y, int z) {
                return false;
            }
        });
        int last = s.layers.length - 1, mid = GroundScan.N / 2;
        assertTrue(s.densityAt(last, mid, mid) > 2.5, "bottom " + s.densityAt(last, mid, mid));
    }

    @Test
    void encodeRoundTrips() {
        GroundScan a = run(world(0, 30, 0, 6, 10, true));
        GroundScan b = GroundScan.decode(a.encode());
        assertNotNull(b);
        assertTrue(b.done());
        assertEquals(a.layers.length, b.layers.length);
        assertArrayEquals(a.density, b.density);
        assertArrayEquals(a.flags, b.flags);
        assertNull(GroundScan.decode(new byte[5]));
    }

    @Test
    void aScanReadsAFewHundredThousandSamplesAtMost() {
        int[] reads = {0};
        GroundScan.World w = world(0, 0, 0, 0, 0, false);
        GroundScan s = new GroundScan(new GroundScan.World() {
            @Override
            public GroundScan.Material material(int x, int y, int z) {
                reads[0]++;
                return w.material(x, y, z);
            }

            @Override
            public boolean built(int x, int y, int z) {
                return false;
            }
        }, 0, 120, 0, -64);
        while (!s.step(1000)) ;
        assertTrue(reads[0] < 500_000, "reads " + reads[0]);
    }
}
