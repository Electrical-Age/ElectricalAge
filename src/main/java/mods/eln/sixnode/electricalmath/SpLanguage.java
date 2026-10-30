package mods.eln.sixnode.electricalmath;

import mods.eln.gui.code.LanguageAdapter;
import mods.eln.solver.ISymbole;
import mods.eln.solver.Program;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The signal processor's language for the code editor (board #45): inputs A red, B blue, C green; functions, named
 * components (their definitions marked), numbers, operators, # comments; a rule above the output line. Problems come
 * from compiling the program (Program) as the processor will.
 */
public final class SpLanguage implements LanguageAdapter {
    public static final SpLanguage INSTANCE = new SpLanguage();
    static final Pattern IDENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*"), NUMBER = Pattern.compile("[0-9]*\\.?[0-9]+"),
        DEFINITION = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*=");
    static final String OPERATORS = "+-*&|/^,()<>=!;";
    /** The processor's own symbols, by name (for checking a program on the client). */
    static final List<String> SYMBOLS = List.of("A", "B", "C", "daytime");

    /** A statement: its line, the columns it spans, whether it defines a component. */
    record Statement(int line, int start, int end, boolean definition) {
    }

    /** The statements of the text, by ; and line, blank and # lines left out; all but the last that define are definitions. */
    static List<Statement> statements(String text) {
        List<Statement> out = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        for (int l = 0; l < lines.length; l++) {
            String s = lines[l];
            int start = 0;
            for (int i = 0; i <= s.length(); i++)
                if (i == s.length() || s.charAt(i) == ';') {
                    String part = s.substring(start, i).trim();
                    if (!part.isEmpty() && !part.startsWith("#")) out.add(new Statement(l, start, i, DEFINITION.matcher(s.substring(start, i)).find()));
                    start = i + 1;
                }
        }
        if (!out.isEmpty()) {
            Statement last = out.remove(out.size() - 1);
            out.add(new Statement(last.line(), last.start(), last.end(), false));
        }
        return out;
    }

    static Set<String> definedNames(String text) {
        Set<String> out = new HashSet<>();
        String[] lines = text.split("\n", -1);
        for (Statement st : statements(text))
            if (st.definition()) {
                Matcher m = DEFINITION.matcher(lines[st.line()].substring(st.start(), st.end()));
                if (m.find()) out.add(m.group(1));
            }
        return out;
    }

    @Override
    public List<List<Token>> tokenize(String text) {
        String[] lines = text.split("\n", -1);
        List<Statement> st = statements(text);
        Set<String> names = definedNames(text);
        List<List<Token>> out = new ArrayList<>();
        for (int l = 0; l < lines.length; l++) {
            String s = lines[l];
            List<Token> ts = new ArrayList<>();
            int comment = s.indexOf('#');
            int i = 0;
            int end = comment < 0 ? s.length() : comment;
            while (i < end) {
                char c = s.charAt(i);
                if (c == ' ') {
                    i++;
                    continue;
                }
                Matcher num = NUMBER.matcher(s).region(i, end), id = IDENT.matcher(s).region(i, end);
                if (Character.isDigit(c) || c == '.' && num.lookingAt()) {
                    num.lookingAt();
                    ts.add(new Token(i, num.end(), Kind.NUMBER));
                    i = num.end();
                } else if (id.lookingAt()) {
                    String w = id.group();
                    ts.add(new Token(i, id.end(), kind(w, isDefinitionName(st, l, i, s), names)));
                    i = id.end();
                } else if (OPERATORS.indexOf(c) >= 0) {
                    ts.add(new Token(i, i + 1, Kind.OPERATOR));
                    i++;
                } else {
                    ts.add(new Token(i, i + 1, Kind.TEXT));
                    i++;
                }
            }
            if (comment >= 0) ts.add(new Token(comment, s.length(), Kind.COMMENT));
            out.add(ts);
        }
        return out;
    }

