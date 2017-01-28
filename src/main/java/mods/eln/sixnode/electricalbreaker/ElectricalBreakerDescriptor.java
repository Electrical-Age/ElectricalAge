package mods.eln.sixnode.electricalbreaker;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalBreakerDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    private Obj3DPart main;
    private Obj3DPart lever;
    private Obj3DPart led;

    float alphaOff, alphaOn, speed;

    public ElectricalBreakerDescriptor(String name, Obj3D obj) {
        super(name, ElectricalBreakerElement.class, ElectricalBreakerRender.class);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("case");
            lever = obj.getPart("lever");

            if (lever != null) {
                speed = lever.getFloat("speed");
                alphaOff = lever.getFloat("alphaOff");
                alphaOn = lever.getFloat("alphaOn");
            }
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    @Override
    public boolean use2DIcon() {
        return true;
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
        if (type == ItemRenderType.INVENTORY) /*GL11.glScalef(1.8f, 1.8f, 1.8f);*/ {
            super.renderItem(type, item, data);
        } else
            draw(0f, 0f);
    }

    public void draw(float on, float distance) {
        if (main != null) main.draw();
        if (lever != null) {
            lever.draw(on * (alphaOn - alphaOff) + alphaOff, 0, 1, 0);
        }
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, (tr("Protects electrical components\nOpens contact if:\n  - Voltage exceeds a certain level\n- Current exceeds the cable limit").split("\n")));
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
