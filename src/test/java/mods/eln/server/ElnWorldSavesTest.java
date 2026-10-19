package mods.eln.server;

import mods.eln.node.NodeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A node file that can't be read is renamed aside (*.corrupt-*) instead of being left where the next save overwrites
 * it: if both the file and its backup were unreadable, the next save used to replace both, and the player's whole EA
 * world was gone.
 */
public class ElnWorldSavesTest {
    NodeManager previous = NodeManager.instance;

    @AfterEach
    void restore() {
        NodeManager.instance = previous;
    }

    static final byte[] GARBAGE = "not a gzipped NBT file".getBytes();

    static List<Path> corrupt(Path dir, String name) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().startsWith(name + ".corrupt-")).toList();
        }
    }

    @Test
    public void bothUnreadableAreKeptAside(@TempDir Path dir) throws IOException {
        Path main = dir.resolve("electricalAgeWorld0.dat"), bak = dir.resolve("electricalAgeWorld0.dat.bak");
        Files.write(main, GARBAGE);
        Files.write(bak, GARBAGE);
        new NodeManager("test");

        assertTrue(ElnWorldSaves.load(main, bak, 0), "files moved aside: saving is safe");
        assertFalse(Files.exists(main));
        assertFalse(Files.exists(bak));
        List<Path> m = corrupt(dir, "electricalAgeWorld0.dat"), b = corrupt(dir, "electricalAgeWorld0.dat.bak");
        assertEquals(1, b.size(), "the backup is kept aside");
        assertEquals(1, m.size(), "the file is kept aside");
        assertArrayEquals(GARBAGE, Files.readAllBytes(b.get(0)));
    }

    @Test
    public void anUnreadableFileIsSetAsideAndTheBackupLoadsAndStays(@TempDir Path dir) throws IOException {
        Path main = dir.resolve("electricalAgeWorld0.dat"), bak = dir.resolve("electricalAgeWorld0.dat.bak");
        Files.write(main, GARBAGE);
        CompoundTag good = new CompoundTag();
        good.put("nodes", new CompoundTag());
        NbtIo.writeCompressed(good, bak);
        byte[] bakBytes = Files.readAllBytes(bak);
        new NodeManager("test");

        assertTrue(ElnWorldSaves.load(main, bak, 0));
        assertFalse(Files.exists(main), "moved aside, so the next save doesn't rotate it over the good backup");
        assertArrayEquals(bakBytes, Files.readAllBytes(bak), "the backup is left as it was");
        assertArrayEquals(GARBAGE, Files.readAllBytes(corrupt(dir, "electricalAgeWorld0.dat").get(0)));
    }

    @Test
    public void aMissingFileIsNotCorruption(@TempDir Path dir) throws IOException {
        Path main = dir.resolve("electricalAgeWorld0.dat"), bak = dir.resolve("electricalAgeWorld0.dat.bak");
        CompoundTag good = new CompoundTag();
        good.put("nodes", new CompoundTag());
        NbtIo.writeCompressed(good, bak);
        new NodeManager("test");

        assertTrue(ElnWorldSaves.load(main, bak, 0));
        assertTrue(Files.exists(bak));
        assertTrue(corrupt(dir, "electricalAgeWorld0.dat").isEmpty());
    }
}
