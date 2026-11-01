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
 * name below: no cycles).
 * <p>
 * Field bus channels (mods.eln.bus) are written {@code bus.<name>}: read anywhere a name may stand, as the bus had them
 * at the start of the tick (a channel this program writes too reads the bus, so {@code bus.n = bus.n + 0.1} is a loop
 * with a tick's delay); written by a definition line {@code bus.<name> = expression}, which may also be the last line,
 * leaving the processor no output expression (its wire then gives 0 V). The names read and written are what the
 * processor claims on its bus (busNames). A stateful function (integrate, derivate, pid, rs, rc, ramp) in a component keeps one state,
 * however many lines read the component, saved under the component's name so editing one line leaves the others' state
 * alone. Cost: each line's operators once (Baughn: a named component used several times is paid for once).
 */
public final class Program implements INBTTReady {
    public static final int MAX_CHARS = 4096, MAX_LINES = 40;
    /** Iterations each line's parser may take (Equation.iterationLimit). */
    static final int ITERATIONS = 100;
    static final Pattern DEFINITION = Pattern.compile("^(bus\\.[A-Za-z0-9_]*|[A-Za-z_][A-Za-z0-9_]*)\\s*=(?!=)(.*)$", Pattern.DOTALL);
    /** What a field bus channel is written with before its name. */
    public static final String BUS = "bus.";
    /** Names a component may not take: the functions and the constants. */
    static final Set<String> RESERVED = new HashSet<>(List.of("pi", "PI", "min", "max", "sin", "cos", "asin", "acos", "abs", "ramp", "integrate",
        "derivate", "pow", "pid", "batteryCharge", "rs", "rc", "if", "scale"));

    /** A named component: its line (1-based), its compiled expression, and the symbol later lines read it by. */
    public static final class Component implements ISymbole {
        public final String name;
        public final int line;
        final Equation equation;
        double value;
        /** The bus channel it writes (a {@code bus.<name> =} line, read by no later line), or null. */
        public final String channel;

        Component(String name, int line, Equation equation) {
            this.name = name;
            this.line = line;
            this.equation = equation;
            this.channel = name.startsWith(BUS) ? name.substring(BUS.length()) : null;
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
    /** The bus channels read and written, in the order the text names them first. */
    final java.util.LinkedHashSet<String> busNames = new java.util.LinkedHashSet<>();
    /** The lines that write a bus channel, in order. */
    final List<Component> busWrites = new ArrayList<>();
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
        for (int i = 0; i < lines.length; i++) {
            // # starts a comment anywhere on a line (there are no strings)
            int hash = lines[i].indexOf('#');
            String code = hash < 0 ? lines[i] : lines[i].substring(0, hash);
            for (String part : code.split(";", -1)) {
                String s = part.trim();
                if (s.isEmpty()) continue;
                out.add(new Object[]{s, i + 1});
            }
        }
        return out;
    }

    /** The output line's expression (the last statement), as written; "" if none. */
    public static String outputLine(String source) {
        List<Object[]> st = statements(source == null ? "" : source);
        return st.isEmpty() ? "" : (String) st.get(st.size() - 1)[0];
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
        return compile(source, inputs, defaultable, null);
    }

    /** A bus channel read: the bus's level of it, 0..1 (0 with no bus). */
    static final class BusRead implements ISymbole {
        final String name;
        final String channel;
        final java.util.function.ToDoubleFunction<String> bus;

        BusRead(String channel, java.util.function.ToDoubleFunction<String> bus) {
            this.name = BUS + channel;
            this.channel = channel;
            this.bus = bus;
        }

        @Override
        public double getValue() {
            return bus == null ? 0 : bus.applyAsDouble(channel);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * Each {@code bus.<name>} the expression reads, as a symbol in known (once a name); null, or the first word that
     * isn't a channel name, which the line's error is about.
     */
    String busReads(String expression, List<ISymbole> known, java.util.function.ToDoubleFunction<String> bus) {
        List<String> tokens = Equation.tokenize(expression);
        if (tokens == null) return null;
        for (String t : tokens) {
            if (!t.startsWith(BUS)) continue;
            String channel = t.substring(BUS.length());
            if (!mods.eln.bus.Bus.validName(channel)) return t;
            busNames.add(channel);
            boolean have = false;
            for (ISymbole k : known) have |= k.getName().equals(t);
            if (!have) known.add(new BusRead(channel, bus));
        }
        return null;
    }

    Program badChannel(int line, String word) {
        return fail(line, "line " + line + ": " + word + " is not a bus channel: its name is 1 to 16 of a-z, 0-9 and _").token(word);
    }

    /**
     * The same, reading bus channels through bus (null: they read 0, as on the client, which has no bus). A program's
     * {@code bus.<name>} channels are its busNames.
     */
    public static Program compile(String source, List<ISymbole> inputs, Set<String> defaultable, java.util.function.ToDoubleFunction<String> bus) {
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
            if (name.startsWith(BUS)) {
                if (p.busDefinition(name, expr, line, known, bus) == null) return p;
                continue;
            }
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
            String bad = p.busReads(expr, known, bus);
            if (bad != null) return p.badChannel(line, bad);
            Equation e = equation(expr, known);
            if (!e.isValid()) return p.invalid(line, expr, known);
            Component c = new Component(name, line, e);
            p.components.add(c);
            p.cost += e.getOperatorCount();
            known.add(c);
        }
        String last = (String) st.get(st.size() - 1)[0];
        int line = (int) st.get(st.size() - 1)[1];
        // a bus definition may end the program: no output expression then (only bus definitions are definitions there)
        Matcher lm = DEFINITION.matcher(last);
        if (lm.matches() && lm.group(1).startsWith(BUS)) {
            p.busDefinition(lm.group(1), lm.group(2).trim(), line, known, bus);
            return p;
        }
        String bad = p.busReads(last, known, bus);
        if (bad != null) return p.badChannel(line, bad);
        p.output = equation(last, known);
        if (!p.output.isValid()) return p.invalid(line, last, known);
        p.cost += p.output.getOperatorCount();
        return p;
    }

    /** A {@code bus.<name> = expression} line: a component writing that channel, read by no line (bus.name reads the
     *  bus). Null if it fails (this program then has the error). */
    Component busDefinition(String name, String expr, int line, List<ISymbole> known, java.util.function.ToDoubleFunction<String> bus) {
        String channel = name.substring(BUS.length());
        if (!mods.eln.bus.Bus.validName(channel)) {
            badChannel(line, name);
            return null;
        }
        for (Component w : busWrites)
            if (w.name.equals(name)) {
                fail(line, "line " + line + ": " + name + " is already written above").token(name);
                return null;
            }
        String bad = busReads(expr, known, bus);
        if (bad != null) {
            badChannel(line, bad);
            return null;
        }
        Equation e = equation(expr, known);
        if (!e.isValid()) {
            invalid(line, expr, known);
            return null;
        }
        busNames.add(channel);
        Component c = new Component(name, line, e);
        components.add(c);
        busWrites.add(c);
        cost += e.getOperatorCount();
        return c;
    }

    /** Refuse it whole with this message (the processor's bus has no room for its channels): it no longer compiles. */
    public Program refuse(String message) {
        return fail(0, message);
    }

    Program fail(int line, String message) {
        errorToken = null;
        error = message;
        errorLine = line;
        components.clear();
        defaults.clear();
        busNames.clear();
        busWrites.clear();
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

    /** It compiled: it has an output expression, or ends with a bus definition. */
    public boolean isValid() {
        return error == null && (output != null || !busWrites.isEmpty());
    }

    /** Has it an output expression (else its wire gives 0 V)? */
    public boolean hasOutput() {
        return isValid() && output != null;
    }

    /** The bus channels it reads or writes (what the processor claims), in the order the text names them. */
    public Set<String> busNames() {
        return isValid() ? java.util.Collections.unmodifiableSet(busNames) : Set.of();
    }

    /** Its bus writes, each channel and the value its line had last: once a tick, after getValue. Non-finite: 0. */
    public void writeBus(java.util.function.ObjDoubleConsumer<String> bus) {
        if (!isValid()) return;
        for (Component c : busWrites) bus.accept(c.channel, Double.isFinite(c.value) ? c.value : 0);
    }

    /** Operators in all lines, each line once. */
    public int getOperatorCount() {
        return isValid() ? cost : 0;
    }

    /** Does any line read this symbol (an input pin)? */
    public boolean uses(ISymbole s) {
        if (!isValid()) return false;
        if (output != null && output.isSymboleUsed(s)) return true;
        for (Component c : components) if (c.equation.isSymboleUsed(s)) return true;
        return false;
    }

    /** One tick: each component in order (its stateful functions stepped once), then the output. */
    public double getValue(double dt) {
        if (!isValid()) return 0;
        for (Component c : components) c.value = c.equation.getValue(dt);
        return output == null ? 0 : output.getValue(dt);
    }

    /** The output's state under prefix (as a single expression saved it before 2026-10-30), each component's under prefix.name. */
    @Override
    public void writeToNBT(CompoundTag nbt, String prefix) {
        if (!isValid()) return;
        if (output != null) output.writeToNBT(nbt, prefix);
        for (Component c : components) c.equation.writeToNBT(nbt, prefix + "." + c.name + ".");
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String prefix) {
        if (!isValid()) return;
        if (output != null) output.readFromNBT(nbt, prefix);
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
