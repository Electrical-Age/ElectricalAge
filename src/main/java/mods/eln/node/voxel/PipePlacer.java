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

    /** The pipe kit in RoutePlacer's one flow. */
    static final RoutePlacer.Spec ROUTE = new RoutePlacer.Spec("pipe", true, 0x55C8FF, 0.8f, false, false, true, false, false, false, true, true,
        false, true) {
        @Override
        RoutePlacer.Click endAt(Level level, BlockPos pos, Direction face, net.minecraft.world.phys.Vec3 hit, int tier, boolean start, boolean click) {
            PipeRouter.End port = portAt(level, pos, face, hit);
            boolean device = false;
            if (port == null) device = (port = devicePort(level, pos, hit)) != null;
            if (click && device && portTaken(level, pos, hit)) return RoutePlacer.Click.none(PORT_TAKEN);
            long[] join = port == null ? joinPoint(level, pos, hit) : null;
            PipeRouter.End surface = port == null && join == null ? surfacePoint(RouteView.obstacles(level), pos, face, hit, PipePart.GRID[tier]) : null;
            PipeRouter.End at = port != null ? port : join != null ? new PipeRouter.End(join[0], join[1], join[2], -1) : surface;
            // (the preview says nothing before a start: its outline is enough)
            if (at == null) return RoutePlacer.Click.none(click || !start ? "No room for a pipe there" : null);
            return new RoutePlacer.Click(end(at), join, port != null, port != null, null);
        }

        @Override
        RoutePlacer.Routed route(RouteView view, int tier, CableSpec spec, CableRouter.End start, long[] startJoin, List<CableRouter.End> wps,
                                 CableRouter.End end, long[] endJoin, long budgetMs) {
            PipeRouter.Space sp = RouteView.obstacles(view);
            if (startJoin != null) sp = RouteView.freeAround(sp, startJoin, PipePart.JOIN_CLEAR);
            if (endJoin != null) sp = RouteView.freeAround(sp, endJoin, PipePart.JOIN_CLEAR);
            PipeRouter.Result r = PipeRouter.route(sp, PipePart.GRID[tier], end(start), ends(wps), end(end), budgetMs);
            return new RoutePlacer.Routed(r.error, r.error == null ? r.length() : 0, r.points);
        }

        @Override
        @SuppressWarnings("unchecked")
        String lay(Level level, int tier, CableSpec spec, RoutePlacer.Routed r, long[] startJoin, long[] endJoin, net.minecraft.world.entity.player.Player player) {
            List<long[]> joins = new ArrayList<>();
            if (startJoin != null) joins.add(startJoin);
            if (endJoin != null) joins.add(endJoin);
            return layJoined(level, tier, (List<long[]>) r.path(), joins, player);
        }

        @Override
        KitPreview.Answer answer(RouteView view, int tier, CableSpec spec, CableRouter.End start, long[] startJoin, List<CableRouter.End> wps,
                                 CableRouter.End end, long[] endJoin, int have, RoutePlacer.Click c) {
            return KitPreview.pipeAnswer(view, endJoin, startJoin, tier, end(start), ends(wps), end(end), have, c.port());
        }

        @Override
        @SuppressWarnings("unchecked")
        List<double[]> trace(RoutePlacer.Routed r) {
            List<double[]> out = new ArrayList<>();
            for (long[] q : (List<long[]>) r.path()) out.add(new double[]{q[0], q[1], q[2], 0, 0, 0});
            return out;
        }

        @Override
        String started(KitSupply kit, boolean onJoin) {
            return (onJoin ? "Pipe route started on the pipe" : "Pipe route started")
                + ": click for waypoints, a port or pipe to finish, the last waypoint again to end it there; " + kit.undoHint() + " to undo";
        }

        @Override
        String waypointTail() {
            return " (click it again to end the pipe there)";
        }

        @Override
        String startedWhat() {
            return "pipe";
        }
    };

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
        PipeRouter.End best = null;
        double bestD = Double.MAX_VALUE;
        List<FluidDevice.FluidPort> ports = new ArrayList<>();
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level));
        if (n instanceof VoxelNode v) ports.addAll(v.fluidPorts());
        // (a multiblock's ports name their own blocks: the 2x2x2 Large Boiler's, at its node or on its ghost blocks)
        else if (n instanceof TransparentNode t && t.element instanceof mods.eln.oil.LargeBoilerElement b) ports.addAll(b.portsAt(pos.asLong()));
        else if (n instanceof TransparentNode t && t.element instanceof FluidDevice d) ports.addAll(d.fluidPorts());
        else if (n instanceof mods.eln.oil.LargeBoilerElement.FaceNode g) ports.addAll(g.fluidPorts());
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
        boolean anySide = !onFace && inside && !(n instanceof VoxelNode);
        for (FluidDevice.FluidPort p : ports) {
            if (anySide ? p.face() < 2 || p.face() > 5 : p.face() != face.get3DDataValue()) continue;
            Direction pf = Direction.from3DDataValue(p.face());
            int u = p.at() / 17, w = p.at() % 17;
            long bx = pos.getX() * 16L, by = pos.getY() * 16L, bz = pos.getZ() * 16L;
            long x, y, z;
            switch (pf.getAxis()) {
                case Y -> { x = bx + u; z = bz + w; y = by + (pf == Direction.UP ? 16 : 0); }
                case Z -> { x = bx + u; y = by + w; z = bz + (pf == Direction.SOUTH ? 16 : 0); }
                default -> { y = by + u; z = bz + w; x = bx + (pf == Direction.EAST ? 16 : 0); }
            }
            PipeRouter.End end = new PipeRouter.End(x, y, z, dirOf(pf));
            if (hit == null) return end;
            double d = Math.pow(x / 16.0 - hit.x, 2) + Math.pow(y / 16.0 - hit.y, 2) + Math.pow(z / 16.0 - hit.z, 2);
            if (d < bestD) {
                bestD = d;
                best = end;
            }
        }
        return best;
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

    /** A pipe sensor device's port that already has a pipe: no second route starts or ends there (a lab test: clicking
     *  the device twice laid a second pipe beside the first). */
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
    public static boolean use(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, ItemStack kit, int tier,
                              net.minecraft.world.phys.Vec3 hit) {
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
            if (!(level.getBlockEntity(jb) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode n)) return "the pipe is gone";
            Laying.Join j = new Laying.Join(jb, new PipePart(tier, mine.get(0)), (int) (join[0] - jb.getX() * 16L), (int) (join[1] - jb.getY() * 16L),
                (int) (join[2] - jb.getZ() * 16L));
            String jerr = n.joinError(j.part(), j.x(), j.y(), j.z());   // (each join's refusal before the next join's)
            if (jerr != null) return jerr;
            jbs.add(jb);
            js.add(j);
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
