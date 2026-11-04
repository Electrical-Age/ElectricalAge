package mods.eln.node.transparent;

import mods.eln.misc.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * ModelHitbox turns a box as Direction.glRotateXnRef turns the model: checked against the GL calls themselves, as
 * matrices (glRotatef(a, axis) then glScalef, each applied to the vertex after the ones that follow it), every front.
 */
class ModelHitboxTurnTest {
    /** v' = M v for the calls of glRotateXnRef on front f, in order (a GL call multiplies on the right). */
    static double[][] gl(Direction f) {
        double[][] m = id();
        switch (f) {
            case XP -> m = mul(m, rot(180, 0, 1, 0));
            case YN -> m = mul(mul(m, rot(90, 0, 0, 1)), scale(1, -1, -1));
            case YP -> m = mul(m, rot(90, 0, 0, -1));
            case ZN -> m = mul(m, rot(270, 0, 1, 0));
            case ZP -> m = mul(m, rot(90, 0, 1, 0));
            default -> {
            }
        }
        return m;
    }

    @Test
    void everyFrontAsTheRenderTurnsIt() {
        double[][] pts = {{0.3, -0.1, 0.2}, {-0.45, 0.4, 0.05}, {0.1, 0.2, -0.35}};
        for (Direction f : Direction.values()) {
            double[][] m = gl(f);
            for (double[] p : pts) {
                double[] want = new double[3];
                for (int i = 0; i < 3; i++) want[i] = m[i][0] * p[0] + m[i][1] * p[1] + m[i][2] * p[2];
                assertArrayEquals(want, ModelHitbox.turnedPoint(p[0], p[1], p[2], f), 1e-9, f.toString());
            }
        }
    }

    @Test
    void aBoxStaysABoxTurned() {
        // a slab on the model's floor, its back half: turned any way it keeps its volume
        double[] b = {0, 0, 0, 0.5, 0.25, 1};
        for (Direction f : Direction.values()) {
            double[] t = ModelHitbox.turned(b, f);
            double vol = (t[3] - t[0]) * (t[4] - t[1]) * (t[5] - t[2]);
            assertArrayEquals(new double[]{0.125}, new double[]{vol}, 1e-9, f + " keeps its volume");
        }
    }

    static double[][] id() {
        return new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    }

    static double[][] scale(double x, double y, double z) {
        return new double[][]{{x, 0, 0}, {0, y, 0}, {0, 0, z}};
    }

    /** glRotatef: angle in degrees about (x, y, z), right-handed. */
    static double[][] rot(double deg, double x, double y, double z) {
        double n = Math.sqrt(x * x + y * y + z * z), c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg));
        x /= n;
        y /= n;
        z /= n;
        return new double[][]{
            {x * x * (1 - c) + c, x * y * (1 - c) - z * s, x * z * (1 - c) + y * s},
            {y * x * (1 - c) + z * s, y * y * (1 - c) + c, y * z * (1 - c) - x * s},
            {x * z * (1 - c) - y * s, y * z * (1 - c) + x * s, z * z * (1 - c) + c}};
    }

    static double[][] mul(double[][] a, double[][] b) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) r[i][j] += a[i][k] * b[k][j];
        return r;
    }
}
