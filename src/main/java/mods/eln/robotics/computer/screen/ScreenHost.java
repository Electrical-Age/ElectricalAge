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
    static final Set<String> EVENTS = Set.of("submit", "press", "select", "save", "stash", "key", "click", "doc?", "close", "break",
        "moved", "raised", "lowered", "active", "closed", "pick", "answer", "set", "open", "dropped", "minimized", "restored");
    /** The kernel's window (what the machine prints at the console device) and its log. */
    public static final int CONSOLE_TAB = Scene.KERNEL, CONSOLE_LOG = 1;

    public final Scene scene = new Scene();
    private final StringBuilder partial = new StringBuilder();
    private final List<List<Object>> batch = new ArrayList<>();
    private int batchBytes;
    private boolean overflow;

    /** Bytes from the computer's display channel: whole lines become messages. */
    public synchronized void fromComputer(byte[] display) {
        if (display.length == 0) return;
        partial.append(new String(display, StandardCharsets.UTF_8));
        int nl;
        while ((nl = partial.indexOf("\n")) >= 0) {
            String line = partial.substring(0, nl);
            partial.delete(0, nl + 1);
            take(Form.parse(line));
        }
        if (partial.length() > Form.MAX_LINE) partial.setLength(0);   // a line that never ends: dropped
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
        take(List.of(new Form.Symbol("append"), (long) CONSOLE_TAB, (long) CONSOLE_LOG, Form.kw("white"),
            new String(bytes, StandardCharsets.UTF_8)));
    }

    /** The computer restarted: everyone's screen empties (a half-sent line is forgotten). */
    public synchronized void reset() {
        partial.setLength(0);
        take(List.of(new Form.Symbol("reset"), (long) Scene.DEFAULT_W, (long) Scene.DEFAULT_H));
    }

    private void take(List<Object> m) {
        if (m == null || !scene.apply(m)) return;
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
        Map<String, Integer> last = new HashMap<>();
        for (int k = 0; k < batch.size(); k++) {
            List<Object> m = batch.get(k);
            if (Scene.replaces(m)) last.put(key(m), k);
        }
        List<String> out = new ArrayList<>();
        int bytes = 0;
        for (int k = 0; k < batch.size(); k++) {
            List<Object> m = batch.get(k);
            if (Scene.replaces(m) && last.get(key(m)) != k) continue;
            String line = Form.print(m);
            bytes += line.length() + 1;
            out.add(line);
        }
        batch.clear();
        batchBytes = 0;
        return bytes > BATCH_MAX ? null : out;
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
        if (line.length() > (v.equals("save") || v.equals("stash") ? MAX_SAVE : v.equals("submit") ? MAX_SUBMIT : MAX_EVENT)) return null;
        boolean system = v.equals("pick") && m.size() > 1 && Long.valueOf(0).equals(m.get(1));   // (the system menu: window 0)
        if (!system && !(m.size() > 1 && m.get(1) instanceof Long t && scene.windows.containsKey((int) (long) t))) return null;
        if (!WINDOW_EVENTS.contains(v) && !(m.size() > 2 && m.get(2) instanceof Long)) return null;
        List<Object> told = scene.applyPlayer(m);
        if (told != null && echo != null) echo.add(Form.print(told));
        return Form.print(m);
    }
}
