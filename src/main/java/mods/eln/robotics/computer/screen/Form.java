package mods.eln.robotics.computer.screen;

import java.util.ArrayList;
import java.util.List;

/**
 * The screen protocol's wire format: one message per line, a Janet form. Only what messages use is read: lists
 * {@code ( )} and tuples {@code [ ]} (both as java.util.List), strings (Janet's escapes), integers (as Long), keywords
 * (Keyword), bare words (Symbol: a message's verb) and true, false, nil. A form nests at most MAX_DEPTH deep and a line holds at most MAX_LINE characters;
 * anything else is malformed and the line is skipped (Argon's review: the parser faces untrusted input both ways).
 */
public final class Form {
    public static final int MAX_DEPTH = 4, MAX_LINE = 64 * 1024;

    private Form() {
    }

    /** A Janet keyword, :name. */
    public record Keyword(String name) {
        @Override
        public String toString() {
            return ":" + name;
        }
    }

    public static Keyword kw(String name) {
        return new Keyword(name);
    }

    /** The message on a line, or null if it isn't one (malformed, too long, too deep, not a list). */
    public static List<Object> parse(String line) {
        if (line == null || line.length() > MAX_LINE) return null;
        Reader r = new Reader(line);
        try {
            r.skip();
            Object o = r.form(0);
            r.skip();
            if (r.i != line.length() || !(o instanceof List<?>)) return null;
            @SuppressWarnings("unchecked") List<Object> l = (List<Object>) o;
            return l.isEmpty() ? null : l;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static final class Reader {
        final String s;
        int i;

        Reader(String s) {
            this.s = s;
        }

        void skip() {
            while (i < s.length() && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) i++;
        }

        Object form(int depth) {
            if (depth > MAX_DEPTH) throw new IllegalArgumentException("too deep");
            char c = s.charAt(i);
            if (c == '(' || c == '[') {
                char close = c == '(' ? ')' : ']';
                i++;
                List<Object> l = new ArrayList<>();
                skip();
                while (s.charAt(i) != close) {
                    l.add(form(depth + 1));
                    skip();
                }
                i++;
                return l;
            }
            if (c == '"') return string();
            int j = i;
            while (j < s.length() && !Character.isWhitespace(s.charAt(j)) && "()[]\"".indexOf(s.charAt(j)) < 0) j++;
            String t = s.substring(i, j);
            if (t.isEmpty()) throw new IllegalArgumentException("nothing");
            i = j;
            if (t.charAt(0) == ':') return new Keyword(t.substring(1));
            switch (t) {
                case "true" -> {
                    return Boolean.TRUE;
                }
                case "false" -> {
                    return Boolean.FALSE;
                }
                case "nil" -> {
                    return null;
                }
                default -> {
                }
            }
            char c0 = t.charAt(0);
            if (Character.isDigit(c0) || ((c0 == '-' || c0 == '+' || c0 == '.') && t.length() > 1)) {
                try {
                    return Long.parseLong(t);
                } catch (NumberFormatException e) {
                    try {
                        double d = Double.parseDouble(t);   // (Janet prints whole numbers without a point; others round)
                        if (Double.isFinite(d)) return Math.round(d);
                    } catch (NumberFormatException e2) {
                        // a symbol like -> after all
                    }
                }
            }
            return new Symbol(t);
        }

        String string() {
            StringBuilder b = new StringBuilder();
            i++;
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c != '\\') {
                    b.append(c);
                    continue;
                }
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> b.append('\n');
                    case 't' -> b.append('\t');
                    case 'r' -> b.append('\r');
                    case '0' -> b.append('\0');
                    case 'e' -> b.append('\u001b');
                    case 'f' -> b.append('\f');
                    case 'v' -> b.append('\u000b');
                    case 'z' -> b.append('\0');
                    case 'x' -> {
                        b.append((char) Integer.parseInt(s.substring(i, i + 2), 16));
                        i += 2;
                    }
                    case 'u' -> {
                        b.appendCodePoint(Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    case 'U' -> {
                        b.appendCodePoint(Integer.parseInt(s.substring(i, i + 6), 16));
                        i += 6;
                    }
                    default -> b.append(e);   // \" \\ and anything else as itself
                }
            }
        }
    }

    // ---- writing

    /** A message as its line (no newline): (a b c). */
    public static String print(Object... parts) {
        StringBuilder b = new StringBuilder("(");
        for (int k = 0; k < parts.length; k++) {
            if (k > 0) b.append(' ');
            write(b, parts[k]);
        }
        return b.append(')').toString();
    }

    public static String print(List<?> message) {
        return print(message.toArray());
    }

    static void write(StringBuilder b, Object o) {
        if (o == null) b.append("nil");
        else if (o instanceof String s) quote(b, s);
        else if (o instanceof List<?> l) {
            b.append('[');
            for (int k = 0; k < l.size(); k++) {
                if (k > 0) b.append(' ');
                write(b, l.get(k));
            }
            b.append(']');
        } else b.append(o);   // Long, Integer, Boolean, Keyword
    }

    static void quote(StringBuilder b, String s) {
        b.append('"');
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\t' -> b.append("\\t");
                case '\r' -> b.append("\\r");
                default -> {
                    if (c < 0x20) b.append(String.format("\\x%02x", (int) c));
                    else b.append(c);
                }
            }
        }
        b.append('"');
    }

    // ---- reading fields

    public static long num(List<Object> m, int k, long dflt) {
        return k < m.size() && m.get(k) instanceof Long l ? l : dflt;
    }

    public static String str(List<Object> m, int k, String dflt) {
        return k < m.size() && m.get(k) instanceof String s ? s : dflt;
    }

    public static String kwName(List<Object> m, int k, String dflt) {
        return k < m.size() && m.get(k) instanceof Keyword w ? w.name() : dflt;
    }

    /** The message's verb: its first element, a symbol written bare (read as a keyword-less token) or a keyword. */
    public static String verb(List<Object> m) {
        Object o = m.get(0);
        return o instanceof Keyword w ? w.name() : o instanceof Symbol s ? s.name() : "";
    }

    /** A bare word that isn't true, false, nil, a number or a keyword: the verb of a message. */
    public record Symbol(String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}
