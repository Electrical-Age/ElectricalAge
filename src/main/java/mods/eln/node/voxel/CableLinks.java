package mods.eln.node.voxel;

import java.util.Arrays;
import java.util.List;

/**
 * The joints a cable piece keeps (CablePart.joints): what each of its ends, or its body, connects to. Each joint is an
 * int array {kind, end, ...}; end is 0 or 1 for the piece's ends (a knuckle: the index of its port), -1 for the body.
 * <ul>
 * <li>CONT {kind, end, dx, dy, dz, id, otherEnd}: the next piece of the run, in the block at (dx, dy, dz) from this one.
 * Kept on both pieces.</li>
 * <li>SPLICE {kind, end, dx, dy, dz, id, x, y, z}: this end meets that piece's line at (x, y, z) in that piece's block.
 * Kept on this piece only.</li>
 * <li>TERM_ELEMENT {kind, end, dx, dy, dz, slot, lrdu, offset}: a device hosted in the voxel block at (dx, dy, dz):
 * its slot, edge and the terminal's place along the edge (SixNodeElement.edgePins; 0 for the edge's middle).</li>
 * <li>TERM_NEIGHBOUR {kind, end, dir, lrdu, sub, ...}: the machine in the non-voxel neighbour toward dir: sub
 * TERM_EDGE (EA's edge rule), TERM_BUS (a BusPeer's bus port) or TERM_PIN (a FacePins pin, its leftward follows).</li>
 * <li>TERM_FITTING {kind, -1, x, y, z}: a control valve's or pipe sensor's signal, in this block, at its point.</li>
 * <li>TOUCH {kind, -1, dx, dy, dz, id}: two pieces in face neighbours whose bodies touch across the face though no end
 * meets an end (VoxelNode.connectTouching). Kept on both pieces.</li>
 * </ul>
 * IMPLIED marks a joint recorded from a live connection the rules made, rather than written by the editor that laid the
 * pieces; implied joints are recorded afresh at every reconnect, so they always say what the network had.
 */
public final class CableLinks {
    public static final int CONT = 1, SPLICE = 2, TERM_ELEMENT = 3, TERM_NEIGHBOUR = 4, TERM_FITTING = 5, TOUCH = 6;
    public static final int IMPLIED = 0x100;
    public static final int TERM_EDGE = 0, TERM_BUS = 1, TERM_PIN = 2;
    static final String[] NAMES = {"?", "continue", "splice", "device", "neighbour", "fitting", "touch"};

    private CableLinks() {
    }

    public static int kind(int[] j) {
        return j[0] & 0xff;
    }

    public static boolean implied(int[] j) {
        return (j[0] & IMPLIED) != 0;
    }

    /** The joint's block offset from the piece holding it, or null if it isn't toward a voxel block (TERM_NEIGHBOUR's
     *  dir is a neighbour too, see neighbourDir). */
    static int[] offset(int[] j) {
        return switch (kind(j)) {
            case CONT, SPLICE, TERM_ELEMENT, TOUCH -> new int[]{j[2], j[3], j[4]};
            case TERM_FITTING -> new int[]{0, 0, 0};
            default -> null;
        };
    }

    /** The joint's target piece id (CONT, SPLICE, TOUCH), or -1. */
    static int target(int[] j) {
        return switch (kind(j)) {
            case CONT, SPLICE, TOUCH -> j[5];
            default -> -1;
        };
    }

    /** The end index of point (x, y, z) on c: 0 or 1 (its first or last point), -1 if neither. Knuckles: -1. */
    static int endAt(CablePart c, int x, int y, int z) {
        if (c.isKnuckle()) return -1;
        int[][] e = c.ends();
        for (int i = 0; i < 2; i++) if (e[i][0] == x && e[i][1] == y && e[i][2] == z) return i;
        return -1;
    }

    /** The end index of port p of c: its port index for a knuckle, else as endAt. */
    static int endOf(CablePart c, CablePart.Port p) {
        if (c.isKnuckle()) return c.cablePorts().indexOf(p);
        return endAt(c, p.x(), p.y(), p.z());
    }

    /** Add j to c's joints unless the same joint (implied or not) is there already. */
    static void add(CablePart c, int[] j) {
        for (int[] o : c.joints) if (same(o, j)) return;
        c.joints.add(j);
    }

