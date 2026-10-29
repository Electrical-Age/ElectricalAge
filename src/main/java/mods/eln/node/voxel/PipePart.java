package mods.eln.node.voxel;

import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * A pipe inside one block (notes/pipes/DESIGN.md sections 2, 5, 6a): an axis-aligned polyline through the block's
 * voxels (0..16 on each axis); its tier sets what it takes on the voxel grid (GRID) and how wide it is drawn (DRAWN).
 * Its contents are one fluid-core node
 * per block (lumping longer runs is later). An end on the block's boundary is a port: it meets the pipe or device on
 * the other side at the same spot; an end inside the block is capped (sealed).
 */
public final class PipePart extends VoxelPart {
    public static final String KIND = "pipe";
    /**
     * Per tier (iron, alloy, large alloy): what it takes on the voxel grid (occupancy, hitbox, routing; even, so a box
     * centred on a lattice point: iron would be 1, but a 1-voxel box can't sit centred on the centre lines, which lie on
     * voxel corners), and the width it is drawn (Baughn 2026-10-29: iron and alloy at a quarter of their old 4 and 6;
     * the large alloy pipe 6 voxels, the old alloy look). Volume per block of length (m^3), conductance per block (m^3).
     */
    public static final int[] GRID = {2, 2, 6};
    public static final double[] DRAWN = {1.0, 1.5, 6.0};
    /** Before 2026-10-29: the grid widths saved in fittings' "at" arrays (no "g" key): iron 4, alloy 6. */
    static int oldBore(int saved) {
        return saved >= 4 ? 2 : saved;
    }

    /**
     * The large alloy pipe (Baughn 2026-10-29): inner diameter 16 x the alloy's, so 256 x its volume; its conductance
     * 1024 x (d^2.5): the network's flow is linear in the pressure drop (rho k dP / mu, Poiseuille's form, which would
     * give d^4 = 65536 x), but steam in a pipe this size is turbulent, where the flow at a given drop per length goes as
     * about d^2.5. Ratings lower than the alloy's: a wide thin-walled low-pressure line.
     */
    public static final double LARGE_D = 16;
    public static final double[] VOLUME_PER_BLOCK = {0.00003, 0.00003 * 2.25, 0.00003 * 2.25 * LARGE_D * LARGE_D};
    /** What saves before 2026-10-25 had (no "v" key): read scales their contents to the new volume. */
    static final double[] OLD_VOLUME_PER_BLOCK = {0.00015, 0.00015 * 2.25, 0.00015 * 2.25 * LARGE_D * LARGE_D};
    public static final double[] K_PER_BLOCK = {3e-11, 3e-11 * 2.25, 3e-11 * 2.25 * Math.pow(LARGE_D, 2.5)};
    public static final String[] TIER_NAME = {"iron", "alloy", "large alloy"};
    /** A device port made to take a large alloy pipe (m^3): its end's conductance (LP turbine, condenser, tanks). */
    public static final double LARGE_END_K = 2 * K_PER_BLOCK[2] * 16;
    /**
     * Ratings (DESIGN.md 6a): working and burst pressure (Pa), and the temperature (K) from which the rating falls,
     * linearly to half at SOFT_SPAN above it. Iron: 1850s-60s wrought iron; alloy: a Cr-Mo steam-line steel.
     */
    public static final double[] WORKING_PA = {10e5, 100e5, 40e5}, BURST_PA = {40e5, 300e5, 120e5}, SOFT_K = {573.15, 873.15, 873.15};
    static final double SOFT_SPAN = 100;
    /** A burst pipe vents through this (m^3): a crack, about a third of an open bore (a jet that lasts: Gemini). */
    static final double BURST_VENT_K = 1e-11;

    public final int tier;
    /** The polyline, x y z per point, in voxels. */
    public final int[] points;
    /** Server: the contents (null on the client). */
    public FluidNode fluid;
    /** Burst (over its pressure rating): it vents its run to the air until replaced; breaking it drops nothing. */
    public boolean burst;
    /** Where a burst's jet points (Direction 3D value), -1 if not burst. */
    public int burstDir = -1;

    public PipePart(int tier, int[] points) {
        this.tier = tier;
        this.points = points;
    }

    @Override
    public String kind() {
        return KIND;
    }

    /** Its width on the voxel grid (GRID). */
    public int bore() {
        return GRID[tier];
    }

    /** The width it is drawn (DRAWN, voxels). */
    public double drawn() {
        return DRAWN[tier];
    }

    /** Length in voxels. */
    public int length() {
        int n = 0;
        for (int i = 3; i < points.length; i += 3)
            n += Math.abs(points[i] - points[i - 3]) + Math.abs(points[i + 1] - points[i - 2]) + Math.abs(points[i + 2] - points[i - 1]);
        return n;
    }

