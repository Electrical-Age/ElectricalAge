package mods.eln.client;

import mods.eln.handbook.Handbook;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

/**
 * Builds the Ledger's GuideME guide (client only, when GuideME is present). It is built when the client reload
 * listeners are collected: after every mod constructor, so a start page set by another mod is seen, and before the
 * first resource load, which reads the pages.
 */
final class HandbookGuide {
    private HandbookGuide() {
    }

    private static boolean built;

    static void onAddReloadListeners(AddClientReloadListenersEvent e) {
        if (built) return;
        built = true;
        guideme.Guide.builder(Handbook.GUIDE)
            .startPage(Handbook.startPage())
            .build();
    }
}
