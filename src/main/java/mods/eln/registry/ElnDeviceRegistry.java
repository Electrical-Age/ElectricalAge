package mods.eln.registry;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.series.SerieEE;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricalsource.ElectricalSourceDescriptor;
import mods.eln.sixnode.groundcable.GroundCableDescriptor;
import mods.eln.sixnode.resistor.ResistorDescriptor;
import mods.eln.transparentnode.battery.BatteryDescriptor;
import mods.eln.transparentnode.solarpanel.SolarPanelDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.FunctionTableYProtect;

import static mods.eln.Eln.*;
import static mods.eln.i18n.I18N.TR_NAME;
import static mods.eln.i18n.I18N.Type;

import mods.eln.i18n.I18N;
import mods.eln.misc.Utils;

/**
 * Device registration, ported batch by batch from the 1.12 ElnDeviceRegistry and registry/batch/Wp*Content (in
 * legacy/). Methods keep their 1.12 bodies: descriptors are created and added to Eln.sixNodeItem /
 * transparentNodeItem under their 1.12 ids; ElnRegistry turns each into a 26.x item named by eln-ids.json.
 */
@SuppressWarnings({"SameParameterValue", "PointlessArithmeticExpression", "unused"})
public final class ElnDeviceRegistry {
    /** eln:light (1.12 ElnDeviceRegistry.lightBlock); set at common setup. */
    public static net.minecraft.world.level.block.Block lightBlock;

    /** Registered with the shared items (1.12 Wp12); null until then. */
    public static mods.eln.item.TreeResin treeResin;

    private ElnDeviceRegistry() {
    }

    public static ElectricalCableDescriptor veryHighVoltageCableDescriptor;
    public static ElectricalCableDescriptor highVoltageCableDescriptor;
    public static ElectricalCableDescriptor signalCableDescriptor;
    public static ElectricalCableDescriptor lowVoltageCableDescriptor;
    public static ElectricalCableDescriptor batteryCableDescriptor;
    public static ElectricalCableDescriptor meduimVoltageCableDescriptor;

    // moved from Eln (they read the low-voltage cable descriptor)
    public static double getSmallRs() {
        return lowVoltageCableDescriptor.electricalRs;
    }

    public static void applySmallRs(mods.eln.sim.nbt.NbtElectricalLoad aLoad) {
        lowVoltageCableDescriptor.applyTo(aLoad);
    }

    public static void applySmallRs(mods.eln.sim.mna.component.Resistor r) {
        lowVoltageCableDescriptor.applyTo(r);
    }

    /** P1 batch 1, in the 1.12 order (ElnContentImpl). */
    public static void registerBatch1() {
        registerGround(2);
        registerElectricalSource(3);
        registerElectricalCable(32);
        // resistors (96): Wp9aContent.registerPassiveComponent, with the rest of that group
        registerBattery(16);
        registerSolarPanel(48);
    }

    public static void registerElectricalCable(int id) {
        int subId, completId;
        String name;

        CableRenderDescriptor render;
        ElectricalCableDescriptor desc;
        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Signal Cable");

            stdCableRenderSignal = new CableRenderDescriptor("eln",
                "sprites/cable.png", 0.95f, 0.95f);

            desc = new ElectricalCableDescriptor(name, stdCableRenderSignal,
                "For signal transmission.", true);

            signalCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(SVU, SVP, 0.02 / 50
                    * gateOutputCurrent / SVII,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                SVU * 1.3, SVP * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                0.5,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, 1// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
            // GameRegistryCompat.registerCustomItemStack(name, desc.newItemStack(1));

        }

        {
            subId = 4;

            name = TR_NAME(Type.NONE, "Low Voltage Cable");

            stdCableRender50V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 1.95f, 0.95f);

            desc = new ElectricalCableDescriptor(name, stdCableRender50V,
                "For low voltage with high current.", false);

            lowVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(LVU, LVP(), 0.2 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                LVU * 1.3, LVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                20,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);

            desc = new ElectricalCableDescriptor(name, stdCableRender50V,
                "For low voltage with high current.", false);

            desc.setPhysicalConstantLikeNormalCable(
                LVU, LVP() / 4, 0.2 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                LVU * 1.3, LVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                20,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );
            batteryCableDescriptor = desc;

        }

        {
            subId = 8;

            name = TR_NAME(Type.NONE, "Medium Voltage Cable");

            stdCableRender200V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 2.95f, 0.95f);

