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

public class WaterTurbineDescriptor extends TransparentNodeDescriptor {


    public WaterTurbineDescriptor(
            String name, Obj3D obj,
            ElectricalCableDescriptor cable,
            double nominalPower,
            double maxVoltage,
            Coordonate waterCoord,
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
    }

    Coordonate waterCoord;

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
    public boolean use2DIcon() {
        return false;
    }

    @Override
    public Direction getFrontFromPlace(Direction side,
                                       EntityLivingBase entityLiving) {
        return super.getFrontFromPlace(side, entityLiving);
    }


    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {

        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
                                         ItemRendererHelper helper) {

        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        objItemScale(obj);
        Direction.ZN.glRotateXnRef();
        draw(0f);
    }


    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
                               List list, boolean par4) {

        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add("Produces power from water.");
        list.add(Utils.plotVolt("Voltage:", cable.electricalNominalVoltage));
        list.add(Utils.plotPower("Power:", nominalPower));

    }

    public Coordonate getWaterCoordonate(World w) {
        Coordonate coord = new Coordonate(waterCoord);
        coord.setDimention(w.provider.dimensionId);
        return coord;
    }


    @Override
    public String checkCanPlace(Coordonate coord, Direction front) {

        String str = super.checkCanPlace(coord, front);
        if (str != null) return str;
        if (checkCanPlaceWater(coord, front) == false) return "No place for water";
        return str;
    }


    public boolean checkCanPlaceWater(Coordonate coord, Direction front) {
        Coordonate water = new Coordonate(waterCoord);
        water.applyTransformation(front, coord);
        if (coord.getBlockExist() == false) return true;
        if (water.getBlock() == Blocks.air || Utils.isWater(water)) return true;
        return false;
    }
}

