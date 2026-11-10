package mods.eln.solver;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The signal processor's stateful functions hold their state through a non-finite input (Equation.finiteOr0): 0/0
 * from two unwired inputs made rc, integrate, pid and derivate NaN for good, and the NaN was saved.
 */
public class StatefulNanTest {
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

    /** Steps `program` once with A = B = 0 (0/0), then 40 times with A = 0.5, B = 1; the last output. */
    double afterNan(String program) {
        Program p = Program.compile(program, inputs());
        assertTrue(p.isValid(), program + ": " + p.error);
        a.v = 0;
        b.v = 0;
        p.getValue(0.05);
        a.v = 0.5;
        b.v = 1;
        double v = 0;
        for (int t = 0; t < 40; t++) v = p.getValue(0.05);
        return v;
    }

    @Test
    void oneNanInputDoesNotStick() {
        assertEquals(0.5, afterNan("rc(A/B, 0.1)"), 1e-3, "rc follows A/B again");
        assertEquals(0.5 * 0.05 * 40, afterNan("integrate(A/B, 0)"), 1e-9, "integrate sums only the finite steps");
        assertEquals(1, afterNan("integrate(A/B, 0, 1)"), 1e-9, "integrate with bounds");
        assertEquals(0, afterNan("derivate(A/B)"), 1e-9, "derivate of a constant");
        double pid = afterNan("pid(A/B, C, 1, 1, 0)");
        assertTrue(Double.isFinite(pid) && pid > 0.5, "pid works again: " + pid);
        double pidMinMax = afterNan("pid(A/B, C, 1, 1, 0, -2, 2)");
        assertTrue(Double.isFinite(pidMinMax) && pidMinMax > 0.5, "pid with bounds works again: " + pidMinMax);
        // 1/0 = infinity: rc used to reach NaN one step later (inf - inf)
        Program p = Program.compile("rc(A/B, 0.1)", inputs());
        a.v = 1;
        b.v = 0;
        p.getValue(0.05);
        p.getValue(0.05);
        a.v = 0.5;
        b.v = 1;
        double v = 0;
        for (int t = 0; t < 40; t++) v = p.getValue(0.05);
        assertEquals(0.5, v, 1e-3, "rc after an infinite input");
    }

    @Test
    void aNanTauHoldsRc() {
        Program p = Program.compile("rc(A, B/C)", inputs());
        a.v = 1;
        b.v = 0;
        c.v = 0;
        p.getValue(0.05);
        b.v = 0.1;
        c.v = 1;
        double v = 0;
        for (int t = 0; t < 40; t++) v = p.getValue(0.05);
        assertEquals(1, v, 1e-3);
    }

    @Test
    void aNanInASaveLoadsAsZero() {
        String[] programs = {"rc(A, 1)", "integrate(A, 0)", "integrate(A, 0, 1)", "derivate(A)", "pid(A, B, 1, 1, 1)", "ramp(1)"};
        for (String src : programs) {
            Program p = Program.compile(src, inputs());
            for (int t = 0; t < 3; t++) p.getValue(0.05);
            CompoundTag nbt = new CompoundTag();
            p.writeToNBT(nbt, "e");
            CompoundTag poisoned = new CompoundTag();
            for (String k : nbt.keySet())
                poisoned.put(k, nbt.get(k) instanceof net.minecraft.nbt.DoubleTag ? net.minecraft.nbt.DoubleTag.valueOf(Double.NaN) : nbt.get(k).copy());
            Program q = Program.compile(src, inputs());
            q.readFromNBT(poisoned, "e");
            a.v = 0.5;
            b.v = 0.25;
            double v = q.getValue(0.05);
            assertTrue(Double.isFinite(v), src + " loaded a NaN: " + v + " from " + poisoned);
        }
    }
}
