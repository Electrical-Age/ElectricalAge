package org.erisia.cad;

/** A cuboid lo..hi on each axis (in its frame). Faces: kind FACE, index 0..5 = -x +x -y +y -z +z; corners 0..7 = bit 0 x, bit 1 y, bit 2 z. */
public final class Box extends Solid {
    public static final int FACE = 1;
    public final double[] lo, hi;
    /** Faces left out, as Box face numbers (bit i: -x +x -y +y -z +z): a face that lies on another solid. */
    public int skip;

    public Box(double x0, double y0, double z0, double x1, double y1, double z1) {
        lo = new double[]{x0, y0, z0};
        hi = new double[]{x1, y1, z1};
    }

    public Box(double[] lo, double[] hi) {
        this.lo = lo.clone();
        this.hi = hi.clone();
    }

    /** Only these faces, in mcmodel's letters: n (-z), s (+z), e (+x), w (-x), u (+y), d (-y). */
    public Box only(String faces) {
        skip = 0;
        String letters = "weduns";   // (Box face order: -x +x -y +y -z +z)
        for (int i = 0; i < 6; i++) if (faces.indexOf(letters.charAt(i)) < 0) skip |= 1 << i;
        return this;
    }

    @Override
    public double thickness() {
        return Math.min(hi[0] - lo[0], Math.min(hi[1] - lo[1], hi[2] - lo[2]));
    }

    @Override
    public void emit(FaceSink sink) {
        double g = layer * LAYER;
        double[][] c = new double[8][];
        for (int i = 0; i < 8; i++)
            c[i] = new double[]{(i & 1) == 0 ? lo[0] - g : hi[0] + g, (i & 2) == 0 ? lo[1] - g : hi[1] + g, (i & 4) == 0 ? lo[2] - g : hi[2] + g};
        Frame f = placeOrNull();
        if ((skip & 1) == 0) quad(sink, f, c[0], c[4], c[6], c[2], -1, 0, 0, FACE, 0, 0, 4, 6, 2);
        if ((skip & 2) == 0) quad(sink, f, c[1], c[3], c[7], c[5], 1, 0, 0, FACE, 1, 1, 3, 7, 5);
        if ((skip & 4) == 0) quad(sink, f, c[0], c[1], c[5], c[4], 0, -1, 0, FACE, 2, 0, 1, 5, 4);
        if ((skip & 8) == 0) quad(sink, f, c[2], c[6], c[7], c[3], 0, 1, 0, FACE, 3, 2, 6, 7, 3);
        if ((skip & 16) == 0) quad(sink, f, c[0], c[2], c[3], c[1], 0, 0, -1, FACE, 4, 0, 2, 3, 1);
        if ((skip & 32) == 0) quad(sink, f, c[4], c[5], c[7], c[6], 0, 0, 1, FACE, 5, 4, 5, 7, 6);
    }
}
