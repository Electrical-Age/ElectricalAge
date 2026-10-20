package mods.eln.node.voxel;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property tests of routed cable (Baughn 2026-10-20: "start with property tests to catch all these"): on random little
 * worlds of whole blocks and slabs, every route the router returns
 * <ul>
 *   <li>has only valid parts, none inside a solid voxel (a cable was seen phasing through a floor),</li>
 *   <li>is one unbroken run: each piece's boxes share a face with the next piece's (a voxel was missing at the outer
 *       corner onto a top slab's underside, so the corner only touched along an edge).</li>
 * </ul>
 */
public class CableRoutePropertyTest {
    static final int LV = 1;
    static final int SX = 7, SY = 4, SZ = 7;

    /** Per block: 0 air, 1 full, 2 bottom slab (y 0..8), 3 top slab (y 8..16). Block y 0 is a full floor. */
    static int[][][] world(long seed) {
        Random r = new Random(seed);
        int[][][] w = new int[SX][SY][SZ];
        for (int x = 0; x < SX; x++)
            for (int z = 0; z < SZ; z++) {
                w[x][0][z] = 1;
                for (int y = 1; y < SY; y++) {
                    int roll = r.nextInt(12);
                    w[x][y][z] = roll < 7 ? 0 : roll < 9 ? 1 : roll < 10 ? 2 : 3;
                }
            }
        return w;
    }

    /** Voxel (world voxel coordinates) solid? Outside the world: air (and not supporting). */
    static boolean voxel(int[][][] w, long vx, long vy, long vz) {
        long bx = Math.floorDiv(vx, 16), by = Math.floorDiv(vy, 16), bz = Math.floorDiv(vz, 16);
        if (bx < 0 || by < 0 || bz < 0 || bx >= SX || by >= SY || bz >= SZ) return false;
        int k = w[(int) bx][(int) by][(int) bz];
        int ly = (int) Math.floorMod(vy, 16);
        return k == 1 || k == 2 && ly < 8 || k == 3 && ly >= 8;
    }

    static CableRouter.Space space(int[][][] w) {
        return new CableRouter.Space() {
            @Override
            public boolean blocked(long x0, long y0, long z0, long x1, long y1, long z1) {
                // (outside the little world it's blocked: the router stays inside)
                if (x0 < 0 || y0 < 0 || z0 < 0 || x1 > SX * 16L || y1 > SY * 16L + 32 || z1 > SZ * 16L) return true;
                for (long x = x0; x < x1; x++)
                    for (long y = y0; y < y1; y++)
                        for (long z = z0; z < z1; z++) if (voxel(w, x, y, z)) return true;
                return false;
            }

            @Override
            public boolean solid(long x0, long y0, long z0, long x1, long y1, long z1) {
                for (long x = x0; x < x1; x++)
                    for (long y = y0; y < y1; y++)
                        for (long z = z0; z < z1; z++) if (!voxel(w, x, y, z)) return false;
                return true;
            }
        };
    }

    /** A point on the top surface of column (bx, bz), at its centre: the highest solid voxel's top. */
    static CableRouter.End top(int[][][] w, int bx, int bz) {
        long vx = bx * 16L + 8, vz = bz * 16L + 8;
        for (long vy = SY * 16L - 1; vy >= 0; vy--)
            if (voxel(w, vx, vy, vz)) return new CableRouter.End(vx, vy + 1, vz, 0, -1);
        return null;
    }

    /** A cable part's boxes in world voxels. */
    static List<long[]> boxes(int tier, CableRouter.Piece p) {
        List<long[]> out = new ArrayList<>();
        CablePart part = p.face() < 0 ? CablePart.knuckle(tier, p.points()[0], p.points()[1], p.points()[2]) : new CablePart(tier, p.face(), p.points());
        for (int[] b : part.boxes())
            out.add(new long[]{p.bx() * 16 + b[0], p.by() * 16 + b[1], p.bz() * 16 + b[2], p.bx() * 16 + b[3], p.by() * 16 + b[4], p.bz() * 16 + b[5]});
        return out;
    }

    static String boxesText(int tier, CableRouter.Piece p) {
        StringBuilder sb = new StringBuilder();
        for (long[] b : boxes(tier, p)) sb.append(java.util.Arrays.toString(b));
        return sb.toString();
    }

    /** Do two boxes share a face (overlap on two axes with positive length, touch or overlap on the third)? */
    static boolean faceTouch(long[] a, long[] b) {
        int over = 0, touch = 0;
        for (int k = 0; k < 3; k++) {
            long lo = Math.max(a[k], b[k]), hi = Math.min(a[k + 3], b[k + 3]);
            if (hi > lo) over++;
            else if (hi == lo) touch++;
            else return false;
        }
        return over == 3 || over == 2 && touch == 1;
    }

    @Property(tries = 1000)
    void routesAreWholeAndOutOfTheWay(@ForAll long seed, @ForAll @IntRange(min = 0, max = 4) int tier, @ForAll @IntRange(min = 0, max = SX - 1) int ax, @ForAll @IntRange(min = 0, max = SZ - 1) int az,
                                      @ForAll @IntRange(min = 0, max = SX - 1) int bx, @ForAll @IntRange(min = 0, max = SZ - 1) int bz) {
        int[][][] w = world(seed);
        CableRouter.End a = top(w, ax, az), b = top(w, bx, bz);
        if (a == null || b == null || ax == bx && az == bz) return;
        CablePart.Tier t = CablePart.TIERS[tier];
        CableRouter.Result r = CableRouter.route(space(w), t.width(), t.height(), a, List.of(), b, 2000);
        if (r.error != null) return;   // (no route is allowed; a wrong one isn't)
        List<CableRouter.Piece> ps = CableRouter.pieces(r.steps, t.width(), t.height());
        for (CableRouter.Piece p : ps) {
            CablePart part = p.face() < 0 ? CablePart.knuckle(tier, p.points()[0], p.points()[1], p.points()[2]) : new CablePart(tier, p.face(), p.points());
            assertTrue(part.valid(), "invalid piece " + p + " " + java.util.Arrays.toString(p.points()));
            for (long[] q : boxes(tier, p))
                for (long x = q[0]; x < q[3]; x++)
                    for (long y = q[1]; y < q[4]; y++)
                        for (long z = q[2]; z < q[5]; z++)
                            assertFalse(voxel(w, x, y, z), () -> "a piece inside a solid voxel: " + p + " at " + java.util.Arrays.toString(q));
        }
        for (int i = 1; i < ps.size(); i++) {
            boolean touch = false;
            for (long[] p : boxes(tier, ps.get(i - 1)))
                for (long[] q : boxes(tier, ps.get(i))) touch |= faceTouch(p, q);
            final int k = i;
            assertTrue(touch, () -> "tier " + tier + ": a gap between pieces " + (k - 1) + " and " + k + ": " + ps.get(k - 1) + " "
                + java.util.Arrays.toString(ps.get(k - 1).points()) + " " + boxesText(tier, ps.get(k - 1)) + " / " + ps.get(k) + " "
                + java.util.Arrays.toString(ps.get(k).points()) + " " + boxesText(tier, ps.get(k)) + "; steps " + r.steps);
        }
    }
}
