package mods.eln.solver;

import mods.eln.sixnode.electricalmath.ElectricalMathElement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two signal processors on Incognito (snapshot 2026-10-30 ~21:45, saved without programVersion, redstone in their
 * slots): their load conversion, dry run. The program is kept or rewritten, the redstone becomes cheap chips, the chips
 * cover the program, and the output is what it was over a sweep of inputs (batteryCharge now stops at 1).
 */
class IncognitoProgramsTest {
    static final String BATTERY = "batteryCharge(((B * 400) + ((C - 0.5) * 100 * 0.55)) / 200)", COMPARE = "(C<0.0528*2)|(A<C)";

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

    Program load(String saved) {
        String now = ProgramUpgrade.upgrade(saved, 0);
        assertEquals(saved, now, "reads the same today: kept as written");
        Program p = Program.compile(now, new ArrayList<>(List.of(a, b, c)), ElectricalMathElement.DEFAULTABLE);
        assertTrue(p.isValid(), p.error);
        return p;
    }

    @Test
    void chipsFromTheirRedstoneCoverTheirPrograms() {
        assertEquals(8, ElectricalMathElement.chipsFor(32), "32 redstone -> 8 cheap chips");
        assertEquals(8, ElectricalMathElement.chipsFor(30), "30 redstone -> 8 cheap chips");
        int battery = ElectricalMathElement.chipsFor(load(BATTERY).getOperatorCount()), compare = ElectricalMathElement.chipsFor(load(COMPARE).getOperatorCount());
        assertTrue(battery <= 8, battery + " chips needed");
        assertTrue(compare <= 8, compare + " chips needed");
        // (what they need: 14 and 4 operators, 4 and 1 chips; 1.12 charged one redstone an operator, so 14 and 4 redstone)
        assertEquals(14, load(BATTERY).getOperatorCount());
        assertEquals(4, load(COMPARE).getOperatorCount());
    }

    @Test
    void theComparisonReadsTheSame() {
        Program p = load(COMPARE);
        ProgramUpgrade.N tree = new ProgramUpgrade.Reader(ProgramUpgrade.tokens(COMPARE, false), true).low();
        for (int i = 0; i <= 60; i++)
            for (int j = 0; j <= 60; j++) {
                a.v = i / 50.0 - 0.1;
                c.v = j / 50.0 - 0.1;
                double want = ProgramUpgradeTest.legacy(tree, Map.of("A", a.v, "B", 0.0, "C", c.v));
                assertEquals(want, p.getValue(0.05), 0, "A " + a.v + " C " + c.v);
            }
    }

    /** 1.12's batteryCharge, uncapped: what the processor computed before. */
    static double oldBatteryCharge(double probeU) {
        var uFq = mods.eln.Eln.batteryVoltageFunctionTable;
        double dq = 0.001, q = 0, eMax = 0;
        while (q <= 1.0) {
            eMax += uFq.getValue(q) * dq;
            q += dq;
        }
        if (probeU > 1.5) return 1;
        q = 0;
        double e = 0, u;
        while ((u = uFq.getValue(q)) < probeU) {
            e += u * dq;
            q += dq;
        }
        return e / eMax;
    }

    @Test
    void theBatteryGaugeReadsTheSameUpToFull() {
        assertNotNull(mods.eln.Eln.batteryVoltageFunctionTable, "the battery curve (registered with the devices)");
        Program p = load(BATTERY);
        int capped = 0;
        for (int i = 0; i <= 50; i++)
            for (int j = 0; j <= 20; j++) {
                b.v = i / 50.0;
                c.v = j / 20.0;
                double old = oldBatteryCharge(((b.v * 400) + ((c.v - 0.5) * 100 * 0.55)) / 200);
                double now = p.getValue(0.05);
                if (old > 1) {
                    capped++;
                    assertEquals(1, now, 0, "past full it now reads 1 (it read " + old + ")");
                } else assertEquals(old, now, 1e-9, "B " + b.v + " C " + c.v);
            }
        // and its saved inputs (B 28.71 V, C 25.00 V): the gauge reads full, as its saved 50 V output
        b.v = 28.709714889526367 / 50;
        c.v = 24.99593734741211 / 50;
        assertEquals(1, p.getValue(0.05), 0);
        System.out.println("batteryCharge: " + capped + " of 1071 sweep points were above 1 before and read 1 now");
    }
}
