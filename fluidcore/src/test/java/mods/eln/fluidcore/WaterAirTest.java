package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The fluid core's water/air thermodynamics (M1): its boiling curve, solves, derivatives and smoothness. */
class WaterAirTest {
    @Test
    void boilsOnTheSteamTablesCurveWithARealLatentHeat() {
        // steam tables: saturation pressure (kPa) and latent heat (kJ/kg)
        double[][] table = {{293.15, 2.339, 2453.5}, {323.15, 12.35, 2382.0}, {373.15, 101.42, 2256.4}, {423.15, 476.16, 2113.7},
            {473.15, 1554.9, 1939.7}, {523.15, 3976.2, 1715.5}, {573.15, 8587.9, 1404.9}};
        for (double[] r : table) {
            double p = WaterAir.pSat(r[0], r[1] * 1e3);
            assertTrue(Math.abs(p / (r[1] * 1e3) - 1) < 1e-3 + r[1] * 1e3 / WaterAir.K, r[0] + " K: " + p + " Pa vs " + r[1] + " kPa");
            double latent = WaterAir.hVapour(r[0]) - (WaterAir.uLiquidThermal(r[0]) + WaterAir.P0 * WaterAir.V0);
            // (an ideal-gas vapour: consistency then makes the latent heat Clausius-Clapeyron's with v = R T / p, which
            // overstates it: +1.6 % at 100 C, +4.8 % at 150, +11 % at 200, +24 % at 250, where real steam is far from ideal.
            // A virial term in the vapour would fix it; open.)
            double bound = r[0] <= 423.15 ? 0.05 : r[0] <= 473.15 ? 0.12 : 0.6;
            assertTrue(Math.abs(latent / (r[2] * 1e3) - 1) < bound, r[0] + " K: latent " + latent + " vs " + r[2] + " kJ/kg");
        }
    }

    /** Random states: water and air from none to a lot, liquid share from dry to full, cold to hot. */
    static double[] state(Random r) {
        double V = Math.pow(10, -4 + 3 * r.nextDouble());
        double fill = r.nextDouble() < 0.2 ? 0.97 + 0.03 * r.nextDouble() : r.nextDouble();
        double mw = r.nextDouble() < 0.1 ? 0 : fill * V / WaterAir.V0;
        double ma = r.nextDouble() < 0.3 ? 0 : (0.2 + 20 * r.nextDouble()) * V;
        double T = 280 + 220 * r.nextDouble();
        return new double[]{mw, ma, V, T};
    }

    @Test
    void solveRecoversTheTemperatureAnEnergyCameFrom() {
        Random r = new Random(1);
        for (int k = 0; k < 2000; k++) {
            double[] s = state(r);
            double U = WaterAir.energyAt(s[0], s[1], s[2], s[3]);
            var sp = WaterAir.solve(s[0], s[1], U, s[2], 300);
            assertEquals(s[3], sp.T(), 1e-6, "case " + k + " " + java.util.Arrays.toString(s));
            assertEquals(s[2], sp.vg() + sp.vl(), 1e-9 * s[2], "volume, case " + k);
            assertTrue(sp.ml() >= 0 && sp.mv() >= 0 && sp.p() > 0, "case " + k + " " + sp);
        }
    }

    @Test
    void derivativesMatchFiniteDifferencesOfTheSolve() {
        Random r = new Random(2);
        int checked = 0;
        for (int k = 0; k < 400; k++) {
            double[] s = state(r);
            double mw = s[0], ma = s[1], V = s[2];
            double U = WaterAir.energyAt(mw, ma, V, s[3]);
            var sp = WaterAir.solve(mw, ma, U, V, s[3]);
            double[][] d = WaterAir.derivatives(sp, mw, ma, U, V);
            double[] base = {mw, ma, U};
            for (int j = 0; j < 3; j++) {
                if (j < 2 && base[j] == 0) continue;   // (a one-sided edge: no central difference)
                double h = j == 2 ? 1e-6 * Math.max(1, Math.abs(U)) : 1e-6 * base[j];
                double[] p = base.clone(), m = base.clone();
                p[j] += h;
                m[j] -= h;
                var sp1 = WaterAir.solve(p[0], p[1], p[2], V, sp.T());
                var sm1 = WaterAir.solve(m[0], m[1], m[2], V, sp.T());
                double fdT = (sp1.T() - sm1.T()) / (2 * h);
                double scale = Math.max(Math.abs(fdT), Math.abs(d[0][j])) + 1e-9 * sp.T() / Math.max(1e-30, Math.abs(base[j]) + 1);
                assertEquals(fdT, d[0][j], 2e-3 * scale + 1e-12, "dT/d" + j + ", case " + k + " " + java.util.Arrays.toString(s));
                checked++;
            }
        }
        assertTrue(checked > 600, "checked " + checked);
    }

