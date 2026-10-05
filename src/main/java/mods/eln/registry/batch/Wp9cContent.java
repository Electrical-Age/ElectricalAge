package mods.eln.registry.batch;

import mods.eln.ElnContent;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp9c (1.12 port): six-node logic and misc: logic gates, analog chips, math, timeout, gate source, hub, data logger, energy meter, wireless signal, modbus rtu, tutorial sign, resin collector, emergency lamps.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * Sources: m1-exclude-wp9c.txt (delete lines to build them). Not yet ported registrations:
 * registry/pending/Wp9cPending.java (excluded). To port a device: move its register method (and the fields it
 * sets) from Wp9cPending into this class, un-comment its PENDING line below, point device code at
 * Wp9cContent.&lt;field&gt;. Keep the ids (sub-UIDs) unchanged. Selftest cases: mods.eln.selftest.cases.Wp9cCases.
 */
public class Wp9cContent implements ElnContent {
    @Override
    public void preInit() {
        // PENDING(1.12 wp9c): registerHub(2);
        // PENDING(1.12 wp9c): registerEmergencyLamps(64);
        // PENDING(1.12 wp9c): registerWirelessSignal(92);
        // PENDING(1.12 wp9c): registerElectricalDataLogger(93);
        // PENDING(1.12 wp9c): registerElectricalGateSource(95);
        // PENDING(1.12 wp9c): registerElectricalManagerEnergyMeter(98);
        // PENDING(1.12 wp9c): registerElectricalGate(109);
        // PENDING(1.12 wp9c): registerTreeResinCollector(116);
        // PENDING(1.12 wp9c): registerSixNodeMisc(117); // modbus rtu, tutorial sign
        // PENDING(1.12 wp9c): registerLogicalGates(118);
        // PENDING(1.12 wp9c): registerAnalogChips(124);
        // PENDING(1.12 wp9c): registerWirelessAnalyser(14); // shared item
        // PENDING(1.12 wp9c): registerMiscItemDataLogsPrint(120); // shared item
    }

    @Override
    public void serverAboutToStart() {
        WirelessSignalTxElement.channelMap.clear(); // wirelesssignal already builds (lamp supply)
    }

    @Override
    public void serverStopped() {
        IWirelessSignalSpot.spots.clear();
        WirelessSignalTxElement.channelMap.clear();
        // PENDING(1.12 wp9c): TutorialSignElement.resetBalise();
    }

    @Override
    public void clientInit() {
        // PENDING(1.12 wp9c): MinecraftForge.EVENT_BUS.register(new TutorialSignOverlay());
    }
}
