package mods.eln.anim;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;

/**
 * Draws a belt as a chain of links (a model part: one short link built at the block's centre line, in the belt's
 * plane) along a {@link BeltPath}: each link turned to the belt's heading there, so the belt bends round its pulleys,
 * and shifted along it by the drive's turn, so it runs with them. A model names its belt in its .txt under the link
 * part ("o beltlink": f ax ay ar bx by br pitch, px in the model's frame; see {@link #of}).
 */
public final class Belt {
    public final Obj3D.Obj3DPart link;
    public final BeltPath path;
    final int links;

    public Belt(Obj3D.Obj3DPart link, BeltPath path, double pitch) {
        this.link = link;
        this.path = path;
        this.links = Math.max(3, (int) Math.round(path.length / pitch));
    }

    /** The belt a model's part describes (its .txt floats), or null if the model has none. */
    public static Belt of(Obj3D.Obj3DPart link) {
        if (link == null || link.getFloat("ar") <= 0) return null;
        return new Belt(link, new BeltPath(link.getFloat("ax"), link.getFloat("ay"), link.getFloat("ar"),
            link.getFloat("bx"), link.getFloat("by"), link.getFloat("br")), link.getFloat("pitch") > 0 ? link.getFloat("pitch") : 0.8);
    }

    /**
     * Draw it with pulley A turned by angle (degrees, clockwise as the belt runs: a part drawn with draw(angle, 0, 0, -1)).
     * Call inside the model's frame (front already turned).
     */
    public void draw(float angleDeg) {
        double shift = Math.toRadians(angleDeg) * path.ar, step = path.length / links;
        for (int k = 0; k < links; k++) {
            double[] p = path.at(k * step + shift);
            GL11.glPushMatrix();
            GL11.glTranslatef((float) ((p[0] - 8) / 16), (float) ((p[1] - 8) / 16), 0f);
            GL11.glRotatef((float) Math.toDegrees(p[2]), 0f, 0f, 1f);
            link.draw();
            GL11.glPopMatrix();
        }
    }
}
