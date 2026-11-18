package mods.eln.robotics.computer.screen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The screen protocol's parser and the scene it builds. */
class SceneTest {
    static List<Object> p(String s) {
        List<Object> m = Form.parse(s);
        assertNotNull(m, s);
        return m;
    }

    @Test
    void parsesWhatMessagesUse() {
        List<Object> m = p("(text 1 3 0 2 :light-blue \"a\\\"b\\nc\\x41\" nil true [1 [2]])");
        assertEquals(new Form.Symbol("text"), m.get(0));
        assertEquals(1L, m.get(1));
        assertEquals(Form.kw("light-blue"), m.get(5));
        assertEquals("a\"b\ncA", m.get(6));
        assertNull(m.get(7));
        assertEquals(Boolean.TRUE, m.get(8));
        assertEquals(List.of(1L, List.of(2L)), m.get(9));
        assertEquals("text", Form.verb(m));
    }

    @Test
    void refusesWhatIsntAMessage() {
        assertNull(Form.parse("text 1 2"));
        assertNull(Form.parse("(text 1 \"unclosed)"));
        assertNull(Form.parse("(a [[[[[1]]]]])"), "too deep");
        assertNull(Form.parse("(a) (b)"));
        assertNull(Form.parse("()"));
        assertNull(Form.parse("(" + "x".repeat(Form.MAX_LINE) + ")"));
    }

    @Test
    void printsWhatItParses() {
        String line = Form.print(new Form.Symbol("submit"), 1L, 2L, 7L, "say \"hi\"\n");
        assertEquals("(submit 1 2 7 \"say \\\"hi\\\"\\n\")", line);
        assertEquals("say \"hi\"\n", Form.str(Form.parse(line), 4, null));
    }

    static final String WIN1 = "(window 1 2 3 40 10 \"REPL\" [:close :depth :size :drag])";

    @Test
    void buildsWindowsAndElementsInOrder() {
        Scene s = new Scene();
        assertTrue(s.apply(p(WIN1)));
        assertTrue(s.apply(p("(box 1 5 0 0 10 2 :blue)")));
        assertTrue(s.apply(p("(text 1 6 1 0 :white \"hello\")")));
        assertTrue(s.apply(p("(box 1 5 0 0 12 2 :red)")), "an update");
        assertEquals(List.of(5, 6), List.copyOf(s.windows.get(1).elements.keySet()), "an update keeps its place");
        assertEquals(Form.kw("red"), s.windows.get(1).elements.get(5).msg.get(7));
        assertFalse(s.apply(p("(box 9 1 0 0 1 1 :red)")), "no such window");
        assertFalse(s.apply(p("(frobnicate 1)")), "no such message");
        assertFalse(s.apply(p("(tab 1 \"old\")")), "tabs are gone");
        assertTrue(s.apply(p("(remove 1 6)")));
        assertEquals(List.of(5), List.copyOf(s.windows.get(1).elements.keySet()));
    }

    @Test
    void windowsStackAndStayOnTheScreen() {
        Scene s = new Scene();
        s.apply(p(WIN1));
        s.apply(p("(window 2 10 5 20 6 \"Clock\" [:drag])"));
        s.apply(p("(window 3 0 0 0 0 \"Desk\" [:backdrop])"));
        assertEquals(List.of(3, 1, 2), s.order, "a backdrop at the back, the newest in front");
        s.apply(p("(front 1)"));
        assertEquals(List.of(3, 2, 1), s.order);
        s.apply(p("(back 1)"));
        assertEquals(List.of(3, 1, 2), s.order, "lowered, but in front of the backdrop");
        Scene.Window desk = s.windows.get(3);
        assertEquals(List.of(0, 1, 80, 24), List.of(desk.x, desk.y, desk.cw, desk.ch), "a backdrop fills the screen under the bar");
        s.apply(p("(place 2 200 -5 500 1)"));
        Scene.Window c = s.windows.get(2);
        assertEquals(List.of(1, 2, 78, 1), List.of(c.x, c.y, c.cw, c.ch), "kept on the screen, its frame too");
        assertEquals(2, s.windowAt(1, 1), "its title row");
        assertEquals(3, s.windowAt(79, 20), "the desk");
        assertEquals(-1, s.windowAt(5, 0), "the screen bar");
        s.apply(p("(window 2 1 1 1 1 \"Clock 2\" [:drag])"));
        assertEquals("Clock 2", c.title);
        assertEquals(78, c.cw, "an open window's place stays where the players put it");
    }

