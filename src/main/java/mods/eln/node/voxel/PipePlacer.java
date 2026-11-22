package mods.eln.node.voxel;

import mods.eln.node.transparent.TransparentNode;
import mods.eln.sim.fluid.FluidDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The pipe kit's side of RoutePlacer (Baughn 2026-11-11: one kit UX): where a click ends (a port: a device's flange or
 * a pipe sensor's port; a point on an old pipe, for a branch; a point in the block in front of a surface, the pipe
 * passing through it), how a pipe is routed through the middle of blocks, and laying it with a junction where it joins
 * an old pipe (ROUTE, the pipe spec; pipes of any tier join, the junction taking the lower). The route's state and
 * clicks are RoutePlacer's.
 */
public final class PipePlacer {
    private PipePlacer() {
    }

    /** The pipe kit in RoutePlacer's one flow (a route's path: the router's points). */
    static final RoutePlacer.Spec<List<long[]>> ROUTE = new RoutePlacer.Spec<>("pipe", "a port or pipe", 0x55C8FF, 0.8f, true, false) {
        @Override
        RoutePlacer.Click endAt(Level level, BlockPos pos, Direction face, net.minecraft.world.phys.Vec3 hit, int tier, boolean start, boolean click) {
            FluidDevice.FluidPort fp = portNear(level, pos, face, hit);
            if (fp != null && coupled(level, fp)) return RoutePlacer.Click.none(PORT_TAKEN);
            PipeRouter.End port = fp == null ? null : endOf(pos, fp);
            boolean device = false;
            if (port == null) device = (port = devicePort(level, pos, hit)) != null;
            if (device && portTaken(level, pos, hit)) return RoutePlacer.Click.none(PORT_TAKEN);
            long[] join = port == null ? joinPoint(level, pos, hit) : null;
            PipeRouter.End surface = port == null && join == null ? surfacePoint(RouteView.obstacles(level), pos, face, hit, PipePart.GRID[tier]) : null;
            PipeRouter.End at = port != null ? port : join != null ? new PipeRouter.End(join[0], join[1], join[2], -1) : surface;
            // (the preview says nothing before a start: its outline is enough)
            if (at == null) return RoutePlacer.Click.none(click || !start ? "No room for a pipe there" : null);
            return new RoutePlacer.Click(end(at), join, port != null, port != null, null);
        }

        @Override
        RoutePlacer.Routed<List<long[]>> route(RouteView view, int tier, CableSpec spec, CableRouter.End start, long[] startJoin, List<CableRouter.End> wps,
                                               CableRouter.End end, long[] endJoin, long budgetMs) {
            // (the two cubes are freed in either order alike: obstacles are boxes, so a box is blocked by what it overlaps
            // outside both)
            // (and through the old pipe itself near a join, JoinRun: a route running inside it is cut where it leaves it,
            // and joins it there; todo #516)
            JoinRun sj = startJoin == null ? null : JoinRun.pipe(view, startJoin);
            JoinRun ej = endJoin == null ? null : JoinRun.pipe(view, endJoin);
            RoutePlacer.Routed<List<long[]>> r = routeJoined(view, tier, start, startJoin, sj, wps, end, endJoin, ej, true, budgetMs);
            // (back onto or across the old pipe away from its cut, as after a waypoint, which the freed voxels allowed:
            // routed again without them, as before JoinRun; through the free cube round the join point it still can, and
            // is refused, not laid through the old pipe)
            if (crosses(r, sj, ej, tier)) {
                r = routeJoined(view, tier, start, startJoin, sj, wps, end, endJoin, ej, false, budgetMs);
                if (crosses(r, sj, ej, tier)) return new RoutePlacer.Routed<>("it crosses the pipe it joins", 0, null);
            }
            if (r.error() != null) return r;
            // the junctions where the route meets the old pipes, checked now (the cut moves them off the click), so the
            // preview and the click refuse alike, with the reason
            for (long[] j : new long[][]{r.startJoin(), r.endJoin()}) {
                String err = j == null ? null : joinError(view, tier, r.path(), j);
                if (err != null) return RoutePlacer.Routed.said("Can't join there: " + err);
            }
            return r;
        }

        @Override
        String lay(Level level, int tier, CableSpec spec, RoutePlacer.Routed<List<long[]>> r, long[] startJoin, long[] endJoin,
                   net.minecraft.world.entity.player.Player player) {
            // (the joins where the route meets the old pipes: route() moves one to where the route leaves the old pipe)
            List<long[]> joins = new ArrayList<>();
            if (r.startJoin() != null) joins.add(r.startJoin());
            if (r.endJoin() != null) joins.add(r.endJoin());
            return layJoined(level, tier, r.path(), joins, player);
        }

        @Override
        List<int[]> boxes(RoutePlacer.Routed<List<long[]>> r, int tier, CableSpec spec) {
            List<int[]> bs = new ArrayList<>();
            for (var e : PipeRouter.perBlock(r.path()).entrySet()) {
                long bx = e.getKey().get(0), by = e.getKey().get(1), bz = e.getKey().get(2);
                for (int[] pts : e.getValue())
                    for (int[] b : new PipePart(tier, pts).boxes())
                        bs.add(new int[]{(int) bx * 16 + b[0], (int) by * 16 + b[1], (int) bz * 16 + b[2], (int) bx * 16 + b[3], (int) by * 16 + b[4], (int) bz * 16 + b[5]});
            }
            return bs;
        }

        @Override
        List<double[]> trace(RoutePlacer.Routed<List<long[]>> r) {
            List<double[]> out = new ArrayList<>();
            for (long[] q : r.path()) out.add(new double[]{q[0], q[1], q[2], 0, 0, 0});
            return out;
        }
    };

