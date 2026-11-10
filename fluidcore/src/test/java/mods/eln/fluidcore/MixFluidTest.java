package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MixFluid with the game's species (numbers from data/eln/eln/fluid_species/*.json): 10-species-plan.md tests 1-5. */
class MixFluidTest {
    static final Species GASOLINE = Species.curve("eln:gasoline", "hydrocarbon", 740, 2200, 1700, 0.1, 370, 350000);
    static final Species DIESEL = Species.curve("eln:diesel", "hydrocarbon", 840, 2000, 1700, 0.2, 530, 250000);
    static final Species BUTADIENE = Species.curve("eln:butadiene", "hydrocarbon", 615, 2270, 1470, 0.054, 268.7, 416000);
    static final Species RESIDUE = Species.none("eln:residue", "hydrocarbon", 1050, 1800, 0.6);
    static final Species NATURAL_GAS = Species.gas("eln:natural_gas", 1700, 0.016);

    static Network.Node node(Species[] sp, double V, double[] m, double T) {
        MixFluid.DEBUG_NAN = true;
        return new Network.Node(V, 0, 1, false, sp).fillMasses(m, T);
    }

    static MixFluid mf(Network.Node n) {
        return (MixFluid) n.fluid;
    }

    /** 1. A CURVE species boils at tb under 1 atm, with its latent heat there, and follows Clausius-Clapeyron. */
    @Test
    void curveSpeciesBoilsWhereItsDataSays() {
        for (Species s : new Species[]{GASOLINE, DIESEL, BUTADIENE}) {
            assertEquals(0, s.lnPsCurve(s.tb, WaterAir.P0), 1e-12, s + " boils at tb");
            for (double T = s.tb - 60; T <= s.tb + 150; T += 30) {
                double h = 1e-3, dl = (s.lnPsCurve(T + h, WaterAir.P0) - s.lnPsCurve(T - h, WaterAir.P0)) / (2 * h);
                double L = s.lb + (s.cpV - s.cpL) * (T - s.tb);   // (Kirchhoff, ideal vapour)
                assertEquals(L / (s.r * T * T), dl, 2e-3 * dl, s + " Clausius-Clapeyron at " + T);
            }
        }
    }

    /** 1. A sealed node of gasoline and air: its energy rises with temperature (the solve is unique), and T comes back. */
    @Test
    void energyRisesWithTemperatureAndSolvesBack() {
        Species[] sp = {GASOLINE, Species.AIR};
        double V = 1e-3;
        double[] m = {0.4, 0.002};
        Network.Node n = node(sp, V, m, 300);
        double last = -Double.MAX_VALUE;
        for (double T = 250; T <= 1400; T += 25) {
            double E = mf(n).energyAt(m, T);
            assertTrue(E > last, "energy rises at " + T + ": " + E + " after " + last);
            last = E;
            Network.Node b = node(sp, V, m, T);
            assertEquals(T, b.T(), 1e-6, "solves back at " + T);
        }
    }

    /** 2. Two condensables in one group: each vapour's partial pressure is its liquid mole fraction times its own curve. */
    @Test
    void raoultInOneGroup() {
        Species[] sp = {GASOLINE, DIESEL, Species.AIR};
        double V = 1e-3;
        for (double T : new double[]{330, 380, 430}) {
            Network.Node n = node(sp, V, new double[]{0.2, 0.3, 0.001}, T);
            MixFluid f = mf(n);
            MixFluid.Eval e = f.st;
            for (int a = 0; a < 2; a++) {
                double x = e.x[0][a].v, ps = WaterAir.P0 * Math.exp(sp[a].lnPsCurve(T, e.p.v));
                assertEquals(x * ps, e.pv[a].v, 1e-3 * x * ps, sp[a] + " at " + T + " K");
            }
            assertEquals(1, e.x[0][0].v + e.x[0][1].v, 1e-12, "mole fractions sum to 1");
        }
    }

