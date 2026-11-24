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

    /** Coalescing kept a changed element at its last place: clients made 6 before 5, the reverse of the computer. */
    @Test
    void aBatchKeepsTheOrderElementsWereMadeIn() {
        ScreenHost h = new ScreenHost();
        from(h, "(window 2 1 1 40 10 \"W\")");
        h.takeBatch();
        from(h, "(text 2 5 0 0 :white \"a\")", "(text 2 6 0 1 :white \"b\")", "(text 2 5 0 0 :white \"c\")");
        assertEquals(List.of("(text 2 5 0 0 :white \"c\")", "(text 2 6 0 1 :white \"b\")"), h.takeBatch());
        from(h, "(text 2 7 0 0 :white \"a\")", "(remove 2 7)", "(text 2 7 0 0 :white \"b\")");
        assertEquals(3, h.takeBatch().size(), "never across a remove");
    }

    /** Clients are untrusted: what the screen doesn't show never reaches the program. */
    @Test
    void eventsMustNameWhatTheScreenShows() {
        ScreenHost h = new ScreenHost();
        from(h, "(window 2 1 1 40 10 \"W\")", "(menus 2 [[\"Project\" [\"Open\" \"o\"] nil [\"Quit\" \"q\"]]])",
            "(enable 2 \"Project\" \"Quit\" false)", "(button 2 3 0 0 8 \"Go\")", "(list 2 4 0 1 10 3 [\"a\" \"b\"])");
        assertNotNull(h.check("(pick 2 \"Project\" \"Open\")", null));
        assertNull(h.check("(pick 2 \"Project\" \"Quit\")", null), "greyed out");
        assertNull(h.check("(pick 2 \"Project\" \"Format A:\")", null), "not there");
        assertNotNull(h.check("(press 2 3)", null));
        assertNull(h.check("(press 2 4)", null), "a list isn't a button");
        assertNotNull(h.check("(select 2 4 1)", null));
        assertNull(h.check("(select 2 4 2)", null), "past the end");
        assertNull(h.check("(answer 2 9 0)", null), "no requester open");
        assertNull(h.check("(close 2)", null), "not an event clients send");
        from(h, "(ask 2 9 \"T\" \"B\" [\"OK\"])");
        assertNull(h.check("(press 2 3)", null), "under a requester");
        assertNotNull(h.check("(answer 2 9 0)", null));
        assertEquals("(moved 2 1 1 40 10)", h.check("(moved 2 1 1 9999 9999)", null).replaceAll("moved 2 \\d+ \\d+ \\d+ \\d+", "moved 2 1 1 40 10"));
    }

    /** A UTF-8 character split between two slices of the channel stays whole. */
    @Test
    void aCharacterSplitBetweenSlicesStaysWhole() {
        ScreenHost h = new ScreenHost();
        byte[] b = "(window 2 1 1 40 10 \"Café\")\n".getBytes(StandardCharsets.UTF_8);
        int cut = new String(b, StandardCharsets.UTF_8).indexOf('é') + 1;   // (inside the é's two bytes)
        h.fromComputer(java.util.Arrays.copyOfRange(b, 0, cut));
        h.fromComputer(java.util.Arrays.copyOfRange(b, cut, b.length));
        assertEquals("Café", h.scene.windows.get(2).title);
    }

    /** A log line that never ends wraps instead of growing for ever. */
    @Test
    void aLogLineHasALimit() {
        ScreenHost h = new ScreenHost();
        from(h, "(window 2 1 1 40 10 \"Log\")", "(log 2 1 0 0 :fill :fill)");
        for (int k = 0; k < 50; k++) from(h, "(append 2 1 :white \"" + "z".repeat(100) + "\")");
        var lines = h.scene.windows.get(2).elements.get(1).lines;
        assertEquals(5, lines.size());
        for (var l : lines) assertTrue(l.stream().mapToInt(r -> r.text().length()).sum() <= Scene.LINE_MAX);
    }

    /** The program hears the geometry the screen fitted its window to (it asked for more than the screen has). */
    @Test
    void theProgramHearsAFittedWindow() {
        ScreenHost h = new ScreenHost();
        from(h, "(window 2 1 2 300 10 \"Wide\")");
        List<String> told = h.takeToComputer();
        assertEquals(1, told.size(), String.valueOf(told));
        assertTrue(told.getFirst().startsWith("(moved 2 "), told.getFirst());
        from(h, "(window 3 1 2 20 5 \"Small\")");
        assertTrue(h.takeToComputer().isEmpty(), "one that fits: nothing to say");
        from(h, "(screen 40 20)");
        assertTrue(h.takeToComputer().stream().anyMatch(t -> t.startsWith("(moved 2 ")), "a smaller screen narrowed it");
    }
}
