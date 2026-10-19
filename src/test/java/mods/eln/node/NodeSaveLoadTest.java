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
}
