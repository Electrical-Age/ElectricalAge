package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a device placed anywhere goes in a voxel block that already has cables (AdventurAgent: with a cable across the
 * floor, every place and facing was refused; only device first, then cables, worked). Aimed at a cable, in this order:
 * <ul>
 * <li>in line: a device with two opposite pins of the cable's kind (a relay's contacts, a probe's, a LED switch's) sits on
 * the cable, turned so those pins lie along it, and the cable is cut where the device and its pins go; the two cut ends
 * meet the pins. The cut-out length comes back as a cable kit (cable is neither made nor lost);</li>
 * <li>at a cable's free end: one of its pins of the cable's kind on that end;</li>
 * <li>beside the cable: one of its pins of the cable's kind touching the cable's side (a tap);</li>
 * </ul>
 * and else where the click is, as before. A device in line, once broken, leaves the gap: the two ends stay where they are.
 * The front follows the player where the cable leaves a choice (the facing is tried first, then turned clockwise).
 */
public final class DevicePlacer {
    private DevicePlacer() {
    }

    /** A cable cut for a device in line: the piece at index replaced by a and b (either may be null), `returned` voxels
     *  of cable back to the player. */
    public record Cut(int index, CablePart a, CablePart b, int returned) {}

    /** Where it goes: offset, front, a cut (null if none), or why not (err, the rest unset). */
    public record Plan(int ox, int oy, int oz, LRDU front, Cut cut, String err) {
        static Plan no(String err) {
            return new Plan(0, 0, 0, null, null, err);
        }
    }

    /** Said when nothing near a cable fits. */
    static final String HINT = "; aim along a cable of its kind to put it in line, at a cable's end, or just beside a cable"
        + " with a pin toward it, or place the device first and lay cable to its pins";

    /** A straight piece of a cable on the device's face, near the click. */
    private record Seg(int part, CablePart c, int k, int axis, int s0, int s1, int[] line, double dist) {}

    /**
     * Plan a device of this stack on face (its slot `slot` past the faces) for a click at the block-local point h. The
     * plain place (snapped at the click, the player's facing) is used unless the click is on a cable or that place is
     * refused next to one.
     */
    public static Plan plan(VoxelNode v, ItemStack stack, int slot, Direction face, double[] h, Player player) {
        SixNodeDescriptor d = Eln.sixNodeItem.getDescriptor(stack);
        LRDU front0 = d.getFrontFromPlace(face, player);
        int[] o = VoxelNode.snap(d, face, front0, h[0], h[1], h[2]);
        Seg seg = nearest(v, face, h);
        boolean onCable = seg != null && seg.dist <= seg.c.tierInfo().width() / 2.0 + 1;
        if (!onCable) {
            String err = v.placeErrorAt(stack, slot, face, o[0], o[1], o[2], front0);
            if (err == null) return new Plan(o[0], o[1], o[2], front0, null, null);
            if (seg == null || seg.dist > 6) return Plan.no(err);
        }
        List<LRDU> fronts = new ArrayList<>();
        for (LRDU f = front0; fronts.size() < 4; f = f.getNextClockwise()) fronts.add(f);
        int[][] masks = new int[4][];
        for (int i = 0; i < 4; i++) masks[i] = masks(v, stack, slot, face, fronts.get(i));
        Plan p = inline(v, d, stack, slot, face, h, seg, fronts, masks);
        if (p == null) p = atEnd(v, d, stack, slot, face, h, fronts, masks);
        if (p == null) p = tap(v, d, stack, slot, face, h, seg, fronts, masks);
        if (p != null) return p;
        if (onCable) {
            String err = v.placeErrorAt(stack, slot, face, o[0], o[1], o[2], front0);
            if (err == null) return new Plan(o[0], o[1], o[2], front0, null, null);
            return Plan.no(err + HINT);
        }
        return Plan.no(v.placeErrorAt(stack, slot, face, o[0], o[1], o[2], front0) + HINT);
    }

