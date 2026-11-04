package mods.eln.misc;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ModelShape: a model's triangles to a device's boxes (Baughn 2026-10-30: hitboxes from geometry). */
class ModelShapeTest {
    /** The 12 triangles of the box (x0 y0 z0)..(x1 y1 z1). */
    static List<double[]> cube(double x0, double y0, double z0, double x1, double y1, double z1) {
        double[][] v = {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] f = {{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 2, 6, 7}, {0, 3, 7, 4}, {1, 2, 6, 5}};
        List<double[]> t = new ArrayList<>();
        for (int[] q : f) {
            t.add(tri(v[q[0]], v[q[1]], v[q[2]]));
            t.add(tri(v[q[0]], v[q[2]], v[q[3]]));
        }
        return t;
    }

    static double[] tri(double[] a, double[] b, double[] c) {
        return new double[]{a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]};
    }

    @Test
    void aGridAlignedBoxIsItself() {
        List<double[]> b = ModelShape.outline(cube(0.25, 0, 0.25, 0.75, 0.5, 0.75));
        assertEquals(1, b.size());
        assertArrayEquals(new double[]{0.25, 0, 0.25, 0.75, 0.5, 0.75}, b.get(0), 1e-12);
    }

    @Test
    void anOffGridBoxRoundsOutToItsCells() {
        List<double[]> b = ModelShape.outline(cube(0.26, 0.01, 0.3, 0.7, 0.4, 0.71));
        assertEquals(1, b.size());
        assertArrayEquals(new double[]{0.25, 0, 0.25, 0.75, 0.4375, 0.75}, b.get(0), 1e-12);
    }

    @Test
    void anLShapeIsTwoBoxesAndItsInnerCornerStaysOpenInTheOutline() {
        List<double[]> t = new ArrayList<>(cube(0, 0, 0, 1, 0.25, 1));   // a slab
        t.addAll(cube(0, 0.25, 0, 0.25, 1, 1));                          // a wall on its west edge
        List<double[]> b = ModelShape.outline(t);
        double vol = 0;
        for (double[] x : b) vol += (x[3] - x[0]) * (x[4] - x[1]) * (x[5] - x[2]);
        assertEquals(0.25 + 0.75 * 0.25, vol, 1e-12);
        assertTrue(b.size() <= 2, b.size() + " boxes");
    }

    @Test
    void aLoneFlapLyingOnAGridLineStillCounts() {
        // a single quad at y = 0.5 (a grid line), open on both sides: it neither vanishes nor fills a half-space
        List<double[]> t = List.of(tri(new double[]{0.25, 0.5, 0.25}, new double[]{0.75, 0.5, 0.25}, new double[]{0.75, 0.5, 0.75}),
            tri(new double[]{0.25, 0.5, 0.25}, new double[]{0.75, 0.5, 0.75}, new double[]{0.25, 0.5, 0.75}));
        List<double[]> b = ModelShape.outline(t);
        double vol = 0, ymin = 1, ymax = 0;
        for (double[] x : b) {
            vol += (x[3] - x[0]) * (x[4] - x[1]) * (x[5] - x[2]);
            ymin = Math.min(ymin, x[1]);
            ymax = Math.max(ymax, x[4]);
        }
        assertTrue(vol > 0 && ymin >= 0.4375 - 1e-12 && ymax <= 0.5625 + 1e-12, "vol " + vol + ", y " + ymin + ".." + ymax);
    }

    @Test
    void collisionClosesASlot() {
        // two posts 3 px apart on a slab: the outline keeps the slot, the collision fills it
        List<double[]> t = new ArrayList<>(cube(0, 0, 0, 1, 0.25, 1));
        t.addAll(cube(0.25, 0.25, 0.25, 0.4375, 0.75, 0.75));
        t.addAll(cube(0.625, 0.25, 0.25, 0.8125, 0.75, 0.75));
        assertTrue(holds(ModelShape.outline(t), 0.53, 0.5, 0.5) == false, "the outline keeps the slot");
        assertTrue(holds(ModelShape.collision(t), 0.53, 0.5, 0.5), "the collision fills it");
    }

    @Test
    void manyBoxesCoarsen() {
        // a staircase of 16 one-voxel steps in z and x: too many boxes at 1/16
        List<double[]> t = new ArrayList<>();
        for (int i = 0; i < 16; i++) for (int j = 0; j < 4; j++) t.addAll(cube(i / 16.0, 0, j * 0.25, (i + 1) / 16.0, (i + j * 4 + 1) / 64.0 + 0.01, j * 0.25 + 0.0625));
        assertTrue(ModelShape.boxes(t, 16, false, 24).size() <= 24);
    }

    static boolean holds(List<double[]> boxes, double x, double y, double z) {
        for (double[] b : boxes) if (x > b[0] && x < b[3] && y > b[1] && y < b[4] && z > b[2] && z < b[5]) return true;
        return false;
    }

    /** Every random box's centre is held, and nothing reaches past its cells. */
    @Property(tries = 200)
    void randomBoxesAreCovered(@ForAll @IntRange(min = 0, max = 12) int ax, @ForAll @IntRange(min = 0, max = 12) int ay,
                               @ForAll @IntRange(min = 0, max = 12) int az, @ForAll @IntRange(min = 1, max = 4) int w,
                               @ForAll @IntRange(min = 1, max = 4) int hgt, @ForAll @IntRange(min = 1, max = 4) int d) {
        double x0 = ax / 16.0 + 0.01, y0 = ay / 16.0 + 0.01, z0 = az / 16.0 + 0.01;
        double x1 = x0 + w / 16.0 - 0.02, y1 = y0 + hgt / 16.0 - 0.02, z1 = z0 + d / 16.0 - 0.02;
        List<double[]> b = ModelShape.outline(cube(x0, y0, z0, x1, y1, z1));
        assertTrue(holds(b, (x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2));
        for (double[] x : b)
            assertTrue(x[0] >= Math.floor(x0 * 16) / 16 - 1e-12 && x[3] <= Math.ceil(x1 * 16) / 16 + 1e-12
                && x[1] >= Math.floor(y0 * 16) / 16 - 1e-12 && x[4] <= Math.ceil(y1 * 16) / 16 + 1e-12, "within its cells");
    }
}
