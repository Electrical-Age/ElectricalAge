package mods.eln.robotics.computer.screen;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The server's copy of a screen: what it keeps, sends and replays (Wire review, Baughn 2026-11-24). */
class ScreenHostTest {
    static void from(ScreenHost h, String... lines) {
        h.fromComputer((String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    /** A save's answer goes to the operator alone, and the scene's editor keeps the text as saved for the next one. */
    @Test
    void aSavedFileIsWhatTheNextOperatorOpens() {
        ScreenHost h = new ScreenHost();
        from(h, "(window 2 1 1 40 10 \"Ed\")", "(editor 2 4 0 1 :fill :fill :plain \"A:/f.txt\" \"r1\" \"one\\ntwo\\nthree\")");
        h.takeBatch();
        assertNotNull(h.check("(save 2 4 5 \"r1\" [[1 1 [\"TWO\" \"2b\"]]])", null));
        from(h, "(saved 2 4 5 \"r2\")");
        assertEquals(List.of("(saved 2 4 5 \"r2\")"), h.takeReplies(), "the answer, for the operator");
        assertTrue(h.takeBatch().isEmpty(), "not for everyone");
        var e = h.scene.windows.get(2).elements.get(4);
        assertEquals("r2", Form.str(e.msg, 9, ""));
        assertEquals("one\nTWO\n2b\nthree", Form.str(e.msg, 10, ""));
        String replayed = String.join("\n", h.replay());
        assertTrue(replayed.contains("\"r2\" \"one\\nTWO\\n2b\\nthree\""), replayed);
    }

    @Test
    void aRefusedSaveLeavesTheScenesText() {
        ScreenHost h = new ScreenHost();
        from(h, "(window 2 1 1 40 10 \"Ed\")", "(editor 2 4 0 1 :fill :fill :plain \"A:/f.txt\" \"r1\" \"one\")");
        h.check("(save 2 4 5 \"r1\" [[0 1 [\"ONE\"]]])", null);
        from(h, "(conflict 2 4 5 \"r9\" \"other\")");
        assertEquals(1, h.takeReplies().size());
        assertEquals("one", Form.str(h.scene.windows.get(2).elements.get(4).msg, 10, ""));
    }

    /** A full log was replayed as one message over the line limit: new viewers saw it empty. */
    @Test
    void aFullLogReplaysInPieces() {
        ScreenHost h = new ScreenHost();
        from(h, "(window 2 1 1 70 20 \"Log\")", "(log 2 1 0 0 :fill :fill)");
        String line = "x".repeat(70);
        for (int k = 0; k < 1100; k++) from(h, "(append 2 1 :white \"" + line + "\\n\")");
        ScreenHost again = new ScreenHost();
        again.load(h.replay());
        int lines = again.scene.windows.get(2).elements.get(1).lines.size();
        assertTrue(lines >= 999, "the log came back: " + lines + " lines");
        for (String l : h.replay()) assertTrue(l.length() < Form.MAX_LINE, "every replayed message parses");
    }

    /** A big batch (one picture, a big file) was a full replay for everyone; now it is sent as it is. */
    @Test
    void aBigBatchIsNotAReplay() {
        ScreenHost h = new ScreenHost();
        from(h, "(window 2 1 1 40 10 \"Ed\")");
        h.takeBatch();
        from(h, "(editor 2 4 0 1 :fill :fill :plain \"A:/f.txt\" \"r1\" \"" + "y".repeat(30_000) + "\")");
        List<String> b = h.takeBatch();
        assertNotNull(b);
        assertEquals(1, b.size());
    }
}
