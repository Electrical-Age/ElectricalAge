package mods.eln.solver;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites a signal processor program saved by an older version so that it does exactly what it did with today's
 * language (Baughn 2026-10-30, board #45), once, when it loads. Comments are left as written.
 * <ul>
 * <li>Version 0 (1.12 and the port before 2026-10-30) took rs(reset, set) and rc(tau, x): their arguments swap.</li>
 * <li>Version 1 had 1.12's operators: = and ^ compared as on/off (above 0.5), ! was 1 - a, and = ^ &amp; | shared one
 * level, read left to right, below the comparisons. Version 2: ^ is power, == and != compare values (within 0.01),
 * ! is on/off, and the usual order (^, * /, + -, comparisons, !, &amp;, |). So a = b becomes (a &gt; 0.5) == (b &gt; 0.5),
 * a ^ b becomes (a &gt; 0.5) != (b &gt; 0.5), !a becomes 1 - a, and brackets go where the order differs.</li>
 * </ul>
 * A statement that reads the same either way keeps its text. The old compiler numbered a statement's stateful functions
 * (their saved state) in the order it reduced them; today's in the order they stand in the text: {@link #remapState}
 * renumbers the saved state to match.
 */
public final class ProgramUpgrade {
    /** The version programs are written in now. */
    public static final int VERSION = 2;

    private ProgramUpgrade() {
    }

    /** The upgraded text and, per statement, how its saved state renumbers. */
    public record Result(String program, List<Remap> remaps) {
    }

    /**
     * A statement's state: its key prefix after the program's ("" the output, ".name." a component) and, for each old
     * number, the new one (-1: gone).
     */
    public record Remap(String prefix, int[] newIndex) {
    }

    public static String upgrade(String program, int version) {
        return upgradeAll(program, version).program();
    }

    static final Pattern OLD_DEFINITION = Pattern.compile("^(\\s*)([A-Za-z_][A-Za-z0-9_]*)(\\s*=)(.*)$", Pattern.DOTALL);

    public static Result upgradeAll(String program, int version) {
        if (version >= VERSION || program == null) return new Result(program, List.of());
        String[] lines = program.split("\n", -1);
        // the statements (line, part), as Program reads them: the last is the output, the rest definitions
        List<int[]> st = new ArrayList<>();
        List<String[]> parts = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        for (int l = 0; l < lines.length; l++) {
            int hash = lines[l].indexOf('#');
            String code = hash < 0 ? lines[l] : lines[l].substring(0, hash);
            comments.add(hash < 0 ? "" : lines[l].substring(hash));
            String[] p = code.split(";", -1);
            parts.add(p);
            for (int i = 0; i < p.length; i++) if (!p[i].isBlank()) st.add(new int[]{l, i});
        }
        List<Remap> remaps = new ArrayList<>();
        for (int k = 0; k < st.size(); k++) {
            int l = st.get(k)[0], i = st.get(k)[1];
            String s = parts.get(l)[i];
            Matcher m = OLD_DEFINITION.matcher(s);
            if (k < st.size() - 1 && m.matches()) {
                Converted c = convert(m.group(4), version);
                parts.get(l)[i] = m.group(1) + m.group(2) + m.group(3) + c.text;
                remaps.add(new Remap("." + m.group(2) + ".", c.newIndex));
            } else {
                Converted c = convert(s, version);
                parts.get(l)[i] = c.text;
                remaps.add(new Remap("", c.newIndex));
            }
        }
        StringBuilder out = new StringBuilder();
        for (int l = 0; l < lines.length; l++) {
            if (l > 0) out.append('\n');
            out.append(String.join(";", parts.get(l))).append(comments.get(l));
        }
        return new Result(out.toString(), remaps);
    }

    /**
     * Renumbers the saved state of a program's statements (keys base + prefix + number + field, e.g. "equation0state"
     * or "equation.x.1counter") as the upgrade's remaps say.
     */
    public static void remapState(CompoundTag nbt, String base, List<Remap> remaps) {
        for (Remap r : remaps) {
            String pre = base + r.prefix();
            Pattern key = Pattern.compile(Pattern.quote(pre) + "(\\d+)([A-Za-z].*)");
            Map<String, net.minecraft.nbt.Tag> moved = new java.util.LinkedHashMap<>();
            List<String> old = new ArrayList<>();
            for (String k : nbt.keySet()) {
                Matcher m = key.matcher(k);
                if (!m.matches()) continue;
                int i = Integer.parseInt(m.group(1));
                old.add(k);
                if (i < r.newIndex().length && r.newIndex()[i] >= 0) moved.put(pre + r.newIndex()[i] + m.group(2), nbt.get(k));
            }
            for (String k : old) nbt.remove(k);
            for (var e : moved.entrySet()) nbt.put(e.getKey(), e.getValue());
        }
    }

    // ---- expressions: the old grammar read, today's written

    /** An expression: a leaf (number or name), a call (with its tag "name#k": the k-th call of the name), unary, binary. */
    sealed interface N permits Leaf, Call, Un, Bin {
    }

    record Leaf(String text) implements N {
    }

    record Call(String name, String tag, List<N> args) implements N {
    }

    record Un(String op, N x) implements N {
    }

    record Bin(String op, N l, N r) implements N {
    }

    record Converted(String text, int[] newIndex) {
    }

    static Converted convert(String expr, int version) {
        String lead = expr.substring(0, expr.length() - expr.stripLeading().length());
        String trail = expr.substring(expr.stripTrailing().length());
        N old;
        try {
            Reader r = new Reader(tokens(expr, false), true);
            old = r.low();
            if (r.i != r.t.size()) throw new IllegalArgumentException("trailing");
        } catch (IllegalArgumentException e) {
            // not an expression: leave it (the swap still done textually), it won't compile either way
            return new Converted(version == 0 ? swap(expr) : expr, new int[0]);
        }
        boolean changed = false;
        if (version == 0 && hasCall(old, Set.of("rs", "rc"))) changed = true;
        N now = modern(old, version);
        if (!changed) {
            // the same text read today: the same tree?
            try {
                Reader r = new Reader(tokens(expr, true), false);
                N same = r.low();
                changed = r.i != r.t.size() || !canon(same).equals(canon(now));
            } catch (IllegalArgumentException e) {
                changed = true;
            }
        }
        String text = changed ? lead + print(now) + trail : expr;
        // the old compiler's order of the stateful calls against today's (the order in the text)
        List<String> before = Equation.legacyStatefulOrder(expr);
        List<String> after = new ArrayList<>();
        stateful(now, after);
        int[] idx = new int[before.size()];
        for (int i = 0; i < idx.length; i++) idx[i] = after.indexOf(before.get(i));
        return new Converted(text, idx);
    }

    static boolean hasCall(N n, Set<String> names) {
        return switch (n) {
            case Call c -> names.contains(c.name()) || c.args().stream().anyMatch(a -> hasCall(a, names));
            case Un u -> hasCall(u.x(), names);
            case Bin b -> hasCall(b.l(), names) || hasCall(b.r(), names);
            case Leaf l -> false;
        };
    }

    /** The stateful calls' tags in text order (pre-order: a call before its arguments). */
    static void stateful(N n, List<String> out) {
        switch (n) {
            case Call c -> {
                if (Equation.stateful(c.name())) out.add(c.tag());
                for (N a : c.args()) stateful(a, out);
            }
            case Un u -> stateful(u.x(), out);
            case Bin b -> {
                stateful(b.l(), out);
                stateful(b.r(), out);
            }
            case Leaf l -> {
            }
        }
    }

    /** The old tree in today's operators (and, from version 0, rs and rc with their arguments swapped). */
    static N modern(N n, int version) {
        return switch (n) {
            case Leaf l -> l;
            case Call c -> {
                List<N> args = new ArrayList<>();
                for (N a : c.args()) args.add(modern(a, version));
                if (version == 0 && (c.name().equals("rs") || c.name().equals("rc")) && args.size() == 2) args = List.of(args.get(1), args.get(0));
                yield new Call(c.name(), c.tag(), args);
            }
            case Un u -> u.op().equals("!") ? new Bin("-", new Leaf("1"), modern(u.x(), version)) : new Un(u.op(), modern(u.x(), version));
            case Bin b -> switch (b.op()) {
                case "=" -> new Bin("==", on(modern(b.l(), version)), on(modern(b.r(), version)));
                case "^" -> new Bin("!=", on(modern(b.l(), version)), on(modern(b.r(), version)));
                default -> new Bin(b.op(), modern(b.l(), version), modern(b.r(), version));
            };
        };
    }

    static N on(N x) {
        return new Bin(">", x, new Leaf("0.5"));
    }

    /** Fully bracketed, no tags: to tell whether two trees read the same. */
    static String canon(N n) {
        return switch (n) {
            case Leaf l -> l.text();
            case Call c -> c.name() + "(" + String.join(",", c.args().stream().map(ProgramUpgrade::canon).toList()) + ")";
            case Un u -> "(" + u.op() + canon(u.x()) + ")";
            case Bin b -> "(" + canon(b.l()) + b.op() + canon(b.r()) + ")";
        };
    }

    static int prec(N n) {
        return switch (n) {
            case Leaf l -> 9;
            case Call c -> 9;
            case Un u -> u.op().equals("!") ? 3 : 7;
            case Bin b -> switch (b.op()) {
                case "|" -> 1;
                case "&" -> 2;
                case "+", "-" -> 5;
                case "*", "/" -> 6;
                case "^" -> 8;
                default -> 4;   // comparisons
            };
        };
    }

    /** In today's syntax, brackets only where the order needs them. */
    static String print(N n) {
        return switch (n) {
            case Leaf l -> l.text();
            case Call c -> c.name() + "(" + String.join(", ", c.args().stream().map(ProgramUpgrade::print).toList()) + ")";
            case Un u -> u.op() + wrap(u.x(), prec(u.x()) < prec(u));
            case Bin b -> {
                int p = prec(b);
                boolean pow = b.op().equals("^");
                // (a comparison of comparisons gets its brackets either side, to read plainly)
                boolean cmp = p == 4;
                yield wrap(b.l(), pow ? prec(b.l()) < 9 : prec(b.l()) < p || cmp && prec(b.l()) == p) + " " + b.op() + " "
                    + wrap(b.r(), pow ? prec(b.r()) < 7 : prec(b.r()) <= p);
            }
        };
    }

    static String wrap(N n, boolean bracket) {
        return bracket ? "(" + print(n) + ")" : print(n);
    }

    /** Words and operators; today's two-character operators kept whole (modern), or every character alone (1.12). */
    static List<String> tokens(String exp, boolean modern) {
        List<String> t = Equation.tokenize(exp);
        if (modern) return t;
        List<String> out = new ArrayList<>();
        for (String s : t)
            if (s.length() == 2 && "<>=!".indexOf(s.charAt(0)) >= 0 && s.charAt(1) == '=') {
                out.add(s.substring(0, 1));
                out.add("=");
            } else out.add(s);
        return out;
    }

    /** Reads an expression in 1.12's grammar (old) or today's, into a tree; calls tagged by their order in the text. */
    static final class Reader {
        final List<String> t;
        final boolean old;
        int i;
        final java.util.Map<String, Integer> seen = new java.util.HashMap<>();
        final java.util.IdentityHashMap<Object, String> tags = new java.util.IdentityHashMap<>();
        final List<String> tagOf = new ArrayList<>();

        Reader(List<String> t, boolean old) {
            this.t = t;
            this.old = old;
            // a call's tag counts every token of its name before it, as the old compiler's order does
            for (String s : t) tagOf.add(Equation.FUNCTIONS.containsKey(s) ? s + "#" + (seen.merge(s, 1, Integer::sum) - 1) : null);
        }

        String peek() {
            return i < t.size() ? t.get(i) : "";
        }

        boolean take(String s) {
            if (!peek().equals(s)) return false;
            i++;
            return true;
        }

        boolean takeAny(Set<String> ops) {
            return ops.contains(peek());
        }

        N low() {
            if (old) {
                N a = cmp();
                while (takeAny(Set.of("=", "^", "&", "|"))) a = new Bin(t.get(i++), a, cmp());
                return a;
            }
            N a = and();
            while (take("|")) a = new Bin("|", a, and());
            return a;
        }

        N and() {
            N a = not();
            while (take("&")) a = new Bin("&", a, not());
            return a;
        }

        N not() {
            if (take("!")) return new Un("!", not());
            return cmp();
        }

        N cmp() {
            N a = add();
            Set<String> ops = old ? Set.of(">", "<") : Set.of(">", "<", ">=", "<=", "==", "!=");
            while (takeAny(ops)) a = new Bin(t.get(i++), a, add());
            return a;
        }

        N add() {
            N a = mul();
            while (takeAny(Set.of("+", "-"))) a = new Bin(t.get(i++), a, mul());
            return a;
        }

        N mul() {
            N a = unary();
            while (takeAny(Set.of("*", "/"))) a = new Bin(t.get(i++), a, unary());
            return a;
        }

        N unary() {
            if (take("-")) return new Un("-", unary());
            if (old && take("!")) return new Un("!", unary());
            if (old) return primary();
            N a = primary();
            if (take("^")) return new Bin("^", a, unary());
            return a;
        }

        N primary() {
            String s = peek();
            if (s.isEmpty() || Equation.OPERATOR_TOKENS.contains(s) || s.equals(")") || s.equals(",")) throw new IllegalArgumentException(s);
            if (take("(")) {
                N v = low();
                if (!take(")")) throw new IllegalArgumentException(")");
                return v;
            }
            int at = i++;
            if (!peek().equals("(")) return new Leaf(s);
            take("(");
            List<N> args = new ArrayList<>();
            if (!peek().equals(")")) {
                args.add(low());
                while (take(",")) args.add(low());
            }
            if (!take(")")) throw new IllegalArgumentException(")");
            return new Call(s, tagOf.get(at) == null ? s + "?" : tagOf.get(at), args);
        }
    }

    static final java.util.Set<String> SWAPPED = java.util.Set.of("rs", "rc");

    /** Swaps the two arguments of every rs and rc call in the code, inside out (text that doesn't parse). */
    static String swap(String code) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < code.length()) {
            char ch = code.charAt(i);
            if (!Character.isLetter(ch)) {
                out.append(ch);
                i++;
                continue;
            }
            int j = i;
            while (j < code.length() && (Character.isLetterOrDigit(code.charAt(j)) || code.charAt(j) == '_')) j++;
            String name = code.substring(i, j);
            int open = j;
            while (open < code.length() && code.charAt(open) == ' ') open++;
            if (!SWAPPED.contains(name) || open >= code.length() || code.charAt(open) != '(') {
                out.append(name);
                i = j;
                continue;
            }
            int close = matching(code, open);
            if (close < 0) {
                out.append(code, i, code.length());
                break;
            }
            List<String> args = split(code.substring(open + 1, close));
            out.append(code, i, open + 1);
            if (args.size() == 2) {
                out.append(respace(args.get(0), swap(args.get(1)))).append(',').append(respace(args.get(1), swap(args.get(0))));
            } else {
                for (int k = 0; k < args.size(); k++) out.append(k > 0 ? "," : "").append(swap(args.get(k)));
            }
            out.append(')');
            i = close + 1;
        }
        return out.toString();
    }

    static String respace(String slot, String a) {
        int lead = 0, trail = slot.length();
        while (lead < slot.length() && slot.charAt(lead) == ' ') lead++;
        while (trail > lead && slot.charAt(trail - 1) == ' ') trail--;
        return slot.substring(0, lead) + a.strip() + slot.substring(trail);
    }

    static int matching(String s, int open) {
        int depth = 0;
        for (int k = open; k < s.length(); k++) {
            if (s.charAt(k) == '(') depth++;
            else if (s.charAt(k) == ')' && --depth == 0) return k;
        }
        return -1;
    }

    static List<String> split(String args) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = 0;
        for (int k = 0; k < args.length(); k++) {
            char c = args.charAt(k);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                out.add(args.substring(start, k));
                start = k + 1;
            }
        }
        out.add(args.substring(start));
        return out;
    }
}
