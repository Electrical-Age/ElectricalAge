package mods.eln.item.xray;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XRayRayMarchTest {
    static final int W = 48, H = 30;
    static final double[] EYE = {0.5, 0.5, 0.5}, FORWARD = {0, 0, 1}, UP = {0, 1, 0};

    static float[] scan(XRayRayMarch.Lookup world, double range) {
        float[] out = new float[W * H * 3];
        XRayRayMarch.scan(world, EYE, FORWARD, UP, Math.PI / 2, range, W, H, out);
        return out;
    }

    static float[] px(float[] pic, int x, int y) {
        int o = (y * W + x) * 3;
        return new float[]{pic[o], pic[o + 1], pic[o + 2]};
    }

    @Test
    void anOreAheadLightsTheCentreInItsSlotColour() {
        // a green-slot (1) ore block 3 ahead, rock everywhere else beyond the first block
        float[] pic = scan((x, y, z) -> x == 0 && y == 0 && z == 3 ? 1 : z >= 1 ? XRayRayMarch.SOLID : XRayRayMarch.AIR, 6);
        float[] c = px(pic, W / 2, H / 2);
        assertTrue(c[1] > 0.5f && c[1] > 2 * c[0], "centre is green: " + c[0] + " " + c[1] + " " + c[2]);
        float[] corner = px(pic, 0, 0);
        assertTrue(corner[1] < 0.4f, "corner is not: " + corner[1]);
    }

    @Test
    void nothingBeyondTheRange() {
        float[] pic = scan((x, y, z) -> z == 9 ? 0 : XRayRayMarch.AIR, 5);
        float max = 0;
        for (int i = 0; i < pic.length; i += 3) max = Math.max(max, pic[i]);
        assertTrue(max < 0.05f, "no red past the range: " + max);
    }

    @Test
    void leftIsLeftAndUpIsUp() {
        // ore to the left (-x seen looking +z with +y up is... right-handed: right = forward x up = (0,0,1)x(0,1,0) = (-1,0,0))
        float[] right = scan((x, y, z) -> x <= -2 && z >= 2 && z <= 4 ? 0 : XRayRayMarch.AIR, 6);
        assertTrue(px(right, W - 4, H / 2)[0] > px(right, 3, H / 2)[0], "an ore on the right shows on the right");
        float[] up = scan((x, y, z) -> y >= 2 && z >= 2 && z <= 4 ? 2 : XRayRayMarch.AIR, 6);
        assertTrue(px(up, W / 2, 2)[2] > px(up, W / 2, H - 3)[2], "an ore above shows at the top");
    }

    @Test
    void airIsDarkAndRockGrey() {
        float[] air = scan((x, y, z) -> XRayRayMarch.AIR, 5);
        float[] rock = scan((x, y, z) -> XRayRayMarch.SOLID, 5);
        assertTrue(px(rock, W / 2, H / 2)[0] > px(air, W / 2, H / 2)[0]);
        assertEquals(px(rock, W / 2, H / 2)[0], px(rock, W / 2, H / 2)[1], 1e-6);
    }
}
