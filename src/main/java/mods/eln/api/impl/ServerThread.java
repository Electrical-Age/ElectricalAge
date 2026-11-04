package mods.eln.api.impl;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** The API's thread rule: machines are the server thread's. */
public final class ServerThread {
    private ServerThread() {
    }

    /** Throws IllegalStateException when a server runs and this isn't its thread (no server: unit tests, allowed). */
    public static void check() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.isSameThread())
            throw new IllegalStateException("machine called from " + Thread.currentThread().getName()
                + "; only the server thread may");
    }
}
