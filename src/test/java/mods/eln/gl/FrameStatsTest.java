package mods.eln.gl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrameStatsTest {
    @Test
    void percentilesAreNearestRank() {
        long[] v = new long[100];
        for (int i = 0; i < 100; i++) v[i] = 100 - i;   // 100..1, unsorted
        FrameStats.Summary s = FrameStats.summary(v, 100);
        assertEquals(100, s.n());
        assertEquals(50.5, s.mean(), 1e-9);
        assertEquals(50, s.p50());
        assertEquals(95, s.p95());
        assertEquals(99, s.p99());
        assertEquals(100, s.max());
        assertEquals(1, s.min());
        assertEquals(100, v[0], "the input is left as it was");
    }

    @Test
    void onlyTheFirstNValuesCount() {
        long[] v = {5, 1, 3, 1000, 1000};
        FrameStats.Summary s = FrameStats.summary(v, 3);
        assertEquals(3, s.p50());
        assertEquals(5, s.p99());
        assertEquals(5, s.max());
        assertEquals(3.0, s.mean(), 1e-9);
    }

    @Test
    void oneValueAndNone() {
        FrameStats.Summary one = FrameStats.summary(new long[]{7}, 1);
        assertEquals(7, one.p50());
        assertEquals(7, one.p95());
        assertEquals(7, one.p99());
        FrameStats.Summary none = FrameStats.summary(new long[0], 0);
        assertEquals(0, none.n());
        assertEquals(0, none.p99());
    }

    @Test
    void aSpikeShowsInP99NotInP50() {
        long[] v = new long[200];
        java.util.Arrays.fill(v, 10);
        v[17] = 500;
        v[99] = 400;
        v[150] = 300;
        FrameStats.Summary s = FrameStats.summary(v, 200);
        assertEquals(10, s.p50());
        assertEquals(10, s.p95());
        assertEquals(300, s.p99());
        assertEquals(500, s.max());
    }
}
