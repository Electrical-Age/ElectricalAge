package mods.eln.sixnode.electricallightsensor;

import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalLightSensorDescriptor extends SixNodeDescriptor {

    private Obj3DPart main;
    public boolean dayLightOnly;
    public float[] pinDistance;

    Obj3D obj;

    public ElectricalLightSensorDescriptor(String name, Obj3D obj, boolean dayLightOnly) {
        super(name, ElectricalLightSensorElement.class, ElectricalLightSensorRender.class);
        this.obj = obj;
        this.dayLightOnly = dayLightOnly;

        if (obj != null) {
            main = obj.getPart("main");
            pinDistance = Utils.getSixNodePinDistance(main);
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    void draw() {
        if (main != null) main.draw();
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    @Override
    public boolean use2DIcon() {
        return true;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        if (dayLightOnly) {
            Collections.addAll(list, tr("Provides an electrical voltage\nwhich is proportional to\nthe intensity of daylight.").split("\n"));
            list.add(tr("0V at night, %1$V at noon.", Eln.SVU));
        } else {
            Collections.addAll(list, tr("Provides an electrical voltage\nin the presence of light.").split("\n"));
        }
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        } else {
            GL11.glScalef(2f, 2f, 2f);
            draw();
        }
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).right();
    }
}
