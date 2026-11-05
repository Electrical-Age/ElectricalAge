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
}
