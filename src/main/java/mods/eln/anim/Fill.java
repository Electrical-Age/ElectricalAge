package mods.eln.anim;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;

/** A level (a gauge glass's column, a tank's liquid): the part scaled in y about its origin (its .txt originX/Y/Z). */
public final class Fill {
    private Fill() {
    }

    /**
     * The part drawn at scale share (of its modelled height) in y, about its origin's height. (Until 2026-11-28 a part's
     * origin read 0 whatever its .txt said, so a column stretched about the block's middle and not its foot: right only
     * at scale 1, the boilers' glasses at half; found with the Still Pot's glass.)
     */
    public static void draw(Obj3D.Obj3DPart part, float share) {
        if (part == null || share <= 0) return;
        drawAbout(part, part.origin()[1], share);
    }

    /** The part scaled share in y about the height y (block units, model frame): a fire bed's foot, not its origin. */
    public static void drawAbout(Obj3D.Obj3DPart part, float y, float share) {
        if (part == null || share <= 0) return;
        GL11.glPushMatrix();
        GL11.glTranslatef(0, y, 0);
        GL11.glScalef(1, share, 1);
        GL11.glTranslatef(0, -y, 0);
        part.draw();
        GL11.glPopMatrix();
    }
}
