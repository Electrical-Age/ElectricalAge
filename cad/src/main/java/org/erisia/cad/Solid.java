package org.erisia.cad;

import java.util.ArrayList;
import java.util.List;

/**
 * A solid of one material: its faces, made in its own frame and placed by its frame and its part's. Solids may overlap
 * (the house style); the checker finds faces that would z-fight. Set what it looks like with the fluent setters.
 */
public abstract class Solid {
    /** Its name, for relations and reports. */
    public String name = "";
    /** What it is made of: a target's material (a sprite id, a texture region's name), compared for z-fight exemption. */
    public Object material;
    /** Its colour (ARGB), multiplied into the texture. */
    public int argb = 0xFFFFFFFF;
    /** Its part (entity models: the moving piece it belongs to), or null for the model's frame. */
    public Part part;
    /** Its placement in its part's frame. */
    public Frame frame = Frame.IDENTITY;
    public UvMap uv;
    /** A decal's layer: grown outward by layer x LAYER px, so it sits over what it decorates without z-fighting. */
    public int layer;
    /** Faces on the target's outer boundary may be culled against a neighbour (voxel models: the block's faces). */
    public boolean cull;
    /** Not drawn: a hitbox or clearance volume, for relations only. */
    public boolean ghost;
    /** Entity models: the texture region's corner (texOffs). */
    public int texU, texV;

    public static final double LAYER = 0.1;

    public Solid named(String n) {
        name = n;
        return this;
    }

    public Solid material(Object m) {
        material = m;
        return this;
    }

    public Solid argb(int c) {
        argb = c;
        return this;
    }

    public Solid rgb(float r, float g, float b) {
        argb = 0xFF000000 | Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
        return this;
    }

    public Solid in(Part p) {
        part = p;
        return this;
    }

    public Solid at(Frame f) {
        frame = f;
        return this;
    }

    public Solid uv(UvMap m) {
        uv = m;
        return this;
    }

    public Solid layer(int n) {
        layer = n;
        return this;
    }

    public Solid culled() {
        cull = true;
        return this;
    }

    public Solid ghost() {
        ghost = true;
        return this;
    }

    public Solid tex(int u, int v) {
        texU = u;
        texV = v;
        return this;
    }

    /** Its smallest dimension (px): wall, radius x 2, length. Under Checker.EPS it is degenerate. */
    public abstract double thickness();

    /** Make its faces, in model space, into the sink. */
    public abstract void emit(FaceSink sink);

    public interface FaceSink {
        void accept(Face f);
    }

    public final List<Face> faces() {
        List<Face> out = new ArrayList<>();
        emit(out::add);
        return out;
    }

    /** Its placement in model space (part, then frame). */
    public Frame placement() {
        if (part == null) return frame;
        Frame w = part.world();
        return frame.identity() ? w : w.then(frame);
    }

    /**
     * A face from four corners (local), wound so its normal points along the hint (a degenerate first edge skipped,
     * as for a cap's triangle), placed, textured, into the sink.
     */
    protected final void quad(FaceSink sink, Frame place, double[] a, double[] b, double[] c, double[] d, double hx, double hy, double hz,
                              int kind, int index, int ia, int ib, int ic, int id) {
        double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        if (ux * ux + uy * uy + uz * uz < 1e-12) {
            ux = c[0] - b[0]; uy = c[1] - b[1]; uz = c[2] - b[2];
            vx = d[0] - b[0]; vy = d[1] - b[1]; vz = d[2] - b[2];
        }
        double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
        boolean keep = cx * hx + cy * hy + cz * hz >= 0;
        double[] p = new double[12];
        double[][] q = keep ? new double[][]{a, b, c, d} : new double[][]{a, d, c, b};
        int[] vid = keep ? new int[]{ia, ib, ic, id} : new int[]{ia, id, ic, ib};
        for (int i = 0; i < 4; i++) {
            p[3 * i] = q[i][0];
            p[3 * i + 1] = q[i][1];
            p[3 * i + 2] = q[i][2];
            if (place != null) place.apply(p, 3 * i);
        }
        Face f = new Face(this, p, vid, kind, index);
        if (uv != null) f.uv = uv.map(f);
        sink.accept(f);
    }

    /** The placement to apply, or null for none (identity: voxel models, kept exact and cheap). */
    protected final Frame placeOrNull() {
        Frame f = placement();
        return f.identity() ? null : f;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (name.isEmpty() ? "" : " " + name) + (part == null ? "" : " in " + part);
    }
}
