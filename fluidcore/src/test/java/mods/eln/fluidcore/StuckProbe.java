package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/** Probe (STUCK_PROBE=path): steps a dump N times with the game's settings and prints what keeps it busy. */
class StuckProbe {
    @Test
    void probe() throws Exception {
        String path = System.getenv("STUCK_PROBE");
        if (path == null) return;
        int steps = Integer.parseInt(System.getenv().getOrDefault("STUCK_STEPS", "60"));
        String text = java.nio.file.Files.readString(java.nio.file.Path.of(path));
        Network.MAX_DEPTH = 8;
        Network.CUT_SHORT = true;
        double[] dt = new double[1];
        Network net = NetworkDump.read(text, dt);
        List<Network.Node> free = new ArrayList<>();
        for (Network.Node n : net.nodes) if (!n.fixed) free.add(n);
        int C = net.nodes.get(0).m.length + 1;
        System.out.printf("dt %.4f, %d nodes (%d free), %d edges, C %d%n", dt[0], net.nodes.size(), free.size(), net.edges.size(), C);
        for (int i = 0; i < net.nodes.size(); i++) {
            Network.Node n = net.nodes.get(i);
            System.out.printf("node %d (free %d) V %.4g z %.3g H %.3g fixed %s m %s P %.4g bar T %.1f C level %.3f wallUA %.3g%n", i, free.indexOf(n), n.V, n.z, n.H,
                n.fixed, java.util.Arrays.toString(n.m), n.P() / 1e5, n.T() - 273.15, n.level(), n.wallUA);
        }
        for (int i = 0; i < net.edges.size(); i++) {
            Network.Edge e = net.edges.get(i);
            System.out.printf("edge %d %d->%d fa %.2f fb %.2f g %.3g gBack %.3g kind %s boost %.3g exch %.3g cond %.3g qMax %.3g h0 %.3g speed %.3g nozzle %.3g open %.3g drop %.3g gas %s setPa %.4g relief %s gasOnly %s%n",
                i, net.nodes.indexOf(e.a), net.nodes.indexOf(e.b), e.fa, e.fb, e.g, e.gBack, e.kind, e.boost, e.exchange, e.conductance, e.qMax, e.h0, e.speed,
                e.nozzle, e.open, e.drop, e.gas, e.setPa, e.relief, e.gasOnly);
        }
        long tAll = System.nanoTime();
        for (int s = 0; s < steps; s++) {
            long fb0 = Sparse.FALLBACKS, rb0 = Sparse.REBUILDS; long it0 = net.stats.iterations, so0 = net.stats.solves, ss0 = net.stats.substeps, f0 = net.stats.failures;
            long t0 = System.nanoTime();
            String vent = System.getenv("STUCK_VENT");
            if (vent != null) {
                // (the flare stack between steps: the gas over the air's pressure leaves by its tip, FlareStackElement.burn;
                // taken out at the node's own specific energy, a slight overestimate of what it keeps)
                Network.Node v = net.nodes.get(Integer.parseInt(vent));
                double f = 1 - 101325 / v.P();
                if (f > 0) {
                    for (int j = 0; j < v.m.length; j++) v.m[j] *= 1 - f;
                    v.U *= 1 - f;
                    v.update();
                }
            }
            String dbg = System.getenv("STUCK_DEBUG");
            if (dbg != null && Integer.parseInt(dbg) == s) Network.DEBUG = new StringBuilder();
            net.step(dt[0]);
            if (Network.DEBUG != null) {
                for (String l : Network.DEBUG.toString().split("\n")) {
                    String t = l.trim();
                    if (t.startsWith("it ") || t.startsWith("held") || t.startsWith("row") || t.startsWith("stalled") || t.startsWith("ls 0"))
                        System.out.println("DBG " + (t.length() > 160 ? t.substring(0, 160) : t));
                }
                Network.DEBUG = null;
            }
            double ms = (System.nanoTime() - t0) / 1e6;
            int at = net.restlessAt;
            String who = at < 0 ? "" : String.format("free node %d comp %d", at / C, at % C);
            Network.Node rn = at < 0 ? null : free.get(at / C);
            System.out.printf("fallbacks %d rebuilds %d", Sparse.FALLBACKS - fb0, Sparse.REBUILDS - rb0);
            System.out.printf("step %d: %.2f ms it %d solves %d sub %d fail %d done %.4f restless %.3g at %d (%s)%s%n", s, ms, net.stats.iterations - it0,
                net.stats.solves - so0, net.stats.substeps - ss0, net.stats.failures - f0, net.done, net.restless, at, who,
                rn == null ? "" : String.format(" -> node %d m %s U %.5g P %.4f bar T %.2f C", net.nodes.indexOf(rn), java.util.Arrays.toString(rn.m), rn.U, rn.P() / 1e5, rn.T() - 273.15));
            if (System.getenv("STUCK_FLOWS") != null) {
                StringBuilder b = new StringBuilder("  flows");
                for (int i = 0; i < net.edges.size(); i++) b.append(String.format(" %d:%.3g", i, net.edges.get(i).flow));
                System.out.println(b);
            }
        }
        System.out.printf("total %.1f ms for %d steps%n", (System.nanoTime() - tAll) / 1e6, steps);
        for (int i = 0; i < net.nodes.size(); i++) {
            Network.Node n = net.nodes.get(i);
            System.out.printf("end node %d m %s P %.4g bar T %.2f C%n", i, java.util.Arrays.toString(n.m), n.P() / 1e5, n.T() - 273.15);
        }
    }
}
