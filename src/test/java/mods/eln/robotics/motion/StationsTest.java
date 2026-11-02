package mods.eln.robotics.motion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Stations: from markers or spread evenly; the setpoint's bands and share; the panel's next stop; block-aligned stops. */
class StationsTest {
    @Test
    void markersMakeTheStations() {
        assertArrayEquals(new int[]{0, 3, 6}, Stations.of(new int[]{6, 3, 0, 3, 9, -1}, 6), "sorted, distinct, on the rail");
    }

    @Test
    void withoutMarkersUpToEightEvenStops() {
        assertArrayEquals(new int[]{0, 1, 2, 3}, Stations.of(new int[0], 3));
        int[] s = Stations.of(new int[0], 28);
        assertEquals(8, s.length);
        assertEquals(0, s[0]);
        assertEquals(28, s[7]);
        assertEquals(4, s[1]);
    }

    @Test
    void bandsPickStations() {
        int[] s = {0, 3, 6};
        assertEquals(0, Stations.band(0, s));
        assertEquals(0, Stations.band(0.12, s), "band 0: 0..6.25 V");
        assertEquals(3, Stations.band(0.13, s), "band 1");
        assertEquals(6, Stations.band(0.3, s), "past the last station: the top");
        assertEquals(6, Stations.band(1.0, s));
    }

    @Test
    void proportionalSpansTheTravelInWholeBlocks() {
        assertEquals(0, Stations.proportional(0, 10));
        assertEquals(5, Stations.proportional(0.5, 10));
        assertEquals(3, Stations.proportional(0.27, 10));
        assertEquals(10, Stations.proportional(1.2, 10));
    }

    @Test
    void panelGoesAStationUpOrDown() {
        int[] s = {0, 3, 6};
        assertEquals(3, Stations.above(0, s));
        assertEquals(6, Stations.above(3, s));
        assertEquals(-1, Stations.above(6, s));
        assertEquals(3, Stations.above(2.6, s), "between stations: the next one up");
        assertEquals(3, Stations.below(6, s));
        assertEquals(-1, Stations.below(0, s));
        assertEquals(0, Stations.below(2.4, s));
    }

    @Test
    void stopsAheadOnAWholeBlock() {
        double a = 1.0 / 400;   // 1 m/s^2 in m/tick^2
        assertEquals(6, Stations.stopAhead(3.2, 0.1, a, 10), "2 m to brake from 2 m/s: past 5.2, the next whole block");
        assertEquals(1, Stations.stopAhead(3.2, -0.1, a, 10), "going down: 1.2 -> 1");
        assertEquals(10, Stations.stopAhead(9.5, 0.1, a, 10), "never past the rail's end");
        assertEquals(4, Stations.stopAhead(4.0, 0, a, 10), "at rest on a block: there");
    }
}
