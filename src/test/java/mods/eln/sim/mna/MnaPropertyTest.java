package mods.eln.sim.mna;

import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.TestConductance;
import mods.eln.sim.mna.component.TestCurrentSource;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.PowerSourceBipole;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Properties of the MNA solver on random DC networks (RandomNet), checked against an independent dense solve and
 * Kirchhoff's current law: plain states (the bare SubSystem solve), cable loads (Line collapse), add/remove/
 * high-impedance transitions between steps, networks over 100 states (InterSystemAbstraction), a Monopole, and the
 * solar panel's PowerSourceBipole.
 */
public class MnaPropertyTest {
    static final double DT = 0.05;

    /** A RandomNet built into a RootSystem. */
    static final class Built {
        final RandomNet net;
        final RootSystem root;
        final State[] nodes;
        final List<Resistor> res = new ArrayList<>();      // parallel to net.edges
        final VoltageSource[] src;
        final List<TestCurrentSource> cs = new ArrayList<>();
        /** Edges currently in the circuit (the reference solves these). */
        final List<RandomNet.Edge> active;
        final boolean[] in;

        Built(RandomNet net, int overSampling, boolean loads, Random rnd) {
            this(net, overSampling, loads, rnd, 0.5);
        }

        /** loads: cable loads and connections (lineReady: the share of loads that may collapse into a Line). */
        Built(RandomNet net, int overSampling, boolean loads, Random rnd, double lineReady) {
            this.net = net;
            root = new RootSystem(DT, overSampling);
            nodes = new State[net.n];
            double[] rs = new double[net.n];
            for (int i = 0; i < net.n; i++) {
                if (loads) {
                    ElectricalLoad l = new ElectricalLoad();
                    rs[i] = RandomNet.logUniform(rnd, 0.01, 10);
                    l.setRs(rs[i]);
                    l.setCanBeSimplifiedByLine(rnd.nextDouble() < lineReady);
                    nodes[i] = l;
                } else {
                    nodes[i] = new VoltageState();
                }
                root.addState(nodes[i]);
            }
            if (loads) // a connection's resistance is its two loads' Rs
                net.edges.replaceAll(e -> e.b() < 0 ? e : new RandomNet.Edge(e.a(), e.b(), rs[e.a()] + rs[e.b()]));
            for (RandomNet.Edge e : net.edges) {
                Resistor r;
                if (loads && e.b() >= 0) r = new ElectricalConnection((ElectricalLoad) nodes[e.a()], (ElectricalLoad) nodes[e.b()]);
                else r = new Resistor(nodes[e.a()], node(e.b())).setR(e.r());
                res.add(r);
                root.addComponent(r);
            }
            src = new VoltageSource[net.n];
            for (int i = 0; i < net.n; i++) {
                if (Double.isNaN(net.vs[i])) continue;
                src[i] = new VoltageSource("v" + i, nodes[i], null).setU(net.vs[i]);
                root.addComponent(src[i]);
            }
            for (RandomNet.Cs c : net.cs) {
                TestCurrentSource t = new TestCurrentSource(node(c.a()), node(c.b()), c.i());
                cs.add(t);
                root.addComponent(t);
            }
            active = new ArrayList<>(net.edges);
            in = new boolean[net.edges.size()];
            java.util.Arrays.fill(in, true);
        }

        State node(int i) {
            return i < 0 ? null : nodes[i];
        }

        List<RandomNet.Edge> activeEdges() {
            List<RandomNet.Edge> out = new ArrayList<>();
            for (int k = 0; k < active.size(); k++) if (in[k]) out.add(active.get(k));
            return out;
        }

