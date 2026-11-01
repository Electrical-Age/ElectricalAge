package mods.eln.node.six;

import mods.eln.misc.LRDU;

/** The box of a device's pin, as quads; pure, so its geometry can be tested off the client. */
public final class PinBox {
    private PinBox() {
    }

    /**
     * The four visible faces (top, end, two sides; none on the face it lies on) of a box on the face toward edge l, from
     * `from` to `to` off the centre, w wide and h high, in the element frame (x off the face, Left/Right along z, Down/Up
     * along y): per quad x y z of four corners, counter-clockwise seen from outside, as culling wants. Toward Right or
     * Down the box is a mirror image of the one toward Left or Up, so those quads run the other way round.
     */
    public static float[][] quads(LRDU l, float from, float to, float w, float h) {
        float s = l == LRDU.Left || l == LRDU.Down ? -1 : 1;
        float a0 = s * from, a1 = s * to, b0 = -w / 2, b1 = w / 2;
        boolean alongZ = l == LRDU.Left || l == LRDU.Right;
        boolean reverse = (s > 0) == alongZ;
        float[][] faces = {
            {h, a0, b0, h, a1, b0, h, a1, b1, h, a0, b1},     // top
            {0, a1, b0, 0, a1, b1, h, a1, b1, h, a1, b0},     // end
            {0, a0, b0, 0, a1, b0, h, a1, b0, h, a0, b0},     // sides
            {0, a0, b1, h, a0, b1, h, a1, b1, 0, a1, b1},
        };
        float[][] out = new float[faces.length][12];
        for (int q = 0; q < faces.length; q++)
            for (int v = 0; v < 4; v++) {
                float[] f = faces[q];
                int i = 3 * (reverse ? 3 - v : v);
                float x = f[i], a = f[i + 1], b = f[i + 2];
                out[q][3 * v] = x;
                out[q][3 * v + 1] = alongZ ? b : a;
                out[q][3 * v + 2] = alongZ ? a : b;
            }
        return out;
    }
}
