package mods.eln.anim;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;

/**
 * A part that glows with heat or fire (a firebox window, peepholes): its colour from a {@link Ramp} of a 0..1 level, drawn
 * unlit (self-lit) above {@link Ramp#litFrom}. Each device keeps its own ramp, so its colours are as they were; for a
 * temperature-true colour use misc.Incandescence instead.
 */
public final class Glow {
    private Glow() {
    }

    /** The colour of a level (0..1), and the level from which it's drawn self-lit. */
    public interface Ramp {
        float[] rgb(float k);

        default float litFrom() {
            return 0.02f;
        }
    }

    public static void draw(Obj3D.Obj3DPart part, float k, Ramp ramp) {
        if (part == null) return;
        boolean lit = k > ramp.litFrom();
        float[] c = ramp.rgb(k);
        if (lit) UtilsClient.disableLight();
        GL11.glColor4f(c[0], c[1], c[2], 1);
        part.draw();
        if (lit) UtilsClient.enableLight();
        UtilsClient.glDefaultColor();
    }
}