    /** One routing: free through the old pipe near the joins (free) or not, cut where it leaves them. */
    static RoutePlacer.Routed<List<long[]>> routeJoined(RouteView view, int tier, CableRouter.End start, long[] startJoin, JoinRun sj,
                                                         List<CableRouter.End> wps, CableRouter.End end, long[] endJoin, JoinRun ej, boolean free,
                                                         long budgetMs) {
        RouteView v = view;
        if (free && sj != null) v = sj.freeing(v);
        if (free && ej != null) v = ej.freeing(v);
        // (the two cubes are freed in either order alike: obstacles are boxes, so a box is blocked by what it overlaps
        // outside both)
        PipeRouter.Space sp = RouteView.obstacles(v);
        if (startJoin != null) sp = RouteView.freeAround(sp, startJoin, PipePart.JOIN_CLEAR);
        if (endJoin != null) sp = RouteView.freeAround(sp, endJoin, PipePart.JOIN_CLEAR);
        PipeRouter.Result r = PipeRouter.route(sp, PipePart.GRID[tier], end(start), ends(wps), end(end), budgetMs);
        if (r.error != null) return new RoutePlacer.Routed<>(r.error, 0, null);
        List<long[]> pts = r.points;
        if (sj != null && pts != null) pts = sj.trimHead(pts);
        if (ej != null && pts != null) pts = ej.trimTail(pts);
        if (pts == null) return RoutePlacer.Routed.said(RoutePlacer.INSIDE);
        long[] from = startJoin == null ? null : joinAt(pts.get(0)), to = endJoin == null ? null : joinAt(pts.get(pts.size() - 1));
        return new RoutePlacer.Routed<>(null, new PipeRouter.Result(pts, null).length(), pts, from, to);
    }

    /** Does a route found pass through the old pipe at either join away from its cut (JoinRun.crosses)? */
    static boolean crosses(RoutePlacer.Routed<List<long[]>> r, JoinRun sj, JoinRun ej, int tier) {
        if (r.error() != null) return false;
        int h = PipePart.GRID[tier] / 2;   // (a pipe's box: its centre line grown by half its bore)
        boolean s = sj != null, e = ej != null;
        return s && sj.crosses(r.path(), null, s, e, h, h, 0) || e && ej.crosses(r.path(), null, s, e, h, h, 0);
    }

