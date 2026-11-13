package mods.eln.node.voxel;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.AfterContainer;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property tests of cable air runs (Baughn 2026-11-11, D5: up to 1.2 blocks off any support, never further) on random
 * little worlds of islands, pillars, ledges and slabs over nothing, where many routes can only be made through the air.
 * Every route the router returns has only valid pieces, none inside a solid voxel, unbroken from piece to piece, and no
 * stretch off a support longer than the kind's limit; enough of them do cross air that the test means something.
 */
public class CableAirPropertyTest {
    static final int SX = 7, SY = 4, SZ = 7;
    static final AtomicInteger routes = new AtomicInteger(), airy = new AtomicInteger();

    /** Per block as CableRoutePropertyTest.world, but the floor (y 0) has holes: islands and gaps. */
    static int[][][] world(long seed) {
        Random r = new Random(seed);
        int[][][] w = new int[SX][SY][SZ];
        for (int x = 0; x < SX; x++)
            for (int z = 0; z < SZ; z++) {
                w[x][0][z] = r.nextInt(3) == 0 ? 0 : 1;
                for (int y = 1; y < SY; y++) {
                    int roll = r.nextInt(12);
                    w[x][y][z] = roll < 8 ? 0 : roll < 10 ? 1 : roll < 11 ? 2 : 3;
                }
            }
        return w;
    }

    @Property(tries = 1000)
    void airRunsAreWholeAndWithinTheLimit(@ForAll long seed, @ForAll @IntRange(min = 0, max = 4) int tier, @ForAll @IntRange(min = 0, max = SX - 1) int ax,
                                          @ForAll @IntRange(min = 0, max = SZ - 1) int az, @ForAll @IntRange(min = 0, max = SX - 1) int bx,
                                          @ForAll @IntRange(min = 0, max = SZ - 1) int bz) {
        int[][][] w = world(seed);
        CableRouter.End a = CableRoutePropertyTest.top(w, ax, az), b = CableRoutePropertyTest.top(w, bx, bz);
        if (a == null || b == null || ax == bx && az == bz) return;
        CablePart.Tier t = CablePart.TIERS[tier];
        RouteKind kind = RouteKind.cable("cable", t.width(), t.height());
        CableRouter.Space space = CableRoutePropertyTest.space(w);
        CableRouter.Result r = CableRouter.route(space, kind, a, List.of(), b, 2000);
        if (r.error != null) return;   // (no route is allowed; a wrong one isn't)
        routes.incrementAndGet();
        int air = CableAirTest.longestAir(space, kind, r.steps);
        assertTrue(air <= kind.airLimit(), "an air stretch of " + air + " voxels: " + r.steps);
        if (air > 0) airy.incrementAndGet();
        List<CableRouter.Piece> ps = CableRouter.pieces(r.steps, t.width(), t.height());
        for (CableRouter.Piece p : ps) {
            CablePart part = p.face() < 0 ? CablePart.knuckle(tier, p.points()[0], p.points()[1], p.points()[2]) : new CablePart(tier, p.face(), p.points());
            assertTrue(part.valid(), "invalid piece " + p + " " + java.util.Arrays.toString(p.points()) + "; steps " + r.steps);
            for (long[] q : CableRoutePropertyTest.boxes(tier, p))
                for (long x = q[0]; x < q[3]; x++)
                    for (long y = q[1]; y < q[4]; y++)
                        for (long z = q[2]; z < q[5]; z++)
                            assertFalse(CableRoutePropertyTest.voxel(w, x, y, z), () -> "a piece inside a solid voxel: " + p + " at "
                                + java.util.Arrays.toString(q) + "; steps " + r.steps);
        }
        for (int i = 1; i < ps.size(); i++) {
            boolean touch = false;
            for (long[] p : CableRoutePropertyTest.boxes(tier, ps.get(i - 1)))
                for (long[] q : CableRoutePropertyTest.boxes(tier, ps.get(i))) touch |= CableRoutePropertyTest.faceTouch(p, q);
            final int k = i;
            assertTrue(touch, () -> "tier " + tier + ": a gap between pieces " + (k - 1) + " and " + k + ": " + ps.get(k - 1) + " "
                + java.util.Arrays.toString(ps.get(k - 1).points()) + " " + CableRoutePropertyTest.boxesText(tier, ps.get(k - 1)) + " / "
                + ps.get(k) + " " + java.util.Arrays.toString(ps.get(k).points()) + " " + CableRoutePropertyTest.boxesText(tier, ps.get(k))
                + "; steps " + r.steps);
        }
    }

    /**
     * A sample the property found: a one-voxel cable drops from a ceiling through the air onto a slab and leaves along
     * the floor, across the drop. The floor run must reach back under the drop, not meet it at an edge.
     */
    @Example
    void aRunAcrossADropTouchesIt() {
        airRunsAreWholeAndWithinTheLimit(13, 0, 0, 0, 0, 1);
    }

    @AfterContainer
    static void enoughCrossedTheAir() {
        System.out.println("CableAirPropertyTest: " + routes.get() + " routes, " + airy.get() + " through the air");
        assertTrue(routes.get() >= 200 && airy.get() >= routes.get() / 10,
            "too few routes through the air to test them: " + airy.get() + " of " + routes.get());
    }
}