    /** 3. Water and gasoline layer: gasoline floats; a floor port draws water, one high in the liquid gasoline, and the interface a mix. */
    @Test
    void immiscibleLiquidsLayer() {
        Species[] sp = {Species.WATER, GASOLINE, Species.AIR};
        double V = 1e-3;
        Network.Node n = node(sp, V, new double[]{0.3, 0.2, 0.001}, 300);
        MixFluid f = mf(n);
        double lw = 0.3 * WaterAir.V0 / V, lg = 0.2 / 740 / V;
        assertEquals(lw, f.st.top[0].v, 0.01, "water is the bottom layer");
        assertEquals(lw + lg, f.level(), 0.01, "gasoline on top");
        double[] floor = f.draw(0), high = f.draw(lw + 0.8 * lg), mid = f.draw(lw - Network.BAND / 2), top = f.draw(1);
        assertEquals(1, floor[0], 1e-6, "the floor port draws water");
        assertEquals(1, high[1], 1e-3, "a port in the gasoline draws gasoline: " + java.util.Arrays.toString(high));
        assertTrue(mid[0] > 0.05 && mid[1] > 0.05, "at the interface, a mix: " + java.util.Arrays.toString(mid));
        assertTrue(top[2] > 0.3, "the top port draws gas: " + java.util.Arrays.toString(top));
        // port pressure: the floor sees both layers' heads
        double head = f.portP(0) - f.P(), want = Network.G * (0.3 + 0.2) / V;   // (rho g h over the whole column = g m / A, A = V / H, H = 1)
        assertEquals(want, head, 0.02 * want, "head over the floor port");
    }

    /** 3. A trace of a bottom layer at a floor port: every draw share stays in [0, 1]. */
    @Test
    void thinBottomLayerDrawsNoNegatives() {
        Species[] sp = {Species.WATER, GASOLINE, Species.AIR};
        for (double mw : new double[]{0, 1e-9, 1e-6, 1e-4}) {
            Network.Node n = node(sp, 1e-3, new double[]{mw, 0.2, 0.001}, 300);
            for (double f : new double[]{0, 0.001, 0.01, 0.5, 1}) {
                double[] d = mf(n).draw(f);
                for (int i = 0; i < 3; i++) assertTrue(d[i] >= -1e-12 && d[i] <= 1 + 1e-12, "mw " + mw + " f " + f + ": " + java.util.Arrays.toString(d));
            }
        }
    }

    /** 4. Residue never boils, and it lowers gasoline's vapour pressure (Raoult). */
    @Test
    void residueStaysLiquidAndDilutes() {
        Species[] sp = {GASOLINE, RESIDUE, Species.AIR};
        Network.Node hot = node(sp, 1e-3, new double[]{0.01, 0.3, 0.001}, 700);
        assertEquals(0.3, mf(hot).st.ml[1].v, 1e-9, "residue liquid at 700 K");
        Network.Node alone = node(sp, 1e-3, new double[]{0.2, 0, 0.001}, 360);
        Network.Node mixed = node(sp, 1e-3, new double[]{0.2, 0.6, 0.001}, 360);
        assertTrue(mf(mixed).st.pv[0].v < 0.8 * mf(alone).st.pv[0].v, "diluted: " + mf(mixed).st.pv[0].v + " vs " + mf(alone).st.pv[0].v);
    }

