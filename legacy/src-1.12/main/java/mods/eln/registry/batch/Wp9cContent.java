package mods.eln.registry.batch;

import mods.eln.Eln;
import mods.eln.ElnContent;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.sixnode.AmplifierElement;
import mods.eln.sixnode.AmplifierRender;
import mods.eln.sixnode.Amplifier;
import mods.eln.sixnode.AnalogChipDescriptor;
import mods.eln.sixnode.EmergencyLampDescriptor;
import mods.eln.sixnode.Filter;
import mods.eln.sixnode.FilterElement;
import mods.eln.sixnode.FilterRender;
import mods.eln.sixnode.OpAmp;
import mods.eln.sixnode.PIDRegulator;
import mods.eln.sixnode.PIDRegulatorElement;
import mods.eln.sixnode.PIDRegulatorRender;
import mods.eln.sixnode.SampleAndHold;
import mods.eln.sixnode.SummingUnit;
import mods.eln.sixnode.SummingUnitElement;
import mods.eln.sixnode.SummingUnitRender;
import mods.eln.sixnode.VoltageControlledAmplifier;
import mods.eln.sixnode.VoltageControlledSawtoothOscillator;
import mods.eln.sixnode.VoltageControlledSineOscillator;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor;
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor;
import mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceRenderObj;
import mods.eln.sixnode.electricalmath.ElectricalMathDescriptor;
import mods.eln.sixnode.electricaltimeout.ElectricalTimeoutDescriptor;
import mods.eln.sixnode.energymeter.EnergyMeterDescriptor;
import mods.eln.sixnode.hub.HubDescriptor;
import mods.eln.sixnode.logicgate.*;
import mods.eln.sixnode.modbusrtu.ModbusRtuDescriptor;
import mods.eln.sixnode.tutorialsign.TutorialSignDescriptor;
import mods.eln.sixnode.tutorialsign.TutorialSignElement;
import mods.eln.sixnode.tutorialsign.TutorialSignOverlay;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.WirelessSignalAnalyserItemDescriptor;
import mods.eln.sixnode.wirelesssignal.repeater.WirelessSignalRepeaterDescriptor;
import mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxDescriptor;
import mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceDescriptor;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxDescriptor;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static mods.eln.Eln.*;
import static mods.eln.i18n.I18N.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp9c (1.12 port): six-node logic and misc: logic gates, analog chips, math, timeout, gate source, hub,
 * data logger, energy meter, wireless signal, modbus rtu (registered without creative entry: modbusEnable is false,
 * no TCP server in 1.12), tutorial sign, resin collector, emergency lamps.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * All wp9c registrations are ported here (1.12, ids unchanged); registry/pending/Wp9cPending.java is empty.
 * Selftest cases: mods.eln.selftest.cases.Wp9cCases.
 */
public class Wp9cContent implements ElnContent {
    @Override
    public void preInit() {
        registerHub(2);
        registerEmergencyLamps(64);
        registerWirelessSignal(92);
        registerElectricalDataLogger(93);
        registerElectricalGateSource(95);
        registerElectricalManagerEnergyMeter(98);
        registerElectricalGate(109);
        registerTreeResinCollector(116);
        registerSixNodeMisc(117); // modbus rtu, tutorial sign
        registerLogicalGates(118);
        registerAnalogChips(124);
        registerWirelessAnalyser(14); // shared item
        registerMiscItemDataLogsPrint(120); // shared item
    }

    @Override
    public void serverAboutToStart() {
        WirelessSignalTxElement.channelMap.clear();
    }

