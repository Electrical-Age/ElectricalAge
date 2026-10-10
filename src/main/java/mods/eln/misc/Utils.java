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

    /** The direction the entity looks along (pitch beyond 45 degrees: down/up). Yaw convention as in 1.12. */
    public static Direction entityLivingViewDirection(net.minecraft.world.entity.LivingEntity e) {
        if (e.getXRot() > 45) return Direction.YN;
        if (e.getXRot() < -45) return Direction.YP;
        return entityLivingHorizontalViewDirection(e);
    }

    public static Direction entityLivingHorizontalViewDirection(net.minecraft.world.entity.LivingEntity e) {
        int dirx = net.minecraft.util.Mth.floor((double) (e.getYRot() * 4.0F / 360.0F) + 0.5D) & 3;
        if (dirx == 3) return Direction.XP;
        if (dirx == 0) return Direction.ZP;
        if (dirx == 1) return Direction.XN;
        return Direction.ZN;
    }

    /** Registry access for item (de)serialization: the running server's (null when there is none). */
    @javax.annotation.Nullable
    public static net.minecraft.core.HolderLookup.Provider registries() {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess();
    }

    /** An ItemStack as NBT (26.x codec), or null for empty stacks or when no registries are available. */
    @javax.annotation.Nullable
    public static net.minecraft.nbt.Tag saveItem(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.core.HolderLookup.Provider r = registries();
        if (r == null || isEmpty(stack)) return null;
        return net.minecraft.world.item.ItemStack.CODEC.encodeStart(r.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
            .result().orElse(null);
    }

    public static net.minecraft.world.item.ItemStack loadItem(net.minecraft.nbt.Tag tag) {
        net.minecraft.core.HolderLookup.Provider r = registries();
        if (r == null || tag == null) return net.minecraft.world.item.ItemStack.EMPTY;
        return net.minecraft.world.item.ItemStack.CODEC.parse(r.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
            .result().orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    /** An inventory as a list of {Slot: byte, item} under key str (1.12 layout: Slot + the stack's own tags). */
    public static void readFromNBT(net.minecraft.nbt.CompoundTag nbt, String str, net.minecraft.world.Container inventory) {
        net.minecraft.nbt.ListTag list = nbt.getListOrEmpty(str);
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.CompoundTag t = list.getCompoundOrEmpty(i);
            int slot = t.getByteOr("Slot", (byte) 0) & 255;
            if (slot < inventory.getContainerSize())
                inventory.setItem(slot, loadItem(t.get("item")));
        }
    }

    public static void writeToNBT(net.minecraft.nbt.CompoundTag nbt, String str, net.minecraft.world.Container inventory) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            net.minecraft.nbt.Tag item = saveItem(inventory.getItem(i));
            if (item == null) continue;
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            t.putByte("Slot", (byte) i);
            t.put("item", item);
            list.add(t);
        }
        nbt.put(str, list);
    }

    /** 1.12 Utils.newNbtTagCompund: a new compound stored under key str of nbt, returned for filling. */
    public static net.minecraft.nbt.CompoundTag newNbtTagCompund(net.minecraft.nbt.CompoundTag nbt, String str) {
        net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
        nbt.put(str, c);
        return c;
    }

    /** The compound tags directly inside nbt (1.12 getTags). */
    public static java.util.List<net.minecraft.nbt.CompoundTag> getTags(net.minecraft.nbt.CompoundTag nbt) {
        java.util.List<net.minecraft.nbt.CompoundTag> out = new java.util.ArrayList<>();
        for (String k : nbt.keySet()) {
            net.minecraft.nbt.Tag t = nbt.get(k);
            if (t instanceof net.minecraft.nbt.CompoundTag) out.add((net.minecraft.nbt.CompoundTag) t);
        }
        return out;
    }

    /** 1.12 mustDropItem: drops are on unless the player is in creative. */
    public static boolean mustDropItem(net.minecraft.world.entity.player.Player player) {
        return player == null || !player.isCreative();
    }

    public static boolean isCreative(net.minecraft.world.entity.player.Player player) {
        return player != null && player.isCreative();
    }

    private static int uuid = 1;

    /** Server-side unique ids (sound tracks, element uuids), as in 1.12. */
    public static int getUuid() {
        if (uuid < 1) uuid = 1;
        return uuid++;
    }

    /**
     * An item for the client in an element packet: its registry id ("" for empty). 1.12 wrote two shorts (numeric item
     * id, damage); 26.x has neither, and both ends are EA code.
     */
    public static void serialiseItemStack(java.io.DataOutputStream stream, net.minecraft.world.item.ItemStack stack) throws java.io.IOException {
        if (isEmpty(stack)) stream.writeUTF("");
        else stream.writeUTF(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static net.minecraft.world.item.ItemStack unserialiseItemStack(java.io.DataInputStream stream) throws java.io.IOException {
        String id = stream.readUTF();
        if (id.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.resources.Identifier key = net.minecraft.resources.Identifier.tryParse(id);
        if (key == null) return net.minecraft.world.item.ItemStack.EMPTY;
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(key)
            .map(net.minecraft.world.item.ItemStack::new).orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    /** The player holds an EA wrench (filled when the wrench item is ported). */
    public static java.util.function.Predicate<net.minecraft.world.entity.player.Player> wrenchCheck = p -> false;

    public static boolean isPlayerUsingWrench(net.minecraft.world.entity.player.Player player) {
        return player != null && wrenchCheck.test(player);
    }
}
