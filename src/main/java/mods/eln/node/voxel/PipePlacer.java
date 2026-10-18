package mods.eln.node.voxel;

import mods.eln.node.transparent.TransparentNode;
import mods.eln.sim.fluid.FluidDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Laying a routed pipe (notes/pipes/DESIGN.md section 4) on the server: right-click a port (a device's flange, or a
 * pipe's open end) with a pipe kit to start; right-click other blocks to add waypoints (the pipe passes through the
 * centre of the block in front of the face clicked); right-click a port to finish: the route is found, checked
 * against the kit's length and laid. Sneak-right-click takes back the last waypoint (or cancels). The planned route
 * shows as particles to the player.
 */
public final class PipePlacer {
    private PipePlacer() {
    }

    /** A route being laid by a player. */
    static final class Plan {
        final int tier;
        final PipeRouter.End start;
        final List<PipeRouter.End> waypoints = new ArrayList<>();
        final net.minecraft.resources.ResourceKey<Level> dim;

        Plan(int tier, PipeRouter.End start, net.minecraft.resources.ResourceKey<Level> dim) {
            this.tier = tier;
            this.start = start;
            this.dim = dim;
        }
    }

    static final Map<UUID, Plan> PLANS = new HashMap<>();

    /** The world as the router sees it: solid blocks and other devices are in the way, and so are used voxels of
     *  voxel nodes; air, replaceable blocks and liquids are free. */
    public static PipeRouter.Space space(Level level) {
        return (x0, y0, z0, x1, y1, z1) -> {
            for (long bx = Math.floorDiv(x0, 16); bx <= Math.floorDiv(x1 - 1, 16); bx++)
                for (long by = Math.floorDiv(y0, 16); by <= Math.floorDiv(y1 - 1, 16); by++)
                    for (long bz = Math.floorDiv(z0, 16); bz <= Math.floorDiv(z1 - 1, 16); bz++) {
                        BlockPos p = new BlockPos((int) bx, (int) by, (int) bz);
                        if (!level.isLoaded(p)) return true;
                        BlockState s = level.getBlockState(p);
                        if (s.isAir() || s.canBeReplaced() || s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) continue;
                        if (s.getBlock() instanceof VoxelNodeBlock && level.getBlockEntity(p) instanceof VoxelNodeEntity e) {
                            List<VoxelPart> parts = e.parts();
                            int lx0 = (int) (x0 - bx * 16), ly0 = (int) (y0 - by * 16), lz0 = (int) (z0 - bz * 16);
                            int lx1 = (int) (x1 - bx * 16), ly1 = (int) (y1 - by * 16), lz1 = (int) (z1 - bz * 16);
                            if (e.getNode() instanceof VoxelNode n) {
                                if (n.usedIn(lx0, ly0, lz0, lx1, ly1, lz1)) return true;
                            } else for (VoxelPart part : parts)
                                for (int[] b : part.boxes())
                                    if (b[0] < lx1 && lx0 < b[3] && b[1] < ly1 && ly0 < b[4] && b[2] < lz1 && lz0 < b[5]) return true;
                            continue;
                        }
                        return true;
                    }
            return false;
        };
    }

