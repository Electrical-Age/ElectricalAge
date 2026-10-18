package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * A piece of electrical cable inside one block (notes/cables/PLAN.md; Baughn 2026-10-18): a polyline lying against
 * one face of the block (its attachment face), drawn with its tier's cross-section (width along the face, height off
 * it). Pieces of one tier on one face, joined end to end (or an end on another's line), are one conductor: one
 * electrical and one thermal load, as one six-node cable element was (CableNet). An end on the block's boundary is a
 * port: it meets the cable on the other side at the same point. Pieces on two faces meeting at an inner corner, or two
 * tiers meeting, are joined by a connection, as six-node cables were.
 * <p>
 * A knuckle (face -1) is a cable bent round an outer corner: a single point on an edge of the block, with a port on
 * each of the two faces that meet there. It is its own tiny conductor (no thermal load).
 */
public final class CablePart extends VoxelPart {
    public static final String KIND = "cable";

    /** The tiers: six-node cable descriptor name, cross-section width and height (voxels). Signal 1x1 (Baughn). */
    public record Tier(String name, int width, int height, boolean thermal) {
        Tier(String name, int width, int height) {
            this(name, width, height, false);
        }

        /** An electrical tier's descriptor (null for the thermal cable). */
        public ElectricalCableDescriptor descriptor() {
            return sixDescriptor() instanceof ElectricalCableDescriptor d ? d : null;
        }

        /** The thermal cable's descriptor (null for the electrical tiers). */
        public mods.eln.sixnode.thermalcable.ThermalCableDescriptor thermalDescriptor() {
            return sixDescriptor() instanceof mods.eln.sixnode.thermalcable.ThermalCableDescriptor d ? d : null;
        }

        /** The six-node cable (its item is the coil). */
        public mods.eln.node.six.SixNodeDescriptor sixDescriptor() {
            for (var e : Eln.sixNodeItem.all().values()) if (e.name.equals(name)) return e;
            return null;
        }

        public mods.eln.cable.CableRenderDescriptor render() {
            var d = sixDescriptor();
            if (d instanceof ElectricalCableDescriptor e) return e.render;
            if (d instanceof mods.eln.sixnode.thermalcable.ThermalCableDescriptor t) return t.render;
            return null;
        }
    }

    public static final Tier[] TIERS = {
        new Tier("Signal Cable", 1, 1),
        new Tier("Low Voltage Cable", 2, 1),
        new Tier("Medium Voltage Cable", 3, 1),
        new Tier("High Voltage Cable", 4, 2),
        new Tier("Very High Voltage Cable", 4, 2),
        new Tier("Copper Thermal Cable", 4, 4, true),
    };

    public static int tierOf(String name) {
        for (int i = 0; i < TIERS.length; i++) if (TIERS[i].name.equals(name)) return i;
        return -1;
    }

    public final int tier;
    /** The attachment face (net.minecraft.core.Direction 3D value: 0 down .. 5 east), -1 for a knuckle. */
    public final int face;
    /** The centre line on the face's plane, x y z per point, in voxels (the face's own coordinate is 0 or 16). */
    public final int[] points;
    /** Burnt out (by its watchdogs): an open circuit until repaired; null while it works. */
    public mods.eln.sim.process.destruct.FailureCause burnt;
    /** What breaking it gives back (voxels), if not its length: a converted six-node cable was one item, 16 voxels,
     *  shared among its arms (a cross's four arms are 32 voxels long). -1: its length. */
    public int paid = -1;

    public int dropLength() {
        return paid >= 0 ? paid : isKnuckle() ? 0 : length();
    }

    /** Laid by the converter (a knuckle round an outer corner): a revert takes it out. */
    public boolean byConverter;
    /** Saved state (server): voltage and temperature, restored into its conductor. */
    public double u, t;

    public CablePart(int tier, int face, int[] points) {
        this.tier = tier;
        this.face = face;
        this.points = points;
    }

    /**
     * A six-node cable element as voxel pieces (the converter's core): on face `face`, arms from the face's centre to
     * the middle of each edge toward `dirs` (3D values, along the face); two opposite arms are one straight piece.
     */
    public static List<CablePart> arms(int tier, int face, int... dirs) {
        int n = normalAxis(face);
        int[] c = {8, 8, 8};
        c[n] = plane(face);
        List<CablePart> out = new ArrayList<>();
        if (dirs.length == 2 && dirs[0] / 2 == dirs[1] / 2 && dirs[0] != dirs[1]) {
            int[] a = edge(c, dirs[0]), b = edge(c, dirs[1]);
            out.add(new CablePart(tier, face, new int[]{a[0], a[1], a[2], b[0], b[1], b[2]}));
            return out;
        }
        for (int d : dirs) {
            int[] e = edge(c, d);
            out.add(new CablePart(tier, face, new int[]{c[0], c[1], c[2], e[0], e[1], e[2]}));
        }
        return out;
    }

    /** The point c moved to the block's boundary toward direction d (3D value). */
    static int[] edge(int[] c, int d) {
        int[] e = c.clone();
        int axis = d / 2 == 0 ? 1 : d / 2 == 1 ? 2 : 0;
        e[axis] = d % 2 == 0 ? 0 : 16;
        return e;
    }

    public static CablePart knuckle(int tier, int x, int y, int z) {
        return new CablePart(tier, -1, new int[]{x, y, z});
    }

    @Override
    public String kind() {
        return KIND;
    }

    public Tier tierInfo() {
        return TIERS[tier];
    }

    public boolean isKnuckle() {
        return face < 0;
    }

    /** The axis normal to the attachment face (0 x, 1 y, 2 z). */
    static int normalAxis(int face) {
        return switch (face) {
            case 0, 1 -> 1;
            case 2, 3 -> 2;
            default -> 0;
        };
    }

    /** The axis of a direction (3D value): 0 x, 1 y, 2 z. */
    static int axis(int dir) {
        return normalAxis(dir);
    }

    /** The attachment face's plane coordinate: 0 for down/north/west, 16 for up/south/east. */
    static int plane(int face) {
        return face % 2 == 0 ? 0 : 16;
    }

    /** Length in voxels. */
    public int length() {
        int n = 0;
        for (int i = 3; i < points.length; i += 3)
            n += Math.abs(points[i] - points[i - 3]) + Math.abs(points[i + 1] - points[i - 2]) + Math.abs(points[i + 2] - points[i - 1]);
        return n;
    }

    /** Its share of a block's conductor (length / 16, a quarter to one): scales resistance and heat capacity. */
    public double share() {
        return Math.max(0.25, Math.min(1, length() / 16.0));
    }

    /** A valid piece: on its face's plane, inside the block, pieces along one axis each; a knuckle sits on an edge. */
    public boolean valid() {
        if (tier < 0 || tier >= TIERS.length) return false;
        for (int v : points) if (v < 0 || v > 16) return false;
        if (isKnuckle()) return points.length == 3 && boundaryAxes(points[0], points[1], points[2]) == 2;
        if (face > 5 || points.length < 6 || points.length % 3 != 0) return false;
        int n = normalAxis(face);
        for (int i = n; i < points.length; i += 3) if (points[i] != plane(face)) return false;
        for (int i = 3; i < points.length; i += 3) {
            int axes = 0;
            for (int a = 0; a < 3; a++) if (points[i + a] != points[i - 3 + a]) axes++;
            if (axes != 1) return false;
        }
        // an end on the boundary must open on one face only (not a corner of the face)
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : points.length - 3;
            int[] p = {points[i], points[i + 1], points[i + 2]};
            int on = 0;
            for (int a = 0; a < 3; a++) if (a != n && (p[a] == 0 || p[a] == 16)) on++;
            if (on > 1) return false;
        }
        return true;
    }

    static int boundaryAxes(int x, int y, int z) {
        int n = 0;
        if (x == 0 || x == 16) n++;
        if (y == 0 || y == 16) n++;
        if (z == 0 || z == 16) n++;
        return n;
    }

    /** The boxes its cross-section sweeps, one per straight piece (or one for a knuckle). */
    @Override
    public List<int[]> boxes() {
        Tier t = tierInfo();
        int lo = t.width / 2, hi = t.width - t.width / 2;
        List<int[]> out = new ArrayList<>();
        if (isKnuckle()) {
            int[] b = new int[6];
            for (int a = 0; a < 3; a++) {
                int p = points[a];
                if (p == 0) { b[a] = 0; b[a + 3] = t.height; }
                else if (p == 16) { b[a] = 16 - t.height; b[a + 3] = 16; }
                else { b[a] = Math.max(0, p - lo); b[a + 3] = Math.min(16, p + hi); }
            }
            out.add(b);
            return out;
        }
        int n = normalAxis(face);
        for (int i = 3; i < points.length; i += 3) {
            int[] b = new int[6];
            for (int a = 0; a < 3; a++) {
                int p = points[i - 3 + a], q = points[i + a];
                if (a == n) {
                    b[a] = plane(face) == 0 ? 0 : 16 - t.height;
                    b[a + 3] = plane(face) == 0 ? t.height : 16;
                } else {
                    b[a] = Math.max(0, Math.min(p, q) - lo);
                    b[a + 3] = Math.min(16, Math.max(p, q) + hi);
                }
            }
            out.add(b);
        }
        return out;
    }

    /** A port: an end on the block's boundary, opening on `face` (3D value) at point p (block-local voxels). */
    public record Port(int face, int x, int y, int z) {
        /** The same point seen from the block on the other side of the face. */
        Port mirrored() {
            return switch (face) {
                case 0 -> new Port(1, x, 16, z);
                case 1 -> new Port(0, x, 0, z);
                case 2 -> new Port(3, x, y, 16);
                case 3 -> new Port(2, x, y, 0);
                case 4 -> new Port(5, 16, y, z);
                default -> new Port(4, 0, y, z);
            };
        }
    }

    /** Its ports (a knuckle: two, one on each face of its edge). */
    public List<Port> cablePorts() {
        List<Port> out = new ArrayList<>();
        if (isKnuckle()) {
            int x = points[0], y = points[1], z = points[2];
            if (y == 0) out.add(new Port(0, x, y, z));
            if (y == 16) out.add(new Port(1, x, y, z));
            if (z == 0) out.add(new Port(2, x, y, z));
            if (z == 16) out.add(new Port(3, x, y, z));
            if (x == 0) out.add(new Port(4, x, y, z));
            if (x == 16) out.add(new Port(5, x, y, z));
            return out;
        }
        int n = normalAxis(face);
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : points.length - 3;
            int x = points[i], y = points[i + 1], z = points[i + 2];
            int f = -1;
            if (n != 1 && y == 0) f = 0;
            else if (n != 1 && y == 16) f = 1;
            else if (n != 2 && z == 0) f = 2;
            else if (n != 2 && z == 16) f = 3;
            else if (n != 0 && x == 0) f = 4;
            else if (n != 0 && x == 16) f = 5;
            if (f >= 0) out.add(new Port(f, x, y, z));
        }
        return out;
    }

    /** Its two ends (block-local voxels); a knuckle's point twice. */
    public int[][] ends() {
        int k = points.length - 3;
        return new int[][]{{points[0], points[1], points[2]}, {points[k], points[k + 1], points[k + 2]}};
    }

    /** Is (x, y, z) on the centre line (ends included)? */
    public boolean passes(int x, int y, int z) {
        if (isKnuckle()) return points[0] == x && points[1] == y && points[2] == z;
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

    /** Do the two pieces touch: an end of one on the other's line? */
    public boolean touches(CablePart o) {
        for (int[] e : ends()) if (o.passes(e[0], e[1], e[2])) return true;
        for (int[] e : o.ends()) if (passes(e[0], e[1], e[2])) return true;
        return false;
    }

    @Override
    public void write(CompoundTag tag) {
        tag.putString("tier", TIERS[tier].name);
        tag.putInt("face", face);
        tag.putIntArray("pts", points);
        if (burnt != null) tag.putString("burnt", burnt.name());
        if (byConverter) tag.putBoolean("conv", true);
        if (paid >= 0) tag.putInt("paid", paid);
        tag.putDouble("u", u);
        tag.putDouble("t", t);
    }

    static CablePart read(CompoundTag tag) {
        int tier = tierOf(tag.getStringOr("tier", ""));
        if (tier < 0) return null;
        CablePart p = new CablePart(tier, tag.getIntOr("face", 0), tag.getIntArray("pts").orElse(new int[0]));
        if (!p.valid()) return null;
        String b = tag.getStringOr("burnt", "");
        if (!b.isEmpty()) {
            try {
                p.burnt = mods.eln.sim.process.destruct.FailureCause.valueOf(b);
            } catch (IllegalArgumentException e) {
                p.burnt = mods.eln.sim.process.destruct.FailureCause.OVERHEAT;
            }
        }
        p.byConverter = tag.getBooleanOr("conv", false);
        p.paid = tag.getIntOr("paid", -1);
        p.u = tag.getDoubleOr("u", 0);
        p.t = tag.getDoubleOr("t", 0);
        return p;
    }

    @Override
    public void serialize(java.io.DataOutputStream out) throws java.io.IOException {
        out.writeByte(tier | (burnt != null ? 0x80 : 0));
        out.writeByte(face);
        out.writeByte(points.length / 3);
        for (int v : points) out.writeByte(v);
    }

    static CablePart deserialize(java.io.DataInputStream in) throws java.io.IOException {
        int b = in.readByte();
        int face = in.readByte();
        int n = in.readByte();
        int[] pts = new int[3 * n];
        for (int i = 0; i < pts.length; i++) pts[i] = in.readByte();
        CablePart p = new CablePart(Math.max(0, Math.min(TIERS.length - 1, b & 0x7F)), face, pts);
        if ((b & 0x80) != 0) p.burnt = mods.eln.sim.process.destruct.FailureCause.OVERHEAT;
        return p;
    }
}
