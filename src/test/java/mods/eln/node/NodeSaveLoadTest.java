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
 * DOCUMENTS A DATA LOSS (quality plan, phase 2): a saved node the running version cannot load is dropped from
 * NodeManager, so the next save writes the world's node file without it and the player's machine is gone for good,
 * even if a later version could load it again. These tests assert today's behaviour; when the loader keeps the
 * raw NBT of such nodes, turn them around (the node's tag must come back out of saveToNbt unchanged).
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

    /** NodeManager.loadFromNbt: no class for the type -> NPE inside the try -> stack trace, node skipped. */
    @Test
    public void aNodeOfAnUnknownTypeIsDroppedAndNotSavedAgain() {
        NodeManager nm = new NodeManager("test");
        CompoundTag nodes = new CompoundTag();
        CompoundTag body = new CompoundTag();
        body.putString("someState", "a machine from a newer (or older) version");
        nodes.put("n0", savedNode("eln:not_a_node_type_of_this_version", body));

        nm.loadFromNbt(nodes);
        assertTrue(nm.getNodes().isEmpty(), "the unknown node is not loaded");

        CompoundTag saved = new CompoundTag();
        nm.saveToNbt(saved, Integer.MIN_VALUE);
        assertTrue(saved.isEmpty(), "... and the next save no longer has it: " + saved);
    }

    /**
     * TransparentNode.readFromNBT: a device id this version doesn't have (a removed or renamed device) gives no
     * descriptor; the NPE is not one of the exceptions it catches, so it escapes with element still null, and
     * NodeManager drops the node as above.
     */
    @Test
    public void aTransparentNodeOfAnUnknownDeviceIsDroppedToo() {
        NodeManager.registerUuid("t", TransparentNode.class);
        CompoundTag body = new CompoundTag();
        body.putShort("eid", (short) 32000);   // no such device
        body.put("element", new CompoundTag());

        TransparentNode t = new TransparentNode();
        assertThrows(NullPointerException.class, () -> t.readFromNBT(savedNode("t", body)));
        assertNull(t.element, "no element to run, save or break");

        NodeManager nm = new NodeManager("test");
        CompoundTag nodes = new CompoundTag();
        nodes.put("n0", savedNode("t", body));
        nm.loadFromNbt(nodes);
        assertEquals(0, nm.getNodes().size());
        CompoundTag saved = new CompoundTag();
        nm.saveToNbt(saved, Integer.MIN_VALUE);
        assertTrue(saved.isEmpty());
    }
}
