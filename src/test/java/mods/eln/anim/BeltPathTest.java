package mods.eln.anim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The belt's path: closed, continuous, on the pulleys' rims where it wraps them, its length as worked out. */
class BeltPathTest {
    @Test
    void continuousAndClosed() {
        BeltPath p = new BeltPath(5.6, 9.5, 3.3, 12.9, 3.8, 0.9);
        double[] prev = p.at(0);
        int n = 2000;
        for (int k = 1; k <= n; k++) {
            double[] q = p.at(p.length * k / n);
            assertTrue(Math.hypot(q[0] - prev[0], q[1] - prev[1]) <= p.length / n * 1.01 + 1e-9, "a jump at " + k);
            prev = q;
        }
        double[] start = p.at(0), end = p.at(p.length);
        assertEquals(start[0], end[0], 1e-9);
        assertEquals(start[1], end[1], 1e-9);
    }

    @Test
    void wrapsTheRims() {
        BeltPath p = new BeltPath(0, 0, 3, 10, 0, 1);
        int onA = 0, onB = 0;
        for (int k = 0; k < 400; k++) {
            double[] q = p.at(p.length * k / 400);
            double da = Math.hypot(q[0], q[1]), db = Math.hypot(q[0] - 10, q[1]);
            assertTrue(da >= 3 - 1e-6 && db >= 1 - 1e-6, "inside a pulley at " + k);
            if (Math.abs(da - 3) < 1e-6) onA++;
            if (Math.abs(db - 1) < 1e-6) onB++;
        }
        assertTrue(onA > onB && onB > 0, onA + " " + onB);   // (the big pulley wraps more of the belt)
    }

    @Test
    void equalPulleysLength() {
        BeltPath p = new BeltPath(0, 0, 2, 10, 0, 2);
        assertEquals(2 * 10 + 2 * Math.PI * 2, p.length, 1e-9);
    }
}
