package mods.eln.node;

import mods.eln.node.transparent.TransparentNode;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data safety (quality plan, phase 4): a saved node the running version cannot load (unknown type, unknown device,
 * or a load that throws) is kept by NodeManager as an orphan, the raw tag, and the next save of that dimension writes
 * it back out unchanged, so a later version that can load it again still finds the player's machine.
 */
public class NodeSaveLoadTest {
    NodeManager previous = NodeManager.instance;

    @AfterEach
    void restore() {
        NodeManager.instance = previous;
    }

    static CompoundTag savedNode(String type, CompoundTag body) {
        CompoundTag n = body.copy();
        n.putString("tag", type);
        CompoundTag pos = new CompoundTag();
        pos.putInt("cx", 10);
        pos.putInt("cy", 64);
        pos.putInt("cz", -3);
        pos.putInt("cd", 0);
        n.put("node", pos);
        return n;
    }

    static CompoundTag roundTrip(NodeManager nm, CompoundTag nodes, int dim) {
        nm.loadFromNbt(nodes, dim);
        CompoundTag saved = new CompoundTag();
        nm.saveToNbt(saved, dim);
        return saved;
    }

    @Test
    public void aNodeOfAnUnknownTypeIsKeptAndSavedAgain() {
        NodeManager nm = new NodeManager("test");
        CompoundTag body = new CompoundTag();
        body.putString("someState", "a machine from a newer (or older) version");
        CompoundTag node = savedNode("eln:not_a_node_type_of_this_version", body);
        CompoundTag nodes = new CompoundTag();
        nodes.put("n0", node.copy());

        CompoundTag saved = roundTrip(nm, nodes, 0);
        assertTrue(nm.getNodes().isEmpty(), "the unknown node is not loaded");
        assertEquals(1, saved.size(), "... but the next save still has it: " + saved);
        assertEquals(node, saved.getCompoundOrEmpty("n0"));

        // and again on the save after (load the saved file into a fresh manager)
        assertEquals(saved, roundTrip(new NodeManager("test"), saved, 0));
    }

    /**
     * TransparentNode.readFromNBT: a device id this version doesn't have (a removed or renamed device) throws rather
     * than leaving a node with no element; NodeManager keeps the tag as an orphan.
     */
    @Test
    public void aTransparentNodeOfAnUnknownDeviceIsKeptToo() {
        NodeManager.registerUuid("t", TransparentNode.class);
        CompoundTag body = new CompoundTag();
        body.putShort("eid", (short) 32000);   // no such device
        CompoundTag element = new CompoundTag();
        element.putDouble("energy", 12.5);
        body.put("element", element);

        TransparentNode t = new TransparentNode();
        assertThrows(Exception.class, () -> t.readFromNBT(savedNode("t", body)));
        assertNull(t.element, "no element to run, save or break");

        NodeManager nm = new NodeManager("test");
        CompoundTag nodes = new CompoundTag();
        nodes.put("n0", savedNode("t", body));
        CompoundTag saved = roundTrip(nm, nodes, 0);
        assertEquals(0, nm.getNodes().size());
        assertEquals(savedNode("t", body), saved.getCompoundOrEmpty("n0"));
    }

    @Test
    public void orphansBelongToTheirDimension() {
        NodeManager nm = new NodeManager("test");
        CompoundTag nodes = new CompoundTag();
        nodes.put("n0", savedNode("eln:unknown", new CompoundTag()));
        nm.loadFromNbt(nodes, 7);

        CompoundTag other = new CompoundTag();
        nm.saveToNbt(other, 0);
        assertTrue(other.isEmpty(), "another dimension's save doesn't get it: " + other);
        assertEquals(1, nm.getOrphans(7).size());

        nm.unload(7);
        assertTrue(nm.getOrphans(7).isEmpty(), "unloading the dimension forgets it (its file still has it)");
        CompoundTag after = new CompoundTag();
        nm.saveToNbt(after, 7);
        assertTrue(after.isEmpty());
    }

    /** A six-node whose load or save can be made to fail (a device with a bug, or a class missing at run time). */
    public static class FlakyNode extends mods.eln.node.six.SixNode {
        static final String TYPE = "test:flaky";
        static boolean failRead, failWrite, failInit;

        @Override
        public String getNodeUuid() {
            return TYPE;
        }

        @Override
        public void readFromNBT(CompoundTag nbt) {
            if (failRead) throw new NoClassDefFoundError("some/device/Class");
            super.readFromNBT(nbt);
        }

        @Override
        public void writeToNBT(CompoundTag nbt) {
            if (failWrite) throw new IllegalStateException("a device that cannot save");
            super.writeToNBT(nbt);
        }

