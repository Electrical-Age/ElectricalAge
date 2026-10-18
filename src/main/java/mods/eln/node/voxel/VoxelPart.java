package mods.eln.node.voxel;

import mods.eln.sim.fluid.FluidDevice;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * One thing inside a voxel node's block (notes/pipes/DESIGN.md section 2): it occupies some of the block's 16x16x16
 * voxels (its boxes), saves itself, and tells the client how to draw it. Kinds by name: "pipe" now; the six-node
 * devices are to become parts too (Baughn's endgame), so nothing here assumes pipes.
 */
public abstract class VoxelPart {
    public abstract String kind();

    /** The voxels it occupies, as boxes (x0 y0 z0 x1 y1 z1, 0..16). */
    public abstract List<int[]> boxes();

    /** Its fluid ports, for the block at pos (none by default). */
    public List<FluidDevice.FluidPort> ports(long pos) {
        return List.of();
    }

    public abstract void write(CompoundTag t);

    /** Client data (its own bytes; the node length-prefixes them). */
    public abstract void serialize(DataOutputStream out) throws IOException;

    /** Read a part saved by write (server), in the block at blockY; null for an unknown kind. */
    static VoxelPart read(CompoundTag t, int blockY) {
        return switch (t.getStringOr("kind", "")) {
            case PipePart.KIND -> PipePart.read(t, blockY);
            case ValvePart.KIND -> ValvePart.read(t);
            default -> null;
        };
    }

    /** Read a part's client data; null for an unknown kind. */
    static VoxelPart deserialize(String kind, DataInputStream in) throws IOException {
        return switch (kind) {
            case PipePart.KIND -> PipePart.deserialize(in);
            case ValvePart.KIND -> ValvePart.deserialize(in);
            default -> null;
        };
    }

    /** Does the box (voxels, 0..16) hold the point (block-local, 0..1), give or take eps? */
    static boolean contains(int[] b, double x, double y, double z, double eps) {
        return x * 16 >= b[0] - eps && x * 16 <= b[3] + eps && y * 16 >= b[1] - eps && y * 16 <= b[4] + eps
            && z * 16 >= b[2] - eps && z * 16 <= b[5] + eps;
    }
}
