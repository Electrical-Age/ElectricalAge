package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.voxel.CableLinks;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.CablePlacer;
import mods.eln.node.voxel.CableRouter;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelNodeEntity;
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
    }

    static VoxelNode node(TestContext c, BlockPos p) {
        return c.level().getBlockEntity(p) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode v ? v : null;
    }

    /** The joints of every cable piece in the block, as text, the implied ones marked. */
    static String joints(VoxelNode v) {
        if (v == null) return "no block";
        StringBuilder b = new StringBuilder();
        for (var p : v.parts)
            if (p instanceof CablePart c) {
                b.append('#').append(c.id).append('{');
                for (int[] j : c.joints) b.append(CableLinks.describe(j)).append("; ");
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
            VoxelNode mid = node(c, c.at(2, 1, 1));
            laidBefore = joints(mid);
            contsBefore = laidConts(mid, 1, 0, 0) + laidConts(mid, -1, 0, 0);
            // a relay in line in the middle block (DevicePlacer cuts the piece in two)
            if (mid != null) {
                var stack = VoxelDeviceCases.desc("Low Voltage Relay").newItemStack();
                cutErr = mid.placeAnywhere(stack, Direction.YN, 0.55, 0, 0.5, VoxelDeviceCases.player(c));
            }
            afterCut = joints(mid);
            contsAfterCut = laidConts(mid, 1, 0, 0) + laidConts(mid, -1, 0, 0);
            // break the last block's piece: the block before it keeps no joint to it
            VoxelNode last = node(c, c.at(4, 1, 1));
            if (last != null)
                for (int i = last.parts.size() - 1; i >= 0; i--)
                    if (last.parts.get(i) instanceof CablePart) {
                        if (last.removePart(i, false)) last.removeEmptyBlock();
                        break;
                    }
            VoxelNode before = node(c, c.at(3, 1, 1));
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
                VoxelNode v = node(c, c.at(x, 1, 1));
                if (v != null) scope.add(v);
            }
            var rep = mods.eln.node.voxel.CableVerify.verify(scope);
            c.check("links: the joints match the network after it all", rep.ok(), rep.summary() + " " + String.join("; ", rep.problems()));
        }
    }
}
