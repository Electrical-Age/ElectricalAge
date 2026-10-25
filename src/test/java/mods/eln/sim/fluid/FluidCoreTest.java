package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The fluid core (notes/pipes/DESIGN.md sections 6, 6b): properties, flash, network invariants, turbine calibration. */
public class FluidCoreTest {
    static final double DT = 0.05;

    @Test
    void saturationMatchesSteamTables() {
        assertEquals(101_418, Water.psat(373.15), 1_500);
        assertEquals(1_002_600, Water.psat(453.15), 20_000);
        assertEquals(453.03, Water.tsat(1e6), 0.5);
        assertEquals(2.257e6, Water.latent(Water.TB), 1);
    }

    @Test
    void flashFindsTheStateItWasGiven() {
        // 1 kg of water at 180 C in 10 L: boiling, at its saturation pressure
        double T = 453.15, V = 0.01, mw = 1.0;
        double U = Flash.energyAt(mw, 0, V, T);
        Flash f = new Flash().solve(mw, 0, U, V, 300);
        assertEquals(T, f.T, 1e-3);
        assertEquals(Water.psat(T), f.P, 1);
        assertTrue(f.mLiquid > 0.9 && f.mVapour > 0, "liquid " + f.mLiquid + " vapour " + f.mVapour);
        // air alone: the ideal gas
        double ma = 0.012;
        Flash g = new Flash().solve(0, ma, Flash.energyAt(0, ma, V, 300), V, 250);
        assertEquals(ma * Water.R_AIR * 300 / V, g.P, 1);
        // liquid filling the vessel: the pressure climbs steeply but stays finite
        Flash full = new Flash().solve(V * Water.RHO_L, 0, Flash.energyAt(V * Water.RHO_L, 0, V, 350), V, 300);
        assertTrue(full.P > 1e5 && full.P < 1e8, "full " + full.P);
    }

    @Test
    void energyAndPressureRiseWithTemperatureEverywhere() {
        // nothing may cap pressure (Baughn) or make the energy fold back (bisection would break): sweep T at fill levels
        double V = 0.01;
        for (double fill : new double[]{0.001, 0.1, 0.5, 0.9, 0.98, 1.0, 1.1}) {
            double mw = fill * V * Water.RHO_L;
            Flash f = new Flash();
            double lastU = -Double.MAX_VALUE, lastP = -1;
            for (double T = 280; T <= 5000; T += 5) {
                f.split(mw, 0.001, V, T);
                double U = f.energy(0.001);
                assertTrue(U > lastU, "U not rising at T " + T + " fill " + fill);
                assertTrue(f.P >= lastP - 1e-6 * Math.abs(lastP), "P falling at T " + T + " fill " + fill + ": " + lastP + " -> " + f.P);
                lastU = U;
                lastP = f.P;
            }
            // and the solver finds it back
            double U = Flash.energyAt(mw, 0.001, V, 1500);
            assertEquals(1500, new Flash().solve(mw, 0.001, U, V, 300).T, 0.1, "fill " + fill);
        }
    }

    static double[] totals(FluidNetwork net) {
        double m = 0, a = 0, u = 0;
        for (FluidNode n : net.nodes) {
            if (n.fixed) continue;
            m += n.mWater;
            a += n.mAir;
            u += n.energy;
        }
        return new double[]{m, a, u};
    }

    @Test
    void checkValvePassesOneWayOnly() {
        // a high, b low: a check edge a -> b passes; b high, a low: it holds
        for (boolean forward : new boolean[]{true, false}) {
            FluidNetwork net = new FluidNetwork();
            FluidNode a = net.add(new FluidNode(0.01, 0, 0.2, false).fill(0, forward ? 5e5 : 1e5, 300));
            FluidNode b = net.add(new FluidNode(0.01, 0, 0.2, false).fill(0, forward ? 1e5 : 5e5, 300));
            net.connect(a, 0.5, b, 0.5, 1e-11).kind(FluidEdge.Kind.CHECK);
            double pa = a.P(), pb = b.P();
            for (int i = 0; i < 400; i++) net.step(DT);
            if (forward) assertTrue(a.P() < pa - 1e5 && b.P() > pb + 1e5, "forward: " + a.P() + " " + b.P());
            else {
                assertEquals(pa, a.P(), 1e-6 * pa, "backward: a");
                assertEquals(pb, b.P(), 1e-6 * pb, "backward: b");
            }
        }
    }

