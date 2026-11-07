package mods.eln.anim;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;

/**
 * Draws a belt as a chain of links (a model part: one short link built at the block's centre line, in the belt's
 * plane) along a {@link BeltPath}: each link turned to the belt's heading there, so the belt bends round its pulleys,
 * and shifted along it by the drive's turn, so it runs with them. A model names its belt in its .txt under the link
 * part ("o beltlink": f ax ay ar bx by br pitch, and lx ly where the link is built if not at the centre line, px in the model's frame; see {@link #of}).
 */
public final class Belt {
    public final Obj3D.Obj3DPart link;
    /** A marked link drawn every {@link #MARK_EVERY}th place, so the belt's running shows (AdventurAgent: a plain black
     *  belt looks still); optional. */
    public Obj3D.Obj3DPart mark;
    public static final int MARK_EVERY = 5;
    public final BeltPath path;
    final int links;
    /** Where the link part is built (px; default the block's centre line). */
    public double lx = 8, ly = 8;

    public Belt(Obj3D.Obj3DPart link, BeltPath path, double pitch) {
        this.link = link;
        this.path = path;
        this.links = Math.max(3, (int) Math.round(path.length / pitch));
    }

    /** The belt a model's part describes (its .txt floats), or null if the model has none. */
    public static Belt of(Obj3D.Obj3DPart link) {
        if (link == null || link.getFloat("ar") <= 0) return null;
        Belt b = new Belt(link, new BeltPath(link.getFloat("ax"), link.getFloat("ay"), link.getFloat("ar"),
            link.getFloat("bx"), link.getFloat("by"), link.getFloat("br")), link.getFloat("pitch") > 0 ? link.getFloat("pitch") : 0.8);
        if (link.getFloat("ly") != 0) {
            b.lx = link.getFloat("lx");
            b.ly = link.getFloat("ly");
        }
        return b;
    }

    /** A machine's own run of its belt: the last angle seen and the distance run since (Belt is shared by a model). */
    public static final class Run {
        double lastDeg = Double.NaN, dist;
    }

    /**
     * Draw it with pulley A turned by angle (degrees, clockwise as the belt runs: a part drawn with draw(angle, 0, 0, -1)).
     * Call inside the model's frame (front already turned). The angle may wrap (a counter kept in 0..360): run keeps the
     * distance going on across the wrap, so the belt never snaps back (Baughn: a whole turn of the pulley isn't a whole
     * number of links).
     */
    public void draw(float angleDeg, Run run) {
        double step = path.length / links;
        if (!Double.isNaN(run.lastDeg)) {
            double d = angleDeg - run.lastDeg;
            d -= 360 * Math.round(d / 360);   // (the shortest way round: the counter's wrap)
            run.dist += Math.toRadians(d) * path.ar;
            run.dist %= step * MARK_EVERY * 1000;   // (bounded; a multiple of the mark spacing, so no jump)
        }
        run.lastDeg = angleDeg;
        double shift = run.dist;
        for (int k = 0; k < links; k++) {
            double[] p = path.at(k * step + shift);
            GL11.glPushMatrix();
            GL11.glTranslatef((float) ((p[0] - 8) / 16), (float) ((p[1] - 8) / 16), 0f);
            GL11.glRotatef((float) Math.toDegrees(p[2]), 0f, 0f, 1f);
            GL11.glTranslatef((float) ((8 - lx) / 16), (float) ((8 - ly) / 16), 0f);   // (the link from where it was built)
            (mark != null && k % MARK_EVERY == 0 ? mark : link).draw();
            GL11.glPopMatrix();
        }
    }
}