    /** 5. The slopes against one-sided or central differences of the solve, on random states of several species (zeros included). */
    @Test
    void slopesMatchDifferencesOfTheSolve() {
        Species[] sp = {Species.WATER, GASOLINE, DIESEL, RESIDUE, Species.AIR, NATURAL_GAS};
        Random r = new Random(11);
        double worst = 0;
        String at = "";
        for (int k = 0; k < 120; k++) {
            double V = Math.pow(10, -3.5 + 2 * r.nextDouble());
            double[] m = new double[sp.length];
            for (int i = 0; i < sp.length; i++) {
                if (r.nextDouble() < 0.35) continue;
                m[i] = sp[i].liquidCapable() ? r.nextDouble() * 0.25 * V * sp[i].rho : r.nextDouble() * 5 * V;
            }
            double T = 290 + 250 * r.nextDouble();
            Network.Node n = node(sp, V, m, T);
            MixFluid f = mf(n);
            double U = n.U;
            for (double port : new double[]{0, 0.3, 1}) {
                double[] ps = f.portPSlope(port);
                double[][] ds = f.drawSlope(port);
                for (int j = 0; j <= sp.length; j++) {
                    double base = j < sp.length ? m[j] : U;
                    double h = j < sp.length ? Math.max(1e-9, 1e-6 * base) : 1e-6 * Math.max(1, Math.abs(U));
                    boolean forward = j < sp.length && base - h < 0;
                    double[] mp = m.clone(), mm = m.clone();
                    double Up = U, Um = U;
                    if (j < sp.length) {
                        mp[j] += h;
                        if (!forward) mm[j] -= h;
                    } else {
                        Up += h;
                        Um -= h;
                    }
                    MixFluid a = new MixFluid(sp, V, 1, false), b = new MixFluid(sp, V, 1, false);
                    a.T = b.T = f.T;
                    a.vg = b.vg = f.vg;
                    a.update(mp, Up);
                    b.update(mm, Um);
                    double den = forward ? h : 2 * h;
                    double fdP = (a.portP(port) - b.portP(port)) / den;
                    double scaleP = Math.max(Math.abs(fdP), Math.abs(ps[j])) + 1e-3 * Math.abs(f.portP(port)) / Math.max(1e-6, Math.abs(base));
                    double e = Math.abs(fdP - ps[j]) / scaleP;
                    if (e > worst) {
                        worst = e;
                        at = "dP(" + port + ")/d" + j + " case " + k + ": " + ps[j] + " vs " + fdP + " V " + V + " m " + java.util.Arrays.toString(m) + " T " + f.T + " level " + f.level() + " P " + f.P();
                    }
                    double[] da = a.draw(port), db = b.draw(port), d0 = f.draw(port);
                    for (int c = 0; c <= sp.length; c++) {
                        double fd = (da[c] - db[c]) / den;
                        double sc = Math.max(Math.abs(fd), Math.abs(ds[c][j])) + 1e-3 * (Math.abs(d0[c]) + (c == sp.length ? 1e3 : 1e-3)) / Math.max(1e-6, Math.abs(base));
                        double ec = Math.abs(fd - ds[c][j]) / sc;
                        if (ec > worst) {
                            worst = ec;
                            at = "d draw" + c + "(" + port + ")/d" + j + " case " + k + ": " + ds[c][j] + " vs " + fd;
                        }
                    }
                }
            }
        }
        System.out.println("MixFluid slopes: worst " + worst + " at " + at);
        assertTrue(worst < 2e-2, "worst " + worst + " at " + at);
    }

    /** 6. Water and air through MixFluid give WaterAirFluid's state and slopes. */
    @Test
    void waterAirThroughMixMatchesTheFastPath() {
        Species[] sp = {Species.WATER, Species.AIR};
        Random r = new Random(3);
        for (int k = 0; k < 300; k++) {
            double[] s = WaterAirTest.state(r);
            Network.Node mx = node(sp, s[2], new double[]{s[0], s[1]}, s[3]);
            Network.Node wa = new Network.Node(s[2], 0, 1, false).fill(s[0], s[1], s[3]);
            assertEquals(wa.T(), mx.T(), 1e-7, "T, case " + k);
            assertEquals(wa.P(), mx.P(), 1e-7 * wa.P(), "P, case " + k);
            for (double f : new double[]{0, 0.3, 1}) {
                double[] p1 = mx.fluid.portPSlope(f), p2 = wa.fluid.portPSlope(f);
                double[][] d1 = mx.fluid.drawSlope(f), d2 = wa.fluid.drawSlope(f);
                for (int j = 0; j < 3; j++) {
                    assertEquals(p2[j], p1[j], 1e-4 * Math.abs(p2[j]) + 1e-6, "dP/d" + j + ", case " + k);
                    for (int c = 0; c < 3; c++) assertEquals(d2[c][j], d1[c][j], 1e-3 * Math.abs(d2[c][j]) + 1e-6, "d draw" + c + "/d" + j + ", case " + k);
                }
            }
        }
    }
}
