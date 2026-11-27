package mods.eln.robotics.motion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Stations: the setpoint's share of the travel; block-aligned stops. */
class StationsTest {
    @Test
    void proportionalSpansTheTravelInWholeBlocks() {
        assertEquals(0, Stations.proportional(0, 10));
        assertEquals(5, Stations.proportional(0.5, 10));
        assertEquals(3, Stations.proportional(0.27, 10));
        assertEquals(10, Stations.proportional(1.2, 10));
    }

    @Test
    void stopsAheadOnAWholeBlock() {
        double a = 1.0 / 400;   // 1 m/s^2 in m/tick^2
        assertEquals(6, Stations.stopAhead(3.2, 0.1, a, 10), "2 m to brake from 2 m/s: past 5.2, the next whole block");
        assertEquals(1, Stations.stopAhead(3.2, -0.1, a, 10), "going down: 1.2 -> 1");
        assertEquals(10, Stations.stopAhead(9.5, 0.1, a, 10), "never past the rail's end");
        assertEquals(4, Stations.stopAhead(4.0, 0, a, 10), "at rest on a block: there");
    }

    /** A laid track's stops (metres along it, not whole blocks): the nearest, the next one braking allows, being at one. */
    @Test
    void stopsOfALaidTrack() {
        double[] st = {0, 7.3, 16.07, 22.07};
        assertEquals(7.3, Stations.nearest(9, st), 1e-12);
        assertEquals(22.07, Stations.nearest(100, st), 1e-12);
        assertEquals(16.07, Stations.stopAhead(8, 0.1, 0.05 / 20, st), 1e-12, "past the braking distance forwards");
        assertEquals(7.3, Stations.stopAhead(10, -0.01, 0.05, st), 1e-12, "backwards: the first at or before it");
        assertEquals(22.07, Stations.stopAhead(21, 2, 0.01, st), 1e-12, "none far enough: the last");
        assertTrue(Stations.atStop(16.07, st));
        assertFalse(Stations.atStop(16.1, st));
    }
}
