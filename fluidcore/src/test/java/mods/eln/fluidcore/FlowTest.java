package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The FLOW component (Network.Edge.rate): a prescribed flow inside the implicit step. A source from outside adds
 * exactly rate x time and its energy; a source into a vented node settles, sleeps and still reports its rate; a sink
 * never takes a node below zero, books what left, and passes its rate while the node is full; the gates hold; the
 * slopes match finite differences; random networks with sources, sinks and flows between nodes conserve.
 */
class FlowTest {
    static final double DT = 0.05;
    static final Species[] MIX = {Species.WATER, Species.AIR};

    static Network.Node node(Network net, double V, boolean fixed, double mw, double ma, double T, Species[] sp) {
        return net.add(new Network.Node(V, 0, 1, fixed, sp).fillMasses(new double[]{mw, ma}, T));
    }

    static Network.Node air(Network net, double V, boolean fixed, double bar, Species[] sp) {
        return node(net, V, fixed, 0, bar * 1e5 * V / (287.05 * 300), 300, sp);
    }

    static Network.Edge flow(Network net, Network.Node a, Network.Node b, double rate, Network.Phase phase) {
        return flow(net, a, 0.5, b, rate, phase);
    }

    static Network.Edge flow(Network net, Network.Node a, double fa, Network.Node b, double rate, Network.Phase phase) {
        Network.Edge e = net.connect(a, fa, b, 0.5, 0);
        e.kind = Network.Kind.FLOW;
        e.rate = rate;
        e.phase = phase;
        return e;
    }

    /** A source from outside into a closed node: its masses and energy grow by exactly rate x share x time. */
    @Test
    void aSourceIntoAClosedNodeAddsExactlyItsRate() {
        for (Species[] sp : new Species[][]{null, MIX}) {
            Network net = new Network();
            var atm = air(net, 1, true, 1.01325, sp);
            var v = air(net, 0.001, false, 1, sp);
            double rate = 2e-4, h = 2.5e5;
            var e = flow(net, atm, v, rate, Network.Phase.ANY);
            e.supply = new double[]{0.3, 0.7, h};
            double mw0 = v.m[0], ma0 = v.m[1], u0 = v.U;
            int steps = 400;
            for (int s = 0; s < steps; s++) {
                net.step(DT);
                assertEquals(DT * rate * 0.3, e.moved[0], 1e-18, "water moved, step " + s);
                assertEquals(DT * rate * 0.7, e.moved[1], 1e-18, "air moved, step " + s);
                assertEquals(DT * rate * h, e.moved[2], 1e-12, "energy moved, step " + s);
                assertEquals(rate, e.flow, 0, "flow, step " + s);
            }
            double t = steps * DT;
            String what = sp == null ? "water/air" : "mix";
            assertEquals(mw0 + 0.3 * rate * t, v.m[0], 1e-15, what + " water");
            assertEquals(ma0 + 0.7 * rate * t, v.m[1], 1e-15, what + " air");
            assertEquals(u0 + rate * h * t, v.U, 1e-9 * Math.abs(u0 + rate * h * t), what + " energy");
            System.out.printf("source into a closed node (%s): %.4g kg in, %.2f bar, %.1f K, failures %d%n", what, rate * t, v.P() / 1e5, v.T(),
                net.stats.failures);
        }
    }

    /** A source of air into a vessel vented to the air: it settles, sleeps, and asleep still reports its rate. */
    @Test
    void aSourceIntoAVentedNodeSettlesAndSleeps() {
        for (Species[] sp : new Species[][]{null, MIX}) {
            Network net = new Network();
            var atm = air(net, 1, true, 1.01325, sp);
            var v = air(net, 0.01, false, 1.01325, sp);
            double rate = 1e-3;
            var e = flow(net, atm, v, rate, Network.Phase.ANY);
            double[] g = v.fluid.gasDraw().clone();
            g[0] = 0;
            g[1] = 1;
            e.supply = g;
            var vent = net.connect(v, 0.5, atm, 0.5, 1e-7);
            int s = 0;
            while (!net.asleep() && s < 5000) {
                net.step(DT);
                s++;
            }
            String what = sp == null ? "water/air" : "mix";
            assertTrue(net.asleep(), what + ": asleep after " + s + " steps (restless " + net.restless + ")");
            for (int k = 0; k < 20; k++) net.step(DT);
            assertTrue(net.asleep(), what + ": still asleep");
            assertEquals(DT * rate, e.moved[1], 1e-18, what + ": the source's air, asleep");
            assertEquals(DT * rate * g[2], e.moved[2], 1e-9, what + ": the source's energy, asleep");
            assertEquals(rate, e.flow, 0, what + ": the source's flow, asleep");
            assertEquals(DT * rate, vent.moved[1], 1e-6 * DT * rate, what + ": the vent passes what comes in");
            System.out.printf("source into a vented node (%s): asleep after %d steps at %.4f bar, %.2f K%n", what, s, v.P() / 1e5, v.T());
        }
    }

