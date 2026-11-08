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

    /** A ramp from a colour function, self-lit from litFrom (below 0: always). */
    public static Ramp ramp(java.util.function.Function<Float, float[]> rgb, float litFrom) {
        return new Ramp() {
            @Override
            public float[] rgb(float k) {
                return rgb.apply(k);
            }

            @Override
            public float litFrom() {
                return litFrom;
            }
        };
    }

    public static void draw(Obj3D.Obj3DPart part, float k, Ramp ramp) {
        if (part == null) return;
        draw(part::draw, k, ramp);
    }

    /** As draw(part, ...), for a draw of its own (a part scaled to the fire's height: Fill.drawAbout). */
    public static void draw(Runnable drawer, float k, Ramp ramp) {
        boolean lit = k > ramp.litFrom();
        float[] c = ramp.rgb(k);
        if (lit) UtilsClient.disableLight();
        GL11.glColor4f(c[0], c[1], c[2], 1);
        drawer.run();
        if (lit) UtilsClient.enableLight();
        UtilsClient.glDefaultColor();
    }
}