    /** Why the route's piece in join j's block can't join the old pipe at j (VoxelNode.joinError, on the view), or null. */
    static String joinError(RouteView view, int tier, List<long[]> pts, long[] j) {
        BlockPos b = new BlockPos((int) Math.floorDiv(j[0], 16), (int) Math.floorDiv(j[1], 16), (int) Math.floorDiv(j[2], 16));
        List<int[]> mine = PipeRouter.perBlock(pts).get(List.of((long) b.getX(), (long) b.getY(), (long) b.getZ()));
        if (mine == null || mine.size() != 1) return "the route doesn't end in the pipe's block";
        RouteView.Voxels vx = view.voxels(b);
        return VoxelNode.joinError(view.parts(b), vx == null ? null : vx.used(), new PipePart(tier, mine.get(0)), (int) (j[0] - b.getX() * 16L),
            (int) (j[1] - b.getY() * 16L), (int) (j[2] - b.getZ() * 16L));
    }

    /**
     * A route end on an old pipe as a join: the point, or null on a block's face plane (the route cut there, at the old
     * pipe's end on the face: the two ends meet across the face as ports, no junction).
     */
    // (a cut at the old pipe's end inside a block is a join at its end, as clicking that end was: a junction with two
    // arms, not a plain straight continuation; that would mean lengthening the old piece, its fluid node and id, instead
    // of adding one)
    static long[] joinAt(long[] p) {
        for (int a = 0; a < 3; a++) if (Math.floorMod(p[a], 16) == 0) return null;
        return p;
    }

    /** A face (3D value) by pipe direction (0..5 = -x +x -y +y -z +z, dirOf), and back: RoutePlacer's ends name faces. */
    private static final int[] FACE_OF_DIR = {4, 5, 0, 1, 2, 3}, DIR_OF_FACE = {2, 3, 4, 5, 0, 1};

    /** A pipe end as RoutePlacer's (attach -1, dir as a face), and back. */
    static CableRouter.End end(PipeRouter.End e) {
        return new CableRouter.End(e.x(), e.y(), e.z(), -1, e.dir() < 0 ? -1 : FACE_OF_DIR[e.dir()]);
    }

    static PipeRouter.End end(CableRouter.End e) {
        return new PipeRouter.End(e.x(), e.y(), e.z(), e.dir() < 0 ? -1 : DIR_OF_FACE[e.dir()]);
    }

    static List<PipeRouter.End> ends(List<CableRouter.End> es) {
        List<PipeRouter.End> out = new ArrayList<>(es.size());
        for (CableRouter.End e : es) out.add(end(e));
        return out;
    }

    /**
     * The point a pipe of this bore would start, pass or end at for a click on a block's face: in the block in front of
     * the face, on the pipe lattice, as near the face as the lattice allows (it lies along the floor or wall clicked)
     * and nearest the clicked point across it; never in a face plane. Null if a pipe there would hit something.
     */
    static PipeRouter.End surfacePoint(PipeRouter.Space space, BlockPos pos, Direction face, net.minecraft.world.phys.Vec3 hit, int bore) {
        BlockPos at = pos.relative(face);
        long[] base = {at.getX() * 16L, at.getY() * 16L, at.getZ() * 16L};
        double[] h = {hit.x * 16, hit.y * 16, hit.z * 16};
        int n = face.getAxis().ordinal();   // (X 0, Y 1, Z 2)
        long[] p = new long[3];
        int lattice = PipeRouter.LATTICE;
        for (int a = 0; a < 3; a++) {
            if (a == n) p[a] = base[a] + (face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? lattice : 16 - lattice);
            else {
                long v = Math.round((h[a] - base[a]) / lattice) * lattice;
                p[a] = base[a] + Math.max(lattice, Math.min(16 - lattice, v));
            }
        }
        long r = bore / 2;
        if (space.blocked(p[0] - r, p[1] - r, p[2] - r, p[0] + r, p[1] + r, p[2] + r)) return null;
        return new PipeRouter.End(p[0], p[1], p[2], -1);
    }

