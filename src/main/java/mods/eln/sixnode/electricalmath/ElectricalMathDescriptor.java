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
    Obj3DPart main;
    Obj3DPart led[] = new Obj3DPart[8];
    /** The logger's screen on top (block units, x up from the face): where the graph is drawn. */
    float screenX, screenY0, screenY1, screenZ0, screenZ1;

    static final boolean[] ledDefault = {true, false, true, false, true, true, true, false};

    public ElectricalMathDescriptor(String name, Obj3D obj) {
        super(name, ElectricalMathElement.class, null);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
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

            pinDistance = Utils.getSixNodePinDistance(main);
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }
    /** The model, its lamps, and the logger's graph on its screen (log null: none, as for the item). */
    void draw(boolean ledOn[], mods.eln.sixnode.electricaldatalogger.DataLogs log) {
        if (main != null) main.draw();
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
        if (log != null && log.size() >= 2 && screenY1 > screenY0) drawGraph(log);
    }

    /** Channel colours on the tiny screen: A red, B blue, C green, out white (as in the editor). */
    static final float[][] INK = {{1f, 0.35f, 0.35f}, {0.35f, 0.6f, 1f}, {0.35f, 0.85f, 0.35f}, {0.95f, 0.95f, 0.95f}};

    /**
     * Each channel's mean as a thin strip across the screen, time along z (newest at z1), the signal along y (0..1 of
     * the screen's height, clipped), 0.1 px above the glass. It shows the shape, not numbers: the screen is a few pixels.
     */
    void drawGraph(mods.eln.sixnode.electricaldatalogger.DataLogs log) {
        float x = screenX + 0.1f / 16, h = screenY1 - screenY0, w = screenZ1 - screenZ0, t = 0.12f / 16;
        int n = log.size();
        UtilsClient.disableTexture();
        UtilsClient.disableLight();
        UtilsClient.disableCulling();
        for (int c = 0; c < log.channelCount() && c < INK.length; c++) {
            GL11.glColor3f(INK[c][0], INK[c][1], INK[c][2]);
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int age = 0; age < n; age++) {
                float m = log.mean(c, age);
                if (Float.isNaN(m)) continue;
                float z = screenZ1 - w * age / (n - 1), y = screenY0 + h * Math.max(0, Math.min(1, m));
                GL11.glVertex3f(x, y - t, z);
                GL11.glVertex3f(x, y + t, z);
            }
            GL11.glEnd();
        }
        UtilsClient.enableCulling();
        UtilsClient.enableLight();
        UtilsClient.enableTexture();
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
        draw(ledDefault, null);

    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Calculates an output signal from\n3 inputs (A, B, C) using an equation.").split("\n"));
    }

    // 26.x port: top() removed (client rendering; redone in P3)
    // 26.x port: bottom() removed (client rendering; redone in P3)
}
