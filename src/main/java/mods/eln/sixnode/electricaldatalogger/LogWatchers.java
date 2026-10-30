package mods.eln.sixnode.electricaldatalogger;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The players a logger's graph goes to (board #45; Baughn: the network sends only what is needed). A client asks to
 * watch while it can see the graph (its screen open, or the logger near enough to read) and repeats the ask every
 * second; a watcher not heard from for {@link #EXPIRY} ticks is dropped. A new watcher gets the whole graph, watchers
 * then get each point as it is due. Not saved: watching starts again after a load.
 */
public final class LogWatchers {
    public static final long EXPIRY = 100;
    final Map<UUID, Long> seen = new HashMap<>();

    /** A watch request at game tick now; true if the player was not watching (send it the whole graph). */
    public boolean watch(ServerPlayer player, long now) {
        return seen.put(player.getUUID(), now) == null;
    }

    /** The watchers still heard from, the rest dropped. */
    public List<ServerPlayer> players(ServerLevel level) {
        long now = level.getGameTime();
        List<ServerPlayer> out = new ArrayList<>();
        seen.entrySet().removeIf(e -> {
            if (now - e.getValue() > EXPIRY) return true;
            var p = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null || p.level() != level) return true;
            out.add(p);
            return false;
        });
        return out;
    }

    public boolean isEmpty() {
        return seen.isEmpty();
    }
}
