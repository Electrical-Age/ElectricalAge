package mods.eln.node.voxel;

import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A tee or cross where pipes branch (Baughn 2026-10-18 and 2026-11-11: a junction fitting at the branch point). Laid
 * by the pipe kit where a route joins a pipe, or another pipe's end. It has its own fluid node that is never lumped,
 * linked to each arm's end (VoxelNode.fluidLinks), so every branch stays its own run with its own pressure drop and
 * height: before, a tee merged the whole tree into one node (LabAgent's review, 2026-11-11). The node has real volume
 * (3/4 block of its bore: a 0.2 mL bore cube is where the core had trouble, #374 and #370), a wall like a pipe's, and
 * bursts an arm when over its rating. Not taken out on its own: it goes when fewer than two arms are left.
 */
public final class JunctionPart extends VoxelPart {
    public static final String KIND = "junction";
    /** Its volume and wall, in blocks of its bore (about a quarter block per arm of a tee: LabAgent). */
    static final double BLOCKS = 0.75;
    public final int x, y, z, tier;
    public FluidNode fluid;
    /** Its wall's temperature (K; NaN: not set yet). Not saved, as a pipe's. */
    double wallTemp = Double.NaN;

    public JunctionPart(int x, int y, int z, int tier) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.tier = tier;
    }

    @Override
    public String kind() {
        return KIND;
    }

    public int bore() {
        return PipePart.GRID[tier];
    }

    /** Its body: a cube a voxel wider than the bore each way. */
    @Override
    public List<int[]> boxes() {
        int h = bore() / 2 + 1;
        List<int[]> out = new ArrayList<>();
        out.add(ValvePart.clamp(new int[]{x - h, y - h, z - h, x + h, y + h, z + h}));
        return out;
    }

    /** Its node in the block at blockY: new air at T, or saved contents {water, air, energy}. */
    void makeNode(int blockY, double T, double[] saved) {
        double h = bore() / 16.0;
        fluid = new FluidNode(BLOCKS * PipePart.VOLUME_PER_BLOCK[tier], blockY + (y - bore() / 2.0) / 16.0, h, false);
        fluid.lumpable = false;
        if (saved != null) {
            fluid.mWater = saved[0];
            fluid.mAir = saved[1];
            fluid.energy = saved[2];
            fluid.update();
        } else fluid.fill(0, Water.P_ATM, T);
    }

    void wallBefore(double ambientK) {
        if (fluid == null) return;
        if (Double.isNaN(wallTemp)) wallTemp = fluid.mass() > 0 ? fluid.T() : ambientK;
        fluid.wallUA = PipePart.WALL_IN_UA_PER_BLOCK[tier] * BLOCKS;
        fluid.wallT = wallTemp;
    }

    void wallAfter(double ambientK, double dt) {
        if (fluid == null || Double.isNaN(wallTemp)) return;
        double c = PipePart.WALL_C_PER_BLOCK[tier] * BLOCKS;
        wallTemp += (-fluid.wallJ - PipePart.WALL_UA_PER_BLOCK[tier] * BLOCKS * (wallTemp - ambientK) * dt) / c;
    }

    @Override
    public void write(CompoundTag t) {
        t.putIntArray("at", new int[]{x, y, z, tier});
        if (fluid != null) {
            t.putDouble("w", fluid.mWater);
            t.putDouble("a", fluid.mAir);
            t.putDouble("u", fluid.energy);
            t.putInt("fv", mods.eln.fluid.FluidSaves.FORMAT);
            mods.eln.fluid.FluidSaves.putExtra(t, "s", fluid);
        }
    }

    static JunctionPart read(CompoundTag t, int blockY) {
        int[] a = t.getIntArray("at").orElse(new int[]{8, 8, 8, 0});
        if (a.length < 4) return null;
        JunctionPart j = new JunctionPart(a[0], a[1], a[2], Math.max(0, Math.min(PipePart.GRID.length - 1, a[3])));
        double[] saved = t.contains("w") ? new double[]{t.getDoubleOr("w", 0), t.getDoubleOr("a", 0), t.getDoubleOr("u", 0)} : null;
        j.makeNode(blockY, Water.T_AMBIENT, saved);
        if (saved != null) {
            j.fluid.extra = mods.eln.fluid.FluidSaves.extra(t, "s");
            j.fluid.update();
        }
        return j;
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(x);
        out.writeByte(y);
        out.writeByte(z);
        out.writeByte(tier);
    }

    static JunctionPart deserialize(DataInputStream in) throws IOException {
        int x = in.readByte(), y = in.readByte(), z = in.readByte(), tier = in.readByte();
        return new JunctionPart(x, y, z, Math.max(0, Math.min(PipePart.GRID.length - 1, tier)));
    }
}
