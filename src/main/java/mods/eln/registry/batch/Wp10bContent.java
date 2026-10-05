package mods.eln.registry.batch;

import mods.eln.ElnContent;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp10b (1.12 port): transparent-node generators and thermal: solar, wind, water, turbine, heat furnace, fuel generators, dissipators, large rheostat, antennas, teleporter, turret.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * Sources: m1-exclude-wp10b.txt (delete lines to build them). Not yet ported registrations:
 * registry/pending/Wp10bPending.java (excluded). To port a device: move its register method (and the fields it
 * sets) from Wp10bPending into this class, un-comment its PENDING line below, point device code at
 * Wp10bContent.&lt;field&gt;. Keep the ids (sub-UIDs) unchanged. Selftest cases: mods.eln.selftest.cases.Wp10bCases.
 */
public class Wp10bContent implements ElnContent {
    @Override
    public void preInit() {
        // PENDING(1.12 wp10b): registerHeatFurnace(3);
        // PENDING(1.12 wp10b): registerTurbineElectrical(4); // the two TurbineDescriptor sub-UIDs
        // PENDING(1.12 wp10b): registerElectricalAntenna(7);
        // PENDING(1.12 wp10b): registerSolarPanel(48);
        // PENDING(1.12 wp10b): registerWindTurbine(49);
        // PENDING(1.12 wp10b): registerThermalDissipatorPassiveAndActive(64);
        // PENDING(1.12 wp10b): registerTransparentNodeMisc(65); // teleporter
        // PENDING(1.12 wp10b): registerTurret(66);
        // PENDING(1.12 wp10b): registerFuelGenerator(67);
        // PENDING(1.12 wp10b): registerPassiveComponentRheostat(96); // large rheostat (uses a passive dissipator)
    }

    @Override
    public void serverAboutToStart() {
        // PENDING(1.12 wp10b): TeleporterElement.teleporterList.clear();
    }

    @Override
    public void serverStopped() {
        // PENDING(1.12 wp10b): TeleporterElement.teleporterList.clear();
    }
}
