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
        String set = System.getenv("STUCK_SET");   // e.g. CRAWL_OK=1e-12,STALL_OK=1e-12,MAX_ITERS=60
        if (set != null) for (String kv : set.split(",")) {
            String[] p = kv.split("=");
            var f = p[0].contains(".") ? Class.forName("mods.eln.fluidcore." + p[0].substring(0, p[0].indexOf('.'))).getDeclaredField(p[0].substring(p[0].indexOf('.') + 1)) : Network.class.getDeclaredField(p[0]);
            f.setAccessible(true);
            if (f.getType() == int.class) f.setInt(null, Integer.parseInt(p[1]));
            else if (f.getType() == boolean.class) f.setBoolean(null, Boolean.parseBoolean(p[1]));
            else f.setDouble(null, Double.parseDouble(p[1]));
            System.out.println("set " + kv);
        }
        String gs = System.getenv("STUCK_GSCALE");   // e.g. 8,9,21,22:0.1
        if (gs != null) {
            String[] p = gs.split(":");
            double k = Double.parseDouble(p[1]);
            for (String e : p[0].split(",")) { Network.Edge ed = net.edges.get(Integer.parseInt(e)); ed.g *= k; ed.gBack *= k; }
        }
        String cs = System.getenv("STUCK_CSCALE");   // e.g. 25,26:0
        if (cs != null) {
            String[] p = cs.split(":");
            for (String e : p[0].split(",")) net.edges.get(Integer.parseInt(e)).conductance *= Double.parseDouble(p[1]);
        }
        String zero = System.getenv("STUCK_ZERO");   // node:comp,... masses set to 0 (energy kept)
        if (zero != null) for (String nc : zero.split(",")) {
            String[] p = nc.split(":");
            Network.Node n = net.nodes.get(Integer.parseInt(p[0]));
            n.m[Integer.parseInt(p[1])] = 0;
            n.update();
        }
        String zt = System.getenv("STUCK_ZEROTRACE");   // masses under this share of their node's set to 0
        if (zt != null) for (Network.Node n : net.nodes) {
            if (n.fixed) continue;
            double tot = 0;
            for (double m : n.m) tot += m;
            for (int j = 0; j < n.m.length; j++) if (Math.abs(n.m[j]) < Double.parseDouble(zt) * tot) n.m[j] = 0;
            n.update();
        }
        String watch = System.getenv("STUCK_WATCH");   // node indices whose P and T are printed each step
        String warm = System.getenv("STUCK_WARM");   // steps of a throwaway copy first (JIT warm-up)
        if (warm != null) {
            Network w = NetworkDump.read(text, new double[1]);
            for (int i = 0; i < Integer.parseInt(warm); i++) w.step(dt[0]);
        }
        double halfMs = 0;
        long tAll = System.nanoTime();
        for (int s = 0; s < steps; s++) {
            long fb0 = Sparse.FALLBACKS, rb0 = Sparse.REBUILDS; long it0 = net.stats.iterations, so0 = net.stats.solves, ss0 = net.stats.substeps, f0 = net.stats.failures;
            String flue = System.getenv("STUCK_FLUE");   // nodes:kg/s:K, the furnaces' Flue.step before each step
            if (flue != null) {
                String[] p = flue.split(":");
                double rate = Double.parseDouble(p[1]), TK = Double.parseDouble(p[2]);
                for (String w : p[0].split(",")) {
                    Network.Node n = net.nodes.get(Integer.parseInt(w));
                    MixFluid mf = (MixFluid) n.fluid;
                    int j = mf.sp.length - 1;
                    double[] one = new double[mf.sp.length];
                    one[j] = 1;
                    double h = mf.energyOf(one, TK) + mf.sp[j].r * TK;
                    double dm = rate * dt[0];
                    n.m[j] += dm;
                    n.U += dm * h;
                    n.update();
                }
            }
            String jr = System.getenv("STUCK_JROWS");
            if (jr != null && s % 10 == 5) { int[] rows = java.util.Arrays.stream(jr.split(",")).mapToInt(Integer::parseInt).toArray(); System.out.print("JROWS step " + s + "\n" + net.rowJacobianErrors(dt[0], rows)); }
            String fr = System.getenv("STUCK_FLUX");
            if (fr != null) for (String w : fr.split(",")) { Network.Node n = net.nodes.get(Integer.parseInt(w)); System.out.printf("flux %d node %s P %.6f T %.3f lvl %.5f:%s%n", s, w, n.P() / 1e5, n.T() - 273.15, n.level(), net.fluxReport(n)); }
            long u0 = Network.UPDATES, r0 = Network.RESIDUALS, j0 = Network.JACOBIANS, b0 = MixFluid.BRACKETS, p0 = MixFluid.POLISH_ITS, sm0 = MixFluid.SAME;
            long t0 = java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
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
            String tr = System.getenv("STUCK_TRACE");
            if (tr != null && (tr.equals("all") || ("," + tr + ",").contains("," + s + ","))) Network.TRACE = new StringBuilder();
            net.step(dt[0]);
            if (Network.TRACE != null) {
                for (String l : Network.TRACE.toString().split("\n")) System.out.println("TR " + s + " " + l);
                Network.TRACE = null;
            }
            if (Network.DEBUG != null) {
                for (String l : Network.DEBUG.toString().split("\n")) {
                    String t = l.trim();
                    if (t.startsWith("it ") || t.startsWith("held") || t.startsWith("row") || t.startsWith("stalled") || t.startsWith("ls 0"))
                        System.out.println("DBG " + (t.length() > 160 && !t.startsWith("stalled") ? t.substring(0, 160) : t));
                }
                Network.DEBUG = null;
            }
            double ms = (java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - t0) / 1e6;
            if (s >= steps / 2) halfMs += ms;
            int at = net.restlessAt;
            String who = at < 0 ? "" : String.format("free node %d comp %d", at / C, at % C);
            Network.Node rn = at < 0 ? null : free.get(at / C);
            System.out.printf("counts updates %d residuals %d jacobians %d brackets %d polish %d same %d%n", Network.UPDATES - u0, Network.RESIDUALS - r0, Network.JACOBIANS - j0, MixFluid.BRACKETS - b0, MixFluid.POLISH_ITS - p0, MixFluid.SAME - sm0);
            System.out.printf("fallbacks %d rebuilds %d", Sparse.FALLBACKS - fb0, Sparse.REBUILDS - rb0);
            System.out.printf("step %d: %.2f ms it %d solves %d sub %d fail %d done %.4f restless %.3g at %d (%s)%s%n", s, ms, net.stats.iterations - it0,
                net.stats.solves - so0, net.stats.substeps - ss0, net.stats.failures - f0, net.done, net.restless, at, who,
                rn == null ? "" : String.format(" -> node %d m %s U %.5g P %.4f bar T %.2f C", net.nodes.indexOf(rn), java.util.Arrays.toString(rn.m), rn.U, rn.P() / 1e5, rn.T() - 273.15));
            if (watch != null) {
                StringBuilder b = new StringBuilder("  watch");
                for (String w : watch.split(",")) { Network.Node n = net.nodes.get(Integer.parseInt(w)); b.append(String.format(" n%s P %.6f T %.3f lvl %.4f", w, n.P() / 1e5, n.T() - 273.15, n.level())); }
                System.out.println(b);
            }
            if (System.getenv("STUCK_FLOWS") != null) {
                StringBuilder b = new StringBuilder("  flows");
                for (int i = 0; i < net.edges.size(); i++) b.append(String.format(" %d:%.3g", i, net.edges.get(i).flow));
                System.out.println(b);
            }
        }
        System.out.printf("total %.1f ms for %d steps; last half %.2f ms/step%n", (System.nanoTime() - tAll) / 1e6, steps, halfMs / (steps - steps / 2));
        for (int i = 0; i < net.nodes.size(); i++) {
            Network.Node n = net.nodes.get(i);
            System.out.printf("end node %d m %s P %.4g bar T %.2f C%n", i, java.util.Arrays.toString(n.m), n.P() / 1e5, n.T() - 273.15);
        }
    }
}
