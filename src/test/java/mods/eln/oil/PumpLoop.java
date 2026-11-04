package mods.eln.oil;

import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNetwork;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;

/**
 * A condensate pump stepped as CondenserVessel steps it: a vessel the condensate keeps coming into (fixed), a run of
 * nearly liquid-full pipe nodes, and a fixed boiler fed through a check valve.
 */
final class PumpLoop {
    /**
     * Over the last CHUNK steps: the first pipe node's pressure range (Pa), the mean pump and feed flows (kg/s), the
     * back-pressure at the end, and how many steps it took for every node's mean pressure to stop moving.
     */
    record Result(double pLo, double pHi, double pump, double feed, double back, int steps) {
    }

    /** Stepped in chunks until no node's mean pressure over a chunk moves by more than 1 Pa, at most MAX_STEPS. */
    static final int CHUNK = 200, MAX_STEPS = 40000;

    static Result run(CondensatePump curve, double speed, double boilerPa, double pipeK, int pipes, double pipeVolume) {
        FluidNetwork net = new FluidNetwork();
        FluidNode vessel = net.add(new FluidNode(0.06, 0, 1, true).fill(30, 0.1e5, 320));   // (fixed: the condensate keeps coming)
        FluidNode[] p = new FluidNode[pipes];
        for (int i = 0; i < pipes; i++)
            p[i] = net.add(new FluidNode(pipeVolume, 0, 0.25, false).fill(pipeVolume * 0.95 * Water.RHO_L, Math.min(boilerPa, curve.shutoffPa), 320));
        FluidNode boiler = net.add(new FluidNode(1, 0, 1, true).fill(500, boilerPa, 400));
        FluidEdge pump = net.connect(vessel, 0, p[0], 0, curve.freeKgPerS / curve.shutoffPa * Water.MU_L / Water.RHO_L).kind(FluidEdge.Kind.CHECK);
        for (int i = 0; i + 1 < pipes; i++) net.connect(p[i], 0.5, p[i + 1], 0.5, pipeK);
        FluidEdge feed = net.connect(p[pipes - 1], 0.5, boiler, 0, pipeK).kind(FluidEdge.Kind.CHECK);
        double lo = 0, hi = 0, qp = 0, qf = 0, back = 0;
        double[] last = new double[pipes];
        int t = 0;
        while (t < MAX_STEPS) {
            lo = Double.MAX_VALUE;
            hi = -Double.MAX_VALUE;
            qp = qf = 0;
            double[] mean = new double[pipes];
            for (int c = 0; c < CHUNK; c++, t++) {
                // the back-pressure as CondenserVessel.backPressure reads it: outlet over the vessel's bottom port, no fall
                back = p[0].pressureAt(0) - vessel.pressureAt(0);
                pump.open = speed * curve.lineShare(back);
                pump.boost = curve.boost(back);
                net.step(0.05);
                lo = Math.min(lo, p[0].P());
                hi = Math.max(hi, p[0].P());
                qp += pump.flow / CHUNK;
                qf += feed.flow / CHUNK;
                for (int i = 0; i < pipes; i++) mean[i] += p[i].P() / CHUNK;
            }
            double moved = 0;
            for (int i = 0; i < pipes; i++) moved = Math.max(moved, Math.abs(mean[i] - last[i]));
            last = mean;
            if (moved < 1) break;
        }
        return new Result(lo, hi, qp, qf, back, t);
    }

    /** The first pipe node's lowest and highest pressure (Pa) once settled, for a pump sized like CondensatePumpTest's. */
    static double[] run(double shutoffPa, double boilerPa, double pipeK) {
        Result r = run(new CondensatePump(shutoffPa, 0.75 * shutoffPa, 0.01), 1, boilerPa, pipeK, 4, 0.0005);
        return new double[]{r.pLo(), r.pHi()};
    }
}