    /** The connection masks of the device turned to front, toward each edge (LRDU.toInt()). */
    static int[] masks(VoxelNode v, ItemStack stack, int slot, Direction face, LRDU front) {
        var e = v.standIn(stack, slot, face, 0, 0, 0, front);
        int[] m = new int[4];
        if (e == null) return m;
        try {
            for (LRDU l : LRDU.values()) m[l.toInt()] = e.getConnectionMask(l);
        } finally {
            e.unload();
        }
        return m;
    }

    static int cableMask(CablePart c) {
        var cd = c.tierInfo().descriptor();
        return cd != null ? cd.getNodeMask() : NodeBase.maskThermalWire;
    }

    /** The edge of face pointing along axis a, toward + (pos) or -. */
    static LRDU edge(Direction face, int a, boolean pos) {
        for (LRDU l : LRDU.values()) {
            int dv = face.applyLRDU(l).toEnumFacing().get3DDataValue();
            if (CablePart.normalAxis(dv) == a && (dv % 2 == 1) == pos) return l;
        }
        return null;
    }

    /** The nearest straight piece of a working cable lying on the device's face, to the click (voxels, in the plane). */
    static Seg nearest(VoxelNode v, Direction face, double[] h) {
        int f3 = face.toEnumFacing().get3DDataValue(), n = CablePart.normalAxis(f3);
        double[] q = {h[0] * 16, h[1] * 16, h[2] * 16};
        Seg best = null;
        for (int i = 0; i < v.parts.size(); i++) {
            if (!(v.parts.get(i) instanceof CablePart c) || c.isKnuckle() || c.face != f3 || c.burnt != null) continue;
            int[] p = c.points;
            for (int k = 0; k + 5 < p.length; k += 3) {
                int a = -1;
                for (int x = 0; x < 3; x++) if (p[k + x] != p[k + 3 + x]) a = x;
                if (a < 0 || a == n) continue;
                int s0 = Math.min(p[k + a], p[k + 3 + a]), s1 = Math.max(p[k + a], p[k + 3 + a]);
                int b = 3 - n - a;
                double along = Math.max(0, Math.max(s0 - q[a], q[a] - s1));
                double dist = Math.hypot(along, q[b] - p[k + b]);
                if (best == null || dist < best.dist)
                    best = new Seg(i, c, k / 3, a, s0, s1, new int[]{p[k], p[k + 1], p[k + 2]}, dist);
            }
        }
        return best;
    }

    /** Try it at offset o turned to front, with the cut made for the check; null if it fits there. */
    static String tryAt(VoxelNode v, ItemStack stack, int slot, Direction face, int[] o, LRDU front, Cut cut) {
        if (cut == null) return v.placeErrorAt(stack, slot, face, o[0], o[1], o[2], front);
        CablePart old = (CablePart) v.parts.get(cut.index());
        apply(v, cut);
        try {
            return v.placeErrorAt(stack, slot, face, o[0], o[1], o[2], front);
        } finally {
            unapply(v, cut, old);
        }
    }

    /** The cut's pieces in place of the old one (the parts and the used voxels only: no rebuild). */
    static void apply(VoxelNode v, Cut cut) {
        v.parts.remove(cut.index());
        int at = cut.index();
        if (cut.b() != null) v.parts.add(at, cut.b());
        if (cut.a() != null) v.parts.add(at, cut.a());
        v.markUsedForPlacer();
    }

    static void unapply(VoxelNode v, Cut cut, CablePart old) {
        if (cut.a() != null) v.parts.remove(cut.a());
        if (cut.b() != null) v.parts.remove(cut.b());
        v.parts.add(cut.index(), old);
        v.markUsedForPlacer();
    }

