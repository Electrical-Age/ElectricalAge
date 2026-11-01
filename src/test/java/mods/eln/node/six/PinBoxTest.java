package mods.eln.node.six;

import mods.eln.misc.LRDU;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A pin's box faces outward and has the same size toward every edge, so a turned device's pins look the same. */
class PinBoxTest {
    static final float FROM = 5 / 16f, TO = 8 / 16f, W = 3 / 16f, H = 1 / 16f;

    /** {min x, min y, min z, max x, max y, max z} of the quads' corners. */
    static float[] bounds(float[][] quads) {
        float[] b = {1e9f, 1e9f, 1e9f, -1e9f, -1e9f, -1e9f};
        for (float[] q : quads)
            for (int i = 0; i < 12; i++) {
                b[i % 3] = Math.min(b[i % 3], q[i]);
                b[i % 3 + 3] = Math.max(b[i % 3 + 3], q[i]);
            }
        return b;
    }

    @Test
    void everyQuadFacesOutOfTheBox() {
        for (LRDU l : LRDU.values()) {
            float[][] quads = PinBox.quads(l, FROM, TO, W, H);
            float[] b = bounds(quads);
            float[] c = {(b[0] + b[3]) / 2, (b[1] + b[4]) / 2, (b[2] + b[5]) / 2};
            for (float[] q : quads) {
                float[] u = {q[3] - q[0], q[4] - q[1], q[5] - q[2]}, v = {q[6] - q[3], q[7] - q[4], q[8] - q[5]};
                float[] n = {u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]};
                float out = 0;
                for (int a = 0; a < 3; a++) out += n[a] * ((q[a] + q[3 + a] + q[6 + a] + q[9 + a]) / 4 - c[a]);
                assertTrue(out > 0, l + ": a quad faces into the box (culled from outside)");
            }
        }
    }

    @Test
    void turningGivesTheSameBoxOnTheTurnedEdge() {
        for (LRDU l : LRDU.values()) {
            float[] b = bounds(PinBox.quads(l, FROM, TO, W, H));
            assertEquals(0, b[0], 1e-6);
            assertEquals(H, b[3], 1e-6);
            boolean alongZ = l == LRDU.Left || l == LRDU.Right;
            int along = alongZ ? 2 : 1, across = alongZ ? 1 : 2;
            float s = l == LRDU.Left || l == LRDU.Down ? -1 : 1;
            assertEquals(Math.min(s * FROM, s * TO), b[along], 1e-6, l + " along");
            assertEquals(Math.max(s * FROM, s * TO), b[along + 3], 1e-6, l + " along");
            assertEquals(-W / 2, b[across], 1e-6, l + " across");
            assertEquals(W / 2, b[across + 3], 1e-6, l + " across");
        }
    }
}
