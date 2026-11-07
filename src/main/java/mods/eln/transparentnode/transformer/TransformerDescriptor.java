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
     * turns (WindingTexture), its wire's insulation colour (ARGB rgb), red when the window is overfull.
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

    /** One coil around the leg at z = -5 px (x -1..1, z -6..-4), from the board (y -2.45) to the yoke (y 4.9). */
    static void drawCoil(Coil c, int rgb) {
        net.minecraft.resources.Identifier tex = WindingTexture.of(c.turnsPerLayer(), c.layers(), rgb, c.over());
        float hx = 1.15f, hz0 = -6.15f, hz1 = -3.85f, y0 = -2.45f, y1 = 4.9f;
        float x1 = hx + (float) Math.min(c.build(), 2.8), x0 = -x1;
        float zIn = hz1 + (float) c.build(), zOut = hz0 - (float) Math.min(c.build(), 1.8);
        if (tex != null) {
            GL11.bindTexture(tex);
            GL11.glColor4f(1, 1, 1, 1);
        } else {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, 1);
        }
        mods.eln.misc.UtilsClient.disableCulling();
        GL11.glPushMatrix();
        GL11.glScalef(1 / 16f, 1 / 16f, 1 / 16f);
        GL11.glBegin(GL11.GL_QUADS);
        // the sides: stripes run round the coil (u across, v down)
        side(0, 0, 1, x0, zIn, x1, zIn, y0, y1);
        side(0, 0, -1, x1, zOut, x0, zOut, y0, y1);
        side(-1, 0, 0, x0, zOut, x0, zIn, y0, y1);
        side(1, 0, 0, x1, zIn, x1, zOut, y0, y1);
        // the top: one ring a layer, out from the hole round the leg
        GL11.glNormal3f(0, 1, 0);
        top(x0, hz1, x1, zIn, true, 'z');
        top(x0, zOut, x1, hz0, false, 'z');
        top(x0, hz0, -hx, hz1, false, 'x');
        top(hx, hz0, x1, hz1, true, 'x');
        GL11.glEnd();
        GL11.glPopMatrix();
        mods.eln.misc.UtilsClient.enableCulling();
        if (tex == null) GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
    }

    static void side(float nx, float ny, float nz, float ax, float az, float bx, float bz, float y0, float y1) {
        GL11.glNormal3f(nx, ny, nz);
        GL11.glTexCoord2f(0.02f, 0);
        GL11.glVertex3f(ax, y1, az);
        GL11.glTexCoord2f(0.02f, 1);
        GL11.glVertex3f(ax, y0, az);
        GL11.glTexCoord2f(0.48f, 1);
        GL11.glVertex3f(bx, y0, bz);
        GL11.glTexCoord2f(0.48f, 0);
        GL11.glVertex3f(bx, y1, bz);
    }

    /** A top quad over [xa, xb] x [za, zb] at y 4.9; u runs 0.5 at the hole to 1 at the coil's edge, along axis. */
    static void top(float xa, float za, float xb, float zb, boolean outAtHigh, char axis) {
        float y = 4.9f;
        float[][] p = {{xa, za}, {xa, zb}, {xb, zb}, {xb, za}};
        for (float[] q : p) {
            float along = axis == 'z' ? (q[1] - za) / (zb - za) : (q[0] - xa) / (xb - xa);
            float u = 0.5f + 0.5f * (outAtHigh ? along : 1 - along);
            GL11.glTexCoord2f(Math.max(0.51f, Math.min(0.99f, u)), 0.5f);
            GL11.glVertex3f(q[0], y, q[1]);
        }
    }
}
