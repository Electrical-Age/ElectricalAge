package mods.eln.transparentnode.eggincubator;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class EggIncubatorDescriptor extends TransparentNodeDescriptor {

    Obj3D obj;
    Obj3D defaultFeroObj;
    public ElectricalCableDescriptor cable;
    private Obj3DPart lamp;
    private ItemEntity eggEntity;
    private Obj3DPart lampf;

    Obj3DPart main;

    double nominalVoltage, nominalPower;
    double Rp;

    public EggIncubatorDescriptor(String name,
                                  Obj3D obj,
                                  ElectricalCableDescriptor cable,
                                  double nominalVoltage, double nominalPower) {
        super(name, EggIncubatorElement.class, null);
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

        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addMachine(newItemStack());
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)
// 26.x port: draw() removed (client rendering; redone in P3)

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
    public void addCollisionBoxesToList(AABB par5AxisAlignedBB, List list, Level world, int x, int y, int z) {
        // 1.7.10: stone.getCollisionBoundingBoxFromPool (absolute), then maxY -= 0.5 (AABB is immutable now)
        AABB bb = new AABB(x, y, z, x + 1, y + 1, z + 1);
        bb = bb.setMaxY(bb.maxY - 0.5);
        if (par5AxisAlignedBB.intersects(bb)) list.add(bb);
    }
}