        /**
         * A relative tolerance for comparing two double-precision solves of the active network: floor, or more when
         * the spread of conductances makes the matrix ill-conditioned. A 1e9 high impedance next to a 0.02 ohm
         * connection (two 0.01 ohm loads) gives a condition number ~5e10; a node cut off but for the 1e9 then
         * comes out ~1e-5 off in both solves (and differently each run, as the matrix order follows hash order).
         */
        double conditionTolerance(double floor) {
            double gMin = Double.POSITIVE_INFINITY, gMax = 1; // (1: a voltage source's entries in the matrix)
            for (RandomNet.Edge e : activeEdges()) {
                gMin = Math.min(gMin, 1 / e.r());
                gMax = Math.max(gMax, 1 / e.r());
            }
            return Math.max(floor, 20 * Math.ulp(1.0) * gMax / gMin);
        }

        /** Every state finite. */
        void assertFinite(String ctx) {
            for (int i = 0; i < nodes.length; i++)
                assertTrue(Double.isFinite(nodes[i].state), ctx + ": node " + i + " = " + nodes[i].state + " in " + net);
            for (VoltageSource v : src)
                if (v != null) assertTrue(Double.isFinite(v.getI()), ctx + ": source current " + v.getI() + " in " + net);
        }

        /** Grounded nodes equal the reference solve; KCL holds at each of them. */
        void assertMatchesReference(String ctx, double relTol) {
            List<RandomNet.Edge> act = activeEdges();
            double[] ref = net.solve(act);
            double scale = 1;
            for (double v : ref) if (!Double.isNaN(v)) scale = Math.max(scale, Math.abs(v));
            for (int i = 0; i < net.n; i++) {
                if (Double.isNaN(ref[i])) continue;
                assertEquals(ref[i], nodes[i].state, relTol * scale, ctx + ": node " + i + " in " + net + " active " + act);
            }
            // KCL: the currents leaving each grounded node through its components add up to zero
            double[] sum = new double[net.n], abs = new double[net.n];
            for (int k = 0; k < res.size(); k++) {
                if (!in[k]) continue;
                RandomNet.Edge e = active.get(k);
                double i = res.get(k).getCurrent();
                sum[e.a()] += i;
                abs[e.a()] += Math.abs(i);
                if (e.b() >= 0) {
                    sum[e.b()] -= i;
                    abs[e.b()] += Math.abs(i);
                }
            }
            for (int k = 0; k < net.n; k++) {
                if (src[k] == null || Double.isNaN(net.vs[k])) continue;
                double i = -src[k].getI();   // getI: the current the source delivers into its aPin node
                sum[k] += i;
                abs[k] += Math.abs(i);
            }
            for (TestCurrentSource t : cs) {
                for (int k = 0; k < net.n; k++) {
                    if (t.aPin == nodes[k]) {
                        sum[k] -= t.i;
                        abs[k] += Math.abs(t.i);
                    }
                    if (t.bPin == nodes[k]) {
                        sum[k] += t.i;
                        abs[k] += Math.abs(t.i);
                    }
                }
            }
            for (int k = 0; k < net.n; k++) {
                if (Double.isNaN(ref[k])) continue;
                assertEquals(0, sum[k], relTol * 10 * abs[k] + 1e-8, ctx + ": KCL at node " + k + " in " + net);
            }
        }
    }

    /**
     * A net that failed once (2026-10-25): dead-end nodes hanging off 22..82 kohm next to 0.11 ohm came out ~2e-6
     * relative off (the explicit QR inverse lost digits); fixed by solving the step with LU factors in SubSystem. Built 40 times, as
     * the matrix order follows hash order.
     */
    @org.junit.jupiter.api.Test
    void deadEndNodesMatchTheReference() {
        for (int k = 0; k < 40; k++) {
            RandomNet net = new RandomNet(8);
            double[][] e = {{1, 0, 22417.56022850012}, {2, 1, 0.4579127041061821}, {3, 1, 57104.03485760915}, {4, 2, 0.20121733165678407},
                {5, 4, 999.5625943812363}, {6, 1, 0.11211220728152951}, {7, 3, 82325.42344870533}, {4, 6, 146.18580691586544}, {6, -1, 46469.441288656366}};
            for (double[] x : e) net.edges.add(new RandomNet.Edge((int) x[0], (int) x[1], x[2]));
            net.cs.add(new RandomNet.Cs(6, 1, 5.602));
            net.cs.add(new RandomNet.Cs(4, 1, -0.784));
            Built b = new Built(net, 1, false, new Random(k));
            b.root.step();
            b.assertMatchesReference("step", 1e-7);
        }
    }