    /** A device with two opposite pins of the cable's kind, on the cable, those pins along it. */
    static Plan inline(VoxelNode v, SixNodeDescriptor d, ItemStack stack, int slot, Direction face, double[] h, Seg seg,
                       List<LRDU> fronts, int[][] masks) {
        if (seg == null) return null;
        int a = seg.axis, n = CablePart.normalAxis(face.toEnumFacing().get3DDataValue()), b = 3 - n - a;
        int want = cableMask(seg.c);
        LRDU lp = edge(face, a, true), ln = edge(face, a, false);
        for (int i = 0; i < 4; i++) {
            int mp = masks[i][lp.toInt()], mn = masks[i][ln.toInt()];
            if (mp == 0 || mp != mn || !NodeBase.compareConnectionMask(mp, want)) continue;
            LRDU front = fronts.get(i);
            int[] ext = d.bodyExtents(face, front);
            // a cut end on the block's side keeps 2 voxels of cable: a pin there would face the next block, which
            // only another voxel block's pin or cable meets
            int lo = seg.s0 + (seg.s0 == 0 ? 2 : 0) + ext[ln.toInt()] + 1, hi = seg.s1 - (seg.s1 == 16 ? 2 : 0) - ext[lp.toInt()] - 1;
            if (lo > hi) continue;
            int c = Math.max(lo, Math.min(hi, 2 * (int) Math.round(h[a] * 8)));
            int[] o = new int[3];
            o[a] = c - 8;
            o[b] = seg.line[b] - 8;
            Cut cut = cut(seg, c - ext[ln.toInt()] - 1, c + ext[lp.toInt()] + 1);
            if (cut == null) continue;
            if (tryAt(v, stack, slot, face, o, front, cut) == null) return new Plan(o[0], o[1], o[2], front, cut, null);
        }
        return null;
    }

    /** The cable of seg with the stretch between tipLo and tipHi (along its axis) taken out. */
    static Cut cut(Seg seg, int tipLo, int tipHi) {
        CablePart c = seg.c;
        int[] p = c.points;
        int k = seg.k * 3, a = seg.axis;
        boolean up = p[k + a] < p[k + 3 + a];   // (the piece runs toward +a)
        int near = up ? tipLo : tipHi, far = up ? tipHi : tipLo;
        List<Integer> first = new ArrayList<>(), second = new ArrayList<>();
        for (int i = 0; i <= k + 2; i++) first.add(p[i]);
        if (near != p[k + a]) for (int x = 0; x < 3; x++) first.add(x == a ? near : p[k + x]);
        if (far != p[k + 3 + a]) for (int x = 0; x < 3; x++) second.add(x == a ? far : p[k + 3 + x]);
        for (int i = k + 3; i < p.length; i++) second.add(p[i]);
        CablePart pa = first.size() >= 6 ? new CablePart(c.tier, c.face, first.stream().mapToInt(Integer::intValue).toArray()) : null;
        CablePart pb = second.size() >= 6 ? new CablePart(c.tier, c.face, second.stream().mapToInt(Integer::intValue).toArray()) : null;
        if (pa != null && !pa.valid() || pb != null && !pb.valid()) return null;
        int len = c.length(), la = pa == null ? 0 : pa.length(), lb = pb == null ? 0 : pb.length();
        int returned;
        if (c.paid < 0) returned = len - la - lb;   // (each keeps its length's worth)
        else {   // (a converted cable paid for less than its length: its share goes with each piece)
            if (pa != null) pa.paid = c.paid * la / len;
            if (pb != null) pb.paid = c.paid * lb / len;
            returned = c.paid - (pa == null ? 0 : pa.paid) - (pb == null ? 0 : pb.paid);
        }
        for (CablePart x : new CablePart[]{pa, pb})
            if (x != null) {
                x.u = c.u;
                x.t = c.t;
            }
        return new Cut(seg.part, pa, pb, returned);
    }

