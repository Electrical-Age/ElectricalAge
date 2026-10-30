package org.erisia.cad;

import java.util.ArrayList;
import java.util.List;

/**
 * A solid given as its polygons (convex, wound counter-clockwise seen from outside), for shapes the other solids
 * don't make: what mcmodel.py's primitives made, oriented boxes, swept and turned pieces. closed: the polygons enclose
 * a volume (its thickness is checked); an open mesh is a surface (a single face, a tube without caps).
 */
public final class Mesh extends Solid {
    public final List<double[]> polys = new ArrayList<>();
    public boolean closed;
    /** Closed and convex (a box, a capped prism): it may hide what lies inside it from the checker. */
    public boolean convex;

    public Mesh(String material) {
        this.material = material;
    }

    /** Add a polygon (x y z per corner). */
    public Mesh poly(double[]... corners) {
        double[] p = new double[3 * corners.length];
        for (int i = 0; i < corners.length; i++) System.arraycopy(corners[i], 0, p, 3 * i, 3);
        polys.add(p);
        return this;
    }

    @Override
    public Mesh free(String why) {
        super.free(why);
        return this;
    }

    @Override
    public Mesh named(String n) {
        super.named(n);
        return this;
    }

    @Override
    public Mesh layer(int n) {
        super.layer(n);
        return this;
    }

    public Mesh closed(boolean c) {
        closed = c;
        return this;
    }

    /** A copy with each corner mapped (in place on the copy's arrays) and, if flip, the winding reversed. */
    public Mesh mapped(java.util.function.Consumer<double[]> map, boolean flip) {
        Mesh m = new Mesh((String) material);
        m.closed = closed;
        m.convex = convex;
        m.name = name;
        m.argb = argb;
        for (double[] p : polys) {
            int n = p.length / 3;
            double[] q = new double[p.length];
            for (int i = 0; i < n; i++) {
                int src = flip ? n - 1 - i : i;
                double[] v = {p[3 * src], p[3 * src + 1], p[3 * src + 2]};
                map.accept(v);
                System.arraycopy(v, 0, q, 3 * i, 3);
            }
            m.polys.add(q);
        }
        return m;
    }

    @Override
    public double thickness() {
        if (!closed) return Double.POSITIVE_INFINITY;
        double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE}, hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (double[] p : polys)
            for (int i = 0; i < p.length; i++) {
                lo[i % 3] = Math.min(lo[i % 3], p[i]);
                hi[i % 3] = Math.max(hi[i % 3], p[i]);
            }
        return Math.min(hi[0] - lo[0], Math.min(hi[1] - lo[1], hi[2] - lo[2]));
    }

    @Override
    public String toString() {
        double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE}, hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (double[] p : polys)
            for (int i = 0; i < p.length; i++) {
                lo[i % 3] = Math.min(lo[i % 3], p[i]);
                hi[i % 3] = Math.max(hi[i % 3], p[i]);
            }
        return (name.isEmpty() ? "" : name + " ") + material + String.format(java.util.Locale.ROOT, " [%.2f..%.2f %.2f..%.2f %.2f..%.2f]", lo[0], hi[0], lo[1], hi[1],
            lo[2], hi[2]) + (body == null ? "" : " in " + body);
    }

    @Override
    public void emit(FaceSink sink) {
        Frame f = placeOrNull();
        int idx = 0;
        for (double[] p : polys) {
            double[] q = p.clone();
            if (layer != 0) {   // (a decal: each polygon moved out along its normal by layer x LAYER)
                Face raw = new Face(this, q, new int[q.length / 3], 0, 0);
                for (int i = 0; i < q.length; i += 3) {
                    q[i] += raw.nx * layer * LAYER;
                    q[i + 1] += raw.ny * layer * LAYER;
                    q[i + 2] += raw.nz * layer * LAYER;
                }
            }
            if (f != null) for (int i = 0; i < q.length; i += 3) f.apply(q, i);
            int[] vid = new int[q.length / 3];
            for (int i = 0; i < vid.length; i++) vid[i] = i;
            Face face = new Face(this, q, vid, 0, idx++);
            if (uv != null) face.uv = uv.map(face);
            sink.accept(face);
        }
    }
}
