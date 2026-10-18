package mods.eln.node.voxel;

import mods.eln.fluid.FluidWorld;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.FluidDevice;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * A block that tracks which of its 16x16x16 voxels are used (notes/pipes/DESIGN.md section 2): a list of parts (pipes
 * now), none overlapping. One per block (NodeManager), uuid "v". Its pipes are fluid devices: their ends on the block's
 * faces couple with whatever is on the other side at the same spot (FluidWorld). No electrical or thermal side yet.
 */
public class VoxelNode extends Node implements FluidDevice {
    public static final String UUID = "v";

    public final List<VoxelPart> parts = new ArrayList<>();
    private final BitSet used = new BitSet(4096);

    @Override
    public String getNodeUuid() {
        return UUID;
    }

    static int voxel(int x, int y, int z) {
        return (x * 16 + y) * 16 + z;
    }

    private void markUsed() {
        used.clear();
        for (VoxelPart p : parts) for (int[] b : p.boxes()) fill(used, b);
    }

    private static void fill(BitSet s, int[] b) {
        for (int x = b[0]; x < b[3]; x++)
            for (int y = b[1]; y < b[4]; y++)
                for (int z = b[2]; z < b[5]; z++) s.set(voxel(x, y, z));
    }

    /** Would this part fit (no voxel already used)? */
    public boolean fits(VoxelPart p) {
        BitSet want = new BitSet(4096);
        for (int[] b : p.boxes()) fill(want, b);
        return !want.intersects(used);
    }

    /** Add a part (a pipe laid here): null if done, else why not. */
    public String addPart(VoxelPart p) {
        if (p instanceof PipePart pipe && !pipe.valid()) return "not a straight-pieced pipe inside the block";
        if (!fits(p)) return "something is in the way";
        if (p instanceof PipePart pipe && pipe.fluid == null) pipe.makeNode(coordonate.y, null);
        parts.add(p);
        markUsed();
        changed();
        return null;
    }

    /** Take a part out (it drops if a player broke it). Returns true if the block is now empty. */
    public boolean removePart(int index, boolean drop) {
        if (index < 0 || index >= parts.size()) return parts.isEmpty();
        VoxelPart p = parts.remove(index);
        markUsed();
        if (drop && p instanceof PipePart pipe) dropItem(PipeKitItem.kit(pipe.tier, pipe.length()));
        changed();
        return parts.isEmpty();
    }

    /** The part whose boxes hold the block-local point (0..1), or -1. */
    public int partAt(double x, double y, double z) {
        for (int i = parts.size() - 1; i >= 0; i--)
            for (int[] b : parts.get(i).boxes()) if (VoxelPart.contains(b, x, y, z, 0.05)) return i;
        return -1;
    }

    private void changed() {
        if (isAdded()) FluidWorld.changed(this);
        setNeedPublish(true);
    }

    // ---- fluid

    @Override
    public Object fluidLevel() {
        return coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        List<FluidPort> out = new ArrayList<>();
        long pos = coordonate.pos().asLong();
        for (VoxelPart p : parts) out.addAll(p.ports(pos));
        return out;
    }

    @Override
    public void connectJob() {
        super.connectJob();
        FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    // ---- node

    @Override
    public void initializeFromThat(Direction front, LivingEntity entityLiving, ItemStack itemStack) {
        connect();
    }

    @Override
    public void initializeFromNBT() {
        connect();
    }

    @Override
    public void onBreakBlock() {
        for (VoxelPart p : parts) if (p instanceof PipePart pipe) dropItem(PipeKitItem.kit(pipe.tier, pipe.length()));
        parts.clear();
        super.onBreakBlock();
    }

    @Override
    public int getSideConnectionMask(Direction directionA, LRDU lrduA) {
        return 0;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction directionA, LRDU lrduA) {
        return null;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction directionB, LRDU lrduB) {
        return null;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putInt("parts", parts.size());
        for (int i = 0; i < parts.size(); i++) {
            CompoundTag t = new CompoundTag();
            t.putString("kind", parts.get(i).kind());
            parts.get(i).write(t);
            nbt.put("p" + i, t);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        parts.clear();
        int n = nbt.getIntOr("parts", 0);
        for (int i = 0; i < n; i++) {
            VoxelPart p = VoxelPart.read(nbt.getCompoundOrEmpty("p" + i), coordonate.y);
            if (p != null) parts.add(p);
        }
        markUsed();
    }

    /** Client data: the part count, then each part's kind and length-prefixed bytes (an unknown kind is skipped). */
    @Override
    public void publishSerialize(DataOutputStream stream) {
        super.publishSerialize(stream);
        try {
            stream.writeShort(parts.size());
            for (VoxelPart p : parts) {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                p.serialize(new DataOutputStream(b));
                stream.writeUTF(p.kind());
                stream.writeShort(b.size());
                stream.write(b.toByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean playerAskToBreakPart(Player player, int index) {
        return removePart(index, player == null || !player.isCreative());
    }
}
