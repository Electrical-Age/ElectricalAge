package mods.eln.robotics.computer;

import mods.eln.gui.code.LanguageAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Janet for the computer's editor: colours (comments, strings, numbers, keywords, the called function, the name a def
 * defines) and unbalanced brackets as problems. Strings don't span lines here (long strings are rare in programs).
 */
public final class JanetLanguage implements LanguageAdapter {
    static final Set<String> DEFINERS = Set.of("def", "def-", "defn", "defn-", "var", "var-", "defmacro", "defmacro-");

    @Override
    public List<List<Token>> tokenize(String text) {
        List<List<Token>> out = new ArrayList<>();
        for (String line : text.split("\n", -1)) out.add(line(line));
        return out;
    }

    static boolean symbolChar(char c) {
        return !Character.isWhitespace(c) && "()[]{}\"#;'`,@~|".indexOf(c) < 0;
    }

    static List<Token> line(String s) {
        List<Token> t = new ArrayList<>();
        int i = 0, n = s.length();
        boolean afterOpen = false;
        String lastFn = null;
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '#') {
                t.add(new Token(i, n, Kind.COMMENT));
                break;
            }
            if (c == '"') {
                int j = i + 1;
                while (j < n && s.charAt(j) != '"') j += s.charAt(j) == '\\' ? 2 : 1;
                j = Math.min(n, j + 1);
                t.add(new Token(i, j, Kind.INPUT_C));
                i = j;
                afterOpen = false;
                continue;
            }
            if ("()[]{}".indexOf(c) >= 0) {
                t.add(new Token(i, i + 1, Kind.OPERATOR));
                afterOpen = c == '(';
                i++;
                continue;
            }
            if ("'`,@~|".indexOf(c) >= 0) {
                t.add(new Token(i, i + 1, Kind.OPERATOR));
                i++;
                continue;
            }
            int j = i;
            while (j < n && symbolChar(s.charAt(j))) j++;
            if (j == i) j = i + 1;
            String w = s.substring(i, j);
            Kind k;
            if (w.startsWith(":")) k = Kind.KEYWORD;
            else if (isNumber(w)) k = Kind.NUMBER;
            else if (w.equals("true") || w.equals("false") || w.equals("nil")) k = Kind.NUMBER;
            else if (afterOpen) k = Kind.FUNCTION;
            else if (lastFn != null && DEFINERS.contains(lastFn)) k = Kind.DEFINITION;
            else k = Kind.TEXT;
            t.add(new Token(i, j, k));
            lastFn = afterOpen ? w : null;
            afterOpen = false;
            i = j;
        }
        return t;
    }

    static boolean isNumber(String w) {
        char c = w.charAt(0);
        if (Character.isDigit(c)) return true;
        return (c == '-' || c == '+' || c == '.') && w.length() > 1 && Character.isDigit(w.charAt(1));
    }

    @Override
    public List<Diagnostic> validate(String text) {
        List<Diagnostic> d = new ArrayList<>();
        java.util.ArrayDeque<int[]> open = new java.util.ArrayDeque<>();   // line, col, the bracket
        String[] lines = text.split("\n", -1);
        for (int l = 0; l < lines.length; l++) {
            String s = lines[l];
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '#') break;
                if (c == '"') {
                    i++;
                    while (i < s.length() && s.charAt(i) != '"') i += s.charAt(i) == '\\' ? 2 : 1;
                    continue;
                }
                if ("([{".indexOf(c) >= 0) open.push(new int[]{l, i, c});
                else if (")]}".indexOf(c) >= 0) {
                    char want = c == ')' ? '(' : c == ']' ? '[' : '{';
                    if (open.isEmpty()) d.add(new Diagnostic(l, i, i + 1, "nothing to close here"));
                    else if (open.peek()[2] != want) {
                        int[] o = open.pop();
                        d.add(new Diagnostic(l, i, i + 1, "closes the " + (char) o[2] + " on line " + (o[0] + 1)));
                    } else open.pop();
                }
            }
        }
        for (int[] o : open) d.add(new Diagnostic(o[0], o[1], o[1] + 1, "never closed"));
        return d;
    }
}
