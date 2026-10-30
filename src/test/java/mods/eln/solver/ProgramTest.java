package mods.eln.solver;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The signal processor's programs (Baughn 2026-10-30): named components, the output line, cost, state, the rs latch. */
public class ProgramTest {
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

    final In a = new In("A"), b = new In("B"), c = new In("C");

    List<ISymbole> inputs() {
        return new ArrayList<>(List.of(a, b, c));
    }

    @Test
    void anOldExpressionIsStillAProgram() {
        Program p = Program.compile("A*0.5", inputs());
        assertTrue(p.isValid(), p.error);
        a.v = 0.8;
        assertEquals(0.4, p.getValue(0.05), 1e-12);
        // = is still "equals" in the output line
        Program eq = Program.compile("A=B", inputs());
        assertTrue(eq.isValid(), eq.error);
        assertEquals(1, eq.getOperatorCount());
    }

    @Test
    void componentsAreComputedOnceAndPaidForOnce() {
        Program p = Program.compile("x = A*0.5 + 0.1\n\n# a comment\ny = x*2\nx + y + x", inputs());
        assertTrue(p.isValid(), p.error);
        a.v = 0.8;
        assertEquals(0.5 + 1.0 + 0.5, p.getValue(0.05), 1e-12);
        Program x = Program.compile("A*0.5 + 0.1", inputs()), y = Program.compile("A*2", inputs()), out = Program.compile("A + B + A", inputs());
        assertEquals(x.getOperatorCount() + y.getOperatorCount() + out.getOperatorCount(), p.getOperatorCount(), "each line once");
        // ; separates statements too
        assertEquals(p.getOperatorCount(), Program.compile("x = A*0.5 + 0.1; y = x*2; x + y + x", inputs()).getOperatorCount());
    }

    @Test
    void aStatefulComponentKeepsOneState() {
        // read three times, the integrator still steps once a tick
        Program p = Program.compile("i = integrate(1, 0)\ni + i + i", inputs());
        assertTrue(p.isValid(), p.error);
        double v = 0;
        for (int t = 0; t < 20; t++) v = p.getValue(0.05);
        Program one = Program.compile("integrate(1, 0)", inputs());
        double w = 0;
        for (int t = 0; t < 20; t++) w = one.getValue(0.05);
        assertEquals(3 * w, v, 1e-9);
    }

    @Test
    void stateIsSavedUnderTheComponentsName() {
        Program p = Program.compile("i = integrate(1, 0)\nj = integrate(2, 0)\ni + j", inputs());
        for (int t = 0; t < 20; t++) p.getValue(0.05);
        CompoundTag nbt = new CompoundTag();
        p.writeToNBT(nbt, "equation");
        // another line edited, the integrators keep their state
        Program q = Program.compile("j = integrate(2, 0)\nk = A\ni = integrate(1, 0)\ni + j + k", inputs());
        q.readFromNBT(nbt, "equation");
        a.v = 0;
        assertEquals(p.getValue(0), q.getValue(0), 1e-9);
    }

    @Test
    void definitionsComeFirstAndAreChecked() {
        Program fwd = Program.compile("x = y + 1\ny = A\nx", inputs());
        assertFalse(fwd.isValid());
        assertEquals(1, fwd.errorLine);
        assertTrue(fwd.error.contains("y is not defined above"), fwd.error);
        Program notDef = Program.compile("A + 1\nB", inputs());
        assertFalse(notDef.isValid());
        assertTrue(notDef.error.contains("definition"), notDef.error);
        assertFalse(Program.compile("A = B\nA", inputs()).isValid(), "an input's name");
        assertFalse(Program.compile("sin = A\nsin", inputs()).isValid(), "a function's name");
        assertFalse(Program.compile("x = A\nx = B\nx", inputs()).isValid(), "twice");
        assertFalse(Program.compile("", inputs()).isValid());
        assertNull(Program.compile("x = A\nx", inputs()).error);
    }

    @Test
    void tooBigIsRefused() {
        assertTrue(Program.tooBig("A".repeat(Program.MAX_CHARS + 1)) != null);
        assertTrue(Program.tooBig("A\n".repeat(Program.MAX_LINES)) != null);
        assertNull(Program.tooBig("x = A\nx"));
        assertFalse(Program.compile("A\n".repeat(Program.MAX_LINES + 1), inputs()).isValid());
    }

    @Test
    void theLatchStepsEvenInAnIfsBranchNotTaken() {
        // rs(set, reset): set by B while the if shows 0; then the if shows the latch (it stepped while unread)
        Program p = Program.compile("if(A, rs(B, 0), 0)", inputs());
        assertTrue(p.isValid(), p.error);
        a.v = 0;
        b.v = 1;
        assertEquals(0, p.getValue(0.05), 0);
        a.v = 1;
        b.v = 0;
        assertEquals(1, p.getValue(0.05), 0, "latched while its branch wasn't taken");
    }

    @Test
    void anInputMayHaveADefault() {
        Program p = Program.compile("A = 0.25 * 2\nB = 1\nA + B", inputs(), java.util.Set.of("A", "B", "C"));
        assertTrue(p.isValid(), p.error);
        assertEquals(0.5, p.defaultOf("A"), 1e-12);
        assertEquals(1, p.defaultOf("B"), 0);
        assertTrue(Double.isNaN(p.defaultOf("C")));
        assertEquals(0, p.getOperatorCount() - 1, "a default costs nothing; A + B one operator");
        Program twice = Program.compile("A = 1\nA = 0\nA", inputs(), java.util.Set.of("A"));
        assertEquals("line 2: A's default is already set", twice.error);
        assertEquals("A", twice.errorToken);
        assertTrue(Program.compile("A = B\nA", inputs(), java.util.Set.of("A")).error.contains("must be a number"));
        // without defaults allowed, A is an input already
        assertTrue(Program.compile("A = 1\nA", inputs()).error.contains("already defined"));
    }
}
