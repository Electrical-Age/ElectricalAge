package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.CableKitItem;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.CablePlacer;
import mods.eln.node.voxel.CableRouter;
import mods.eln.node.voxel.JunctionPart;
import mods.eln.node.voxel.PipeKitItem;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.PipePlacer;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Kit routes that start on (continue) or finish on (join) an old cable or pipe (todo #516, Baughn: "the pathfinder
 * regularly creates duplicate cable pieces"). An old run lies along x on the floor from block 1's middle to block 4's
 * middle; a route starts or finishes on it at one of its points, clicked as a player does, and goes to or comes from
 * the floor further on. Near the join the route may run inside the old run and is cut where it leaves it (JoinRun): no
 * piece over the old one (overlapping voxels no more than a plain tee's), no dead stub of pipe, and one network.
 * Routes that go along the old run past its end from further than JoinRun.REACH still run beside it (laid and connected
 * only: Baughn's call).
 */
public final class JoinRouteCases {
    private JoinRouteCases() {
    }

    public static void addTo(List<TestCase> cases) {
        for (boolean pipe : new boolean[]{false, true}) {
            // start just short of its end and go straight on (laid a piece over the old cable's end; a pipe hopped over a stub)
            cases.add(new Join(pipe, "near_end", 4, 5, true, 7, 3, true));
            // finish just short of its end, arriving along it from beyond
            cases.add(new Join(pipe, "join_near_end", 4, 5, false, 7, 3, true));
            // start at its end and go straight on
            cases.add(new Join(pipe, "end_straight", 4, 8, true, 7, 3, true));
            // finish on its middle, arriving from the side (the plain tee)
            cases.add(new Join(pipe, "join_side", 3, 8, false, 3, 7, true));
            // start near a block's face, the old run going on past it
            cases.add(new Join(pipe, "near_face", 3, pipe ? 12 : 14, true, 7, 3, false));
            // start on its middle and head along it past its end: still beside it (further than JoinRun.REACH)
            cases.add(new Join(pipe, "mid_along", 2, 8, true, 7, 3, false));
            // finish on its middle, arriving along it from beyond its end: likewise
            cases.add(new Join(pipe, "join_parallel", 3, 8, false, 7, 3, false));
        }
        // a Medium Voltage kit on the Low Voltage cable (another cable: it may join, but not run inside it)
        cases.add(new Join(false, "mv_near_end", 4, 5, true, 7, 3, false).kit(2));
        for (boolean pipe : new boolean[]{false, true}) {
            // another run of the kind 4 voxels beside the old one: the route goes round it, not through it
            cases.add(new Join(pipe, "beside_another", 2, 8, true, 2, 7, true).beside(12));
            // start and finish on the old run a few voxels apart: nothing to lay
            cases.add(new Join(pipe, "inside", 3, 4, true, 0, 0, true).inside(8));
            // the old run ends just past the block's face from the start
            cases.add(new Join(pipe, "end_past_face", 4, pipe ? 12 : 14, true, 7, 3, true).oldEnd(5, 2));
        }
        for (boolean pipe : new boolean[]{false, true}) {
            // start on the old run, a waypoint on one side of it, finish on the other: the way back crosses the old run at
            // the join, through the free cube round it; refused (JoinRun.crosses), never laid over it or through it
            cases.add(new Join(pipe, "back_across", 3, 8, true, 3, 6, true).via(3, 8, 0).orRefused());
        }
        // a waypoint on the floor under the old cable's last voxels, near the start: taken (nothing laid yet), not refused
        cases.add(new Join(false, "waypoint_on_run", 4, 4, true, 7, 3, true).via(4, 6, 3));
        // a pipe ending on a block's face, continued from just short of it: the ends meet across the face, no junction
        cases.add(new Join(true, "end_on_face", 4, 12, true, 7, 3, true).oldEnd(4, 16));
    }

    static final int LV = 1;
    static final int Z = 3;

    /** The old run along x at z = Z (block-local z lz), block 1's middle to local x endX of block endB, on the floor (y 1). */
    static void oldRun(TestContext c, boolean pipe, int lz, int endB, int endX) {
        if (pipe) {
            for (int b = 1; b <= endB; b++) PipeCases.lay(c, c.at(b, 1, Z), 0, b == 1 ? 8 : 0, 4, lz, b == endB ? endX : 16, 4, lz);
            return;
        }
        BlockPos a = c.at(1, 1, Z), b = c.at(endB, 1, Z);
        var s = new CableRouter.End(a.getX() * 16L + 8, a.getY() * 16L, a.getZ() * 16L + lz, 0, -1);
        var e = new CableRouter.End(b.getX() * 16L + endX, b.getY() * 16L, b.getZ() * 16L + lz, 0, -1);
        String err = CablePlacer.routeAndLayForTests(c.level(), LV, s, e);
        if (err != null) throw new IllegalStateException("old cable: " + err);
    }

    static final class Join implements TestCase {
        final boolean pipe, startOnRun, clean;
        final String what;
        final int runBlock, runX, floorX, floorZ;
        int kitTier = LV, endB = 4, endX = 8, besideZ = -1, insideX = -1;
        int[] via;
        boolean orRefused;
        boolean first, second;
        int partsBefore, partsAfter, overlap, deadEnds, junctionCount;
        boolean connected;
        String pieces = "";

        /** clean: the case JoinRun fixes (no overlap past a tee's, no stub); else only laid and connected. */
        Join(boolean pipe, String what, int runBlock, int runX, boolean startOnRun, int floorX, int floorZ, boolean clean) {
            this.pipe = pipe;
            this.what = what;
            this.runBlock = runBlock;
            this.runX = runX;
            this.startOnRun = startOnRun;
            this.floorX = floorX;
            this.floorZ = floorZ;
            this.clean = clean;
        }

        Join kit(int tier) {
            kitTier = tier;
            return this;
        }

        /** The old run ends at local x of block b. */
        Join oldEnd(int b, int x) {
            endB = b;
            endX = x;
            return this;
        }

        /** A second run of the kind beside the old one, blocks 1 to 3 at local z. */
        Join beside(int z) {
            besideZ = z;
            return this;
        }

        /** The route may be refused (then nothing may be laid); if laid, the usual checks. */
        Join orRefused() {
            orRefused = true;
            return this;
        }

        /** A waypoint on the floor first: at local x (voxels) of block bx, z the middle of block bz. */
        Join via(int bx, int x, int bz) {
            via = new int[]{bx, x, bz};
            return this;
        }

        /** The route finishes on the old run at local x of the start's block (nothing to lay). */
        Join inside(int x) {
            insideX = x;
            return this;
        }

        public String id() {
            return (pipe ? "pipe" : "cable") + "_route_" + what;
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }

        public int ticks() {
            return 10;
        }

        boolean click(TestContext c, net.minecraft.server.level.ServerPlayer p, ItemStack kit, BlockPos pos, Vec3 hit) {
            double x = p.getX(), y = p.getY(), z = p.getZ();
            p.snapTo(hit.x, hit.y + 0.5, hit.z, 0, 90);   // (over the click: it must be within reach)
            try {
                return pipe ? PipePlacer.use(p, c.level(), pos, Direction.UP, kit, hit) : CablePlacer.use(p, c.level(), pos, Direction.UP, kit, hit);
            } finally {
                p.snapTo(x, y, z, 0, 0);
            }
        }

        List<VoxelNode> nodes(TestContext c) {
            List<VoxelNode> out = new ArrayList<>();
            for (int x = 0; x < width(); x++)
                for (int z = 0; z < depth(); z++) {
                    VoxelNode n = c.voxelByEntity(c.at(x, 1, z));
                    if (n != null) out.add(n);
                }
            return out;
        }

        int runParts(TestContext c) {
            int k = 0;
            for (VoxelNode n : nodes(c)) for (VoxelPart q : n.parts) if (q instanceof CablePart || q instanceof PipePart) k++;
            return k;
        }

        public void build(TestContext c) {
            oldRun(c, pipe, 8, endB, endX);
            if (besideZ >= 0) oldRun(c, pipe, besideZ, 3, 8);
            partsBefore = runParts(c);
            var p = c.player();
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            PipePlacer.forget(p.getUUID());
            CablePlacer.cancelForTests(p.getUUID());
            ItemStack kit = pipe ? PipeKitItem.kit(0, 16 * 16) : CableKitItem.kit(kitTier, 16 * 16);
            BlockPos run = c.at(runBlock, 1, Z), floor = c.at(floorX, 0, floorZ);
            Vec3 onRun = Vec3.atLowerCornerOf(run).add(runX / 16.0, (pipe ? 5 : 1) / 16.0 + 0.001, 0.5);
            Vec3 onFloor = Vec3.atLowerCornerOf(floor).add(0.5, 1.0, 0.5);
            if (insideX >= 0) {
                first = click(c, p, kit, run, onRun);
                second = click(c, p, kit, run, Vec3.atLowerCornerOf(run).add(insideX / 16.0, (pipe ? 5 : 1) / 16.0 + 0.001, 0.5));
            } else if (startOnRun) {
                first = click(c, p, kit, run, onRun);
                if (via != null) {
                    BlockPos w = c.at(via[0], 0, via[2]);
                    first &= click(c, p, kit, w, Vec3.atLowerCornerOf(w).add(via[1] / 16.0, 1.0, 0.5));
                }
                // (a floor click is a waypoint; the same spot again ends the route there)
                second = click(c, p, kit, floor, onFloor) && click(c, p, kit, floor, onFloor);
            } else {
                first = click(c, p, kit, floor, onFloor);
                second = click(c, p, kit, run, onRun);
            }
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            PipePlacer.forget(p.getUUID());
            CablePlacer.cancelForTests(p.getUUID());
        }

        public void measure(TestContext c) {
            partsAfter = runParts(c);
            StringBuilder sb = new StringBuilder();
            List<VoxelNode> ns = nodes(c);
            int x0 = c.at(0, 0, 0).getX(), z0 = c.at(0, 0, 0).getZ();
            for (VoxelNode n : ns) {
                int bx = n.coordonate.x - x0;
                int[] count = new int[4096];
                sb.append(" [").append(bx).append(",").append(n.coordonate.z - z0).append("]");
                List<int[]> junctions = new ArrayList<>();
                for (VoxelPart q : n.parts) if (q instanceof JunctionPart j) junctions.add(new int[]{j.x, j.y, j.z});
                junctionCount += junctions.size();
                for (VoxelPart q : n.parts) {
                    if (q instanceof CablePart cp) sb.append(" c").append(java.util.Arrays.toString(cp.points));
                    else if (q instanceof PipePart pp) sb.append(" p").append(java.util.Arrays.toString(pp.points));
                    else sb.append(" ").append(q.getClass().getSimpleName());
                    if (!(q instanceof CablePart || q instanceof PipePart)) continue;
                    java.util.BitSet mine = new java.util.BitSet(4096);
                    for (int[] b : q.boxes())
                        for (int vx = b[0]; vx < b[3]; vx++)
                            for (int vy = b[1]; vy < b[4]; vy++)
                                for (int vz = b[2]; vz < b[5]; vz++) mine.set((vx * 16 + vy) * 16 + vz);
                    for (int i = mine.nextSetBit(0); i >= 0; i = mine.nextSetBit(i + 1)) if (++count[i] == 2) overlap++;
                    // a dead stub: a pipe from a junction to an end inside the block that meets nothing
                    if (q instanceof PipePart pp && bx != floorX) {
                        int at = 0, free = 0;
                        for (int e : new int[]{0, pp.points.length - 3}) {
                            int x = pp.points[e], y = pp.points[e + 1], z = pp.points[e + 2];
                            boolean atJ = false;
                            for (int[] j : junctions) atJ |= j[0] == x && j[1] == y && j[2] == z;
                            if (atJ) at++;
                            else if (x % 16 != 0 && y % 16 != 0 && z % 16 != 0) free++;
                        }
                        if (at == 1 && free == 1) deadEnds++;
                    }
                }
            }
            pieces = sb.toString();
            connected = pipe ? pipeConnected(c) : cableConnected(c, ns);
            String d = "overlap " + overlap + ", parts " + partsBefore + " -> " + partsAfter + ";" + pieces;
            c.check(id() + ": both clicks handled", first && second, first + " " + second);
            if (insideX >= 0) {
                c.check(id() + ": a route inside the old run lays nothing", partsAfter == partsBefore, d);
                return;
            }
            if (orRefused && partsAfter == partsBefore) {
                c.check(id() + ": refused, nothing laid over the old run", overlap == 0, d);
                return;
            }
            c.check(id() + ": a route was laid", partsAfter > partsBefore, d);
            c.check(id() + ": the old run and the new one are one network", connected, d);
            if (clean) {
                // (a tee: an LV cable's end on the old one's line, 2 voxels; a pipe arm's bore past the junction, 2x2x2)
                c.check(id() + ": no piece lies over the old run", overlap <= (pipe ? 8 : 2), d);
                if (pipe) c.check(id() + ": no dead stub of pipe", deadEnds == 0, deadEnds + " dead end(s);" + pieces);
                // (an old pipe ending on a face: the new one couples to it there as ports)
                if (pipe && endX == 16) c.check(id() + ": no junction where the ends meet across a face", junctionCount == 0, d);
            } else c.line("route beside or over the old run (not checked): " + d);
        }

        /** The old run's first block and the route's floor end in one fluid network. */
        boolean pipeConnected(TestContext c) {
            VoxelNode a = c.voxelByEntity(c.at(1, 1, Z)), b = c.voxelByEntity(c.at(floorX, 1, floorZ));
            if (a == null || b == null) return false;
            Object na = null, nb = null;
            // (the old run's piece, at z 8: not the other run beside it)
            for (VoxelPart q : a.parts) if (q instanceof PipePart pp && pp.fluid != null && pp.points[2] == 8) na = mods.eln.fluid.FluidWorld.networkOf(pp.fluid);
            for (VoxelPart q : b.parts) if (q instanceof PipePart pp && pp.fluid != null) nb = mods.eln.fluid.FluidWorld.networkOf(pp.fluid);
            return na != null && na == nb;
        }

        /** The old cable's first piece reaches the route's floor end through pieces that touch (in a block) or meet end to
         *  end (across a face). */
        boolean cableConnected(TestContext c, List<VoxelNode> ns) {
            List<long[]> base = new ArrayList<>();
            List<CablePart> all = new ArrayList<>();
            for (VoxelNode n : ns)
                for (VoxelPart q : n.parts)
                    if (q instanceof CablePart cp) {
                        all.add(cp);
                        base.add(new long[]{n.coordonate.x, n.coordonate.y, n.coordonate.z});
                    }
            int start = -1, goal = -1;
            for (int i = 0; i < all.size(); i++) {
                long[] b = base.get(i);
                if (b[0] == c.at(1, 1, Z).getX() && b[2] == c.at(1, 1, Z).getZ() && all.get(i).points[2] == 8) start = i;
                if (b[0] == c.at(floorX, 1, floorZ).getX() && b[2] == c.at(floorX, 1, floorZ).getZ()) goal = i;
            }
            if (start < 0 || goal < 0) return false;
            boolean[] seen = new boolean[all.size()];
            java.util.ArrayDeque<Integer> todo = new java.util.ArrayDeque<>(List.of(start));
            seen[start] = true;
            while (!todo.isEmpty()) {
                int i = todo.poll();
                if (i == goal) return true;
                for (int k = 0; k < all.size(); k++) {
                    if (seen[k]) continue;
                    boolean same = java.util.Arrays.equals(base.get(i), base.get(k));
                    boolean link = same ? all.get(i).touches(all.get(k)) : meet(all.get(i), base.get(i), all.get(k), base.get(k));
                    if (link) {
                        seen[k] = true;
                        todo.add(k);
                    }
                }
            }
            return false;
        }

        static boolean meet(CablePart a, long[] ba, CablePart b, long[] bb) {
            for (int[] e : a.ends())
                for (int[] f : b.ends())
                    if (ba[0] * 16 + e[0] == bb[0] * 16 + f[0] && ba[1] * 16 + e[1] == bb[1] * 16 + f[1] && ba[2] * 16 + e[2] == bb[2] * 16 + f[2]) return true;
            return false;
        }
    }
}