    /** A sink far faster than its node holds: no mass under zero, what it books is what left, nothing else taken. */
    @Test
    void aSinkNeverTakesANodeBelowZero() {
        for (Species[] sp : new Species[][]{null, MIX})
            for (Network.Phase ph : Network.Phase.values()) {
                Network net = new Network();
                var atm = air(net, 1, true, 1.01325, sp);
                var v = node(net, 0.001, false, 5e-4, 1.2e-3, 300, sp);
                // (at its floor port, so ANY reaches the liquid first and then the gas)
                var e = flow(net, v, 0, atm, 1, ph);
                double[] m0 = v.m.clone(), out = new double[2];
                String what = (sp == null ? "water/air " : "mix ") + ph;
                for (int s = 0; s < 200; s++) {
                    net.step(DT);
                    for (int k = 0; k < 2; k++) {
                        out[k] += e.moved[k];
                        assertTrue(v.m[k] >= -1e-15 * (m0[0] + m0[1]), what + " m[" + k + "] = " + v.m[k] + " at step " + s);
                        assertTrue(e.moved[k] >= 0, what + " moved backwards at step " + s);
                    }
                }
                for (int k = 0; k < 2; k++) assertEquals(m0[k] - v.m[k], out[k], 1e-18, what + ": booked = left, m[" + k + "]");
                // (ANY at the floor port reaches the liquid only while its surface is over the port's window: the last
                // BAND of the height stays, as a pipe's floor port leaves it)
                if (ph == Network.Phase.LIQUID) {
                    assertEquals(m0[1], v.m[1], 0, what + ": no air taken");
                    assertTrue(v.liquidMass(0) < 1e-3 * m0[0], what + ": the liquid drained, " + v.liquidMass(0));
                }
                if (ph == Network.Phase.GAS) assertTrue(v.m[1] < 1e-3 * m0[1], what + ": the air let out, " + v.m[1]);
                System.out.printf("sink at 1 kg/s from 1.7 g (%s): left %.3g kg water, %.3g kg air, failures %d%n", what, v.m[0], v.m[1], net.stats.failures);
            }
    }

    /** A sink from a full tank passes its rate. */
    @Test
    void aSinkPassesItsRateWhileFull() {
        Network net = new Network();
        var atm = air(net, 1, true, 1.01325, null);
        var v = node(net, 1, false, 500, 0.6, 300, null);
        double rate = 0.1;
        var e = flow(net, v, atm, rate, Network.Phase.LIQUID);
        for (int s = 0; s < 100; s++) {
            net.step(DT);
            assertEquals(DT * rate, e.moved[0], 1e-12 * DT * rate, "step " + s);
        }
        assertEquals(500 - 100 * DT * rate, v.m[0], 1e-9);
    }

