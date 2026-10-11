package mods.eln.sixnode.electricalvumeter;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;

import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.awt.Color;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalVuMeterDescriptor extends SixNodeDescriptor {

    Obj3D obj;

    enum ObjType {Rot, LedOnOff}

    ObjType objType;

    Obj3DPart vumeter, pointer, led, halo, main;

    public boolean onOffOnly;

    public float[] pinDistance;

    public ElectricalVuMeterDescriptor(String name, String objName, boolean onOffOnly) {
        super(name, ElectricalVuMeterElement.class, null);
        this.onOffOnly = onOffOnly;
        obj = Eln.obj.getObj(objName);

        if (obj != null) {
            if (obj.getString("type").toLowerCase().equals("rot")) {
                objType = ObjType.Rot;
                vumeter = obj.getPart("Vumeter");
                pointer = obj.getPart("Pointer");
                pinDistance = Utils.getSixNodePinDistance(vumeter);
            }
            if (obj.getString("type").equals("LedOnOff")) {
                objType = ObjType.LedOnOff;
                main = obj.getPart("main");
                halo = obj.getPart("halo");
                led = obj.getPart("Led");
                pinDistance = Utils.getSixNodePinDistance(main);
            }
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }
    void draw(float factor, float distance, BlockEntity entity) {
        if (factor < 0.0) factor = 0.0f;
        if (factor > 1.0) factor = 1.0f;
        switch (objType) {
            case LedOnOff:
                main.draw();
                boolean s = factor > 0.5;
                Color c = UtilsClient.ledOnOffColorC(s);
                GL11.glColor3f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
                UtilsClient.drawLight(led);
                //Utils.enableBilinear();
                if (entity != null)
                    UtilsClient.drawHalo(halo, c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, entity, false);
                else
                    UtilsClient.drawLight(halo);
                //Utils.disableBilinear();
                break;
            case Rot:
                vumeter.draw();
                float alphaOff, alphaOn;
                alphaOff = pointer.getFloat("alphaOff");
                alphaOn = pointer.getFloat("alphaOn");
                pointer.draw((factor * (alphaOn - alphaOff) + alphaOff), 1.0f, 0, 0);
                break;
            default:
                break;
        }
    }
    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Displays the value of a signal."));
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
        draw(0.0f, 1f, null);

    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
