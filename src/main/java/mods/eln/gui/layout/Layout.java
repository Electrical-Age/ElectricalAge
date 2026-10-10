package mods.eln.gui.layout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EA's screen layout (P4b): rows and columns of self-sizing leaves, so nothing on a screen is placed by hand and
 * nothing can overlap. Pure Java (no client classes): the client measures text with its font, tests with a fixed width
 * per character. A screen's tree comes from assets/eln/gui/&lt;name&gt;.json:
 * <pre>
 * {"column": [ ... ], "gap": 4}                    a column (children top to bottom), or "row" (left to right)
 * {"slot": "in", "big": true}                      an item slot (18x18; big: 26x26), id = the slot's name
 * {"text": "status", "sample": ["..", ".."]}       a text line, as wide as its widest sample
 * {"button": "power", "sample": ["On", "Off"]}     a button that fits every label
 * {"slider": "target", "sample": ["Target 800°C"]} a slider (20 high)
 * {"field": "channel", "chars": 20}                a text box for so many characters
 * {"gauge": "supply", "width": 60, "height": 12}   a bar
 * {"progress": "process"}                          the furnace arrow (24x16)
 * {"inventory": true}                              the player inventory (162x76)
 * {"canvas": "grid", "width": 80, "height": 80}    an area the screen draws itself (the hub's connection grid)
 * {"space": 4} / {"space": [w, h]}                 empty room
 * </pre>
 * Any node can say "fill": true (takes the spare width of its row; in a column it spans the column's width) and
 * "width"/"height" (a minimum). Rows centre their children vertically; columns align them left ("align": "center").
 */
public final class Layout {
    private Layout() {
    }

    /** Text metrics: the client's font, or a fixed width per character in tests. */
    public interface Measure {
        int width(String text);

        default int lineHeight() {
            return 9;
        }
    }

    /** A fixed-width measure (6 px a character, the default font's widest common glyphs plus spacing). */
    public static final Measure FIXED = s -> s.length() * 6;

    public record Rect(int x, int y, int w, int h) {
        public boolean overlaps(Rect o) {
            return x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h;
        }

        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }
    }

    /** A placed leaf: kind ("slot", "button"...), id, rectangle. */
    public record Placed(String kind, String id, Rect rect) {
        public String key() {
            return kind + ":" + id;
        }
    }

    public record Result(int width, int height, List<Placed> leaves) {
        public Rect get(String kind, String id) {
            for (Placed p : leaves) if (p.kind.equals(kind) && p.id.equals(id)) return p.rect;
            return null;
        }

        public Map<String, Rect> byKey() {
            Map<String, Rect> m = new LinkedHashMap<>();
            for (Placed p : leaves) m.put(p.key(), p.rect);
            return m;
        }

        /** Pairs of leaves that overlap, and leaves outside the panel; empty when the layout is sound. */
        public List<String> problems() {
            List<String> out = new ArrayList<>();
            for (int i = 0; i < leaves.size(); i++) {
                Placed a = leaves.get(i);
                if (a.rect.x < 0 || a.rect.y < 0 || a.rect.right() > width || a.rect.bottom() > height)
                    out.add(a.key() + " outside the panel " + width + "x" + height + ": " + a.rect);
                for (int j = i + 1; j < leaves.size(); j++) {
                    Placed b = leaves.get(j);
                    if (a.rect.overlaps(b.rect)) out.add(a.key() + " overlaps " + b.key());
                }
            }
            return out;
        }
    }

    // ---- the tree
    static final class Node {
        final JsonObject json;
        final String kind, id;
        final boolean row, fill, center;
        final int gap, minW, minH;
        final List<Node> children = new ArrayList<>();
        int w, h; // measured (preferred) size

        Node(JsonObject o) {
            json = o;
            fill = o.has("fill") && o.get("fill").getAsBoolean();
            minW = o.has("width") ? o.get("width").getAsInt() : 0;
            minH = o.has("height") ? o.get("height").getAsInt() : 0;
            center = o.has("align") && o.get("align").getAsString().equals("center");
            if (o.has("row") || o.has("column")) {
                row = o.has("row");
                kind = row ? "row" : "column";
                id = "";
                gap = o.has("gap") ? o.get("gap").getAsInt() : 4;
                for (JsonElement c : o.getAsJsonArray(row ? "row" : "column")) children.add(new Node(c.getAsJsonObject()));
            } else {
                row = false;
                gap = 0;
                String k = null;
                for (String cand : new String[]{"slot", "text", "button", "slider", "field", "gauge", "progress", "inventory", "canvas", "space"})
                    if (o.has(cand)) {
                        k = cand;
                        break;
                    }
                if (k == null) throw new IllegalArgumentException("layout node without a kind: " + o);
                kind = k;
                JsonElement v = o.get(k);
                id = v.isJsonPrimitive() && v.getAsJsonPrimitive().isString() ? v.getAsString() : "";
            }
        }

        List<String> samples() {
            List<String> s = new ArrayList<>();
            if (json.has("sample")) {
                JsonElement e = json.get("sample");
                if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) s.add(x.getAsString());
                else s.add(e.getAsString());
            }
            return s;
        }

        int widest(Measure m) {
            int w = 0;
            for (String s : samples()) w = Math.max(w, m.width(s));
            return w;
        }

        void measure(Measure m) {
            switch (kind) {
                case "row", "column" -> {
                    int a = 0, b = 0;
                    for (Node c : children) {
                        c.measure(m);
                        if (row) {
                            a += c.w;
                            b = Math.max(b, c.h);
                        } else {
                            a = Math.max(a, c.w);
                            b += c.h;
                        }
                    }
                    int gaps = Math.max(0, children.size() - 1) * gap;
                    w = row ? a + gaps : a;
                    h = row ? b : b + gaps;
                }
                case "slot" -> {
                    boolean big = json.has("big") && json.get("big").getAsBoolean();
                    w = h = big ? 26 : 18;
                }
                case "text" -> {
                    w = widest(m);
                    h = m.lineHeight();
                }
                case "button" -> {
                    w = Math.max(20, widest(m) + 10);
                    h = 20;
                }
                case "slider" -> {
                    w = widest(m) + 20;
                    h = 20;
                }
                case "field" -> {
                    int chars = json.has("chars") ? json.get("chars").getAsInt() : 10;
                    w = Math.max(widest(m), chars * m.width("0")) + 10;
                    h = 18;
                }
                case "gauge" -> {
                    w = Math.max(widest(m) + 6, 20);
                    h = 12;
                }
                case "progress" -> {
                    w = 24;
                    h = 16;
                }
                case "inventory" -> {
                    w = 162;
                    h = 76;
                }
                case "canvas" -> {
                    w = 0; // its "width"/"height"
                    h = 0;
                }
                case "space" -> {
                    JsonElement v = json.get("space");
                    if (v.isJsonArray()) {
                        JsonArray a = v.getAsJsonArray();
                        w = a.get(0).getAsInt();
                        h = a.get(1).getAsInt();
                    } else w = h = v.getAsInt();
                }
                default -> throw new IllegalStateException(kind);
            }
            w = Math.max(w, minW);
            h = Math.max(h, minH);
        }

        /** Place at (x, y) with the given width (columns and filled leaves stretch to it). */
        void place(int x, int y, int width, List<Placed> out) {
            switch (kind) {
                case "row" -> {
                    int spare = Math.max(0, width - w);
                    int fills = 0;
                    for (Node c : children) if (c.fill) fills++;
                    int cx = x, given = 0;
                    for (Node c : children) {
                        int cw = c.w;
                        if (c.fill && fills > 0) {
                            int extra = spare / fills;
                            if (++given == fills) extra = spare - extra * (fills - 1);
                            cw += extra;
                        }
                        c.place(cx, y + (h - c.h) / 2, cw, out);
                        cx += cw + gap;
                    }
                }
                case "column" -> {
                    int cy = y;
                    for (Node c : children) {
                        int cw = c.fill ? width : c.w;
                        int cx = c.fill || !center ? x : x + (width - c.w) / 2;
                        c.place(cx, cy, cw, out);
                        cy += c.h + gap;
                    }
                }
                default -> {
                    if (!kind.equals("space")) out.add(new Placed(kind, id, new Rect(x, y, fill ? width : w, h)));
                }
            }
        }
    }

    public static final int PAD = 7, MIN_WIDTH = 176;

    /** Lay out a screen: the root node inside a panel padded by 7, at least 176 wide. */
    public static Result layout(JsonObject root, Measure m) {
        Node n = new Node(root);
        n.measure(m);
        int width = Math.max(MIN_WIDTH, n.w + 2 * PAD);
        int inner = width - 2 * PAD;
        List<Placed> out = new ArrayList<>();
        n.place(PAD, PAD, inner, out);
        int height = n.h + 2 * PAD;
        return new Result(width, height, out);
    }
}
