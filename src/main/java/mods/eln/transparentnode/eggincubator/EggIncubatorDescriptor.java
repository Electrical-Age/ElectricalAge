package mods.eln.transparentnode.eggincubator;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class EggIncubatorDescriptor extends TransparentNodeDescriptor {

    Obj3D obj;
    Obj3D defaultFeroObj;
    public ElectricalCableDescriptor cable;
    private Obj3DPart lamp;
    private EntityItem eggEntity;
    private Obj3DPart lampf;

    Obj3DPart main;

    double nominalVoltage, nominalPower;
    double Rp;

    public EggIncubatorDescriptor(String name,
                                  Obj3D obj,
                                  ElectricalCableDescriptor cable,
                                  double nominalVoltage, double nominalPower) {
        super(name, EggIncubatorElement.class, EggIncubatorRender.class);
        this.obj = obj;
        this.cable = cable;
        this.nominalVoltage = nominalVoltage;
        this.nominalPower = nominalPower;
        Rp = nominalVoltage * nominalVoltage / nominalPower;

        if (obj != null) {
            main = obj.getPart("main");
            lamp = obj.getPart("lamp");
            lampf = obj.getPart("lampf");
        }

        voltageLevelColor = VoltageLevelColor.fromCable(cable);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addMachine(newItemStack());
    }

    // TODO(1.10): Fix item render.
//    @Override
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
//        return type != ItemRenderType.INVENTORY;
//    }
//
//    @Override
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//        return true;
//    }
//
//    @Override
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        if (type == ItemRenderType.INVENTORY) {
//            super.renderItem(type, item, data);
//        } else {
//            draw(0, 1f);
//        }
//    }

    void draw(int eggStackSize, float powerFactor) {
        if (eggStackSize == 0) powerFactor = 0f;
        UtilsClient.disableCulling();
        if (main != null) main.draw();
        if (lampf != null) {
            GL11.glColor3f(0.1f, 0.1f, 0.1f);
            lampf.draw();
        }
        if (lamp != null) {
            UtilsClient.disableLight();
            UtilsClient.enableBlend();
            GL11.glColor4f(1f, 0.2f, 0.0f, powerFactor * powerFactor * 0.5f);
            lamp.draw();
            UtilsClient.disableBlend();
            UtilsClient.enableLight();
        }
        UtilsClient.enableCulling();
    }

    public void applyTo(NbtElectricalLoad powerLoad) {
        cable.applyTo(powerLoad);
    }

    public void setState(Resistor powerLoad, boolean enable) {
        if (enable)
            powerLoad.setR(Rp);
        else
            powerLoad.setR(MnaConst.highImpedance);
    }

    @Override
    public void addCollisionBoxesToList(AxisAlignedBB par5AxisAlignedBB, List list, BlockPos pos) {
        AxisAlignedBB bb = new AxisAlignedBB(pos);
        bb = bb.setMaxY(0.5);
        if (par5AxisAlignedBB.intersectsWith(bb)) list.add(bb);
    }
}
