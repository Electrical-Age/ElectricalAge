package mods.eln.fluid;

import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A fluid-world rebuild (a device joins, leaves or changes anywhere) keeps what was put into a pipe run's lump since the
 * last step (batch-flake, 2026-11-16: a test's feed fill vanished when another test's device joined), and keeps a run's
 * pieces as edited after they were handed back for editing (a pipe cut for a junction).
 */
class FluidWorldRebuildTest {
    static final Object LEVEL = new Object();
    static final int WEST = 4, EAST = 5;

    /** A piece of pipe in a block: one lumpable node, with a port on each given face. */
    static final class Piece implements FluidDevice {
        final FluidNode node = new FluidNode(1e-3, 64, 1, false).fill(0, Water.P_ATM, 293.15);
        final List<FluidPort> ports;

        Piece(int x, int... faces) {
            node.lumpable = true;
            long pos = new BlockPos(x, 64, 0).asLong();
            FluidPort[] ps = new FluidPort[faces.length];
            for (int i = 0; i < faces.length; i++) ps[i] = new FluidPort(node, pos, faces[i], 0.5, 1e-6);
            ports = List.of(ps);
        }

        public Object fluidLevel() {
            return LEVEL;
        }

        public List<FluidPort> fluidPorts() {
            return ports;
        }
    }

    Piece a, b, far;

    @BeforeEach
    void run() {
        FluidWorld.reset();
        a = new Piece(0, EAST);
        b = new Piece(1, WEST);
        far = new Piece(10, EAST);
        FluidWorld.add(a);
        FluidWorld.add(b);
    }

    @AfterEach
    void done() {
        FluidWorld.reset();
    }

    static double[] contents(FluidNode n) {
        return new double[]{n.mWater, n.mAir, n.energy};
    }

    static void assertKept(double[] want, FluidNode n, String what) {
        double[] got = contents(n);
        String[] names = {"water", "air", "energy"};
        for (int i = 0; i < 3; i++) assertEquals(want[i], got[i], 1e-12 * Math.abs(want[i]) + 1e-15, what + ": " + names[i]);
    }

    /** Water written into the run's lump, then a device joins elsewhere: the rebuilt lump holds exactly the same. */
    @Test
    void aRebuildKeepsWhatWasWrittenIntoARunsLump() {
        FluidNode lump = FluidWorld.nodeFor(a.node);
        assertNotSame(a.node, lump, "the two pieces are one run");
        lump.fill(0.4 * lump.volume * Water.RHO_L, 3 * Water.P_ATM, 320);
        double[] want = contents(lump);
        FluidWorld.add(far);   // (a device joining anywhere rebuilds every network)
        FluidNode rebuilt = FluidWorld.nodeFor(a.node);
        assertNotSame(lump, rebuilt, "rebuilt");
        assertKept(want, rebuilt, "the run across a device joining");
        FluidWorld.changed(b);
        assertKept(want, FluidWorld.nodeFor(b.node), "the run across a device changing");
    }

    /** A run written back for a save (writeBackOf), then written into: a rebuild keeps the write. */
    @Test
    void aRebuildKeepsAWriteAfterASaveSync() {
        FluidNode lump = FluidWorld.nodeFor(a.node);
        FluidWorld.writeBackOf(a.node);
        lump.fill(0.2 * lump.volume * Water.RHO_L, 2 * Water.P_ATM, 300);
        double[] want = contents(lump);
        FluidWorld.changed(far);
        assertKept(want, FluidWorld.nodeFor(a.node), "the run after a save sync and a write");
    }

    /**
     * A run handed back for editing (handBack, as VoxelNode.addJoined does before a pipe gives a new junction its share):
     * a rebuild keeps the pieces as edited, with neither the share given away duplicated nor the edit reverted.
     */
    @Test
    void aRebuildKeepsPiecesEditedAfterAHandBack() {
        FluidNode lump = FluidWorld.nodeFor(a.node);
        lump.fill(0.4 * lump.volume * Water.RHO_L, 3 * Water.P_ATM, 320);
        double total = lump.mWater;
        FluidWorld.handBack(a.node);
        assertEquals(total / 2, a.node.mWater, 1e-12 * total, "a's share after the hand-back");
        FluidNode taken = new FluidNode(1e-3, 64, 1, false);
        taken.setShareOf(a.node, 0.25);   // (a quarter of a's contents go elsewhere, as to a new junction)
        a.node.setShareOf(a.node, 0.75);
        FluidWorld.changed(far);
        assertEquals(total - taken.mWater, FluidWorld.nodeFor(a.node).mWater, 1e-12 * total, "the run's water: what's left, once");
        assertEquals(total * 7 / 8, FluidWorld.nodeFor(a.node).mWater, 1e-12 * total, "the run's water");
    }

    /**
     * After a hand-back and an edit, a save of another piece of the same run (writeBackOf, as a device's stub on the run
     * does) leaves the pieces as edited: the run's lump is stale until the rebuild.
     */
    @Test
    void aSaveAfterAHandBackKeepsTheEdit() {
        FluidNode lump = FluidWorld.nodeFor(a.node);
        lump.fill(0.4 * lump.volume * Water.RHO_L, 3 * Water.P_ATM, 320);
        double total = lump.mWater;
        FluidWorld.handBack(a.node);
        a.node.setShareOf(a.node, 0.75);
        FluidWorld.writeBackOf(b.node);
        FluidWorld.handBack(b.node);   // (a second edit in the same tick: the pieces are already the authority)
        assertEquals(total * 3 / 8, a.node.mWater, 1e-12 * total, "a as edited");
        FluidWorld.changed(far);
        assertEquals(total * 7 / 8, FluidWorld.nodeFor(a.node).mWater, 1e-12 * total, "the run's water");
    }

    /** The topology plan is pure: planning leaves the built lumps, networks and contents as they were, and no rebuild pending. */
    @Test
    void aPlanTouchesNothing() {
        FluidNode lump = FluidWorld.nodeFor(a.node);
        var net = FluidWorld.networkOf(a.node);
        lump.fill(0.4 * lump.volume * Water.RHO_L, 3 * Water.P_ATM, 320);
        double[] want = contents(lump), wantA = contents(a.node);
        FluidWorld.Plan plan = FluidWorld.plan(List.of(far, b, a));
        assertEquals(1, plan.couplings.size(), "a and b couple");
        assertEquals(2, plan.runs.size(), "two runs: a with b, and far");
        assertEquals(2, plan.networkCount, "two networks");
        assertEquals(false, FluidWorld.dirty(), "no rebuild pending");
        assertSame(lump, FluidWorld.nodeFor(a.node), "the same lump");
        assertSame(net, FluidWorld.networkOf(a.node), "the same network");
        assertKept(want, lump, "the lump");
        assertKept(wantA, a.node, "a piece");
    }

    /** The plan's canonical dump doesn't depend on the order the devices come in, and changes with the topology. */
    @Test
    void aPlansDumpIsCanonical() {
        java.util.function.LongPredicate all = p -> true;
        var one = FluidWorld.plan(List.of(a, b, far)).dump(all);
        assertEquals(one, FluidWorld.plan(List.of(far, b, a)).dump(all), "reordered");
        assertEquals(one, FluidWorld.plan(List.of(b, far, a)).dump(all), "reordered again");
        var apart = FluidWorld.plan(List.of(a, new Piece(1, EAST), far)).dump(all);
        assertNotEquals(one, apart, "b turned away from a: another topology");
    }
}
