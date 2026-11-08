package mods.eln.anim;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;

/**
 * A status LED (a model part): drawn full-bright when on, dimmed when off; by default in its own colour (dimmed to 30 %),
 * or tinted on and off as given (a red/green LED, a dark glass when off).
 */
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

    /** Tinted: on (r, g, b) full-bright, off (offR, offG, offB) lit by the world. */
    public static void draw(Obj3D.Obj3DPart part, boolean on, float r, float g, float b, float offR, float offG, float offB) {
        if (part == null) return;
        if (on) {
            GL11.glColor3f(r, g, b);
            UtilsClient.drawLight(part);
        } else {
            GL11.glColor3f(offR, offG, offB);
            part.draw();
        }
        UtilsClient.glDefaultColor();
    }

    /** A two-colour status LED, always lit: green when on, red when off (UtilsClient.ledOnOffColor). */
    public static void redGreen(Obj3D.Obj3DPart part, boolean on) {
        if (part == null) return;
        UtilsClient.ledOnOffColor(on);
        UtilsClient.drawLight(part);
        UtilsClient.glDefaultColor();
    }
}
