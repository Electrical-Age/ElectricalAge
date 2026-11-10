package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the phase split promises of its shares, whatever the contents: the spec's P statements, in doubles. */
class SplitShapeTest {
    static final Species[] SP = {MixFluidTest.GASOLINE, MixFluidTest.DIESEL, MixFluidTest.RESIDUE, Species.WATER, Species.AIR};

    /** A solved node of random contents: any species may be absent, a trace, or plenty. */
    static MixFluid random(Random r, double[] m) {
        for (int i = 0; i < m.length; i++) {
            double u = r.nextDouble();
            m[i] = u < 0.3 ? 0 : u < 0.5 ? Math.pow(10, -30 + 25 * r.nextDouble()) : Math.pow(10, -4 + 3 * r.nextDouble());
        }
        m[m.length - 1] = 0.001 + 0.004 * r.nextDouble();
        return MixFluidTest.mf(MixFluidTest.node(SP, 1e-3, m, 280 + 320 * r.nextDouble()));
    }

    /** Every share from 0 to 1, a group's shares one whole, no species more liquid or vapour than there is of it. */
    static void check(MixFluid f, double[] m, String what) {
        MixFluid.Eval e = f.st;
        assertTrue(Double.isFinite(e.p.v) && Double.isFinite(f.T()), what + ": p " + e.p.v + " T " + f.T());
        for (int g = 0; g < f.groups.length; g++) {
            double sum = 0;
            for (int a = 0; a < f.groups[g].length; a++) {
                int i = f.groups[g][a];
                double x = e.x[g][a].v, tol = 1e-12 * Math.abs(m[i]);
                assertTrue(x >= 0 && x <= 1 + 1e-9, what + ": share " + x + " of " + SP[i].key);
                sum += x;
                assertTrue(e.ml[i].v >= Math.min(0, m[i]) - tol && e.ml[i].v <= Math.max(0, m[i]) + tol, what + ": liquid " + e.ml[i].v + " of " + m[i] + " " + SP[i].key);
                assertTrue(e.mv[i].v >= Math.min(0, m[i]) - tol && e.mv[i].v <= Math.max(0, m[i]) + tol, what + ": vapour " + e.mv[i].v + " of " + m[i] + " " + SP[i].key);
                assertEquals(m[i], e.ml[i].v + e.mv[i].v, tol, what + ": parts of " + SP[i].key);
            }
            assertEquals(1, sum, 1e-9, what + ": shares of group " + g);
        }
    }

    /** P4. Random contents, traces and absences included: the shares keep their bounds. */
    @Test
    void sharesKeepTheirBounds() {
        Random r = new Random(11);
        for (int n = 0; n < 4000; n++) {
            double[] m = new double[SP.length];
            check(random(r, m), m, "case " + n + " " + java.util.Arrays.toString(m));
        }
    }

    /**
     * Masses that rounding left a little below zero (the limiter allows 1e-15 of a cell) count as none: no negative
     * share, no share of 1e16, whatever else the group holds.
     */
    @Test
    void negativeTracesCountAsNone() {
        Random r = new Random(12);
        for (int n = 0; n < 4000; n++) {
            double[] m = new double[SP.length];
            MixFluid f = random(r, m);
            double U = f.energyAt(m, f.T());
            for (int i = 0; i < m.length - 1; i++) if (r.nextDouble() < 0.4) m[i] = -Math.pow(10, -32 + 14 * r.nextDouble());
            f.update(m, U);
            check(f, m, "case " + n + " " + java.util.Arrays.toString(m));
        }
    }
}
