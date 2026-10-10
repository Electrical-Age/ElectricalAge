package mods.eln.client;

import mods.eln.node.ElnRenders;

/**
 * Descriptor class -> render object (1.12: the RenderClass each descriptor passed to its super constructor). Filled
 * as the renders are ported (P3 R2/R3); unregistered devices draw nothing yet.
 */
final class ClientRenders {
    private ClientRenders() {
    }

    static void registerAll() {
        // R2 pilots
        ElnRenders.six(mods.eln.sixnode.groundcable.GroundCableDescriptor.class, mods.eln.sixnode.groundcable.GroundCableRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalcable.ElectricalCableDescriptor.class, mods.eln.sixnode.electricalcable.ElectricalCableRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.battery.BatteryDescriptor.class, mods.eln.transparentnode.battery.BatteryRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.electricalmachine.ElectricalMachineDescriptor.class,
            mods.eln.transparentnode.electricalmachine.ElectricalMachineRender::new);
    }
}
