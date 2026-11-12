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
    }

    /** A sealed level pipe (x 2..14, 4 px up) in a block, a riser joined to its middle up into the block above (sealed). */
    static VoxelNode tee(TestContext c, BlockPos at) {
        PipeCases.lay(c, at, 0, 2, 4, 8, 14, 4, 8);
        VoxelNode n = PipeCases.node(c, at);
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
            for (VoxelNode v : new VoxelNode[]{n, PipeCases.node(c, at.above())})
                for (VoxelPart p : v.parts)
                    if (p instanceof PipePart pp && pp.points[0] == pp.points[3] && pp.points[2] == pp.points[5])   // (the vertical ones)
                        pp.fluid.fill(0.6 * pp.fluid.volume * Water.RHO_L, Water.P_ATM, 293);
            for (VoxelNode v : new VoxelNode[]{n, PipeCases.node(c, at.above())}) mods.eln.fluid.FluidWorld.changed(v);
            before = water(n, PipeCases.node(c, at.above()));
            riserBefore = before;
        }

        public void measure(TestContext c) {
            VoxelNode n = PipeCases.node(c, at), up = PipeCases.node(c, at.above());
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
}
