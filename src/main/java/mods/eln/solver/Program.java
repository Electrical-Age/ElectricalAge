package mods.eln.solver;

import mods.eln.misc.INBTTReady;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A signal processor's program (Baughn 2026-10-30): named components, one a line, `name = expression`, then a last line
 * with the output's expression. A single line is just the output, so every old expression is still a program (and may
 * use = as "equals"; only lines before the last are definitions). Lines may also be separated by ;. Blank lines and
 * lines starting with # are skipped.
 * <p>
 * Each component is computed once a tick, in order, before the output; a later line can use the names above it (never a
 * name below: no cycles). A stateful function (integrate, derivate, pid, rs, rc, ramp) in a component keeps one state,
 * however many lines read the component, saved under the component's name so editing one line leaves the others' state
 * alone. Cost: each line's operators once (Baughn: a named component used several times is paid for once).
 */
public final class Program implements INBTTReady {
    public static final int MAX_CHARS = 4096, MAX_LINES = 40;
    /** Iterations each line's parser may take (Equation.iterationLimit). */
    static final int ITERATIONS = 100;
    static final Pattern DEFINITION = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*=(?!=)(.*)$", Pattern.DOTALL);
    /** Names a component may not take: the functions and the constants. */
    static final Set<String> RESERVED = new HashSet<>(List.of("pi", "PI", "min", "max", "sin", "cos", "asin", "acos", "abs", "ramp", "integrate",
        "derivate", "pow", "pid", "batteryCharge", "rs", "rc", "if", "scale"));

    /** A named component: its line (1-based), its compiled expression, and the symbol later lines read it by. */
    public static final class Component implements ISymbole {
        public final String name;
        public final int line;
        final Equation equation;
        double value;

        Component(String name, int line, Equation equation) {
            this.name = name;
            this.line = line;
            this.equation = equation;
        }

