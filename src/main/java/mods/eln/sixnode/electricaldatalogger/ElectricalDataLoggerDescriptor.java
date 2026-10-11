package mods.eln.sixnode.electricaldatalogger;


import mods.eln.gl.GL11;


import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalDataLoggerDescriptor extends SixNodeDescriptor {

    Obj3D obj;
    Obj3DPart main, led, reflection;
    float sx, sy, sz;
    float tx, ty, tz;
    float rx, ry, rz, ra;
    float mx, my;

    float cr, cg, cb;

    float reflc;

    public boolean onFloor;
    public String textColor;

    public ElectricalDataLoggerDescriptor(String name, boolean onFloor, String objName, float cr, float cg, float cb, String textColor) {
        super(name, ElectricalDataLoggerElement.class, null);
        this.cb = cb;
        this.cr = cr;
        this.cg = cg;
        this.onFloor = onFloor;
        this.textColor = textColor;
        obj = Eln.obj.getObj(objName);
        if (obj != null) {
            main = obj.getPart("main");
            reflection = obj.getPart("reflection");
            if (main != null) {
                sx = main.getFloat("sx");
                sy = main.getFloat("sy");
                sz = main.getFloat("sz");
                tx = main.getFloat("tx");
                ty = main.getFloat("ty");
                tz = main.getFloat("tz");
                rx = main.getFloat("rx");
                ry = main.getFloat("ry");
                rz = main.getFloat("rz");
                ra = main.getFloat("ra");
                mx = main.getFloat("mx");
                my = main.getFloat("my");
                reflc = main.getFloat("reflc");
                led = obj.getPart("led");
            }
        }

        if (onFloor) {
            setPlaceDirection(Direction.YN);
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }
    void draw(DataLogs log, Direction side, LRDU front, int objPosMX, int objPosMZ) {
        if (onFloor || side.isY()) front.glRotateOnX();
        if (!onFloor && side.isNotY()) GL11.glRotatef(90, 1, 0, 0);
        //GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (main != null) main.draw();
        //GL11.glEnable(GL11.GL_TEXTURE_2D);

        //Glass (reflections)
        UtilsClient.enableBlend();
        obj.bindTexture("Reflection.png");
        float rotYaw = UtilsClient.clientPlayerYaw() / 360.f;
        float rotPitch = UtilsClient.clientPlayerPitch() / 180.f;
        float pos = (((float) UtilsClient.clientPlayerX()) - ((float) (objPosMX * 2)) + ((float) UtilsClient.clientPlayerZ()) - ((float) (objPosMZ * 2))) / 24.f;
        GL11.glColor4f(1, 1, 1, reflc);
        reflection.draw(rotYaw + pos, rotPitch * 0.857f);
        UtilsClient.disableBlend();

        //Plot
        if (log != null) {
            UtilsClient.disableLight();
            // GL11.glPushMatrix();
            UtilsClient.ledOnOffColor(true);
            if (led != null) led.draw();

            UtilsClient.glDefaultColor();

            GL11.glTranslatef(tx, ty, tz);
            GL11.glRotatef(ra, rx, ry, rz);
            GL11.glScalef(sx, sy, sz);
            GL11.glColor4f(cr, cg, cb, 1);
            log.draw(mx, my, textColor);

            UtilsClient.glDefaultColor();

            UtilsClient.enableLight();
        }
    }
    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    @Override
    public boolean hasVolume() {
        return onFloor;
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        if (main != null) main.draw();

    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Measures the voltage of an\nelectrical signal and plots\nthe data in real time.").split("\n"));
        list.add(tr("It can store up to 256 points."));
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        LRDU front = super.getFrontFromPlace(side, player);
        if (onFloor) {
            return front.inverse();
        } else {
            return front;
        }
    }
}
