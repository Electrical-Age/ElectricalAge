package mods.eln.misc;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;

/**
 * 1.12 dimension ints for 26.x level keys, so Coordonate and the saved node data keep an int: overworld 0, nether -1,
 * end 1 (the 1.12 ids), other levels the hash of their id (deterministic, so stable across restarts).
 */
public final class Dimensions {
    private Dimensions() {
    }

    public static final int INVALID = -5123;

    public static int id(ResourceKey<Level> key) {
        if (key == Level.OVERWORLD) return 0;
        if (key == Level.NETHER) return -1;
        if (key == Level.END) return 1;
        int h = key.identifier().toString().hashCode();
        if (h == 0 || h == -1 || h == 1 || h == INVALID) h ^= 0x5bd1e995;
        return h;
    }

    public static int id(Level level) {
        return id(level.dimension());
    }

    /** The server level with this id, or null (no server, or no such level). */
    @Nullable
    public static ServerLevel level(int id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (ServerLevel l : server.getAllLevels()) if (id(l) == id) return l;
        return null;
    }
}
