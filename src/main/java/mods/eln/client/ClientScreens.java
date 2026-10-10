package mods.eln.client;

import mods.eln.client.gui.ElnScreens;

/** Descriptor class -> screen (P4b). Devices without one open the generic slot screen. */
final class ClientScreens {
    private ClientScreens() {
    }

    static void registerAll() {
        ElnScreens.register(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor.class,
            mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceScreen::new);
        ElnScreens.register(mods.eln.sixnode.electricalsource.ElectricalSourceDescriptor.class,
            mods.eln.sixnode.electricalsource.ElectricalSourceScreen::new);
        reg(mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxDescriptor.class, mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxScreen::new);
        reg(mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceDescriptor.class, mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceScreen::new);
        reg(mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxDescriptor.class, mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxScreen::new);
        reg(mods.eln.sixnode.electricalalarm.ElectricalAlarmDescriptor.class, mods.eln.sixnode.electricalalarm.ElectricalAlarmScreen::new);
        reg(mods.eln.sixnode.electricalrelay.ElectricalRelayDescriptor.class, mods.eln.sixnode.electricalrelay.ElectricalRelayScreen::new);
        reg(mods.eln.sixnode.electricalgatesource.ElectricalGateSourceDescriptor.class, mods.eln.sixnode.electricalgatesource.ElectricalGateSourceScreen::new);
        reg(mods.eln.sixnode.electricaltimeout.ElectricalTimeoutDescriptor.class, mods.eln.sixnode.electricaltimeout.ElectricalTimeoutScreen::new);
    }

    static void reg(Class<?> descriptor, ElnScreens.Factory f) {
        ElnScreens.register(descriptor, f);
    }
}
