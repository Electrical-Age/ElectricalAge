package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A heat exchanger as two chains of N segments coupled by conductance at matching positions (notes 23-hx.md): how
 * close N slices come to counterflow effectiveness, and what it costs. Hot air at 600 K runs through side A past side B
 * held at 373 K (a boiling pool); the effectiveness is the heat taken over what the air could give, at NTU 2.5.
 */
class HeatChainTest {
    static final double DT = 0.05;

    record Result(double eps, double ntu, double msPerStep) {
    }

    static Result run(int n, double ntuWanted) {
        Network net = new Network();
        double pIn = 1.2e5, pOut = 1.0e5, T = 600, V = 2e-3;
        // (air: R 287, cv 718: m = p V / (R T))
        var in = net.add(new Network.Node(10, 0, 1, true).fill(0, pIn * 10 / (287.05 * T), T));
        var out = net.add(new Network.Node(10, 0, 1, true).fill(0, pOut * 10 / (287.05 * 400), 400));
        Network.Node[] a = new Network.Node[n], b = new Network.Node[n];
        for (int i = 0; i < n; i++) {
            a[i] = net.add(new Network.Node(V / n, 0, 1, false).fill(0, 1.1e5 * V / n / (287.05 * 450), 450));
            b[i] = net.add(new Network.Node(1, 0, 1, true).fill(500, 1e-4, 373.15));   // (held at saturation: a boiling pool)
        }
        double g = 2e-6;   // (kg/s/Pa through the whole side: n joints in series each n g, the ends 2 n g)
        net.connect(in, 0.5, a[0], 0.5, 2 * n * g);
        for (int i = 0; i + 1 < n; i++) {
            net.connect(a[i], 0, a[i + 1], 0, n * g / 2);
            net.connect(a[i], 1, a[i + 1], 1, n * g / 2);
        }
        net.connect(a[n - 1], 0.5, out, 0.5, 2 * n * g);
        Network.Edge[] c = new Network.Edge[n];
        for (int i = 0; i < n; i++) {
            c[i] = net.connect(a[i], 0.5, b[i], 0.5, 0);
        }
        // first find the flow, then set UA for the NTU wanted at it
        for (int s = 0; s < 400; s++) net.step(DT);
        double m = net.edges.get(0).flow, cp = 1005;
        double ua = ntuWanted * m * cp;
        for (Network.Edge e : c) e.conductance = ua / n;
        long t0 = System.nanoTime();
        int steps = 2000;
        for (int s = 0; s < steps; s++) net.step(DT);
        double ms = (System.nanoTime() - t0) / 1e6 / steps;
        double q = 0;
        for (Network.Edge e : c) q += e.moved[e.moved.length - 1] / DT;
        m = net.edges.get(0).flow;
        double eps = q / (m * cp * (T - 373.15));
        return new Result(eps, ua / (m * cp), ms);
    }

    @Test
    void slicesApproachCounterflow() {
        StringBuilder out = new StringBuilder();
        double last = 0;
        for (int n : new int[]{1, 2, 4, 8, 16}) {
            Result r = run(n, 2.5);
            out.append(String.format("N %2d: effectiveness %.3f at NTU %.2f (ideal %.3f, well mixed %.3f), %.2f ms a step%n",
                n, r.eps, r.ntu, 1 - Math.exp(-r.ntu), r.ntu / (1 + r.ntu), r.msPerStep));
            if (n == 8) last = r.eps;
        }
        System.out.print(out);
        assertTrue(last > 0.85, "8 slices reach 85 % at NTU 2.5:\n" + out);
    }
}
