package mods.eln.transparentnode.waterturbine;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class WaterTurbineDescriptor extends TransparentNodeDescriptor {
    public WaterTurbineDescriptor(
        String name, Obj3D obj,
        ElectricalCableDescriptor cable,
        double nominalPower,
        double maxVoltage,
        Coordinate waterCoord,
        String soundName,
        float nominalVolume
    ) {
        super(name, WaterTurbineElement.class, WaterTurbineRender.class);

        this.cable = cable;
        this.nominalPower = nominalPower;
        this.maxVoltage = maxVoltage;
        this.waterCoord = waterCoord;
        this.soundName = soundName;
        this.nominalVolume = nominalVolume;

        this.obj = obj;
        if (obj != null) {
            wheel = obj.getPart("Wheel");
            support = obj.getPart("Support");
            generator = obj.getPart("Generator");
            speed = 60;
        }

        voltageLevelColor = VoltageLevelColor.LowVoltage;
    }

    Coordinate waterCoord;

    public void setParent(net.minecraft.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }


    Obj3DPart wheel, support, generator;

    Obj3D obj;
    public ElectricalCableDescriptor cable;
    public double nominalPower;


    public double maxVoltage;

    public float speed;

    public String soundName;
    public float nominalVolume;


    public FunctionTable PfW;

    public void draw(float alpha) {
        if (support != null) support.draw();
        if (generator != null) generator.draw();
        if (wheel != null) wheel.draw(alpha, 1f, 0f, 0f);
    }

    @Override
    public Direction getFrontFromPlace(Direction side,
                                       EntityLivingBase entityLiving) {
        return super.getFrontFromPlace(side, entityLiving);
    }


    // TODO(1.10): Fix item render.
//    @Override
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//
//        return true;
//    }
//
//    @Override
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
//                                         ItemRendererHelper helper) {
//
//        return type != ItemRenderType.INVENTORY;
//    }
//
//    @Override
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        if (type == ItemRenderType.INVENTORY) {
//            super.renderItem(type, item, data);
//        } else {
//            objItemScale(obj);
//            Direction.ZN.glRotateXnRef();
//            draw(0f);
//        }
//    }


    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
                               List list, boolean par4) {

        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(tr("Generates energy using water stream."));
        list.add(tr("Voltage: %sV", Utils.plotValue(cable.electricalNominalVoltage)));
        list.add(tr("Power: %sW", Utils.plotValue(nominalPower)));
    }


    public Coordinate getWaterCoordinate(World w) {
        Coordinate coord = new Coordinate(waterCoord);
        coord.setDimension(w.provider.getDimension());
        return coord;
    }


    @Override
    public String checkCanPlace(Coordinate coord, Direction front) {

        String str = super.checkCanPlace(coord, front);
        if (str != null) return str;
        if (checkCanPlaceWater(coord, front) == false) return tr("No place for water turbine!");
        return str;
    }


    public boolean checkCanPlaceWater(Coordinate coord, Direction front) {
        Coordinate water = new Coordinate(waterCoord);
        water.applyTransformation(front, coord);
        if (!coord.doesBlockExist()) return true;
        if (water.world().isAirBlock(water.pos)  || Utils.isWateryEnoughForTurbine(water)) return true;
        return false;
    }
}

