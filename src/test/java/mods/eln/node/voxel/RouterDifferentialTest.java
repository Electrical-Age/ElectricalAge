package mods.eln.node.voxel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The routers against frozen copies of themselves (LegacyCableRouter, LegacyPipeRouter, as of 79f42651): on thousands of
 * random little worlds of blocks and slabs, every cable tier and pipe tier, random ends (free points, device-like ends
 * with a direction, pipe ports on block faces that need a jog) and waypoints, the routes and errors must be identical
 * (Baughn 2026-11-11: one router for cables and pipes; it must not change what either finds). With no time budget, so a
 * deadline never decides. Enough of the cases must find a route, round corners, knuckles and jogs, or the comparison
 * proves little.
 */
public class RouterDifferentialTest {
    static final int CASES = 3000;
    static final long NO_BUDGET = 60_000;

    /** The little world as both routers' Space (it implements the live and the frozen interfaces). */
    static final class World implements CableRouter.Space, LegacyCableRouter.Space {
        final int[][][] w;

        World(int[][][] w) {
            this.w = w;
        }

        @Override
        public boolean blocked(long x0, long y0, long z0, long x1, long y1, long z1) {
            if (x0 < 0 || y0 < 0 || z0 < 0 || x1 > CableRoutePropertyTest.SX * 16L || y1 > CableRoutePropertyTest.SY * 16L + 32
                || z1 > CableRoutePropertyTest.SZ * 16L) return true;
            for (long x = x0; x < x1; x++)
                for (long y = y0; y < y1; y++)
                    for (long z = z0; z < z1; z++) if (CableRoutePropertyTest.voxel(w, x, y, z)) return true;
            return false;
        }

        @Override
        public boolean solid(long x0, long y0, long z0, long x1, long y1, long z1) {
            for (long x = x0; x < x1; x++)
                for (long y = y0; y < y1; y++)
                    for (long z = z0; z < z1; z++) if (!CableRoutePropertyTest.voxel(w, x, y, z)) return false;
            return true;
        }
    }

    /** A point on the top surface of a column, off-centre on the cable lattice (in 2..14), or null. */
    static long[] top(int[][][] w, Random r, int bx, int bz) {
        long vx = bx * 16L + 2 + 2 * r.nextInt(7), vz = bz * 16L + 2 + 2 * r.nextInt(7);
        for (long vy = CableRoutePropertyTest.SY * 16L - 1; vy >= 0; vy--)
            if (CableRoutePropertyTest.voxel(w, vx, vy, vz)) return new long[]{vx, vy + 1, vz};
        return null;
    }

    static String cable(CableRouter.Result r) {
        if (r.error != null) return "error " + r.error;
        StringBuilder sb = new StringBuilder();
        for (CableRouter.Step s : r.steps) sb.append(s.x()).append(',').append(s.y()).append(',').append(s.z()).append(' ').append(s.attach()).append(s.outer() ? 'o' : '-').append(';');
        return sb.toString();
    }

    static String cable(LegacyCableRouter.Result r) {
        if (r.error != null) return "error " + r.error;
        StringBuilder sb = new StringBuilder();
        for (LegacyCableRouter.Step s : r.steps) sb.append(s.x()).append(',').append(s.y()).append(',').append(s.z()).append(' ').append(s.attach()).append(s.outer() ? 'o' : '-').append(';');
        return sb.toString();
    }

    static String pipe(List<long[]> pts, String error) {
        if (error != null) return "error " + error;
        StringBuilder sb = new StringBuilder();
        for (long[] p : pts) sb.append(p[0]).append(',').append(p[1]).append(',').append(p[2]).append(';');
        return sb.toString();
    }

    @Test
    void cablesRouteAsBefore() {
        Random r = new Random(20261111);
        int found = 0, outer = 0, inner = 0, withWaypoints = 0, arrive = 0;
        for (int c = 0; c < CASES; c++) {
            int[][][] w = CableRoutePropertyTest.world(r.nextLong());
            World sp = new World(w);
            int tier = r.nextInt(CablePart.TIERS.length);
            CablePart.Tier t = CablePlacer.tierOf(tier, null);
            long[] a = top(w, r, r.nextInt(CableRoutePropertyTest.SX), r.nextInt(CableRoutePropertyTest.SZ));
            long[] b = top(w, r, r.nextInt(CableRoutePropertyTest.SX), r.nextInt(CableRoutePropertyTest.SZ));
            if (a == null || b == null) continue;
            int wpN = r.nextInt(3);
            List<long[]> wps = new ArrayList<>();
            for (int i = 0; i < wpN; i++) {
                long[] p = top(w, r, r.nextInt(CableRoutePropertyTest.SX), r.nextInt(CableRoutePropertyTest.SZ));
                if (p != null) wps.add(p);
            }
            // a device-like end now and then: arrive along a horizontal direction
            int dirA = r.nextInt(4) == 0 ? 2 + r.nextInt(4) : -1, dirB = r.nextInt(4) == 0 ? 2 + r.nextInt(4) : -1;
            List<CableRouter.End> nw = new ArrayList<>();
            List<LegacyCableRouter.End> ow = new ArrayList<>();
            for (long[] p : wps) {
                nw.add(new CableRouter.End(p[0], p[1], p[2], 0, -1));
                ow.add(new LegacyCableRouter.End(p[0], p[1], p[2], 0, -1));
            }
            // (with no air allowance: air runs are new, so only the on-support routes must match)
            CableRouter.Result n = CableRouter.route(sp, RouteKind.cable("cable", t.width(), t.height()).withAir(RouteKind.NO_AIR),
                new CableRouter.End(a[0], a[1], a[2], 0, dirA), nw,
                new CableRouter.End(b[0], b[1], b[2], 0, dirB), NO_BUDGET);
            LegacyCableRouter.Result o = LegacyCableRouter.route(sp, t.width(), t.height(), new LegacyCableRouter.End(a[0], a[1], a[2], 0, dirA), ow,
                new LegacyCableRouter.End(b[0], b[1], b[2], 0, dirB), NO_BUDGET);
            String ns = cable(n), os = cable(o);
            final int cc = c;
            assertEquals(os, ns, () -> "case " + cc + " tier " + tier + " from " + java.util.Arrays.toString(a) + " to " + java.util.Arrays.toString(b));
            if (n.error == null) {
                found++;
                if (!wps.isEmpty()) withWaypoints++;
                if (dirB >= 0) arrive++;
                for (int i = 1; i < n.steps.size(); i++) {
                    if (n.steps.get(i).outer()) outer++;
                    else if (n.steps.get(i).attach() != n.steps.get(i - 1).attach()) inner++;
                }
            }
        }
        String cov = "found " + found + ", outer corners " + outer + ", inner corners " + inner + ", with waypoints " + withWaypoints + ", arriving " + arrive;
        System.out.println("cable differential: " + cov);
        assertTrue(found > CASES / 4 && outer > 50 && inner > 50 && withWaypoints > 100 && arrive > 50, cov);
    }

