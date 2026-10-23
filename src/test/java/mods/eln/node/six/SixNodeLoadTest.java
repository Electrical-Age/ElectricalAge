package mods.eln.node.six;

import mods.eln.node.NodeManager;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Loading a six-node block from its saved tag. */
public class SixNodeLoadTest {
    NodeManager previous = NodeManager.instance;

    @BeforeEach
    void manager() {
        new NodeManager("test");
    }

    @AfterEach
    void restore() {
        NodeManager.instance = previous;
    }

    static CompoundTag saved() {
        CompoundTag n = new CompoundTag();
        n.putString("tag", "s");
        CompoundTag pos = new CompoundTag();
        pos.putInt("cx", 10);
        pos.putInt("cy", 64);
        pos.putInt("cz", -3);
        pos.putInt("cd", 0);
        n.put("node", pos);
        for (int i = 0; i < 6; i++) n.putShort("EID" + i, (short) 0);
        return n;
    }

    /**
     * readFromNBT only reads: the loader connects the node once it is in NodeManager (initializeFromNBT). It used to
     * connect at its end as well, before the node was added, so every six-node connected twice on load.
     */
    @Test
    public void readingDoesNotConnect() {
        SixNode s = new SixNode();
        s.readFromNBT(saved());
        assertFalse(s.isAdded(), "not in the simulation until the loader connects it");
    }

    /**
     * A side whose device can't be built (here: no such device id) keeps its saved data, which the next save writes
     * back unchanged; the side stays taken, and the block stays while it holds one. It used to vanish from the save.
     */
    @Test
    public void aSideThatCannotLoadIsKeptAndTheSideStaysTaken() {
        CompoundTag tag = saved();
        tag.putShort("EID2", (short) 32000);   // no such device
        CompoundTag ed = new CompoundTag();
        ed.putDouble("someState", 4.5);
        ed.putByte("sixFront", (byte) 1);
        tag.put("ED2", ed);

        SixNode s = new SixNode();
        s.readFromNBT(tag.copy());
        assertNull(s.sideElementList[2], "nothing runs there");
        assertTrue(s.isSideKept(2));
        assertTrue(s.getSideEnable(mods.eln.misc.Direction.fromInt(2)), "the side is taken");
        assertTrue(s.getIfSideRemain(), "the block stays");
        assertFalse(s.createSubBlock(net.minecraft.world.item.ItemStack.EMPTY, mods.eln.misc.Direction.fromInt(2), null),
            "nothing can be placed over it");

        CompoundTag out = new CompoundTag();
        s.writeToNBT(out);
        assertEquals(32000, out.getShortOr("EID2", (short) 0));
        assertEquals(ed, out.getCompoundOrEmpty("ED2"));

        // and again after another load
        SixNode again = new SixNode();
        again.readFromNBT(out.copy());
        CompoundTag out2 = new CompoundTag();
        again.writeToNBT(out2);
        assertEquals(out, out2);
    }

    /** Client packets naming a side out of range, or an empty side, are ignored (the side byte used to NPE). */
    @Test
    public void malformedClientPacketsAreIgnored() {
        SixNode s = new SixNode();
        s.readFromNBT(saved());
        for (byte[] packet : new byte[][]{{9, 0, 0}, {-1, 0, 5}, {0, 0, 0}, {3, 0, 1}, {2}})
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> s.networkUnserialize(new java.io.DataInputStream(new java.io.ByteArrayInputStream(packet)), null));
    }
}