    /** The bare solver: one step of a resistive network is the exact DC answer. */
    @Property(tries = 300)
    void solverMatchesReferenceAndKcl(@ForAll @IntRange(min = 1, max = 12) int n, @ForAll long seed) {
        Random rnd = new Random(seed);
        Built b = new Built(RandomNet.random(rnd, n, 0.1, 1e5, rnd.nextInt(4)), 1, false, rnd);
        b.root.step();
        b.assertFinite("step");
        b.assertMatchesReference("step", 1e-7);
        assertEquals(1, b.root.getSubSystemCount(), "a connected network is one SubSystem");
    }

    /** Cable loads: chains of loads with two connections collapse into Lines; the physics must not change. */
    @Property(tries = 300)
    void lineCollapseKeepsThePhysics(@ForAll @IntRange(min = 2, max = 40) int n, @ForAll long seed) {
        Random rnd = new Random(seed);
        Built b = new Built(RandomNet.random(rnd, n, 0.1, 1e4, rnd.nextInt(3)), 1, true, rnd);
        b.root.step();
        b.assertFinite("step");
        b.assertMatchesReference("step", 1e-7);
    }

    /**
     * Between steps, resistors go to high impedance (a burnt device), change value, are removed (removeComponent +
     * breakConnection, what a device's disconnect does) and put back, and sources change or go. After every
     * change: no NaN anywhere, and the grounded part of the circuit is the exact DC answer (a part cut off from
     * ground has no defined voltage; it only has to stay finite).
     */
    @Property(tries = 300)
    void topologyChangesBetweenSteps(@ForAll @IntRange(min = 2, max = 12) int n, @ForAll long seed,
                                     @ForAll @IntRange(min = 1, max = 25) int ops, @ForAll boolean loads) {
        Random rnd = new Random(seed);
        Built b = new Built(RandomNet.random(rnd, n, 0.1, 1e4, 0), 1, loads, rnd);
        b.root.step();
        b.assertMatchesReference("initial", 1e-7);
        for (int op = 0; op < ops; op++) {
            int k = rnd.nextInt(b.res.size());
            Resistor r = b.res.get(k);
            RandomNet.Edge e = b.active.get(k);
            String what;
            switch (rnd.nextInt(5)) {
                case 0 -> { // burnt: high impedance
                    r.highImpedance();
                    b.active.set(k, new RandomNet.Edge(e.a(), e.b(), r.getR()));
                    what = "highImpedance " + k;
                }
                case 1 -> { // a new value
                    double v = RandomNet.logUniform(rnd, 0.1, 1e4);
                    r.setR(v);
                    b.active.set(k, new RandomNet.Edge(e.a(), e.b(), v));
                    what = "setR " + k + " " + v;
                }
                case 2 -> { // removed / put back
                    if (b.in[k]) {
                        b.root.removeComponent(r);
                        r.breakConnection();
                        b.in[k] = false;
                        what = "remove " + k;
                    } else {
                        r.connectTo(b.nodes[e.a()], b.node(e.b()));
                        b.root.addComponent(r);
                        b.in[k] = true;
                        // (an ElectricalConnection takes its loads' Rs again when added)
                        b.active.set(k, new RandomNet.Edge(e.a(), e.b(), r.getR()));
                        what = "re-add " + k;
                    }
                }
                case 3 -> { // a source changes or goes
                    int i = rnd.nextInt(n);
                    VoltageSource v = b.src[i];
                    if (v == null || Double.isNaN(b.net.vs[i])) {
                        what = "nothing";
                    } else if (rnd.nextBoolean()) {
                        b.net.vs[i] = Math.round(rnd.nextDouble() * 400 - 200);
                        v.setU(b.net.vs[i]);
                        what = "source " + i + " = " + b.net.vs[i];
                    } else {
                        b.root.removeComponent(v);
                        v.breakConnection();
                        b.net.vs[i] = Double.NaN;
                        what = "remove source " + i;
                    }
                }
                default -> what = "nothing";
            }
            int steps = 1 + rnd.nextInt(2);
            for (int s = 0; s < steps; s++) b.root.step();
            b.assertFinite("after op " + op + " (" + what + ")");
            b.assertMatchesReference("after op " + op + " (" + what + ")", b.conditionTolerance(1e-7));
        }
    }