    @Test
    void twoTanksEqualizeAndConserve() {
        FluidNetwork net = new FluidNetwork();
        FluidNode a = net.add(new FluidNode(0.01, 0, 0.2, false).fill(0, 5e5, 300));
        FluidNode b = net.add(new FluidNode(0.01, 0, 0.2, false).fill(0, 1e5, 300));
        net.connect(a, 0.5, b, 0.5, 1e-11);
        double[] t0 = totals(net);
        for (int i = 0; i < 4000; i++) net.step(DT);
        double[] t1 = totals(net);
        assertEquals(t0[1], t1[1], 1e-12 * t0[1]);
        assertEquals(t0[2], t1[2], 1e-9 * Math.abs(t0[2]));
        assertEquals(a.P(), b.P(), 0.01 * a.P(), a.P() + " vs " + b.P());
        assertEquals(3e5, a.P(), 0.05 * 3e5);   // (the gas cools on one side and warms on the other, roughly)
    }

    @Test
    void equalizationTimeMatchesTheRcEstimate() {
        FluidNetwork net = new FluidNetwork();
        double V = 0.01, k = 5e-14;   // (tau ~ 10 s: many steps)
        FluidNode a = net.add(new FluidNode(V, 0, 0.2, false).fill(0, 2e5, 300));
        FluidNode b = net.add(new FluidNode(V, 0, 0.2, false).fill(0, 1.8e5, 300));
        net.connect(a, 0.5, b, 0.5, k);
        // isothermal-ish small step: C = V / (R T) per tank (adiabatic: / gamma), g = rho k / mu
        double rho = 1.9e5 / (Water.R_AIR * 300), g = rho * k / Water.MU_AIR;
        double cIso = V / (Water.R_AIR * 300);
        double tauIso = cIso / 2 / g, tauAd = tauIso / 1.4;
        double d0 = a.P() - b.P();
        double t = 0;
        while (a.P() - b.P() > d0 / Math.E && t < 100 * tauIso) {
            net.step(DT);
            t += DT;
        }
        assertTrue(t > 0.6 * tauAd && t < 1.4 * tauIso, "t " + t + " tau " + tauAd + ".." + tauIso);
    }

    @Test
    void closedNetworkConservesMassAndEnergy() {
        FluidNetwork net = new FluidNetwork();
        FluidNode boiler = net.add(new FluidNode(0.008, 0, 0.3, false).fill(0.5, 1e5, 300));
        FluidNode p1 = net.add(FluidNode.air(0.00015, 0.3, 0.25));
        FluidNode p2 = net.add(FluidNode.air(0.00015, 1.0, 0.25));
        FluidNode tank = net.add(FluidNode.air(0.004, 0, 0.3));
        net.connect(boiler, 1, p1, 0.5, 3e-11);
        net.connect(p1, 0.5, p2, 0.5, 3e-11);
        net.connect(p2, 0.5, tank, 1, 3e-11);
        double[] t0 = totals(net);
        double heatIn = 0;
        for (int i = 0; i < 6000; i++) {
            boiler.heat = 3000;
            heatIn += 3000 * DT;
            net.step(DT);
        }
        double[] t1 = totals(net);
        assertEquals(t0[0], t1[0], 1e-9 * t0[0], "water");
        assertEquals(t0[1], t1[1], 1e-9 * t0[1], "air");
        assertEquals(t0[2] + heatIn, t1[2], 1e-6 * heatIn, "energy");
        assertTrue(tank.mWater > 0.01, "steam reached the far tank and condensed or stayed: " + tank.mWater);
        assertTrue(boiler.P() > 2e5, "the boiler builds pressure: " + boiler.P());
    }

