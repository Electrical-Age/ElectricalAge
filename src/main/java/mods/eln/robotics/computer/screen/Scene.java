package mods.eln.robotics.computer.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a computer's screen shows, as Wire (the window protocol, Baughn 2026-11-18: Dynamo) builds it: a screen of
 * w x h character cells with a screen bar on row 0 and windows over a desk. Each window has its content's place and
 * size in cells, a title, flags, its elements (each the last message that made it, drawn in the order they were first
 * made: Argon's review), its menus and a requester. Logs keep their last LOG_LINES lines. The host keeps one per
 * computer, to answer a player opening the screen without asking the computer; each viewing client keeps one too and
 * draws it.
 * <p>
 * Who changes it (eventual consistency, Baughn 1572606714631561411): the computer, with apply(); and every player's
 * display, which moves, sizes, stacks and activates windows on its own copy at once and reports it (applyPlayer() on
 * the server, which passes the change on to the other displays). The last report wins.
 */
public final class Scene {
    public static final int LOG_LINES = 1000, MAX_WINDOWS = 32, MAX_ELEMENTS = 256, DEFAULT_W = 80, DEFAULT_H = 25;
    /** The text modes (Baughn 2026-11-18: two, booting in the first). */
    public static final int[][] MODES = {{80, 25}, {120, 40}};
    /** The kernel's window (what the machine prints at the console device): the old Console tab. */
    public static final int KERNEL = 0;

    public int w = DEFAULT_W, h = DEFAULT_H;
    /** The screen bar's title. */
    public String title = "";
    /** The pen set's name (Pens; Prefs), and the clock's hours on the menu bar (24 or 12). */
    public String pens = "Grid Blue";
    public int clockHours = 24;
    /** Windows by id, in the order they were first opened. */
    public final Map<Integer, Window> windows = new LinkedHashMap<>();
    /** The stacking order, back to front. */
    public final List<Integer> order = new ArrayList<>();
    /** The active window (the keyboard's), as last set by the computer or reported by a display; -1: none. */
    public int active = -1;
    /** Counts the computer's asks to activate a window (a display takes each new one: the program brought it forward). */
    public long activateSeq;
    /** Pictures by id (pixmap messages), screen-wide; the desk's picture (0: none). */
    public final Map<Integer, Pixmap> pixmaps = new LinkedHashMap<>();
    public int backdrop;
    public static final int MAX_PIXMAPS = 256, PIXMAP_W = 256, PIXMAP_H = 128;
    /** A minimised window's icon, and a desk icon element: a picture of 4 x 3 cells over a label 6 cells wide. */
    public static final int ICON_W = 6, ICON_H = 4;

    /**
     * A picture of w x h dots, each ARGB (0: clear), from (pixmap id w h [palette] "data"): the palette's entries are dye
     * keywords or "#rrggbb", the data one character a dot, "0".."9" then "a".."z" into the palette, "." clear (Baughn
     * 1572655325104832613: icons, Gorillas' gorillas, the desk's picture). version counts changes, for displays' textures.
     */
    public record Pixmap(int w, int h, int[] argb, int version) {
        static int versions;

        static Pixmap of(List<Object> m) {
            int w = (int) Form.num(m, 2, 0), h = (int) Form.num(m, 3, 0);
            if (w < 1 || h < 1 || w > PIXMAP_W || h > PIXMAP_H || !(m.size() > 5 && m.get(4) instanceof List<?> pal)) return null;
            String data = Form.str(m, 5, "");
            if (data.length() != w * h || pal.size() > 36) return null;
            int[] colours = new int[pal.size()];
            for (int i = 0; i < colours.length; i++) {
                Object o = pal.get(i);
                if (o instanceof String hex && hex.matches("#[0-9a-fA-F]{6}")) colours[i] = 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
                else colours[i] = SceneCells.colour(o, 0xFF000000);
            }
            int[] argb = new int[w * h];
            for (int i = 0; i < argb.length; i++) {
                char c = data.charAt(i);
                int k = c >= '0' && c <= '9' ? c - '0' : c >= 'a' && c <= 'z' ? c - 'a' + 10 : -1;
                argb[i] = k >= 0 && k < colours.length ? colours[k] : 0;
            }
            return new Pixmap(w, h, argb, ++versions);
        }
    }

    /**
     * The built-in icon of a minimised window that has none: a small window, its title bar amber (12 x 9 dots).
     */
    public static final Pixmap WINDOW_ICON;

    static {
        String rows = "............" + ".aaaaaaaaaa." + ".abbbbbbbba." + ".abccccccba." + ".abccccccba."
            + ".abccccccba." + ".abccccccba." + ".abbbbbbbba." + "............";
        int[] colours = {0, 0xFFE0A030, 0xFF8A90A0, 0xFF101418};
        int[] argb = new int[rows.length()];
        for (int i = 0; i < argb.length; i++) argb[i] = colours[rows.charAt(i) == '.' ? 0 : rows.charAt(i) - 'a' + 1];
        WINDOW_ICON = new Pixmap(12, 9, argb, 0);
    }
    public static final class Window {
        public String title;
        public int x, y, cw, ch;
        public java.util.Set<String> flags = java.util.Set.of();
        public final Map<Integer, Element> elements = new LinkedHashMap<>();
        public List<String> dict = List.of();
        /** The menus message's list ([["Project" ["Open..." "o"] nil ...] ...]), or empty. */
        public List<Object> menus = List.of();
        /** Greyed menu items, "Menu/Item". */
        public final java.util.Set<String> disabled = new java.util.HashSet<>();
        /** The open requester's message (ask w id "title" "body" [buttons]), or null. */
        public List<Object> ask;
        /** Minimised to an icon on the desk (shared: the last report wins), and that icon's picture (0: the built-in). */
        public boolean minimized;
        public int icon;

        Window(String title) {
            this.title = title;
        }

        public boolean has(String flag) {
            return flags.contains(flag);
        }

        /** Drawn with a frame and a title row (not :borderless or :backdrop). */
        public boolean framed() {
            return !has("borderless") && !has("backdrop");
        }

        /** Its outer rectangle in cells, frame included: x, y, w, h. */
        public int[] outer() {
            return framed() ? new int[]{x - 1, y - 1, cw + 2, ch + 2} : new int[]{x, y, cw, ch};
        }

        public boolean contains(int cx, int cy) {
            int[] o = outer();
            return cx >= o[0] && cy >= o[1] && cx < o[0] + o[2] && cy < o[1] + o[3];
        }
    }

    /** One run of text in one colour. */
    public record Run(String colour, String text) {
    }

    public static final class Element {
        public final String kind;
        public List<Object> msg;
        /** A log's lines, each a list of runs; the last one is still open. */
        public final java.util.ArrayDeque<List<Run>> lines = new java.util.ArrayDeque<>();
        /** A line's busy mark and the submission it is for. */
        public boolean busy;
        public long busySeq;

        Element(String kind, List<Object> msg) {
            this.kind = kind;
            this.msg = msg;
        }

        public int x() {
            return (int) Form.num(msg, 3, 0);
        }

        public int y() {
            return (int) Form.num(msg, 4, 0);
        }

        /** Its column in the window's content: a negative x counts from the right edge (-1: the last column). */
        public int x(Window win) {
            int x = x();
            return x < 0 ? Math.max(0, win.cw + x) : x;
        }

        /** Its row in the window's content: a negative y counts from the bottom (-1: the last row, a Shell's line). */
        public int y(Window win) {
            int y = y();
            return y < 0 ? Math.max(0, win.ch + y) : y;
        }

        /**
         * Its width (k 5) or height (k 6) in the window's content: a number, or :fill / [:fill n] for "to the edge, less
         * n cells" (so a window resized on a display lays itself out there, with no round trip).
         */
        public int size(int k, Window win, int dflt) {
            Object o = msg.size() > k ? msg.get(k) : null;
            int room = k == 5 ? win.cw - x(win) : win.ch - y(win);
            if (o instanceof Form.Keyword kw && kw.name().equals("fill")) return Math.max(0, room);
            if (o instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Form.Keyword kw && kw.name().equals("fill"))
                return Math.max(0, room - (l.size() > 1 && l.get(1) instanceof Long n ? (int) (long) n : 0));
            return o instanceof Long n ? (int) (long) n : dflt;
        }

        void append(String colour, String text) {
            if (lines.isEmpty()) lines.add(new ArrayList<>());
            String[] parts = text.split("\n", -1);
            for (int k = 0; k < parts.length; k++) {
                if (k > 0) {
                    lines.add(new ArrayList<>());
                    while (lines.size() > LOG_LINES) lines.poll();
                }
                if (parts[k].isEmpty()) continue;
                // (a line is at most LINE_MAX characters: one that never ends wraps, rather than growing for ever)
                List<Run> last = lines.peekLast();
                int have = 0;
                for (Run r : last) have += r.text().length();
                String rest = parts[k];
                while (have + rest.length() > LINE_MAX) {
                    int take = LINE_MAX - have;
                    if (take > 0) last.add(new Run(colour, rest.substring(0, take)));
                    rest = rest.substring(Math.max(0, take));
                    lines.add(last = new ArrayList<>());
                    while (lines.size() > LOG_LINES) lines.poll();
                    have = 0;
                }
                if (!rest.isEmpty()) last.add(new Run(colour, rest));
            }
        }
    }

    /** A log line's characters at most. */
    public static final int LINE_MAX = 1000;

    static final java.util.Set<String> ELEMENTS = java.util.Set.of("box", "text", "log", "line", "button", "list", "editor", "raw",
        "image", "icon");
    static final java.util.Set<String> FLAGS = java.util.Set.of("close", "depth", "size", "drag", "iconify", "backdrop", "borderless", "full");

    /** Apply one of the computer's messages; false if it wasn't one (unknown, malformed, out of bounds). */
    public boolean apply(List<Object> m) {
        String v = Form.verb(m);
        switch (v) {
            case "screen", "reset" -> {
                w = (int) Math.max(20, Math.min(200, Form.num(m, 1, DEFAULT_W)));
                h = (int) Math.max(8, Math.min(100, Form.num(m, 2, DEFAULT_H)));
                if (v.equals("reset")) {
                    windows.clear();
                    order.clear();
                    active = -1;
                    title = "";
                    pixmaps.clear();
                    backdrop = 0;
                    pens = "Grid Blue";
                    clockHours = 24;
                } else for (Window win : windows.values()) fit(win);
                return true;
            }
            case "pens" -> {
                pens = Pens.SETS.containsKey(Form.str(m, 1, "")) ? Form.str(m, 1, "") : "Grid Blue";
                return true;
            }
            case "clock" -> {
                clockHours = Form.num(m, 1, 24) == 12 ? 12 : 24;
                return true;
            }
            case "title" -> {
                title = Form.str(m, 1, "");
                return true;
            }
            case "pixmap" -> {
                int id = (int) Form.num(m, 1, -1);
                Pixmap p = id >= 1 && id <= 4095 ? Pixmap.of(m) : null;
                if (p == null || (!pixmaps.containsKey(id) && pixmaps.size() >= MAX_PIXMAPS)) return false;
                pixmaps.put(id, p);
                return true;
            }
            case "unpixmap" -> {
                return pixmaps.remove((int) Form.num(m, 1, -1)) != null;
            }
            case "backdrop" -> {
                backdrop = (int) Math.max(0, Form.num(m, 1, 0));
                return true;
            }
            case "minimize", "restore" -> {
                Window win = windows.get(win(m));
                if (win == null) return false;
                win.minimized = v.equals("minimize");
                if (win.minimized && active == win(m)) active = -1;
                return true;
            }
            case "icon" -> {
                if (m.size() > 3) break;   // (an icon element: (icon w id x y "label" pixmap))
                Window win = windows.get(win(m));
                if (win == null) return false;
                win.icon = (int) Math.max(0, Form.num(m, 2, 0));
                return true;
            }
            case "window" -> {
                int id = win(m);
                if (id < 0) return false;
                Window win = windows.get(id);
                java.util.Set<String> flags = flags(m.size() > 7 ? m.get(7) : null);
                if (win == null) {
                    if (windows.size() >= MAX_WINDOWS) return false;
                    win = new Window(Form.str(m, 6, ""));
                    win.flags = flags;
                    win.x = (int) Form.num(m, 2, 1);
                    win.y = (int) Form.num(m, 3, 2);
                    win.cw = (int) Form.num(m, 4, 20);
                    win.ch = (int) Form.num(m, 5, 8);
                    windows.put(id, win);
                    if (win.has("backdrop")) order.add(0, id);
                    else order.add(id);
                } else {
                    // (open already: title and flags only; its place belongs to the players now)
                    win.title = Form.str(m, 6, win.title);
                    win.flags = flags;
                    if (win.has("backdrop")) {   // (turned into the desk: behind everything)
                        order.remove((Integer) id);
                        order.add(0, id);
                    }
                }
                fit(win);
                return true;
            }
            case "place" -> {
                Window win = windows.get(win(m));
                if (win == null) return false;
                win.x = (int) Form.num(m, 2, win.x);
                win.y = (int) Form.num(m, 3, win.y);
                win.cw = (int) Form.num(m, 4, win.cw);
                win.ch = (int) Form.num(m, 5, win.ch);
                fit(win);
                return true;
            }
            case "unwindow" -> {
                int id = win(m);
                if (windows.remove(id) == null) return false;
                order.remove((Integer) id);
                if (active == id) active = -1;
                return true;
            }
            case "front", "back" -> {
                int id = win(m);
                if (!windows.containsKey(id)) return false;
                if (v.equals("front")) raise(id);
                else lower(id);
                return true;
            }
            case "activate" -> {
                int id = win(m);
                if (!windows.containsKey(id)) return false;
                active = id;
                activateSeq++;
                raise(id);
                return true;
            }
            case "menus" -> {
                Window win = windows.get(win(m));
                if (win == null) return false;
                win.menus = m.size() > 2 && m.get(2) instanceof List<?> l ? new ArrayList<>(l) : List.of();
                win.disabled.clear();
                return true;
            }
            case "enable" -> {
                Window win = windows.get(win(m));
                if (win == null) return false;
                String key = Form.str(m, 2, "") + "/" + Form.str(m, 3, "");
                if (m.size() > 4 && Boolean.FALSE.equals(m.get(4))) win.disabled.add(key);
                else win.disabled.remove(key);
                return true;
            }
            case "ask" -> {
                Window win = windows.get(win(m));
                if (win == null) return false;
                win.ask = m;
                return true;
            }
            case "unask" -> {
                Window win = windows.get(win(m));
                if (win == null) return false;
                win.ask = null;
                return true;
            }
            case "clear" -> {
                Window win = windows.get(win(m));
                if (win == null) return false;
                win.elements.clear();
                return true;
            }
            case "remove" -> {
                Window win = windows.get(win(m));
                return win != null && win.elements.remove((int) Form.num(m, 2, -1)) != null;
            }
            case "append" -> {
                Element e = element(m);
                if (e == null || !e.kind.equals("log")) return false;
                e.append(Form.kwName(m, 3, "white"), Form.str(m, 4, ""));
                return true;
            }
            case "busy" -> {
                Element e = element(m);
                if (e == null || !e.kind.equals("line")) return false;
                e.busySeq = Form.num(m, 3, 0);
                e.busy = m.size() > 4 && Boolean.TRUE.equals(m.get(4));
                return true;
            }
            case "dict" -> {
                int t = win(m);
                if (t < 0) return false;
                List<String> names = new ArrayList<>();
                if (m.size() > 2 && m.get(2) instanceof List<?> l)
                    for (Object o : l) if (o instanceof String s && names.size() < 4096) names.add(s);
                if (t == 0) baseDict = names;   // (window 0's names are every window's)
                else if (windows.containsKey(t)) windows.get(t).dict = names;
                else return false;
                return true;
            }
            case "doc", "saved", "conflict" -> {
                return windows.containsKey(win(m));   // passed on to clients, not kept
            }
            default -> {
                return element(v, m);
            }
        }
        return element(v, m);   // (an icon element)
    }

    /** An element message (box, text, ..., image, icon): made, or changed in place. */
    boolean element(String v, List<Object> m) {
        if (!ELEMENTS.contains(v)) return false;
        Window win = windows.get(win(m));
        int id = (int) Form.num(m, 2, -1);
        if (win == null || id < 0 || id > 65535) return false;
        Element old = win.elements.get(id);
        if (old != null && old.kind.equals(v)) {
            old.msg = m;   // an update keeps its place and, for a log, its lines
            return true;
        }
        if (old == null && win.elements.size() >= MAX_ELEMENTS) return false;
        win.elements.remove(id);
        win.elements.put(id, new Element(v, m));
        return true;
    }

    /**
     * A display's report of what its player did to the windows (moved, raised, lowered, active), applied here. Returns
     * the message that tells the other displays (place, front, back, activate), or null if it was none of these or
     * named no window. The computer is told the report itself; it isn't asked.
     */
    public List<Object> applyPlayer(List<Object> m) {
        String v = Form.verb(m);
        int id = win(m);
        Window win = windows.get(id);
        if (win == null) return null;
        switch (v) {
            case "moved" -> {
                win.x = (int) Form.num(m, 2, win.x);
                win.y = (int) Form.num(m, 3, win.y);
                win.cw = (int) Form.num(m, 4, win.cw);
                win.ch = (int) Form.num(m, 5, win.ch);
                fit(win);
                return List.of(new Form.Symbol("place"), (long) id, (long) win.x, (long) win.y, (long) win.cw, (long) win.ch);
            }
            case "raised" -> {
                raise(id);
                return List.of(new Form.Symbol("front"), (long) id);
            }
            case "lowered" -> {
                lower(id);
                return List.of(new Form.Symbol("back"), (long) id);
            }
            case "active" -> {
                active = id;
                raise(id);
                return List.of(new Form.Symbol("activate"), (long) id);
            }
            case "minimized", "restored" -> {
                win.minimized = v.equals("minimized");
                if (win.minimized && active == id) active = -1;
                return List.of(new Form.Symbol(win.minimized ? "minimize" : "restore"), (long) id);
            }
            default -> {
                return null;
            }
        }
    }

    /** Window id on top at cell (x, y), or -1 (the desk or the screen bar). */
    public int windowAt(int x, int y) {
        for (int k = order.size() - 1; k >= 0; k--) {
            Window win = windows.get(order.get(k));
            if (win != null && !win.minimized && win.contains(x, y)) return order.get(k);
        }
        return -1;
    }

    /** The minimised windows, in the order their icons sit along the desk's bottom row (by id: alike on every display). */
    public List<Integer> minimizedOrder() {
        List<Integer> out = new ArrayList<>();
        for (var en : windows.entrySet()) if (en.getValue().minimized) out.add(en.getKey());
        out.sort(null);
        return out;
    }

    /** Where minimised window wid's icon sits: its top left cell, or null if it isn't minimised. */
    public int[] iconSlot(int wid) {
        int k = minimizedOrder().indexOf(wid);
        return k < 0 ? null : new int[]{2 + (ICON_W + 3) * k, h - ICON_H};
    }

    /** The minimised window whose icon covers cell (x, y), or -1. */
    public int minimizedAt(int x, int y) {
        for (int wid : minimizedOrder()) {
            int[] at = iconSlot(wid);
            if (x >= at[0] && x < at[0] + ICON_W && y >= at[1] && y < at[1] + ICON_H) return wid;
        }
        return -1;
    }

    /** Puts window id at the front of the stack (a backdrop stays at the back). */
    public void raise(int id) {
        Window win = windows.get(id);
        if (win == null || win.has("backdrop")) return;
        order.remove((Integer) id);
        order.add(id);
    }

    /** Puts window id behind the other windows (but in front of the backdrops). */
    public void lower(int id) {
        Window win = windows.get(id);
        if (win == null || win.has("backdrop")) return;
        order.remove((Integer) id);
        int k = 0;
        while (k < order.size() && windows.get(order.get(k)) != null && windows.get(order.get(k)).has("backdrop")) k++;
        order.add(k, id);
    }

    /**
     * Keeps a window on the screen, under the screen bar: :full fills rows 1 to h-1 (inside its frame, if it has one),
     * a backdrop the same without a frame; else its frame is kept inside the screen and its size at least 4 x 1.
     */
    public void fit(Window win) {
        int f = win.framed() ? 1 : 0;
        if (win.has("full") || win.has("backdrop")) {
            win.x = f;
            win.y = 1 + f;
            win.cw = w - 2 * f;
            win.ch = h - 1 - 2 * f;
            return;
        }
        win.cw = Math.max(4, Math.min(win.cw, w - 2 * f));
        win.ch = Math.max(1, Math.min(win.ch, h - 1 - 2 * f));
        win.x = Math.max(f, Math.min(win.x, w - f - win.cw));
        win.y = Math.max(1 + f, Math.min(win.y, h - f - win.ch));
    }

    /** The names to complete in every window (sent as window 0's). */
    public List<String> baseDict = List.of();

    static int win(List<Object> m) {
        long t = Form.num(m, 1, -1);
        return t >= 0 && t <= 255 ? (int) t : -1;
    }

    static java.util.Set<String> flags(Object o) {
        java.util.Set<String> out = new java.util.HashSet<>();
        if (o instanceof List<?> l)
            for (Object x : l) if (x instanceof Form.Keyword k && FLAGS.contains(k.name())) out.add(k.name());
        return java.util.Set.copyOf(out);
    }

    Element element(List<Object> m) {
        Window win = windows.get(win(m));
        return win == null ? null : win.elements.get((int) Form.num(m, 2, -1));
    }

    /** A pixmap as the message that makes it (its palette rebuilt from its colours). */
    static String pixmapMessage(int id, Pixmap p) {
        List<Integer> colours = new ArrayList<>();
        StringBuilder data = new StringBuilder(p.argb().length);
        for (int c : p.argb()) {
            if (c == 0) {
                data.append('.');
                continue;
            }
            int k = colours.indexOf(c);
            if (k < 0) {
                k = colours.size();
                colours.add(c);
            }
            data.append(k < 10 ? (char) ('0' + k) : (char) ('a' + k - 10));
        }
        List<Object> pal = new ArrayList<>();
        for (int c : colours) pal.add(String.format("#%06x", c & 0xFFFFFF));
        return Form.print(new Form.Symbol("pixmap"), (long) id, (long) p.w(), (long) p.h(), pal, data.toString());
    }

    /** Is this message one that only replaces what an earlier one of the same (verb, window, id) said (coalescable)? */
    public static boolean replaces(List<Object> m) {
        String v = Form.verb(m);
        return v.equals("text") || v.equals("box") || v.equals("busy") || v.equals("button");
    }

    /** The messages that rebuild this scene on an empty one, after a (reset w h). */
    /**
     * A save landed ((saved w id seq rev), with the hunks the operator sent): the editor element keeps the text as saved
     * and its new revision, so whoever opens the screen next edits the file as it is, not as it was first opened. The
     * computer needn't send the whole text again (a 32 KB message on a two-chip computer). Hunks that don't apply leave it.
     */
    public void saved(List<Object> m, List<?> hunks) {
        Window w = windows.get((int) Form.num(m, 1, -1));
        Element e = w == null ? null : w.elements.get((int) Form.num(m, 2, -1));
        if (e == null || !e.kind.equals("editor") || e.msg.size() < 11) return;
        String[] lines = Form.str(e.msg, 10, "").split("\n", -1);
        List<String> out = new ArrayList<>();
        int at = 0;
        for (Object o : hunks) {
            if (!(o instanceof List<?> hk) || hk.size() < 3 || !(hk.get(0) instanceof Long start) || !(hk.get(1) instanceof Long del)
                || !(hk.get(2) instanceof List<?> ins) || start < at || del < 0 || start + del > lines.length) return;
            for (int k = at; k < start; k++) out.add(lines[k]);
            for (Object l : ins) out.add(String.valueOf(l));
            at = (int) (start + del);
        }
        for (int k = at; k < lines.length; k++) out.add(lines[k]);
        List<Object> msg = new ArrayList<>(e.msg);
        msg.set(9, Form.str(m, 4, ""));
        msg.set(10, String.join("\n", out));
        e.msg = msg;
    }

    public List<String> replay() {
        List<String> out = new ArrayList<>();
        out.add(Form.print(new Form.Symbol("reset"), (long) w, (long) h));
        if (!title.isEmpty()) out.add(Form.print(new Form.Symbol("title"), title));
        if (!pens.equals("Grid Blue")) out.add(Form.print(new Form.Symbol("pens"), pens));
        if (clockHours != 24) out.add(Form.print(new Form.Symbol("clock"), (long) clockHours));
        for (var pe : pixmaps.entrySet()) out.add(pixmapMessage(pe.getKey(), pe.getValue()));
        if (backdrop != 0) out.add(Form.print(new Form.Symbol("backdrop"), (long) backdrop));
        if (!baseDict.isEmpty()) out.add(Form.print(new Form.Symbol("dict"), 0L, baseDict));
        for (Map.Entry<Integer, Window> we : windows.entrySet()) {
            long t = we.getKey();
            Window win = we.getValue();
            List<Object> fl = new ArrayList<>();
            for (String f : win.flags) fl.add(Form.kw(f));
            out.add(Form.print(new Form.Symbol("window"), t, (long) win.x, (long) win.y, (long) win.cw, (long) win.ch, win.title, fl));
            if (!win.menus.isEmpty()) out.add(Form.print(new Form.Symbol("menus"), t, win.menus));
            if (win.icon != 0) out.add(Form.print(new Form.Symbol("icon"), t, (long) win.icon));
            if (win.minimized) out.add(Form.print(new Form.Symbol("minimize"), t));
            for (String d : win.disabled) {
                String[] p = d.split("/", 2);
                out.add(Form.print(new Form.Symbol("enable"), t, p[0], p.length > 1 ? p[1] : "", false));
            }
            if (!win.dict.isEmpty()) out.add(Form.print(new Form.Symbol("dict"), t, win.dict));
            for (Map.Entry<Integer, Element> ee : win.elements.entrySet()) {
                Element e = ee.getValue();
                out.add(Form.print(e.msg));
                long id = ee.getKey();
                if (e.kind.equals("log")) {
                    // the lines, one append per run of a colour (and per 8 K)
                    StringBuilder b = new StringBuilder();
                    String colour = null;
                    boolean first = true;
                    for (List<Run> line : e.lines) {
                        if (!first) b.append('\n');
                        first = false;
                        for (Run r : line) {
                            if (colour != null && !colour.equals(r.colour()) && b.length() > 0) {
                                out.add(Form.print(new Form.Symbol("append"), t, id, Form.kw(colour), b.toString()));
                                b.setLength(0);
                            }
                            colour = r.colour();
                            b.append(r.text());
                            if (b.length() > 8192) {   // (in pieces: a full log as one message passed the line limit)
                                out.add(Form.print(new Form.Symbol("append"), t, id, Form.kw(colour), b.toString()));
                                b.setLength(0);
                            }
                        }
                    }
                    if (b.length() > 0 || colour != null)
                        out.add(Form.print(new Form.Symbol("append"), t, id, Form.kw(colour == null ? "white" : colour), b.toString()));
                }
                if (e.kind.equals("line") && e.busy) out.add(Form.print(new Form.Symbol("busy"), t, id, e.busySeq, true));
            }
            if (win.ask != null) out.add(Form.print(win.ask));
        }
        for (int id : order) out.add(Form.print(new Form.Symbol("front"), (long) id));   // (back to front)
        if (active >= 0) out.add(Form.print(new Form.Symbol("activate"), (long) active));
        return out;
    }
}
