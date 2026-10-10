package mods.eln.transparentnode.battery;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;

import mods.eln.Eln;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.*;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class BatteryDescriptor extends TransparentNodeDescriptor {

    public double electricalU, electricalDischargeRate;
    public double electricalStdP, electricalStdDischargeTime, electricalStdHalfLife, electricalStdEfficiency, electricalPMax;

    public double electricalStdEnergy, electricalStdI;

    public double thermalHeatTime, thermalWarmLimit, thermalCoolLimit;

    public double electricalQ, electricalRs, electricalRp;
    public double thermalC, thermalRp, thermalPMax;
    public double lifeNominalCurrent, lifeNominalLost;
    public double startCharge;
    public boolean isRechargable;
    String description = "todo battery";

    FunctionTable UfCharge;
    String modelName;
    Obj3DPart modelPart;
    public double IMax;
    public boolean lifeEnable;
    private ElectricalCableDescriptor cable;

    Obj3D obj;

    Obj3DPart main, plugPlus, plusMinus, battery;

    int renderType;
    private String renderSpec;

    public static final BatteryDescriptor[] list = new BatteryDescriptor[8];

    public double currentDropVoltage = 1000000, currentDropFactor = 0;
// 26.x port: draw() removed (client rendering; redone in P3)

    public BatteryDescriptor(String name, String modelName,
                             ElectricalCableDescriptor cable,
                             double startCharge, boolean isRechargable, boolean lifeEnable,
                             FunctionTable UfCharge,
                             double electricalU, double electricalPMax, double electricalDischargeRate,
                             double electricalStdP, double electricalStdDischargeTime, double electricalStdEfficiency, double electricalStdHalfLife,
                             double thermalHeatTime, double thermalWarmLimit, double thermalCoolLimit,
                             String description) {
        super(name, BatteryElement.class, null);
        this.electricalU = electricalU;
        this.electricalDischargeRate = electricalDischargeRate;
        this.electricalStdEfficiency = electricalStdEfficiency;
        this.electricalStdP = electricalStdP;
        this.electricalStdHalfLife = electricalStdHalfLife;
        this.electricalStdDischargeTime = electricalStdDischargeTime;
        this.startCharge = startCharge;
        this.isRechargable = isRechargable;
        this.lifeEnable = lifeEnable;
        this.cable = cable;

        this.thermalHeatTime = thermalHeatTime;
        this.thermalWarmLimit = thermalWarmLimit;
        this.thermalCoolLimit = thermalCoolLimit;
        this.electricalPMax = electricalPMax;

        this.UfCharge = UfCharge;
        this.description = description;

        electricalStdI = electricalStdP / electricalU;
        electricalStdEnergy = electricalStdDischargeTime * electricalStdP;

        electricalQ = electricalStdP * electricalStdDischargeTime / electricalU;
        electricalQ = 1;
        double energy = getEnergy(1.0, 1.0);
        electricalQ *= electricalStdEnergy / energy;
        electricalRs = electricalStdP * (1 - electricalStdEfficiency) / electricalStdI / electricalStdI / 2;
        //electricalRs = cable.electricalRs;
        electricalRp = Math.min(electricalU * electricalU / electricalStdP / electricalDischargeRate, 1000000000.0);

        lifeNominalCurrent = electricalStdP / electricalU;
        lifeNominalLost = 0.5 / electricalStdHalfLife;

        thermalPMax = electricalPMax / electricalU * electricalPMax / electricalU * electricalRs * 2;
        thermalC = Math.pow(electricalPMax / electricalU, 2) * electricalRs * thermalHeatTime / thermalWarmLimit;
        thermalRp = thermalWarmLimit / thermalPMax;

        IMax = electricalStdI * 3;

        obj = Eln.obj.getObj(modelName);

        if (obj != null) {
            if (obj.getString("type").equals("A"))
                renderType = 0;
            if (obj.getString("type").equals("B"))
                renderType = 1;

            switch (renderType) {
                case 0:
                    modelPart = obj.getPart("Battery");
                case 1:
                    break;
            }
        }

        voltageLevelColor = VoltageLevelColor.fromVoltage(electricalU);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    public void applyTo(Resistor resistor) {
        resistor.setR(electricalRp);
    }

    public void applyTo(BatteryProcess battery) {
        battery.uNominal = electricalU;
        battery.QNominal = electricalQ;
        battery.voltageFunction = UfCharge;
        battery.isRechargeable = isRechargable;
        //battery.efficiency = electricalStdEfficiency;

        // Convert old battery absolute charge in Coulomb to to fraction of battery capacity if the capacity is
        // very small and the output voltage is more than a quarter of the nominal voltage.
        if (battery.Q > 1.5 && battery.getU() > (battery.uNominal / 4)) {
            battery.Q /= electricalQ;
        }
    }

    public void applyTo(ElectricalLoad load, Simulator simulator) {
        load.setRs(electricalRs);
    }

    public void applyTo(ThermalLoad load) {
        load.Rp = thermalRp;
        load.C = thermalC;
        //load.setRsByTao(2);
    }

    public void applyTo(BatterySlowProcess process) {
        process.lifeNominalCurrent = lifeNominalCurrent;
        process.lifeNominalLost = lifeNominalLost;
    }

    public static BatteryDescriptor getDescriptorFrom(ItemStack itemStack) {
        // 1.12: battery sub-items were damage values 0..7 of the group; on 26.x the item knows its descriptor
        GenericItemBlockUsingDamageDescriptor d = GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack);
        return d instanceof BatteryDescriptor ? (BatteryDescriptor) d : null;
    }

    @Override
    public CompoundTag getDefaultNBT() {
        CompoundTag nbt = new CompoundTag();

        nbt.putDouble("charge", startCharge);
        nbt.putDouble("life", 1.0);
        return nbt;
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(Utils.plotVolt(tr("Nominal voltage: "), electricalU));
        list.add(Utils.plotPower(tr("Nominal power: "), electricalStdP));
        list.add(Utils.plotEnergy(tr("Energy capacity: "), electricalStdDischargeTime * electricalStdP));
        list.add(Utils.plotOhm(tr("Internal resistance: "), electricalRs * 2));
        list.add("");
        list.add(Utils.plotPercent(tr("Actual charge: "), getChargeInTag(itemStack)));

        if (lifeEnable)
            list.add(Utils.plotPercent(tr("Life : "), getLifeInTag(itemStack)));
    }

    @Override
    public String getName(ItemStack stack) {
        return super.getName(stack) + Utils.plotPercent(tr(" charged at "), getChargeInTag(stack));
    }

    double getChargeInTag(ItemStack stack) {
        if (!mods.eln.generic.ItemNbt.has(stack))
            mods.eln.generic.ItemNbt.set(stack, getDefaultNBT());
        return mods.eln.generic.ItemNbt.get(stack).getDoubleOr("charge", 0.0);
    }

    double getLifeInTag(ItemStack stack) {
        if (!mods.eln.generic.ItemNbt.has(stack))
            mods.eln.generic.ItemNbt.set(stack, getDefaultNBT());
        return mods.eln.generic.ItemNbt.get(stack).getDoubleOr("life", 0.0);
    }

    public double getEnergy(double charge, double life) {
        int stepNbr = 50;
        double chargeStep = charge / stepNbr;
        double chargeIntegrator = 0;
        double energy = 0;
        double QperStep = electricalQ * life * charge / stepNbr;

        for (int step = 0; step < stepNbr; step++) {
            double voltage = UfCharge.getValue(chargeIntegrator) * electricalU;
            energy += voltage * QperStep;
            chargeIntegrator += chargeStep;
        }
        return energy;
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public boolean onEntityItemUpdate(ItemEntity entityItem) {
        if (entityItem.isOnFire()) {
            entityItem.level().explode(entityItem, entityItem.getX(), entityItem.getY(), entityItem.getZ(), 2, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
            entityItem.clearFire();
            entityItem.discard();
        }
        return false;
    }

    public void setRenderSpec(String renderSpec) {
        this.renderSpec = renderSpec;

        if (obj != null) {
            switch (renderType) {
                case 0:

                case 1:
                    main = obj.getPart("main");
                    plugPlus = obj.getPart("plugPlus");
                    plusMinus = obj.getPart("plugMinus");
                    //	cables = obj.getPart("cables");
                    battery = obj.getPart("battery_" + renderSpec);
                    break;
            }
        }
    }

    public void setCurrentDrop(double currentDropVoltage, double currentDropFactor) {
        this.currentDropFactor = currentDropFactor;
        this.currentDropVoltage = currentDropVoltage;
    }
}