        @Override
        public double getValue() {
            return value;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public final List<Component> components = new ArrayList<>();
    /**
     * Inputs given a value on a definition line, `A = 0.5` (Baughn 2026-10-30): what the input reads while no cable is
     * connected to it, a weak pull up or down; a connected cable overrides it. A constant, set once per input, free.
     */
    public final java.util.Map<String, Double> defaults = new java.util.LinkedHashMap<>();
    Equation output;
    /** The first problem, with its line (1-based; 0: the whole program), or null when it compiles. */
    public String error;
    public int errorLine;
    /** The name the error is about (an unknown or clashing name), or null: the whole line. */
    public String errorToken;
    int cost;

    private Program() {
    }

    /** The statements (lines or ;-separated), with their line numbers; blank and # lines left out. */
    static List<Object[]> statements(String source) {
        List<Object[]> out = new ArrayList<>();
        String[] lines = source.split("\n", -1);
        for (int i = 0; i < lines.length; i++)
            for (String part : lines[i].split(";", -1)) {
                String s = part.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                out.add(new Object[]{s, i + 1});
            }
        return out;
    }

    /** Is the source too long to take (server-side check of what a client sends)? */
    public static String tooBig(String source) {
        if (source.length() > MAX_CHARS) return "longer than " + MAX_CHARS + " characters";
        if (source.split("\n", -1).length > MAX_LINES) return "more than " + MAX_LINES + " lines";
        return null;
    }

    static Equation equation(String expression, List<ISymbole> symbols) {
        Equation e = new Equation();
        e.setUpDefaultOperatorAndMapper();
        e.setIterationLimit(ITERATIONS);
        e.addSymbol(new ArrayList<>(symbols));
        e.preProcess(expression);
        return e;
    }

    /** The names an expression reads that are neither known symbols, functions nor numbers (for the error message). */
    static String unknownName(String expression, List<ISymbole> symbols) {
        for (String token : expression.split("[-+*&|/^,()<>=!\\s]+")) {
            if (token.isEmpty() || RESERVED.contains(token)) continue;
            try {
                Double.parseDouble(token);
                continue;
            } catch (NumberFormatException ignored) {
            }
            boolean known = false;
            for (ISymbole s : symbols) known |= s.getName().equals(token);
            if (!known) return token;
        }
        return null;
    }

    /** Compile a program over the inputs (A, B, C, daytime...). Never null; check isValid and error. */
    public static Program compile(String source, List<ISymbole> inputs) {
        return compile(source, inputs, Set.of());
    }

    /** The same, the inputs named in defaultable taking a default value from a definition line (`A = 0.5`). */
    public static Program compile(String source, List<ISymbole> inputs, Set<String> defaultable) {
        Program p = new Program();
        String big = tooBig(source);
        if (big != null) return p.fail(0, "The program is " + big);
        List<Object[]> st = statements(source);
        if (st.isEmpty()) return p.fail(0, "Program required");
        List<ISymbole> known = new ArrayList<>(inputs);
        for (int i = 0; i < st.size() - 1; i++) {
            String s = (String) st.get(i)[0];
            int line = (int) st.get(i)[1];
            Matcher m = DEFINITION.matcher(s);
            if (!m.matches()) return p.fail(line, "line " + line + ": a line before the last must be a definition, name = expression");
            String name = m.group(1), expr = m.group(2).trim();
            if (RESERVED.contains(name)) return p.fail(line, "line " + line + ": " + name + " is a function's name").token(name);
            if (defaultable.contains(name)) {
                if (p.defaults.containsKey(name)) return p.fail(line, "line " + line + ": " + name + "'s default is already set").token(name);
                Equation e = equation(expr, List.of());
                if (!e.isValid()) return p.fail(line, "line " + line + ": an input's default must be a number");
                p.defaults.put(name, e.getValue(0));
                continue;
            }
            for (ISymbole k : known)
                if (k.getName().equals(name)) return p.fail(line, "line " + line + ": " + name + " is already defined").token(name);
            Equation e = equation(expr, known);
            if (!e.isValid()) return p.invalid(line, expr, known);
            Component c = new Component(name, line, e);
            p.components.add(c);
            p.cost += e.getOperatorCount();
            known.add(c);
        }
        String last = (String) st.get(st.size() - 1)[0];
        int line = (int) st.get(st.size() - 1)[1];
        p.output = equation(last, known);
        if (!p.output.isValid()) return p.invalid(line, last, known);
        p.cost += p.output.getOperatorCount();
        return p;
    }

    Program fail(int line, String message) {
        errorToken = null;
        error = message;
        errorLine = line;
        components.clear();
        defaults.clear();
        output = null;
        cost = 0;
        return this;
    }

    Program invalid(int line, String expr, List<ISymbole> known) {
        String unknown = unknownName(expr, known);
        return fail(line, "line " + line + ": " + (unknown != null ? unknown + " is not defined above" : "invalid expression")).token(unknown);
    }

    Program token(String t) {
        errorToken = t;
        return this;
    }

    public boolean isValid() {
        return output != null && error == null;
    }

    /** Operators in all lines, each line once. */
    public int getOperatorCount() {
        return isValid() ? cost : 0;
    }

    /** Does any line read this symbol (an input pin)? */
    public boolean uses(ISymbole s) {
        if (!isValid()) return false;
        if (output.isSymboleUsed(s)) return true;
        for (Component c : components) if (c.equation.isSymboleUsed(s)) return true;
        return false;
    }

    /** One tick: each component in order (its stateful functions stepped once), then the output. */
    public double getValue(double dt) {
        if (!isValid()) return 0;
        for (Component c : components) c.value = c.equation.getValue(dt);
        return output.getValue(dt);
    }

    /** The output's state under prefix (as a single expression saved it before 2026-10-30), each component's under prefix.name. */
    @Override
    public void writeToNBT(CompoundTag nbt, String prefix) {
        if (!isValid()) return;
        output.writeToNBT(nbt, prefix);
        for (Component c : components) c.equation.writeToNBT(nbt, prefix + "." + c.name + ".");
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String prefix) {
        if (!isValid()) return;
        output.readFromNBT(nbt, prefix);
        for (Component c : components) c.equation.readFromNBT(nbt, prefix + "." + c.name + ".");
    }

    /** The default of an input (`A = 0.5`), or NaN. */
    public double defaultOf(String input) {
        Double d = isValid() ? defaults.get(input) : null;
        return d == null ? Double.NaN : d;
    }

    @Override
    public String toString() {
        return isValid() ? components.size() + " components, cost " + cost : "invalid: " + error;
    }
}
