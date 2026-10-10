package mods.eln.server;

import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which players hold EA's wrench key (1.12 PlayerManager interactEnable, sent as packetPlayerKey). With it, a click on
 * a switch, button or wireless switch opens its screen instead of toggling it.
 */
public final class PlayerKeys {
    private PlayerKeys() {
    }

    public static final int WRENCH = 1;
    static final Set<UUID> WRENCH_DOWN = ConcurrentHashMap.newKeySet();

    public static void set(Player p, int key, boolean down) {
        if (key != WRENCH) return;
        if (down) WRENCH_DOWN.add(p.getUUID());
        else WRENCH_DOWN.remove(p.getUUID());
    }

    /** 1.12 Utils.isPlayerUsingWrench: the wrench key, or an item called a wrench in hand. */
    public static boolean usingWrench(Player p) {
        if (WRENCH_DOWN.contains(p.getUUID())) return true;
        var stack = p.getMainHandItem();
        return !stack.isEmpty() && stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains("wrench");
    }
}
