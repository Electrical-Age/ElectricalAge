package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.network.ElnNet;
import mods.eln.node.six.SixNodeBlock;
import mods.eln.node.six.SixNodeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * With a device that goes anywhere on a face (a chip) in hand: where a right-click would put it, and whether it may go
 * there (Baughn: the device renders translucent while you hold it ready to place). The same rules as the click
 * (SixNodeItem.useOn, VoxelNode.placeAnywhere), sent when they change: Eln.packetDevicePreview, drawn by
 * client.render.DevicePreviewRender. The wrench key (C) pressed with it in hand turns it before it is placed
 * (wrenchPressed), and the click places it as shown.
 */
public final class DevicePreview {
    private DevicePreview() {
    }

    private static final Map<UUID, String> LAST = new java.util.concurrent.ConcurrentHashMap<>();

    /** What the preview shows for a player at a placement turn: the key it is sent under ("" for nothing), the packet
     *  (null for nothing), the front and whether it may go there. */
    record Shown(String key, byte[] packet, LRDU front, boolean ok) {
        static final Shown NONE = new Shown("", null, null, false);
    }

    public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel)) return;
        Shown shown = compute(p, turn(p, p.getMainHandItem()));
        String key = shown.key();
        byte[] packet = shown.packet();
        String last = LAST.get(p.getUUID());
        if (key.equals(last == null ? "" : last)) return;
        LAST.put(p.getUUID(), key);
        if (packet == null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(Eln.packetDevicePreview);
            bos.write(0);   // (nothing to show)
            ElnNet.sendToPlayer(p, bos);
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.writeBytes(packet);
            ElnNet.sendToPlayer(p, bos);
        }
    }

    /** The preview for the device in p's main hand, turned `turn` quarters clockwise from the player's facing. */
    static Shown compute(ServerPlayer p, int turn) {
        if (!(p.level() instanceof ServerLevel level)) return Shown.NONE;
        if (p.getMainHandItem().getItem() instanceof SixNodeItem item && item.descriptor.placeAnywhere() && CableConverter.convertOnLoad
            && p.pick(p.blockInteractionRange(), 1f, false) instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
            var face = bh.getDirection();
            BlockPos pos = bh.getBlockPos().relative(face);
            var h = bh.getLocation();
            Direction side = Direction.fromIntMinecraftSide(face.get3DDataValue()).getInverse();
            if (level.getBlockState(bh.getBlockPos()).getBlock() instanceof VoxelNodeBlock) {
                pos = bh.getBlockPos();
                if (level.getBlockEntity(pos) instanceof VoxelNodeEntity ve && ve.getNode() instanceof VoxelNode v)
                    side = v.placeFace(face, h.x - pos.getX(), h.y - pos.getY(), h.z - pos.getZ());
            }
            var front = turned(item.descriptor.getFrontFromPlace(side, p), turn);
            int[] o = VoxelNode.snap(item.descriptor, side, front, h.x - pos.getX(), h.y - pos.getY(), h.z - pos.getZ());
            var st = level.getBlockState(pos);
            String err = null;
            boolean show = true;
            if (st.getBlock() instanceof VoxelNodeBlock) {
                if (level.getBlockEntity(pos) instanceof VoxelNodeEntity ve && ve.getNode() instanceof VoxelNode v) {
                    int slot = v.freeSlot();
                    if (slot < 0) err = "full";
                    else if (!item.descriptor.allowedOnSide(side)) err = "side";
                    else if (!v.inFaceSlot(item.descriptor, side, o)) {
                        // where the click would put it: in line on a cable, at its end, beside it, or at the click
                        var plan = DevicePlacer.plan(v, p.getMainHandItem(), slot, side, new double[]{h.x - pos.getX(), h.y - pos.getY(), h.z - pos.getZ()}, p, turn);
                        err = plan.err();
                        if (err == null) {
                            o = new int[]{plan.ox(), plan.oy(), plan.oz()};
                            front = plan.front();
                        }
                    }
                } else show = false;
            } else if (!st.isAir() && !st.canBeReplaced()) show = false;
            if (show && err == null && !item.descriptor.allowedOnSide(side)) err = "side";
            if (show && err == null && !SixNodeBlock.get().getIfOtherBlockIsSolid(level, pos, side)) err = "no support";
            if (show) {
                String key = pos.toShortString() + side + o[0] + "," + o[1] + "," + o[2] + front + item.descriptor.parentItemDamage + (err == null);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (DataOutputStream out = new DataOutputStream(bos)) {
                    out.writeByte(Eln.packetDevicePreview);
                    out.writeByte(err == null ? 1 : 2);
                    out.writeInt(pos.getX());
                    out.writeInt(pos.getY());
                    out.writeInt(pos.getZ());
                    out.writeByte(side.getInt());
                    out.writeByte(o[0]);
                    out.writeByte(o[1]);
                    out.writeByte(o[2]);
                    out.writeByte(front.dir);
                    out.writeShort(item.descriptor.parentItemDamage);
                } catch (IOException ignored) {
                }
                return new Shown(key, bos.toByteArray(), front, err == null);
            }
        }
        return Shown.NONE;
    }

    /** The front the preview shows for p now, or null if it shows nothing (GameTests). */
    public static LRDU previewFrontForTests(ServerPlayer p) {
        return compute(p, turn(p, p.getMainHandItem())).front();
    }

    // ---- turning the device before it is placed (Baughn 2026-11-18, #513) ----

    /** Each player's placement turn: the item kind it is for (parentItemDamage) and quarter turns clockwise (0..3). */
    private static final Map<UUID, int[]> TURN = new java.util.concurrent.ConcurrentHashMap<>();

    /** f turned n quarters clockwise. */
    static LRDU turned(LRDU f, int n) {
        for (int i = 0; i < Math.floorMod(n, 4); i++) f = f.getNextClockwise();
        return f;
    }

    /** The quarter turns clockwise the player chose with the wrench key for the device in stack (0 unless it is the kind in
     *  the main hand and the turn was chosen for that kind). */
    public static int turn(Player p, ItemStack stack) {
        if (p == null || !(stack.getItem() instanceof SixNodeItem item) || !(p.getMainHandItem().getItem() instanceof SixNodeItem held)
            || held.descriptor != item.descriptor) return 0;
        int[] t = TURN.get(p.getUUID());
        return t != null && t[0] == item.descriptor.parentItemDamage ? t[1] : 0;
    }

    /** The front a device of descriptor d placed on side by p gets: the player's facing, by the descriptor, turned as the
     *  player chose with the wrench key before placing (only for the device in the main hand). */
    public static LRDU placeFront(mods.eln.node.six.SixNodeDescriptor d, Direction side, Player p) {
        LRDU f = d.getFrontFromPlace(side, p);
        if (p == null || !(p.getMainHandItem().getItem() instanceof SixNodeItem item) || item.descriptor != d) return f;
        return turned(f, turn(p, p.getMainHandItem()));
    }

    /**
     * The wrench key (C) pressed: with a device that goes anywhere in the main hand, its placement turns to the next
     * front clockwise at which the preview shows a different place it may go (as a wrench click on a placed device skips
     * fronts that don't fit, #512); where none does it stays and says so, or turns a quarter if it may not go there anyway
     * or nothing is shown. Not when aiming at a device in a voxel block or at another EA block: C there is for the click
     * that turns it.
     */
    public static void wrenchPressed(Player pl) {
        if (!(pl instanceof ServerPlayer p) || !(p.getMainHandItem().getItem() instanceof SixNodeItem item) || !item.descriptor.placeAnywhere()
            || !CableConverter.convertOnLoad) return;
        if (p.pick(p.blockInteractionRange(), 1f, false) instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bh.getBlockPos();
            var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, p.level()));
            if (n instanceof VoxelNode v) {
                var h = bh.getLocation();
                if (v.elementAt(h.x - pos.getX(), h.y - pos.getY(), h.z - pos.getZ(), 0) >= 0) return;
            } else if (n != null) return;   // (a machine or six-node block: C + click turns it)
        }
        int was = turn(p, p.getMainHandItem());
        Shown now = compute(p, was);
        int to = was + 1;
        if (now.packet() != null) {
            boolean found = false;
            for (int k = 1; k < 4 && !found; k++) {
                Shown s = compute(p, was + k);
                if (s.ok() && !s.key().equals(now.key())) {
                    to = was + k;
                    found = true;
                }
            }
            if (!found && now.ok()) {
                to = was;   // (it stays as it fits, as a placed device does)
                p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("No other way round fits here"));
            }
        }
        TURN.put(p.getUUID(), new int[]{item.descriptor.parentItemDamage, Math.floorMod(to, 4)});
    }

    public static void forget(UUID player) {
        LAST.remove(player);
        TURN.remove(player);
    }
}
