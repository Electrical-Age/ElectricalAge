package mods.eln.node.voxel;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The geometric half of the six-node -> voxel cable conversion (CableConverter.plan decides which edges a cable is
 * linked through, from the world; CablePart.arms lays the pieces). Whatever the edges and their order: the pieces
 * are valid, form one conductor (CableNet joins touching pieces of a face), open exactly at the middles of the
 * requested edges, and two cables on perpendicular faces of a block touch (CableNet's inner-corner join) exactly
 * when each was linked toward the other, as the six-node inner corner was.
 */
public class CableArmsPropertyTest {

    /** The four directions (3D values) along a face. */
    static List<Integer> along(int face) {
        List<Integer> out = new ArrayList<>();
        for (int d = 0; d < 6; d++) if (d / 2 != face / 2) out.add(d);
        return out;
    }

    static int[] dirs(int face, int mask, long seed) {
        List<Integer> a = along(face), out = new ArrayList<>();
        for (int i = 0; i < 4; i++) if ((mask & 1 << i) != 0) out.add(a.get(i));
        Collections.shuffle(out, new Random(seed));
        return out.stream().mapToInt(i -> i).toArray();
    }

    static boolean connected(List<CablePart> parts) {
        int n = parts.size();
        boolean[] seen = new boolean[n];
        seen[0] = true;
        boolean grew = true;
        while (grew) {
            grew = false;
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    if (seen[i] && !seen[j] && parts.get(i).touches(parts.get(j))) seen[j] = grew = true;
        }
        for (boolean s : seen) if (!s) return false;
        return true;
    }

    @Property(tries = 300)
    void armsAreOneConductorOpenAtTheLinkedEdges(@ForAll @IntRange(min = 0, max = 5) int tier,
                                                 @ForAll @IntRange(min = 0, max = 5) int face,
                                                 @ForAll @IntRange(min = 1, max = 15) int mask, @ForAll long seed) {
        int[] dirs = dirs(face, mask, seed);
        List<CablePart> parts = CablePart.arms(tier, face, dirs);
        String ctx = "tier " + tier + " face " + face + " dirs " + java.util.Arrays.toString(dirs);
        for (CablePart p : parts) assertTrue(p.valid(), ctx + ": invalid " + java.util.Arrays.toString(p.points));
        assertTrue(connected(parts), ctx + ": not one conductor");
        int[] c = {8, 8, 8};
        c[CablePart.normalAxis(face)] = CablePart.plane(face);
        // the ends on the block's boundary are exactly the requested edges' middles
        List<String> open = new ArrayList<>(), want = new ArrayList<>();
        for (CablePart p : parts)
            for (int e = 0; e < p.points.length; e += p.points.length - 3) {
                int[] q = {p.points[e], p.points[e + 1], p.points[e + 2]};
                if (CablePart.boundaryAxes(q[0], q[1], q[2]) == 2) open.add(java.util.Arrays.toString(q));
            }
        for (int d : dirs) want.add(java.util.Arrays.toString(CablePart.edge(c, d)));
        Collections.sort(open);
        Collections.sort(want);
        assertEquals(want, open, ctx);
    }

    @Property(tries = 500)
    void perpendicularCablesTouchExactlyWhenLinkedBothWays(@ForAll @IntRange(min = 0, max = 4) int tier,
                                                          @ForAll @IntRange(min = 0, max = 5) int f,
                                                          @ForAll @IntRange(min = 0, max = 3) int dPick,
                                                          @ForAll @IntRange(min = 1, max = 15) int maskF,
                                                          @ForAll @IntRange(min = 1, max = 15) int maskD, @ForAll long seed) {
        int d = along(f).get(dPick);
        int[] dirsF = dirs(f, maskF, seed), dirsD = dirs(d, maskD, ~seed);
        boolean fToD = java.util.Arrays.stream(dirsF).anyMatch(x -> x == d), dToF = java.util.Arrays.stream(dirsD).anyMatch(x -> x == f);
        List<CablePart> a = CablePart.arms(tier, f, dirsF), b = CablePart.arms(tier, d, dirsD);
        boolean touch = false;
        for (CablePart p : a) for (CablePart q : b) touch |= p.touches(q);
        assertEquals(fToD && dToF, touch, "faces " + f + "/" + d + " dirs " + java.util.Arrays.toString(dirsF) + " / "
            + java.util.Arrays.toString(dirsD));
    }

    /** Every outer-corner knuckle point (cable direction d, cable face f) is a valid knuckle on an edge of the wrap block. */
    @Property(tries = 50)
    void knucklesSitOnAnEdge(@ForAll @IntRange(min = 0, max = 5) int f, @ForAll @IntRange(min = 0, max = 3) int dPick) {
        mods.eln.misc.Direction fd = null, dd = null;
        int d = along(f).get(dPick);
        for (mods.eln.misc.Direction x : mods.eln.misc.Direction.values()) {
            if (x.toEnumFacing().get3DDataValue() == f) fd = x;
            if (x.toEnumFacing().get3DDataValue() == d) dd = x;
        }
        int[] k = CableConverter.knucklePoint(dd, fd);
        assertTrue(CablePart.knuckle(1, k[0], k[1], k[2]).valid(), "f " + f + " d " + d + ": " + java.util.Arrays.toString(k));
    }
}
