package mods.eln.registry.batch;

import mods.eln.ElnContent;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp10a (1.12 port): transparent-node power and machines: transformer, power capacitor/inductor, electrical machines (macerator, compressor, magnetizer, plate machine), electrical furnace, egg incubator, autominer.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * Sources: m1-exclude-wp10a.txt (delete lines to build them). Not yet ported registrations:
 * registry/pending/Wp10aPending.java (excluded). To port a device: move its register method (and the fields it
 * sets) from Wp10aPending into this class, un-comment its PENDING line below, point device code at
 * Wp10aContent.&lt;field&gt;. Keep the ids (sub-UIDs) unchanged. Selftest cases: mods.eln.selftest.cases.Wp10aCases.
 */
public class Wp10aContent implements ElnContent {
    @Override
    public void preInit() {
        // PENDING(1.12 wp10a): registerPowerComponent(1);
        // PENDING(1.12 wp10a): registerTransformer(2);
        // PENDING(1.12 wp10a): registerElectricalFurnace(32);
        // PENDING(1.12 wp10a): registerMacerator(33);
        // PENDING(1.12 wp10a): registerCompressor(35);
        // PENDING(1.12 wp10a): registerMagnetizer(36);
        // PENDING(1.12 wp10a): registerPlateMachine(37);
        // PENDING(1.12 wp10a): registerEggIncubator(41);
        // PENDING(1.12 wp10a): registerAutoMiner(42);
    }
}
