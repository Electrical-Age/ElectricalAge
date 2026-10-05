package mods.eln.registry.batch;

import mods.eln.ElnContent;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp9a (1.12 port): six-node power components: switch, relay, breaker, fuse, diode, resistor, capacitor/inductor six, signal inductor, power socket, battery charger.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * Sources: m1-exclude-wp9a.txt (delete lines to build them). Not yet ported registrations:
 * registry/pending/Wp9aPending.java (excluded). To port a device: move its register method (and the fields it
 * sets) from Wp9aPending into this class, un-comment its PENDING line below, point device code at
 * Wp9aContent.&lt;field&gt;. Keep the ids (sub-UIDs) unchanged. Selftest cases: mods.eln.selftest.cases.Wp9aCases.
 */
public class Wp9aContent implements ElnContent {
    @Override
    public void preInit() {
        // PENDING(1.12 wp9a): registerBatteryCharger(66);
        // PENDING(1.12 wp9a): registerPowerSocket(67);
        // PENDING(1.12 wp9a): registerElectricalRelay(94);
        // PENDING(1.12 wp9a): registerPassiveComponent(96); // diodes, signal inductor, capacitor/inductor six, resistors
        // PENDING(1.12 wp9a): registerSwitch(97);
        // PENDING(1.12 wp9a): registerElectricalManager(98); // breaker, fuse holder, fuses
    }

    @Override
    public void serverAboutToStart() {
        // PENDING(1.12 wp9a): PowerSocketElement.channelMap.clear();
    }

    @Override
    public void serverStopped() {
        // PENDING(1.12 wp9a): PowerSocketElement.channelMap.clear();
    }
}
