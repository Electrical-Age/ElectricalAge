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

    public static final class Plan {
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

    /** A route left alone this long lapses (Baughn: waypoints must time out; a later cable started from an old attempt). */
    public static final long IDLE_TICKS = 20 * 60;

    /**
     * Why a route being laid lapses now, or null if it holds: another item in hand (Baughn's rule), a minute without a
     * click, the player further from its last point than a full kit reaches, another dimension.
     */
    static String lapse(Plan plan, long now, boolean holdingItsKit, boolean sameDim, Vec3 at) {
        if (!sameDim) return "another dimension";
        if (!holdingItsKit) return "another item in hand";
        if (now - plan.touched > IDLE_TICKS) return "a minute without a click";
        CableRouter.End last = plan.waypoints.isEmpty() ? plan.start : plan.waypoints.get(plan.waypoints.size() - 1);
        double dx = at.x * 16 - last.x(), dy = at.y * 16 - last.y(), dz = at.z * 16 - last.z();
        if (dx * dx + dy * dy + dz * dz > (double) KitItem.MAX * KitItem.MAX) return "too far from it";
        return null;
    }

    /** Each player tick (KitPreview): drop a route that lapsed, and say why. */
    static void checkLapse(ServerPlayer p, long now) {
        Plan plan = PLANS.get(p.getUUID());
        if (plan == null) return;
        boolean holding = p.getMainHandItem().getItem() instanceof CableKitItem k && k.tier == plan.tier;
        String why = lapse(plan, now, holding, plan.dim == p.level().dimension(), p.position());
        if (why == null) return;
        PLANS.remove(p.getUUID());
        p.sendOverlayMessage(Component.literal("Cable route dropped (" + why + ")"));
    }

    public static void forgetAll() {
        PLANS.clear();
    }

    /** The world for the cable router: the pipe router's obstacles, and full solid blocks to lay cables on. */
    public static CableRouter.Space space(Level level) {
        return space(RouteView.live(level));
    }

    /** ... read through a view of the world (live, or a snapshot for the preview thread). */
    public static CableRouter.Space space(RouteView view) {
        PipeRouter.Space blocked = PipePlacer.space(view);
        return new CableRouter.Space() {
            @Override
            public boolean blocked(long x0, long y0, long z0, long x1, long y1, long z1) {
                return blocked.blocked(x0, y0, z0, x1, y1, z1);
            }

            @Override
            public boolean solid(long x0, long y0, long z0, long x1, long y1, long z1) {
                return CablePlacer.solid(view, x0, y0, z0, x1, y1, z1);
            }
        };
    }

    /**
     * Is every voxel of the box (world voxels) solid enough to lay a cable on: a full solid block, or inside the shape of
     * a decorative block (a stair's step, a slab), real or imitated by a voxel block (Mimic)?
     */
    static boolean solid(RouteView view, long x0, long y0, long z0, long x1, long y1, long z1) {
        for (long bx = Math.floorDiv(x0, 16); bx <= Math.floorDiv(x1 - 1, 16); bx++)
            for (long by = Math.floorDiv(y0, 16); by <= Math.floorDiv(y1 - 1, 16); by++)
                for (long bz = Math.floorDiv(z0, 16); bz <= Math.floorDiv(z1 - 1, 16); bz++) {
                    BlockPos p = new BlockPos((int) bx, (int) by, (int) bz);
                    if (!view.loaded(p)) return false;
                    BlockState s = view.state(p);
                    if (!(s.getBlock() instanceof mods.eln.node.NodeBlock) && s.isCollisionShapeFullBlock(view.shapes(), p)) continue;
                    List<int[]> boxes = null;
                    if (Mimic.allowed(s)) boxes = Mimic.boxes(s, view.shapes(), p);
                    else if (s.getBlock() instanceof VoxelNodeBlock) {
                        RouteView.Voxels v = view.voxels(p);
                        if (v != null) boxes = v.hostBoxes();
                    }
                    if (boxes == null || boxes.isEmpty()) return false;
                    // every voxel of the box's part in this block inside one of the shape's boxes
                    int lx0 = (int) Math.max(0, x0 - bx * 16), ly0 = (int) Math.max(0, y0 - by * 16), lz0 = (int) Math.max(0, z0 - bz * 16);
                    int lx1 = (int) Math.min(16, x1 - bx * 16), ly1 = (int) Math.min(16, y1 - by * 16), lz1 = (int) Math.min(16, z1 - bz * 16);
                    for (int x = lx0; x < lx1; x++)
                        for (int y = ly0; y < ly1; y++)
                            for (int z = lz0; z < lz1; z++) {
                                boolean in = false;
                                for (int[] b : boxes) if (x >= b[0] && x < b[3] && y >= b[1] && y < b[4] && z >= b[2] && z < b[5]) { in = true; break; }
                                if (!in) return false;
                            }
                }
        return true;
    }

    /** Free round a junction point (the new cable meets the old one there): of what's in the way there, only the old
     *  cable; solid blocks stay solid (they were free too, and a route could pass through a floor beside a junction). */
    static CableRouter.Space around(CableRouter.Space s, long[] p) {
        final int h = 4;
        PipeRouter.Space free = PipePlacer.spaceAround(s, p, h);
        return new CableRouter.Space() {
            @Override
            public boolean blocked(long x0, long y0, long z0, long x1, long y1, long z1) {
                if (free.blocked(x0, y0, z0, x1, y1, z1)) return true;
                long cx0 = Math.max(x0, p[0] - h), cx1 = Math.min(x1, p[0] + h), cy0 = Math.max(y0, p[1] - h), cy1 = Math.min(y1, p[1] + h),
                    cz0 = Math.max(z0, p[2] - h), cz1 = Math.min(z1, p[2] + h);
                for (long x = cx0; x < cx1; x++)
                    for (long y = cy0; y < cy1; y++)
                        for (long z = cz0; z < cz1; z++) if (s.solid(x, y, z, x + 1, y + 1, z + 1)) return true;
                return false;
            }

            @Override
            public boolean solid(long x0, long y0, long z0, long x1, long y1, long z1) {
                return s.solid(x0, y0, z0, x1, y1, z1);
            }
        };
    }

    /** A click as a routing end: on a device, its face's nearest edge; on a cable, a point of it; else a point on the
     *  face clicked. join (out) gets the junction point for a cable. */
    static CableRouter.End endAt(Level level, BlockPos pos, Direction face, Vec3 hit, boolean start, long[][] join) {
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level));
        double hx = hit.x - pos.getX(), hy = hit.y - pos.getY(), hz = hit.z - pos.getZ();
        if (n instanceof VoxelNode v) {
            // a device hosted here: the edge of its face nearest the click; the cable comes from the next block, on the
            // same face plane, and ends at that edge's midpoint (its port)
            int el = v.elementAt(hx, hy, hz, 0);
            if (el < 0 && v.partAt(hx, hy, hz) < 0) el = v.elementAt(hx, hy, hz, 2);
            if (el >= 0) {
                int f = mods.eln.misc.Direction.fromInt(el).toEnumFacing().get3DDataValue();
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
                long[] base = {pos.getX() * 16L, pos.getY() * 16L, pos.getZ() * 16L};
                long[] p = {base[0] + 8, base[1] + 8, base[2] + 8};
                p[fa] = base[fa] + CablePart.plane(f);
                p[CableRouter.axis(edge)] = base[CableRouter.axis(edge)] + (edge % 2 == 0 ? 0 : 16);
                return new CableRouter.End(p[0], p[1], p[2], f, start ? edge : CableRouter.reverse(edge));
            }
            int idx = v.partAt(hx, hy, hz);
            if (idx >= 0 && v.parts.get(idx) instanceof CablePart c && !c.isKnuckle()) {
                long[] best = null;
                double bd = Double.MAX_VALUE;
                for (int a = 0; a <= 16; a += 2)
                    for (int b = 0; b <= 16; b += 2) {
                        int[] q = new int[3];
                        int nx = CablePart.normalAxis(c.face);
                        int[] o = VoxelGeometry.others(nx);
                        q[nx] = c.points[nx];
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
            if (v.host != null) return surface(pos, face, hx, hy, hz);   // (the imitated stair's surface)
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
        return surface(pos, face, hx, hy, hz);
    }

    /** A point on the surface clicked (a block's face, or a stair's step inside its block), the cable lying on it. */
    static CableRouter.End surface(BlockPos pos, Direction face, double hx, double hy, double hz) {
        int f = face.get3DDataValue();
        int fa = CableRouter.axis(f);
        double[] h = {hx, hy, hz};
        long[] base = {pos.getX() * 16L, pos.getY() * 16L, pos.getZ() * 16L};
        long[] p = new long[3];
        for (int a = 0; a < 3; a++)
            p[a] = a == fa ? base[a] + Math.round(h[a] * 16 / CableRouter.LATTICE) * CableRouter.LATTICE : base[a] + snap(h[a] * 16);
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
        if (plan != null && (plan.tier != tier || lapse(plan, now, true, plan.dim == level.dimension(), player.position()) != null)) {
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

    /** Route between two ends, each maybe joining an old cable (join point or null), and lay it (GameTests). */
    public static String routeAndLayJoinedForTests(Level level, int tier, CableRouter.End start, long[] startJoin, CableRouter.End end, long[] endJoin) {
        CablePart.Tier t = CablePart.TIERS[tier];
        CableRouter.Space s = space(level);
        if (startJoin != null) s = around(s, startJoin);
        if (endJoin != null) s = around(s, endJoin);
        var r = CableRouter.route(s, t.width(), t.height(), start, List.of(), end);
        if (r.error != null) return r.error;
        return lay(level, tier, r.steps, null);
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
        List<CableRouter.Piece> pieces = CableRouter.pieces(steps, CablePart.TIERS[tier].width(), CablePart.TIERS[tier].height());
        List<CablePart> parts = new ArrayList<>();
        for (CableRouter.Piece p : pieces) {
            CablePart part = p.face() < 0 ? CablePart.knuckle(tier, p.points()[0], p.points()[1], p.points()[2]) : new CablePart(tier, p.face(), p.points());
            if (!part.valid()) return "bad piece at " + p.bx() + " " + p.by() + " " + p.bz();
            parts.add(part);
            BlockPos bp = new BlockPos((int) p.bx(), (int) p.by(), (int) p.bz());
            BlockState s = level.getBlockState(bp);
            VoxelNode existing = s.getBlock() instanceof VoxelNodeBlock && level.getBlockEntity(bp) instanceof VoxelNodeEntity ve
                && ve.getNode() instanceof VoxelNode n ? n : null;
            if (existing == null && Mimic.allowed(s)) {   // (through a stair, under a fence: clear of its own voxels)
                for (int[] hb : Mimic.boxes(s, level, bp))
                    for (int[] pb : part.boxes())
                        if (hb[0] < pb[3] && pb[0] < hb[3] && hb[1] < pb[4] && pb[1] < hb[4] && hb[2] < pb[5] && pb[2] < hb[5])
                            return "the " + s.getBlock().getName().getString() + " is in the way at " + bp.toShortString();
            } else if (existing == null && !(s.isAir() || s.canBeReplaced())) return "a block is in the way at " + bp.toShortString();
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
