package org.erisia.cad;

/**
 * One flat convex face of a solid, in model coordinates (px): its corners (x y z each, wound counter-clockwise seen
 * from outside), outward unit normal, texture coordinates (u v per corner, whatever the target's convention; null if
 * the face is untextured), colour and material. A triangle may come as a quad with a corner repeated (a capped
 * cylinder's cap: the target wants quads).
 */
public final class Face {
    public final double[] xyz;
    public final int n;
    public final double nx, ny, nz;
    /** Which face of its solid: one of the solid's kinds (Box.FACE, Frustum.SIDE, ...) and an index (side number). */
    public final int kind, index;
    /** Per corner, the solid's own vertex number (Frustum: ring 0 is 0..S-1, ring 1 S..2S-1, the cap centres 2S, 2S+1). */
    public final int[] vid;
    public final Solid solid;
    public float[] uv;

    public Face(Solid solid, double[] xyz, int[] vid, int kind, int index) {
        this.solid = solid;
        this.xyz = xyz;
        this.vid = vid;
        this.n = xyz.length / 3;
        this.kind = kind;
        this.index = index;
        // Newell's normal: robust for a quad with a repeated corner
        double x = 0, y = 0, z = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double ax = xyz[3 * i], ay = xyz[3 * i + 1], az = xyz[3 * i + 2], bx = xyz[3 * j], by = xyz[3 * j + 1], bz = xyz[3 * j + 2];
            x += (ay - by) * (az + bz);
            y += (az - bz) * (ax + bx);
            z += (ax - bx) * (ay + by);
        }
        double l = Math.sqrt(x * x + y * y + z * z);
        if (l < 1e-12) {
            nx = ny = nz = 0;
        } else {
            nx = x / l;
            ny = y / l;
            nz = z / l;
        }
    }

    /** Twice the area (px^2), from the unnormalised normal. */
    public double area() {
        double x = 0, y = 0, z = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double ax = xyz[3 * i], ay = xyz[3 * i + 1], az = xyz[3 * i + 2], bx = xyz[3 * j], by = xyz[3 * j + 1], bz = xyz[3 * j + 2];
            x += (ay - by) * (az + bz);
            y += (az - bz) * (ax + bx);
            z += (ax - bx) * (ay + by);
        }
        return Math.sqrt(x * x + y * y + z * z) / 2;
    }

    public int argb() {
        return solid == null ? 0xFFFFFFFF : solid.argb;
    }

    public Object material() {
        return solid == null ? null : solid.material;
    }

    /** The face's plane offset (normal . corner). */
    public double plane() {
        return nx * xyz[0] + ny * xyz[1] + nz * xyz[2];
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(solid == null ? "face" : solid.toString()).append(" kind ").append(kind).append('/').append(index).append(" [");
        for (int i = 0; i < n; i++) b.append(i == 0 ? "" : " ").append(String.format("(%.3f %.3f %.3f)", xyz[3 * i], xyz[3 * i + 1], xyz[3 * i + 2]));
        return b.append("]").toString();
    }
}
