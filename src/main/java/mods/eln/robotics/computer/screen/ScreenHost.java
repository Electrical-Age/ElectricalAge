package mods.eln.robotics.computer.screen;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The server's side of a computer's screen (Argon's review: the host keeps the scene). It reads the display channel's
 * messages into a Scene, turns console bytes into the Kernel window (window 0), and batches what to send to the players
 * watching: per tick, coalesced (a text or box redrawn many times in a tick goes once: Mosh's idea), or the whole
 * scene again when a batch would be too big. It also checks what clients send before it reaches the computer. Its
 * methods are synchronized: the computer's output comes from the pool, events from the network thread.
 */
public final class ScreenHost {
    /** A tick's batch at most; past it, viewers get the scene again instead. */
    public static final int BATCH_MAX = 16 * 1024;
    /** The longest event a client may send: a save; the others are short. */
    public static final int MAX_SAVE = 40 * 1024, MAX_SUBMIT = 8192, MAX_EVENT = 256;
    static final Set<String> EVENTS = Set.of("submit", "press", "select", "save", "stash", "key", "click", "break",
        "moved", "raised", "lowered", "active", "closed", "pick", "answer", "open", "minimized", "restored", "keyup");
    /** The kernel's window (what the machine prints at the console device) and its log. */
    public static final int CONSOLE_TAB = Scene.KERNEL, CONSOLE_LOG = 1;

    public final Scene scene = new Scene();
    /** The display channel's bytes after its last newline (a message split between slices, or a UTF-8 character). */
    private final java.io.ByteArrayOutputStream partial = new java.io.ByteArrayOutputStream();
    /** The console's undecoded tail (a UTF-8 character split between slices). */
    private byte[] consoleTail = new byte[0];
    private final List<List<Object>> batch = new ArrayList<>();
    /** Counts the scene's changes (the watchers' cells are composed again when it moved). */
    public volatile long version;
    private int batchBytes;
    private boolean overflow;
    /** Saves on their way to the computer (window/id/seq -> hunks), oldest first. */
    private final java.util.LinkedHashMap<String, Object> saves = new java.util.LinkedHashMap<>();
    /** Answers to the operator's saves (saved, conflict): theirs alone, never kept in the scene. */
    private final List<String> replies = new ArrayList<>();

    /** Bytes from the computer's display channel: whole lines become messages. */
    public synchronized void fromComputer(byte[] display) {
        if (display.length == 0) return;
        // (split on newline bytes, then decode each whole line: a character split between slices stays whole, and
        // the buffer isn't shifted once a line)
        int from = 0;
        for (int k = 0; k < display.length; k++) {
            if (display[k] == mods.eln.robotics.computer.hw.Console.CANCEL) {   // (the message so far was cut short: dropped)
                partial.reset();
                from = k + 1;
                continue;
            }
            if (display[k] != '\n') continue;
            byte[] line;
            if (partial.size() > 0) {
                partial.write(display, from, k - from);
                line = partial.toByteArray();
                partial.reset();
            } else line = java.util.Arrays.copyOfRange(display, from, k);
            from = k + 1;
            take(Form.parse(new String(line, StandardCharsets.UTF_8)));
        }
        partial.write(display, from, display.length - from);
        if (partial.size() > 4 * Form.MAX_LINE) partial.reset();   // a line that never ends: dropped
    }

    /** Bytes from the console (boot messages, traps, eprint): the Kernel window, opened when it first has something. */
    public synchronized void console(byte[] bytes) {
        if (bytes.length == 0) return;
        if (!scene.windows.containsKey(CONSOLE_TAB)) {
            List<Object> flags = List.of(Form.kw("iconify"), Form.kw("depth"), Form.kw("size"), Form.kw("drag"));
            take(List.of(new Form.Symbol("window"), (long) CONSOLE_TAB, 2L, 3L, (long) Math.min(76, scene.w - 4), 12L, "Kernel", flags));
            take(List.of(new Form.Symbol("log"), (long) CONSOLE_TAB, (long) CONSOLE_LOG, 0L, 0L, Form.kw("fill"), Form.kw("fill")));
            // (it waits as an icon on the desk: the boot banner shouldn't cover it; the system menu brings it back)
            take(List.of(new Form.Symbol("minimize"), (long) CONSOLE_TAB));
        }
        // (up to the last whole UTF-8 character; the rest waits for the next slice; a dropped byte's CANCEL isn't text)
        byte[] all = consoleTail.length == 0 ? bytes : concat(consoleTail, bytes);
        int kept = 0;
        for (byte x : all) if (x != mods.eln.robotics.computer.hw.Console.CANCEL) all[kept++] = x;
        if (kept < all.length) all = java.util.Arrays.copyOf(all, kept);
        int end = all.length, back = 0;
        while (back < 3 && end - back - 1 >= 0 && (all[end - back - 1] & 0xC0) == 0x80) back++;
        int lead = end - back - 1;
        if (lead >= 0 && (all[lead] & 0x80) != 0) {
            int need = (all[lead] & 0xE0) == 0xC0 ? 2 : (all[lead] & 0xF0) == 0xE0 ? 3 : (all[lead] & 0xF8) == 0xF0 ? 4 : 1;
            if (back + 1 < need) end = lead;
        }
        consoleTail = java.util.Arrays.copyOfRange(all, end, all.length);
        if (end == 0) return;
        take(List.of(new Form.Symbol("append"), (long) CONSOLE_TAB, (long) CONSOLE_LOG, Form.kw("white"),
            new String(all, 0, end, StandardCharsets.UTF_8)));
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] c = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    /** The computer restarted: everyone's screen empties (a half-sent line is forgotten). */
    public synchronized void reset() {
        partial.reset();
        consoleTail = new byte[0];
        take(List.of(new Form.Symbol("reset"), (long) Scene.DEFAULT_W, (long) Scene.DEFAULT_H));
    }

    /**
     * Wire 2's hello, the first thing a program reads from the screen after every reset: the protocol's version, the
     * text modes, the pen sets, the limits and the key names, so a program (Janet's library, or a system of your own
     * from the Floppy Boot ROM) needn't guess them.
     */
    public static String hello() {
        return Form.print(new Form.Symbol("wire"), 2L, Form.kw("modes"), List.of(List.of(80L, 25L), List.of(120L, 40L)),
            Form.kw("pens"), List.copyOf(Pens.SETS.keySet()),
            Form.kw("max-message"), (long) Form.MAX_LINE, Form.kw("max-event"), (long) MAX_EVENT, Form.kw("max-submit"), (long) MAX_SUBMIT,
            Form.kw("max-save"), (long) MAX_SAVE, Form.kw("max-windows"), (long) Scene.MAX_WINDOWS, Form.kw("max-elements"), (long) Scene.MAX_ELEMENTS,
            Form.kw("max-pixmaps"), (long) Scene.MAX_PIXMAPS, Form.kw("pixmap"), List.of((long) Scene.PIXMAP_W, (long) Scene.PIXMAP_H),
            Form.kw("keys"), List.of("enter", "backspace", "tab", "delete", "insert", "left", "right", "up", "down", "home", "end",
                "pageup", "pagedown", "f1", "f12"));
    }

    /** Events the screen itself has for the computer (geometry it fitted, refusals), since last asked. */
    public synchronized List<String> takeToComputer() {
        refusals = 0;
        if (toComputer.isEmpty()) return List.of();
        List<String> r = List.copyOf(toComputer);
        toComputer.clear();
        return r;
    }

    private final List<String> toComputer = new ArrayList<>();
    private int refusals;

    private Map<Integer, int[]> geometry() {
        Map<Integer, int[]> g = new HashMap<>();
        for (var e : scene.windows.entrySet()) g.put(e.getKey(), new int[]{e.getValue().x, e.getValue().y, e.getValue().cw, e.getValue().ch});
        return g;
    }

    private static String movedEvent(int id, Scene.Window w) {
        return Form.print(new Form.Symbol("moved"), (long) id, (long) w.x, (long) w.y, (long) w.cw, (long) w.ch);
    }

    /** The answers to saves since last asked (for the operator only). */
    public synchronized List<String> takeReplies() {
        if (replies.isEmpty()) return List.of();
        List<String> r = List.copyOf(replies);
        replies.clear();
        return r;
    }

    /** The highest submission or save number the scene shows (a line's busy mark): where the next operator starts. */
    public synchronized long highestSeq() {
        long h = 0;
        for (Scene.Window w : scene.windows.values())
            for (Scene.Element e : w.elements.values()) h = Math.max(h, e.busySeq);
        return h;
    }

    /**
     * An event names something the screen shows, as the client says it does (Wire review, E13): clients are untrusted,
     * so a pick of a menu item that isn't there or is greyed out, an answer with no requester, a row past a list's
     * end, a press on what isn't a button, or anything under an open requester but its answer, never reaches the program.
     */
    private boolean fits(String v, List<Object> m) {
        Scene.Window w = scene.windows.get((int) Form.num(m, 1, -1));
        if (w == null) return false;
        if (w.ask != null && !v.equals("answer") && !v.equals("moved") && !v.equals("raised") && !v.equals("lowered")
            && !v.equals("active") && !v.equals("break")) return false;
        Scene.Element e = m.size() > 2 && m.get(2) instanceof Long id ? w.elements.get((int) (long) id) : null;
        return switch (v) {
            case "submit" -> e != null && e.kind.equals("line") && m.size() > 4 && m.get(3) instanceof Long && m.get(4) instanceof String;
            case "press" -> e != null && e.kind.equals("button");
            case "select" -> e != null && e.kind.equals("list") && m.size() > 3 && m.get(3) instanceof Long row && row >= 0
                && e.msg.size() > 7 && e.msg.get(7) instanceof List<?> rows && row < rows.size();
            case "save", "stash" -> e != null && e.kind.equals("editor") && m.size() > 5 && m.get(3) instanceof Long && m.get(5) instanceof List<?>;
            case "key", "keyup" -> e != null && e.kind.equals("raw") && m.size() > 3 && m.get(3) instanceof String;
            case "click" -> e != null && e.kind.equals("raw") && m.size() > 4 && m.get(3) instanceof Long && m.get(4) instanceof Long;
            case "open" -> e != null && e.kind.equals("icon");
            case "answer" -> w.ask != null && m.size() > 3 && m.get(3) instanceof Long i && i >= 0
                && w.ask.size() > 5 && w.ask.get(5) instanceof List<?> buttons && i < buttons.size();
            case "pick" -> m.size() > 3 && m.get(2) instanceof String menu && m.get(3) instanceof String item
                && !w.disabled.contains(menu + "/" + item) && hasItem(w.menus, menu, item);
            case "moved" -> m.size() > 5 && m.get(2) instanceof Long && m.get(3) instanceof Long && m.get(4) instanceof Long && m.get(5) instanceof Long;
            default -> true;   // (closed, break, raised, lowered, active, minimized, restored: the window is enough)
        };
    }

    static boolean hasItem(List<Object> menus, String menu, String item) {
        for (Object o : menus)
            if (o instanceof List<?> mn && !mn.isEmpty() && menu.equals(mn.get(0)))
                for (int k = 1; k < mn.size(); k++)
                    if (mn.get(k) instanceof List<?> it && !it.isEmpty() && item.equals(it.get(0))) return true;
        return false;
    }

    private void take(List<Object> m) {
        if (m != null && (Form.verb(m).equals("saved") || Form.verb(m).equals("conflict"))) {
            if (scene.apply(m) && replies.size() < 64) replies.add(Form.print(m));
            Object hunks = saves.remove(Form.num(m, 1, -1) + "/" + Form.num(m, 2, -1) + "/" + Form.num(m, 3, -1));
            if (Form.verb(m).equals("saved") && hunks instanceof List<?> h) scene.saved(m, h);
            return;
        }
        if (m == null) return;
        String verb = Form.verb(m);
        int wid = (int) Form.num(m, 1, -1);
        boolean opening = verb.equals("window") && !scene.windows.containsKey(wid);
        if (opening && m.size() > 8) m = m.subList(0, 8);   // (a generation is the screen's to give)
        Map<Integer, int[]> before = verb.equals("screen") ? geometry() : null;
        if (!scene.apply(m)) {   // (Wire 2: the program hears what the screen refused, a few a batch at most)
            if (refusals++ < 8)
                toComputer.add(Form.print(new Form.Symbol("error"), new Form.Symbol(Form.verb(m)), Form.num(m, 1, -1), Form.num(m, 2, -1),
                    "refused: unknown, malformed, or past a limit (windows, elements, pictures, sizes)"));
            return;
        }
        // (the program hears the geometry the screen fitted a window to, where it differs from what it asked for:
        // Wire review E24; it already takes moved events, which players' moves send)
        if (opening || verb.equals("place")) {
            Scene.Window w = scene.windows.get(wid);
            if (w != null && (w.x != Form.num(m, 2, w.x) || w.y != Form.num(m, 3, w.y) || w.cw != Form.num(m, 4, w.cw) || w.ch != Form.num(m, 5, w.ch)))
                toComputer.add(movedEvent(wid, w));
        } else if (before != null)
            for (var e : geometry().entrySet())
                if (!java.util.Arrays.equals(e.getValue(), before.get(e.getKey()))) toComputer.add(movedEvent(e.getKey(), scene.windows.get(e.getKey())));
        version++;
        if (opening && scene.windows.get(wid) != null) {   // (the displays get its generation: Wire 2)
            List<Object> withGen = new ArrayList<>(m);
            while (withGen.size() < 8) withGen.add(withGen.size() == 6 ? "" : List.of());
            withGen.add(Form.kw("gen"));
            withGen.add((long) scene.windows.get(wid).gen);
            m = withGen;
        }
        batch.add(m);
        batchBytes += 32;   // (a rough size; the exact one is counted when the batch is printed)
        if (batchBytes > BATCH_MAX * 4) {
            batch.clear();
            overflow = true;
        }
    }

    /**
     * What to send every watching player this tick: the batch's lines, coalesced. Null when it was too big: send each
     * the replay() instead. Empty when there is nothing.
     */
    public synchronized List<String> takeBatch() {
        if (overflow) {
            overflow = false;
            batch.clear();
            batchBytes = 0;
            return null;
        }
        // (a message that replaces an element's state replaces it where that element first appeared in the batch, so
        // the order elements are made in, which is their drawing order, stays the computer's; a remove, a clear or a
        // message of another kind for the same element starts afresh after it: Wire review, E9)
        Map<String, Integer> at = new HashMap<>();   // window/id -> its place in out
        Map<String, String> verbAt = new HashMap<>();
        List<String> out = new ArrayList<>();
        for (List<Object> m : batch) {
            String v = Form.verb(m), w = String.valueOf(Form.num(m, 1, -1)), el = w + "/" + Form.num(m, 2, -1);
            String line = Form.print(m);
            if (v.equals("reset") || v.equals("screen")) {
                at.clear();
                verbAt.clear();
            } else if (v.equals("clear") || v.equals("unwindow") || v.equals("window")) {
                at.keySet().removeIf(k -> k.startsWith(w + "/"));
            } else if (v.equals("remove")) at.remove(el);
            if (Scene.replaces(m)) {
                Integer k = at.get(el);
                if (k != null && v.equals(verbAt.get(el))) {
                    out.set(k, line);
                    continue;
                }
                at.put(el, out.size());
                verbAt.put(el, v);
            } else if (!v.equals("reset") && !v.equals("screen")) at.remove(el);
            out.add(line);
        }
        batch.clear();
        batchBytes = 0;
        return out;   // (however big: the element sends it in pieces; a full replay for everyone was worse)
    }

    static String key(List<Object> m) {
        return Form.verb(m) + " " + Form.num(m, 1, -1) + " " + Form.num(m, 2, -1);
    }

    /** The messages that rebuild the screen from nothing, for a player who just opened it. */
    public synchronized List<String> replay() {
        return scene.replay();
    }

    /** Rebuild from saved replay lines (the screen as it was when the world was saved). */
    public synchronized void load(List<String> lines) {
        for (String l : lines) {
            List<Object> m = Form.parse(l);
            if (m != null) scene.apply(m);
        }
    }

    /**
     * A client's event checked (Argon's review: clients are untrusted): the line to pass to the computer, or null to
     * drop it. Its verb must be an event's, its tab and id numbers, and it must fit the size limits.
     */
    public synchronized String check(String line) {
        return check(line, null);
    }

    /** Events about a window as a whole: the window's number is all they name (no element). */
    static final Set<String> WINDOW_EVENTS = Set.of("break", "close", "closed", "moved", "raised", "lowered", "active", "pick",
        "minimized", "restored");

    /**
     * As check(line); a player's report of what they did to the windows (moved, raised, lowered, active) is applied to
     * the scene here as well, and the message telling the other displays is added to echo (if not null).
     */
    public synchronized String check(String line, List<String> echo) {
        if (line == null || line.length() > MAX_SAVE) return null;
        List<Object> m = Form.parse(line);
        if (m == null) return null;
        String v = Form.verb(m);
        if (!EVENTS.contains(v)) return null;
        // (Wire 2: an event names the window's generation last, :gen n; one for an earlier opening is refused, and the
        // program never sees it)
        if (m.size() > 3 && m.get(m.size() - 2) instanceof Form.Keyword k && k.name().equals("gen")) {
            Scene.Window gw = scene.windows.get((int) Form.num(m, 1, -1));
            if (gw == null || gw.gen != Form.num(m, m.size() - 1, -1)) return null;
            m = m.subList(0, m.size() - 2);
        }
        if (line.length() > (v.equals("save") || v.equals("stash") ? MAX_SAVE : v.equals("submit") ? MAX_SUBMIT : MAX_EVENT)) return null;
        boolean system = v.equals("pick") && m.size() > 1 && Long.valueOf(0).equals(m.get(1));   // (the system menu: window 0)
        if (!system && !(m.size() > 1 && m.get(1) instanceof Long t && scene.windows.containsKey((int) (long) t))) return null;
        if (!WINDOW_EVENTS.contains(v) && !(m.size() > 2 && m.get(2) instanceof Long)) return null;
        if (!system && !fits(v, m)) return null;
        List<Object> told = scene.applyPlayer(m);
        if (told != null) version++;
        if (told != null && echo != null) echo.add(Form.print(told));
        if (v.equals("moved") && told != null)   // (the computer hears the geometry the screen kept, not the client's)
            m = List.of(m.get(0), m.get(1), told.get(2), told.get(3), told.get(4), told.get(5));
        if (v.equals("save") && m.size() > 5) {   // (kept until its answer: a saved one updates the scene's copy)
            saves.put(m.get(1) + "/" + m.get(2) + "/" + m.get(3), m.get(5));
            while (saves.size() > 8) saves.remove(saves.keySet().iterator().next());
        }
        return Form.print(m);
    }
}