    @Test
    void heatedVesselBoilsOnTheSaturationCurve() {
        FluidNetwork net = new FluidNetwork();
        FluidNode v = net.add(new FluidNode(0.008, 0, 0.3, false).fill(2.0, 0, 300));   // water and its vapour, no air
        for (int i = 0; i < 40000 && v.T() < 470; i++) {
            v.heat = 5000;
            net.step(DT);
            if (i % 2000 == 0 && v.mLiquid() > 0)
                assertEquals(Water.psat(v.T()), v.P(), 0.02 * v.P() + 100, "at T " + v.T());
        }
        assertTrue(v.T() >= 470, "heated to " + v.T());
    }

    @Test
    void communicatingVesselsLevelOut() {
        // two open-topped vessels joined at the bottom: water flows from the fuller one until both levels match
        FluidNetwork net = new FluidNetwork();
        FluidNode atm = net.add(FluidNode.atmosphere(10));
        FluidNode a = net.add(new FluidNode(0.02, 0, 1.0, false).fill(15, Water.P_ATM, 293.15));
        FluidNode b = net.add(new FluidNode(0.02, 0, 1.0, false).fill(3, Water.P_ATM, 293.15));
        net.connect(a, 0, b, 0, 2e-10);
        net.connect(a, 1, atm, 0, 1e-8);
        net.connect(b, 1, atm, 0, 1e-8);
        double w0 = a.mWater + b.mWater;
        for (int i = 0; i < 20000; i++) net.step(DT);
        assertEquals(a.level(), b.level(), 0.01, a.level() + " vs " + b.level());
        assertEquals(w0, a.mWater + b.mWater, 0.01, "water (a little evaporates to the air)");
    }

    @Test
    void waterStaysAtTheBottomOfALiftedPipe() {
        // a vessel above another, joined bottom to top: water falls into the lower one (gravity head)
        FluidNetwork net = new FluidNetwork();
        FluidNode atm = net.add(FluidNode.atmosphere(20));
        FluidNode hi = net.add(new FluidNode(0.01, 5, 0.5, false).fill(5, Water.P_ATM, 293.15));
        FluidNode lo = net.add(new FluidNode(0.01, 0, 0.5, false).fill(0, Water.P_ATM, 293.15));
        net.connect(hi, 0, lo, 1, 1e-10);
        net.connect(hi, 1, atm, 0, 1e-8);
        net.connect(lo, 1, atm, 0, 1e-8);
        for (int i = 0; i < 20000; i++) net.step(DT);
        assertTrue(lo.mLiquid() > 4.9, "fell: " + lo.mLiquid() + " left " + hi.mLiquid());
    }

    @Test
    void turbineGivesEasPowerForItsSteam() {
        // EA today: electricity = 0.40 x 2.257 J per mB (= mg) of steam. A boiler venting through a turbine at steady
        // state should turn about 40% of its heat into electricity (the water is already at its boiling point)
        FluidNetwork net = new FluidNetwork();
        FluidNode atm = net.add(FluidNode.atmosphere(0));
        FluidNode boiler = net.add(new FluidNode(0.008, 0, 0.3, false).fill(5, Water.P_ATM, 373));
        FluidEdge turbine = net.connect(boiler, 1, atm, 0, 2e-11).kind(FluidEdge.Kind.TURBINE);
        double Q = 2000, e = 0, steam = 0;
        for (int i = 0; i < 24000; i++) {
            boiler.heat = Q;
            net.step(DT);
            if (i >= 16000) {
                e += turbine.power * DT;
                steam += turbine.fVapour * DT;
            }
        }
        double t = 8000 * DT, power = e / t, rate = steam / t;
        double ea = 0.40 * 2.257 * rate * 1e6;   // EA: W = 0.40 x 2.257 J/mB x mB/s, 1 mB steam = 1 mg
        assertEquals(ea, power, 0.2 * ea, "EA " + ea + " W, here " + power + " W; boiler " + boiler.P() + " Pa " + boiler.T() + " K");
        assertEquals(0.40 * Q, power, 0.2 * 0.40 * Q, "power " + power + " from " + Q + " W of heat");
    }
}