    /**
     * Over 100 states: the network is split at ElectricalConnections into SubSystems coupled by
     * InterSystemAbstraction (half-resistors + Thevenin delay sources). In DC it must settle on the exact answer.
     */
    @Property(tries = 12)
    void interSystemAbstractionConvergesToTheDcAnswer(@ForAll @IntRange(min = 130, max = 200) int n, @ForAll long seed) {
        Random rnd = new Random(seed);
        RandomNet net = new RandomNet(n);
        for (int i = 1; i < n; i++) net.edges.add(new RandomNet.Edge(i, rnd.nextInt(i), 0)); // r: the loads' Rs
        for (int k = rnd.nextInt(n / 20 + 1); k > 0; k--) {
            int a = rnd.nextInt(n), c = rnd.nextInt(n);
            if (a != c) net.edges.add(new RandomNet.Edge(a, c, 0));
        }
        for (int i = 0; i < n; i++)
            if (rnd.nextDouble() < 0.05) net.edges.add(new RandomNet.Edge(i, -1, RandomNet.logUniform(rnd, 1, 1e4)));
        net.vs[rnd.nextInt(n)] = 100 + rnd.nextInt(400);
        for (int i = 0; i < n; i++) if (rnd.nextDouble() < 0.02) net.vs[i] = rnd.nextInt(400);
        Built b = new Built(net, 50, true, rnd, 0.1);
        for (int i = 0; i < 200; i++) b.root.step();
        assertTrue(b.root.getSubSystemCount() > 1, "expected a split network");
        b.assertFinite("200 steps");
        double[] before = new double[n];
        for (int i = 0; i < n; i++) before[i] = b.nodes[i].state;
        b.root.step();
        for (int i = 0; i < n; i++) assertEquals(before[i], b.nodes[i].state, 1e-6 * 500, "settled, node " + i);
        double[] ref = net.solve(b.activeEdges());
        for (int i = 0; i < n; i++)
            assertEquals(ref[i], b.nodes[i].state, 1e-3 * 500, "node " + i + " in " + net);
    }

    /** A Monopole conductance on a node acts as a resistor 1/g to ground, and stops acting once removed. */
    @Property(tries = 200)
    void monopoleIsAConductanceToGround(@ForAll @IntRange(min = 1, max = 10) int n, @ForAll long seed,
                                        @ForAll @DoubleRange(min = 1e-4, max = 10, maxIncluded = true) @net.jqwik.api.constraints.Scale(6) double g) {
        Random rnd = new Random(seed);
        RandomNet net = RandomNet.random(rnd, n, 0.1, 1e4, 0);
        int at = rnd.nextInt(n);
        Built b = new Built(net, 1, false, new Random(seed));
        TestConductance m = new TestConductance(g);
        m.connectTo((VoltageState) b.nodes[at]);
        b.root.addComponent(m);
        b.root.step();
        List<RandomNet.Edge> withM = new ArrayList<>(b.activeEdges());
        withM.add(new RandomNet.Edge(at, -1, 1 / g));
        double[] ref = net.solve(withM);
        for (int i = 0; i < n; i++) assertEquals(ref[i], b.nodes[i].state, 1e-7 * 500, "with the monopole, node " + i);

        b.root.removeComponent(m);
        m.breakConnection();
        b.root.step();
        b.assertMatchesReference("monopole removed", 1e-7);
    }

