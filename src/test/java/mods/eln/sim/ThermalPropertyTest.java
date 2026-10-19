package mods.eln.sim;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Properties of the thermal network on random networks run through the Simulator's tick: a closed network
 * (no loss to ambient) conserves its heat and stays between its initial extremes, and an open one with heaters
 * balances its energy exactly (heat in - loss to ambient = change of the stored heat). Explicit Euler is only stable
 * when dt * (sum of a load's conductances) / C is small, so the generator keeps every load at or under 0.5.
 */
public class ThermalPropertyTest {

    static double logUniform(Random rnd, double lo, double hi) {
        return Math.exp(Math.log(lo) + rnd.nextDouble() * (Math.log(hi) - Math.log(lo)));
    }

    record Net(List<ThermalLoad> loads, List<ThermalConnection> connections) {
        double heat() {
            double e = 0;
            for (ThermalLoad l : loads) e += l.C * l.Tc;
            return e;
        }

        double absHeat() {
            double e = 0;
            for (ThermalLoad l : loads) e += Math.abs(l.C * l.Tc);
            return e;
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            for (ThermalLoad l : loads) s.append(String.format("[C %.4g Rs %.4g Rp %.4g Tc %.4g] ", l.C, l.Rs, l.Rp, l.Tc));
            for (ThermalConnection c : connections) s.append(loads.indexOf(c.L1)).append('-').append(loads.indexOf(c.L2)).append(' ');
            return s.toString();
        }
    }

    /** Connected random network; Rp infinite for a closed one; C raised where needed for stability at dt. */
    static Net network(Random rnd, int n, boolean closed, boolean slow, double dt) {
        List<ThermalLoad> loads = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ThermalLoad l = new ThermalLoad(rnd.nextDouble() * 1200 - 200,
                closed ? Double.POSITIVE_INFINITY : logUniform(rnd, 0.1, 1e4), logUniform(rnd, 0.01, 10), logUniform(rnd, 1, 1e4));
            if (slow) l.setAsSlow();
            loads.add(l);
        }
        List<ThermalConnection> cons = new ArrayList<>();
        for (int i = 1; i < n; i++) cons.add(new ThermalConnection(loads.get(i), loads.get(rnd.nextInt(i))));
        for (int k = rnd.nextInt(n + 1); k > 0; k--) {
            int a = rnd.nextInt(n), b = rnd.nextInt(n);
            if (a != b) cons.add(new ThermalConnection(loads.get(a), loads.get(b)));
        }
        double[] g = new double[n];
        for (ThermalConnection c : cons) {
            double y = 1 / (c.L1.Rs + c.L2.Rs);
            g[loads.indexOf(c.L1)] += y;
            g[loads.indexOf(c.L2)] += y;
        }
        for (int i = 0; i < n; i++) {
            ThermalLoad l = loads.get(i);
            l.C = Math.max(l.C, 2 * dt * (g[i] + 1 / l.Rp));
        }
        return new Net(loads, cons);
    }

    static void install(SimHarness h, Net net) {
        for (ThermalLoad l : net.loads) h.sim.addThermalLoad(l);
        for (ThermalConnection c : net.connections) h.sim.addThermalConnection(c);
    }

    @Property(tries = 200)
    void closedNetworkConservesHeat(@ForAll @IntRange(min = 2, max = 12) int n, @ForAll long seed, @ForAll boolean slow) {
        Random rnd = new Random(seed);
        SimHarness h = new SimHarness();
        Net net = network(rnd, n, true, slow, slow ? SimHarness.TICK : SimHarness.THERMAL_PERIOD);
        String desc = net.toString();
        double e0 = net.heat(), scale = net.absHeat();
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (ThermalLoad l : net.loads) {
            lo = Math.min(lo, l.Tc);
            hi = Math.max(hi, l.Tc);
        }
        install(h, net);
        for (int t = 0; t < 200; t++) {
            h.tick();
            for (ThermalLoad l : net.loads) {
                assertTrue(Double.isFinite(l.Tc), "tick " + t + ": " + desc);
                assertTrue(l.Tc >= lo - 1e-9 * (hi - lo + 1) && l.Tc <= hi + 1e-9 * (hi - lo + 1),
                    "tick " + t + ": " + l.Tc + " left [" + lo + ", " + hi + "]: " + desc);
            }
        }
        assertEquals(e0, net.heat(), 1e-10 * scale + 1e-9, "heat after 200 ticks: " + desc);
    }

    /** Heaters on some loads, loss to ambient through Rp: stored heat changes by exactly heat in - heat lost. */
    @Property(tries = 200)
    void openNetworkBalancesEnergy(@ForAll @IntRange(min = 1, max = 12) int n, @ForAll long seed) {
        Random rnd = new Random(seed);
        SimHarness h = new SimHarness();
        Net net = network(rnd, n, false, false, SimHarness.THERMAL_PERIOD);
        String desc = net.toString();
        double[] power = new double[n];
        for (int i = 0; i < n; i++) if (rnd.nextDouble() < 0.4) power[i] = logUniform(rnd, 1, 1e4) * (rnd.nextBoolean() ? 1 : -0.1);
        double[] flow = {0, 0}; // heat in, heat lost to ambient (J)
        install(h, net);
        // (runs after the connections and before the loads update, at the fast thermal step: Tc is this step's)
        h.sim.addThermalFastProcess(dt -> {
            for (int i = 0; i < n; i++) {
                ThermalLoad l = net.loads.get(i);
                if (power[i] != 0) {
                    l.movePowerTo(power[i]);
                    flow[0] += power[i] * dt;
                }
                flow[1] += l.Tc / l.Rp * dt;
            }
        });
        double e0 = net.heat();
        for (int t = 0; t < 200; t++) {
            h.tick();
            for (ThermalLoad l : net.loads) assertTrue(Double.isFinite(l.Tc), "tick " + t + ": " + desc);
        }
        double scale = net.absHeat() + Math.abs(e0) + Math.abs(flow[0]) + Math.abs(flow[1]);
        assertEquals(e0 + flow[0] - flow[1], net.heat(), 1e-10 * scale + 1e-9, "energy balance: " + desc);
    }

    /**
     * Devices only check each load on its own (Simulator.checkThermalLoad: C at least 3 dt (Rs || Rp)), but a load
     * may have up to six connections. Loads at that minimum C, in a 3D grid, in a closed network: no load may leave
     * the initial temperature range (explicit Euler overshooting would).
     */
    @Property(tries = 100)
    void loadsAtTheirMinimalCapacityStayBounded(@ForAll @IntRange(min = 2, max = 4) int side, @ForAll long seed) {
        Random rnd = new Random(seed);
        SimHarness h = new SimHarness();
        int n = side * side * side;
        List<ThermalLoad> loads = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double rs = logUniform(rnd, 0.01, 10), rp = logUniform(rnd, 0.1, 1e4);
            ThermalLoad l = new ThermalLoad(rnd.nextDouble() * 1000, rp, rs, h.sim.getMinimalThermalC(rs, Double.POSITIVE_INFINITY));
            h.sim.checkThermalLoad(rs, Double.POSITIVE_INFINITY, l.C);
            l.Rp = Double.POSITIVE_INFINITY; // closed (no loss to ambient), so the bound is strict
            loads.add(l);
        }
        List<ThermalConnection> cons = new ArrayList<>();
        for (int x = 0; x < side; x++)
            for (int y = 0; y < side; y++)
                for (int z = 0; z < side; z++) {
                    int i = (x * side + y) * side + z;
                    if (x + 1 < side) cons.add(new ThermalConnection(loads.get(i), loads.get(i + side * side)));
                    if (y + 1 < side) cons.add(new ThermalConnection(loads.get(i), loads.get(i + side)));
                    if (z + 1 < side) cons.add(new ThermalConnection(loads.get(i), loads.get(i + 1)));
                }
        Net net = new Net(loads, cons);
        String desc = net.toString();
        install(h, net);
        for (int t = 0; t < 100; t++) {
            h.tick();
            for (ThermalLoad l : loads)
                assertTrue(Double.isFinite(l.Tc) && l.Tc >= -1e-6 && l.Tc <= 1000 + 1e-6, "tick " + t + ": " + l.Tc + " in " + desc);
        }
    }
}
