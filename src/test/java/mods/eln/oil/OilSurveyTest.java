package mods.eln.oil;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The oil survey's pick of a lake (todo #470): from the placement's arithmetic alone, in the ring, spread, repeatable. */
class OilSurveyTest {
    static final int SPACING = 75, SEPARATION = 30, SALT = 31415926;   // data/eln/worldgen/structure_set/oil_lakes.json
    static final long SEED = 8675309L;

    /** The real placement class: what worldgen asks for each chunk. */
    static OilSurvey.Cells real(long seed) {
        RandomSpreadStructurePlacement pl = new RandomSpreadStructurePlacement(SPACING, SEPARATION, RandomSpreadType.LINEAR, SALT);
        return (gx, gz) -> {
            ChunkPos c = pl.getPotentialStructureChunk(seed, gx * SPACING, gz * SPACING);
            return OilSurvey.pack(c.x(), c.z());
        };
    }

    static double dist(long p, int ox, int oz) {
        return Math.hypot(OilSurvey.middle(OilSurvey.cx(p)) - ox, OilSurvey.middle(OilSurvey.cz(p)) - oz);
    }

    @Test
    void candidatesAreExactlyTheLakesInTheRing() {
        OilSurvey.Cells cells = real(SEED);
        int ox = 137, oz = -2210, minR = 600, maxR = 3000;
        List<Long> got = OilSurvey.candidates(SPACING, cells, ox, oz, minR, maxR);
        // brute force over a much wider block of cells
        int n = 0;
        for (int gx = -12; gx <= 12; gx++)
            for (int gz = -12; gz <= 12; gz++) {
                long p = cells.start(gx, gz);
                double d = dist(p, ox, oz);
                if (d >= minR && d <= maxR) {
                    n++;
                    assertTrue(got.contains(p), "missed a lake at distance " + d);
                }
            }
        assertEquals(n, got.size());
        // one lake per 1200 x 1200 blocks: the ring holds about pi (3000^2 - 600^2) / 1200^2 = 18.8
        assertTrue(n >= 12 && n <= 26, "lakes in the ring: " + n);
        for (long p : got) {
            // each is a chunk the placement itself calls a start chunk
            assertEquals(p, cells.start(Math.floorDiv(OilSurvey.cx(p), SPACING), Math.floorDiv(OilSurvey.cz(p), SPACING)));
        }
    }

    @Test
    void pickIsSpreadOverTheRingAndNotTheNearest() {
        OilSurvey.Cells cells = real(SEED);
        int ox = 0, oz = 0, minR = 600, maxR = 3000;
        List<Long> all = OilSurvey.candidates(SPACING, cells, ox, oz, minR, maxR);
        long nearest = all.stream().min((a, b) -> Double.compare(dist(a, ox, oz), dist(b, ox, oz))).orElseThrow();
        Random rnd = new Random(1);
        Map<Long, Integer> hits = new HashMap<>();
        int draws = 200 * all.size();
        for (int i = 0; i < draws; i++) {
            long p = OilSurvey.pick(SPACING, cells, ox, oz, minR, maxR, q -> true, rnd::nextInt);
            double d = dist(p, ox, oz);
            assertTrue(d >= minR && d <= maxR, "outside the ring: " + d);
            hits.merge(p, 1, Integer::sum);
        }
        assertEquals(all.size(), hits.size(), "every lake in the ring comes up");
        for (int h : hits.values()) assertTrue(h > 120 && h < 300, "uniform within noise (expected 200): " + h);
        assertTrue(hits.get(nearest) < draws / 4, "the nearest lake is not favoured");
    }

    @Test
    void pickRepeatsForTheSameRandomAndDiffersBetweenSeeds() {
        OilSurvey.Cells cells = real(SEED);
        long a = OilSurvey.pick(SPACING, cells, 50, 50, 600, 3000, q -> true, new Random(7)::nextInt);
        long b = OilSurvey.pick(SPACING, cells, 50, 50, 600, 3000, q -> true, new Random(7)::nextInt);
        assertEquals(a, b);
        // another world seed has its lakes elsewhere
        assertNotEquals(OilSurvey.candidates(SPACING, cells, 0, 0, 600, 3000), OilSurvey.candidates(SPACING, real(SEED + 1), 0, 0, 600, 3000));
    }

    @Test
    void spentLakesAreSkippedAndAnEmptyRingWidens() {
        OilSurvey.Cells cells = real(SEED);
        List<Long> all = OilSurvey.candidates(SPACING, cells, 0, 0, 600, 3000);
        long only = all.get(3);
        for (int i = 0; i < 50; i++)
            assertEquals(only, OilSurvey.pick(SPACING, cells, 0, 0, 600, 3000, q -> q == only, new Random(i)::nextInt));
        // a ring that ends short of the nearest lake: widened (outer edge x2, then x4) until it holds one
        double nearest = OilSurvey.candidates(SPACING, cells, 0, 0, 0, 6000).stream().mapToDouble(q -> dist(q, 0, 0)).min().orElseThrow();
        int maxR = (int) (nearest / 3);
        assertTrue(OilSurvey.candidates(SPACING, cells, 0, 0, 1, 2 * maxR).isEmpty());
        long p = OilSurvey.pick(SPACING, cells, 0, 0, 1, maxR, q -> true, new Random(3)::nextInt);
        assertNotEquals(Long.MIN_VALUE, p);
        assertTrue(dist(p, 0, 0) <= 4 * maxR);
        // nothing passes: none
        assertEquals(Long.MIN_VALUE, OilSurvey.pick(SPACING, cells, 0, 0, 600, 3000, q -> false, new Random(3)::nextInt));
        assertFalse(all.isEmpty());
    }

    @Test
    void recordKeyMatchesTheStructures() {
        // OilLakeStructure: ((long) chunk.x << 32) ^ (chunk.z & 0xffffffffL), for negative chunks too
        int x = -37, z = -1204;
        assertEquals("oil:" + (((long) x << 32) ^ (z & 0xffffffffL)), OilSurvey.key(OilSurvey.pack(x, z)));
        assertEquals(x, OilSurvey.cx(OilSurvey.pack(x, z)));
        assertEquals(z, OilSurvey.cz(OilSurvey.pack(x, z)));
    }
}
