package mods.eln.oil;

import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNetwork;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A player's plant on Incognito (2026-11-14): two turbines' condensers on one condensate line to boilers at their full
 * mark, one turbine taking most of the steam. Without a level control each pump delivered about half the feed: the busy
 * turbine's hotwell filled with what its pump didn't return until it flooded the turbine, while the other's sat at its
 * makeup level passing makeup water through. With the condensate pump's level control (CondenserVessel) both hotwells
 * settle under LEVEL_FULL and the makeup replaces the losses alone.
 */
public class ParallelCondensateTest {
    static final double DT = 0.05, VOLUME = 0.000846, RATED_KG = 0.583, LOSS = 0.05e-3;
    static final double[] STEAM = {1.5e-3, 0.3e-3};

    /** Runs the plant for seconds; returns {peak fill of the busy hotwell, makeup taken and
     *  losses over the last third (kg), last fill of each}. */
    static double[] run(boolean levelControl, double seconds, double... steam) {
        FluidNetwork net = new FluidNetwork();
        CondensatePump curve = CondensatePump.forTurbine(10e5, 2e-3);
        FluidNode tank = net.add(new FluidNode(1, 142, 1, true).fill(0.9 * Water.RHO_L, 5e5, 291));
        FluidNode boiler = net.add(new FluidNode(1, 141, 1, true).fill(0.5 * Water.RHO_L, 11e5, 320));
        FluidNode line = net.add(new FluidNode(0.0003, 141.5, 1, false).fill(0.97 * 0.0003 * Water.RHO_L, 11e5, 293));
        FluidNode[] well = new FluidNode[2];
        FluidEdge[] pump = new FluidEdge[2], makeup = new FluidEdge[2];
        for (int i = 0; i < 2; i++) {
            well[i] = net.add(new FluidNode(VOLUME, 142, 1, false).fill(0.28 * VOLUME * Water.RHO_L, 0.04e5, 300));
            pump[i] = net.connect(well[i], 0, line, 0.5, 0).kind(FluidEdge.Kind.PUMP);
            pump[i].qMax = curve.freeKgPerS;
            makeup[i] = net.connect(tank, 0, well[i], 1, CondenserVessel.PORT_K).kind(FluidEdge.Kind.CHECK);
        }
        FluidEdge feed = net.connect(line, 0.5, boiler, 0.3, BoilerElement.PORT_K).kind(FluidEdge.Kind.CHECK);
        // (the boilers' water, kept beside the fixed node that stands for them: steam and losses out, the feed in)
        double level = RATED_KG, peak = 0, madeUp = 0, lost = 0;
        for (double t = 0; t < seconds; t += DT) {
            feed.open = BoilerElement.feedFloat(level / RATED_KG);
            for (int i = 0; i < 2; i++) {
                double fill = well[i].mLiquid() / Water.RHO_L / VOLUME;
                double share = levelControl ? CondenserVessel.levelShare(fill) : 1;
                pump[i].h0 = curve.shutoffPa * CondenserVessel.headLevel(share);
                pump[i].speed = CondenserVessel.flowLevel(share) * (levelControl ? CondenserVessel.suction(fill) : 1);
                makeup[i].open = CondenserVessel.floatOpening(fill);
                EaUnits.addWater(well[i], steam[i] * DT, 300, false);
                if (i == 0) peak = Math.max(peak, fill);
            }
            net.step(DT);
            level += (feed.flow - steam[0] - steam[1] - LOSS) * DT;
            if (t < seconds * 2 / 3) continue;
            lost += LOSS * DT;
            madeUp += (makeup[0].flow + makeup[1].flow) * DT;
        }
        return new double[]{peak, madeUp, lost, well[0].mLiquid() / Water.RHO_L / VOLUME, well[1].mLiquid() / Water.RHO_L / VOLUME};
    }

    @Test
    void withoutLevelControlTheBusyHotwellFloods() {
        double[] r = run(false, 600, STEAM);
        assertTrue(r[0] > CondenserDescriptor.FLOOD, String.format("peak %.0f%%, made up %.1f g", r[0] * 100, r[1] * 1000));
    }

    @Test
    void levelControlledPumpsShareTheFeed() {
        double[] r = run(true, 1500, STEAM);
        String what = String.format("peak %.1f%%, now %.1f%% and %.1f%%; made up %.1f g for %.1f g lost in the last third", r[0] * 100, r[3] * 100, r[4] * 100,
            r[1] * 1000, r[2] * 1000);
        System.out.println("parallel condensers: " + what);
        assertTrue(r[0] < CondenserVessel.LEVEL_FULL, what);
        assertTrue(r[1] < 1.2 * r[2] + 0.001, "makeup beyond the losses: " + what);
    }

    /** One turbine carrying the load, the other turning idle: the fuller hotwell still returns all its condensate. */
    @Test
    void anIdleTurbinesCondenserDoesNotTakeTheFeed() {
        for (double[] st : new double[][]{{1.9e-3, 0.02e-3}, {2.0e-3, 0}, {1.0e-3, 0.01e-3}, {0.3e-3, 0}, {2.4e-3, 0.1e-3}, {2.14e-3, 0.33e-3}}) {
            double[] r = run(true, 3000, st);
            String what = String.format("%.2f and %.2f g/s: peak %.1f%%, now %.1f%% and %.1f%%; made up %.1f g for %.1f g lost in the last third",
                st[0] * 1000, st[1] * 1000, r[0] * 100, r[3] * 100, r[4] * 100, r[1] * 1000, r[2] * 1000);
            System.out.println("parallel condensers: " + what);
            assertTrue(r[0] < CondenserVessel.LEVEL_FULL, what);
            assertTrue(r[1] < 1.2 * r[2] + 0.001, "makeup beyond the losses: " + what);
        }
    }
}