    public static PipeRouter.End surfacePointForTests(Level level, BlockPos pos, Direction face, net.minecraft.world.phys.Vec3 hit, int bore) {
        return surfacePoint(RouteView.obstacles(level), pos, face, hit, bore);
    }

    /** Drop the player's route if it is a pipe's (tests: mock players share one UUID). */
    public static void forget(UUID player) {
        RoutePlacer.forget(player, ROUTE);
    }

    /** The port on this block's face, if any (a device's flange, or a pipe's end on that face), as a routing end
     *  leaving the face. */
    static PipeRouter.End portAt(Level level, BlockPos pos, Direction face) {
        return portAt(level, pos, face, null);
    }

    /**
     * The device port on that face of the block; of several on one face (the 230V compressor's IN and OUT flanges,
     * 2026-11-07), the one nearest the clicked point hit (world coordinates; null: the first).
     */
    static PipeRouter.End portAt(Level level, BlockPos pos, Direction face, net.minecraft.world.phys.Vec3 hit) {
        FluidDevice.FluidPort p = portNear(level, pos, face, hit);
        return p == null ? null : endOf(pos, p);
    }

    /** The fluid ports a block offers a pipe kit (a device's, a multiblock's ports on that block, a voxel block's pipe ends). */
    static List<FluidDevice.FluidPort> portsAt(Level level, BlockPos pos) {
        List<FluidDevice.FluidPort> ports = new ArrayList<>();
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level));
        if (n instanceof VoxelNode v) ports.addAll(v.fluidPorts());
        // (a multiblock's ports name their own blocks: the 2x2x2 Large Boiler's, at its node or on its ghost blocks)
        else if (n instanceof TransparentNode t && t.element instanceof mods.eln.oil.LargeBoilerElement b) ports.addAll(b.portsAt(pos.asLong()));
        else if (n instanceof TransparentNode t && t.element instanceof FluidDevice d) ports.addAll(d.fluidPorts());
        else if (n instanceof mods.eln.oil.LargeBoilerElement.FaceNode g) ports.addAll(g.fluidPorts());
        return ports;
    }

    /** The port portAt picks on that face of the block (nearest hit), or null. */
    static FluidDevice.FluidPort portNear(Level level, BlockPos pos, Direction face, net.minecraft.world.phys.Vec3 hit) {
        List<FluidDevice.FluidPort> ports = portsAt(level, pos);
        boolean voxel = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level)) instanceof VoxelNode;
        // (none on the face clicked, the click on the model inside the block rather than on its face: the side port
        // nearest the click, of any side. Baughn 2026-11-09: a tank box's blocks are its round shell, so a corner
        // block's side faces are empty air, and a click on the shell there hit a face of a step of it)
        boolean inside = hit != null && switch (face) {
            case EAST -> hit.x < pos.getX() + 1 - 1e-4;
            case WEST -> hit.x > pos.getX() + 1e-4;
            case SOUTH -> hit.z < pos.getZ() + 1 - 1e-4;
            case NORTH -> hit.z > pos.getZ() + 1e-4;
            case UP -> hit.y < pos.getY() + 1 - 1e-4;
            case DOWN -> hit.y > pos.getY() + 1e-4;
        };
        boolean onFace = false;
        for (FluidDevice.FluidPort p : ports) onFace |= p.face() == face.get3DDataValue();
        // (only for devices: inside a voxel block a click on a pipe's body is a join or a waypoint, never its end on
        // another face, which already couples to the next block)
        boolean anySide = !onFace && inside && !voxel;
        FluidDevice.FluidPort best = null;
        double bestD = Double.MAX_VALUE;
        for (FluidDevice.FluidPort p : ports) {
            if (anySide ? p.face() < 2 || p.face() > 5 : p.face() != face.get3DDataValue()) continue;
            if (hit == null) return p;
            PipeRouter.End e = endOf(pos, p);
            double d = Math.pow(e.x() / 16.0 - hit.x, 2) + Math.pow(e.y() / 16.0 - hit.y, 2) + Math.pow(e.z() / 16.0 - hit.z, 2);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** Port p of the block at pos as a routing end leaving its face (world voxels). */
    static PipeRouter.End endOf(BlockPos pos, FluidDevice.FluidPort p) {
        Direction pf = Direction.from3DDataValue(p.face());
        int u = p.at() / 17, w = p.at() % 17;
        long bx = pos.getX() * 16L, by = pos.getY() * 16L, bz = pos.getZ() * 16L;
        long x, y, z;
        switch (pf.getAxis()) {
            case Y -> { x = bx + u; z = bz + w; y = by + (pf == Direction.UP ? 16 : 0); }
            case Z -> { x = bx + u; y = by + w; z = bz + (pf == Direction.SOUTH ? 16 : 0); }
            default -> { y = by + u; z = bz + w; x = bx + (pf == Direction.EAST ? 16 : 0); }
        }
        return new PipeRouter.End(x, y, z, dirOf(pf));
    }

    /**
     * Is port p coupled already: a port of the block in front of its face facing back at the same spot (a pipe's end
     * there, or another device's flange)? FluidWorld's rule for coupling two ports, so a port refused here is one a new
     * pipe couldn't join (AdventurAgent: a second route to a tank's piped port "gave up" instead of saying so).
     */
    static boolean coupled(Level level, FluidDevice.FluidPort p) {
        Direction f = Direction.from3DDataValue(p.face());
        BlockPos other = BlockPos.of(p.pos()).relative(f);
        for (FluidDevice.FluidPort q : portsAt(level, other))
            if (q.pos() == other.asLong() && q.face() == f.getOpposite().get3DDataValue() && q.at() == p.at() && q.node() != p.node()) return true;
        return false;
    }

    /**
     * The port of a pipe sensor device under the crosshair (PipeTransmitterElement.port: inside the block, over the
     * device), as a routing end any way round (Baughn 2026-11-02: pipe is laid from the sensor to the pipe to measure),
     * or null. A route ends there capped, as at any inner end.
     */
    static PipeRouter.End devicePort(Level level, BlockPos pos, net.minecraft.world.phys.Vec3 hit) {
        var t = deviceAt(level, pos, hit);
        if (t == null) return null;
        int[] p = t.port();
        return new PipeRouter.End(pos.getX() * 16L + p[0], pos.getY() * 16L + p[1], pos.getZ() * 16L + p[2], -1);
    }

    /** The pipe sensor device under the crosshair, or null. */
    static mods.eln.sixnode.pipetransmitter.PipeTransmitterElement deviceAt(Level level, BlockPos pos, net.minecraft.world.phys.Vec3 hit) {
        if (hit == null || !(level.getBlockEntity(pos) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode n)) return null;
        int el = n.aimed(hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ()).element();
        return el >= 0 && n.getElement(el) instanceof mods.eln.sixnode.pipetransmitter.PipeTransmitterElement t ? t : null;
    }

    /** A port that already has a pipe (a machine's flange coupled to a pipe end, a pipe sensor's port with its pipe): no
     *  second route starts or ends there (a lab test: clicking the device twice laid a second pipe beside the first). */
    static final String PORT_TAKEN = "Its port already has a pipe";

    static boolean portTaken(Level level, BlockPos pos, net.minecraft.world.phys.Vec3 hit) {
        var t = deviceAt(level, pos, hit);
        return t != null && t.pipe() != null;
    }

    /** Route from start to end (ports, or a device's port) and lay it (GameTests). Null if done, else why not. */
    public static String routeForTests(Level level, int tier, PipeRouter.End start, PipeRouter.End end) {
        var r = PipeRouter.route(RouteView.obstacles(level), PipePart.GRID[tier], start, List.of(), end);
        return r.error != null ? r.error : lay(level, tier, r.points, null);
    }

    public static PipeRouter.End devicePortForTests(Level level, BlockPos pos, net.minecraft.world.phys.Vec3 hit) {
        return devicePort(level, pos, hit);
    }

    /** Route from a port to join a pipe at join (world voxels) and lay it (GameTests). Null if done, else why not. */
    public static String routeAndJoinForTests(Level level, int tier, PipeRouter.End start, long[] join) {
        var r = PipeRouter.route(RouteView.freeAround(RouteView.obstacles(level), join, PipePart.JOIN_CLEAR), PipePart.GRID[tier], start, List.of(),
            new PipeRouter.End(join[0], join[1], join[2], -1));
        if (r.error != null) return r.error;
        return layJoined(level, tier, r.points, join, null);
    }

    /** Lay a route's pieces with placing forced to fail at piece failAt (GameTests: nothing may stay laid). */
    public static String layFailingForTests(Level level, int tier, List<long[]> points, int failAt) {
        List<BlockPos> at = new ArrayList<>();
        List<VoxelPart> parts = new ArrayList<>();
        pieceParts(tier, PipeRouter.perBlock(points), at, parts);
        return Laying.layFailingAtForTests(level, at, parts, failAt < 0 ? parts.size() + failAt : failAt);
    }

    /** Check two pipe pieces in one block as one route (GameTests: a route may not cross itself). */
    public static String checkTwoForTests(Level level, BlockPos pos, int tier, int[] a, int[] b) {
        return Laying.check(level, List.of(pos, pos), List.of(new PipePart(tier, a), new PipePart(tier, b)));
    }

    public static long[] joinPointForTests(Level level, BlockPos pos, net.minecraft.world.phys.Vec3 hit) {
        return joinPoint(level, pos, hit);
    }

    public static PipeRouter.End portAtForTests(Level level, BlockPos pos, Direction face) {
        return portAt(level, pos, face);
    }

    /** (for tests) the port nearest a point on the face (world coordinates). */
    public static PipeRouter.End portAtForTests(Level level, BlockPos pos, Direction face, net.minecraft.world.phys.Vec3 hit) {
        return portAt(level, pos, face, hit);
    }

    /** Router direction (0..5 = -x +x -y +y -z +z) of a face. */
    static int dirOf(Direction f) {
        return switch (f) {
            case WEST -> 0;
            case EAST -> 1;
            case DOWN -> 2;
            case UP -> 3;
            case NORTH -> 4;
            case SOUTH -> 5;
        };
    }

    /** A kit used on a block face: start, extend or finish a route (RoutePlacer). Returns false only for a sneak-click with no route. */
    public static boolean use(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, ItemStack kit, net.minecraft.world.phys.Vec3 hit) {
        return use(player, level, pos, face, KitSupply.of(kit), hit, player.isSecondaryUseActive());
    }

    /** The same, laid from a supply (a kit, or several taken as one); sneak: the click takes back a waypoint or cancels. */
    public static boolean use(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, KitSupply kit, net.minecraft.world.phys.Vec3 hit,
                              boolean sneak) {
        return RoutePlacer.use(player, level, pos, face, kit, hit, sneak);
    }

    /** Is a pipe route being laid by this player? */
    public static boolean active(UUID player) {
        return RoutePlacer.active(player, ROUTE);
    }

    /** The point on the pipe under the crosshair where a route could join it (world voxels: a lattice point on its
     *  centre line, inside the block), or null. */
    static long[] joinPoint(Level level, BlockPos pos, net.minecraft.world.phys.Vec3 hit) {
        if (hit == null || !(level.getBlockEntity(pos) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode n)) return null;
        double hx = (hit.x - pos.getX()) * 16, hy = (hit.y - pos.getY()) * 16, hz = (hit.z - pos.getZ()) * 16;
        int idx = n.partAt(hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ());
        if (idx < 0 || !(n.parts.get(idx) instanceof PipePart pipe)) return null;
        long[] best = null;
        double bd = Double.MAX_VALUE;
        for (int x = 4; x <= 12; x += 4)
            for (int y = 4; y <= 12; y += 4)
                for (int z = 4; z <= 12; z += 4) {
                    if (!pipe.passes(x, y, z) || n.fittingAt(x, y, z)) continue;
                    double d = (x - hx) * (x - hx) + (y - hy) * (y - hy) + (z - hz) * (z - hz);
                    if (d < bd) {
                        bd = d;
                        best = new long[]{pos.getX() * 16L + x, pos.getY() * 16L + y, pos.getZ() * 16L + z};
                    }
                }
        return best;
    }

    /** Lay a route that ends joining a pipe at join (world voxels): the last block's piece goes in as a junction. */
    public static String layJoined(Level level, int tier, List<long[]> points, long[] join, net.minecraft.world.entity.player.Player player) {
        return layJoined(level, tier, points, join == null ? List.of() : List.of(join), player);
    }

    /**
     * Lay a route whose ends may join old pipes (world voxel points: a start on a pipe, a finish on one): each join
     * block's piece goes in as a junction, in the same all-or-nothing lay as the other pieces (Laying: every junction
     * and every other piece is checked before anything is laid).
     */
    public static String layJoined(Level level, int tier, List<long[]> points, List<long[]> joins, net.minecraft.world.entity.player.Player player) {
        var pieces = PipeRouter.perBlock(points);
        List<Laying.Join> js = new ArrayList<>();
        List<BlockPos> jbs = new ArrayList<>();
        for (long[] join : joins) {
            BlockPos jb = new BlockPos((int) Math.floorDiv(join[0], 16), (int) Math.floorDiv(join[1], 16), (int) Math.floorDiv(join[2], 16));
            if (jbs.contains(jb)) return "it starts and ends on the same pipe block";
            List<int[]> mine = pieces.remove(List.of((long) jb.getX(), (long) jb.getY(), (long) jb.getZ()));
            if (mine == null || mine.size() != 1) return "the route doesn't end in the pipe's block";
            jbs.add(jb);
            js.add(new Laying.Join(jb, new PipePart(tier, mine.get(0)), (int) (join[0] - jb.getX() * 16L), (int) (join[1] - jb.getY() * 16L),
                (int) (join[2] - jb.getZ() * 16L)));
        }
        List<BlockPos> at = new ArrayList<>();
        List<VoxelPart> parts = new ArrayList<>();
        pieceParts(tier, pieces, at, parts);
        return Laying.lay(level, at, parts, js, player);   // (all or nothing, the junctions too)
    }

    /** Lay the route: one pipe part per block piece; all or nothing. */
    public static String lay(Level level, int tier, List<long[]> points, net.minecraft.world.entity.player.Player player) {
        return lay(level, tier, PipeRouter.perBlock(points), player);
    }

    static String lay(Level level, int tier, java.util.Map<List<Long>, List<int[]>> pieces, net.minecraft.world.entity.player.Player player) {
        List<BlockPos> at = new ArrayList<>();
        List<VoxelPart> parts = new ArrayList<>();
        pieceParts(tier, pieces, at, parts);
        return Laying.lay(level, at, parts, player);   // (all or nothing)
    }

    /** One pipe part per block piece, into at and parts (in the same order). */
    static void pieceParts(int tier, java.util.Map<List<Long>, List<int[]>> pieces, List<BlockPos> at, List<VoxelPart> parts) {
        for (var e : pieces.entrySet())
            for (int[] pts : e.getValue()) {
                at.add(new BlockPos(e.getKey().get(0).intValue(), e.getKey().get(1).intValue(), e.getKey().get(2).intValue()));
                parts.add(new PipePart(tier, pts));
            }
    }
}
