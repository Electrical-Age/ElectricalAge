package mods.eln.gui.code;

import java.util.List;
import java.util.Set;

/**
 * What the code editor knows of a language (board #45): how to colour it, what is wrong with it, and where to draw a rule
 * between lines. Nothing about compiling: the device compiles (the signal processor into its program; an assembler,
 * an interpreter later).
 */
public interface LanguageAdapter {
    /** Kinds of token, each drawn in its colour by the editor. */
    enum Kind {
        TEXT, COMMENT, NUMBER, OPERATOR, FUNCTION, NAME, DEFINITION, INPUT_A, INPUT_B, INPUT_C, KEYWORD, ERROR
    }

    /** A token: columns start (inclusive) to end (exclusive) of its line. */
    record Token(int start, int end, Kind kind) {
    }

    /** A problem: its line and columns (0-based; end exclusive, end = start: a point), the message shown on hover. */
    record Diagnostic(int line, int start, int end, String message) {
    }

    /** The tokens of every line of the text (one list per line, in order, not overlapping). */
    List<List<Token>> tokenize(String text);

    /** What is wrong with the text (empty: nothing). */
    List<Diagnostic> validate(String text);

    /** Lines (0-based) to draw a rule above (the signal processor: its output line). */
    default Set<Integer> rulesAbove(String text) {
        return Set.of();
    }
}
