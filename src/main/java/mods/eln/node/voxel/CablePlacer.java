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
        /** The kit's spec (power cables): a route is laid of one cable. */
        CableSpec spec;
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
        boolean holding = p.getMainHandItem().getItem() instanceof CableKitItem k && k.tier == plan.tier
            && java.util.Objects.equals(CableKitItem.specOf(p.getMainHandItem()), plan.spec);
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
     * Does cable part c in the block at pos lie on a support all along (the voxel layer beyond its face plane under each of
     * its boxes solid)? Read live, not stored: a cable crossing air (routing phase 2) may be landed on later, or its
     * support mined out. A knuckle wraps an edge: supported.
     */
    public static boolean supported(Level level, BlockPos pos, CablePart c) {
        if (c.isKnuckle()) return true;
        RouteView view = RouteView.live(level);
        int n = CablePart.normalAxis(c.face);
        long[] o = {pos.getX() * 16L, pos.getY() * 16L, pos.getZ() * 16L};
        for (int[] b : c.boxes()) {
            long[] lo = {o[0] + b[0], o[1] + b[1], o[2] + b[2]}, hi = {o[0] + b[3], o[1] + b[4], o[2] + b[5]};
            if (c.face % 2 == 0) hi[n] = lo[n]--;
            else lo[n] = hi[n]++;
            if (!solid(view, lo[0], lo[1], lo[2], hi[0], hi[1], hi[2])) return false;
        }
        return true;
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

    /** Is the clicked thing a device (a machine's face, hosted in a voxel block or a block of its own)? */
    static boolean isDevice(Level level, BlockPos pos, Vec3 hit) {
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level));
        if (n == null) return false;
        if (!(n instanceof VoxelNode v)) return true;
        double hx = hit.x - pos.getX(), hy = hit.y - pos.getY(), hz = hit.z - pos.getZ();
        int el = v.elementAt(hx, hy, hz, 0);
        if (el < 0 && v.partAt(hx, hy, hz) < 0) el = v.elementAt(hx, hy, hz, 2);
        return el >= 0;
    }

    /** Why a click is no end: on a machine, no connector on that edge (AdventurAgent: say so); else no place for one. */
    public static String noEndForTests(Level level, BlockPos pos) {
        return noEnd(level, pos);
    }

    static String noEnd(Level level, BlockPos pos) {
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level));
        return n != null && !(n instanceof VoxelNode) ? "No connector there: try another face or edge of the machine" : "Not a place for a cable";
    }

    /** A click as a routing end: on a device, its face's nearest edge; on a cable, a point of it; else a point on the
     *  face clicked. join (out) gets the junction point for a cable. */
    static CableRouter.End endAt(Level level, BlockPos pos, Direction face, Vec3 hit, boolean start, long[][] join) {
        return endAt(level, pos, face, hit, start, join, -1);
    }

    /** The same for a kit of that tier (-1: any): a device off centre ends at its nearest pin of the kit's kind. */
    static CableRouter.End endAt(Level level, BlockPos pos, Direction face, Vec3 hit, boolean start, long[][] join, int tier) {
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level));
        double hx = hit.x - pos.getX(), hy = hit.y - pos.getY(), hz = hit.z - pos.getZ();
        if (n instanceof VoxelNode v) {
            // a device hosted here: the edge of its face nearest the click that has a connector (a trimmer has one, on
            // its front); the cable comes from the next block, on the same face plane, and ends at that edge's midpoint
            // (its port)
            int el = v.elementAt(hx, hy, hz, 0);
            if (el < 0 && v.partAt(hx, hy, hz) < 0) el = v.elementAt(hx, hy, hz, 2);
            if (el >= 0) return deviceEnd(v, pos, el, new double[]{hx, hy, hz}, start, tier);
            int idx = v.partAt(hx, hy, hz);
            // a control valve: its signal comes from a signal cable laid into its block: the floor under it (Baughn
            // 2026-10-26; AdventurAgent: clicking its actuator was "not a place for a cable")
            if (idx >= 0 && v.parts.get(idx) instanceof ValvePart cv && (cv.mode == ValvePart.CONTROL || cv.mode == ValvePart.PRESSURE))
                return new CableRouter.End(pos.getX() * 16L + cv.x, pos.getY() * 16L, pos.getZ() * 16L + cv.z, 0, -1);
            // a pipe sensor likewise (AdventurAgent: its dial and the pipe were "not a place for a cable")
            if (idx >= 0 && v.parts.get(idx) instanceof PipeSensorPart ps)
                return new CableRouter.End(pos.getX() * 16L + ps.x, pos.getY() * 16L, pos.getZ() * 16L + ps.z, 0, -1);
            if (idx >= 0 && v.parts.get(idx) instanceof CablePart clicked && !clicked.isKnuckle()) {
                // a branch of a cable of the kit's own tier: the clicked one, or the nearest such in the block
                // (AdventurAgent: a bus kit started on a signal cable crossing the cell began a separate bus, "Laid")
                List<CablePart> cands = new ArrayList<>();
                if (tier < 0 || clicked.tier == tier) cands.add(clicked);
                else for (VoxelPart vp : v.parts) if (vp instanceof CablePart o && !o.isKnuckle() && o.tier == tier) cands.add(o);
                // not a cable crossing the air: a branch from it would start its own air allowance afresh and could chain
                // past the 1.2-block limit (Baughn 2026-11-11)
                cands.removeIf(c -> !supported(level, pos, c));
                long[] best = null;
                int bestFace = -1;
                double bd = Double.MAX_VALUE;
                // (inside the block, never on its faces: a branch from a point on a face looked, from either block, like
                // a plain corner, and got no sleeve; AdventurAgent)
                for (CablePart c : cands)
                for (int a = 2; a <= 14; a += 2)
                    for (int b = 2; b <= 14; b += 2) {
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
                            bestFace = c.face;
                        }
                    }
                if (best == null) return null;
                join[0] = best;
                return new CableRouter.End(best[0], best[1], best[2], bestFace, -1);
            }
            if (v.host != null) return surface(pos, face, hx, hy, hz);   // (the imitated stair's surface)
            return null;
        }
        if (n != null) {
            // a device: the edge of the clicked face nearest the click that has a connector (a click high on a face whose
            // connector is at the floor still finds it, AdventurAgent); the cable lies in the block in front, against the
            // face beyond that edge, and meets the device there (EA's edge rule)
            int f = face.get3DDataValue();
            int fa = CableRouter.axis(f);
            double[] h = {hx, hy, hz};
            var side = mods.eln.misc.Direction.fromEnumFacing(face);
            int edge = -1;
            double bd = Double.MAX_VALUE;
            for (int g = 0; g < 6; g++) {
                if (CableRouter.axis(g) == fa) continue;
                if (n.getSideConnectionMask(side, side.getLRDUGoingTo(mods.eln.misc.Direction.fromEnumFacing(Direction.from3DDataValue(g)))) == 0) continue;
                double d = g % 2 == 0 ? h[CableRouter.axis(g)] : 1 - h[CableRouter.axis(g)];
                if (d < bd) {
                    bd = d;
                    edge = g;
                }
            }
            // (no connector on any edge of that face, as the steam turbine's flanks: not an end, AdventurAgent)
            if (edge < 0) return null;
            long[] p = new long[3];
            long[] base = {pos.getX() * 16L, pos.getY() * 16L, pos.getZ() * 16L};
            p[fa] = base[fa] + (f % 2 == 0 ? 0 : 16);
            int ga = CableRouter.axis(edge);
            p[ga] = base[ga] + (edge % 2 == 0 ? 0 : 16);
            int w = 3 - fa - ga;
            p[w] = base[w] + 8;   // (the middle of the edge: the connector's centre, where a terminal plate's lug sits, Baughn)
            // an edge with several pins (FacePins: the servo drive's flanks): the pin of the kit's kind nearest the click,
            // a quarter block either side of the middle (AdventurAgent: a bus kit always ended at the middle pin)
            if (n instanceof mods.eln.node.FacePins fp) {
                double best = Double.MAX_VALUE;
                int lw = w == 0 ? -face.getStepZ() : w == 2 ? face.getStepX() : 0;   // (left, seen facing the face from outside)
                for (var pin : fp.facePins(side)) {
                    if (side.applyLRDU(pin.lrdu()).toEnumFacing().get3DDataValue() != edge) continue;
                    if (tier >= 0 && !mods.eln.node.NodeBase.compareConnectionMask(pin.mask(), DevicePlacer.tierMask(tier))) continue;
                    int at = 8 + pin.leftward() * lw;
                    double d = Math.abs(at - h[w] * 16);
                    if (d < best) {
                        best = d;
                        p[w] = base[w] + at;
                    }
                }
            }
            return new CableRouter.End(p[0], p[1], p[2], edge, start ? f : CableRouter.reverse(f));
        }
        CableRouter.End snapped = snapToPort(level, pos, face, hx, hy, hz, start, join, tier);
        if (snapped != null) return snapped;
        return surface(pos, face, hx, hy, hz);
    }

    /** The end at the hosted device in slot el (0..5: centred on that face) of v: the edge of its face nearest the block-local
     *  point h that has a connector; null if it has none. */
    static CableRouter.End deviceEnd(VoxelNode v, BlockPos pos, int el, double[] h, boolean start) {
        return deviceEnd(v, pos, el, h, start, -1);
    }

    /** The same for a kit of that tier (-1: any). */
    public static CableRouter.End deviceEnd(VoxelNode v, BlockPos pos, int el, double[] h, boolean start, int tier) {
        if (el >= 6) {   // (a device placed off centre: its pins, the one nearest the click)
            var e = v.getElement(el);
            if (e == null) return null;
            VoxelNode.Pin best = null;
            double bd = Double.MAX_VALUE;
            for (VoxelNode.Pin p : VoxelNode.pins(e)) {
                // a pin of the kit's kind (AdventurAgent: a thermal kit ended at a thermal probe's signal pin)
                if (tier >= 0 && !mods.eln.node.NodeBase.compareConnectionMask(p.mask(), DevicePlacer.tierMask(tier)))
                    continue;
                double d = sq(p.tip()[0] - h[0] * 16) + sq(p.tip()[1] - h[1] * 16) + sq(p.tip()[2] - h[2] * 16);
                if (d < bd) {
                    bd = d;
                    best = p;
                }
            }
            if (best == null) return null;
            int f = e.side.toEnumFacing().get3DDataValue();
            return new CableRouter.End(pos.getX() * 16L + best.tip()[0], pos.getY() * 16L + best.tip()[1], pos.getZ() * 16L + best.tip()[2], f,
                start ? best.out() : CableRouter.reverse(best.out()));
        }
        var side = mods.eln.misc.Direction.fromInt(el);
        int f = side.toEnumFacing().get3DDataValue();
        int fa = CableRouter.axis(f);
        // an element with several terminals on an edge (edgePins: the three-channel logger, the Bus I/O Module, the
        // signal processor's bus pin): each is its own point along the edge, as VoxelNode.ends() puts them; the one of
        // the kit's kind nearest the click (AdventurAgent: a bus kit couldn't reach the processor's bus pin)
        var host = v.getElement(el);
        if (host != null && !host.edgePins().isEmpty()) {
            long[] best = null;
            int bestOut = -1;
            double bd = Double.MAX_VALUE;
            for (var lrdu : mods.eln.misc.LRDU.values()) {
                int dv = side.applyLRDU(lrdu).toEnumFacing().get3DDataValue();
                int[] c = {8, 8, 8};
                c[fa] = CablePart.plane(f);
                c[CableRouter.axis(dv)] = dv % 2 == 0 ? 0 : 16;
                List<int[]> points = new java.util.ArrayList<>();   // {x, y, z, mask}
                boolean pinned = false;
                for (var pin : host.edgePins()) {
                    if (pin.lrdu() != lrdu) continue;
                    pinned = true;
                    int along = side.applyLRDU(lrdu.left()).toEnumFacing().get3DDataValue();
                    int[] q = c.clone();
                    q[CableRouter.axis(along)] = 8 + (along % 2 == 0 ? -pin.offset() : pin.offset());
                    points.add(new int[]{q[0], q[1], q[2], pin.mask()});
                }
                if (!pinned) points.add(new int[]{c[0], c[1], c[2], host.getConnectionMask(lrdu)});
                for (int[] q : points) {
                    if (q[3] == 0 || tier >= 0 && !mods.eln.node.NodeBase.compareConnectionMask(q[3], DevicePlacer.tierMask(tier))) continue;
                    double d = sq(q[0] - h[0] * 16) + sq(q[1] - h[1] * 16) + sq(q[2] - h[2] * 16);
                    if (d < bd) {
                        bd = d;
                        best = new long[]{pos.getX() * 16L + q[0], pos.getY() * 16L + q[1], pos.getZ() * 16L + q[2]};
                        bestOut = dv;
                    }
                }
            }
            if (best == null) return null;
            return new CableRouter.End(best[0], best[1], best[2], f, start ? bestOut : CableRouter.reverse(bestOut));
        }
        int edge = -1;
        double bd = Double.MAX_VALUE;
        for (int g = 0; g < 6; g++) {
            if (CableRouter.axis(g) == fa) continue;
            var out = mods.eln.misc.Direction.fromEnumFacing(Direction.from3DDataValue(g));
            int m = v.getSideConnectionMask(out, out.getLRDUGoingTo(side));
            if (m == 0 || tier >= 0 && !mods.eln.node.NodeBase.compareConnectionMask(m, DevicePlacer.tierMask(tier))) continue;
            double d = g % 2 == 0 ? h[CableRouter.axis(g)] : 1 - h[CableRouter.axis(g)];
            if (d < bd) {
                bd = d;
                edge = g;
            }
        }
        if (edge < 0) return null;   // (no connector at all, as a wireless button)
        long[] base = {pos.getX() * 16L, pos.getY() * 16L, pos.getZ() * 16L};
        long[] p = {base[0] + 8, base[1] + 8, base[2] + 8};
        p[fa] = base[fa] + CablePart.plane(f);
        p[CableRouter.axis(edge)] = base[CableRouter.axis(edge)] + (edge % 2 == 0 ? 0 : 16);
        return new CableRouter.End(p[0], p[1], p[2], f, start ? edge : CableRouter.reverse(edge));
    }

    /**
     * A click on the surface within three quarters of a block of a machine beside it (so anywhere from the middle of the
     * next cell on), where the machine has a connector on that edge: the end goes to its port, as if the machine had
     * been clicked (AdventurAgent: a run laid from the floor next to a Signal Source stopped short of it, looked
     * connected and wasn't; Baughn: a cable must never look connected without being so). The machine is a transparent
     * node, or a six-node device on the same surface (hosted in a VoxelNode). Null if there's no such machine.
     */
    static CableRouter.End snapToPort(Level level, BlockPos pos, Direction face, double hx, double hy, double hz, boolean start, long[][] join, int tier) {
        BlockPos cb = pos.relative(face);
        double[] h = {hx, hy, hz};
        CableRouter.End best = null;
        double bd = 0.75;
        for (Direction d : Direction.values()) {
            if (d.getAxis() == face.getAxis()) continue;
            int ax = d.getAxis().ordinal();
            double dist = d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 - h[ax] : h[ax];
            if (dist > bd) continue;
            BlockPos nb = cb.relative(d);
            var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(nb, level));
            if (n == null) continue;
            CableRouter.End e;
            if (n instanceof VoxelNode v) {
                // a device on the same surface: its edge toward the cable, if it has a connector there
                var side = mods.eln.misc.Direction.fromEnumFacing(face.getOpposite());
                if (v.sideElementList[side.getInt()] == null) continue;
                var out = mods.eln.misc.Direction.fromEnumFacing(d.getOpposite());
                if (v.getSideConnectionMask(out, out.getLRDUGoingTo(side)) == 0) continue;
                Vec3 hit = Vec3.atCenterOf(nb).add(-d.getStepX() * 0.47, -d.getStepY() * 0.47, -d.getStepZ() * 0.47)
                    .add(-face.getStepX() * 0.47, -face.getStepY() * 0.47, -face.getStepZ() * 0.47);
                e = deviceEnd(v, nb, side.getInt(), new double[]{hit.x - nb.getX(), hit.y - nb.getY(), hit.z - nb.getZ()}, start, tier);
                if (e == null || e.dir() < 0) continue;
            } else {
                // the machine's face toward the cable, low on the edge the cable lies along
                Vec3 hit = Vec3.atCenterOf(nb).add(-d.getStepX() * 0.5, -d.getStepY() * 0.5, -d.getStepZ() * 0.5)
                    .add(-face.getStepX() * 0.45, -face.getStepY() * 0.45, -face.getStepZ() * 0.45);
                e = endAt(level, nb, d.getOpposite(), hit, start, join, tier);
                if (e == null) continue;   // (no connector on that edge)
            }
            best = e;
            bd = dist;
        }
        return best;
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

    public static CableRouter.End endAtForTests(Level level, BlockPos pos, Direction face, Vec3 hit, boolean start, long[][] join, int tier) {
        return endAt(level, pos, face, hit, start, join, tier);
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
        CableSpec spec = CableKitItem.specOf(kit);
        boolean other = plan != null && (plan.tier != tier || !java.util.Objects.equals(plan.spec, spec));
        if (plan != null && (other || lapse(plan, now, true, plan.dim == level.dimension(), player.position()) != null)) {
            PLANS.remove(player.getUUID());
            if (other) player.sendOverlayMessage(Component.literal("Cable route cancelled (another kind of cable in hand)"));
            plan = null;
        }
        if (plan != null) plan.touched = now;
        if (plan == null && !player.isSecondaryUseActive() && hit != null && repair(player, level, pos, hit, kit, tier, spec)) return true;
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
        CableRouter.End at = endAt(level, pos, face, hit, plan == null, join, tier);
        if (at == null) {
            player.sendOverlayMessage(Component.literal(noEnd(level, pos)));
            return true;
        }
        boolean device = at.dir() >= 0;
        if (plan == null) {
            Plan p = new Plan(tier, at, join[0], level.dimension());
            p.spec = spec;
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
                player.sendOverlayMessage(Component.literal("Can't reach that waypoint: " + r.error + " (sneak-click to cancel the route)"));
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
            player.sendOverlayMessage(Component.literal("No route: " + r.error + " (sneak-click to cancel the route)"));
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
        String err = lay(level, tier, spec, r.steps, player);
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
        CablePart.Tier t = tierOf(plan.tier, plan.spec);
        return CableRouter.route(s, t.width(), t.height(), plan.start, wps, last);
    }

    /** How many blocks of burnt cable one repair click mends at most (the burnt run, followed from the clicked piece). */
    static final int REPAIR_MAX_BLOCKS = 512;

    /**
     * A kit clicked on a burnt cable of its kind mends the burnt run there, like a repair part on a machine (Baughn
     * 2026-11-11): every burnt piece joined to the clicked one through the blocks' faces, for the kit length they
     * hold. False if the click isn't on a burnt cable (the kit then routes as usual).
     */
    static boolean repair(ServerPlayer player, ServerLevel level, BlockPos pos, Vec3 hit, ItemStack kit, int tier, CableSpec spec) {
        if (!(level.getBlockEntity(pos) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode n)) return false;
        int idx = n.partAt(hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ());
        if (idx < 0 || !(n.parts.get(idx) instanceof CablePart c) || c.burnt == null) return false;
        if (c.tier != tier || spec != null && c.spec != null && spec.mm2() != c.spec.mm2()) {
            player.sendOverlayMessage(Component.literal("This kit doesn't match the burnt cable"));
            return true;
        }
        List<Object[]> run = burntRun(level, pos, n, c);
        int len = 0;
        for (Object[] r : run) len += ((CablePart) r[1]).isKnuckle() ? 0 : ((CablePart) r[1]).length();
        boolean creative = player.isCreative();
        if (!creative && KitItem.length(kit) < len) {
            player.sendOverlayMessage(Component.literal(String.format("Mending it takes %.1f blocks of cable; the kit holds %.1f", len / 16.0, KitItem.length(kit) / 16.0)));
            return true;
        }
        for (Object[] r : run) ((VoxelNode) r[0]).repairCable((CablePart) r[1]);
        if (!creative) {
            int left = KitItem.length(kit) - len;
            if (left <= 0) kit.shrink(1);
            else KitItem.setLength(kit, left);
        }
        player.sendOverlayMessage(Component.literal(String.format("Mended %.1f blocks of burnt cable", len / 16.0)));
        return true;
    }

    /** The burnt pieces joined to c (same cable, through mirrored ports), as {node, piece} pairs, c's first. */
    static List<Object[]> burntRun(Level level, BlockPos pos, VoxelNode n, CablePart c) {
        List<Object[]> out = new ArrayList<>();
        java.util.Set<CablePart> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        java.util.ArrayDeque<Object[]> todo = new java.util.ArrayDeque<>();
        todo.add(new Object[]{n, c, pos});
        seen.add(c);
        java.util.Set<BlockPos> blocks = new java.util.HashSet<>();
        while (!todo.isEmpty()) {
            Object[] cur = todo.poll();
            VoxelNode cn = (VoxelNode) cur[0];
            CablePart cp = (CablePart) cur[1];
            BlockPos cpos = (BlockPos) cur[2];
            out.add(new Object[]{cn, cp});
            blocks.add(cpos);
            if (blocks.size() >= REPAIR_MAX_BLOCKS) break;
            // the same burnt conductor in this block
            for (VoxelPart q : cn.parts)
                if (q instanceof CablePart o && o.burnt != null && o.sameCable(cp) && !seen.contains(o) && touchesIn(o, cp)) {
                    seen.add(o);
                    todo.add(new Object[]{cn, o, cpos});
                }
            for (CablePart.Port p : cp.cablePorts()) {
                BlockPos np = cpos.relative(net.minecraft.core.Direction.from3DDataValue(p.face()));
                if (!(level.getBlockEntity(np) instanceof VoxelNodeEntity ne) || !(ne.getNode() instanceof VoxelNode nn)) continue;
                CablePart.Port m = p.mirrored();
                for (VoxelPart q : nn.parts)
                    if (q instanceof CablePart o && o.burnt != null && o.sameCable(cp) && !seen.contains(o) && o.cablePorts().contains(m)) {
                        seen.add(o);
                        todo.add(new Object[]{nn, o, np});
                    }
            }
        }
        return out;
    }

    /** Do two pieces in one block meet (an end of one on the other's line, or sharing a point)? */
    static boolean touchesIn(CablePart a, CablePart b) {
        return a.touches(b) || b.touches(a);
    }

    /** A tier's cross-section with a spec (power cables: by cross-section and insulation). */
    static CablePart.Tier tierOf(int tier, CableSpec spec) {
        return new CablePart(tier, spec, 0, new int[0]).tierInfo();
    }

    /** Route between two ends, each maybe joining an old cable (join point or null), and lay it (GameTests). */
    public static String routeAndLayJoinedForTests(Level level, int tier, CableRouter.End start, long[] startJoin, CableRouter.End end, long[] endJoin) {
        CablePart.Tier t = tierOf(tier, null);   // (the preset's cross-section, as lay() lays it)
        CableRouter.Space s = space(level);
        if (startJoin != null) s = around(s, startJoin);
        if (endJoin != null) s = around(s, endJoin);
        var r = CableRouter.route(s, t.width(), t.height(), start, List.of(), end);
        if (r.error != null) return r.error;
        return lay(level, tier, r.steps, null);
    }

    /** Route between two ends and lay it (GameTests). Null if done, else why not. */
    /**
     * Lab timing (elnlab routetime): an LV route between the tops of two blocks' columns on the live world, with and
     * without the air allowance, at the click's budget: result, length, milliseconds and expansions of each.
     */
    public static String routeTimeForTests(Level level, BlockPos a0, BlockPos b0) {
        // (on the tops of their columns: the surface at those x, z)
        var hm = net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        BlockPos a = new BlockPos(a0.getX(), level.getHeight(hm, a0.getX(), a0.getZ()) - 1, a0.getZ());
        BlockPos b = new BlockPos(b0.getX(), level.getHeight(hm, b0.getX(), b0.getZ()) - 1, b0.getZ());
        CableRouter.End ea = new CableRouter.End(a.getX() * 16L + 8, (a.getY() + 1) * 16L, a.getZ() * 16L + 8, 0, -1);
        CableRouter.End eb = new CableRouter.End(b.getX() * 16L + 8, (b.getY() + 1) * 16L, b.getZ() * 16L + 8, 0, -1);
        StringBuilder sb = new StringBuilder(a.toShortString() + " -> " + b.toShortString() + ": ");
        for (boolean air : new boolean[]{false, true}) {
            RouteKind k = RouteKind.cable("cable", 2, 1);
            if (!air) k = k.withAir(RouteKind.NO_AIR);
            long t0 = System.nanoTime();
            var r = CableRouter.route(space(level), k, ea, List.of(), eb, PipeRouter.TIME_BUDGET_MS);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            sb.append(air ? " | air: " : "no air: ").append(r.error != null ? r.error : String.format("%.1f blocks", r.length() / 16.0))
                .append(", ").append(ms).append(" ms, ").append(Router.LAST_EXPANSIONS.get()).append(" expansions");
        }
        return sb.toString();
    }

    public static String routeAndLayForTests(Level level, int tier, CableRouter.End start, CableRouter.End end) {
        CablePart.Tier t = tierOf(tier, null);   // (the preset's cross-section, as lay() lays it)
        var r = CableRouter.route(space(level), t.width(), t.height(), start, List.of(), end);
        if (r.error != null) return r.error;
        return lay(level, tier, r.steps, null);
    }

    /** Lay a route: a cable part per block piece (and the knuckles); checked first, all or nothing. */
    public static String lay(Level level, int tier, List<CableRouter.Step> steps, net.minecraft.world.entity.player.Player player) {
        return lay(level, tier, null, steps, player);
    }

    public static String lay(Level level, int tier, CableSpec spec, List<CableRouter.Step> steps, net.minecraft.world.entity.player.Player player) {
        CablePart.Tier ti = tierOf(tier, spec);
        List<CableRouter.Piece> pieces = CableRouter.pieces(steps, ti.width(), ti.height());
        List<BlockPos> at = new ArrayList<>();
        List<VoxelPart> parts = new ArrayList<>();
        CableRouter.Space sp = space(level);
        List<long[]> stretches = airStretches(sp, RouteKind.cable(tier, spec), steps);
        List<List<double[]>> chains = new ArrayList<>();
        for (long[] st : stretches) chains.add(CableHang.hang(sp, st));
        for (CableRouter.Piece p : pieces) {
            CablePart part = p.face() < 0 ? CablePart.knuckle(tier, spec, p.points()[0], p.points()[1], p.points()[2]) : new CablePart(tier, spec, p.face(), p.points());
            if (!part.isKnuckle()) part.hang = hangOf(p, stretches, chains);
            parts.add(part);
            at.add(new BlockPos((int) p.bx(), (int) p.by(), (int) p.bz()));
        }
        return Laying.lay(level, at, parts, player);   // (all or nothing)
    }

    /**
     * The route's horizontal air stretches (world voxels): {axis, the line's coordinate on the two other axes (in
     * others(axis) order), from, to}. A stretch runs from where the cable leaves its support to where it lands, along
     * one straight segment (the router keeps them straight). Vertical ones hang straight and aren't listed.
     */
    static List<long[]> airStretches(CableRouter.Space space, RouteKind kind, List<CableRouter.Step> steps) {
        List<long[]> out = new ArrayList<>();
        int lattice = kind.lattice();
        for (int i = 0; i + 1 < steps.size(); i++) {
            CableRouter.Step a = steps.get(i), b = steps.get(i + 1);
            int d = CableRouter.dirOf(a, b);
            if (d < 0) continue;
            int ax = CableRouter.axis(d);
            long[] p = {a.x(), a.y(), a.z()};
            long len = Math.abs(b.x() - a.x()) + Math.abs(b.y() - a.y()) + Math.abs(b.z() - a.z());
            long from = Long.MIN_VALUE, to = 0;
            for (long t = 0; t <= len; t += lattice) {
                boolean air = t < len && !Router.surfaceSupported(space, kind, p, b.attach(), d);
                long here = p[ax], next = here + CableRouter.DV[d][ax] * lattice;
                if (air) {
                    if (from == Long.MIN_VALUE) from = here;
                    to = next;
                } else if (from != Long.MIN_VALUE) {
                    if (ax != 1) {
                        int[] o = VoxelGeometry.others(ax);
                        out.add(new long[]{ax, p[o[0]], p[o[1]], Math.min(from, to), Math.max(from, to)});
                    }
                    from = Long.MIN_VALUE;
                }
                for (int j = 0; j < 3; j++) p[j] += CableRouter.DV[d][j] * lattice;
            }
        }
        return out;
    }

    /**
     * A piece's share of the hanging chains (CablePart.hang), or null: for each stretch whose line one of the piece's
     * segments lies on, the chain's points inside the piece's block, cut at the block's faces (interpolated on the
     * chain, so neighbouring blocks meet at the same point).
     */
    static int[] hangOf(CableRouter.Piece pc, List<long[]> stretches, List<List<double[]>> chains) {
        if (stretches.isEmpty()) return null;
        long[] base = {pc.bx() * 16, pc.by() * 16, pc.bz() * 16};
        int[] pts = pc.points();
        List<Integer> out = new ArrayList<>();
        for (int s = 0; s < stretches.size(); s++) {
            long[] st = stretches.get(s);
            int ax = (int) st[0];
            int[] o = VoxelGeometry.others(ax);
            boolean on = false;
            for (int k = 3; k < pts.length && !on; k += 3)
                on = pts[k + ax] != pts[k - 3 + ax] && pts[k + o[0]] == pts[k - 3 + o[0]] && pts[k + o[1]] == pts[k - 3 + o[1]]
                    && base[o[0]] + pts[k + o[0]] == st[1] && base[o[1]] + pts[k + o[1]] == st[2]
                    && Math.max(pts[k + ax], pts[k - 3 + ax]) + base[ax] > st[3] && Math.min(pts[k + ax], pts[k - 3 + ax]) + base[ax] < st[4];
            if (!on) continue;
            List<double[]> chain = chains.get(s);
            int n = chain.size() - 1;
            double from = st[3], span = st[4] - st[3];
            double lo = Math.max(from, base[ax]), hi = Math.min(st[4], base[ax] + 16);
            if (hi <= lo) continue;
            List<double[]> in = new ArrayList<>();
            in.add(along(chain, (lo - from) / span * n));
            for (int i = 0; i <= n; i++) {
                double u = from + i * span / n;
                if (u > lo + 1e-9 && u < hi - 1e-9) in.add(chain.get(i));
            }
            in.add(along(chain, (hi - from) / span * n));
            // (the routed line's y: the stretch's horizontal, so its y is one of the two others)
            double lineY = o[0] == 1 ? st[1] : st[2];
            out.add(ax);
            out.add((int) Math.round((lineY - base[1]) * 8));
            out.add(in.size());
            for (double[] q : in) for (int c = 0; c < 3; c++) out.add((int) Math.round((q[c] - base[c]) * 8));
        }
        if (out.isEmpty()) return null;
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    /** The chain's point at fractional node index f (linear between nodes). */
    static double[] along(List<double[]> chain, double f) {
        int i = (int) Math.max(0, Math.min(chain.size() - 2, Math.floor(f)));
        double t = Math.max(0, Math.min(1, f - i));
        double[] a = chain.get(i), b = chain.get(i + 1);
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
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
