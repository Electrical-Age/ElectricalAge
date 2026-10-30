package mods.eln.solver;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProgramUpgradeTest {
    @Test
    void oldCallsSwapTheirArguments() {
        assertEquals("rs(B, 0)", ProgramUpgrade.upgrade("rs(0,B)", 0));
        assertEquals("rs(B > 0.5, A)", ProgramUpgrade.upgrade("rs(A, B > 0.5)", 0));
        assertEquals("rc(A, 2) * 2", ProgramUpgrade.upgrade("rc(2, A)*2", 0));
        assertEquals("if(rs(rc(B, 2), A), 1, 0)", ProgramUpgrade.upgrade("if(rs(A, rc(2, B)), 1, 0)", 0));
        assertEquals("rs(min(A, B), C)", ProgramUpgrade.upgrade("rs (C, min(A, B))", 0));
        assertEquals("x = rc(A, 0.5) # rc(tau, x)\nrs(x > 0.5, B)", ProgramUpgrade.upgrade("x = rc(0.5, A) # rc(tau, x)\nrs(B, x > 0.5)", 0));
    }

    @Test
    void otherNamesAndNewProgramsStayAsTheyAre() {
        assertEquals("pid(A, B, 1, 0.1, 0) + min(A, B)", ProgramUpgrade.upgrade("pid(A, B, 1, 0.1, 0) + min(A, B)", 0));
        assertEquals("rs(A, B)", ProgramUpgrade.upgrade("rs(A, B)", ProgramUpgrade.VERSION));
        assertEquals("rs(A", ProgramUpgrade.upgrade("rs(A", 0));
        assertEquals("", ProgramUpgrade.upgrade("", 0));
    }

    static final class In implements ISymbole {
        final String name;
        double v;

        In(String name) {
            this.name = name;
        }

        public double getValue() {
            return v;
        }

        public String getName() {
            return name;
        }
    }

    /** The upgraded latch is set by what was the second argument and reset by the first, as in 1.12. */
    @Test
    void anUpgradedLatchBehavesAsBefore() {
        In a = new In("A"), b = new In("B");
        ArrayList<ISymbole> s = new ArrayList<>(List.of(a, b));
        Program p = Program.compile(ProgramUpgrade.upgrade("rs(A, B)", 0), s); // 1.12: reset A, set B
        assertTrue(p.isValid(), p.error);
        b.v = 1;
        assertEquals(1, p.getValue(0.05), 0, "B sets");
        b.v = 0;
        assertEquals(1, p.getValue(0.05), 0, "and it holds");
        a.v = 1;
        assertEquals(0, p.getValue(0.05), 0, "A resets");
    }

    /** The upgraded lag follows what was its second argument with what was its first as the time constant. */
    @Test
    void anUpgradedLagBehavesAsBefore() {
        In a = new In("A");
        Program p = Program.compile(ProgramUpgrade.upgrade("rc(2, A)", 0), new ArrayList<>(List.of(a))); // 1.12: tau 2 s, input A
        assertTrue(p.isValid(), p.error);
        a.v = 1;
        double v = 0;
        for (int i = 0; i < 40; i++) v = p.getValue(0.05);
        assertEquals(1 - Math.exp(-1), v, 0.02, "after one time constant");
    }

    // ---- version 1 -> 2: today's operators (Baughn 2026-10-30)

    /** 1.12's meaning of an old expression (stateless): its own reading of the operators. */
    static double legacy(ProgramUpgrade.N n, java.util.Map<String, Double> in) {
        return switch (n) {
            case ProgramUpgrade.Leaf l -> in.containsKey(l.text()) ? in.get(l.text()) : l.text().equals("pi") ? Math.PI : Double.parseDouble(l.text());
            case ProgramUpgrade.Un u -> u.op().equals("-") ? -legacy(u.x(), in) : 1 - legacy(u.x(), in);
            case ProgramUpgrade.Call c -> {
                double[] a = c.args().stream().mapToDouble(x -> legacy(x, in)).toArray();
                yield switch (c.name()) {
                    case "min" -> Math.min(a[0], a[1]);
                    case "max" -> Math.max(a[0], a[1]);
                    case "abs" -> Math.abs(a[0]);
                    case "if" -> a[0] > 0.5 ? a[1] : a[2];
                    case "pow" -> Math.pow(a[0], a[1]);
                    default -> throw new IllegalArgumentException(c.name());
                };
            }
            case ProgramUpgrade.Bin b -> {
                double x = legacy(b.l(), in), y = legacy(b.r(), in);
                yield switch (b.op()) {
                    case "+" -> x + y;
                    case "-" -> x - y;
                    case "*" -> x * y;
                    case "/" -> x / y;
                    case ">" -> x > y ? 1 : 0;
                    case "<" -> x < y ? 1 : 0;
                    case "=" -> (x > 0.5) == (y > 0.5) ? 1 : 0;
                    case "^" -> (x > 0.5) != (y > 0.5) ? 1 : 0;
                    case "&" -> x > 0.5 && y > 0.5 ? 1 : 0;
                    case "|" -> x > 0.5 || y > 0.5 ? 1 : 0;
                    default -> throw new IllegalArgumentException(b.op());
                };
            }
        };
    }

    static final List<String> OLD = List.of("A = B", "A ^ B & C", "!A * B", "A + B > C = C | A", "-A*B - -C", "A > 0.3 & B < 0.7 | C",
        "!A > 0.2", "A = B ^ C", "1 - A / B * C", "if(A = B, 1, 0)", "max(A, !B)", "A | B & C", "A & B | C = !C", "!(A - B) + 2 * -C",
        "A*B > C*0.5 = A < B", "min(A ^ B, C) / 2", "A > B > C", "-(A + B) * !C ^ A");

    @Test
    void oldOperatorsReadTheSameAfterTheUpgrade() {
        In a = new In("A"), b = new In("B"), c = new In("C");
        java.util.Random rnd = new java.util.Random(7);
        for (String old : OLD) {
            ProgramUpgrade.Reader r = new ProgramUpgrade.Reader(ProgramUpgrade.tokens(old, false), true);
            ProgramUpgrade.N tree = r.low();
            String now = ProgramUpgrade.upgrade(old, 1);
            Program p = Program.compile(now, new ArrayList<>(List.of(a, b, c)));
            assertTrue(p.isValid(), old + " -> " + now + ": " + p.error);
            for (int k = 0; k < 300; k++) {
                // signals near the 0.5 threshold and the 0.01 tolerance, and anywhere
                a.v = k % 3 == 0 ? 0.5 + (rnd.nextInt(5) - 2) * 0.005 : rnd.nextDouble() * 1.4 - 0.2;
                b.v = k % 4 == 0 ? a.v : rnd.nextDouble() * 1.4 - 0.2;
                c.v = k % 5 == 0 ? 0.5 : rnd.nextDouble() * 1.4 - 0.2;
                double want = legacy(tree, java.util.Map.of("A", a.v, "B", b.v, "C", c.v)), got = p.getValue(0.05);
                assertEquals(want, got, 1e-12, old + " -> " + now + " at A " + a.v + " B " + b.v + " C " + c.v);
            }
        }
    }

    @Test
    void theRewriteIsReadable() {
        assertEquals("(A > 0.5) == (B > 0.5)", ProgramUpgrade.upgrade("A = B", 1));
        assertEquals("(A > 0.5) != (B > 0.5) & C", ProgramUpgrade.upgrade("A ^ B & C", 1));
        assertEquals("(1 - A) * B", ProgramUpgrade.upgrade("!A * B", 1));
        // | and & were one level, left to right: (A | B) & C needs its brackets now
        assertEquals("(A | B) & C", ProgramUpgrade.upgrade("A | B & C", 1));
        // what reads the same keeps its text, spaces and all
        assertEquals("A>0.3 &  B<0.7", ProgramUpgrade.upgrade("A>0.3 &  B<0.7", 1));
        assertEquals("x = A*2\n# note\nx + 1", ProgramUpgrade.upgrade("x = A*2\n# note\nx + 1", 1));
        assertEquals("x = (A > 0.5) == (B > 0.5)\nx", ProgramUpgrade.upgrade("x = A = B\nx", 1));
    }

    @Test
    void todaysOperators() {
        In a = new In("A");
        var in = new ArrayList<ISymbole>(List.of(a));
        assertEquals(512, Program.compile("2 ^ 3 ^ 2", in).getValue(0), 1e-9);
        assertEquals(-4, Program.compile("-2 ^ 2", in).getValue(0), 1e-9);
        assertEquals(19, Program.compile("1 + 2 * 3 ^ 2", in).getValue(0), 1e-9);
        a.v = 0.505;
        assertEquals(1, Program.compile("A == 0.5", in).getValue(0), 0);
        assertEquals(0, Program.compile("A != 0.5", in).getValue(0), 0);
        a.v = 0.52;
        assertEquals(0, Program.compile("A == 0.5", in).getValue(0), 0);
        assertEquals(1, Program.compile("A >= 0.52 & A <= 0.52", in).getValue(0), 0);
        // ! is on/off, below the comparisons: !A > 0.6 is !(A > 0.6)
        assertEquals(1, Program.compile("!A > 0.6", in).getValue(0), 0);
        assertEquals(0, Program.compile("!A", in).getValue(0), 0);
        // | below &
        assertEquals(1, Program.compile("1 | 0 & 0", in).getValue(0), 0);
        // a line before the last is a definition only with a single =
        assertFalse(Program.compile("A == 1\n2", in).isValid());
    }

    @Test
    void savedStateFollowsItsFunction() {
        // the old compiler made rs first (its arguments were ready), then integrate: state 0 rs, 1 integrate
        assertEquals(List.of("rs#0", "integrate#0"), Equation.legacyStatefulOrder("integrate(A+1,0)+rs(B,C)"));
        var up = ProgramUpgrade.upgradeAll("integrate(A+1,0)+rs(B,C)", 1);
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putBoolean("equation0state", true);
        t.putDouble("equation1counter", 5);
        ProgramUpgrade.remapState(t, "equation", up.remaps());
        assertTrue(t.getBooleanOr("equation1state", false));
        assertEquals(5, t.getDoubleOr("equation0counter", 0), 0);
        In a = new In("A"), b = new In("B"), c = new In("C");
        Program p = Program.compile(up.program(), new ArrayList<>(List.of(a, b, c)));
        p.readFromNBT(t, "equation");
        assertEquals(6, p.getValue(0), 1e-9, "the integral (5) and the latch (on) as saved");
    }

    @Test
    void pidHoldsItsIntegralWithinLoHi() {
        In a = new In("A");
        Program p = Program.compile("pid(0, A, 0, 1, 0, -1, 1)", new ArrayList<>(List.of(a)));
        a.v = 1;
        double v = 0;
        for (int i = 0; i < 100; i++) v = p.getValue(0.05);
        assertEquals(-1, v, 1e-9, "the integral goes below 0, down to lo (1.12 stopped it at 0)");
    }
}