    @Test
    void playersMoveStackAndActivate() {
        Scene s = new Scene();
        s.apply(p(WIN1));
        s.apply(p("(window 2 10 5 20 6 \"Clock\" [:drag])"));
        assertEquals(p("(place 1 5 6 30 8)"), s.applyPlayer(p("(moved 1 5 6 30 8)")));
        assertEquals(30, s.windows.get(1).cw);
        assertEquals(p("(back 2)"), s.applyPlayer(p("(lowered 2)")));
        assertEquals(List.of(2, 1), s.order);
        assertEquals(p("(activate 2)"), s.applyPlayer(p("(active 2)")));
        assertEquals(2, s.active);
        assertNull(s.applyPlayer(p("(moved 9 1 1 1 1)")), "no such window");
        assertNull(s.applyPlayer(p("(press 1 2)")), "not a window report");
    }

    @Test
    void fillSizesFollowTheWindow() {
        Scene s = new Scene();
        s.apply(p(WIN1));
        s.apply(p("(log 1 1 0 0 :fill [:fill 1])"));
        Scene.Window w = s.windows.get(1);
        Scene.Element e = w.elements.get(1);
        assertEquals(40, e.size(5, w, 0));
        assertEquals(9, e.size(6, w, 0));
        s.applyPlayer(p("(moved 1 2 3 50 12)"));
        assertEquals(50, e.size(5, w, 0));
        assertEquals(11, e.size(6, w, 0));
    }

    @Test
    void menusRequestersAndTitle() {
        Scene s = new Scene();
        s.apply(p(WIN1));
        assertTrue(s.apply(p("(menus 1 [[\"Project\" [\"Open...\" \"o\"] nil [\"Quit\" \"q\"]]])")));
        assertTrue(s.apply(p("(enable 1 \"Project\" \"Open...\" false)")));
        assertTrue(s.windows.get(1).disabled.contains("Project/Open..."));
        assertTrue(s.apply(p("(ask 1 7 \"Quit\" \"Really?\" [\"Yes\" \"No\"])")));
        assertTrue(s.apply(p("(title \"Shell\")")));
        Scene copy = new Scene();
        for (String line : s.replay()) assertTrue(copy.apply(p(line)), line);
        assertEquals(s.replay(), copy.replay());
        assertEquals("Shell", copy.title);
        assertNotNull(copy.windows.get(1).ask);
        assertTrue(copy.windows.get(1).disabled.contains("Project/Open..."));
        assertTrue(s.apply(p("(unask 1)")));
        assertNull(s.windows.get(1).ask);
    }

    @Test
    void logsKeepTheirLastLines() {
        Scene s = new Scene();
        s.apply(p(WIN1));
        s.apply(p("(log 1 1 0 0 52 17)"));
        s.apply(p("(append 1 1 :white \"one\\ntw\")"));
        s.apply(p("(append 1 1 :red \"o\\n\")"));
        Scene.Element e = s.windows.get(1).elements.get(1);
        assertEquals(3, e.lines.size());
        assertEquals(List.of(new Scene.Run("white", "tw"), new Scene.Run("red", "o")), List.copyOf(e.lines).get(1));
        for (int k = 0; k < 2 * Scene.LOG_LINES; k++) s.apply(p("(append 1 1 :white \"x\\n\")"));
        assertEquals(Scene.LOG_LINES, e.lines.size());
    }

    @Test
    void replayRebuildsTheScene() {
        Scene s = new Scene();
        for (String m : List.of("(screen 60 20)", "(dict 0 [\"print\" \"map\"])", WIN1, "(log 1 1 0 0 40 9)",
            "(append 1 1 :white \"> (+ 1 2)\\n\")", "(append 1 1 :green \"3\\n\")", "(line 1 2 0 9 40 \"> \")",
            "(busy 1 2 4 true)", "(window 2 10 5 20 6 \"Clock\" [:drag])", "(text 2 1 0 0 :yellow \"12:00\")", "(front 1)", "(activate 2)"))
            assertTrue(s.apply(p(m)), m);
        s.applyPlayer(p("(moved 1 4 4 30 8)"));
        Scene copy = new Scene();
        for (String line : s.replay()) assertTrue(copy.apply(p(line)), line);
        assertEquals(s.replay(), copy.replay());
        assertEquals(60, copy.w);
        assertEquals(2, copy.active);
        assertEquals(List.of(1, 2), copy.order);
        assertEquals(30, copy.windows.get(1).cw, "a player's move is kept");
        assertTrue(copy.windows.get(1).elements.get(2).busy);
        assertEquals(List.of("print", "map"), copy.baseDict);
        assertEquals("3", List.copyOf(copy.windows.get(1).elements.get(1).lines).get(1).get(0).text());
    }

    @Test
    void resetEmptiesIt() {
        Scene s = new Scene();
        s.apply(p(WIN1));
        s.apply(p("(reset 52 18)"));
        assertTrue(s.windows.isEmpty());
        assertTrue(s.order.isEmpty());
    }
}
