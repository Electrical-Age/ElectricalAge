package mods.eln.robotics.motion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriveTest {
    final Drive lift = new Drive(200, 2.0, 1.0, 50, 0.8);

    @Test
    void speedFollowsTheLine() {
        assertEquals(2.0, lift.vmaxAt(200), 1e-9);
        assertEquals(1.0, lift.vmaxAt(100), 1e-9, "half voltage, half speed");
        assertEquals(2.2, lift.vmaxAt(300), 1e-9, "a little over nominal at most");
        assertEquals(0, lift.vmaxAt(40), "a dead line stalls it");
    }

    @Test
    void liftingCostsWork() {
        double idle = lift.watts(0, 0), up = lift.watts(2, 1000), down = lift.watts(-2, 1000);
        assertEquals(50, idle, 1e-9);
        assertEquals(50 + 2000 / 0.8, up, 1e-9);
        assertEquals(50, down, 1e-9, "lowering draws only the idle losses");
        assertEquals(800, Drive.resistance(200, 50), 1e-9);
    }
}
