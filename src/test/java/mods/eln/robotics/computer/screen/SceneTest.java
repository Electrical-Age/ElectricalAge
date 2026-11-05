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

    @Test
    void buildsTabsAndElementsInOrder() {
        Scene s = new Scene();
        assertTrue(s.apply(p("(tab 1 \"REPL\")")));
        assertTrue(s.apply(p("(box 1 5 0 0 10 2 :blue)")));
        assertTrue(s.apply(p("(text 1 6 1 0 :white \"hello\")")));
        assertTrue(s.apply(p("(box 1 5 0 0 12 2 :red)")), "an update");
        assertEquals(List.of(5, 6), List.copyOf(s.tabs.get(1).elements.keySet()), "an update keeps its place");
        assertEquals(Form.kw("red"), s.tabs.get(1).elements.get(5).msg.get(7));
        assertFalse(s.apply(p("(box 9 1 0 0 1 1 :red)")), "no such tab");
        assertFalse(s.apply(p("(frobnicate 1)")), "no such message");
        assertTrue(s.apply(p("(remove 1 6)")));
        assertEquals(List.of(5), List.copyOf(s.tabs.get(1).elements.keySet()));
    }

    @Test
    void logsKeepTheirLastLines() {
        Scene s = new Scene();
        s.apply(p("(tab 1 \"x\")"));
        s.apply(p("(log 1 1 0 0 52 17)"));
        s.apply(p("(append 1 1 :white \"one\\ntw\")"));
        s.apply(p("(append 1 1 :red \"o\\n\")"));
        Scene.Element e = s.tabs.get(1).elements.get(1);
        assertEquals(3, e.lines.size());
        assertEquals(List.of(new Scene.Run("white", "tw"), new Scene.Run("red", "o")), List.copyOf(e.lines).get(1));
        for (int k = 0; k < 2 * Scene.LOG_LINES; k++) s.apply(p("(append 1 1 :white \"x\\n\")"));
        assertEquals(Scene.LOG_LINES, e.lines.size());
    }

    @Test
    void replayRebuildsTheScene() {
        Scene s = new Scene();
        for (String m : List.of("(screen 60 20)", "(dict 0 [\"print\" \"map\"])", "(tab 1 \"REPL\")", "(log 1 1 0 0 60 19)",
            "(append 1 1 :white \"> (+ 1 2)\\n\")", "(append 1 1 :green \"3\\n\")", "(line 1 2 0 19 60 \"> \")",
            "(busy 1 2 4 true)", "(tab 2 \"Clock\")", "(text 2 1 0 0 :yellow \"12:00\")", "(focus 2)"))
            assertTrue(s.apply(p(m)), m);
        Scene copy = new Scene();
        for (String line : s.replay()) assertTrue(copy.apply(p(line)), line);
        assertEquals(s.replay(), copy.replay());
        assertEquals(60, copy.w);
        assertEquals(2, copy.focus);
        assertTrue(copy.tabs.get(1).elements.get(2).busy);
        assertEquals(List.of("print", "map"), copy.baseDict);
        assertEquals("3", List.copyOf(copy.tabs.get(1).elements.get(1).lines).get(1).get(0).text());
    }

    @Test
    void resetEmptiesIt() {
        Scene s = new Scene();
        s.apply(p("(tab 1 \"x\")"));
        s.apply(p("(reset 52 18)"));
        assertTrue(s.tabs.isEmpty());
    }
}