        @Override
        public void initializeFromNBT() {
            if (failInit) throw new NoClassDefFoundError("some/device/Class");
        }

        @Override
        public void globalBoot() {
            if (failInit) throw new NoClassDefFoundError("some/device/Class");
        }
    }

    static CompoundTag flaky(int x) {
        CompoundTag body = new CompoundTag();
        for (int i = 0; i < 6; i++) body.putShort("EID" + i, (short) 0);
        CompoundTag n = savedNode(FlakyNode.TYPE, body);
        n.getCompoundOrEmpty("node").putInt("cx", x);
        return n;
    }

    /** A LinkageError while loading one node makes it an orphan; the rest of the file still loads. */
    @Test
    public void aLinkageErrorOrphansOnlyThatNode() {
        NodeManager.registerUuid(FlakyNode.TYPE, FlakyNode.class);
        FlakyNode.failRead = true;
        try {
            NodeManager nm = new NodeManager("test");
            CompoundTag nodes = new CompoundTag();
            nodes.put("n0", flaky(1));
            nodes.put("n1", savedNode("eln:unknown", new CompoundTag()));
            nm.loadFromNbt(nodes, 0);
            assertEquals(2, nm.getOrphans(0).size(), "both kept: " + nm.getOrphans(0));
        } finally {
            FlakyNode.failRead = false;
        }
    }

    /**
     * A node whose writeToNBT throws is saved with its last good data (here: what it was loaded from, then the last
     * save that worked). It used to be left out of the file, so the next load deleted it.
     */
    @Test
    public void aNodeThatFailsToSaveKeepsItsLastGoodData() {
        NodeManager.registerUuid(FlakyNode.TYPE, FlakyNode.class);
        try {
            NodeManager nm = new NodeManager("test");
            CompoundTag nodes = new CompoundTag();
            CompoundTag original = flaky(1);
            nodes.put("n0", original.copy());
            nm.loadFromNbt(nodes, 0);
            assertEquals(1, nm.getNodes().size());

            FlakyNode.failWrite = true;
            CompoundTag failed = new CompoundTag();
            nm.saveToNbt(failed, 0);
            assertEquals(1, failed.size(), "still in the file: " + failed);
            assertEquals(original, failed.getCompoundOrEmpty("n0"), "as it was loaded");

            FlakyNode.failWrite = false;
            CompoundTag good = new CompoundTag();
            nm.saveToNbt(good, 0);
            FlakyNode.failWrite = true;
            CompoundTag again = new CompoundTag();
            nm.saveToNbt(again, 0);
            assertEquals(good, again, "as last saved");
        } finally {
            FlakyNode.failWrite = false;
        }
    }

    /** dropOrphans forgets the orphans at one position only; a node added there replaces its orphan. */
    @Test
    public void orphansAreDroppedByPosition() {
        NodeManager nm = new NodeManager("test");
        CompoundTag nodes = new CompoundTag();
        nodes.put("n0", savedNode("eln:unknown", new CompoundTag()));
        CompoundTag other = savedNode("eln:unknown", new CompoundTag());
        other.getCompoundOrEmpty("node").putInt("cx", 11);
        nodes.put("n1", other);
        nm.loadFromNbt(nodes, 0);
        mods.eln.misc.Coordonate at = new mods.eln.misc.Coordonate();
        at.readFromNBT(savedNode("x", new CompoundTag()).getCompoundOrEmpty("node"), "c");

        assertTrue(nm.hasOrphanAt(at));
        assertEquals(1, nm.dropOrphans(at, "test"));
        assertTrue(!nm.hasOrphanAt(at));
        CompoundTag saved = new CompoundTag();
        nm.saveToNbt(saved, 0);
        assertEquals(1, saved.size(), "the other stays: " + saved);
        assertEquals(11, saved.getCompoundOrEmpty("n0").getCompoundOrEmpty("node").getIntOr("cx", 0));
    }

    /** A LinkageError while initializing or booting one node doesn't stop the load of the others. */
    @Test
    public void aLinkageErrorAtInitDoesNotStopTheLoad() {
        NodeManager.registerUuid(FlakyNode.TYPE, FlakyNode.class);
        FlakyNode.failInit = true;
        try {
            NodeManager nm = new NodeManager("test");
            CompoundTag nodes = new CompoundTag();
            nodes.put("n0", flaky(1));
            nodes.put("n1", flaky(2));
            nm.loadFromNbt(nodes, 0);
            assertEquals(2, nm.getNodes().size());
        } finally {
            FlakyNode.failInit = false;
        }
    }
}
