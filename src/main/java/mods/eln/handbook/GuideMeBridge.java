package mods.eln.handbook;

import net.minecraft.server.level.ServerPlayer;

/** The only common class that touches GuideME, loaded only when GuideME is present. */
final class GuideMeBridge {
    private GuideMeBridge() {
    }

    static void open(ServerPlayer p) {
        guideme.GuidesCommon.openGuide(p, Handbook.GUIDE);
    }
}
