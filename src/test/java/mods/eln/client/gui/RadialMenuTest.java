package mods.eln.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Which ring and segment of a radial menu lie under the mouse (RadialMenu.at). */
public class RadialMenuTest {
    static final int[] COUNTS = {4, 3};

    static double mid(int ring) {
        return RadialMenu.inner(ring) + RadialMenu.RING / 2.0;
    }

    @Test
    void centreRingsAndGaps() {
        assertArrayEquals(new int[]{-1, 0}, RadialMenu.at(0, 0, COUNTS));
        assertArrayEquals(new int[]{-1, 0}, RadialMenu.at(5, -10, COUNTS));
        assertNull(RadialMenu.at(0, -(RadialMenu.CENTRE + 1), COUNTS), "the gap after the centre");
        assertNull(RadialMenu.at(0, -(RadialMenu.inner(1) + RadialMenu.RING + 1), COUNTS), "outside");
    }

    @Test
    void segmentsGoClockwiseFromTheTop() {
        double r = mid(0);
        assertArrayEquals(new int[]{0, 0}, RadialMenu.at(0, -r, COUNTS));    // top
        assertArrayEquals(new int[]{0, 1}, RadialMenu.at(r, 0, COUNTS));     // right
        assertArrayEquals(new int[]{0, 2}, RadialMenu.at(0, r, COUNTS));     // bottom
        assertArrayEquals(new int[]{0, 3}, RadialMenu.at(-r, 0, COUNTS));    // left
        assertArrayEquals(new int[]{0, 0}, RadialMenu.at(-r * 0.7, -r * 0.71, COUNTS));   // just left of top-left diagonal
        r = mid(1);
        assertArrayEquals(new int[]{1, 0}, RadialMenu.at(0, -r, COUNTS));
        assertArrayEquals(new int[]{1, 1}, RadialMenu.at(r * 0.87, r * 0.5, COUNTS));   // 120 degrees
        assertArrayEquals(new int[]{1, 2}, RadialMenu.at(-r * 0.87, r * 0.5, COUNTS));  // 240 degrees
    }
}
