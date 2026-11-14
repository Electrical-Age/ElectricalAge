package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the spec's flux lemmas (spec/FluidSpec/Flux.lean) assume of the numbers the fluids hand the network, checked in
 * doubles: streams and shares are not negative, shares sum to at most 1, windows lie in [0, 1], densities are positive.
 */
class FluxShapeTest {
    /** Two equal nodes joined at one height, so the edge's pressure difference is its boost alone. */
    static Network.Edge pair(double g) {
        Network net = new Network();
        Network.Node a = net.add(new Network.Node(1e-2, 0, 1, false).fill(3, 0.01, 300));
        Network.Node b = net.add(new Network.Node(1e-2, 0, 1, false).fill(3, 0.01, 300));
        return net.connect(a, 0, b, 0, g);
    }

    /**
     * Neither stream of a pipe is ever negative, and they differ by the flow. (Above the band the return stream was
     * q+ - q, which rounding left a few 1e-16 kg/s below zero: a's contents sent as if they were b's.)
     */
    @Test
    void streamsAreNeverNegative() {
        Random r = new Random(1);
        Network.Edge e = pair(1);
        for (int i = 0; i < 400000; i++) {
            double x = switch (i % 4) {
                case 0 -> -45 + 90 * r.nextDouble();
                case 1 -> 25 + 6 * r.nextDouble();
                case 2 -> 30 + (r.nextInt(41) - 20) * Math.ulp(30.0);
                default -> Math.pow(10, 1.5 + 4 * r.nextDouble()) * (r.nextBoolean() ? 1 : -1);
            };
            e.g = Math.pow(10, -9 + 6 * r.nextDouble());
            e.boost = x * Network.SPLIT_DP;
            double[] q = Network.streams(e);
            double flow = e.g * e.boost;
            assertTrue(q[0] >= 0 && q[1] >= 0, "streams " + q[0] + ", " + q[1] + " at dp " + e.boost + ", g " + e.g);
            assertEquals(flow, q[0] - q[1], 4 * Math.ulp(Math.max(q[0], q[1])), "net flow at dp " + e.boost);
            assertEquals(flow, Network.flow(e), 0, "the reported flow");
            double forward = q[0];
            e.kind = Network.Kind.CHECK;
            q = Network.streams(e);
            // (a check valve: a forward stream alone, none at all at or under balance, and never over the pipe's)
            assertTrue(q[1] == 0 && q[0] >= 0 && q[0] <= forward && (x > 0 || q[0] == 0), "a check valve at dp " + e.boost + ": " + q[0] + ", " + q[1]);
            // (far above its cracking pressure, the pipe's slope)
            if (x > 40) assertEquals(e.g, q[2], 1e-9 * e.g, "a check valve's slope at dp " + e.boost);
            e.kind = Network.Kind.PIPE;
        }
    }

    /** The streams of an edge and of the same edge turned round mirror each other, to the softplus's cut at 30 bands. */
    @Test
    void streamsMirror() {
        Network.Edge e = pair(1e-5);
        double cut = e.g * Network.SPLIT_DP * Math.exp(-30);
        for (double x = -40; x <= 40; x += 0.37) {
            e.boost = x * Network.SPLIT_DP;
            double[] q = Network.streams(e);
            e.boost = -e.boost;
            double[] back = Network.streams(e);
            assertEquals(q[0], back[1], cut + 1e-12 * q[0], "q+ against the mirror's q- at " + x);
            assertEquals(q[1], back[0], cut + 1e-12 * q[1], "q- against the mirror's q+ at " + x);
        }
    }

    /** Every edge's draws, windows and densities over random networks as they run, both fluids. */
    @Test
    void drawsAndWindowsKeepTheirShapes() {
        for (int kind = 0; kind < 2; kind++) {
            Limiter.Tap before = Limiter.TAP;
            double[] held = {0};
            Limiter.TAP = (C, y0, moves, passes, scale, y) -> {
                if (before != null) before.call(C, y0, moves, passes, scale, y);
                if (passes == 0) return;
                double[] asked = new double[y0.length];
                for (double[] mv : moves) if (mv[0] >= 0 && mv[2] < C - 1) asked[C * (int) mv[0] + (int) mv[2]] += mv[3];
                for (int i = 0; i < y0.length; i++) if (i % C != C - 1) held[0] = Math.max(held[0], asked[i] * (1 - scale[i]));
            };
            try {
                for (long seed = 1; seed <= 25; seed++) {
                    Network net = kind == 0 ? NetworkTest.level(seed) : MixNetworkTest.level(seed);
                    look(net, "seed " + seed + " at the start");
                    for (int s = 0; s < 400; s++) {
                        long halved = net.stats.substeps;
                        held[0] = 0;
                        net.step(0.05);
                        String at = (kind == 0 ? "water/air" : "mixed") + " seed " + seed + " step " + s;
                        // (a step that halved also counts the attempts it threw away, which may ask for anything)
                        if (net.stats.substeps == halved) assertTrue(held[0] < 1e-9, at + ": the limiter withheld " + held[0] + " kg from a cell in a step that converged");
                        look(net, at);
                    }
                }
            } finally {
                Limiter.TAP = before;
            }
        }
    }

