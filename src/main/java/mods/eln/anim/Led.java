package mods.eln.anim;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;

/** A status LED (a model part in its lit colour): drawn full-bright when on, dimmed to 30 % when off. */
public final class Led {
    private Led() {
    }

    public static void draw(Obj3D.Obj3DPart part, boolean on) {
        if (part == null) return;
        if (on) UtilsClient.drawLight(part);
        else {
            GL11.glColor3f(0.3f, 0.3f, 0.3f);
            part.draw();
            UtilsClient.glDefaultColor();
        }
    }
}
