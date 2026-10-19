package mods.eln.node.six;

import mods.eln.node.NodeManager;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
