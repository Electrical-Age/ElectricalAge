package mods.eln.sixnode.electricaldatalogger;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogStoreTest {
    static final float EPS = 1e-3f;

    static LogStore ticks(LogStore s, int n, java.util.function.LongFunction<Float> f) {
        for (int i = 0; i < n; i++) {
            float[] v = new float[s.channels()];
            java.util.Arrays.fill(v, f.apply(s.ticks()));
            s.tick(v);
        }
        return s;
    }

    @Test
    void aSteadyInputReadsTheSameAtAnyPeriod() {
        LogStore s = ticks(new LogStore(1), 20000, t -> 0.6f);
        for (double p : new double[]{0.05, 0.5, 5, 60}) {
            s.setPeriod(p);
            float[][][] v = s.view(256);
            assertTrue(v.length > 0);
            for (float[][] point : v) assertEquals(0.6f, point[0][0], EPS, "period " + p);
        }
    }

    @Test
    void theRingKeepsTheNewestTicks() {
        LogStore s = ticks(new LogStore(1), 3000, t -> (t % 7) / 7f);
        // tier 0 holds ticks 1976..2999: one tick each, exact to a half
        for (long t = 2990; t < 3000; t++) assertEquals((t % 7) / 7f, s.aggregate(0, t, t + 1)[0], EPS);
        float sum = 0;
        for (long t = 2000; t < 2100; t++) sum += (t % 7) / 7f;
        assertEquals(sum / 100, s.aggregate(0, 2000, 2100)[0], EPS);
        assertNull(s.aggregate(0, 3000, 3010));
    }

    @Test
    void decimatedBucketsKeepTheMeanAndTheExtremes() {
        LogStore s = ticks(new LogStore(1), 4096, t -> (t % 4) == 0 ? 1f : 0f);
        // ticks 0..1023 are only in tier 1 and up now: each bucket of 4 has one 1 and three 0
        float[] a = s.aggregate(0, 0, 64);
        assertEquals(0.25f, a[0], EPS);
        assertEquals(0f, a[1], EPS);
        assertEquals(1f, a[2], EPS);
    }

    @Test
    void anOldSpikeSurvivesInOnePoint() {
        LogStore s = ticks(new LogStore(1), 300000, t -> t == 1000 ? 1f : 0f);
        s.setPeriod(60); // 1200 ticks a point: 256 points cover 307200 ticks
        float[][][] v = s.view(256);
        int spikes = 0;
        for (float[][] p : v) if (p[0] != null && p[0][2] > 0.9f) spikes++;
        assertEquals(1, spikes);
    }

    @Test
    void aPeriodChangeKeepsTheFineHistory() {
        LogStore s = ticks(new LogStore(1), 1000, t -> (float) Math.sin(t / 9.0) * 0.5f + 0.5f);
        s.setPeriod(0.5);
        float[][][] before = s.view(80);
        s.setPeriod(5);
        float[][][] coarse = s.view(80);
        assertEquals(10, coarse.length); // 1000 ticks at 100 ticks a point
        ticks(s, 50, t -> 0.2f);
        s.setPeriod(0.5);
        float[][][] after = s.view(85);
        // 50 ticks later, the old points sit 5 further back, unchanged
        for (int j = 0; j < 80; j++) assertArrayEquals(before[j][0], after[j + 5][0], 1e-6f, "point " + j);
        for (int j = 0; j < 5; j++) assertEquals(0.2f, after[j][0][0], EPS);
    }

    @Test
    void emittedPointsMatchTheView() {
        LogStore s = new LogStore(2);
        s.setPeriod(0.5);
        float[][] last = null;
        for (int i = 0; i < 95; i++)
            if (s.tick(new float[]{i / 100f, 1 - i / 100f})) last = s.emit();
        assertNotNull(last);
        float[][][] v = s.view(256);
        assertEquals(9, v.length);
        assertArrayEquals(last[0], v[0][0], 1e-6f);
        assertArrayEquals(last[1], v[0][1], 1e-6f);
        assertEquals((80 + 89) / 2f / 100f, v[0][0][0], EPS);
    }

    @Test
    void recordingAveragesWithinATick() {
        LogStore s = new LogStore(1);
        for (int i = 0; i < 4; i++) s.record(new float[]{i < 2 ? 0f : 1f}, 0.0125);
        assertEquals(1, s.ticks());
        assertEquals(0.5f, s.aggregate(0, 0, 1)[0], EPS);
        s.record(new float[]{0.3f}, 1.0); // a long step fills its ticks
        assertEquals(21, s.ticks());
    }

    @Test
    void nbtRoundTripIsExact() {
        LogStore s = ticks(new LogStore(2), 70000, t -> (t * 37 % 101) / 101f);
        s.setPeriod(2);
        ticks(s, 333, t -> 0.5f);
        CompoundTag a = new CompoundTag();
        s.writeToNBT(a);
        LogStore r = new LogStore(2);
        r.readFromNBT(a);
        CompoundTag b = new CompoundTag();
        r.writeToNBT(b);
        assertEquals(a, b);
        float[][][] v1 = s.view(256), v2 = r.view(256);
        assertEquals(v1.length, v2.length);
        for (int j = 0; j < v1.length; j++)
            for (int c = 0; c < 2; c++) assertArrayEquals(v1[j][c], v2[j][c], 0f);
    }

    @Test
    void anOldLogReplaysAsHistory() {
        float[][] old = new float[256][];
        for (int i = 0; i < 256; i++) old[i] = new float[]{(i * 3 % 255) / 255f};
        LogStore s = new LogStore(1);
        s.replay(old, 10);
        s.setPeriod(0.5);
        float[][][] v = s.view(256);
        assertEquals(256, v.length);
        // the newest 102 samples are still tick by tick; older ones come from 4- and 16-tick buckets, which straddle
        // a sample's edges and blend in some of a neighbour (less than the step to it)
        for (int j = 0; j < 102; j++) assertEquals(old[255 - j][0], v[j][0][0], EPS, "point " + j);
        for (int j = 102; j < 256; j++) {
            int i = 255 - j;
            float a = old[i][0], step = Math.max(Math.abs(a - old[Math.max(0, i - 1)][0]), Math.abs(a - old[Math.min(255, i + 1)][0]));
            assertEquals(a, v[j][0][0], step + EPS, "point " + j);
        }
    }

    @Test
    void aHugeOldLogKeepsWhatFits() {
        float[][] old = new float[256][];
        for (int i = 0; i < 256; i++) old[i] = new float[]{i / 255f};
        LogStore s = new LogStore(1);
        s.replay(old, 72000); // an hour a sample
        assertEquals(LogStore.SPAN, s.ticks());
        s.setPeriod(3600);
        float[][][] v = s.view(256);
        assertEquals(1f, v[0][0][0], EPS);
    }
}
