package mods.eln.oil;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * What a sight glass shows (Lashtear 2026-11-28: the Still Pot's glass showed a level when empty, painted into its
 * texture; Baughn 2026-11-28: do it properly, the real level, live): the vessel's liquid layers from the bottom, each
 * one's top (0..1 of the glass) and colour. The element publishes it when it changes by STEP; the render draws the
 * model's "level" part once a layer (anim.Fill), tinted. Network only: nothing of it is saved.
 */
public final class SightGlass {
    /** A layer's top moving this far (of the glass) publishes it again: 2 %, as a tank's. */
    public static final float STEP = 0.02f;
    /** Under this share a layer isn't drawn. */
    static final float MIN = 0.005f;
    /** Water on the white column: the tank's blue (the palette's gaugewater), not its species' colour. */
    static final int WATER = 0x3870BE;
    public static final SightGlass EMPTY = new SightGlass(new float[0], new int[0]);

    /** An element with a sight glass: what it shows now (what it publishes). */
    public interface Host {
        SightGlass glass();
    }

    public final float[] tops;
    public final int[] colours;

    public SightGlass(float[] tops, int[] colours) {
        this.tops = tops;
        this.colours = colours;
    }

    /** The liquid layers of v, from the bottom: each one's top (share of the volume). */
    static float[] tops(FluidNode v) {
        float[] t = new float[v.layers()];
        for (int l = 0; l < t.length; l++) t[l] = (float) v.layerTop(l);
        return t;
    }

    /** The liquid layers of v, from the bottom: each one's colour, its main species' (by mass). */
    static int[] colours(FluidNode v) {
        var reg = SpeciesRegistry.get();
        int[] c = new int[v.layers()];
        for (int l = 0; l < c.length; l++) {
            int best = -1;
            double most = 0;
            for (int id = 0; id < v.speciesBound(); id++)
                if (v.layerOf(id) == l && v.liquid(id) > most) { best = id; most = v.liquid(id); }
            c[l] = best < 0 ? 0 : reg.byId(best).colour;
        }
        return c;
    }

    /** What v's glass shows (null: empty). */
    public static SightGlass of(FluidNode v) {
        return v == null ? EMPTY : new SightGlass(tops(v), colours(v));
    }

    /** The top of all the liquid (0..1 of the glass). */
    public float fill() {
        return tops.length == 0 ? 0 : Math.max(0, Math.min(1, tops[tops.length - 1]));
    }

    /** Whether this looks different from what was last published: other layers or colours, or a top STEP away. */
    public boolean differs(SightGlass shown) {
        if (shown == null || tops.length != shown.tops.length) return true;
        for (int l = 0; l < tops.length; l++)
            if (colours[l] != shown.colours[l] || Math.abs(tops[l] - shown.tops[l]) > STEP) return true;
        return false;
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeByte(tops.length);
        for (int l = 0; l < tops.length; l++) {
            stream.writeFloat(tops[l]);
            stream.writeInt(colours[l]);
        }
    }

    public static SightGlass read(DataInputStream stream) throws IOException {
        int n = stream.readByte();
        float[] tops = new float[n];
        int[] colours = new int[n];
        for (int l = 0; l < n; l++) {
            tops[l] = stream.readFloat();
            colours[l] = stream.readInt();
        }
        return new SightGlass(tops, colours);
    }

    /**
     * The layers drawn with obj's "level" part: a white column the glass's whole height (its .txt "height", block
     * units) about its foot (its origin), a layer at a time from the one below's top, in the layer's colour.
     */
    public void draw(Obj3D obj) {
        Obj3D.Obj3DPart level = obj == null ? null : obj.getPart("level");
        if (level == null) return;
        float height = level.getFloat("height"), below = 0;
        int water = SpeciesRegistry.get().byId(SpeciesRegistry.WATER).colour;
        for (int l = 0; l < tops.length; l++) {
            float top = Math.max(below, Math.min(1, tops[l]));
            if (top - below > MIN) {
                int c = colours[l] == water ? WATER : colours[l];
                GL11.glColor4f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, 1);
                GL11.glPushMatrix();
                GL11.glTranslatef(0, below * height, 0);
                mods.eln.anim.Fill.draw(level, top - below);
                GL11.glPopMatrix();
            }
            below = top;
        }
        GL11.glColor4f(1, 1, 1, 1);
    }
}
