package mods.eln.sixnode.electricalmath;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The signal processor's quick reference (board #45), the handbook page beside its editor: every function and operator by
 * category, with its signature, what it does and a small example. A click inserts the example's call. Signals are 0 to 1
 * for 0 to 50 V.
 */
public final class FunctionReference {
    private FunctionReference() {
    }

    /** One entry: its category, how it is written, what it does, an example, and the text a click inserts. */
    public record Entry(String category, String signature, String meaning, String example, String insert) {
    }

    public static final List<String> CATEGORIES = List.of("Arithmetic", "Comparison and logic", "Trigonometry", "Stateful", "Utility");

    public static final List<Entry> ENTRIES = List.of(
        new Entry("Arithmetic", "a + b   a - b", "Adds or subtracts.", "A + 0.2", " + "),
        new Entry("Arithmetic", "a * b   a / b", "Multiplies or divides.", "A * 0.5", " * "),
        new Entry("Arithmetic", "-a", "Negates.", "1 - A", "-"),
        new Entry("Arithmetic", "pow(x, n)", "x to the power n.", "pow(A, 2)", "pow(A, 2)"),
        new Entry("Arithmetic", "abs(x)", "x without its sign.", "abs(A - B)", "abs(A)"),
        new Entry("Comparison and logic", "a > b   a < b", "1 when true, else 0.", "A > 0.5", " > "),
        new Entry("Comparison and logic", "a = b   a ^ b", "Equal or different, reading each as on above 0.5.", "A = B", " = "),
        new Entry("Comparison and logic", "a & b   a | b", "And, or: on above 0.5.", "A & B", " & "),
        new Entry("Comparison and logic", "!a", "Not: 1 - a.", "!A", "!"),
        new Entry("Trigonometry", "sin(x)   cos(x)", "Sine, cosine of x in radians.", "sin(A * pi)", "sin(A)"),
        new Entry("Trigonometry", "asin(x)   acos(x)", "The angle whose sine or cosine is x.", "asin(A)", "asin(A)"),
        new Entry("Trigonometry", "pi", "3.14159...", "2 * pi", "pi"),
        new Entry("Stateful", "integrate(x, reset)", "Adds x every second; back to 0 while reset is on.", "integrate(A, B)", "integrate(A, 0)"),
        new Entry("Stateful", "integrate(x, lo, hi)", "Adds x every second, held between lo and hi.", "integrate(A - 0.5, 0, 1)", "integrate(A, 0, 1)"),
        new Entry("Stateful", "derivate(x)", "How fast x changes, per second.", "derivate(A)", "derivate(A)"),
        new Entry("Stateful", "pid(target, x, p, i, d)", "PID controller driving x to target.", "pid(0.5, A, 1, 0.1, 0)", "pid(0.5, A, 1, 0.1, 0)"),
        new Entry("Stateful", "pid(target, x, p, i, d, lo, hi)", "The same, its output held between lo and hi.", "pid(0.5, A, 1, 0.1, 0, 0, 1)",
            "pid(0.5, A, 1, 0.1, 0, 0, 1)"),
        new Entry("Stateful", "rs(reset, set)", "Latch: set turns it on, reset off; reset wins.", "rs(B, A)", "rs(B, A)"),
        new Entry("Stateful", "rc(tau, x)", "Smooths x, following it with time constant tau seconds.", "rc(2, A)", "rc(1, A)"),
        new Entry("Stateful", "ramp(period)", "Climbs from 0 to 1 over period seconds, then again.", "ramp(10)", "ramp(10)"),
        new Entry("Utility", "if(c, a, b)", "a while c is on, else b.", "if(A > 0.5, B, C)", "if(A > 0.5, B, C)"),
        new Entry("Utility", "min(a, b)   max(a, b)", "The smaller or the larger.", "max(A, B)", "max(A, B)"),
        new Entry("Utility", "scale(x, in0, in1, out0, out1)", "Maps x from in0..in1 to out0..out1.", "scale(A, 0.2, 0.8, 0, 1)", "scale(A, 0, 1, 0, 1)"),
        new Entry("Utility", "batteryCharge(u)", "A battery's charge from its voltage signal.", "batteryCharge(A)", "batteryCharge(A)"),
        new Entry("Utility", "daytime", "The time of day, 0 to 1.", "daytime > 0.5", "daytime"),
        new Entry("Utility", "name = expression", "Names a part, computed once and paid once; the last line is the output.", "x = A * 0.5", "x = "),
        new Entry("Utility", "# text", "A comment.", "# my notes", "# "));

    /** The function names (for colouring). */
    public static Set<String> names() {
        return NAMES;
    }

    static final Set<String> NAMES = new LinkedHashSet<>(List.of("min", "max", "sin", "cos", "asin", "acos", "abs", "ramp", "integrate", "derivate",
        "pow", "pid", "batteryCharge", "rs", "rc", "if", "scale"));
}