    /** The port on this block's face, if any (a device's flange, or a pipe's end on that face), as a routing end
     *  leaving the face. */
    static PipeRouter.End portAt(Level level, BlockPos pos, Direction face) {
        List<FluidDevice.FluidPort> ports = new ArrayList<>();
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level));
        if (n instanceof VoxelNode v) ports.addAll(v.fluidPorts());
        else if (n instanceof TransparentNode t && t.element instanceof FluidDevice d) ports.addAll(d.fluidPorts());
        for (FluidDevice.FluidPort p : ports) {
            if (p.face() != face.get3DDataValue()) continue;
            int u = p.at() / 17, w = p.at() % 17;
            long bx = pos.getX() * 16L, by = pos.getY() * 16L, bz = pos.getZ() * 16L;
            long x, y, z;
            switch (face.getAxis()) {
                case Y -> { x = bx + u; z = bz + w; y = by + (face == Direction.UP ? 16 : 0); }
                case Z -> { x = bx + u; y = by + w; z = bz + (face == Direction.SOUTH ? 16 : 0); }
                default -> { y = by + u; z = bz + w; x = bx + (face == Direction.EAST ? 16 : 0); }
            }
            return new PipeRouter.End(x, y, z, dirOf(face));
        }
        return null;
    }

    public static PipeRouter.End portAtForTests(Level level, BlockPos pos, Direction face) {
        return portAt(level, pos, face);
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

    /** A kit used on a block face: start, extend or finish a route. Returns true if it handled the click. */
    public static boolean use(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, ItemStack kit, int tier) {
        Plan plan = PLANS.get(player.getUUID());
        if (plan != null && plan.dim != level.dimension()) {
            PLANS.remove(player.getUUID());
            plan = null;
        }
        PipeRouter.End port = portAt(level, pos, face);
        if (player.isSecondaryUseActive()) {
            if (plan == null) return false;
            if (plan.waypoints.isEmpty()) {
                PLANS.remove(player.getUUID());
                player.sendOverlayMessage(Component.literal("Pipe route cancelled"));
            } else {
                plan.waypoints.remove(plan.waypoints.size() - 1);
                player.sendOverlayMessage(Component.literal("Waypoint taken back (" + plan.waypoints.size() + " left)"));
                preview(player, level, plan, null);
            }
            return true;
        }
        if (plan == null) {
            if (port == null) return false;   // (not a port: the kit lays a straight block)
            PLANS.put(player.getUUID(), new Plan(tier, port, level.dimension()));
            player.sendOverlayMessage(Component.literal("Pipe route started: click blocks for waypoints, a port to finish, sneak-click to undo"));
            return true;
        }
        if (port == null) {
            BlockPos at = pos.relative(face);
            plan.waypoints.add(new PipeRouter.End(at.getX() * 16L + 8, at.getY() * 16L + 8, at.getZ() * 16L + 8, -1));
            PipeRouter.Result r = route(level, plan, null);
            if (r.error != null) {
                plan.waypoints.remove(plan.waypoints.size() - 1);
                player.sendOverlayMessage(Component.literal("Can't reach that waypoint: " + r.error));
            } else {
                player.sendOverlayMessage(Component.literal(String.format("Waypoint %d: %.1f blocks so far", plan.waypoints.size(), r.length() / 16.0)));
                show(player, level, r.points);
            }
            return true;
        }
        // finish
        PipeRouter.Result r = route(level, plan, port);
        if (r.error != null) {
            player.sendOverlayMessage(Component.literal("No route: " + r.error));
            return true;
        }
        int len = r.length();
        boolean creative = player.isCreative();
        if (!creative && PipeKitItem.length(kit) < len) {
            player.sendOverlayMessage(Component.literal(String.format("This route takes %.1f blocks of pipe; the kit holds %.1f", len / 16.0, PipeKitItem.length(kit) / 16.0)));
            show(player, level, r.points);
            return true;
        }
        String err = lay(level, plan.tier, r.points, player);
        if (err != null) {
            player.sendOverlayMessage(Component.literal("Can't lay it: " + err));
            return true;
        }
        PLANS.remove(player.getUUID());
        if (!creative) {
            int left = PipeKitItem.length(kit) - len;
            if (left <= 0) kit.shrink(1);
            else kit.setDamageValue(PipeKitItem.MAX - left);
        }
        player.sendOverlayMessage(Component.literal(String.format("Laid %.1f blocks of pipe", len / 16.0)));
        return true;
    }

    static PipeRouter.Result route(Level level, Plan plan, PipeRouter.End end) {
        List<PipeRouter.End> wps = new ArrayList<>(plan.waypoints);
        PipeRouter.End last = end;
        if (last == null) {
            if (wps.isEmpty()) return new PipeRouter.Result(List.of(), null);
            last = wps.remove(wps.size() - 1);
        }
        return PipeRouter.route(space(level), PipePart.BORE[plan.tier], plan.start, wps, last);
    }

    /** Lay the route: one pipe part per block piece; all or nothing. */
    public static String lay(Level level, int tier, List<long[]> points, net.minecraft.world.entity.player.Player player) {
        var pieces = PipeRouter.perBlock(points);
        // check first
        for (var e : pieces.entrySet()) {
            BlockPos p = new BlockPos(e.getKey().get(0).intValue(), e.getKey().get(1).intValue(), e.getKey().get(2).intValue());
            BlockState s = level.getBlockState(p);
            VoxelNode existing = s.getBlock() instanceof VoxelNodeBlock && level.getBlockEntity(p) instanceof VoxelNodeEntity ve
                && ve.getNode() instanceof VoxelNode n ? n : null;
            if (existing == null && !(s.isAir() || s.canBeReplaced())) return "a block is in the way at " + p.toShortString();
            for (int[] pts : e.getValue()) {
                PipePart part = new PipePart(tier, pts);
                if (!part.valid()) return "bad piece at " + p.toShortString();
                if (existing != null && !existing.fits(part)) return "something is in the way at " + p.toShortString();
            }
        }
        for (var e : pieces.entrySet()) {
            BlockPos p = new BlockPos(e.getKey().get(0).intValue(), e.getKey().get(1).intValue(), e.getKey().get(2).intValue());
            for (int[] pts : e.getValue()) {
                String err = PipeKitItem.place(level, p, new PipePart(tier, pts), player);
                if (err != null) return err + " at " + p.toShortString();
            }
        }
        return null;
    }

    static void preview(ServerPlayer player, ServerLevel level, Plan plan, PipeRouter.End end) {
        PipeRouter.Result r = route(level, plan, end);
        if (r.error == null) show(player, level, r.points);
    }

    /** The route as particles, for this player only. */
    static void show(ServerPlayer player, ServerLevel level, List<long[]> pts) {
        var dust = new net.minecraft.core.particles.DustParticleOptions(0x55C8FF, 0.8f);
        for (int i = 1; i < pts.size(); i++) {
            long[] a = pts.get(i - 1), b = pts.get(i);
            int n = (int) Math.max(1, (Math.abs(b[0] - a[0]) + Math.abs(b[1] - a[1]) + Math.abs(b[2] - a[2])) / 4);
            for (int k = 0; k <= n; k++) {
                double t = k / (double) n;
                level.sendParticles(player, dust, true, false, (a[0] + (b[0] - a[0]) * t) / 16.0, (a[1] + (b[1] - a[1]) * t) / 16.0,
                    (a[2] + (b[2] - a[2]) * t) / 16.0, 1, 0, 0, 0, 0);
            }
        }
    }
}
