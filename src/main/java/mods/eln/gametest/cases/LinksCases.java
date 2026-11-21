package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.voxel.CableLinks;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.CablePlacer;
import mods.eln.node.voxel.CableRouter;
import mods.eln.node.voxel.VoxelNode;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Stored cable links (routing phase 3a): what the editors write. A laid run carries the layer's own joints between its
 * pieces; a device put in line on it re-points them to the cut's pieces; a piece broken leaves no joint pointing at it.
 * (The harness's links check compares every test's joints with its live network.)
 */
public final class LinksCases {
    private LinksCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new LaidCutBroken());
        cases.add(new TrimBurnRepair());
        cases.add(new FrozenBoundary());
    }

    /** The joints of every cable piece in the block, as text, the implied ones marked. */
    static String joints(VoxelNode v) {
        if (v == null) return "no block";
        StringBuilder b = new StringBuilder();
        for (var p : v.parts)
            if (p instanceof CablePart c) {
                b.append('#').append(c.id).append('{');
                List<int[]> sorted = new ArrayList<>(c.joints);
                sorted.sort(java.util.Arrays::compare);
                for (int[] j : sorted) b.append(CableLinks.describe(j)).append("; ");
                b.append("} ");
            }
        return b.toString();
    }

    /** The layer's (not implied) CONT joints of the block's pieces toward the block at (dx, dy, dz). */
    static int laidConts(VoxelNode v, int dx, int dy, int dz) {
        int n = 0;
        if (v == null) return 0;
        for (var p : v.parts)
            if (p instanceof CablePart c)
                for (int[] j : c.joints)
                    if (!CableLinks.implied(j) && CableLinks.kind(j) == CableLinks.CONT && j[2] == dx && j[3] == dy && j[4] == dz) n++;
        return n;
    }

    /** No joint in these blocks points at a piece that isn't there (CONT, SPLICE, TOUCH targets). */
    static String dangling(TestContext c, List<BlockPos> at) {
        StringBuilder b = new StringBuilder();
        for (BlockPos p : at) {
            VoxelNode v = c.voxelByEntity(p);
            if (v == null) continue;
            for (var part : v.parts)
                if (part instanceof CablePart cp)
                    for (int[] j : cp.joints) {
                        int k = CableLinks.kind(j);
                        if (k != CableLinks.CONT && k != CableLinks.SPLICE && k != CableLinks.TOUCH) continue;
                        VoxelNode t = c.voxelByEntity(p.offset(j[2], j[3], j[4]));
                        if (t == null || t.partById(j[5]) == null) b.append(p.toShortString()).append(" #").append(cp.id).append(' ').append(CableLinks.describe(j)).append("; ");
                    }
        }
        return b.toString();
    }

    /** Every joint of the cable pieces in these blocks, as text (to compare states). */
    static String allJoints(TestContext c, List<BlockPos> at) {
        StringBuilder b = new StringBuilder();
        for (BlockPos p : at) b.append(p.toShortString()).append(':').append(joints(c.voxelByEntity(p))).append(' ');
        return b.toString();
    }

    /**
     * A block of a laid run frozen out of the simulation (Stasis: NodeBase.simulationVeto), its neighbours reconnected,
     * then thawed: the neighbours keep their joints toward it while it is out, the check counts it as out of the
     * simulation rather than a problem, and after the thaw everything is as before.
     */
    static final class FrozenBoundary implements TestCase {
        String layErr, before = "", frozenState = "", thawed = "", frozenReport = "", thawedReport = "";
        boolean frozenOk, thawedOk, outCounted;

        public String id() {
            return "links_frozen_boundary";
        }

        public int width() {
            return 6;
        }

        public void build(TestContext c) {
            BlockPos o = c.at(0, 0, 0);
            int tier = CablePart.tierOf("Low Voltage Cable");
            var a = new CableRouter.End(o.getX() * 16L + 4, (o.getY() + 1) * 16L, (o.getZ() + 1) * 16L + 8, 0, -1);
            var b = new CableRouter.End((o.getX() + 4) * 16L + 12, (o.getY() + 1) * 16L, (o.getZ() + 1) * 16L + 8, 0, -1);
            layErr = CablePlacer.routeAndLayForTests(c.level(), tier, a, b);
            List<BlockPos> run = new ArrayList<>();
            for (int x = 0; x < 5; x++) run.add(c.at(x, 1, 1));
            VoxelNode mid = c.voxelByEntity(c.at(2, 1, 1)), left = c.voxelByEntity(c.at(1, 1, 1)), right = c.voxelByEntity(c.at(3, 1, 1));
            if (mid == null || left == null || right == null) return;
            // let every block connect first (new blocks connect when their block entity loads)
            for (BlockPos p : run) {
                VoxelNode v = c.voxelByEntity(p);
                if (v != null && !v.isAdded()) v.connect();
            }
            before = allJoints(c, run);
            var was = mods.eln.node.NodeBase.simulationVeto;
            try {
                mods.eln.node.NodeBase.simulationVeto = n -> n == mid;
                mid.reconnect();
                left.reconnect();
                right.reconnect();
                frozenState = allJoints(c, run);
                List<VoxelNode> scope = new ArrayList<>();
                for (BlockPos p : run) if (c.voxelByEntity(p) != null) scope.add(c.voxelByEntity(p));
                var rep = mods.eln.node.voxel.CableVerify.verify(scope);
                frozenOk = rep.ok() && !mid.isAdded();
                outCounted = rep.excludedBlocks() == 1 && rep.unjoinedToAbsent() == 0;
                frozenReport = rep.summary() + " " + String.join("; ", rep.problems());
            } finally {
                mods.eln.node.NodeBase.simulationVeto = was;
            }
            mid.connect();
            left.reconnect();
            right.reconnect();
            thawed = allJoints(c, run);
            List<VoxelNode> scope = new ArrayList<>();
            for (BlockPos p : run) if (c.voxelByEntity(p) != null) scope.add(c.voxelByEntity(p));
            var rep = mods.eln.node.voxel.CableVerify.verify(scope);
            thawedOk = rep.ok() && mid.isAdded();
            thawedReport = rep.summary() + " " + String.join("; ", rep.problems());
        }

        public void measure(TestContext c) {
            c.check("frozen: the run was laid", layErr == null, String.valueOf(layErr));
            c.check("frozen: with the middle block out, the joints match and nothing is a problem", frozenOk, frozenReport);
            c.check("frozen: the frozen block is counted as out, no end meets it unjoined", outCounted, frozenReport);
            c.check("frozen: the neighbours keep their joints toward it", frozenState.equals(before), "before " + before + " | frozen " + frozenState);
            c.check("frozen: thawed, the joints match again", thawedOk, thawedReport);
            c.check("frozen: thawed, every joint is as before", thawed.equals(before), "before " + before + " | thawed " + thawed);
        }
    }

    /**
     * A device put at a laid run's free end trims the last piece (DevicePlacer.trim): the joint from the block before it
     * follows the trimmed piece. A piece of the run burnt and repaired keeps the layer's joints, both ways.
     */
    static final class TrimBurnRepair implements TestCase {
        String layErr, trimErr, danglingAfterTrim = "", trimmedJoints = "", burnt = "", repaired = "";
        int contsToTrimmed = -1, laidBeforeBurn = -1, laidWhileBurnt = -1, laidAfterRepair = -1;

        public String id() {
            return "links_trim_burn_repair";
        }

        public int width() {
            return 6;
        }

        public void build(TestContext c) {
            BlockPos o = c.at(0, 0, 0);
            int tier = CablePart.tierOf("Low Voltage Cable");
            // a run on the floor from block x 0 to the middle of block x 3 (its end in the block, free)
            var a = new CableRouter.End(o.getX() * 16L + 4, (o.getY() + 1) * 16L, (o.getZ() + 1) * 16L + 8, 0, -1);
            var b = new CableRouter.End((o.getX() + 3) * 16L + 8, (o.getY() + 1) * 16L, (o.getZ() + 1) * 16L + 8, 0, -1);
            layErr = CablePlacer.routeAndLayForTests(c.level(), tier, a, b);
            VoxelNode end = c.voxelByEntity(c.at(3, 1, 1));
            if (end != null) {
                var stack = VoxelDeviceCases.desc("Voltage Probe").newItemStack();
                trimErr = end.placeAnywhere(stack, Direction.YN, 9.5 / 16, 0.5 / 16, 8.0 / 16, c.player());
            }
            List<BlockPos> run = new ArrayList<>();
            for (int x = 0; x < 4; x++) run.add(c.at(x, 1, 1));
            danglingAfterTrim = dangling(c, run);
            VoxelNode before = c.voxelByEntity(c.at(2, 1, 1));
            contsToTrimmed = laidConts(before, 1, 0, 0);
            trimmedJoints = joints(end);
            // burn the middle block's piece, then repair it
            VoxelNode mid = c.voxelByEntity(c.at(1, 1, 1));
            laidBeforeBurn = laidConts(mid, 1, 0, 0) + laidConts(mid, -1, 0, 0);
            CablePart piece = null;
            if (mid != null) for (var part : mid.parts) if (part instanceof CablePart cp) piece = cp;
            if (piece != null && mid.burnableOf(piece) != null) {
                mid.burnableOf(piece).burn(mods.eln.sim.process.destruct.FailureCause.OVERHEAT, mods.eln.sim.process.destruct.Failure.Kind.CABLE);
                laidWhileBurnt = laidConts(mid, 1, 0, 0) + laidConts(mid, -1, 0, 0)
                    + laidConts(c.voxelByEntity(c.at(0, 1, 1)), 1, 0, 0) + laidConts(c.voxelByEntity(c.at(2, 1, 1)), -1, 0, 0);
                burnt = joints(mid);
                if (mid.burnableOf(piece) != null) mid.burnableOf(piece).repair();
                laidAfterRepair = laidConts(mid, 1, 0, 0) + laidConts(mid, -1, 0, 0)
                    + laidConts(c.voxelByEntity(c.at(0, 1, 1)), 1, 0, 0) + laidConts(c.voxelByEntity(c.at(2, 1, 1)), -1, 0, 0);
                repaired = joints(mid);
            }
        }

        public void measure(TestContext c) {
            c.check("trim: the run was laid", layErr == null, String.valueOf(layErr));
            c.check("trim: a Voltage Probe went on its free end", trimErr == null, String.valueOf(trimErr));
            c.check("trim: no joint points at the replaced piece", danglingAfterTrim.isEmpty(), danglingAfterTrim);
            c.check("trim: the block before keeps the layer's joint to the trimmed piece", contsToTrimmed == 1, contsToTrimmed + "; end block: " + trimmedJoints);
            c.check("burn: the middle piece held the layer's joints both ways", laidBeforeBurn == 2, String.valueOf(laidBeforeBurn));
            c.check("burn: burnt, it and its neighbours keep them", laidWhileBurnt == 4, laidWhileBurnt + ": " + burnt);
            c.check("burn: repaired, still", laidAfterRepair == 4, laidAfterRepair + ": " + repaired);
        }
    }

    static final class LaidCutBroken implements TestCase {
        String layErr, laidBefore = "", cutErr, afterCut = "", afterBreak = "";
        int contsBefore, contsAfterCut, leftToBroken = -1;

        public String id() {
            return "links_laid_cut_broken";
        }

        public int width() {
            return 6;
        }

        public void build(TestContext c) {
            BlockPos o = c.at(0, 0, 0);
            int tier = CablePart.tierOf("Low Voltage Cable");
            // a straight run on the floor through blocks x 0..4 at z 1
            var a = new CableRouter.End(o.getX() * 16L + 4, (o.getY() + 1) * 16L, (o.getZ() + 1) * 16L + 8, 0, -1);
            var b = new CableRouter.End((o.getX() + 4) * 16L + 12, (o.getY() + 1) * 16L, (o.getZ() + 1) * 16L + 8, 0, -1);
            layErr = CablePlacer.routeAndLayForTests(c.level(), tier, a, b);
            VoxelNode mid = c.voxelByEntity(c.at(2, 1, 1));
            laidBefore = joints(mid);
            contsBefore = laidConts(mid, 1, 0, 0) + laidConts(mid, -1, 0, 0);
            // a relay in line in the middle block (DevicePlacer cuts the piece in two)
            if (mid != null) {
                var stack = VoxelDeviceCases.desc("Low Voltage Relay").newItemStack();
                cutErr = mid.placeAnywhere(stack, Direction.YN, 0.55, 0, 0.5, c.player());
            }
            afterCut = joints(mid);
            contsAfterCut = laidConts(mid, 1, 0, 0) + laidConts(mid, -1, 0, 0);
            // break the last block's piece: the block before it keeps no joint to it
            VoxelNode last = c.voxelByEntity(c.at(4, 1, 1));
            if (last != null)
                for (int i = last.parts.size() - 1; i >= 0; i--)
                    if (last.parts.get(i) instanceof CablePart) {
                        if (last.removePart(i, false)) last.removeEmptyBlock();
                        break;
                    }
            VoxelNode before = c.voxelByEntity(c.at(3, 1, 1));
            leftToBroken = laidConts(before, 1, 0, 0);
            afterBreak = joints(before);
        }

        public void measure(TestContext c) {
            c.check("links: the run was laid", layErr == null, String.valueOf(layErr));
            c.check("links: the middle block's piece holds the layer's joints to both neighbours", contsBefore == 2, contsBefore + ": " + laidBefore);
            c.check("links: a relay went in line on it", cutErr == null, String.valueOf(cutErr));
            c.check("links: the cut's pieces took over the joints to both neighbours", contsAfterCut == 2, contsAfterCut + ": " + afterCut);
            c.check("links: no joint points at a broken piece", leftToBroken == 0, leftToBroken + ": " + afterBreak);
            List<VoxelNode> scope = new ArrayList<>();
            for (int x = 0; x < 5; x++) {
                VoxelNode v = c.voxelByEntity(c.at(x, 1, 1));
                if (v != null) scope.add(v);
            }
            var rep = mods.eln.node.voxel.CableVerify.verify(scope);
            c.check("links: the joints match the network after it all", rep.ok(), rep.summary() + " " + String.join("; ", rep.problems()));
        }
    }
}
