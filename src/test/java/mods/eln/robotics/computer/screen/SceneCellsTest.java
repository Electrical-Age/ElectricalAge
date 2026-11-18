package mods.eln.robotics.computer.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The monitor's cells (SceneCells): what a tab of a scene shows, cell by cell. */
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
    void laysOutTextLogsLinesAndButtons() {
        Scene s = scene("(reset 80 25)", "(tab 1 \"REPL\")", "(log 1 1 0 0 80 24)", "(append 1 1 :red \"first\\nsecond \")",
            "(append 1 1 :white \"line\")", "(line 1 2 0 24 80 \"> \")", "(button 1 3 70 0 6 \"Run\")", "(text 1 4 0 5 :lime \"hi\" :blue)");
        SceneCells c = SceneCells.of(s, 1);
        assertEquals("first", row(c, 0, 0, 5));
        assertEquals("second line", row(c, 1, 0, 11));
        assertEquals(c.ink[1 * 80], SceneCells.colour(Form.kw("red"), 0));
        assertEquals(c.ink[1 * 80 + 7], SceneCells.colour(Form.kw("white"), 0));
        assertEquals("> ", row(c, 24, 0, 2));
        assertEquals(" Run  ", row(c, 0, 70, 6));
        assertEquals(SceneCells.BUTTON, c.paper[70]);
        assertEquals("hi", row(c, 5, 0, 2));
        assertEquals(SceneCells.colour(Form.kw("blue"), 0), c.paper[5 * 80]);
    }

    @Test
    void aLogShowsItsNewestRowsWrapped() {
        Scene s = scene("(reset 10 3)", "(tab 1 \"t\")", "(log 1 1 0 0 4 2)", "(append 1 1 :white \"a\\nb\\ncdefgh\")");
        SceneCells c = SceneCells.of(s, 1);
        assertEquals("cdef", row(c, 0, 0, 4));
        assertEquals("gh  ", row(c, 1, 0, 4));
    }

    @Test
    void theMonitorFollowsTheFocusThenTheRepl() {
        Scene s = scene("(tab 0 \"Console\")", "(tab 1 \"REPL\")", "(tab 2 \"edit\")");
        assertEquals(1, SceneCells.defaultTab(s));
        s.apply(Form.parse("(focus 2)"));
        assertEquals(2, SceneCells.defaultTab(s));
        assertEquals(-1, SceneCells.defaultTab(new Scene()));
        assertEquals(' ', SceneCells.of(new Scene(), 3).at(0, 0));
    }
}