    static void look(Network net, String at) {
        for (Network.Edge e : net.edges) {
            double[] q = Network.streams(e);
            assertTrue(q[0] >= 0 && q[1] >= 0, at + ": streams " + q[0] + ", " + q[1]);
            for (int side = 0; side < 2; side++) {
                NodeFluid fl = (side == 0 ? e.a : e.b).fluid;
                double f = side == 0 ? e.fa : e.fb;
                int S = fl.species();
                double[] d = fl.draw(f), l = fl.liquidDraw(f), g = fl.gasDraw();
                double sum = 0, ls = 0, gs = 0;
                for (int k = 0; k < S; k++) {
                    // (a mass that a step left a rounding below zero shows as a share of -1e-30 or so)
                    assertTrue(d[k] >= -1e-20 && l[k] >= -1e-20 && g[k] >= -1e-20, at + ": shares " + d[k] + ", " + l[k] + ", " + g[k]);
                    sum += d[k];
                    ls += l[k];
                    gs += g[k];
                }
                assertTrue(sum >= -1e-20 && sum <= 1 + 1e-12, at + ": a draw's shares sum to " + sum);
                assertEquals(1, ls, 1e-12, at + ": the liquid draw's shares");
                // (the gas's shares leave out the air cushion, which is never drawn)
                assertTrue(gs >= -1e-20 && gs < 1, at + ": the gas draw's shares sum to " + gs);
                double w = fl.liquidWindow(f);
                assertTrue(w >= 0 && w <= 1, at + ": window " + w);
                assertTrue(fl.rhoLiquid(f) > 0 && fl.rhoGas() > 0, at + ": densities " + fl.rhoLiquid(f) + ", " + fl.rhoGas());
                assertTrue(fl.T() > 0, at + ": T " + fl.T());
            }
        }
    }

    /**
     * The exchange swaps equal volumes: the liquid it sends down, at the density it is given, fills the volume of the
     * gas it brings up, also where the port sees two layers at once.
     */
    @Test
    void exchangedLiquidHasTheDensityItIsSentAt() {
        Species[] sp = {Species.WATER, MixFluidTest.GASOLINE, Species.AIR};
        Network.Node n = MixFluidTest.node(sp, 1e-3, new double[]{0.3, 0.2, 0.001}, 300);
        MixFluid f = MixFluidTest.mf(n);
        double interface_ = f.st.top[0].v;
        boolean mixed = false;
        for (double df = -1.5 * Network.BAND; df <= 0.5 * Network.BAND; df += Network.BAND / 16) {
            double at = interface_ + df;
            double[] l = f.liquidDraw(at);
            double volume = 0;
            for (int i = 0; i < 2; i++) volume += l[i] * f.st.v[i].v;
            assertEquals(1, f.rhoLiquid(at) * volume, 1e-12, "density x volume per kg at " + df);
            mixed |= l[0] > 0.2 && l[1] > 0.2;
        }
        assertTrue(mixed, "some port saw both layers");
    }

    /**
     * The shut-off shapes (a check valve's stream, a pump's gate, a root law's drive): nothing at or under 0, exactly,
     * no jump in value or slope there, never falling, and their slopes are their differences'.
     */
    @Test
    void shutOffShapes() {
        java.util.function.DoubleFunction<double[]> sp = x -> new double[]{Network.checkSp(x), Network.checkSg(x)};
        java.util.List<java.util.function.DoubleFunction<double[]>> shapes = java.util.List.of(sp, Network::checkGate, Network::checkRoot);
        for (var f : shapes) {
            for (double x = -50; x <= 0; x += 0.25) {
                assertEquals(0, f.apply(x)[0], 0, "shut at " + x);
                assertEquals(0, f.apply(x)[1], 0, "slope shut at " + x);
            }
            assertTrue(f.apply(1e-6)[0] < 1e-11 && f.apply(1e-6)[1] < 1e-5, "it opens from nothing");
            double last = 0;
            for (double x = 0.01; x <= 60; x += 0.0173) {
                double[] v = f.apply(x);
                double h = 1e-5, fd = (f.apply(x + h)[0] - f.apply(x - h)[0]) / (2 * h);
                assertTrue(v[0] >= last, "never falling at " + x);
                assertEquals(fd, v[1], 1e-6 * Math.max(1, Math.abs(fd)), "slope at " + x);
                last = v[0];
            }
        }
        // (the spec's checkSp_spec: never more than a pipe's flow, never more than the lag behind it)
        double lag = (1 + Math.log1p(Math.exp(-1))) / (1 - 1 / (1 + Math.E));
        for (double x = -5; x <= 60; x += 0.0173) {
            double v = Network.checkSp(x);
            assertTrue(v <= Math.max(0, x) && v >= x - lag - 1e-12, "a check valve's stream between a pipe's and one the lag behind, at " + x);
        }
        assertEquals(60 - lag, Network.checkSp(60), 1e-12, "a check valve's stream far above");
        assertEquals(1, Network.checkSg(60), 1e-12, "a check valve's slope far above");
        assertEquals(1, Network.checkGate(60)[0], 1e-12, "a gate open far above");
        assertEquals(1, Network.checkRoot(60)[1], 1e-3, "a root law's drive rises as x far above");
    }
}
