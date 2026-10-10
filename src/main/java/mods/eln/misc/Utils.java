package mods.eln.misc;

import mods.eln.Eln;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The 1.12 Utils, ported a piece at a time (P0: what the simulator needs). With -Deln.deterministic=true (the unit
 * tests), rand returns the midpoint, like the 1.12 test stub did.
 */
public class Utils {
    static final boolean DETERMINISTIC = Boolean.getBoolean("eln.deterministic");

    public static void println(Object str) {
        Eln.LOG.debug(String.valueOf(str));
    }

    public static void print(String format, Object... data) {
        Eln.LOG.debug(String.format(format, data));
    }

    public static void print(Object str) {
        Eln.LOG.debug(String.valueOf(str));
    }

    /** Uniform in [min, max). */
    public static double rand(double min, double max) {
        if (DETERMINISTIC) return (min + max) / 2;
        return min + ThreadLocalRandom.current().nextDouble() * (max - min);
    }

    public static boolean isTheClass(Object o, Class c) {
        for (Class k = o.getClass(); k != null; k = k.getSuperclass()) if (k == c) return true;
        return false;
    }

    public static boolean isEmpty(net.minecraft.world.item.ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    /**
     * Same item, or both in a common tag (1.12: same item + meta, or a shared ore dictionary name). Without damage
     * metadata on 26.x, item identity replaces item + meta.
     */
    public static boolean areSame(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemStack output) {
        if (stack.getItem() == output.getItem()) return true;
        return stack.typeHolder().tags().anyMatch(output::is);
    }

    public static void sendPacketToClient(java.io.ByteArrayOutputStream bos, net.minecraft.server.level.ServerPlayer player) {
        mods.eln.network.ElnNet.sendToPlayer(player, bos);
    }

    public static void sendMessage(net.minecraft.world.entity.player.Player player, String text) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(text));
    }

    public static void notifyNeighbours(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState s = level.getBlockState(pos);
        level.updateNeighborsAt(pos, s.getBlock());
    }
}
