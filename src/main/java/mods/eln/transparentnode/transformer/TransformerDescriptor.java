package mods.eln.transparentnode.transformer;

import mods.eln.gl.GL11;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class TransformerDescriptor extends TransparentNodeDescriptor {
    public final float minimalLoadToHum;

    public TransformerDescriptor(String name, Obj3D obj, Obj3D defaultFeroObj, Obj3D casing, float minimalLoadToHum) {
        this(name, TransformerElement.class, obj, defaultFeroObj, casing, minimalLoadToHum);
    }

    protected TransformerDescriptor(String name, Class<? extends TransformerElement> element, Obj3D obj, Obj3D defaultFeroObj,
                                    Obj3D casing, float minimalLoadToHum) {
        super(name, element, null);
        this.minimalLoadToHum = minimalLoadToHum;

        if (obj != null) {
            main = obj.getPart("main");
            sbire = obj.getPart("sbire");
        }
        if (defaultFeroObj != null) {
            defaultFero = defaultFeroObj.getPart("fero");
        }
        if (casing != null) {
            this.casing = casing.getPart("Case");
            casingDoorR = casing.getPart("DoorR");
            casingDoorL = casing.getPart("DoorL");
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    private Obj3DPart main;
    private Obj3DPart defaultFero;
    private Obj3DPart sbire;
    private Obj3DPart casing;
    private Obj3DPart casingDoorR;
    private Obj3DPart casingDoorL;

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Transforms an input voltage to\nan output voltage.").split("\n"));
        Collections.addAll(list, tr("The voltage ratio is proportional\nto the cable stacks count ratio.").split("\n"));
        Collections.addAll(list, tr("Each cable is a turn, up to 64 a side.\nThe core takes so many volts a turn:\npast that it saturates, heats and burns.").split("\n"));
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(defaultFero, 1, 4, false, 0f);

    }

    void draw(Obj3DPart fero, int priCableNbr, int secCableNbr, boolean hasCasing, float doorOpen) {
        // (a winding model per cable, as in 1.12, but at most four a side: a 64-turn winding would stand 8 blocks tall)
        priCableNbr = Math.min(priCableNbr, 4);
        secCableNbr = Math.min(secCableNbr, 4);
        if (main != null) main.draw();
        if (fero != null) {
            fero.draw();
            if (priCableNbr != 0) {
                GL11.glPushMatrix();
                float y = (priCableNbr - 1) * 1f / 16f;
                GL11.glTranslatef(0f, -y, 0f);
                for (int idx = 0; idx < priCableNbr; idx++) {
                    sbire.draw();
                    GL11.glTranslatef(0f, 2f / 16f, 0f);
                }
                GL11.glPopMatrix();
            }
            if (secCableNbr != 0) {
                GL11.glPushMatrix();
                GL11.glRotatef(180, 0f, 1f, 0f);
                float y = (secCableNbr - 1) * 1f / 16f;
                GL11.glTranslatef(0f, -y, 0f);
                for (int idx = 0; idx < secCableNbr; idx++) {
                    sbire.draw();
                    GL11.glTranslatef(0f, 2f / 16f, 0f);
                }
                GL11.glPopMatrix();
            }

        }

        if (hasCasing) {
            if (casing != null) casing.draw();
            if (casingDoorR != null) casingDoorR.draw(doorOpen * 90, 0f, 1f, 0f);
            if (casingDoorL != null) casingDoorL.draw(-doorOpen * 90, 0f, 1f, 0f);
        }
    }

    /**
     * The windings as coils (Coil; Baughn 2026-11-07) on the core: each side's coil around its leg, textured with its
     * turns (WindingTexture), its wire's insulation colour (rgb), red when the window is overfull.
     */
    void drawCoils(Obj3DPart fero, Coil primary, int primaryRgb, Coil secondary, int secondaryRgb, boolean hasCasing, float doorOpen) {
        if (main != null) main.draw();
        if (fero != null) {
            fero.draw();
            if (primary != null) drawCoil(primary, primaryRgb);
            if (secondary != null) {
                GL11.glPushMatrix();
                GL11.glRotatef(180, 0f, 1f, 0f);
                drawCoil(secondary, secondaryRgb);
                GL11.glPopMatrix();
            }
        }
        if (hasCasing) {
            if (casing != null) casing.draw();
            if (casingDoorR != null) casingDoorR.draw(doorOpen * 90, 0f, 1f, 0f);
            if (casingDoorL != null) casingDoorL.draw(-doorOpen * 90, 0f, 1f, 0f);
        }
    }

    /**
     * One coil around the leg at z = -5 px (x -1..1, z -6..-4), above the board's parts (y -0.75) to the yoke (y 4.9):
     * its sides (a band a turn of a layer) and its ends (a ring a layer), each a display list under a greyscale sprite of
     * the block atlas, tinted with the wire's colour. Nothing here changes from frame to frame, so the chunk draws it
     * (Bake); other turns, layers or colour are another list, texture or tint, and the chunk section is meshed again.
     */
    public static void drawCoil(Coil c, int rgb) {
        int kind = WindingTexture.kind(rgb, c.over());
        int tint = WindingTexture.tint(rgb, c.over());
        GL11.glColor4f((tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f, 1);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glPushMatrix();
        GL11.glScalef(1 / 16f, 1 / 16f, 1 / 16f);
        GL11.bindTexture(WindingTexture.side(kind));
        GL11.glCallList(coilSidesList(c.build(), WindingTexture.bands(c.turnsPerLayer())));
        GL11.bindTexture(WindingTexture.end(c.layers(), kind));
        GL11.glCallList(coilEndsList(c.build()));
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glColor4f(1, 1, 1, 1);
    }

    /**
     * The coil's shape for a build, compiled once into display lists (texture and colour are set around the call):
     * drawn vertex by vertex every frame, its ~4000 vertices were 9 % of a player's render thread (2026-11-12).
     * The sides' list is also by its bands.
     */
    private static final java.util.Map<Long, Integer> COIL_SIDES = new java.util.HashMap<>();
    private static final java.util.Map<Float, Integer> COIL_ENDS = new java.util.HashMap<>();

    public static int coilSidesList(double build, int bands) {
        float b = (float) build;
        return COIL_SIDES.computeIfAbsent((long) Float.floatToIntBits(b) << 32 | bands, k -> {
            int list = GL11.glGenLists(1);
            GL11.glNewList(list, GL11.GL_COMPILE);
            coilSides(b, bands);
            GL11.glEndList();
            return list;
        });
    }

    public static int coilEndsList(double build) {
        return COIL_ENDS.computeIfAbsent((float) build, b -> {
            int list = GL11.glGenLists(1);
            GL11.glNewList(list, GL11.GL_COMPILE);
            coilEnds(b);
            GL11.glEndList();
            return list;
        });
    }

    /** Vertices of one coil: its sides (4 faces of bands quads) and its two ends (4 straight stretches and 4 corners of 8 x 8 cells each). */
    public static int coilVertices(int bands) {
        return 16 * bands + 2 * (16 + 4 * 64 * 4);
    }

    private static final float HX = 1.15f, HZ0 = -6.15f, HZ1 = -3.85f, Y0 = -0.75f, Y1 = 4.9f;

    /** The quads of the coil's sides in pixels (see drawCoil): each face is bands quads, one above the other, each the whole sprite. */
    public static void coilSides(float build, int bands) {
        float x1 = HX + Math.min(build, 2.8f), x0 = -x1;
        float zIn = HZ1 + build, zOut = HZ0 - Math.min(build, 1.8f);
        GL11.glBegin(GL11.GL_QUADS);
        for (int k = 0; k < bands; k++) {
            // (from the top down: a turn's light edge is its top)
            float top = Y1 + (Y0 - Y1) * k / bands, foot = k + 1 == bands ? Y0 : Y1 + (Y0 - Y1) * (k + 1) / bands;
            side(0, 0, 1, x0, zIn, x1, zIn, foot, top);
            side(0, 0, -1, x1, zOut, x0, zOut, foot, top);
            side(-1, 0, 0, x0, zOut, x0, zIn, foot, top);
            side(1, 0, 0, x1, zIn, x1, zOut, foot, top);
        }
        GL11.glEnd();
    }

    /** The quads of the coil's ends in pixels: one ring a layer round the hole, straight along the leg's sides and rounded at its corners. */
    public static void coilEnds(float build) {
        float hx = HX, hz0 = HZ0, hz1 = HZ1;
        float x1 = hx + Math.min(build, 2.8f), x0 = -x1;
        float zIn = hz1 + build, zOut = hz0 - Math.min(build, 1.8f);
        float bxs = x1 - hx, bin = zIn - hz1, bout = hz0 - zOut;
        GL11.glBegin(GL11.GL_QUADS);
        for (float y : new float[]{Y1, Y0}) {   // (and the underside: it stands clear of the board's parts)
            GL11.glNormal3f(0, y == Y1 ? 1 : -1, 0);
            ring(y, -hx, hx, hz1, zIn, 0, 0, hz1, 1, 0, bin);         // along the leg's window side
            ring(y, -hx, hx, zOut, hz0, 0, 0, hz0, -1, 0, bout);    // its outer side
            ring(y, x0, -hx, hz0, hz1, -hx, -1, 0, 0, bxs, 0);       // its flanks
            ring(y, hx, x1, hz0, hz1, hx, 1, 0, 0, bxs, 0);
            corner(y, hx, hz1, 1, 1, bxs, bin);
            corner(y, -hx, hz1, -1, 1, bxs, bin);
            corner(y, hx, hz0, 1, -1, bxs, bout);
            corner(y, -hx, hz0, -1, -1, bxs, bout);
        }
        GL11.glEnd();
    }

    /** One band of a side from a to b: the whole "one turn" sprite (it is the same across; v a hair inside its edge rows). */
    static void side(float nx, float ny, float nz, float ax, float az, float bx, float bz, float y0, float y1) {
        float u0 = 0.1f, u1 = 0.9f, v0 = 1 / 512f, v1 = 511 / 512f;
        GL11.glNormal3f(nx, ny, nz);
        GL11.glTexCoord2f(u0, v0);
        GL11.glVertex3f(ax, y1, az);
        GL11.glTexCoord2f(u0, v1);
        GL11.glVertex3f(ax, y0, az);
        GL11.glTexCoord2f(u1, v1);
        GL11.glVertex3f(bx, y0, bz);
        GL11.glTexCoord2f(u1, v0);
        GL11.glVertex3f(bx, y1, bz);
    }

    /**
     * A straight stretch of an end over [xa, xb] x [za, zb] at y: u (the layer) runs from 0 at the hole (x = ox, or
     * z = oz) to 1 at the coil's edge, sx (or sz) the direction out and bx (or bz) the build that way.
     */
    static void ring(float y, float xa, float xb, float za, float zb, float ox, float sx, float oz, float sz, float bx, float bz) {
        float[][] p = {{xa, za}, {xa, zb}, {xb, zb}, {xb, za}};
        for (float[] q : p) {
            float d = sx != 0 ? (q[0] - ox) * sx / Math.max(1e-3f, bx) : (q[1] - oz) * sz / Math.max(1e-3f, bz);
            GL11.glTexCoord2f(layerU(d), 0.5f);
            GL11.glVertex3f(q[0], y, q[1]);
        }
    }

    /** A corner of an end, out from the hole's corner (cx, cz) towards (sx, sz): rings rounded (cells of a grid, u by radius). */
    static void corner(float y, float cx, float cz, float sx, float sz, float bx, float bz) {
        int n = 8;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                float[][] c = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
                for (float[] q : c) {
                    float fx = q[0] / n, fz = q[1] / n;
                    GL11.glTexCoord2f(layerU((float) Math.sqrt(fx * fx + fz * fz)), 0.5f);
                    GL11.glVertex3f(cx + sx * fx * bx, y, cz + sz * fz * bz);
                }
            }
    }

    /** The end texture's u for a distance out (0 at the hole, 1 at the coil's edge). */
    static float layerU(float d) {
        return Math.max(0.02f, Math.min(0.98f, d));
    }
}