    /** maxPa: a source stops filling at b's maxPa; minPa: a sink stops emptying at a's minPa. Neither passes it. */
    @Test
    void theGatesHold() {
        for (Species[] sp : new Species[][]{null, MIX}) {
            Network net = new Network();
            var atm = air(net, 1, true, 1.01325, sp);
            var v = air(net, 0.001, false, 1, sp);
            var e = flow(net, atm, v, 1e-3, Network.Phase.ANY);
            e.supply = new double[]{0, 1, v.fluid.gasDraw()[2]};
            e.maxPa = 2e5;
            for (int s = 0; s < 400; s++) {
                net.step(DT);
                assertTrue(v.P() <= 2e5 * (1 + 1e-9), "maxPa passed: " + v.P() + " at step " + s);
            }
            assertTrue(v.P() > 1.9e5, "filled to near maxPa: " + v.P());
            assertTrue(e.flow < 1e-2 * e.rate, "and stopped: " + e.flow);

            Network n2 = new Network();
            var atm2 = air(n2, 1, true, 1.01325, sp);
            var w = air(n2, 0.001, false, 3, sp);
            var sink = flow(n2, w, atm2, 1e-3, Network.Phase.GAS);
            sink.minPa = 2e5;
            for (int s = 0; s < 400; s++) {
                n2.step(DT);
                assertTrue(w.P() >= 2e5 * (1 - 1e-9), "minPa passed: " + w.P() + " at step " + s);
            }
            assertTrue(w.P() < 2.1e5, "emptied to near minPa: " + w.P());
            System.out.printf("gates (%s): source held at %.5f bar, sink at %.5f bar%n", sp == null ? "water/air" : "mix", v.P() / 1e5, w.P() / 1e5);
        }
    }

    /** The Jacobian against central differences: sources on their gate's ramp, sinks of each phase, a sink near empty. */
    @Test
    void slopesMatchFiniteDifferences() {
        for (Species[] sp : new Species[][]{null, MIX})
            for (int c = 0; c < 6; c++) {
                Network net = new Network();
                var atm = air(net, 1, true, 1.01325, sp);
                Network.Node v;
                Network.Edge e;
                switch (c) {
                    case 0 -> {
                        // a source of steam and air filling to maxPa, vented
                        v = air(net, 0.001, false, 1, sp);
                        e = flow(net, atm, v, 1e-3, Network.Phase.ANY);
                        e.supply = new double[]{0.5, 0.5, 1.5e6};
                        e.maxPa = 2e5;
                        net.connect(v, 0.5, atm, 0.5, 1e-9);
                    }
                    case 1 -> {
                        // a two-phase vessel drained through its bottom port
                        v = node(net, 0.001, false, 0.3, 1.2e-3, 350, sp);
                        e = flow(net, v, 0, atm, 0.05, Network.Phase.ANY);
                    }
                    case 2 -> {
                        // its gas let out down to 2 bar
                        v = node(net, 0.001, false, 0.3, 4e-3, 400, sp);
                        e = flow(net, v, atm, 1e-3, Network.Phase.GAS);
                        e.minPa = 2e5;
                    }
                    case 3 -> {
                        // its liquid drained near empty: M about rate x FLOW_TAU (sigma's ramp)
                        v = node(net, 0.001, false, 2e-3, 1.2e-3, 300, sp);
                        e = flow(net, v, atm, 0.1, Network.Phase.LIQUID);
                    }
                    case 4 -> {
                        // a flow between two free nodes, fed from a pressurised one
                        var src = node(net, 0.002, false, 0.5, 6e-3, 420, sp);
                        v = air(net, 0.001, false, 1, sp);
                        e = flow(net, src, v, 0.01, Network.Phase.LIQUID);
                        e.maxPa = 3e5;
                        e.minPa = 1.5e5;
                    }
                    default -> {
                        // a sink of all phases from a nearly dry node
                        v = node(net, 0.001, false, 1e-5, 1e-4, 300, sp);
                        e = flow(net, v, atm, 1e-2, Network.Phase.ANY);
                    }
                }
                // (the nearly dry node: its state solve's rounding swamps differences over 1e-8 of its 2 mg; and past
                // its first 8 steps it is a few micrograms at 190 K, where differences of any step are noise)
                double step = Network.CHECK_STEP;
                if (c == 5) Network.CHECK_STEP = 1e-6;
                for (int s = 0; s < (c == 5 ? 8 : 40); s++) {
                    String r;
                    try {
                        r = net.checkJacobian(DT, true);
                    } finally {
                        Network.CHECK_STEP = step;
                    }
                    if (c == 5) Network.CHECK_STEP = 1e-6;
                    double worst = Double.parseDouble(r.substring(0, r.indexOf(' ')));
                    assertTrue(worst < 1e-4, (sp == null ? "water/air" : "mix") + " case " + c + " step " + s + ": " + r);
                    net.step(DT);
                }
                Network.CHECK_STEP = step;
            }
    }

