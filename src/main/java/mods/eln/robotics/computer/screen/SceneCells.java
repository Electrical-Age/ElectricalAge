package mods.eln.robotics.computer.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The whole screen composed in character cells (each cell's code point, ink and paper, ARGB): the desk, the windows
 * back to front with their frames and title rows, their elements, a requester, and the screen bar on row 0. The
 * monitor paints exactly this; the computer's screen draws it too, with what only its own player has (a half-typed
 * line, scrolling) given by a {@link Local}, and overlays (the cursor, menus, a drag) on top.
 * <p>
 * Frames use the game font's box-drawing glyphs: a window's title row has the close box (■) at its left end and the
 * depth box (▒) at its right; its bottom right corner is the size handle (╝).
 */
public final class SceneCells {
    // the pens (Dynamo's default scheme: slate desk, cream text, amber marks)
    public static final int DESK = 0xFF3B5470, BAR = 0xFFE8E0C8, BAR_TEXT = 0xFF14161A, TITLE_ACTIVE = 0xFFE0A030,
        TITLE_ACTIVE_TEXT = 0xFF14161A, TITLE_IDLE = 0xFF5A6070, TITLE_IDLE_TEXT = 0xFFD8D2C0, FRAME_ACTIVE = 0xFFE8E0C8,
        FRAME_IDLE = 0xFF8A90A0, PAPER = 0xFF101418, INK = 0xFFE6E6E6, PROMPT = 0xFF8FB0FF, BUSY = 0xFFE0C060,
        BUTTON = 0xFF5A5A66, LIST = 0xFF181C22, LIST_INK = 0xFFD0D0D0, RAW = 0xFF0C0E10, BOX = 0xFF404040, EDITOR = 0xFF1C1D22,
        ASK = 0xFFD8D2C0, ASK_TEXT = 0xFF14161A, ASK_BUTTON = 0xFF8A90A0;
    public static final int CLOSE = '■', DEPTH = '▒', SIZE = '╝';

    /** What only one player's display has, for the cells it composes (the monitor has none of it). */
    public interface Local {
        /** A line's text as this player is editing it (after the prompt), or null: none. */
        default String lineText(int win, int id) {
            return null;
        }

        /** How far this player scrolled a log back, or a list down. */
        default int scroll(int win, int id) {
            return 0;
        }

        /** An editor this display draws itself (a widget): its cells are left as paper. */
        default boolean ownEditor(int win, int id) {
            return false;
        }
    }

    public static final Local NONE = new Local() {
    };

    public final int w, h;
    public final int[] glyph, ink, paper;

    public SceneCells(int w, int h) {
        this.w = w;
        this.h = h;
        glyph = new int[w * h];
        ink = new int[w * h];
        paper = new int[w * h];
        java.util.Arrays.fill(paper, DESK);
        java.util.Arrays.fill(glyph, ' ');
    }

    /** The screen as everyone sees it. */
    public static SceneCells of(Scene s) {
        return of(s, NONE, s.active);
    }

    /** The screen with one display's own state, and its idea of the active window. */
    public static SceneCells of(Scene s, Local local, int active) {
        SceneCells c = new SceneCells(s.w, s.h);
        for (int id : s.order) {
            Scene.Window win = s.windows.get(id);
            if (win == null) continue;
            c.window(id, win, id == active, local);
        }
        c.bar(s);
        return c;
    }

    public int at(int x, int y) {
        return glyph[y * w + x];
    }

    // ---- painting, clipped to a rectangle (the window being drawn)

    int cx0, cy0, cx1 = Integer.MAX_VALUE, cy1 = Integer.MAX_VALUE;

    void clip(int x, int y, int cw, int ch) {
        cx0 = Math.max(0, x);
        cy0 = Math.max(0, y);
        cx1 = Math.min(w, x + cw);
        cy1 = Math.min(h, y + ch);
    }

    void unclip() {
        cx0 = 0;
        cy0 = 0;
        cx1 = w;
        cy1 = h;
    }

    boolean in(int x, int y) {
        return x >= cx0 && y >= cy0 && x < cx1 && y < cy1 && x < w && y < h;
    }

    void put(int x, int y, int cp, int colour) {
        if (!in(x, y)) return;
        glyph[y * w + x] = cp;
        ink[y * w + x] = colour;
    }

    /** Text from cell (x, y), at most max cells; spaces leave the paper alone. */
    void text(String s, int x, int y, int max, int colour) {
        for (int k = 0, c = 0; k < s.length() && c < max; c++) {
            int cp = s.codePointAt(k);
            k += Character.charCount(cp);
            put(x + c, y, cp, colour);
        }
    }

