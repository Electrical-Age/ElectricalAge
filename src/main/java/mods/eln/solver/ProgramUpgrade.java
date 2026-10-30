package mods.eln.solver;

/**
 * Rewrites a signal processor program saved by an older version so that it does the same with today's functions
 * (Baughn 2026-10-30, board #45). Version 0 (1.12 and the port before 2026-10-30) took rs(reset, set) and rc(tau, x);
 * version 1 takes rs(set, reset) and rc(x, tau), so their two arguments swap. Comments (# to the end of a line) are
 * left as written.
 */
public final class ProgramUpgrade {
    /** The version programs are written in now. */
    public static final int VERSION = 1;

    private ProgramUpgrade() {
    }

    public static String upgrade(String program, int version) {
        if (version >= VERSION || program == null) return program;
        StringBuilder out = new StringBuilder();
        String[] lines = program.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) out.append('\n');
            String line = lines[i];
            int hash = line.indexOf('#');
            String code = hash < 0 ? line : line.substring(0, hash);
            out.append(swap(code)).append(hash < 0 ? "" : line.substring(hash));
        }
        return out.toString();
    }

    static final java.util.Set<String> SWAPPED = java.util.Set.of("rs", "rc");

    /** Swaps the two arguments of every rs and rc call in the code, inside out. */
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
            java.util.List<String> args = split(code.substring(open + 1, close));
            out.append(code, i, open + 1);
            if (args.size() == 2) {
                // each slot keeps its own spacing; the arguments trade places
                out.append(respace(args.get(0), swap(args.get(1)))).append(',').append(respace(args.get(1), swap(args.get(0))));
            } else {
                for (int k = 0; k < args.size(); k++) out.append(k > 0 ? "," : "").append(swap(args.get(k)));
            }
            out.append(')');
            i = close + 1;
        }
        return out.toString();
    }

    /** The argument a, with the spaces round slot. */
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

    static java.util.List<String> split(String args) {
        java.util.List<String> out = new java.util.ArrayList<>();
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
