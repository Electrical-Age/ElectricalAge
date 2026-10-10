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
    }
}
