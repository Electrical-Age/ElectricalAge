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
    Obj3DPart main, door;
    Obj3DPart led[] = new Obj3DPart[8];

    float alphaOff;

    static final boolean[] ledDefault = {true, false, true, false, true, true, true, false};

    public ElectricalMathDescriptor(String name, Obj3D obj) {
        super(name, ElectricalMathElement.class, null);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            door = obj.getPart("door");
            if (door != null) {
                alphaOff = door.getFloat("alphaOff");
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
    void draw(float open, boolean ledOn[]) {
        if (main != null) main.draw();
        if (door != null) door.draw((1f - open) * alphaOff, 0f, 1f, 0f);

        for (int idx = 0; idx < 8; idx++) {
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
        draw(0.7f, ledDefault);

    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Calculates an output signal from\n3 inputs (A, B, C) using an equation.").split("\n"));
    }

    // 26.x port: top() removed (client rendering; redone in P3)
    // 26.x port: bottom() removed (client rendering; redone in P3)
}