    /**
     * The solar panel's power source: two voltage sources to ground on its + and - loads, set each step from the
     * Thevenin equivalents so the pair delivers P, within Umax and Imax. Into a passive load it must deliver at most
     * P (exactly P when no limit applies), never more than Umax/Imax, and stay finite even open or shorted.
     */
    @Property(tries = 300)
    void powerSourceBipoleRespectsItsLimits(@ForAll @DoubleRange(min = 0, max = 2000) double p,
                                            @ForAll @DoubleRange(min = 1, max = 500) double umax,
                                            @ForAll @DoubleRange(min = 0.1, max = 100) double imax,
                                            @ForAll @DoubleRange(min = 1e-3, max = 1e7) @net.jqwik.api.constraints.Scale(4) double rLoad,
                                            @ForAll boolean groundedMinus, @ForAll long seed) {
        Random rnd = new Random(seed);
        RootSystem root = new RootSystem(DT, 50);
        ElectricalLoad pos = new ElectricalLoad(), neg = new ElectricalLoad(), x = new ElectricalLoad(), y = new ElectricalLoad();
        for (ElectricalLoad l : new ElectricalLoad[]{pos, neg, x, y}) {
            l.setRs(RandomNet.logUniform(rnd, 1e-3, 1));
            root.addState(l);
        }
        VoltageSource ps = new VoltageSource("pos", pos, null), ns = new VoltageSource("neg", neg, null);
        root.addComponent(ps);
        root.addComponent(ns);
        root.addComponent(new ElectricalConnection(pos, x));
        root.addComponent(new ElectricalConnection(neg, y));
        Resistor load = new Resistor(x, y).setR(rLoad);
        root.addComponent(load);
        if (groundedMinus) root.addComponent(new Resistor(y, null).setR(RandomNet.logUniform(rnd, 1e-2, 1e3)));
        PowerSourceBipole src = new PowerSourceBipole(pos, neg, ps, ns);
        src.setUmax(umax);
        src.setImax(imax);
        src.setP(p);
        root.addProcess(src);
        for (int i = 0; i < 20; i++) root.step();

        double u = pos.state - neg.state, i = ps.getI();
        double delivered = pos.state * ps.getI() + neg.state * ns.getI();   // both sources, each to ground
        String ctx = "P " + p + " Umax " + umax + " Imax " + imax + " Rload " + rLoad + " grounded- " + groundedMinus
            + ": U " + u + " I " + i + " (neg source " + ns.getI() + "), delivered " + delivered;
        assertTrue(Double.isFinite(u) && Double.isFinite(i) && Double.isFinite(ns.getI()), ctx);
        double eps = 1e-6;
        // grounded minus: the two sources' Thevenin estimates each assume the other fixed, so a little over P (1%)
        assertTrue(delivered <= p * (1 + (groundedMinus ? 1e-2 : eps)) + 1e-6, "delivers more than P: " + ctx);
        assertTrue(u <= umax * (1 + eps) + 1e-9, "above Umax: " + ctx);
        assertTrue(Math.abs(i) <= imax * (1 + eps) + 1e-9, "above Imax: " + ctx);
        if (groundedMinus) return;
        // floating: what the load gets is P, unless a limit applies first
        double rTotal = rLoad + pos.getRs() + x.getRs() + neg.getRs() + y.getRs();
        double expected = Math.min(p, Math.min(umax * umax / rTotal, imax * imax * rTotal));
        assertEquals(expected, delivered, 1e-4 * Math.max(1, expected), "power delivered: " + ctx);
    }
}