    /** A changed rate or supply wakes a sleeping network. */
    @Test
    void aChangedRateOrSupplyWakesIt() {
        Network net = new Network();
        var atm = air(net, 1, true, 1.01325, null);
        var v = air(net, 0.01, false, 1.01325, null);
        var e = flow(net, atm, v, 1e-3, Network.Phase.ANY);
        e.supply = new double[]{0, 1, v.fluid.gasDraw()[2]};
        net.connect(v, 0.5, atm, 0.5, 1e-7);
        for (int s = 0; s < 5000 && !net.asleep(); s++) net.step(DT);
        assertTrue(net.asleep());
        e.rate = 2e-3;
        net.step(DT);
        assertEquals("edge 0 rate", net.wokenByName());
        for (int s = 0; s < 5000 && !net.asleep(); s++) net.step(DT);
        assertTrue(net.asleep());
        e.supply[2] *= 1.01;
        net.step(DT);
        assertEquals("edge 0 supply[2]", net.wokenByName());
    }

    /** A dump with FLOW edges reads back as written; an old dump's edges read as no FLOW. */
    @Test
    void dumpsRoundTrip() {
        Network net = new Network();
        var atm = air(net, 1, true, 1.01325, MIX);
        var v = air(net, 0.01, false, 1.5, MIX);
        var e = flow(net, atm, v, 1e-3, Network.Phase.ANY);
        e.supply = new double[]{0.25, 0.75, 3e5};
        e.maxPa = 4e5;
        var s = flow(net, v, atm, 2e-3, Network.Phase.GAS);
        s.minPa = 1.2e5;
        String d = NetworkDump.write(net, DT);
        double[] dt = new double[1];
        Network back = NetworkDump.read(d, dt);
        assertEquals(d, NetworkDump.write(back, dt[0]));
        assertEquals(Network.Kind.FLOW, back.edges.get(1).kind);
        assertEquals(Network.Phase.GAS, back.edges.get(1).phase);
        assertTrue(back.edges.get(1).supply == null && Double.isNaN(back.edges.get(1).maxPa));
        // (the same edge without its FLOW fields, as dumps before them were written)
        String old = d.lines().map(l -> l.startsWith("edge ") ? String.join(" ", java.util.Arrays.copyOf(l.split(" "), 24)) : l)
            .reduce("", (a, b) -> a + b + "\n");
        Network o = NetworkDump.read(old, dt);
        assertEquals(0, o.edges.get(0).rate, 0);
        assertTrue(o.edges.get(0).supply == null && Double.isNaN(o.edges.get(0).minPa));
    }

