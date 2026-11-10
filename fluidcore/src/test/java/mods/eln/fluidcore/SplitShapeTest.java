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
        int steps = 0;
        for (int n = 0; n < 4000; n++) {
            double[] m = new double[SP.length];
            MixFluid f = random(r, m);
            check(f, m, "case " + n + " " + java.util.Arrays.toString(m));
            steps = Math.max(steps, f.splitSteps);
        }
        System.out.println("SplitShape: at most " + steps + " Newton steps to a split's root");
        assertTrue(steps <= 20, "Newton steps to a split's root: " + steps);
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

    static final Species[] GRA = {MixFluidTest.GASOLINE, MixFluidTest.RESIDUE, Species.AIR};
    static final Species[] GDA = {MixFluidTest.GASOLINE, MixFluidTest.DIESEL, Species.AIR};

    /**
     * P5. A first trace of a heavier species changes nothing: the gasoline's liquid and the node's temperature at a
     * given energy are those without it, superheated, at the dew point and wet. Residue never boils; diesel's vapour
     * capacity runs from far below the smoothing's width to far above it over these temperatures. (Either made 0.5 to
     * 0.7 % of a superheated gasoline vapour liquid, and the node up to 1 K warmer.)
     */
    @Test
    void aTraceOfAHeavierSpeciesChangesNothing() {
        for (Species[] sp : new Species[][]{GRA, GDA}) {
            double worstL = 0, worstT = 0, perKg = 0;
            for (double T = 300; T <= 620; T += T > 340 && T < 400 ? 0.25 : 5) {
                double[] m0 = {0.002, 0, 0.001};
                MixFluid clean = MixFluidTest.mf(MixFluidTest.node(sp, 1e-3, m0, T));
                double U = clean.energyAt(m0, T), l0 = clean.st.ml[0].v;
                for (double res : new double[]{1e-28, 1e-20, 1e-12, 1e-9, 1e-7}) {
                    double[] m = {0.002, res, 0.001};
                    MixFluid f = MixFluidTest.mf(MixFluidTest.node(sp, 1e-3, m, T));
                    double dl = Math.abs(f.st.ml[0].v - l0);
                    f.update(m, U);
                    double dT = Math.abs(f.T() - T);
                    if (res >= 1e-9) perKg = Math.max(perKg, dl / res);   // (below it the difference is rounding's)
                    if (res <= 1e-12) {
                        worstL = Math.max(worstL, dl);
                        worstT = Math.max(worstT, dT);
                    }
                    String what = "T " + T + " " + sp[1].key + " " + res;
                    assertTrue(dl <= 400 * res + 1e-15, what + ": gasoline liquid " + f.st.ml[0].v + " against " + l0);
                    assertTrue(dT <= 2e7 * res + 1e-9, what + ": at the same energy T = " + f.T());
                }
            }
            System.out.println("SplitShape: a trace of " + sp[1].key + " moves the gasoline's liquid by " + worstL + " kg, T by " + worstT
                + " K; at most " + perKg + " kg per kg of it");
        }
    }

    /** The slopes hold where the heavier species is a trace and the gasoline at its dew point: against differences of the solve. */
    @Test
    void slopesHoldAtTheDewPointWithATraceOfAHeavierSpecies() {
        for (Species[] sp : new Species[][]{GRA, GDA}) {
            double worst = 0;
            String at = "";
            for (double T = 350; T <= 390; T += 1)
                for (double res : new double[]{1e-10, 1e-8, 1e-6, 1e-4}) {
                    double[] m = {0.002, res, 0.001};
                    Network.Node n = MixFluidTest.node(sp, 1e-3, m, T);
                    MixFluid f = MixFluidTest.mf(n);
                    double[] ps = f.portPSlope(0.5);
                    for (int j = 0; j <= 3; j++) {
                        // (the trace's own step relative to it: the liquid's answer to a trace bends where the trace
                        // is as large as the smoothing's liquid, which may be far below any fixed step)
                        double base = j < 3 ? m[j] : n.U, h = j == 1 ? 1e-3 * base : 1e-6 * Math.abs(base);
                        double[] mp = m.clone(), mm = m.clone();
                        double Up = n.U, Um = n.U;
                        if (j < 3) {
                            mp[j] += h;
                            mm[j] -= h;
                        } else {
                            Up += h;
                            Um -= h;
                        }
                        MixFluid a = new MixFluid(sp, 1e-3, 1, false), b = new MixFluid(sp, 1e-3, 1, false);
                        a.T = b.T = f.T;
                        a.vg = b.vg = f.vg;
                        a.update(mp, Up);
                        b.update(mm, Um);
                        double fd = (a.portP(0.5) - b.portP(0.5)) / (2 * h);
                        double e = Math.abs(fd - ps[j]) / (Math.max(Math.abs(fd), Math.abs(ps[j])) + 1e-3 * f.portP(0.5) / Math.max(1e-6, Math.abs(base)));
                        if (e > worst) {
                            worst = e;
                            at = "dP/d" + j + " at T " + T + " " + sp[1].key + " " + res + ": " + ps[j] + " vs " + fd;
                        }
                    }
                }
            System.out.println("SplitShape slopes: worst " + worst + " at " + at);
            assertTrue(worst < 2e-2, "worst " + worst + " at " + at);
        }
    }
}