    /** The boxes the bore sweeps, one per straight piece (voxels: x0 y0 z0 x1 y1 z1). */
    @Override
    public List<int[]> boxes() {
        int h = bore() / 2;
        List<int[]> out = new ArrayList<>();
        for (int i = 3; i < points.length; i += 3) {
            int[] b = new int[6];
            for (int a = 0; a < 3; a++) {
                int p = points[i - 3 + a], q = points[i + a];
                b[a] = Math.max(0, Math.min(p, q) - h);
                b[a + 3] = Math.min(16, Math.max(p, q) + h);
            }
            out.add(b);
        }
        return out;
    }

    /** Lowest and highest y of the centre line (voxels). */
    int minY() {
        int m = 16;
        for (int i = 1; i < points.length; i += 3) m = Math.min(m, points[i]);
        return m;
    }

    int maxY() {
        int m = 0;
        for (int i = 1; i < points.length; i += 3) m = Math.max(m, points[i]);
        return m;
    }

    /** The node's inside height (voxels): the bore, or the vertical run if longer. */
    int heightVoxels() {
        return Math.max(bore(), maxY() - minY());
    }

    /** The rating at temperature T: 1 up to SOFT_K, falling to 0.5 at SOFT_K + SOFT_SPAN and staying there. */
    public double derate(double T) {
        double over = T - SOFT_K[tier];
        return over <= 0 ? 1 : Math.max(0.5, 1 - 0.5 * over / SOFT_SPAN);
    }

    public double burstPa(double T) {
        return BURST_PA[tier] * derate(T);
    }

    public double workingPa(double T) {
        return WORKING_PA[tier] * derate(T);
    }

    /** Make the contents node (server), for a pipe in the block at blockY: air at 1 atm if new (at 20 C). */
    void makeNode(int blockY, double[] saved) {
        makeNode(blockY, Water.T_AMBIENT, saved);
    }

    /** The same, new air at the ambient T (K; Ambient). */
    void makeNode(int blockY, double T, double[] saved) {
        double volume = Math.max(1e-7, length() / 16.0 * VOLUME_PER_BLOCK[tier]);
        double z = blockY + (minY() - (heightVoxels() == bore() ? bore() / 2.0 : 0)) / 16.0;
        fluid = new FluidNode(volume, z, heightVoxels() / 16.0, false);
        fluid.lumpable = true;
        fluid.resistance = 1 / (K_PER_BLOCK[tier] * 16.0 / Math.max(1, length()));
        if (saved != null) {
            fluid.mWater = saved[0];
            fluid.mAir = saved[1];
            fluid.energy = saved[2];
            fluid.update();
        } else fluid.fill(0, Water.P_ATM, T);
    }