    /**
     * NetworkTest's random level networks with a fixed reservoir of hot water and steam and the air outside: sources
     * from the reservoir (its draw, or a supply as it would draw) gated at maxPa, sinks of every phase to the air, some
     * gated at minPa, and flows between free nodes. Under the game's settings each keeps its masses and energy plus
     * what the sources brought less what the sinks booked, stays positive, and no step fails or is cut short.
     */
    @Test
    void randomNetworksWithFlowsConserve() throws Exception {
        int cases = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_CASES"), "60"));
        int steps = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_STEPS"), "600"));
        boolean cut = Network.CUT_SHORT;
        int depth = Network.MAX_DEPTH;
        Network.CUT_SHORT = true;
        Network.MAX_DEPTH = 8;
        try {
            List<String> bad = new ArrayList<>();
            long failures = 0, cuts = 0, substeps = 0, total = 0, flows = 0, slept = 0;
            double brought = 0, booked = 0;
            for (long seed = 1; seed <= cases; seed++) {
                Network net = NetworkTest.level(seed);
                List<Network.Node> free = new ArrayList<>(net.nodes);
                Random r = new Random(seed * 104729 + 3);
                var atm = net.add(new Network.Node(1, 0, 1, true, NetworkTest.MIX).fill(0, 1.2, 300));
                var res = net.add(new Network.Node(1, 0, 1, true, NetworkTest.MIX).fill(400, 3, 440));
                List<Network.Edge> in = new ArrayList<>(), out = new ArrayList<>();
                int n = 1 + r.nextInt(4);
                for (int i = 0; i < n; i++) {
                    var v = free.get(r.nextInt(free.size()));
                    Network.Phase ph = Network.Phase.values()[r.nextInt(3)];
                    switch (r.nextInt(3)) {
                        case 0 -> {
                            var e = flow(net, res, r.nextDouble(), v, Math.pow(10, -6 + 3 * r.nextDouble()), ph);
                            if (r.nextBoolean()) e.supply = Network.flowCarries(e).clone();
                            e.maxPa = 2e5 + 18e5 * r.nextDouble();
                            in.add(e);
                        }
                        case 1 -> {
                            var e = flow(net, v, r.nextDouble(), atm, Math.pow(10, -6 + 4 * r.nextDouble()), ph);
                            if (r.nextDouble() < 0.4) e.minPa = 1e5 + 4e5 * r.nextDouble();
                            out.add(e);
                        }
                        default -> {
                            var w = free.get(r.nextInt(free.size()));
                            if (w == v) continue;
                            var e = flow(net, v, r.nextDouble(), w, Math.pow(10, -6 + 4 * r.nextDouble()), ph);
                            if (r.nextBoolean()) e.maxPa = 2e5 + 18e5 * r.nextDouble();
                        }
                    }
                    flows++;
                }
                double[] t0 = NetworkTest.totals(net), books = new double[3];
                for (int s = 0; s < steps; s++) {
                    net.step(DT);
                    for (var e : net.edges) {
                        if (e.kind != Network.Kind.FLOW || e.moved == null) continue;
                        double sign = in.contains(e) ? 1 : out.contains(e) ? -1 : 0;
                        for (int k = 0; k < 3; k++) books[k] += sign * e.moved[k];
                        if (sign > 0) brought += e.moved[0] + e.moved[1];
                        if (sign < 0) booked += e.moved[0] + e.moved[1];
                        for (int k = 0; k < 2; k++) if (e.moved[k] < -1e-18) bad.add("FLOW backwards " + seed + "@" + s);
                    }
                    for (var v : free)
                        if (!(v.mw() >= -1e-12 * Math.max(1e-3, v.mass()) && v.ma() >= -1e-12 * Math.max(1e-3, v.mass())))
                            bad.add("negative " + seed + "@" + s);
                }
                double[] t1 = NetworkTest.totals(net);
                if (Math.abs(t1[0] - t0[0] - books[0]) > 1e-11 * Math.max(1, t0[0]) || Math.abs(t1[1] - t0[1] - books[1]) > 1e-11 * Math.max(1e-3, t0[1]))
                    bad.add(String.format("mass %d: water %.3g, air %.3g off", seed, t1[0] - t0[0] - books[0], t1[1] - t0[1] - books[1]));
                if (Math.abs(t1[2] - t0[2] - books[2]) > 1e-9 * Math.max(1e3, Math.abs(t0[2]) + Math.abs(books[2])))
                    bad.add(String.format("energy %d: %.6g J changed, flows booked %.6g J", seed, t1[2] - t0[2], books[2]));
                if (net.stats.failures > 0 || net.stats.cut > 0) bad.add("seed " + seed + ": failures " + net.stats.failures + ", cut " + net.stats.cut);
                failures += net.stats.failures;
                cuts += net.stats.cut;
                substeps += net.stats.substeps;
                total += net.stats.steps;
                slept += net.stats.slept;
            }
            String log = String.format("cases %d steps %d, %d FLOW edges: failures %d, cut %d, substeps %d over %d steps, %d slept; sources brought %.4g kg, sinks booked %.4g kg%nproblems: %d %s%n",
                cases, steps, flows, failures, cuts, substeps, total, slept, brought, booked, bad.size(), bad.stream().limit(20).toList());
            java.nio.file.Files.writeString(TestOut.path("fluidcore-flows.txt"), log);
            System.out.print(log);
            assertTrue(bad.isEmpty(), log);
        } finally {
            Network.CUT_SHORT = cut;
            Network.MAX_DEPTH = depth;
        }
    }
}
