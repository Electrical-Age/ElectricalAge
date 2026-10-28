package mods.eln.bike;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The bike's riding rules (Baughn 2026-10-28): speed is input averaged over 2.5 s; horse speed on battery; jump ~1 block. */
public class BikePhysicsTest {
    @Test
    void holdingForwardReachesTopSpeedIn2Point5Seconds() {
        BikePhysics p = new BikePhysics();
        double v = 0;
        for (int t = 1; t <= 50; t++) {
            v = p.speed(1, true);
            if (t == 25) assertEquals(BikePhysics.VMAX_POWERED / 2, v, 1e-6);   // halfway in 1.25 s
        }
        assertEquals(BikePhysics.VMAX_POWERED, v, 1e-6);
        assertEquals(9.5, v * 20, 1e-6);   // an average horse, b/s
    }

    @Test
    void tappingForwardGivesPartSpeedAndItCoastsDown() {
        BikePhysics p = new BikePhysics();
        double v = 0;
        for (int t = 0; t < 20; t++) v = p.speed(1, true);   // 1 s of W
        for (int t = 0; t < 30; t++) v = p.speed(0, true);   // then let go
        assertEquals(0.4 * BikePhysics.VMAX_POWERED, v, 1e-6);   // still 40% after 2.5 s
        for (int t = 0; t < 20; t++) v = p.speed(0, true);
        assertEquals(0, v, 1e-9);
    }

    @Test
    void withoutBatteryOnlyALittleFasterThanSprinting() {
        BikePhysics p = new BikePhysics();
        double v = 0;
        for (int t = 0; t < 60; t++) v = p.speed(1, false);
        assertTrue(v * 20 > 5.612 && v * 20 < 7.5, v * 20 + " b/s");
    }

    @Test
    void backBrakesThenBacksUpSlowly() {
        BikePhysics p = new BikePhysics();
        for (int t = 0; t < 50; t++) p.speed(1, true);
        double v = 0;
        for (int t = 0; t < 20; t++) v = p.speed(-1, true);   // S counts as -1 in the average: it brakes
        assertEquals(0.2 * BikePhysics.VMAX_POWERED, v, 1e-6);
        for (int t = 0; t < 100; t++) v = p.speed(-1, true);
        assertEquals(-BikePhysics.REVERSE, v, 1e-9);
    }

    @Test
    void aFullJumpClearsOneBlockButNotOneAndAHalf() {
        double peak = BikePhysics.jumpPeak(BikePhysics.jumpVelocity(1));
        assertTrue(peak > 1.15 && peak < 1.4, "peak " + peak);   // 1.31: one block with room, never 1.5
        assertTrue(BikePhysics.jumpPeak(BikePhysics.jumpVelocity(0.4f)) < 1.0);   // a tap doesn't
        assertTrue(BikePhysics.STEP_HEIGHT >= 0.5f && BikePhysics.STEP_HEIGHT < 1.0f);
    }

    @Test
    void turnsTowardTheRidersLookTheShortWay() {
        assertEquals(10, BikePhysics.turn(0, 10), 1e-6);
        assertEquals(BikePhysics.TURN_PER_TICK, BikePhysics.turn(0, 90), 1e-6);
        assertEquals(10 - BikePhysics.TURN_PER_TICK, BikePhysics.turn(10, 270), 1e-6);   // -100 is nearer than +260
    }

    @Test
    void crashesOnlyAboveThreeBlocksASecond() {
        assertFalse(BikePhysics.crashes(2.9 / 20));
        assertTrue(BikePhysics.crashes(3.1 / 20));
        assertEquals(BikePhysics.MOTOR_WATTS, BikePhysics.motorWatts(BikePhysics.VMAX_POWERED), 1e-9);
        assertEquals(BikePhysics.MOTOR_WATTS / 4, BikePhysics.motorWatts(BikePhysics.VMAX_POWERED / 2), 1e-9);
    }
}
