package mods.eln.anim;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;

/** A level (a gauge glass's column, a tank's liquid): the part scaled in y about its origin (its .txt originX/Y/Z). */
public final class Fill {
    private Fill() {
    }

    /** The part drawn at scale share (of its modelled height) in y. */
    public static void draw(Obj3D.Obj3DPart part, float share) {
        if (part == null || share <= 0) return;
        float ox = part.getFloat("originX"), oy = part.getFloat("originY"), oz = part.getFloat("originZ");
        GL11.glPushMatrix();
        GL11.glTranslatef(ox, oy, oz);
        GL11.glScalef(1, share, 1);
        GL11.glTranslatef(-ox, -oy, -oz);
        part.draw();
        GL11.glPopMatrix();
    }
}
