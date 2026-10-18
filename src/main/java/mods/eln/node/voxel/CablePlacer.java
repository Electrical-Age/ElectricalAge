package mods.eln.node.voxel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Laying a routed cable with a cable kit (notes/cables/PLAN.md; Baughn: the pipes' UX, but cables run against faces).
 * Right-click a face to start (a block's face: the cable starts there, lying on it; a device: the cable starts at the
 * edge of its face nearest the click; a cable: it starts as a branch of it). Right-click more faces for waypoints;
 * right-click a device or a cable to finish there, or the last waypoint again to end the cable there. Sneak-right-click
 * takes back the last waypoint (or cancels). The planned route shows as particles.
 */
public final class CablePlacer {
    private CablePlacer() {
    }

    static final class Plan {
        final int tier;
        final CableRouter.End start;
        final long[] startJoin;
        final List<CableRouter.End> waypoints = new ArrayList<>();
        final net.minecraft.resources.ResourceKey<Level> dim;
        long touched;

        Plan(int tier, CableRouter.End start, long[] startJoin, net.minecraft.resources.ResourceKey<Level> dim) {
            this.tier = tier;
            this.start = start;
            this.startJoin = startJoin;
            this.dim = dim;
        }
    }

    static final Map<UUID, Plan> PLANS = new HashMap<>();

    public static void forget(UUID player) {
        PLANS.remove(player);
    }

    public static void forgetAll() {
        PLANS.clear();
    }

    /** The world for the cable router: the pipe router's obstacles, and full solid blocks to lay cables on. */
    public static CableRouter.Space space(Level level) {
        PipeRouter.Space blocked = PipePlacer.space(level);
        return new CableRouter.Space() {
            @Override
            public boolean blocked(long x0, long y0, long z0, long x1, long y1, long z1) {
                return blocked.blocked(x0, y0, z0, x1, y1, z1);
            }

            @Override
            public boolean solid(long bx, long by, long bz) {
                BlockPos p = new BlockPos((int) bx, (int) by, (int) bz);
                if (!level.isLoaded(p)) return false;
                BlockState s = level.getBlockState(p);
                return !(s.getBlock() instanceof mods.eln.node.NodeBlock) && s.isCollisionShapeFullBlock(level, p);
            }
        };
    }

    /** Free round a junction point (the new cable meets the old one there). */
    static CableRouter.Space around(CableRouter.Space s, long[] p) {
        PipeRouter.Space free = PipePlacer.spaceAround(s, p, 4);
        return new CableRouter.Space() {
            @Override
            public boolean blocked(long x0, long y0, long z0, long x1, long y1, long z1) {
                return free.blocked(x0, y0, z0, x1, y1, z1);
            }

            @Override
            public boolean solid(long bx, long by, long bz) {
                return s.solid(bx, by, bz);
            }
        };
    }

    /** A click as a routing end: on a device, its face's nearest edge; on a cable, a point of it; else a point on the
     *  face clicked. join (out) gets the junction point for a cable. */
    static CableRouter.End endAt(Level level, BlockPos pos, Direction face, Vec3 hit, boolean start, long[][] join) {
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level));
        double hx = hit.x - pos.getX(), hy = hit.y - pos.getY(), hz = hit.z - pos.getZ();
        if (n instanceof VoxelNode v) {
            int idx = v.partAt(hx, hy, hz);
            if (idx >= 0 && v.parts.get(idx) instanceof CablePart c && !c.isKnuckle()) {
                long[] best = null;
                double bd = Double.MAX_VALUE;
                for (int a = 0; a <= 16; a += 2)
                    for (int b = 0; b <= 16; b += 2) {
                        int[] q = new int[3];
                        int nx = CablePart.normalAxis(c.face);
                        int[] o = VoxelGeometry.others(nx);
                        q[nx] = CablePart.plane(c.face);
                        q[o[0]] = a;
                        q[o[1]] = b;
                        if (!c.passes(q[0], q[1], q[2])) continue;
                        double d = sq(q[0] - hx * 16) + sq(q[1] - hy * 16) + sq(q[2] - hz * 16);
                        if (d < bd) {
                            bd = d;
                            best = new long[]{pos.getX() * 16L + q[0], pos.getY() * 16L + q[1], pos.getZ() * 16L + q[2]};
                        }
                    }
                if (best == null) return null;
                join[0] = best;
                return new CableRouter.End(best[0], best[1], best[2], c.face, -1);
            }
            return null;
        }
        if (n != null) {
            // a device: the edge of the clicked face nearest the click; the cable lies in the block in front, against
            // the face beyond that edge, and meets the device there (EA's edge rule)
            int f = face.get3DDataValue();
            int fa = CableRouter.axis(f);
            double[] h = {hx, hy, hz};
            int edge = -1;
            double bd = Double.MAX_VALUE;
            for (int g = 0; g < 6; g++) {
                if (CableRouter.axis(g) == fa) continue;
                double d = g % 2 == 0 ? h[CableRouter.axis(g)] : 1 - h[CableRouter.axis(g)];
                if (d < bd) {
                    bd = d;
                    edge = g;
                }
            }
            long[] p = new long[3];
            long[] base = {pos.getX() * 16L, pos.getY() * 16L, pos.getZ() * 16L};
            p[fa] = base[fa] + (f % 2 == 0 ? 0 : 16);
            int ga = CableRouter.axis(edge);
            p[ga] = base[ga] + (edge % 2 == 0 ? 0 : 16);
            int w = 3 - fa - ga;
            p[w] = base[w] + snap(h[w] * 16);
            return new CableRouter.End(p[0], p[1], p[2], edge, start ? f : CableRouter.reverse(f));
        }
        // a plain block: a point on the face clicked
        int f = face.get3DDataValue();
        int fa = CableRouter.axis(f);
        double[] h = {hx, hy, hz};
        long[] base = {pos.getX() * 16L, pos.getY() * 16L, pos.getZ() * 16L};
        long[] p = new long[3];
        for (int a = 0; a < 3; a++) p[a] = a == fa ? base[a] + (f % 2 == 0 ? 0 : 16) : base[a] + snap(h[a] * 16);
        return new CableRouter.End(p[0], p[1], p[2], CableRouter.reverse(f), -1);
    }

    public static CableRouter.End endAtForTests(Level level, BlockPos pos, Direction face, Vec3 hit, boolean start, long[][] join) {
        return endAt(level, pos, face, hit, start, join);
    }

    /** A lattice coordinate inside the block (not on its edge). */
    static int snap(double v) {
        int s = (int) Math.round(v / CableRouter.LATTICE) * CableRouter.LATTICE;
        return Math.max(CableRouter.LATTICE, Math.min(16 - CableRouter.LATTICE, s));
    }

    static double sq(double v) {
        return v * v;
    }

    /** A cable kit used on a block face: start, extend or finish a route. Returns true if it handled the click. */
    public static boolean use(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, ItemStack kit, int tier, Vec3 hit) {
        Plan plan = PLANS.get(player.getUUID());
        long now = level.getGameTime();
        if (plan != null && (plan.dim != level.dimension() || plan.tier != tier || now - plan.touched > PipePlacer.PLAN_TIMEOUT)) {
            PLANS.remove(player.getUUID());
            if (plan.tier != tier) player.sendOverlayMessage(Component.literal("Cable route cancelled (another kind of cable in hand)"));
            plan = null;
        }
        if (plan != null) plan.touched = now;
        if (player.isSecondaryUseActive()) {
            if (plan == null) return false;
            if (plan.waypoints.isEmpty()) {
                PLANS.remove(player.getUUID());
                player.sendOverlayMessage(Component.literal("Cable route cancelled"));
            } else {
                plan.waypoints.remove(plan.waypoints.size() - 1);
                player.sendOverlayMessage(Component.literal("Waypoint taken back (" + plan.waypoints.size() + " left)"));
            }
            return true;
        }
        long[][] join = new long[1][];
        CableRouter.End at = endAt(level, pos, face, hit, plan == null, join);
        if (at == null) {
            player.sendOverlayMessage(Component.literal("Not a place for a cable"));
            return true;
        }
        boolean device = at.dir() >= 0;
        if (plan == null) {
            Plan p = new Plan(tier, at, join[0], level.dimension());
            p.touched = now;
            PLANS.put(player.getUUID(), p);
            player.sendOverlayMessage(Component.literal("Cable route started: click faces for waypoints, a device or cable (or the last waypoint again) to finish, sneak-click to undo"));
            return true;
        }
        CableRouter.End last = plan.waypoints.isEmpty() ? null : plan.waypoints.get(plan.waypoints.size() - 1);
        boolean again = last != null && last.x() == at.x() && last.y() == at.y() && last.z() == at.z() && last.attach() == at.attach();
        if (!device && join[0] == null && !again) {
            plan.waypoints.add(at);
            CableRouter.Result r = route(level, plan, null, null);
            if (r.error != null) {
                plan.waypoints.remove(plan.waypoints.size() - 1);
                player.sendOverlayMessage(Component.literal("Can't reach that waypoint: " + r.error));
            } else {
                player.sendOverlayMessage(Component.literal(String.format("Waypoint %d: %.1f blocks so far", plan.waypoints.size(), r.length() / 16.0)));
                show(player, level, r.steps);
            }
            return true;
        }
        // finish: at a device, a cable, or the last waypoint
        CableRouter.End end = again ? plan.waypoints.remove(plan.waypoints.size() - 1) : at;
        CableRouter.Result r = route(level, plan, end, join[0]);
        if (r.error != null) {
            if (again) plan.waypoints.add(end);
            player.sendOverlayMessage(Component.literal("No route: " + r.error));
            return true;
        }
        int len = r.length();
        boolean creative = player.isCreative();
        if (!creative && KitItem.length(kit) < len) {
            if (again) plan.waypoints.add(end);
            player.sendOverlayMessage(Component.literal(String.format("This route takes %.1f blocks of cable; the kit holds %.1f", len / 16.0, KitItem.length(kit) / 16.0)));
            show(player, level, r.steps);
            return true;
        }
        String err = lay(level, tier, r.steps, player);
        if (err != null) {
            player.sendOverlayMessage(Component.literal("Can't lay it: " + err));
            return true;
        }
        PLANS.remove(player.getUUID());
        if (!creative) {
            int left = KitItem.length(kit) - len;
            if (left <= 0) kit.shrink(1);
            else KitItem.setLength(kit, left);
        }
        player.sendOverlayMessage(Component.literal(String.format("Laid %.1f blocks of cable", len / 16.0)));
        return true;
    }

    static CableRouter.Result route(Level level, Plan plan, CableRouter.End end, long[] endJoin) {
        List<CableRouter.End> wps = new ArrayList<>(plan.waypoints);
        CableRouter.End last = end;
        if (last == null) {
            if (wps.isEmpty()) return new CableRouter.Result(List.of(), null);
            last = wps.remove(wps.size() - 1);
        }
        CableRouter.Space s = space(level);
        if (plan.startJoin != null) s = around(s, plan.startJoin);
        if (endJoin != null) s = around(s, endJoin);
        CablePart.Tier t = CablePart.TIERS[plan.tier];
        return CableRouter.route(s, t.width(), t.height(), plan.start, wps, last);
    }

    /** Route between two ends and lay it (GameTests). Null if done, else why not. */
    public static String routeAndLayForTests(Level level, int tier, CableRouter.End start, CableRouter.End end) {
        CablePart.Tier t = CablePart.TIERS[tier];
        var r = CableRouter.route(space(level), t.width(), t.height(), start, List.of(), end);
        if (r.error != null) return r.error;
        return lay(level, tier, r.steps, null);
    }

    /** Lay a route: a cable part per block piece (and the knuckles); checked first, all or nothing. */
    public static String lay(Level level, int tier, List<CableRouter.Step> steps, net.minecraft.world.entity.player.Player player) {
        List<CableRouter.Piece> pieces = CableRouter.pieces(steps);
        List<CablePart> parts = new ArrayList<>();
        for (CableRouter.Piece p : pieces) {
            CablePart part = p.face() < 0 ? CablePart.knuckle(tier, p.points()[0], p.points()[1], p.points()[2]) : new CablePart(tier, p.face(), p.points());
            if (!part.valid()) return "bad piece at " + p.bx() + " " + p.by() + " " + p.bz();
            parts.add(part);
            BlockPos bp = new BlockPos((int) p.bx(), (int) p.by(), (int) p.bz());
            BlockState s = level.getBlockState(bp);
            VoxelNode existing = s.getBlock() instanceof VoxelNodeBlock && level.getBlockEntity(bp) instanceof VoxelNodeEntity ve
                && ve.getNode() instanceof VoxelNode n ? n : null;
            if (existing == null && !(s.isAir() || s.canBeReplaced())) return "a block is in the way at " + bp.toShortString();
            if (existing != null) {
                String why = existing.canAdd(part);
                if (why != null) return why + " at " + bp.toShortString();
            }
        }
        for (int i = 0; i < pieces.size(); i++) {
            CableRouter.Piece p = pieces.get(i);
            String err = PipeKitItem.place(level, new BlockPos((int) p.bx(), (int) p.by(), (int) p.bz()), parts.get(i), player);
            if (err != null) return err + " at " + p.bx() + " " + p.by() + " " + p.bz();
        }
        return null;
    }

    static void show(ServerPlayer player, ServerLevel level, List<CableRouter.Step> steps) {
        var dust = new net.minecraft.core.particles.DustParticleOptions(0xFFB040, 0.7f);
        for (int i = 1; i < steps.size(); i++) {
            CableRouter.Step a = steps.get(i - 1), b = steps.get(i);
            int n = (int) Math.max(1, (Math.abs(b.x() - a.x()) + Math.abs(b.y() - a.y()) + Math.abs(b.z() - a.z())) / 4);
            double[] off = {CableRouter.DV[b.attach()][0] * -0.05, CableRouter.DV[b.attach()][1] * -0.05, CableRouter.DV[b.attach()][2] * -0.05};
            for (int k = 0; k <= n; k++) {
                double t = k / (double) n;
                level.sendParticles(player, dust, true, false, (a.x() + (b.x() - a.x()) * t) / 16.0 + off[0], (a.y() + (b.y() - a.y()) * t) / 16.0 + off[1],
                    (a.z() + (b.z() - a.z()) * t) / 16.0 + off[2], 1, 0, 0, 0, 0);
            }
        }
    }
}
