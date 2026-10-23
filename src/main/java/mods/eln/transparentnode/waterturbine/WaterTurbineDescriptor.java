package mods.eln.transparentnode.waterturbine;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

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
        super(name, WaterTurbineElement.class, null);

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

    Coordonate waterCoord;

    public void setParent(net.minecraft.world.item.Item item, int damage) {
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
                                       LivingEntity entityLiving) {
        return super.getFrontFromPlace(side, entityLiving);
    }


    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        objItemScale(obj);
        Direction.ZN.glRotateXnRef();
        draw(0f);

    }


    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer,
                               List list, boolean par4) {

        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(tr("Generates energy using water stream."));
        list.add(tr("Voltage: %1$V", Utils.plotValue(cable.electricalNominalVoltage)));
        list.add(tr("Power: %1$W", Utils.plotValue(nominalPower)));
    }


    public Coordonate getWaterCoordonate(Level w) {
        Coordonate coord = new Coordonate(waterCoord);
        coord.setDimention(mods.eln.misc.Dimensions.id(w));
        return coord;
    }


    @Override
    public String checkCanPlace(Coordonate coord, Direction front) {

        String str = super.checkCanPlace(coord, front);
        if (str != null) return str;
        if (checkCanPlaceWater(coord, front) == false) return tr("No place for water turbine!");
        return str;
    }


    public boolean checkCanPlaceWater(Coordonate coord, Direction front) {
        Coordonate water = new Coordonate(waterCoord);
        water.applyTransformation(front, coord);
        if (coord.getBlockExist() == false) return true;
        if (Utils.isAirBlock(water.getBlock()) || Utils.isWater(water)) return true;
        return false;
    }
}

