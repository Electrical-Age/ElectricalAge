package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gate family of the active and one-way components (P1 plan): NetworkTest's random level networks with some edges
 * made liquid and gas pumps, turbines, reducing and relief regulators, and gas-only valves, stepped under the game's
 * settings (CUT_SHORT, MAX_DEPTH 8); every other network has one node heated (a boiler, up to 2 kW). Each network keeps
 * its masses, keeps its energy plus the heat and the pumps' work less the turbines', stays positive, no step fails or is
 * cut short, and no pump or turbine works backwards. The Lean statements beside them are in spec/README (F7, S5,
 * S6).
 */
class GateFamilyTest {
    static final double DT = NetworkTest.DT;

    /** What each edge became, for the log. */
    static final String[] KINDS = {"pipe", "check", "pump", "gas pump", "turbine", "reducing", "relief", "gas only"};

    /** NetworkTest.level(seed) with a third of its pipes turned into the other kinds; counts what each became. */
    static Network gated(long seed, int[] count) {
        Network net = NetworkTest.level(seed);
        Random r = new Random(seed * 7919 + 1);
        for (var e : net.edges) {
            int k = e.kind == Network.Kind.CHECK ? 1 : 0;
            if (k == 0 && r.nextDouble() < 0.5) {
                k = 2 + r.nextInt(6);
                // (one way or the other: the generator's parent/child order says nothing about which side is high)
                switch (k) {
                    case 2 -> {
                        e.kind = Network.Kind.PUMP;
                        e.qMax = Math.pow(10, -4 + 2 * r.nextDouble());
                        e.h0 = 1e5 + 19e5 * r.nextDouble();
                        e.speed = r.nextDouble();
                    }
                    case 3 -> {
                        e.kind = Network.Kind.PUMP;
                        e.gas = true;
                        e.qMax = Math.pow(10, -6 + 2 * r.nextDouble());
                        e.h0 = 1e5 + 9e5 * r.nextDouble();
                        e.speed = r.nextDouble();
                    }
                    case 4 -> {
                        e.kind = Network.Kind.TURBINE;
                        e.nozzle = Math.pow(10, -5 + 2 * r.nextDouble());
                        e.open = r.nextDouble();
                        e.eta = 0.6;
                    }
                    case 5, 6 -> {
                        e.kind = Network.Kind.REGULATOR;
                        e.setPa = 1e5 + 9e5 * r.nextDouble();
                        e.relief = k == 6;
                    }
                    case 7 -> {
                        e.kind = Network.Kind.CHECK;
                        e.gasOnly = true;
                    }
                }
            }
            count[k]++;
        }
        return net;
    }

    /** As the game sets a turbine's drop each step: an ideal gas's isentropic drop from its inlet to its exhaust (J/kg). */
    static void setDrops(Network net) {
        for (var e : net.edges) {
            if (e.kind != Network.Kind.TURBINE) continue;
            double pa = Network.portP(e.a, e.fa), pb = Network.portP(e.b, e.fb);
            e.drop = pa > pb && pb > 0 ? 2000 * e.a.T() * (1 - Math.pow(pb / pa, 0.23)) : 0;
        }
    }

    @Test
    void randomGatedNetworksConserveStayPositiveAndAreNeverCut() throws Exception {
        int cases = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_CASES"), "60"));
        int steps = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_STEPS"), "600"));
        boolean cut = Network.CUT_SHORT;
        int depth = Network.MAX_DEPTH;
        Network.CUT_SHORT = true;
        Network.MAX_DEPTH = 8;
        try {
            int[] count = new int[KINDS.length];
            List<String> bad = new ArrayList<>();
            long failures = 0, cuts = 0, substeps = 0, total = 0;
            double pumped = 0, turned = 0;
            long[] flowing = new long[Network.Kind.values().length], present = new long[flowing.length];
            for (long seed = 1; seed <= cases; seed++) {
                Network net = gated(seed, count);
                double[] t0 = NetworkTest.totals(net);
                double work = 0;
                Network.Node boiler = seed % 2 == 0 ? net.nodes.get((int) (seed / 2 % net.nodes.size())) : null;
                double watts = boiler == null ? 0 : 2000 * new Random(seed).nextDouble();
                for (int s = 0; s < steps; s++) {
                    setDrops(net);
                    if (boiler != null) {
                        boiler.heat = watts;
                        work += watts * DT;
                    }
                    net.step(DT);
                    for (var e : net.edges) {
                        present[e.kind.ordinal()]++;
                        if (e.flow > 1e-9) flowing[e.kind.ordinal()]++;
                        if (e.kind == Network.Kind.PUMP) {
                            work += e.workJ;
                            pumped += e.workJ;
                            if (e.workJ < -1e-12) bad.add("pump work < 0 " + seed + "@" + s);
                        } else if (e.kind == Network.Kind.TURBINE) {
                            work -= e.workJ;
                            turned += e.workJ;
                            if (e.workJ < -1e-12) bad.add("turbine work < 0 " + seed + "@" + s);
                        }
                        if (e.kind != Network.Kind.PIPE && e.flow < -1e-15) bad.add(e.kind + " backwards " + seed + "@" + s + " " + e.flow);
                    }
                    for (var n : net.nodes)
                        if (!(n.mw() >= -1e-12 * Math.max(1e-3, n.mass()) && n.ma() >= -1e-12 * Math.max(1e-3, n.mass())))
                            bad.add("negative " + seed + "@" + s);
                }
                double[] t1 = NetworkTest.totals(net);
                if (Math.abs(t0[0] - t1[0]) > 1e-11 * Math.max(1, t0[0]) || Math.abs(t0[1] - t1[1]) > 1e-11 * Math.max(1e-3, t0[1]))
                    bad.add("mass " + seed);
                if (Math.abs(t1[2] - t0[2] - work) > 1e-9 * Math.max(1e3, Math.abs(t0[2]) + Math.abs(work)))
                    bad.add(String.format("energy %d: %.6g J changed, heat and work %.6g J", seed, t1[2] - t0[2], work));
                if (net.stats.failures > 0 || net.stats.cut > 0) bad.add("seed " + seed + ": failures " + net.stats.failures + ", cut " + net.stats.cut);
                failures += net.stats.failures;
                cuts += net.stats.cut;
                substeps += net.stats.substeps;
                total += net.stats.steps;
            }
            StringBuilder log = new StringBuilder(String.format("cases %d steps %d: failures %d, cut %d, substeps %d over %d steps; pumps put in %.4g J, turbines took %.4g J%n",
                cases, steps, failures, cuts, substeps, total, pumped, turned));
            for (int k = 0; k < KINDS.length; k++) log.append(String.format("  %s %d%n", KINDS[k], count[k]));
            for (var k : Network.Kind.values())
                log.append(String.format("  %s edges flowing forward (> 1 mg/s) in %.1f %% of their steps%n", k, 100.0 * flowing[k.ordinal()] / Math.max(1, present[k.ordinal()])));
            log.append("problems: ").append(bad.size()).append(' ').append(bad.stream().limit(20).toList()).append('\n');
            java.nio.file.Files.writeString(TestOut.path("fluidcore-gates.txt"), log.toString());
            System.out.print(log);
            assertTrue(bad.isEmpty(), log.toString());
        } finally {
            Network.CUT_SHORT = cut;
            Network.MAX_DEPTH = depth;
        }
    }
}