    /**
     * The pressure's slope along a sweep of energy through the last drop evaporating has no jump: halving the sample
     * spacing four times shrinks the largest change of slope between neighbouring samples (at a kink it would not).
     */
    @Test
    void smoothWhereTheLastDropEvaporates() {
        // (the last drop goes over about 10 J here: dP/dU climbs from ~140 to ~310 Pa/J across it)
        double coarse = slopeJump(1), fine = slopeJump(0.25);
        assertTrue(fine < 0.5 * coarse, "largest slope change per sample: " + coarse + " at 1 J, " + fine + " at 0.25 J");
    }

    /** The largest change of dP/dU between neighbouring samples h apart, across the last drop (U 5.10-5.17 kJ). */
    static double slopeJump(double h) {
        double V = 1e-3, mw = 0.002, ma = 0;
        double prevT = 400, prevSlope = Double.NaN, maxJump = 0;
        for (double U = 5.10e3; U <= 5.17e3; U += h) {
            var a = WaterAir.solve(mw, ma, U, V, prevT);
            var b = WaterAir.solve(mw, ma, U + 1e-3 * h, V, a.T());
            double slope = (b.p() - a.p()) / (1e-3 * h);
            if (!Double.isNaN(prevSlope)) maxJump = Math.max(maxJump, Math.abs(slope - prevSlope));
            prevSlope = slope;
            prevT = a.T();
        }
        return maxJump;
    }

    /** Filling a volume with water: the pressure rises smoothly through the last of the gas space into compressed liquid. */
    @Test
    void smoothIntoACompressedFullVolume() {
        double V = 1e-3, ma = 1.2e-3, T = 300;
        double prevP = 0, prevSlope = Double.NaN, maxJump = 0;
        for (int i = 0; i <= 400; i++) {
            double mw = (0.95 + 0.06 * i / 400.0) * V / WaterAir.V0;   // (95 % to 101 % of the uncompressed volume)
            double U = WaterAir.energyAt(mw, ma, V, T);
            var s = WaterAir.solve(mw, ma, U, V, T);
            assertTrue(s.p() > prevP, "pressure rises: " + s.p() + " after " + prevP + " at " + i);
            double slope = s.p() - prevP;
            if (i > 1) maxJump = Math.max(maxJump, Math.abs(slope / prevSlope - 1));
            prevSlope = slope;
            prevP = s.p();
        }
        assertTrue(maxJump < 0.5, "dP/dm jumped by a factor " + maxJump);
    }

    /** Two-phase: the vapour's partial pressure is the saturation pressure (Gibbs equality) to within the smoothing. */
    @Test
    void twoPhaseVapourSitsOnTheSaturationCurve() {
        Random r = new Random(3);
        for (int k = 0; k < 500; k++) {
            double V = 1e-3, T = 300 + 200 * r.nextDouble(), mw = (0.05 + 0.8 * r.nextDouble()) * V / WaterAir.V0, ma = r.nextDouble() * 1e-3;
            var s = WaterAir.solve(mw, ma, WaterAir.energyAt(mw, ma, V, T), V, T);
            double pv = s.mv() * WaterAir.R_V * s.T() / s.vg();
            assertEquals(WaterAir.pSat(s.T(), s.p()), pv, 1e-4 * pv, "case " + k);
        }
    }

    @Test
    void outputSlopesMatchFiniteDifferences() {
        Random r = new Random(4);
        String[] names = {"P", "ml", "mv", "ma", "hL", "hG", "level", "yv", "ya"};
        int checked = 0;
        for (int k = 0; k < 300; k++) {
            double[] s = state(r);
            double mw = s[0], ma = s[1], V = s[2];
            if (mw == 0 || ma == 0) continue;
            double U = WaterAir.energyAt(mw, ma, V, s[3]);
            var sp = WaterAir.solve(mw, ma, U, V, s[3]);
            double[][] d = WaterAir.outputSlopes(sp, mw, ma, U, V);
            double[] base = {mw, ma, U};
            for (int j = 0; j < 3; j++) {
                double h = j == 2 ? 1e-5 * Math.max(1, Math.abs(U)) : 1e-5 * base[j];   // (1e-5: larger steps keep the solve's noise down)
                double[] p = base.clone(), m = base.clone();
                p[j] += h;
                m[j] -= h;
                double[] op = WaterAir.outputs(WaterAir.solve(p[0], p[1], p[2], V, sp.T()), V);
                double[] om = WaterAir.outputs(WaterAir.solve(m[0], m[1], m[2], V, sp.T()), V);
                double[] o0 = WaterAir.outputs(sp, V);
                for (int o = 0; o < WaterAir.OUTPUTS; o++) {
                    double fd = (op[o] - om[o]) / (2 * h);
                    // (scale: the output's own size over the input's, so tiny outputs aren't held to noise)
                    double scale = Math.max(Math.abs(fd), Math.abs(d[o][j])) + 2e-5 * Math.abs(o0[o]) / Math.max(1e-30, Math.abs(base[j]));   // (the solve resolves T to 1e-11 K: a finite difference of a tiny output carries that noise)
                    assertEquals(fd, d[o][j], 2e-2 * scale + 1e-14, "d" + names[o] + "/d" + j + ", case " + k + " " + java.util.Arrays.toString(s));
                    checked++;
                }
            }
        }
        assertTrue(checked > 2000, "checked " + checked);
    }
}
