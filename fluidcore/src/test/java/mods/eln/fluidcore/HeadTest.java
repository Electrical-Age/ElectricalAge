package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Edges whose two ports are at different heights (Edge.dz), each stream paying the head of its own fluid, and streams
 * with their own conductances (Edge.gBack): exact slopes, conservation, and rest without circulation.
 */
class HeadTest {
    static final double DT = 0.05;

    /** A tank, a short pipe above its middle, a lower vessel and a reservoir under it, joined at many heights. */
    static Network mixed() {
        Network net = new Network();
        var tank = net.add(new Network.Node(1e-3, 0, 1, false).fill(0.48, 0.5e-3, 330));
        var pipe = net.add(new Network.Node(1e-4, 0.5, 0.2, false).fill(0.01, 0.1e-3, 300));
        var low = net.add(new Network.Node(5e-4, 0.2, 0.5, false).fill(0.1, 0.4e-3, 310));
        var res = net.add(new Network.Node(1, -1, 1, true).fill(800, 0.1, 293));
        // a port inside its window (the tank's surface at about 0.48), up into the pipe
        net.connect(tank, 0.48, pipe, 0, 1e-6);
        // gas to gas, 0.3 m apart, the return stream a fifth as conductive
        net.connect(tank, 1.0, pipe, 1.0, 1e-6).gBack = 2e-7;
        // a check valve with a pump's head, down 0.45 m
        Network.Edge chk = net.connect(tank, 0, low, 0.5, 2e-7);
        chk.kind = Network.Kind.CHECK;
        chk.boost = 2e4;
        // the reservoir's middle 0.5 m under the tank's floor
        net.connect(res, 0.5, tank, 0, 3e-7);
        return net;
    }

    @Test
    void slopesMatchFiniteDifferences() {
        Network net = mixed();
        for (int s = 0; s < 40; s++) {
            String r = net.checkJacobian(DT);
            double worst = Double.parseDouble(r.substring(0, r.indexOf(' ')));
            assertTrue(worst < 1e-3, "step " + s + ": " + r);
            net.step(DT);
        }
    }

    @Test
    void massAndEnergyBalance() {
        Network net = mixed();
        double m0 = 0, u0 = 0;
        for (Network.Node n : net.nodes) if (!n.fixed) { m0 += n.mass(); u0 += n.U; }
        double inM = 0, inU = 0;
        for (int s = 0; s < 400; s++) {
            net.step(DT);
            // what the reservoir's edge (the last) brought in
            Network.Edge e = net.edges.get(net.edges.size() - 1);
            for (int k = 0; k < e.moved.length - 1; k++) inM += e.moved[k];
            inU += e.moved[e.moved.length - 1];
        }
        double m1 = 0, u1 = 0;
        for (Network.Node n : net.nodes) if (!n.fixed) { m1 += n.mass(); u1 += n.U; }
        assertEquals(m0 + inM, m1, 1e-10 * m1, "mass: the reservoir's inflow and nothing else");
        assertEquals(u0 + inU, u1, 1e-8 * Math.abs(u1), "energy");
        assertEquals(0, net.stats.failures, "failed sub-steps");
    }

    /** Runs a network to rest; the largest net flow left on any edge, as a share of its g SPLIT_DP. */
    static double restless(Network net, int steps) {
        for (int s = 0; s < steps; s++) {
            net.step(DT);
            if ((s + 1) % 3000 == 0) {
                StringBuilder b = new StringBuilder("  t " + (s + 1) * DT + " s: flows");
                for (Network.Edge e : net.edges) b.append(String.format(" %.3e", Network.flow(e)));
                b.append(" levels");
                for (Network.Node n : net.nodes) b.append(String.format(" %.4f", n.level()));
                b.append(" P");
                for (Network.Node n : net.nodes) b.append(String.format(" %.1f", n.P()));
                System.out.println(b);
            }
        }
        double worst = 0;
        for (Network.Edge e : net.edges) worst = Math.max(worst, Math.abs(Network.flow(e)) / (e.g * Network.SPLIT_DP));
        return worst;
    }

    /**
     * A loop through two tanks at different heights, joined liquid to liquid, gas to gas and from one's liquid to the
     * other's gas, started out of balance: it settles with nothing flowing round.
     */
    @Test
    void aLoopThroughLiquidAndGasPortsComesToRest() {
        Network net = new Network();
        var a = net.add(new Network.Node(1e-3, 0, 1, false).fill(0.85, 0.2e-3, 300));
        var b = net.add(new Network.Node(1e-3, 0.3, 1, false).fill(0.1, 1.0e-3, 300));
        net.connect(a, 0, b, 0, 1e-6);
        net.connect(a, 1, b, 1, 1e-6).gBack = 3e-7;
        net.connect(a, 0.25, b, 0.75, 1e-6);
        double r = restless(net, 18000);
        System.out.printf("loop through liquid and gas ports: worst net flow %.3g of g S; levels %.4f %.4f%n", r, a.level(), b.level());
        assertTrue(r < 1e-3, "still flowing round: " + r);
        assertEquals(0, net.stats.failures, "failed sub-steps");
    }

    /**
     * One tank with a riser beside it, joined tank top to riser floor and riser top to tank floor (the shape where a head
     * weighed by one fluid for the whole edge pumps round for ever): it settles.
     */
    @Test
    void aTankWithARiserBesideItComesToRest() {
        Network net = new Network();
        var t = net.add(new Network.Node(1e-3, 0, 1, false).fill(0.7, 0.35e-3, 300));
        var p = net.add(new Network.Node(1e-4, 0, 1, false).fill(0, 0.12e-3, 300));
        net.connect(t, 1, p, 0, 1e-6);
        net.connect(p, 1, t, 0, 1e-6);
        double r = restless(net, 18000);
        System.out.printf("tank with a riser: worst net flow %.3g of g S; levels %.4f %.4f%n", r, t.level(), p.level());
        assertTrue(r < 1e-3, "still flowing round: " + r);
        assertEquals(0, net.stats.failures, "failed sub-steps");
    }

    /**
     * A tank drained down to a side port on a pipe to a lower tank, gas joined over the top: the upper tank drains to
     * its port, where the port rests inside its window. There it draws mostly gas while its pressure still holds the last
     * few millimetres of liquid over it, so a little gas goes round through the two pipes until the surface reaches the
     * port; that dies away (slowly: the window's tail), it doesn't run for ever.
     */
    @Test
    void aTankDrainedToItsSidePortSettles() {
        Network net = new Network();
        var a = net.add(new Network.Node(1e-3, 0.5, 1, false).fill(0.6, 0.5e-3, 300));
        var b = net.add(new Network.Node(1e-3, 0, 1, false).fill(0.05, 1.1e-3, 300));
        net.connect(a, 0.4, b, 0.5, 1e-6);
        net.connect(a, 1, b, 1, 1e-6);
        double mid = restless(net, 6000), end = restless(net, 12000);
        System.out.printf("tank drained to its side port: net flow %.3g then %.3g of g S; levels %.4f %.4f%n", mid, end, a.level(), b.level());
        assertTrue(a.level() > 0.4 && a.level() < 0.41, "the upper tank drains into its port's window: " + a.level());
        assertTrue(end < 0.7 * mid, "what goes round dies away: " + mid + " then " + end);
        assertEquals(0, net.stats.failures, "failed sub-steps");
    }
}
