package mods.eln.robotics.computer.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The screen composed in cells (SceneCells): the desk, framed windows back to front, elements, requesters, the bar. */
class SceneCellsTest {
    static Scene scene(String... lines) {
        Scene s = new Scene();
        for (String l : lines) assertTrue(s.apply(Form.parse(l)), l);
        return s;
    }

    static String row(SceneCells c, int y, int x0, int n) {
        StringBuilder b = new StringBuilder();
        for (int x = x0; x < x0 + n; x++) b.appendCodePoint(c.at(x, y));
        return b.toString();
    }

    @Test
    void defaultsTo80By25() {
        Scene s = new Scene();
        assertEquals(80, s.w);
        assertEquals(25, s.h);
    }

    @Test
    void aFramedWindowOverTheDesk() {
        Scene s = scene("(window 1 2 3 10 4 \"Shell\" [:close :depth :size :drag])", "(text 1 1 0 0 :lime \"hi\")", "(title \"Dynamo 1.0\")");
        SceneCells c = SceneCells.of(s);
        assertEquals(" Dynamo   Dynamo 1.0", row(c, 0, 0, 20), "the menu bar: the system menu, then the screen's title");
        assertEquals(Pens.DEFAULT.bar(), c.paper[0]);
        assertEquals("■ Shell", row(c, 2, 1, 7), "the title row: the close box, then the title");
        assertEquals(SceneCells.DEPTH, c.at(12, 2), "the depth box at its right end");
        assertEquals(Pens.DEFAULT.titleIdle(), c.paper[3 * 80 + 1], "the border: the title's colour all round");
        assertEquals(1, c.owner[3 * 80 + 1]);
        assertEquals(SceneCells.OWNER_BAR, c.owner[5]);
        assertEquals(SceneCells.OWNER_DESK, c.owner[20 * 80 + 40]);
        assertEquals("hi", row(c, 3, 2, 2), "content from (x, y)");
        assertEquals(SceneCells.SIZE, c.at(12, 7), "the size handle");
        assertEquals(Pens.DEFAULT.desk(), c.paper[20 * 80 + 40], "the desk");
        assertEquals(Pens.DEFAULT.titleIdle(), c.paper[2 * 80 + 5], "not active");
        assertEquals(Pens.DEFAULT.titleActive(), SceneCells.of(s, SceneCells.NONE, 1).paper[2 * 80 + 5], "active");
    }

    @Test
    void theFrontWindowCoversTheOneBehind() {
        Scene s = scene("(window 1 2 3 20 6 \"A\" [:drag])", "(text 1 1 0 1 :white \"aaaaaaaaaaaaaaaaaaaa\")",
            "(window 2 6 4 10 3 \"B\" [:drag])", "(text 2 1 0 0 :white \"bbbb\")");
        SceneCells c = SceneCells.of(s);
        assertEquals("aaa bbbb", row(c, 4, 2, 8), "B's border and text over A's");
        assertEquals(2, c.owner[4 * 80 + 5]);
        s.apply(Form.parse("(front 1)"));
        assertEquals("aaaaaaaa", row(SceneCells.of(s), 4, 2, 8));
    }

    @Test
    void contentIsClippedToItsWindow() {
        Scene s = scene("(window 1 2 3 5 2 \"A\" [])", "(text 1 1 0 0 :white \"0123456789\")");
        SceneCells c = SceneCells.of(s);
        assertEquals("01234 ", row(c, 3, 2, 6), "the border, not the text's rest");
    }

    @Test
    void logsLinesAndButtons() {
        Scene s = scene("(window 1 0 1 80 24 \"REPL\" [:borderless])", "(log 1 1 0 0 80 23)", "(append 1 1 :red \"first\\nsecond \")",
            "(append 1 1 :white \"line\")", "(line 1 2 0 23 80 \"> \")", "(button 1 3 70 0 6 \"Run\")");
        SceneCells c = SceneCells.of(s);
        assertEquals("first", row(c, 1, 0, 5));
        assertEquals("second line", row(c, 2, 0, 11));
        assertEquals(SceneCells.colour(Form.kw("red"), 0), c.ink[2 * 80]);
        assertEquals("> ", row(c, 24, 0, 2));
        assertEquals(" Run  ", row(c, 1, 70, 6));
        SceneCells mine = SceneCells.of(s, new SceneCells.Local() {
            @Override
            public String lineText(int win, int id) {
                return "(+ 1 2)";
            }
        }, 1);
        assertEquals("> (+ 1 2)", row(mine, 24, 0, 9), "a display's own half-typed line");
    }

    @Test
    void aLogShowsItsNewestRowsWrapped() {
        Scene s = scene("(window 1 0 1 10 3 \"t\" [:borderless])", "(log 1 1 0 0 4 2)", "(append 1 1 :white \"a\\nb\\ncdefgh\")");
        SceneCells c = SceneCells.of(s);
        assertEquals("cdef", row(c, 1, 0, 4));
        assertEquals("gh  ", row(c, 2, 0, 4));
    }

    @Test
    void aRequesterOverItsWindow() {
        Scene s = scene("(window 1 2 3 40 10 \"Ed\" [:close])", "(ask 1 7 \"Quit\" \"Save changes?\" [\"Save\" \"Discard\"])");
        int[][] lay = SceneCells.askLayout(s.windows.get(1));
        SceneCells c = SceneCells.of(s);
        assertEquals('╔', c.at(lay[0][0], lay[0][1]));
        int by = lay[0][1] + lay[0][3] - 2;
        assertEquals(" Save ", row(c, by, lay[1][0], lay[1][1]));
        assertEquals(" Discard ", row(c, by, lay[2][0], lay[2][1]));
    }

    /** Prefs' pen sets: the desk, bar and title in the set's colours; the contents keep theirs; replayed. */
    @Test
    void penSetsColourTheDeskBarAndFrames() {
        Scene s = scene("(window 1 2 3 10 4 \"Shell\" [:close :depth :size :drag])", "(text 1 1 0 0 :lime \"hi\")", "(pens \"Amber\")", "(clock 12)");
        SceneCells c = SceneCells.of(s, SceneCells.NONE, 1);
        Pens amber = Pens.SETS.get("Amber");
        assertEquals(amber.bar(), c.paper[0], "the bar");
        assertEquals(amber.desk(), c.paper[20 * 80 + 40], "the desk");
        assertEquals(amber.titleActive(), c.paper[2 * 80 + 5], "the active title");
        Scene again = scene(s.replay().toArray(String[]::new));
        assertEquals("Amber", again.pens, "replayed");
        assertEquals(12, again.clockHours, "replayed");
        assertTrue(s.apply(Form.parse("(pens \"Nonesuch\")")));
        assertEquals("Grid Blue", s.pens, "an unknown set: the default");
        assertTrue(s.apply(Form.parse("(reset 80 25)")));
        assertEquals(24, s.clockHours, "a reset: the defaults");
    }
}