    @Test
    void pipesRouteAsBefore() {
        Random r = new Random(1111);
        int found = 0, jogs = 0, withWaypoints = 0, toPort = 0;
        for (int c = 0; c < CASES; c++) {
            int[][][] w = CableRoutePropertyTest.world(r.nextLong());
            World sp = new World(w);
            int tier = r.nextInt(PipePart.GRID.length);
            int bore = PipePart.GRID[tier];
            // start: a port on top of a column (on the face plane: it needs a jog out), or a free point in the air
            long[] a = top(w, r, r.nextInt(CableRoutePropertyTest.SX), r.nextInt(CableRoutePropertyTest.SZ));
            long[] b = top(w, r, r.nextInt(CableRoutePropertyTest.SX), r.nextInt(CableRoutePropertyTest.SZ));
            if (a == null || b == null) continue;
            boolean portA = r.nextBoolean(), portB = r.nextInt(3) == 0;
            PipeRouter.End na, nb;
            LegacyPipeRouter.End oa, ob;
            long ay = portA ? a[1] : a[1] + 4 + 4 * r.nextInt(2), by = portB ? b[1] : b[1] + 4 + 4 * r.nextInt(2);
            long ax = Math.floorDiv(a[0], 4) * 4, az = Math.floorDiv(a[2], 4) * 4, bx = Math.floorDiv(b[0], 4) * 4, bz = Math.floorDiv(b[2], 4) * 4;
            if (!portA) {
                ax = ax - Math.floorMod(ax, 16) + 8;
                az = az - Math.floorMod(az, 16) + 8;
            }
            if (!portB) {
                bx = bx - Math.floorMod(bx, 16) + 8;
                bz = bz - Math.floorMod(bz, 16) + 8;
            }
            na = new PipeRouter.End(ax, ay, az, portA ? 3 : -1);
            oa = new LegacyPipeRouter.End(ax, ay, az, portA ? 3 : -1);
            nb = new PipeRouter.End(bx, by, bz, portB ? 3 : -1);
            ob = new LegacyPipeRouter.End(bx, by, bz, portB ? 3 : -1);
            List<PipeRouter.End> nw = new ArrayList<>();
            List<LegacyPipeRouter.End> ow = new ArrayList<>();
            if (r.nextInt(3) == 0) {
                long[] p = top(w, r, r.nextInt(CableRoutePropertyTest.SX), r.nextInt(CableRoutePropertyTest.SZ));
                if (p != null) {
                    long px = p[0] - Math.floorMod(p[0], 16) + 8, py = p[1] - Math.floorMod(p[1], 16) + 8, pz = p[2] - Math.floorMod(p[2], 16) + 8;
                    nw.add(new PipeRouter.End(px, py, pz, -1));
                    ow.add(new LegacyPipeRouter.End(px, py, pz, -1));
                }
            }
            PipeRouter.Result n = PipeRouter.route(sp, bore, na, nw, nb, NO_BUDGET);
            LegacyPipeRouter.Result o = LegacyPipeRouter.route(sp, bore, oa, ow, ob, NO_BUDGET);
            String ns = pipe(n.points, n.error), os = pipe(o.points, o.error);
            final int cc = c;
            assertEquals(os, ns, () -> "case " + cc + " tier " + tier);
            if (n.error == null) {
                found++;
                if (portA) jogs++;
                if (!nw.isEmpty()) withWaypoints++;
                if (portB) toPort++;
            }
        }
        String cov = "found " + found + ", from ports (jogs) " + jogs + ", with waypoints " + withWaypoints + ", to ports " + toPort;
        System.out.println("pipe differential: " + cov);
        assertTrue(found > CASES / 4 && jogs > 100 && withWaypoints > 50 && toPort > 50, cov);
    }
}
