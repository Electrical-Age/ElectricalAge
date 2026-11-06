package mods.eln.robotics.computer.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a computer's screen shows, as the protocol builds it: tabs of elements, each the last message that made it
 * (drawn in the order elements were first made: Argon's review), logs with their last LOG_LINES lines, lines' busy
 * marks, completion names. The host keeps one per computer, to answer a player opening the screen without asking the
 * computer; each viewing client keeps one too and draws it. apply() takes the computer's messages; replay() gives the
 * messages that rebuild it from nothing.
 */
public final class Scene {
    public static final int LOG_LINES = 1000, MAX_TABS = 32, MAX_ELEMENTS = 256, DEFAULT_W = 52, DEFAULT_H = 17;

    public int w = DEFAULT_W, h = DEFAULT_H;
    public final Map<Integer, Tab> tabs = new LinkedHashMap<>();
    /** The tab the program last asked to bring forward (-1: none), and a count of the asks (the same tab again is news). */
    public int focus = -1;
    public long focusSeq;

    public static final class Tab {
        public String title;
        public final Map<Integer, Element> elements = new LinkedHashMap<>();
        public List<String> dict = List.of();

        Tab(String title) {
            this.title = title;
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

        void append(String colour, String text) {
            if (lines.isEmpty()) lines.add(new ArrayList<>());
            String[] parts = text.split("\n", -1);
            for (int k = 0; k < parts.length; k++) {
                if (k > 0) {
                    lines.add(new ArrayList<>());
                    while (lines.size() > LOG_LINES) lines.poll();
                }
                if (!parts[k].isEmpty()) lines.peekLast().add(new Run(colour, parts[k]));
            }
        }
    }

    static final java.util.Set<String> ELEMENTS = java.util.Set.of("box", "text", "log", "line", "button", "list", "editor", "raw");

    /** Apply one of the computer's messages; false if it wasn't one (unknown, malformed, out of bounds). */
    public boolean apply(List<Object> m) {
        String v = Form.verb(m);
        switch (v) {
            case "screen", "reset" -> {
                w = (int) Math.max(8, Math.min(200, Form.num(m, 1, DEFAULT_W)));
                h = (int) Math.max(4, Math.min(100, Form.num(m, 2, DEFAULT_H)));
                if (v.equals("reset")) {
                    tabs.clear();
                    focus = -1;
                }
                return true;
            }
            case "tab" -> {
                int t = tab(m);
                if (t < 0) return false;
                Tab tab = tabs.get(t);
                if (tab == null) {
                    if (tabs.size() >= MAX_TABS) return false;
                    tabs.put(t, new Tab(Form.str(m, 2, "")));
                } else tab.title = Form.str(m, 2, tab.title);
                return true;
            }
            case "untab" -> {
                return tabs.remove(tab(m)) != null;
            }
            case "focus" -> {
                int t = tab(m);
                if (!tabs.containsKey(t)) return false;
                focus = t;
                focusSeq++;
                return true;
            }
            case "clear" -> {
                Tab tab = tabs.get(tab(m));
                if (tab == null) return false;
                tab.elements.clear();
                return true;
            }
            case "remove" -> {
                Tab tab = tabs.get(tab(m));
                return tab != null && tab.elements.remove((int) Form.num(m, 2, -1)) != null;
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
                int t = tab(m);
                if (t != 0 && !tabs.containsKey(t)) return false;
                List<String> names = new ArrayList<>();
                if (m.size() > 2 && m.get(2) instanceof List<?> l)
                    for (Object o : l) if (o instanceof String s && names.size() < 4096) names.add(s);
                if (t == 0) baseDict = names;
                else tabs.get(t).dict = names;
                return true;
            }
            case "doc", "saved", "conflict" -> {
                return tabs.containsKey(tab(m));   // passed on to clients, not kept
            }
            default -> {
                if (!ELEMENTS.contains(v)) return false;
                Tab tab = tabs.get(tab(m));
                int id = (int) Form.num(m, 2, -1);
                if (tab == null || id < 0 || id > 65535) return false;
                Element old = tab.elements.get(id);
                if (old != null && old.kind.equals(v)) {
                    old.msg = m;   // an update keeps its place and, for a log, its lines
                    return true;
                }
                if (old == null && tab.elements.size() >= MAX_ELEMENTS) return false;
                tab.elements.remove(id);
                tab.elements.put(id, new Element(v, m));
                return true;
            }
        }
    }

    /** The names to complete in every tab (sent as tab 0's). */
    public List<String> baseDict = List.of();

    static int tab(List<Object> m) {
        long t = Form.num(m, 1, -1);
        return t >= 0 && t <= 255 ? (int) t : -1;
    }

    Element element(List<Object> m) {
        Tab tab = tabs.get(tab(m));
        return tab == null ? null : tab.elements.get((int) Form.num(m, 2, -1));
    }

    /** Is this message one that only replaces what an earlier one of the same (verb, tab, id) said (coalescable)? */
    public static boolean replaces(List<Object> m) {
        String v = Form.verb(m);
        return v.equals("text") || v.equals("box") || v.equals("busy") || v.equals("button");
    }

    /** The messages that rebuild this scene on an empty one, after a (reset w h). */
    public List<String> replay() {
        List<String> out = new ArrayList<>();
        out.add(Form.print(new Form.Symbol("reset"), (long) w, (long) h));
        if (!baseDict.isEmpty()) out.add(Form.print(new Form.Symbol("dict"), 0L, baseDict));
        for (Map.Entry<Integer, Tab> te : tabs.entrySet()) {
            long t = te.getKey();
            Tab tab = te.getValue();
            out.add(Form.print(new Form.Symbol("tab"), t, tab.title));
            if (!tab.dict.isEmpty()) out.add(Form.print(new Form.Symbol("dict"), t, tab.dict));
            for (Map.Entry<Integer, Element> ee : tab.elements.entrySet()) {
                Element e = ee.getValue();
                out.add(Form.print(e.msg));
                long id = ee.getKey();
                if (e.kind.equals("log")) {
                    // the lines, one append per run of a colour
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
                        }
                    }
                    if (b.length() > 0 || colour != null)
                        out.add(Form.print(new Form.Symbol("append"), t, id, Form.kw(colour == null ? "white" : colour), b.toString()));
                }
                if (e.kind.equals("line") && e.busy) out.add(Form.print(new Form.Symbol("busy"), t, id, e.busySeq, true));
            }
        }
        if (focus >= 0) out.add(Form.print(new Form.Symbol("focus"), (long) focus));
        return out;
    }
}
