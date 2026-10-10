package mods.eln.registry;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.series.SerieEE;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricalsource.ElectricalSourceDescriptor;
import mods.eln.sixnode.groundcable.GroundCableDescriptor;
import mods.eln.sixnode.resistor.ResistorDescriptor;

import static mods.eln.Eln.*;
import static mods.eln.i18n.I18N.TR_NAME;
import static mods.eln.i18n.I18N.Type;

/**
 * Device registration, ported batch by batch from the 1.12 ElnDeviceRegistry and registry/batch/Wp*Content (in
 * legacy/). Methods keep their 1.12 bodies: descriptors are created and added to Eln.sixNodeItem /
 * transparentNodeItem under their 1.12 ids; ElnRegistry turns each into a 26.x item named by eln-ids.json.
 */
@SuppressWarnings({"SameParameterValue", "PointlessArithmeticExpression", "unused"})
public final class ElnDeviceRegistry {
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
        registerResistors(96);
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
}
