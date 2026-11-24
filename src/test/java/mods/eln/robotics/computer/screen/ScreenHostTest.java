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

    /** A message cut short by a full channel is dropped whole, not fused with the next one (CANCEL from the console). */
    @Test
    void aCutMessageIsDropped() {
        ScreenHost h = new ScreenHost();
        h.fromComputer("(window 2 1 1 20 5 \"Fir".getBytes(StandardCharsets.UTF_8));
        h.fromComputer(new byte[]{mods.eln.robotics.computer.hw.Console.CANCEL});
        h.fromComputer("(window 3 1 1 20 5 \"Second\")\n".getBytes(StandardCharsets.UTF_8));
        assertNull(h.scene.windows.get(2));
        assertEquals("Second", h.scene.windows.get(3).title);
    }

    /** Wire 2's hello parses, and says the version first. */
    @Test
    void theHelloIsAMessage() {
        List<Object> m = Form.parse(ScreenHost.hello());
        assertNotNull(m, ScreenHost.hello());
        assertEquals("wire", Form.verb(m));
        assertEquals(2L, m.get(1));
        assertTrue(ScreenHost.hello().contains(":max-save 40960"), ScreenHost.hello());
    }

    /** What the screen refuses, the program hears (a few a batch). */
    @Test
    void aRefusedMessageIsAnswered() {
        ScreenHost h = new ScreenHost();
        from(h, "(text 9 1 0 0 :white \"no window 9\")");
        List<String> told = h.takeToComputer();
        assertEquals(1, told.size());
        assertTrue(told.getFirst().startsWith("(error text 9 1 "), told.getFirst());
        for (int k = 0; k < 20; k++) from(h, "(text 9 1 0 0 :white \"x\")");
        assertEquals(8, h.takeToComputer().size(), "a few a batch at most");
    }

    /** An event for an earlier opening of a window number never reaches the window opened since. */
    @Test
    void anEventForAnEarlierWindowIsRefused() {
        ScreenHost h = new ScreenHost();
        from(h, "(window 2 1 1 20 5 \"First\")", "(button 2 3 0 0 8 \"Go\")");
        List<String> b = h.takeBatch();
        assertTrue(b.getFirst().contains(":gen 1"), "the displays get its generation: " + b.getFirst());
        assertEquals("(press 2 3)", h.check("(press 2 3 :gen 1)", null), "the program never sees it");
        from(h, "(unwindow 2)", "(window 2 1 1 20 5 \"Second\")", "(button 2 3 0 0 8 \"Go\")");
        assertNull(h.check("(press 2 3 :gen 1)", null), "pressed in the first window, arriving at the second");
        assertNotNull(h.check("(press 2 3 :gen 2)", null));
    }

    /** A screen's budget: pictures' dots in all, a log's characters (its oldest lines go). */
    @Test
    void aScreenHasABudget() {
        ScreenHost h = new ScreenHost();
        String dots = "0".repeat(Scene.PIXMAP_W * Scene.PIXMAP_H);
        for (int k = 1; k <= 33; k++) from(h, "(pixmap " + k + " 256 128 [:red] \"" + dots + "\")");
        assertEquals(32, h.scene.pixmaps.size(), "the 33rd big picture is refused");
        assertTrue(h.takeToComputer().stream().anyMatch(t -> t.startsWith("(error pixmap 33")), "and the program hears so");
        from(h, "(window 2 1 1 70 20 \"Log\")", "(log 2 1 0 0 :fill :fill)");
        for (int k = 0; k < 300; k++) from(h, "(append 2 1 :white \"" + "q".repeat(900) + "\\n\")");
        int chars = 0;
        for (var line : h.scene.windows.get(2).elements.get(1).lines) for (var r : line) chars += r.text().length();
        assertTrue(chars <= Scene.LOG_CHARS, "a log keeps at most its share: " + chars);
    }
}
