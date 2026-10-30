package mods.eln.node.voxel;

import java.util.ArrayList;
import java.util.List;

/**
 * Blocks of voxel parts for the geometry tests (board #44): every pipe tier x fitting x mode x orientation x route
 * shape, and cables of every tier and shape. Each case is what one block would hold.
 */
final class VoxelCases {
    private VoxelCases() {
    }

    record Case(String name, List<VoxelPart> parts) {
        @Override
        public String toString() {
            return name;
        }
    }

    static final String[] AXIS = {"x", "y", "z"};

    /** A point with axis a set to v, the others from c. */
    static int[] at(int[] c, int a, int v) {
        int[] p = c.clone();
        p[a] = v;
        return p;
    }

    static int[] cat(int[]... ps) {
        int n = 0;
        for (int[] p : ps) n += p.length;
        int[] out = new int[n];
        int i = 0;
        for (int[] p : ps) for (int v : p) out[i++] = v;
        return out;
    }

    /** Route shapes for a tier: straight, capped, bends, an S, off-centre. */
    static List<Case> routes(int tier) {
        String t = PipePart.TIER_NAME[tier];
        List<Case> out = new ArrayList<>();
        int[] c = {8, 8, 8};
        for (int a = 0; a < 3; a++) {
            out.add(new Case(t + " straight " + AXIS[a], List.of(new PipePart(tier, cat(at(c, a, 0), at(c, a, 16))))));
            out.add(new Case(t + " capped " + AXIS[a], List.of(new PipePart(tier, cat(at(c, a, 0), c)))));
            for (int b = 0; b < 3; b++)
                if (b != a)
                    out.add(new Case(t + " bend " + AXIS[a] + AXIS[b], List.of(new PipePart(tier, cat(at(c, a, 0), c, at(c, b, 16))))));
            int o = (a + 1) % 3;
            int[] p0 = at(c, a, 0), p1 = at(c, a, 6), p2 = at(p1, o, 12), p3 = at(p2, a, 16);
            out.add(new Case(t + " S " + AXIS[a] + AXIS[o], List.of(new PipePart(tier, cat(p0, p1, p2, p3)))));
            int[] off = at(at(c, o, 4), (a + 2) % 3, 12);
            out.add(new Case(t + " off-centre " + AXIS[a], List.of(new PipePart(tier, cat(at(off, a, 0), at(off, a, 16))))));
        }
        return out;
    }

    /** The outlets (3D values) across a pipe along axis a. */
    static int[] outlets(int a) {
        // 3D values: 0 down 1 up (y), 2 north 3 south (z), 4 west 5 east (x)
        return a == 0 ? new int[]{0, 1, 2, 3} : a == 1 ? new int[]{2, 3, 4, 5} : new int[]{0, 1, 4, 5};
    }

    /** Fittings: safety valves and sensors on straight pipes (middle, and near the block's edge), valves, adapters. */
    static List<Case> fittings(int tier) {
        String t = PipePart.TIER_NAME[tier];
        int bore = PipePart.GRID[tier];
        List<Case> out = new ArrayList<>();
        for (int a = 0; a < 3; a++)
            for (int outlet : outlets(a))
                for (int pos : new int[]{4, 8, 12}) {
                    // the pipe along a, its centre line at pos on the outlet's axis (lattice points 4, 8, 12, as
                    // VoxelNode.addSafetyValveNear and addSensorNear place them; cut-off boxes are refused there too)
                    int[] c = {8, 8, 8};
                    int oa = outlet / 2 == 0 ? 1 : outlet / 2 == 1 ? 2 : 0;
                    c[oa] = pos;
                    PipePart pipe = new PipePart(tier, cat(at(c, a, 0), at(c, a, 16)));
                    String where = " on " + AXIS[a] + " outlet " + outlet + " at " + pos;
                    SafetyValvePart sv = new SafetyValvePart(c[0], c[1], c[2], outlet, bore, 10e5);
                    if (whole(sv.boxes().get(0), 4)) out.add(new Case(t + " safety valve" + where, List.of(pipe, sv)));
                    for (int s : new int[]{PipeSensorPart.PRESSURE, PipeSensorPart.TEMPERATURE, PipeSensorPart.QUALITY}) {
                        PipeSensorPart ps = new PipeSensorPart(c[0], c[1], c[2], outlet, bore, s, 1);
                        ps.shown = 0.5;
                        if (whole(ps.boxes().get(0), 3)) out.add(new Case(t + " sensor " + s + where, List.of(pipe, ps)));
                    }
                }
        int[] c = {8, 8, 8};
        for (int a = 0; a < 3; a++)
            for (int mode : new int[]{ValvePart.ON_OFF, ValvePart.CONTROL, ValvePart.THROTTLE, ValvePart.PRESSURE})
                for (int variant = 0; variant < 2; variant++) {
                    ValvePart v = new ValvePart(8, 8, 8, a, bore, mode);
                    v.open = variant == 0;
                    v.setting = variant == 0 ? 1 : 0.3;
                    v.pmode = variant == 0 ? ValvePart.RELIEF : ValvePart.REDUCING;
                    v.flow = variant == 0 ? 1 : -1;
                    out.add(new Case(t + " valve mode " + mode + " " + variant + " on " + AXIS[a],
                        List.of(new PipePart(tier, cat(at(c, a, 0), c)), new PipePart(tier, cat(c, at(c, a, 16))), v)));
                }
        for (int face = 0; face < 6; face++)
            out.add(new Case(t + " adapter " + face, List.of(new AdapterPart(face), new PipePart(tier, AdapterPart.stub(face)))));
        return out;
    }

    /** Not cut off by the block's edge: at least n voxels on every axis (the placers' test). */
    static boolean whole(int[] b, int n) {
        return b[3] - b[0] >= n && b[4] - b[1] >= n && b[5] - b[2] >= n;
    }

    static List<Case> cables() {
        List<Case> out = new ArrayList<>();
        for (int tier = 0; tier < CablePart.TIERS.length; tier++) {
            String t = "cable " + CablePart.TIERS[tier].name();
            for (int face = 0; face < 6; face++) {
                int n = CablePart.normalAxis(face);
                int[] dirs = n == 1 ? new int[]{2, 3, 4, 5} : n == 2 ? new int[]{0, 1, 4, 5} : new int[]{0, 1, 2, 3};
                out.add(new Case(t + " straight on " + face, new ArrayList<>(CablePart.arms(tier, face, dirs[0], dirs[1]))));
                out.add(new Case(t + " bend on " + face, new ArrayList<>(CablePart.arms(tier, face, dirs[0], dirs[2]))));
                out.add(new Case(t + " tee on " + face, new ArrayList<>(CablePart.arms(tier, face, dirs[0], dirs[1], dirs[2]))));
                out.add(new Case(t + " cross on " + face, new ArrayList<>(CablePart.arms(tier, face, dirs))));
            }
            out.add(new Case(t + " knuckle", List.of(CablePart.knuckle(tier, 0, 0, 8))));
        }
        return out;
    }

    static List<Case> all() {
        List<Case> out = new ArrayList<>();
        for (int tier = 0; tier < PipePart.TIER_NAME.length; tier++) {
            out.addAll(routes(tier));
            out.addAll(fittings(tier));
        }
        out.addAll(cables());
        return out;
    }
}
