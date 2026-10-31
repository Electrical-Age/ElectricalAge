package mods.eln.sixnode.electricalmath;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;


import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalMathDescriptor extends SixNodeDescriptor {

    public float[] pinDistance;

    Obj3D obj;
    Obj3DPart main, terminals;
    Obj3DPart led[] = new Obj3DPart[8];
    /** The modules on the DIN rails, one per cheap chip's worth inserted (Baughn 2026-10-30), up to MODULES. */
    public static final int MODULES = 18;
    Obj3DPart module[] = new Obj3DPart[MODULES];
    /** The logger's screen on top (block units, x up from the face): where the graph is drawn. */
    float screenX, screenY0, screenY1, screenZ0, screenZ1;

    static final boolean[] ledDefault = {true, false, true, false, true, true, true, false};

    public ElectricalMathDescriptor(String name, Obj3D obj) {
        super(name, ElectricalMathElement.class, null);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            terminals = obj.getPart("terminals");
            if (main != null) {
                screenX = main.getFloat("screenX");
                screenY0 = main.getFloat("screenY0");
                screenY1 = main.getFloat("screenY1");
                screenZ0 = main.getFloat("screenZ0");
                screenZ1 = main.getFloat("screenZ1");
            }
            for (int idx = 0; idx < 8; idx++) {
                led[idx] = obj.getPart("led" + idx);
            }
            for (int idx = 0; idx < MODULES; idx++) module[idx] = obj.getPart("m" + idx);

            pinDistance = Utils.getSixNodePinDistance(main);
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }
    /** The model with modules on its rails, its lamps, and the logger's graph on its screen (log null: none, as for the item). */
    void draw(boolean ledOn[], mods.eln.sixnode.electricaldatalogger.DataLogs log, int modules) {
        draw(ledOn, log, modules, null);
    }

    /** ... graph: the logger's graph as a texture (LogTexture), or null: none. */
    void draw(boolean ledOn[], mods.eln.sixnode.electricaldatalogger.DataLogs log, int modules, net.minecraft.resources.Identifier graph) {
        if (main != null) main.draw();
        if (terminals != null) terminals.draw();
        for (int idx = 0; idx < Math.min(modules, MODULES); idx++) if (module[idx] != null) module[idx].draw();
        for (int idx = 0; idx < 8; idx++) {
            if (led[idx] == null) continue;
            if (ledOn[idx]) {
                if ((idx & 3) == 0)
                    GL11.glColor3f(0.8f, 0f, 0f);
                else
                    GL11.glColor3f(0f, 0.8f, 0f);
                UtilsClient.drawLight(led[idx]);
            } else {
                GL11.glColor3f(0.3f, 0.3f, 0.3f);
                led[idx].draw();
            }
        }
        GL11.glColor3f(1f, 1f, 1f);
        if (graph != null && screenY1 > screenY0) drawGraph(graph);
    }

    /** Channel colours on the tiny screen: A red, B blue, C green, out white (as in the editor). */
    static final float[][] INK = {{1f, 0.35f, 0.35f}, {0.35f, 0.6f, 1f}, {0.35f, 0.85f, 0.35f}, {0.95f, 0.95f, 0.95f}};

    /** The screen's aspect (width along z over height along y), for its texture. */
    float screenAspect() {
        return (screenZ1 - screenZ0) / Math.max(1e-3f, screenY1 - screenY0);
    }

    /**
     * The logger's graph (a texture, newest on the right as one faces the screen) on the screen, 0.1 px over the glass,
     * full bright.
     */
    void drawGraph(net.minecraft.resources.Identifier graph) {
        float x = screenX + 0.1f / 16;
        GL11.bindTexture(graph);
        GL11.setFullBright(true);
        UtilsClient.disableCulling();
        GL11.glColor4f(1, 1, 1, 1);
        // (texture top at y0, its right, the newest points, at z1: the graph reads upright, newest on the right, seen
        // from the processor's front; checked in the dev client 2026-10-31)
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex3f(x, screenY0, screenZ0);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex3f(x, screenY0, screenZ1);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex3f(x, screenY1, screenZ1);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex3f(x, screenY1, screenZ0);
        GL11.glEnd();
        UtilsClient.enableCulling();
        GL11.setFullBright(false);
        GL11.glColor3f(1f, 1f, 1f);
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        GL11.glTranslatef(-0.3f, -0.1f, 0f);
        GL11.glRotatef(90, 1, 0, 0);
        draw(ledDefault, null, 6);

    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Calculates an output signal from\n3 inputs (A, B, C) using an equation.").split("\n"));
    }

    // 26.x port: top() removed (client rendering; redone in P3)
    // 26.x port: bottom() removed (client rendering; redone in P3)

    /** Its terminals reach the block's edge (to meet the cables): not part of its box. */
    @Override
    protected java.util.Set<Obj3DPart> footprintIgnores() {
        return terminals == null ? java.util.Set.of() : java.util.Set.of(terminals);
    }
}