    static boolean same(int[] a, int[] b) {
        if (a.length != b.length || kind(a) != kind(b)) return false;
        for (int i = 1; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    static int[] implied(int kind, int... rest) {
        int[] j = new int[rest.length + 1];
        j[0] = kind | IMPLIED;
        System.arraycopy(rest, 0, j, 1, rest.length);
        return j;
    }

    /** Record what joins two touching pieces of one block (CableNet): an end of one on the other's line, as a CONT if
     *  it is an end of both, else a SPLICE onto the piece whose line it lies on. Knuckles take body ends (-1). */
    static void recordTouching(CablePart a, CablePart b) {
        boolean any = false;
        any |= recordEnds(a, b);
        any |= recordEnds(b, a);
        if (!any) {   // (a knuckle's point on a line: a splice of the knuckle's body)
            CablePart k = a.isKnuckle() ? a : b, o = k == a ? b : a;
            add(k, implied(SPLICE, -1, 0, 0, 0, o.id, k.points[0], k.points[1], k.points[2]));
        }
    }

    private static boolean recordEnds(CablePart a, CablePart b) {
        if (a.isKnuckle()) return false;
        boolean any = false;
        int[][] e = a.ends();
        for (int i = 0; i < 2; i++) {
            int[] p = e[i];
            if (!b.passes(p[0], p[1], p[2])) continue;
            any = true;
            int be = endAt(b, p[0], p[1], p[2]);
            if (be >= 0) {
                add(a, implied(CONT, i, 0, 0, 0, b.id, be));
                add(b, implied(CONT, be, 0, 0, 0, a.id, i));
            } else add(a, implied(SPLICE, i, 0, 0, 0, b.id, p[0], p[1], p[2]));
        }
        return any;
    }

    /** Record a CONT across a face: a's end ea to b's end eb, b in the block (dx, dy, dz) from a's. */
    static void recordAcross(CablePart a, int ea, CablePart b, int eb, int dx, int dy, int dz) {
        add(a, implied(CONT, ea, dx, dy, dz, b.id, eb));
        add(b, implied(CONT, eb, -dx, -dy, -dz, a.id, ea));
    }

    /** Record bodies touching across a face (TOUCH), b in the block (dx, dy, dz) from a's. */
    static void recordTouch(CablePart a, CablePart b, int dx, int dy, int dz) {
        add(a, implied(TOUCH, -1, dx, dy, dz, b.id));
        add(b, implied(TOUCH, -1, -dx, -dy, -dz, a.id));
    }

    /**
     * Drop the implied joints of pieces that the reconnect of `node` re-records: on its own pieces, all but those toward
     * a neighbour that isn't in the simulation (a frozen or unloaded block keeps its joints); on the pieces of its voxel
     * neighbours that are, those pointing into its block.
     */
    static void clearImplied(VoxelNode node) {
        for (VoxelPart p : node.parts) {
            if (!(p instanceof CablePart c) || c.burnt != null) continue;
            c.joints.removeIf(j -> implied(j) && !towardAbsent(node, j) && !toBurnt(node, j));
        }
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            var other = node.getNeighbor(mods.eln.misc.Direction.fromEnumFacing(d));
            if (!(other instanceof VoxelNode v) || !other.isAdded()) continue;
            int dx = -d.getStepX(), dy = -d.getStepY(), dz = -d.getStepZ();
            for (VoxelPart p : v.parts) {
                if (!(p instanceof CablePart c) || c.burnt != null) continue;
                c.joints.removeIf(j -> {
                    if (!implied(j) || toBurnt(v, j)) return false;
                    int[] o = offset(j);
                    return o != null && o[0] == dx && o[1] == dy && o[2] == dz;
                });
            }
        }
    }

    /** Is the joint's target piece burnt? (Burnt pieces are out of the network until repaired in place, so their joints
     *  aren't re-recorded meanwhile: they are kept, on both sides.) */
    private static boolean toBurnt(VoxelNode node, int[] j) {
        int id = target(j);
        if (id < 0) return false;
        int[] o = offset(j);
        VoxelNode at = node;
        if (o[0] != 0 || o[1] != 0 || o[2] != 0) {
            var other = node.getNeighbor(mods.eln.misc.Direction.fromEnumFacing(net.minecraft.core.Direction.getNearest(o[0], o[1], o[2], null)));
            if (!(other instanceof VoxelNode v)) return false;
            at = v;
        }
        return at.partById(id) instanceof CablePart c && c.burnt != null;
    }

    private static boolean towardAbsent(VoxelNode node, int[] j) {
        mods.eln.node.NodeBase other;
        if (kind(j) == TERM_NEIGHBOUR) other = node.getNeighbor(mods.eln.misc.Direction.fromInt(j[2]));
        else {
            int[] o = offset(j);
            if (o == null || (o[0] == 0 && o[1] == 0 && o[2] == 0)) return false;
            other = node.getNeighbor(mods.eln.misc.Direction.fromEnumFacing(net.minecraft.core.Direction.getNearest(o[0], o[1], o[2], null)));
        }
        return other == null || !other.isAdded();
    }

    /** A joint as text for reports: its kind, end and fields. */
    public static String describe(int[] j) {
        int k = kind(j);
        return (implied(j) ? "implied " : "") + (k < NAMES.length ? NAMES[k] : "kind" + k) + " end " + j[1] + " "
            + Arrays.toString(Arrays.copyOfRange(j, 2, j.length));
    }

    /** Count of joints by kind (index kind, plus kind + 8 for implied ones) over these pieces. */
    static int[] counts(List<CablePart> pieces) {
        int[] n = new int[16];
        for (CablePart c : pieces) for (int[] j : c.joints) n[Math.min(7, kind(j)) + (implied(j) ? 8 : 0)]++;
        return n;
    }
}
