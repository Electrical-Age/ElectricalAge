package mods.eln.robotics.servo;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Who rides a car, and the client's carry of its own player (the pure core of client.CarCarry). */
class RidersTest {
    /** The car's plate with its top at y. */
    static AABB car(double top) {
        return new AABB(-0.5, top - 1 / 16.0, -0.5, 0.5, top, 0.5);   // (ElevatorCar.THICK)
    }

    /** A player's box with its feet at (x, y). */
    static AABB player(double x, double y) {
        return new AABB(x - 0.3, y, -0.3, x + 0.3, y + 1.8, 0.3);
    }

    @Test
    void standingOnTopRides() {
        assertTrue(Riders.onTop(player(0, 1), car(1), 0.15));
        assertTrue(Riders.onTop(player(0, 1.1), car(1), 0.15), "a little above (a hop, a step)");
        assertFalse(Riders.onTop(player(0, 1.3), car(1), 0.15), "jumping clear");
        assertFalse(Riders.onTop(player(0, 0.5), car(1), 0.15), "below it");
        assertTrue(Riders.onTop(player(0.75, 1), car(1), 0.15), "on the edge, overhanging");
        assertFalse(Riders.onTop(player(0.85, 1), car(1), 0.15), "beside it");
    }

    @Test
    void toleranceCoversAStep() {
        assertEquals(0.15, Riders.tolerance(0.1));
        assertEquals(0.3, Riders.tolerance(-0.3));
    }

    @Test
    void theServerCountsALatePlayerAsARider() {
        assertTrue(Riders.playerOnTop(player(0, 0.2), car(1), 0.15), "a rising car's passenger, a round trip behind");
        assertTrue(Riders.playerOnTop(player(0, 1.3), car(1), 0.15), "a sinking car's passenger, a round trip behind");
        assertFalse(Riders.playerOnTop(player(0, -1.5), car(1), 0.15), "well under it: in the way");
        assertFalse(Riders.onTop(player(0, 0.2), car(1), 0.15), "mobs are where the server says");
    }

    @Test
    void carryLandsAStandingPlayerOnTheNewTop() {
        Riders.Carry k = Riders.carry(player(0, 1), true, car(1), car(1.1));
        assertTrue(k.carried());
        assertEquals(0.1, k.delta().y, 1e-9);
        assertTrue(k.keepOnGround());
        // a sink of 0.02 below the top (rounding, a late tick) is taken up, not carried along
        k = Riders.carry(player(0, 0.98), true, car(1), car(1.1));
        assertEquals(0.12, k.delta().y, 1e-9);
        // down
        k = Riders.carry(player(0, 1), true, car(1), car(0.9));
        assertEquals(-0.1, k.delta().y, 1e-9);
    }

    @Test
    void carryInTheAirGoesByTheStep() {
        Riders.Carry k = Riders.carry(player(0, 1.08), false, car(1), car(1.1));
        assertTrue(k.carried());
        assertEquals(0.1, k.delta().y, 1e-9);
        assertFalse(k.keepOnGround());
    }

    @Test
    void noCarryOffTheCarOrStopped() {
        assertFalse(Riders.carry(player(2, 1), true, car(1), car(1.1)).carried());
        assertFalse(Riders.carry(player(0, 1), true, car(1), car(1)).carried());
        assertFalse(Riders.carry(player(0, 1.5), false, car(1), car(1.1)).carried(), "jumping clear");
    }

    @Test
    void aFastStepStillFindsItsRider() {
        // 0.3 m a tick (6 m/s): the player stood on the top before the step; the tolerance grows with the step
        Riders.Carry k = Riders.carry(player(0, 1), true, car(1), car(1.3));
        assertTrue(k.carried());
        assertEquals(0.3, k.delta().y, 1e-9);
    }

    @Test
    void sweepBoxStandsOnTheCarClippedToIt() {
        AABB s = Riders.standing(player(0.6, 0.4), car(1));
        assertEquals(1, s.minY, 1e-9);
        assertEquals(2.8, s.maxY, 1e-9);
        assertEquals(0.5, s.maxX, 1e-9, "the overhang is the landing's business");
        assertEquals(0.3, s.minX, 1e-9);
    }
}