            desc = new ElectricalCableDescriptor(name, stdCableRender200V,
                "miaou", false);

            meduimVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(MVU, MVP(), 0.10 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                MVU * 1.3, MVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                30,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);

        }
        {
            subId = 12;

            // highVoltageCableId = subId;
            name = TR_NAME(Type.NONE, "High Voltage Cable");

            stdCableRender800V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 3.95f, 1.95f);

            desc = new ElectricalCableDescriptor(name, stdCableRender800V,
                "miaou2", false);

            highVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(HVU, HVP(), 0.025 * 5 / 4 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                HVU * 1.3, HVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                40,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);

        }


        {
            subId = 16;

            // highVoltageCableId = subId;
            name = TR_NAME(Type.NONE, "Very High Voltage Cable");

            stdCableRender3200V = new CableRenderDescriptor("eln",
                "sprites/cableVHV.png", 3.95f, 1.95f);

            desc = new ElectricalCableDescriptor(name, stdCableRender3200V,
                "miaou2", false);

            veryHighVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(VVU, VVP(), 0.025 * 5 / 4 / 20 / 8,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                VVU * 1.3, VVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                40,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);

        }
    }

    public static void registerGround(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Ground Cable");

            GroundCableDescriptor desc = new GroundCableDescriptor(name, obj.getObj("groundcable"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        // subId 8 "Hub": Wp9c (registry/batch/Wp9cContent, registerHub(2))
    }

    public static void registerElectricalSource(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Electrical Source");

            ElectricalSourceDescriptor desc = new ElectricalSourceDescriptor(
                name, obj.getObj("voltagesource"), false);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Signal Source");

            ElectricalSourceDescriptor desc = new ElectricalSourceDescriptor(
                name, obj.getObj("signalsource"), true);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    /** From 1.12 Wp9aContent.registerPassiveComponent: the resistor family only (P1 batch 1). */
    public static void registerResistors(int id) {
        int subId;
        String name;
        {
            subId = 36;

            name = TR_NAME(Type.NONE, "Power Resistor");

            ResistorDescriptor desc = new ResistorDescriptor(
                name, obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), 0, false
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 37;
            name = TR_NAME(Type.NONE, "Rheostat");

            ResistorDescriptor desc = new ResistorDescriptor(
                name, obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), 0, true
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 38;

            name = TR_NAME(Type.NONE, "Thermistor");

            ResistorDescriptor desc = new ResistorDescriptor(
                name, obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), -0.01, false
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerBattery(int id) {
        int subId, completId;
        String name;
        double heatTIme = 30;
        double[] voltageFunctionTable = {0.000, 0.9, 1.0, 1.025, 1.04, 1.05,
            2.0};
        FunctionTable voltageFunction = new FunctionTable(voltageFunctionTable,
            6.0 / 5);
        double[] condoVoltageFunctionTable = {0.000, 0.89, 0.90, 0.905, 0.91, 1.1,
            1.5};
        FunctionTable condoVoltageFunction = new FunctionTable(condoVoltageFunctionTable,
            6.0 / 5);

        Utils.printFunction(voltageFunction, -0.2, 1.2, 0.1);

        double stdDischargeTime = 4 * 60;
        double stdU = LVU;
        double stdP = LVP() / 4;
        double stdEfficiency = 1.0 - 2.0 / 50.0;
        double condoEfficiency = 1.0 - 2.0 / 50.0;

        batteryVoltageFunctionTable = voltageFunction;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Cost Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction, stdU,
                stdP * 1.2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "Cheap battery" // name, description)
            );
            desc.setRenderSpec("lowcost");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Capacity Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction,
                stdU / 4, stdP / 2 * 1.2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP / 2, stdDischargeTime * 8 * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("capacity");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(Type.NONE, "Voltage Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", meduimVoltageCableDescriptor, 0.5, true, true, voltageFunction, stdU * 4,
                stdP * 1.2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("highvoltage");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 3;
            name = TR_NAME(Type.NONE, "Current Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction, stdU,
                stdP * 1.2 * 4, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP * 4, stdDischargeTime / 6 * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("current");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;
            name = TR_NAME(Type.NONE, "Life Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction, stdU,
                stdP * 1.2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife * 8, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("life");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 5;
            name = TR_NAME(Type.NONE, "Single-use Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", batteryCableDescriptor, 1.0, false, false, voltageFunction, stdU,
                stdP * 1.2 * 2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP * 2, stdDischargeTime * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife * 8, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("coal");
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 32;
            name = TR_NAME(Type.NONE, "50V Condensator");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "condo200", batteryCableDescriptor, 0.0, true, false,
                condoVoltageFunction,
                stdU, stdP * 1.2 * 8, 0.005, // electricalU,//
                // electricalPMax,electricalDischargeRate
                stdP * 8, 4, condoEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "Obselete, must be deleted" // name, description)
            );
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 2.0);
            desc.setDefaultIcon("empty-texture");
            transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }

        {
            subId = 36;
            name = TR_NAME(I18N.Type.NONE, "200V Condensator");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "condo200", highVoltageCableDescriptor, 0.0, true, false,
                condoVoltageFunction,
                MVU, MVP() * 1.5, 0.005, // electricalU,//
                // electricalPMax,electricalDischargeRate
                MVP(), 4, condoEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 2.0);
            desc.setDefaultIcon("empty-texture");
            transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
    }

    /** From 1.12 Wp10bContent. */
    public static void registerSolarPanel(int id) {
        int subId, completId;
        GhostGroup ghostGroup;
        String name;

        FunctionTable diodeIfUBase;
        diodeIfUBase = new FunctionTableYProtect(new double[]{0.0, 0.002,
            0.005, 0.01, 0.015, 0.02, 0.025, 0.03, 0.035, 0.04, 0.045,
            0.05, 0.06, 0.07, 0.08, 0.09, 0.10, 0.11, 0.12, 0.13, 1.0},
            1.0, 0, 1.0);

        FunctionTable solarIfSBase;
        solarIfSBase = new FunctionTable(new double[]{0.0, 0.1, 0.4, 0.6,
            0.8, 1.0}, 1);

        double LVSolarU = 59;

        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Small Solar Panel");

            ghostGroup = new GhostGroup();

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,// String
                // name,
                obj.getObj("smallsolarpannel"), null,
                ghostGroup, 0, 1, 0,// GhostGroup ghostGroup, int
                // solarOffsetX,int solarOffsetY,int
                // solarOffsetZ,
                // FunctionTable solarIfSBase,
                null, LVSolarU / 4, 65.0 * solarPanelPowerFactor,// double electricalUmax,double
                // electricalPmax,
                0.01,// ,double electricalDropFactor
                Math.PI / 2, Math.PI / 2 // alphaMin alphaMax
            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(Type.NONE, "Small Rotating Solar Panel");

            ghostGroup = new GhostGroup();

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,// String
                // name,
                obj.getObj("smallsolarpannelrot"), lowVoltageCableDescriptor.render,
                ghostGroup, 0, 1, 0,// GhostGroup ghostGroup, int
                // solarOffsetX,int solarOffsetY,int
                // solarOffsetZ,
                // FunctionTable solarIfSBase,
                null, LVSolarU / 4, solarPanelBasePower * solarPanelPowerFactor,// double electricalUmax,double
                // electricalPmax,
                0.01,// ,double electricalDropFactor
                Math.PI / 4, Math.PI / 4 * 3 // alphaMin alphaMax
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;
            name = TR_NAME(Type.NONE, "2x3 Solar Panel");

            Coordonate groundCoordinate = new Coordonate(1, 0, 0, 0);

            ghostGroup = new GhostGroup();
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1);
            ghostGroup.removeElement(0, 0, 0);

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,
                obj.getObj("bigSolarPanel"), meduimVoltageCableDescriptor.render,
                ghostGroup, 1, 1, 0,
                groundCoordinate,
                LVSolarU * 2, solarPanelBasePower * solarPanelPowerFactor * 8,
                0.01,
                Math.PI / 2, Math.PI / 2
            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;
            name = TR_NAME(Type.NONE, "2x3 Rotating Solar Panel");

            Coordonate groundCoordinate = new Coordonate(1, 0, 0, 0);

            ghostGroup = new GhostGroup();
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1);
            ghostGroup.removeElement(0, 0, 0);

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,
                obj.getObj("bigSolarPanelrot"), meduimVoltageCableDescriptor.render,
                ghostGroup, 1, 1, 1,
                groundCoordinate,
                LVSolarU * 2, solarPanelBasePower * solarPanelPowerFactor * 8,
                0.01,
                Math.PI / 8 * 3, Math.PI / 8 * 5
            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
}