    /** Is the identifier at (line, col) the name a definition defines? */
    static boolean isDefinitionName(List<Statement> st, int line, int col, String s) {
        for (Statement x : st)
            if (x.definition() && x.line() == line && col >= x.start() && col < x.end()) {
                int first = x.start();
                while (first < x.end() && s.charAt(first) == ' ') first++;
                return first == col;
            }
        return false;
    }

    @Override
    public List<String> hover(String text, int line, int col) {
        List<List<Token>> tokens = tokenize(text);
        if (line < 0 || line >= tokens.size()) return List.of();
        String s = text.split("\n", -1)[line];
        for (Token t : tokens.get(line)) {
            if (col < t.start() || col >= t.end()) continue;
            String w = s.substring(t.start(), t.end());
            switch (t.kind()) {
                case FUNCTION, KEYWORD -> {
                    List<String> out = new ArrayList<>();
                    for (var e : FunctionReference.forName(w.equals("PI") ? "pi" : w)) {
                        out.add(e.signature());
                        out.add(e.meaning());
                        out.add("e.g. " + e.example());
                    }
                    return out;
                }
                case INPUT_A, INPUT_B, INPUT_C -> {
                    List<String> out = new ArrayList<>(List.of("Input " + w, "The signal on that side, 0 to 1 for 0 to 50 V."));
                    for (Statement st : statements(text))
                        if (st.definition()) {
                            String def = text.split("\n", -1)[st.line()].substring(st.start(), st.end()).trim();
                            Matcher m = DEFINITION.matcher(def);
                            if (m.find() && m.group(1).equals(w))
                                out.add("With no cable on it, it reads its default: " + def.substring(m.end()).trim() + ".");
                        }
                    return out;
                }
                case NAME, DEFINITION -> {
                    String[] lines = text.split("\n", -1);
                    for (Statement st : statements(text))
                        if (st.definition()) {
                            String def = lines[st.line()].substring(st.start(), st.end()).trim();
                            Matcher m = DEFINITION.matcher(def);
                            if (m.find() && m.group(1).equals(w)) return List.of(w, def);
                        }
                    return List.of();
                }
                default -> {
                    return List.of();
                }
            }
        }
        return List.of();
    }

    static Kind kind(String w, boolean defining, Set<String> names) {
        switch (w) {
            case "A":
                return Kind.INPUT_A;
            case "B":
                return Kind.INPUT_B;
            case "C":
                return Kind.INPUT_C;
            case "pi", "PI", "daytime":
                return Kind.KEYWORD;
            default:
        }
        if (defining) return Kind.DEFINITION;
        if (FunctionReference.names().contains(w)) return Kind.FUNCTION;
        if (names.contains(w)) return Kind.NAME;
        return Kind.TEXT;
    }

    static List<ISymbole> symbols() {
        List<ISymbole> out = new ArrayList<>();
        for (String n : SYMBOLS)
            out.add(new ISymbole() {
                public double getValue() {
                    return 0;
                }

                public String getName() {
                    return n;
                }
            });
        return out;
    }

    /** The program compiled as the processor compiles it (no live inputs). */
    public static Program compile(String text) {
        return Program.compile(text, symbols(), ElectricalMathElement.DEFAULTABLE);
    }

    @Override
    public List<Diagnostic> validate(String text) {
        if (text.isBlank()) return List.of();
        Program p = compile(text);
        if (p.isValid()) return List.of();
        String[] lines = text.split("\n", -1);
        int l = Math.max(0, Math.min(lines.length - 1, p.errorLine - 1));
        String s = lines[l];
        int start = 0, end = s.length();
        if (p.errorToken != null) {
            Matcher m = Pattern.compile("\\b" + Pattern.quote(p.errorToken) + "\\b").matcher(s);
            if (m.find()) {
                start = m.start();
                end = m.end();
            }
        }
        return List.of(new Diagnostic(l, start, end, p.error));
    }

    @Override
    public Set<Integer> rulesAbove(String text) {
        List<Statement> st = statements(text);
        if (st.size() < 2) return Set.of();
        return Set.of(st.get(st.size() - 1).line());
    }
}
