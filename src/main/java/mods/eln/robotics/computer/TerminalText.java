package mods.eln.robotics.computer;

/**
 * The terminal's text as the screen shows it: lines of at most COLS characters, the last LINES kept. Bytes are UTF-8;
 * newline ends a line, carriage return goes back to its start, backspace steps back one, tab goes to the next stop of
 * 8, a line longer than COLS wraps. Other control bytes (and ANSI escapes, for now) are dropped.
 */
public final class TerminalText {
    public static final int COLS = 52, LINES = 300;

    private final java.util.ArrayDeque<StringBuilder> lines = new java.util.ArrayDeque<>();
    private StringBuilder cur = new StringBuilder();
    private int col;
    private final java.io.ByteArrayOutputStream pending = new java.io.ByteArrayOutputStream();
    private boolean escape;

    public TerminalText() {
        lines.add(cur);
    }

    public void clear() {
        lines.clear();
        cur = new StringBuilder();
        col = 0;
        lines.add(cur);
    }

    public void append(byte[] b) {
        for (byte x : b) {
            int c = x & 0xFF;
            if (escape) {   // an ANSI escape: skip to its final byte
                if (c >= 0x40 && c <= 0x7E && c != '[') escape = false;
                continue;
            }
            if (c == 0x1B) {
                escape = true;
                continue;
            }
            if (c >= 0x80) {
                pending.write(c);
                if (complete()) put(new String(pending.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
                continue;
            }
            switch (c) {
                case '\n' -> newline();
                case '\r' -> col = 0;
                case '\b' -> col = Math.max(0, col - 1);
                case '\t' -> {
                    do put(" "); while (col % 8 != 0);
                }
                default -> {
                    if (c >= 0x20 && c < 0x7F) put(String.valueOf((char) c));
                }
            }
        }
    }

    private boolean complete() {
        byte[] p = pending.toByteArray();
        int lead = p[0] & 0xFF, need = lead >= 0xF0 ? 4 : lead >= 0xE0 ? 3 : lead >= 0xC0 ? 2 : 1;
        if (p.length < need) return false;
        pending.reset();
        return true;
    }

    private void put(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (col >= COLS) newline();
            if (col < cur.length()) cur.setCharAt(col, s.charAt(i));
            else {
                while (cur.length() < col) cur.append(' ');
                cur.append(s.charAt(i));
            }
            col++;
        }
    }

    private void newline() {
        cur = new StringBuilder();
        col = 0;
        lines.add(cur);
        while (lines.size() > LINES) lines.poll();
    }

    public int lineCount() {
        return lines.size();
    }

    /** Line i, 0 the oldest kept. */
    public String line(int i) {
        int k = 0;
        for (StringBuilder l : lines) if (k++ == i) return l.toString();
        return "";
    }

    /** The last n lines (fewer if there aren't). */
    public java.util.List<String> tail(int n) {
        java.util.List<String> all = new java.util.ArrayList<>(lines.size());
        for (StringBuilder l : lines) all.add(l.toString());
        return all.subList(Math.max(0, all.size() - n), all.size());
    }
}
