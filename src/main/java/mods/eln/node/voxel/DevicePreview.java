package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.network.ElnNet;
import mods.eln.node.six.SixNodeBlock;
import mods.eln.node.six.SixNodeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * client.render.DevicePreviewRender.
 */
public final class DevicePreview {
    private DevicePreview() {
    }

    private static final Map<UUID, String> LAST = new java.util.concurrent.ConcurrentHashMap<>();

    public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        String key = "";
        byte[] packet = null;
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
            int[] o = VoxelNode.snap(item.descriptor, side, h.x - pos.getX(), h.y - pos.getY(), h.z - pos.getZ());
            var front = item.descriptor.getFrontFromPlace(side, p);
            var st = level.getBlockState(pos);
            String err = null;
            boolean show = true;
            if (st.getBlock() instanceof VoxelNodeBlock) {
                if (level.getBlockEntity(pos) instanceof VoxelNodeEntity ve && ve.getNode() instanceof VoxelNode v) {
                    int slot = v.freeSlot();
                    err = slot < 0 ? "full" : v.placeErrorAt(p.getMainHandItem(), slot, side, o[0], o[1], o[2], front);
                } else show = false;
            } else if (!st.isAir() && !st.canBeReplaced()) show = false;
            if (show && err == null && !SixNodeBlock.get().getIfOtherBlockIsSolid(level, pos, side)) err = "no support";
            if (show) {
                key = pos.toShortString() + side + o[0] + "," + o[1] + "," + o[2] + front + item.descriptor.parentItemDamage + (err == null);
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
                packet = bos.toByteArray();
            }
        }
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

    public static void forget(UUID player) {
        LAST.remove(player);
    }
}
