package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Property tests (Baughn): random networks (topology, volumes, heights, contents, temperatures, conductances, heaters,
 * check valves, turbines, vents), stepped a while, must keep the physical laws: mass of each species conserved,
 * energy = start + heat in - work out - what left through vents, no NaN, no negative masses or pressures, a node holding
 * liquid sits on or above its saturation pressure, and a closed adiabatic network settles (pressure differences die
 * out apart from the gravity head). The settling cases use ports at the bottom or top of a node only: a port near a
 * liquid level between vessels at different heights can geyser for ever (a limit cycle; physically it would damp
 * slowly through friction heat and potential energy, which the model doesn't track). Each case's seed is in the
 * failure message.
 */
public class FluidPropertyTest {
    static final int CASES = Integer.getInteger("fluid.cases", 200);   // (more: -Dfluid.cases=N via the test task)
    static final double DT = 0.05;

    record Case(FluidNetwork net, List<FluidNode> free, List<FluidEdge> turbines, List<FluidEdge> vents, double[] heat) {
    }

    static Case random(long seed, boolean closed) {
        Random r = new Random(seed);
        FluidNetwork net = new FluidNetwork();
        List<FluidNode> free = new ArrayList<>();
        int n = 2 + r.nextInt(7);
        for (int i = 0; i < n; i++) {
            double V = Math.pow(10, -4 + 2.5 * r.nextDouble());          // 0.1 L .. 30 L
            double z = r.nextInt(12) - 4, h = 0.2 + r.nextDouble();
            double T = 280 + 200 * r.nextDouble();
            double liquid = r.nextDouble() < 0.6 ? r.nextDouble() * 0.9 * V * Water.RHO_L : 0;
            double p = r.nextDouble() < 0.3 ? 0 : 0.5e5 + 15e5 * r.nextDouble();
            FluidNode node = net.add(new FluidNode(V, z, h, false).fill(liquid, p, T));
            node.lossWPerK = closed ? 0 : (r.nextBoolean() ? 0 : r.nextDouble() * 2);
            free.add(node);
        }
        // a spanning tree plus a few extra edges
        for (int i = 1; i < n; i++) edge(net, free.get(r.nextInt(i)), free.get(i), r, closed);
        for (int extra = r.nextInt(3); extra > 0; extra--) {
            FluidNode a = free.get(r.nextInt(n)), b = free.get(r.nextInt(n));
            if (a != b) edge(net, a, b, r, closed);
        }
        List<FluidEdge> turbines = new ArrayList<>(), vents = new ArrayList<>();
        double[] heat = new double[n];
        if (!closed) {
            FluidNode atm = net.add(FluidNode.atmosphere(20));
            for (int i = 0; i < n; i++) {
                if (r.nextDouble() < 0.4) heat[i] = 5000 * r.nextDouble();
                double roll = r.nextDouble();
                if (roll < 0.15) turbines.add(net.connect(free.get(i), 1, atm, 0, 1e-12 * r.nextDouble()).kind(FluidEdge.Kind.TURBINE));
                else if (roll < 0.3) vents.add(net.connect(free.get(i), r.nextDouble(), atm, 0, 1e-11 * r.nextDouble()));
            }
        }
        return new Case(net, free, turbines, vents, heat);
    }

    static void edge(FluidNetwork net, FluidNode a, FluidNode b, Random r, boolean endPorts) {
        double pa = endPorts ? r.nextInt(2) : r.nextDouble(), pb = endPorts ? r.nextInt(2) : r.nextDouble();
        FluidEdge e = net.connect(a, pa, b, pb, Math.pow(10, -13 + 3 * r.nextDouble()));
        if (r.nextDouble() < 0.15) e.kind(FluidEdge.Kind.CHECK);
        if (r.nextDouble() < 0.15) e.open = r.nextDouble();
    }

    static double sum(List<FluidNode> ns, java.util.function.ToDoubleFunction<FluidNode> f) {
        double s = 0;
        for (FluidNode n : ns) s += f.applyAsDouble(n);
        return s;
    }

    static void sane(Case c, long seed, int step) {
        for (FluidNode n : c.free) {
            String at = "seed " + seed + " step " + step + " node V " + n.volume;
            if (!(Double.isFinite(n.P()) && Double.isFinite(n.T()) && Double.isFinite(n.energy))) fail("not finite: " + at);
            assertTrue(n.mWater >= -1e-12 && n.mAir >= -1e-12, "negative mass " + n.mWater + " " + n.mAir + ": " + at);
            assertTrue(n.P() >= 0, "negative pressure " + n.P() + ": " + at);
            if (n.mLiquid() > 1e-9 && n.mAir < 1e-12)
                assertTrue(n.P() >= Water.psat(n.T()) * 0.98 - 50, "liquid below its boiling pressure: " + n.P() + " < psat " + Water.psat(n.T()) + ": " + at);
        }
    }

    @Test
    void openNetworksKeepTheBooks() {
        for (long seed = 1; seed <= CASES; seed++) {
            Case c = random(seed, false);
            double w0 = sum(c.free, n -> n.mWater), a0 = sum(c.free, n -> n.mAir), u0 = sum(c.free, n -> n.energy);
            double heatIn = 0, work = 0, outW = 0, outA = 0, outH = 0, loss = 0;
            for (int s = 0; s < 400; s++) {
                for (int i = 0; i < c.free.size(); i++) {
                    FluidNode n = c.free.get(i);
                    n.heat = c.heat[i];
                    heatIn += c.heat[i] * DT;
                    loss += n.lossWPerK * (n.T() - Water.T_AMBIENT) * DT;
                }
                c.net.step(DT);
                for (FluidEdge e : c.turbines) {
                    work += e.power * DT;
                    outW += (e.fLiquid + e.fVapour) * DT;
                    outA += e.fAir * DT;
                    outH += e.fEnthalpy * DT;
                }
                for (FluidEdge e : c.vents) {
                    outW += (e.fLiquid + e.fVapour) * DT;
                    outA += e.fAir * DT;
                    outH += e.fEnthalpy * DT;
                }
                sane(c, seed, s);
            }
            double w1 = sum(c.free, n -> n.mWater), a1 = sum(c.free, n -> n.mAir), u1 = sum(c.free, n -> n.energy);
            // (turbine flows carry their enthalpy out; the work is part of it, so it isn't subtracted twice)
            assertEquals(w0 - outW, w1, 1e-9 * Math.max(1, w0), "water, seed " + seed);
            assertEquals(a0 - outA, a1, 1e-9 * Math.max(1e-3, a0), "air, seed " + seed);
            double scale = Math.max(1e3, Math.abs(u0) + heatIn + Math.abs(outH));
            assertEquals(u0 + heatIn - loss - outH, u1, 1e-7 * scale, "energy, seed " + seed + " work " + work);
            assertTrue(work >= 0, "turbines don't take power, seed " + seed);
        }
    }

    @Test
    void closedNetworksConserveAndSettle() {
        List<Long> restless = new ArrayList<>();
        for (long seed = 1001; seed < 1001 + CASES; seed++) {
            Case c = random(seed, true);
            double w0 = sum(c.free, n -> n.mWater), a0 = sum(c.free, n -> n.mAir), u0 = sum(c.free, n -> n.energy);
            for (int s = 0; s < 3000; s++) {
                c.net.step(DT);
                if (s % 50 == 0) sane(c, seed, s);
            }
            assertEquals(w0, sum(c.free, n -> n.mWater), 1e-9 * Math.max(1, w0), "water, seed " + seed);
            assertEquals(a0, sum(c.free, n -> n.mAir), 1e-9 * Math.max(1e-3, a0), "air, seed " + seed);
            assertEquals(u0, sum(c.free, n -> n.energy), 1e-7 * Math.max(1e3, Math.abs(u0)), "energy, seed " + seed);
            // settled: what still flows is small next to the network's contents
            double moving = 0, total = sum(c.free, FluidNode::mass);
            for (FluidEdge e : c.net.edges) moving += Math.abs(e.flow);
            if (moving * 10 >= total + 1e-6) restless.add(seed);
        }
        // Known limit: one edge carries one stream, so a puddle on a port that gas wants to enter through (liquid
        // down, gas up the same pipe: counter-current flow) can percolate for ever. 3 of 2000 cases (2026-10-18:
        // seeds 1860, 2411, 2961). More than 1 in 200 is a regression.
        assertTrue(restless.size() <= Math.max(1, CASES / 200), "not settling: seeds " + restless);
    }

    @Test
    void stepsAreDeterministic() {
        for (long seed = 5001; seed < 5021; seed++) {
            Case a = random(seed, false), b = random(seed, false);
            for (int s = 0; s < 200; s++) {
                for (int i = 0; i < a.free.size(); i++) {
                    a.free.get(i).heat = a.heat[i];
                    b.free.get(i).heat = b.heat[i];
                }
                a.net.step(DT);
                b.net.step(DT);
            }
            for (int i = 0; i < a.free.size(); i++) assertEquals(a.free.get(i).energy, b.free.get(i).energy, 0.0, "seed " + seed);
        }
    }
}
