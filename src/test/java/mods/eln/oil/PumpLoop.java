package mods.eln.oil;

import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNetwork;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;

/** A condensate pump stepped as CondenserVessel steps it: a vessel, four liquid-full pipe nodes, a fixed boiler. */
final class PumpLoop {
    /** The first pipe node's lowest and highest pressure (Pa) over steps 400..600 at 20 steps a second. */
    static double[] run(double shutoffPa, double boilerPa, double pipeK) {
        CondensatePump curve = new CondensatePump(shutoffPa, 0.75 * shutoffPa, 0.01);
        FluidNetwork net = new FluidNetwork();
        FluidNode vessel = net.add(new FluidNode(0.06, 0, 1, false).fill(30, 0.1e5, 320));
        FluidNode[] p = new FluidNode[4];
        for (int i = 0; i < p.length; i++) p[i] = net.add(new FluidNode(0.0005, 0, 0.25, false).fill(0.0005 * 990, boilerPa, 320));
        FluidNode boiler = net.add(new FluidNode(1, 0, 1, true).fill(500, boilerPa, 400));
        FluidEdge pump = net.connect(vessel, 0, p[0], 0, curve.freeKgPerS / curve.shutoffPa * Water.MU_L / Water.RHO_L).kind(FluidEdge.Kind.CHECK);
        for (int i = 0; i + 1 < p.length; i++) net.connect(p[i], 0.5, p[i + 1], 0.5, pipeK);
        net.connect(p[p.length - 1], 0.5, boiler, 0, pipeK).kind(FluidEdge.Kind.CHECK);
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (int t = 0; t < 600; t++) {
            vessel.mWater = Math.max(vessel.mWater, 25);   // (the condensate keeps coming)
            vessel.update();
            double back = p[0].P() - vessel.P();
            pump.open = curve.lineShare(back);
            pump.boost = curve.boost(back);
            net.step(0.05);
            if (t >= 400) {
                lo = Math.min(lo, p[0].P());
                hi = Math.max(hi, p[0].P());
            }
        }
        return new double[]{lo, hi};
    }
}
