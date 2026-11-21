package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.JunctionPart;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelPart;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Pipe junctions (routing phase 2; Baughn 2026-11-11, LabAgent's review): a tee has its own fluid node linked to each
 * arm, so each branch is its own run; a sealed riser over it drains through it; it goes when fewer than two arms are
 * left, and gives its contents to the last.
 */
public final class JunctionCases {
    private JunctionCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new RiserDrains());
        cases.add(new GoesWithItsArms());
        cases.add(new EndJoinKeepsRunWater());
        cases.add(new PruneKeepsRunWater());
    }

    /** The water in the runs and junctions of the pipes in ns, as the fluid world holds them now (each run's lump once). */
    static double runWater(VoxelNode... ns) {
        java.util.Set<mods.eln.sim.fluid.FluidNode> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        double m = 0;
        for (VoxelNode n : ns)
            for (VoxelPart p : n.parts) {
                mods.eln.sim.fluid.FluidNode f = p instanceof PipePart pp ? pp.fluid : p instanceof JunctionPart j ? j.fluid : null;
                if (f != null && seen.add(f = mods.eln.fluid.FluidWorld.nodeFor(f))) m += f.mWater;
            }
        return m;
    }

    /** A sealed level pipe (x 2..14, 4 px up) in a block, a riser joined to its middle up into the block above (sealed). */
    static VoxelNode tee(TestContext c, BlockPos at) {
        PipeCases.lay(c, at, 0, 2, 4, 8, 14, 4, 8);
        VoxelNode n = c.voxelByEntity(at);
        String err = n.addJoined(new PipePart(0, new int[]{8, 4, 8, 8, 16, 8}), 8, 4, 8);
        if (err != null) throw new IllegalStateException("tee: " + err);
        PipeCases.lay(c, at.above(), 0, 8, 0, 8, 8, 12, 8);
        return n;
    }

    static double water(VoxelNode... ns) {
        double m = 0;
        for (VoxelNode n : ns)
            for (VoxelPart p : n.parts) {
                if (p instanceof PipePart pp && pp.fluid != null) m += pp.fluid.mWater;
                if (p instanceof JunctionPart j && j.fluid != null) m += j.fluid.mWater;
            }
        return m;
    }

    /**
     * The riser half full of water, the level arms and the junction full of air: the water runs down into them while
     * their air goes up the riser (the junction's links carry both ways, as between pipes' ends), and none is lost.
     */
    static final class RiserDrains implements TestCase {
        BlockPos at;
        double before, riserBefore;

        public String id() {
            return "pipe_junction_sealed_riser_drains";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext c) {
            at = c.at(1, 1, 1);
            VoxelNode n = tee(c, at);
            for (VoxelNode v : new VoxelNode[]{n, c.voxelByEntity(at.above())})
                for (VoxelPart p : v.parts)
                    if (p instanceof PipePart pp && pp.points[0] == pp.points[3] && pp.points[2] == pp.points[5])   // (the vertical ones)
                        pp.fluid.fill(0.6 * pp.fluid.volume * Water.RHO_L, Water.P_ATM, 293);
            for (VoxelNode v : new VoxelNode[]{n, c.voxelByEntity(at.above())}) mods.eln.fluid.FluidWorld.changed(v);
            before = water(n, c.voxelByEntity(at.above()));
            riserBefore = before;
        }

        public void measure(TestContext c) {
            VoxelNode n = c.voxelByEntity(at), up = c.voxelByEntity(at.above());
            double low = 0;
            for (VoxelPart p : n.parts) {
                if (p instanceof PipePart pp && pp.points[1] == pp.points[4]) low += pp.fluid.mWater;   // (the level arms)
                if (p instanceof JunctionPart j) low += j.fluid.mWater;
            }
            double after = water(n, up);
            c.line(String.format("junction riser: water %.5f kg before, %.5f after; %.5f in the level arms and junction", before, after, low));
            c.check("pipe junction: a sealed riser's water runs down into the level arms through it", low > 0.5 * riserBefore,
                String.format("%.5f of %.5f kg", low, riserBefore));
            c.check("pipe junction: no water made or lost", Math.abs(after - before) < 0.01 * before, String.format("%.5f -> %.5f kg", before, after));
        }
    }

    /**
     * Arms taken away one by one: with two left it stays (a node in a straight run), with one it goes and its water
     * goes into that arm; nothing made or lost.
     */
    static final class GoesWithItsArms implements TestCase {
        BlockPos at;
        boolean stayedAtTwo, goneAtOne;
        double before, after;
        String err;

        public String id() {
            return "pipe_junction_goes_with_its_arms";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext c) {
            at = c.at(1, 1, 1);
            VoxelNode n = tee(c, at);
            JunctionPart j = null;
            for (VoxelPart p : n.parts) if (p instanceof JunctionPart jp) j = jp;
            if (j == null) {
                err = "no junction";
                return;
            }
            j.fluid.fill(0.5 * j.fluid.volume * Water.RHO_L, Water.P_ATM, 293);
            before = water(n);
            // the riser first (the vertical piece here), then one level arm
            for (int i = 0; i < n.parts.size(); i++)
                if (n.parts.get(i) instanceof PipePart pp && pp.points[0] == pp.points[3] && pp.points[2] == pp.points[5]) {
                    before -= pp.fluid.mWater;
                    n.removePart(i, false);
                    break;
                }
            stayedAtTwo = n.parts.stream().anyMatch(q -> q instanceof JunctionPart);
            for (int i = 0; i < n.parts.size(); i++)
                if (n.parts.get(i) instanceof PipePart pp) {
                    before -= pp.fluid.mWater;
                    n.removePart(i, false);
                    break;
                }
            goneAtOne = n.parts.stream().noneMatch(q -> q instanceof JunctionPart);
            after = water(n);
        }

        public void measure(TestContext c) {
            c.check("pipe junction: built", err == null, "" + err);
            c.check("pipe junction: with two arms left it stays", stayedAtTwo, "");
            c.check("pipe junction: with one left it goes", goneAtOne, "");
            c.check("pipe junction: its water goes into the last arm", Math.abs(after - before) < 1e-9 + 1e-6 * Math.abs(before),
                String.format("%.6f expected, %.6f there", before, after));
        }
    }

    /**
     * A pipe joined at the end of a run whose lump holds water written since the last step (a fill, as a test or a device
     * makes): the junction takes its share of the run once, and a rebuild neither duplicates that share nor loses the
     * fill (FluidWorld.handBack: the rebuild writes runs' lumps back into their pieces, except a run handed back for
     * editing, as this one is).
     */
    static final class EndJoinKeepsRunWater implements TestCase {
        BlockPos at;
        double before, after, junction;
        boolean endJoin;
        String err;

        public String id() {
            return "pipe_junction_end_join_keeps_run_water";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            at = c.at(1, 1, 2);
            PipeCases.lay(c, at.north(), 0, 8, 8, 4, 8, 8, 16);   // (capped at z 4)
            PipeCases.lay(c, at, 0, 8, 8, 0, 8, 8, 8);           // (ends at the block's centre)
            VoxelNode n = c.voxelByEntity(at);
            PipePart end = null;
            for (VoxelPart p : n.parts) if (p instanceof PipePart pp) end = pp;
            var run = mods.eln.fluid.FluidWorld.nodeFor(end.fluid);
            run.fill(0.5 * run.volume * Water.RHO_L, Water.P_ATM, 293);
            before = run.mWater;
            err = n.addJoined(new PipePart(0, new int[]{8, 8, 8, 12, 8, 8}), 8, 8, 8);
            if (err != null) return;
            endJoin = n.parts.contains(end) && n.parts.stream().anyMatch(q -> q instanceof JunctionPart);
            for (VoxelPart p : n.parts) if (p instanceof JunctionPart j) junction = j.fluid.mWater;
            mods.eln.fluid.FluidWorld.changed(n);
            after = runWater(n, c.voxelByEntity(at.north()));
        }

        public void measure(TestContext c) {
            c.line(String.format("end join: %.6f kg in the run before, %.6f after the join and a rebuild (%.6f in the junction)", before, after, junction));
            c.check("pipe junction end join: joined", err == null, "" + err);
            c.check("pipe junction end join: at the run's end (the pipe kept whole, a junction made)", endJoin, "");
            c.check("pipe junction end join: the junction took a share", junction > 0, String.format("%.6f kg", junction));
            c.check("pipe junction end join: no water made or lost across a rebuild", Math.abs(after - before) < 1e-9 * before,
                String.format("%.9f -> %.9f kg", before, after));
        }
    }

    /**
     * A junction left with one arm whose run's lump holds water written since the last step: the junction's water goes
     * into that arm, and the arm's run keeps its fill as well (VoxelNode.pruneJunctions hands the run back first).
     */
    static final class PruneKeepsRunWater implements TestCase {
        BlockPos at;
        double before, after;
        boolean gone;
        String err;

        public String id() {
            return "pipe_junction_prune_keeps_run_water";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            at = c.at(1, 1, 1);
            VoxelNode n = tee(c, at);
            for (int i = 0; i < n.parts.size(); i++)
                if (n.parts.get(i) instanceof PipePart pp && pp.points[0] == pp.points[3] && pp.points[2] == pp.points[5]) {
                    n.removePart(i, false);   // (the riser: two level arms left)
                    break;
                }
            JunctionPart j = null;
            List<PipePart> arms = new java.util.ArrayList<>();
            for (VoxelPart p : n.parts) {
                if (p instanceof JunctionPart jp) j = jp;
                if (p instanceof PipePart pp) arms.add(pp);
            }
            if (j == null || arms.size() != 2) {
                err = "no junction with two arms";
                return;
            }
            j.fluid.fill(0.5 * j.fluid.volume * Water.RHO_L, Water.P_ATM, 293);
            var run = mods.eln.fluid.FluidWorld.nodeFor(arms.get(1).fluid);
            run.fill(0.5 * run.volume * Water.RHO_L, Water.P_ATM, 293);
            before = j.fluid.mWater + run.mWater + mods.eln.fluid.FluidWorld.nodeFor(arms.get(0).fluid).mWater;
            double removed = mods.eln.fluid.FluidWorld.nodeFor(arms.get(0).fluid).mWater;
            n.removePart(n.parts.indexOf(arms.get(0)), false);
            before -= removed;
            gone = n.parts.stream().noneMatch(q -> q instanceof JunctionPart);
            after = runWater(n);
        }

        public void measure(TestContext c) {
            c.line(String.format("prune: %.6f kg in the junction and the last arm's run before, %.6f in the run after", before, after));
            c.check("pipe junction prune: built", err == null, "" + err);
            c.check("pipe junction prune: with one arm left it goes", gone, "");
            c.check("pipe junction prune: the last arm's run keeps its fill and gains the junction's water", Math.abs(after - before) < 1e-9 * before,
                String.format("%.9f expected, %.9f there", before, after));
        }
    }
}
