package mods.eln.registry.batch;

import mods.eln.ElnContent;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp11 (1.12 port): mechanical (shafts, steam/gas turbines, generator, flywheel, joints, tachometer), grid node (poles, downlink, grid transformer), simple-node energy converters (FE/RF/IC2 EU), TOP provider.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * Sources: m1-exclude-wp11.txt (delete lines to build them). Not yet ported registrations:
 * registry/pending/Wp11Pending.java (excluded). To port a device: move its register method (and the fields it
 * sets) from Wp11Pending into this class, un-comment its PENDING line below, point device code at
 * Wp11Content.&lt;field&gt;. Keep the ids (sub-UIDs) unchanged. Selftest cases: mods.eln.selftest.cases.Wp11Cases.
 */
public class Wp11Content implements ElnContent {
    @Override
    public void preInit() {
        // PENDING(1.12 wp11): registerTestBlock();
        // PENDING(1.12 wp11): registerEnergyConverter();
        // PENDING(1.12 wp11): registerTurbine(4); // mechanical sub-UIDs (steam/gas turbine, generator, joints, flywheel, tachometer)
        // PENDING(1.12 wp11): registerGridDevices(123);
        // PENDING(1.12 wp11): TODO(1.12 WP11): TheOneProbe provider (replaces Waila; Element getWaila() data)
    }
}