    @Override
    public void serverStopped() {
        IWirelessSignalSpot.spots.clear();
        WirelessSignalTxElement.channelMap.clear();
        TutorialSignElement.resetBalise();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void clientInit() {
        MinecraftForge.EVENT_BUS.register(new TutorialSignOverlay());
    }

    public static DataLogsPrintDescriptor dataLogsPrintDescriptor;

    public static void registerSixNodeMisc(int id) {

        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Modbus RTU");

            ModbusRtuDescriptor desc = new ModbusRtuDescriptor(
                name,
                obj.getObj("RTU")

            );

            if (modbusEnable) {
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            } else {
                sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
            }
        }
        {
            subId = 8;
            name = TR_NAME(Type.NONE, "Tutorial Sign");

            TutorialSignDescriptor desc = new TutorialSignDescriptor(
                name, obj.getObj("TutoPlate"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }



    
    }

    public static void registerElectricalManagerEnergyMeter(int id) {
        int subId, completId;
        String name;

        {
            subId = 4;

            name = TR_NAME(Type.NONE, "Energy Meter");

            EnergyMeterDescriptor desc = new EnergyMeterDescriptor(name, obj.getObj("EnergyMeter"), 8, 0);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 5;

            name = TR_NAME(Type.NONE, "Advanced Energy Meter");

            EnergyMeterDescriptor desc = new EnergyMeterDescriptor(name, obj.getObj("AdvancedEnergyMeter"), 7, 8);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerElectricalGate(int id) {
        int subId, completId;
        String name;
        {
            ElectricalTimeoutDescriptor desc;
            subId = 0;

            name = TR_NAME(Type.NONE, "Electrical Timer");

            desc = new ElectricalTimeoutDescriptor(name,
                obj.getObj("electricaltimer"));
            desc.setTickSound("eln:timer", 0.01f);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            ElectricalMathDescriptor desc;
            subId = 4;

            name = TR_NAME(Type.NONE, "Signal Processor");

            desc = new ElectricalMathDescriptor(name,
                obj.getObj("PLC"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerWirelessSignal(int id) {
        int subId, completId;
        String name;

        {
            WirelessSignalRxDescriptor desc;
            subId = 0;

            name = TR_NAME(Type.NONE, "Wireless Signal Receiver");

            desc = new WirelessSignalRxDescriptor(
                name,
                obj.getObj("wirelesssignalrx")

            );
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            WirelessSignalTxDescriptor desc;
            subId = 8;

            name = TR_NAME(Type.NONE, "Wireless Signal Transmitter");

            desc = new WirelessSignalTxDescriptor(
                name,
                obj.getObj("wirelesssignaltx"),
                wirelessTxRange
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            WirelessSignalRepeaterDescriptor desc;
            subId = 16;

            name = TR_NAME(Type.NONE, "Wireless Signal Repeater");

            desc = new WirelessSignalRepeaterDescriptor(
                name,
                obj.getObj("wirelesssignalrepeater"),
                wirelessTxRange
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerElectricalDataLogger(int id) {
        int subId, completId;
        String name;
        {
            ElectricalDataLoggerDescriptor desc;
            subId = 0;

            name = TR_NAME(Type.NONE, "Data Logger");

            desc = new ElectricalDataLoggerDescriptor(name, true,
                "DataloggerCRTFloor", 1f, 0.5f, 0f, "\u00a76");
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            ElectricalDataLoggerDescriptor desc;
            subId = 1;

            name = TR_NAME(Type.NONE, "Modern Data Logger");

            desc = new ElectricalDataLoggerDescriptor(name, true,
                "FlatScreenMonitor", 0.0f, 1f, 0.0f, "\u00A7a");
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            ElectricalDataLoggerDescriptor desc;
            subId = 2;

            name = TR_NAME(Type.NONE, "Industrial Data Logger");

            desc = new ElectricalDataLoggerDescriptor(name, false,
                "IndustrialPanel", 0.25f, 0.5f, 1f, "\u00A7f");
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalGateSource(int id) {
        int subId, completId;
        String name;

        ElectricalGateSourceRenderObj signalsourcepot = new ElectricalGateSourceRenderObj(obj.getObj("signalsourcepot"));
        ElectricalGateSourceRenderObj ledswitch = new ElectricalGateSourceRenderObj(obj.getObj("ledswitch"));

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Signal Trimmer");

            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, signalsourcepot, false,
                "trimmer");

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(Type.NONE, "Signal Switch");

            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, ledswitch, true,
                Eln.noSymbols ? "signalswitch" : "switch");

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;

            name = TR_NAME(Type.NONE, "Signal Button");

            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, ledswitch, true, "button");
            desc.setWithAutoReset();
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 12;

            name = TR_NAME(Type.NONE, "Wireless Button");

            WirelessSignalSourceDescriptor desc = new WirelessSignalSourceDescriptor(name, ledswitch, wirelessTxRange, true);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 16;

            name = TR_NAME(Type.NONE, "Wireless Switch");

            WirelessSignalSourceDescriptor desc = new WirelessSignalSourceDescriptor(name, ledswitch, wirelessTxRange, false);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerLogicalGates(int id) {
        Obj3D model = obj.getObj("LogicGates");
        sixNodeItem.addDescriptor(0 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "NOT Chip"), model, "NOT", Not.class));

        sixNodeItem.addDescriptor(1 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "AND Chip"), model, "AND", And.class));
        sixNodeItem.addDescriptor(2 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "NAND Chip"), model, "NAND", Nand.class));

        sixNodeItem.addDescriptor(3 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "OR Chip"), model, "OR", Or.class));
        sixNodeItem.addDescriptor(4 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "NOR Chip"), model, "NOR", Nor.class));

        sixNodeItem.addDescriptor(5 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "XOR Chip"), model, "XOR", Xor.class));
        sixNodeItem.addDescriptor(6 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "XNOR Chip"), model, "XNOR", XNor.class));

        sixNodeItem.addDescriptor(7 + (id << 6),
            new PalDescriptor(TR_NAME(Type.NONE, "PAL Chip"), model));

        sixNodeItem.addDescriptor(8 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "Schmitt Trigger Chip"), model, "SCHMITT",
                SchmittTrigger.class));

        sixNodeItem.addDescriptor(9 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "D Flip Flop Chip"), model, "DFF", DFlipFlop.class));

        sixNodeItem.addDescriptor(10 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "Oscillator Chip"), model, "OSC", Oscillator.class));

        sixNodeItem.addDescriptor(11 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "JK Flip Flop Chip"), model, "JKFF", JKFlipFlop.class));
    }

    public static void registerAnalogChips(int id) {
        id <<= 6;

        Obj3D model = obj.getObj("AnalogChips");
        sixNodeItem.addDescriptor(id + 0,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "OpAmp"), model, "OP", OpAmp.class));

        sixNodeItem.addDescriptor(id + 1, new AnalogChipDescriptor(TR_NAME(Type.NONE, "PID Regulator"), model, "PID",
            PIDRegulator.class, PIDRegulatorElement.class, PIDRegulatorRender.class));

        sixNodeItem.addDescriptor(id + 2,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Voltage controlled sawtooth oscillator"), model, "VCO-SAW",
                VoltageControlledSawtoothOscillator.class));

        sixNodeItem.addDescriptor(id + 3,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Voltage controlled sine oscillator"), model, "VCO-SIN",
                VoltageControlledSineOscillator.class));

        sixNodeItem.addDescriptor(id + 4,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Amplifier"), model, "AMP",
                Amplifier.class, AmplifierElement.class, AmplifierRender.class));

        sixNodeItem.addDescriptor(id + 5,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Voltage controlled amplifier"), model, "VCA",
                VoltageControlledAmplifier.class));

        sixNodeItem.addDescriptor(id + 6,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Configurable summing unit"), model, "SUM",
                SummingUnit.class, SummingUnitElement.class, SummingUnitRender.class));

        sixNodeItem.addDescriptor(id + 7,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Sample and hold"), model, "SAH",
                SampleAndHold.class));

        sixNodeItem.addDescriptor(id + 8,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Lowpass filter"), model, "LPF",
                Filter.class, FilterElement.class, FilterRender.class));
    }

    public static void registerTreeResinCollector(int id) {
        int subId, completId;
        String name;

        TreeResinCollectorDescriptor descriptor;
        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Tree Resin Collector");

            descriptor = new TreeResinCollectorDescriptor(name, obj.getObj("treeresincolector"));
            sixNodeItem.addDescriptor(completId, descriptor);
        }
    }

    public static void registerMiscItemDataLogsPrint(int id) {
        int subId, completId;
        String name;
        {
            subId = 32;
            name = TR_NAME(Type.NONE, "Data Logger Print");
            DataLogsPrintDescriptor desc = new DataLogsPrintDescriptor(name);
            dataLogsPrintDescriptor = desc;
            desc.setDefaultIcon("empty-texture");
            sharedItem.addWithoutRegistry(subId + (id << 6), desc);
        }

    }


    // Trimmed out of the core slice registrations in ElnDeviceRegistry (same ids):
    /** registerGround(2) sub 8. */
    public static void registerHub(int id) {
        int subId;
        String name;
        {
            subId = 8;
            name = TR_NAME(Type.NONE, "Hub");

            HubDescriptor desc = new HubDescriptor(name, obj.getObj("hub"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    /** registerLampSocket(64) sub-UIDs 15/16. */
    public static void registerEmergencyLamps(int id) {
        sixNodeItem.addDescriptor(15 + (id << 6),
            new EmergencyLampDescriptor(TR_NAME(Type.NONE, "50V Emergency Lamp"),
                lowVoltageCableDescriptor, 10 * 60 * 10, 10, 5, 6, obj.getObj("EmergencyExitLighting")));

        sixNodeItem.addDescriptor(16 + (id << 6),
            new EmergencyLampDescriptor(TR_NAME(Type.NONE, "200V Emergency Lamp"),
                meduimVoltageCableDescriptor, 10 * 60 * 20, 25, 10, 8, obj.getObj("EmergencyExitLighting")));
    }

    /** registerMeter(14) sub 8. */
    public static void registerWirelessAnalyser(int id) {
        int subId, completId;
        GenericItemUsingDamageDescriptor element;
        {
            subId = 8;
            completId = subId + (id << 6);
            element = new WirelessSignalAnalyserItemDescriptor(TR_NAME(Type.NONE, "Wireless Analyser"));
            sharedItem.addElement(completId, element);

        }
    }
}
