package mods.eln.solver;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProgramUpgradeTest {
    @Test
    void oldCallsSwapTheirArguments() {
        assertEquals("rs(B,0)", ProgramUpgrade.upgrade("rs(0,B)", 0));
        assertEquals("rs(B > 0.5, A)", ProgramUpgrade.upgrade("rs(A, B > 0.5)", 0));
        assertEquals("rc(A, 2)*2", ProgramUpgrade.upgrade("rc(2, A)*2", 0));
        assertEquals("if(rs(rc(B, 2), A), 1, 0)", ProgramUpgrade.upgrade("if(rs(A, rc(2, B)), 1, 0)", 0));
        assertEquals("rs (min(A, B), C)", ProgramUpgrade.upgrade("rs (C, min(A, B))", 0));
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
}