    void fill(int x, int y, int fw, int fh, int colour) {
        for (int yy = y; yy < y + fh; yy++)
            for (int xx = x; xx < x + fw; xx++)
                if (in(xx, yy)) {
                    paper[yy * w + xx] = colour;
                    glyph[yy * w + xx] = ' ';
                }
    }

    // ---- the screen bar, windows, requesters

    void bar(Scene s) {
        unclip();
        fill(0, 0, w, 1, BAR);
        text(" " + (s.title.isEmpty() ? "Dynamo" : s.title), 0, 0, w, BAR_TEXT);
    }

    void window(int id, Scene.Window win, boolean active, Local local) {
        int[] o = win.outer();
        clip(o[0], o[1], o[2], o[3]);
        fill(o[0], o[1], o[2], o[3], PAPER);
        if (win.framed()) {
            int fr = active ? FRAME_ACTIVE : FRAME_IDLE;
            int x0 = win.x - 1, x1 = win.x + win.cw, y0 = win.y - 1, y1 = win.y + win.ch;
            fill(x0, y0, o[2], 1, active ? TITLE_ACTIVE : TITLE_IDLE);
            int tink = active ? TITLE_ACTIVE_TEXT : TITLE_IDLE_TEXT;
            put(x0, y0, win.has("close") ? CLOSE : ' ', tink);
            put(x1, y0, win.has("depth") ? DEPTH : ' ', tink);
            String t = win.title;
            int room = win.cw - 1;
            text(t.length() > room ? t.substring(0, Math.max(0, room)) : t, win.x + 1, y0, room, tink);
            for (int y = win.y; y < y1; y++) {
                put(x0, y, '│', fr);
                put(x1, y, '│', fr);
            }
            put(x0, y1, '└', fr);
            for (int x = win.x; x < x1; x++) put(x, y1, '─', fr);
            put(x1, y1, win.has("size") ? SIZE : '┘', fr);
        }
        clip(win.x, win.y, win.cw, win.ch);
        for (Map.Entry<Integer, Scene.Element> en : win.elements.entrySet()) draw(id, en.getKey(), en.getValue(), win, local);
        if (win.ask != null) ask(win);
        unclip();
    }

    /** A requester's box over its window: [x, y, w, h] in cells, and each button's [x, w] on its last row. */
    public static int[][] askLayout(Scene.Window win) {
        List<Object> m = win.ask;
        String title = Form.str(m, 3, ""), body = Form.str(m, 4, "");
        List<String> buttons = buttons(m);
        String[] lines = body.split("\n", -1);
        int bw = 0;
        for (String b : buttons) bw += b.length() + 4;
        int inner = Math.max(title.length() + 2, bw);
        for (String l : lines) inner = Math.max(inner, l.length() + 2);
        inner = Math.min(inner, win.cw - 2);
        int bh = lines.length + 4;
        int x = win.x + Math.max(0, (win.cw - inner - 2) / 2), y = win.y + Math.max(0, (win.ch - bh) / 2);
        int[][] out = new int[1 + buttons.size()][];
        out[0] = new int[]{x, y, inner + 2, bh};
        int bx = x + 1 + Math.max(0, (inner - bw) / 2) + 1;
        for (int i = 0; i < buttons.size(); i++) {
            out[1 + i] = new int[]{bx, buttons.get(i).length() + 2};
            bx += buttons.get(i).length() + 4;
        }
        return out;
    }

    public static List<String> buttons(List<Object> m) {
        List<String> out = new ArrayList<>();
        if (m.size() > 5 && m.get(5) instanceof List<?> l) for (Object o : l) if (o instanceof String s) out.add(s);
        if (out.isEmpty()) out.add("OK");
        return out;
    }