    /** The ends on the block's boundary, as ports (face and spot), for the block at pos. */
    @Override
    public List<FluidDevice.FluidPort> ports(long pos) {
        List<FluidDevice.FluidPort> out = new ArrayList<>();
        if (fluid == null) return out;
        int len = Math.max(1, length());
        double k = 2 * K_PER_BLOCK[tier] * 16.0 / len;   // (half the piece's length to each end)
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : points.length - 3;
            int x = points[i], y = points[i + 1], z = points[i + 2];
            int face = faceOf(x, y, z);
            if (face < 0) continue;
            double draw = heightVoxels() == bore() ? 0.5 : (y - minY()) / (double) heightVoxels();
            out.add(new FluidDevice.FluidPort(fluid, pos, face, draw, k, spot(face, x, y, z)));
        }
        return out;
    }

    /** The face a point on the boundary opens on (net.minecraft.core.Direction 3D data value), or -1 inside. */
    static int faceOf(int x, int y, int z) {
        if (y == 0) return 0;
        if (y == 16) return 1;
        if (z == 0) return 2;
        if (z == 16) return 3;
        if (x == 0) return 4;
        if (x == 16) return 5;
        return -1;
    }

    /** The spot on a face (FluidPort.at): the two other coordinates, in x y z order. */
    static int spot(int face, int x, int y, int z) {
        return switch (face) {
            case 0, 1 -> x * 17 + z;
            case 2, 3 -> x * 17 + y;
            default -> y * 17 + z;
        };
    }

    @Override
    public void write(CompoundTag t) {
        t.putInt("tier", tier);
        t.putIntArray("pts", points);
        if (burst) t.putBoolean("burst", true);
        if (burstDir >= 0) t.putInt("burstDir", burstDir);
        if (fluid != null) {
            t.putDouble("w", fluid.mWater);
            t.putDouble("a", fluid.mAir);
            t.putDouble("u", fluid.energy);
            t.putDouble("v", fluid.volume);
        }
    }

    static PipePart read(CompoundTag t, int blockY) {
        PipePart p = new PipePart(Math.max(0, Math.min(GRID.length - 1, t.getIntOr("tier", 0))), t.getIntArray("pts").orElse(new int[0]));
        p.burst = t.getBooleanOr("burst", false);
        p.burstDir = t.getIntOr("burstDir", -1);
        double[] saved = t.contains("w") ? new double[]{t.getDoubleOr("w", 0), t.getDoubleOr("a", 0), t.getDoubleOr("u", 0)} : null;
        p.makeNode(blockY, saved);
        // contents saved for another volume (before 2026-10-25 iron pipe held 0.15 L per block, now 0.03): kept at the
        // same state (pressure, temperature, mix), scaled to the new volume, or they'd load at 5x the pressure
        if (saved != null) {
            double v0 = t.getDoubleOr("v", Math.max(1e-7, p.length() / 16.0 * OLD_VOLUME_PER_BLOCK[p.tier]));
            if (v0 > 0 && Math.abs(v0 - p.fluid.volume) > 1e-12) {
                double f = p.fluid.volume / v0;
                p.fluid.mWater *= f;
                p.fluid.mAir *= f;
                p.fluid.energy *= f;
                p.fluid.update();
            }
        }
        return p;
    }

    @Override
    public void serialize(java.io.DataOutputStream out) throws java.io.IOException {
        out.writeByte(tier | (burst ? 0x80 : 0));
        out.writeByte(points.length / 3);
        for (int v : points) out.writeByte(v);
    }

    static PipePart deserialize(java.io.DataInputStream in) throws java.io.IOException {
        int b = in.readByte();
        int n = in.readByte();
        int[] pts = new int[3 * n];
        for (int i = 0; i < pts.length; i++) pts[i] = in.readByte();
        PipePart p = new PipePart(Math.max(0, Math.min(GRID.length - 1, b & 0x7F)), pts);
        p.burst = (b & 0x80) != 0;
        return p;
    }

    /** The middle of the longest straight piece (block-local voxels) and its axis: where a burst cracks. */
    public int[] crackPoint() {
        int best = 3, bestLen = -1;
        for (int i = 3; i < points.length; i += 3) {
            int len = Math.abs(points[i] - points[i - 3]) + Math.abs(points[i + 1] - points[i - 2]) + Math.abs(points[i + 2] - points[i - 1]);
            if (len > bestLen) {
                bestLen = len;
                best = i;
            }
        }
        int axis = points[best] != points[best - 3] ? 0 : points[best + 1] != points[best - 2] ? 1 : 2;
        return new int[]{(points[best] + points[best - 3]) / 2, (points[best + 1] + points[best - 2]) / 2, (points[best + 2] + points[best - 1]) / 2, axis};
    }

    /** Is (x, y, z) on the centre line? */
    public boolean passes(int x, int y, int z) {
        for (int i = 3; i < points.length; i += 3) {
            boolean in = true;
            for (int a = 0; a < 3; a++) {
                int v = a == 0 ? x : a == 1 ? y : z;
                if (v < Math.min(points[i - 3 + a], points[i + a]) || v > Math.max(points[i - 3 + a], points[i + a])) in = false;
            }
            if (in) return true;
        }
        return false;
    }

    /** Split at a point on the centre line into two pipes ending there (null if it's not inside the line). */
    public PipePart[] splitAt(int x, int y, int z) {
        for (int i = 3; i < points.length; i += 3) {
            boolean in = true;
            for (int a = 0; a < 3; a++) {
                int v = a == 0 ? x : a == 1 ? y : z;
                if (v < Math.min(points[i - 3 + a], points[i + a]) || v > Math.max(points[i - 3 + a], points[i + a])) in = false;
            }
            if (!in) continue;
            boolean atStart = points[0] == x && points[1] == y && points[2] == z;
            boolean atEnd = points[points.length - 3] == x && points[points.length - 2] == y && points[points.length - 1] == z;
            if (atStart || atEnd) return null;
            int[] p1 = new int[i + 3], p2 = new int[points.length - i + 3];
            System.arraycopy(points, 0, p1, 0, i);
            p1[i] = x; p1[i + 1] = y; p1[i + 2] = z;
            p2[0] = x; p2[1] = y; p2[2] = z;
            System.arraycopy(points, i, p2, 3, points.length - i);
            PipePart a = new PipePart(tier, dedupe(p1)), b = new PipePart(tier, dedupe(p2));
            return new PipePart[]{a, b};
        }
        return null;
    }

    /** Drop repeated points (a split at a corner). */
    static int[] dedupe(int[] p) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (int i = 0; i < p.length; i += 3) {
            int n = out.size();
            if (n >= 3 && out.get(n - 3) == p[i] && out.get(n - 2) == p[i + 1] && out.get(n - 1) == p[i + 2]) continue;
            out.add(p[i]); out.add(p[i + 1]); out.add(p[i + 2]);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Its ends inside the block (not on a face): where it can be joined. */
    public java.util.List<int[]> innerEnds() {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : points.length - 3;
            if (faceOf(points[i], points[i + 1], points[i + 2]) < 0) out.add(new int[]{points[i], points[i + 1], points[i + 2]});
        }
        return out;
    }

    /** A valid pipe: at least two points, each piece along one axis, inside the block, not zero length. */
    public boolean valid() {
        if (points.length < 6 || points.length % 3 != 0) return false;
        for (int v : points) if (v < 0 || v > 16) return false;
        for (int i = 3; i < points.length; i += 3) {
            int axes = 0;
            for (int a = 0; a < 3; a++) if (points[i + a] != points[i - 3 + a]) axes++;
            if (axes != 1) return false;
        }
        return true;
    }
}