    /** One pin of the cable's kind on a free end of a cable on the face, near the click. */
    static Plan atEnd(VoxelNode v, SixNodeDescriptor d, ItemStack stack, int slot, Direction face, double[] h,
                      List<LRDU> fronts, int[][] masks) {
        int f3 = face.toEnumFacing().get3DDataValue(), n = CablePart.normalAxis(f3);
        double[] q = {h[0] * 16, h[1] * 16, h[2] * 16};
        for (VoxelPart part : v.parts) {
            if (!(part instanceof CablePart c) || c.isKnuckle() || c.face != f3 || c.burnt != null) continue;
            int[] p = c.points;
            for (int end = 0; end < 2; end++) {
                int e = end == 0 ? 0 : p.length - 3, f = end == 0 ? 3 : p.length - 6;
                int a = -1;
                for (int x = 0; x < 3; x++) if (p[e + x] != p[f + x]) a = x;
                if (a < 0 || p[e + a] == 0 || p[e + a] == 16) continue;   // (an end on the block's side opens there)
                if (Math.abs(q[0] - p[e]) + Math.abs(q[1] - p[e + 1]) + Math.abs(q[2] - p[e + 2]) > 5) continue;
                boolean out = p[e + a] > p[f + a];   // (the cable leaves toward +a at this end)
                LRDU back = edge(face, a, !out);   // (a pin pointing back at the cable)
                int b = 3 - n - a;
                for (int i = 0; i < 4; i++) {
                    if (!NodeBase.compareConnectionMask(masks[i][back.toInt()], cableMask(c)) || masks[i][back.toInt()] == 0) continue;
                    LRDU front = fronts.get(i);
                    int ext = d.bodyExtents(face, front)[back.toInt()];
                    int[] o = new int[3];
                    o[a] = p[e + a] + (out ? ext + 1 : -ext - 1) - 8;
                    o[b] = p[e + b] - 8;
                    if (tryAt(v, stack, slot, face, o, front, null) == null) return new Plan(o[0], o[1], o[2], front, null, null);
                }
            }
        }
        return null;
    }

    /** One pin of the cable's kind touching the side of the cable, on the click's side first. */
    static Plan tap(VoxelNode v, SixNodeDescriptor d, ItemStack stack, int slot, Direction face, double[] h, Seg seg,
                    List<LRDU> fronts, int[][] masks) {
        if (seg == null) return null;
        int a = seg.axis, n = CablePart.normalAxis(face.toEnumFacing().get3DDataValue()), b = 3 - n - a;
        int w = seg.c.tierInfo().width(), line = seg.line[b];
        int boxLo = line - w / 2, boxHi = line + (w - w / 2);
        boolean lowFirst = h[b] * 16 < line;
        for (boolean low : new boolean[]{lowFirst, !lowFirst}) {
            LRDU toward = edge(face, b, low);   // (from the low side the pin points +b at the cable)
            for (int i = 0; i < 4; i++) {
                int m = masks[i][toward.toInt()];
                if (m == 0 || !NodeBase.compareConnectionMask(m, cableMask(seg.c))) continue;
                LRDU front = fronts.get(i);
                int[] ext = d.bodyExtents(face, front);
                int tip = low ? boxLo : boxHi;
                int cb = low ? tip - ext[toward.toInt()] - 1 : tip + ext[toward.toInt()] + 1;
                int lo = Math.max(seg.s0 + 1, ext[edge(face, a, false).toInt()] + 1), hi = Math.min(seg.s1 - 1, 15 - ext[edge(face, a, true).toInt()]);
                if (lo > hi) continue;
                int ca = Math.max(lo, Math.min(hi, 2 * (int) Math.round(h[a] * 8)));
                int[] o = new int[3];
                o[a] = ca - 8;
                o[b] = cb - 8;
                if (tryAt(v, stack, slot, face, o, front, null) == null) return new Plan(o[0], o[1], o[2], front, null, null);
            }
        }
        return null;
    }
}
