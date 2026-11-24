package mods.eln.robotics.computer.screen;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/** A watcher's composed cells (Baughn 2026-11-24: watchers get the composed screen, not the scene). */
class CellsCodecTest {
    @Test
    void cellsComeBackAsComposedAndPicturesAreSentOnce() {
        ScreenHost h = new ScreenHost();
        String dots = "0".repeat(16) + "1".repeat(16);
        h.fromComputer(("(pens \"Amber\")\n(window 2 3 4 30 8 \"Hello\")\n(text 2 1 0 0 :lime \"Café ok\")\n"
            + "(pixmap 7 8 4 [:red \"#00ff00\"] \"" + dots + "\")\n(image 2 2 0 2 8 4 7)\n").getBytes(StandardCharsets.UTF_8));
        SceneCells c = SceneCells.of(h.scene);
        var sent = new HashSet<Integer>();
        byte[] first = CellsCodec.encode(c, sent), second = CellsCodec.encode(c, sent);
        assertTrue(second.length < first.length, "the picture went with the first only: " + first.length + " vs " + second.length);
        var known = new HashMap<Integer, Scene.Pixmap>();
        SceneCells a = CellsCodec.decode(first, known), b = CellsCodec.decode(second, known);
        assertNotNull(a);
        assertNotNull(b);
        assertArrayEquals(c.glyph, b.glyph);
        assertArrayEquals(c.ink, b.ink);
        assertArrayEquals(c.paper, b.paper);
        assertArrayEquals(c.owner, b.owner);
        assertEquals("Amber", b.pens.name());
        assertEquals(c.images.size(), b.images.size());
        assertArrayEquals(c.images.getFirst().pixmap().argb(), b.images.getFirst().pixmap().argb());
        assertTrue(first.length < 8000, "an 80 x 25 screen deflated: " + first.length + " bytes");
    }

    @Test
    void badDataIsNoCells() {
        assertNull(CellsCodec.decode(new byte[]{1, 2, 3}, new HashMap<>()));
    }

    /** The C parser on the Applications floppy is the one in rom/common (two copies: they mustn't drift). */
    @Test
    void theFloppysParserIsRomCommons() throws java.io.IOException {
        var root = java.nio.file.Path.of("").toAbsolutePath();   // (the tests may run in a directory under the repository)
        while (root != null && !java.nio.file.Files.isDirectory(root.resolve("rom/common"))) root = root.getParent();
        assertNotNull(root, "the repository's rom/common");
        for (String f : new String[]{"wire.h", "wire.c"})
            assertEquals(java.nio.file.Files.readString(root.resolve("rom/common").resolve(f)),
                java.nio.file.Files.readString(root.resolve("rom/disks/applications/System").resolve(f)), f);
    }
}