    void ask(Scene.Window win) {
        int[][] lay = askLayout(win);
        int x = lay[0][0], y = lay[0][1], bw = lay[0][2], bh = lay[0][3];
        fill(x, y, bw, bh, ASK);
        put(x, y, '╔', ASK_TEXT);
        put(x + bw - 1, y, '╗', ASK_TEXT);
        put(x, y + bh - 1, '╚', ASK_TEXT);
        put(x + bw - 1, y + bh - 1, '╝', ASK_TEXT);
        for (int xx = x + 1; xx < x + bw - 1; xx++) {
            put(xx, y, '═', ASK_TEXT);
            put(xx, y + bh - 1, '═', ASK_TEXT);
        }
        for (int yy = y + 1; yy < y + bh - 1; yy++) {
            put(x, yy, '║', ASK_TEXT);
            put(x + bw - 1, yy, '║', ASK_TEXT);
        }
        String title = Form.str(win.ask, 3, "");
        if (!title.isEmpty()) text(" " + title + " ", x + 1, y, bw - 2, ASK_TEXT);
        String[] lines = Form.str(win.ask, 4, "").split("\n", -1);
        for (int i = 0; i < lines.length; i++) text(lines[i], x + 2, y + 1 + i, bw - 4, ASK_TEXT);
        List<String> b = buttons(win.ask);
        for (int i = 0; i < b.size(); i++) {
            int[] at = lay[1 + i];
            fill(at[0], y + bh - 2, at[1], 1, ASK_BUTTON);
            text(" " + b.get(i) + " ", at[0], y + bh - 2, at[1], ASK_TEXT);
        }
    }

    void draw(int wid, int id, Scene.Element e, Scene.Window win, Local local) {
        List<Object> m = e.msg;
        int x = win.x + e.x(), y = win.y + e.y();
        switch (e.kind) {
            case "box" -> fill(x, y, e.size(5, win, 1), e.size(6, win, 1), colour(m.size() > 7 ? m.get(7) : null, BOX));
            case "text" -> {
                String s = Form.str(m, 6, "");
                if (m.size() > 7 && m.get(7) != null) fill(x, y, s.codePointCount(0, s.length()), 1, colour(m.get(7), 0));
                text(s, x, y, win.cw - e.x(), colour(m.size() > 5 ? m.get(5) : null, INK));
            }
            case "log" -> {
                int lw = e.size(5, win, 1), lh = e.size(6, win, 1);
                List<List<Scene.Run>> rows = new ArrayList<>();
                int need = lh + local.scroll(wid, id);
                var it = e.lines.descendingIterator();
                while (it.hasNext() && rows.size() < need) {
                    List<List<Scene.Run>> wrapped = wrap(it.next(), lw);
                    for (int k = wrapped.size() - 1; k >= 0 && rows.size() < need; k--) rows.add(0, wrapped.get(k));
                }
                int off = Math.max(0, Math.min(local.scroll(wid, id), Math.max(0, rows.size() - lh)));
                int first = Math.max(0, rows.size() - lh - off);
                for (int r = 0; r < lh && first + r < rows.size(); r++) {
                    int c = 0;
                    for (Scene.Run run : rows.get(first + r)) {
                        text(run.text(), x + c, y + r, lw - c, colour(Form.kw(run.colour()), INK));
                        c += run.text().codePointCount(0, run.text().length());
                    }
                }
            }
            case "line" -> {
                int lw = e.size(5, win, 1);
                String prompt = Form.str(m, 6, "");
                text(prompt, x, y, lw, PROMPT);
                String own = local.lineText(wid, id);
                int px = prompt.codePointCount(0, prompt.length());
                if (own != null) text(own, x + px, y, lw - px - 1, INK);
                if (e.busy) text("*", x + lw - 1, y, 1, BUSY);
            }
            case "button" -> {
                int bw = e.size(5, win, 4);
                String label = Form.str(m, 6, "");
                fill(x, y, bw, 1, BUTTON);
                text(label, x + Math.max(0, (bw - label.length()) / 2), y, bw, 0xFFFFFFFF);
            }
            case "list" -> {
                int lw = e.size(5, win, 1), lh = e.size(6, win, 1);
                List<?> items = m.size() > 7 && m.get(7) instanceof List<?> l ? l : List.of();
                int off = Math.max(0, Math.min(local.scroll(wid, id), Math.max(0, items.size() - lh)));
                fill(x, y, lw, lh, LIST);
                for (int r = 0; r < lh && off + r < items.size(); r++) text(String.valueOf(items.get(off + r)), x, y + r, lw, LIST_INK);
            }
            case "editor" -> {
                int ew = e.size(5, win, 1), eh = e.size(6, win, 1);
                fill(x, y, ew, eh, EDITOR);
                if (local.ownEditor(wid, id)) break;
                String[] lines = Form.str(m, 10, "").split("\n", -1);
                for (int r = 0; r < eh - 1 && r < lines.length; r++) text(lines[r].replace('\t', ' '), x, y + r, ew, INK);
            }
            case "raw" -> fill(x, y, e.size(5, win, 1), e.size(6, win, 1), RAW);
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
            while (!t.isEmpty() && w > 0) {
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
