package org.erisia.cad;

/**
 * A single polygon from elsewhere (another engine's output: a baked quad, an OBJ face), so the checker can read
 * geometry not made here. Corners in px, wound counter-clockwise seen from outside; uv may be null.
 */
public final class Flat extends Solid {
    final double[] xyz;
    final float[] uvs;

    public Flat(double[] xyz, float[] uv) {
        this.xyz = xyz;
        this.uvs = uv;
    }

    /** One face of a polygon soup, with its look. */
    public static Face face(double[] xyz, float[] uv, int argb, Object material, String name) {
        Flat f = new Flat(xyz, uv);
        f.argb = argb;
        f.material = material;
        f.name = name;
        return f.faces().get(0);
    }

    @Override
    public double thickness() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public void emit(FaceSink sink) {
        int[] vid = new int[xyz.length / 3];
        for (int i = 0; i < vid.length; i++) vid[i] = i;
        double[] p = xyz.clone();
        Frame f = placeOrNull();
        if (f != null) for (int i = 0; i < p.length; i += 3) f.apply(p, i);
        Face face = new Face(this, p, vid, 0, 0);
        face.uv = uvs;
        sink.accept(face);
    }
}
