package mods.eln.robotics.computer.screen;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The server's side of a computer's screen (Argon's review: the host keeps the scene). It reads the display channel's
 * messages into a Scene, turns console bytes into a Console tab (tab 0), and batches what to send to the players
 * watching: per tick, coalesced (a text or box redrawn many times in a tick goes once: Mosh's idea), or the whole
 * scene again when a batch would be too big. It also checks what clients send before it reaches the computer. Its
 * methods are synchronized: the computer's output comes from the pool, events from the network thread.
 */
public final class ScreenHost {
    /** A tick's batch at most; past it, viewers get the scene again instead. */
    public static final int BATCH_MAX = 16 * 1024;
    /** The longest event a client may send: a save; the others are short. */
    public static final int MAX_SAVE = 40 * 1024, MAX_SUBMIT = 8192, MAX_EVENT = 256;
    static final Set<String> EVENTS = Set.of("submit", "press", "select", "save", "key", "click", "doc?", "close", "break");
    public static final int CONSOLE_TAB = 0, CONSOLE_LOG = 1;

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

    /** Bytes from the console (boot messages, traps, eprint): a Console tab, made when it first has something. */
    public synchronized void console(byte[] bytes) {
        if (bytes.length == 0) return;
        if (!scene.tabs.containsKey(CONSOLE_TAB)) {
            take(List.of(new Form.Symbol("tab"), (long) CONSOLE_TAB, "Console"));
            take(List.of(new Form.Symbol("log"), (long) CONSOLE_TAB, (long) CONSOLE_LOG, 0L, 0L, (long) scene.w, (long) scene.h));
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
        if (line == null || line.length() > MAX_SAVE) return null;
        List<Object> m = Form.parse(line);
        if (m == null) return null;
        String v = Form.verb(m);
        if (!EVENTS.contains(v)) return null;
        if (line.length() > (v.equals("save") ? MAX_SAVE : v.equals("submit") ? MAX_SUBMIT : MAX_EVENT)) return null;
        if (!(m.size() > 1 && m.get(1) instanceof Long t && scene.tabs.containsKey((int) (long) t))) return null;
        if (!v.equals("break") && !v.equals("close") && !(m.size() > 2 && m.get(2) instanceof Long)) return null;
        return Form.print(m);
    }
}
