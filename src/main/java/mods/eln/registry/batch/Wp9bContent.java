package mods.eln.registry.batch;

import mods.eln.ElnContent;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp9b (1.12 port): six-node sensors and IO: light/weather/wind/thermal/entity/fire/electrical sensors, scanner, vu meter, watch, alarm, redstone in/out, thermal cable.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * Sources: m1-exclude-wp9b.txt (delete lines to build them). Not yet ported registrations:
 * registry/pending/Wp9bPending.java (excluded). To port a device: move its register method (and the fields it
 * sets) from Wp9bPending into this class, un-comment its PENDING line below, point device code at
 * Wp9bContent.&lt;field&gt;. Keep the ids (sub-UIDs) unchanged. Selftest cases: mods.eln.selftest.cases.Wp9bCases.
 */
public class Wp9bContent implements ElnContent {
    @Override
    public void preInit() {
        // PENDING(1.12 wp9b): registerThermalCable(48);
        // PENDING(1.12 wp9b): registerElectricalSensor(100);
        // PENDING(1.12 wp9b): registerThermalSensor(101);
        // PENDING(1.12 wp9b): registerElectricalVuMeter(102);
        // PENDING(1.12 wp9b): registerElectricalAlarm(103);
        // PENDING(1.12 wp9b): registerElectricalEnvironmentalSensor(104); // light, weather, wind, entity, fire, scanner
        // PENDING(1.12 wp9b): registerElectricalRedstone(108);
        // PENDING(1.12 wp9b): registerSixNodeMiscWatch(117); // electrical watches
    }
}
