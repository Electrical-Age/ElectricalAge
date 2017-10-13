package mods.eln.sixnode.batterycharger;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class BatteryChargerDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    Obj3DPart main;

    public double nominalVoltage;
    public double nominalPower;
    public ElectricalCableDescriptor cable;
    double Rp;
    public float[] pinDistance;

    Obj3DPart[] leds = new Obj3DPart[4];

    public BatteryChargerDescriptor(String name,
                                    Obj3D obj,
                                    ElectricalCableDescriptor cable,
                                    double nominalVoltage, double nominalPower) {
        super(name, BatteryChargerElement.class, BatteryChargerRender.class);

        this.nominalVoltage = nominalVoltage;
        this.nominalPower = nominalPower;
        this.Rp = nominalVoltage * nominalVoltage / nominalPower;
        this.obj = obj;
        this.cable = cable;

        if (obj != null) {
            main = obj.getPart("main");
            for (int idx = 0; idx < 4; idx++) {
                leds[idx] = obj.getPart("led" + idx);
            }
            pinDistance = Utils.getSixNodePinDistance(main);
        }

        setDefaultIcon("batterycharger");
        voltageLevelColor = VoltageLevelColor.fromVoltage(nominalVoltage);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
        Data.addUtilities(newItemStack());
    }

    public void draw(boolean[] presence, boolean[] charged) {
        if (main != null)
            main.draw();

        int idx = 0;
        for (Obj3DPart led : leds) {
            if (presence != null && presence[idx]) {
                UtilsClient.ledOnOffColor(charged[idx]);
                UtilsClient.drawLight(led);
            } else {
                GL11.glColor3f(0.2f, 0.2f, 0.2f);
                led.draw();
            }
            idx++;
        }
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    //boolean[] defaultCharged = new boolean[]{true, true, true, true};

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        }
        draw(null, null);
    }

    public void applyTo(NbtElectricalLoad powerLoad) {
        cable.applyTo(powerLoad);
    }

    public void setRp(Resistor powerload, boolean powerOn) {
        if (!powerOn)
            powerload.highImpedance();
        else
            powerload.setR(Rp);
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Can be used to recharge\nelectrical items like:\nFlash Light, X-Ray scanner\nand Portable Battery ...").split("\\\n"));
        list.add(tr("Nominal power: %1$W", Utils.plotValue(nominalPower)));
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
