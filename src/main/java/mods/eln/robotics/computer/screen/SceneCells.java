package mods.eln.robotics.computer.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One tab of a scene laid out in character cells, as the monitor shows it (Baughn 2026-11-18: the monitor shows what
 * the screen shows): each cell's code point, ink and paper (ARGB), in the screen's colours. What only a player's own
 * screen has (a half-typed line, scrolling, an editor's unsaved text) isn't here: lines show their prompt, logs and
 * lists their newest and first rows, editors the text the computer sent.
 */
public final class SceneCells {
    public static final int PAPER = 0xFF101418, INK = 0xFFE6E6E6, PROMPT = 0xFF8FB0FF, BUSY = 0xFFE0C060, BUTTON = 0xFF5A5A66,
        LIST = 0xFF181C22, LIST_INK = 0xFFD0D0D0, RAW = 0xFF0C0E10, BOX = 0xFF404040, EDITOR = 0xFF1C1D22;

    public final int w, h;
    public final int[] glyph, ink, paper;

    public SceneCells(int w, int h) {
        this.w = w;
        this.h = h;
        glyph = new int[w * h];
        ink = new int[w * h];
        paper = new int[w * h];
        java.util.Arrays.fill(paper, PAPER);
        java.util.Arrays.fill(glyph, ' ');
    }

    /** The tab a viewer without the screen open sees: the one the program brought forward, else the REPL's, else the first. */
    public static int defaultTab(Scene s) {
        if (s.tabs.containsKey(s.focus)) return s.focus;
        if (s.tabs.containsKey(1)) return 1;
        return s.tabs.isEmpty() ? -1 : s.tabs.keySet().iterator().next();
    }

    /** Tab t of s in cells (all blank if there is no such tab). */
    public static SceneCells of(Scene s, int t) {
        SceneCells c = new SceneCells(s.w, s.h);
        Scene.Tab tab = s.tabs.get(t);
        if (tab != null) for (Map.Entry<Integer, Scene.Element> en : tab.elements.entrySet()) c.draw(en.getValue());
        return c;
    }

    public int at(int x, int y) {
        return glyph[y * w + x];
    }

    /** Text from cell (x, y), at most max cells; spaces leave the paper alone. */
    void text(String s, int x, int y, int max, int colour) {
        if (y < 0 || y >= h) return;
        for (int k = 0, c = 0; k < s.length() && c < max; c++) {
            int cp = s.codePointAt(k);
            k += Character.charCount(cp);
            int cx = x + c;
            if (cx < 0 || cx >= w) continue;
            glyph[y * w + cx] = cp;
            ink[y * w + cx] = colour;
        }
    }

    void fill(int x, int y, int fw, int fh, int colour) {
        for (int yy = Math.max(0, y); yy < Math.min(h, y + fh); yy++)
            for (int xx = Math.max(0, x); xx < Math.min(w, x + fw); xx++) paper[yy * w + xx] = colour;
    }

    void draw(Scene.Element e) {
        List<Object> m = e.msg;
        int x = e.x(), y = e.y();
        switch (e.kind) {
            case "box" -> fill(x, y, (int) Form.num(m, 5, 1), (int) Form.num(m, 6, 1), colour(m.size() > 7 ? m.get(7) : null, BOX));
            case "text" -> {
                String s = Form.str(m, 6, "");
                if (m.size() > 7 && m.get(7) != null) fill(x, y, s.codePointCount(0, s.length()), 1, colour(m.get(7), 0));
                text(s, x, y, w - x, colour(m.size() > 5 ? m.get(5) : null, INK));
            }
            case "log" -> {
                int lw = (int) Form.num(m, 5, 1), lh = (int) Form.num(m, 6, 1);
                List<List<Scene.Run>> rows = new ArrayList<>();
                var it = e.lines.descendingIterator();
                while (it.hasNext() && rows.size() < lh) {
                    List<List<Scene.Run>> wrapped = wrap(it.next(), lw);
                    for (int k = wrapped.size() - 1; k >= 0 && rows.size() < lh; k--) rows.add(0, wrapped.get(k));
                }
                for (int r = 0; r < rows.size(); r++) {
                    int c = 0;
                    for (Scene.Run run : rows.get(r)) {
                        text(run.text(), x + c, y + r, lw - c, colour(Form.kw(run.colour()), INK));
                        c += run.text().codePointCount(0, run.text().length());
                    }
                }
            }
            case "line" -> {
                int lw = (int) Form.num(m, 5, 1);
                text(Form.str(m, 6, ""), x, y, lw, PROMPT);
                if (e.busy) text("*", x + lw - 1, y, 1, BUSY);
            }
            case "button" -> {
                int bw = (int) Form.num(m, 5, 4);
                String label = Form.str(m, 6, "");
                fill(x, y, bw, 1, BUTTON);
                text(label, x + Math.max(0, (bw - label.length()) / 2), y, bw, 0xFFFFFFFF);
            }
            case "list" -> {
                int lw = (int) Form.num(m, 5, 1), lh = (int) Form.num(m, 6, 1);
                List<?> items = m.size() > 7 && m.get(7) instanceof List<?> l ? l : List.of();
                fill(x, y, lw, lh, LIST);
                for (int r = 0; r < lh && r < items.size(); r++) text(String.valueOf(items.get(r)), x, y + r, lw, LIST_INK);
            }
            case "editor" -> {
                int ew = (int) Form.num(m, 5, 1), eh = (int) Form.num(m, 6, 1);
                fill(x, y, ew, eh, EDITOR);
                String[] lines = Form.str(m, 10, "").split("\n", -1);
                for (int r = 0; r < eh - 1 && r < lines.length; r++) text(lines[r].replace('\t', ' '), x, y + r, ew, INK);
            }
            case "raw" -> fill(x, y, (int) Form.num(m, 5, 1), (int) Form.num(m, 6, 1), RAW);
            default -> {
            }
        }
    }

    /** A screen colour by its keyword (the 16 bus colours), or dflt. */
    public static int colour(Object kw, int dflt) {
        if (!(kw instanceof Form.Keyword k)) return dflt;
        int c = mods.eln.bus.BusChannel.byName(k.name().replace('-', '_'));
        return c < 0 ? dflt : 0xFF000000 | mods.eln.bus.BusChannel.rgb(c);
    }

    /** A log line cut into rows of w cells. */
    public static List<List<Scene.Run>> wrap(List<Scene.Run> line, int w) {
        List<List<Scene.Run>> out = new ArrayList<>();
        List<Scene.Run> cur = new ArrayList<>();
        int col = 0;
        for (Scene.Run r : line) {
            String t = r.text();
            while (!t.isEmpty()) {
                int room = w - col;
                if (room <= 0) {
                    out.add(cur);
                    cur = new ArrayList<>();
                    col = 0;
                    room = w;
                }
                int n = Math.min(room, t.length());
                cur.add(new Scene.Run(r.colour(), t.substring(0, n)));
                col += n;
                t = t.substring(n);
            }
        }
        out.add(cur);
        return out;
    }
}
