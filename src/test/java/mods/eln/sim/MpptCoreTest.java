package mods.eln.sim;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.BuckConverterProcess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The MPPT controller's core (board #79), before any device: a diode panel, a few blocks of cable, the buck converter
 * process and a battery, in both pre-step orders; then the two trackers on it.
 */
public class MpptCoreTest {
    static final double DT = 0.05, ETA = 0.97;

    static final class Rig {
        final RootSystem root = new RootSystem(DT, 50);
        final PvCurve curve = new PvCurve(114, 520).at(1, PvCurve.REF_C);
        final PvCurveTest.Panel panelRef;
        final java.util.List<PvCurveTest.Panel> panels = new java.util.ArrayList<>();
        final ElectricalLoad inP = load(), inN = load(), outP = load(), outN = load(), bat = load();
        final VoltageSource inPs = src("inP", inP), inNs = src("inN", inN), outPs = src("outP", outP), outNs = src("outN", outN);
        final VoltageSource battery;
        final Resistor outLoad;
        final BuckConverterProcess conv;

        /** batteryV > 0: a stiff battery behind 0.02 ohm; otherwise a resistor of -batteryV ohm on the output. */
        Rig(double batteryV, boolean converterFirst) {
            this(batteryV, converterFirst, false);
        }

        /** commonNegative: the panel's - and the battery's - joined (as players wire it), not two separate returns. */
        Rig(double batteryV, boolean converterFirst, boolean commonNegative) {
            this(batteryV, converterFirst, commonNegative, 1);
        }

        /** count panels in parallel, each on its own few blocks of cable to the controller (one curve for all). */
        Rig(double batteryV, boolean converterFirst, boolean commonNegative, int count) {
            conv = new BuckConverterProcess(inP, inN, inPs, inNs, outP, outN, outPs, outNs);
            conv.setEfficiency(ETA);
            if (converterFirst) root.addProcess(conv);   // the pre-step order follows insertion
            for (int k = 0; k < count; k++) {
                PvCurveTest.Panel p = new PvCurveTest.Panel(root, curve, 1e-3);
                panels.add(p);
                root.addComponent(new Resistor(p.pos, inP).setR(0.05));   // a few blocks of cable each way
                root.addComponent(new Resistor(p.neg, inN).setR(0.05));
            }
            if (!converterFirst) root.addProcess(conv);
            this.panelRef = panels.getFirst();
            root.addComponent(new Resistor(outN, null).setR(1e-3));
            if (commonNegative) root.addComponent(new Resistor(inN, outN).setR(0.05));
            if (batteryV > 0) {
                battery = src("bat", bat).setU(batteryV);
                root.addComponent(new Resistor(outP, bat).setR(0.02));
                outLoad = null;
            } else {
                battery = null;
                outLoad = new Resistor(outP, outN).setR(-batteryV);
                root.addComponent(outLoad);
            }
        }

        ElectricalLoad load() {
            ElectricalLoad l = new ElectricalLoad();
            l.setRs(1e-3);
            root.addState(l);
            return l;
        }

        VoltageSource src(String name, ElectricalLoad l) {
            VoltageSource s = new VoltageSource(name, l, null);
            root.addComponent(s);
            return s;
        }

        /** All the panels' power. */
        double panelP() {
            double p = 0;
            for (PvCurveTest.Panel q : panels) p += q.delivered();
            return p;
        }

        double panelU() {
            return panelRef.pos.state - panelRef.neg.state;
        }
    }

    @Test
    void fixedRatioBalancesPowerAndSettlesInEitherOrder() {
        for (boolean first : new boolean[]{false, true}) {
            Rig r = new Rig(60, first);
            r.conv.setRatio(60.0 / 85);
            double p3 = 0;
            int start = -1;
            for (int s = 0; s < 40; s++) {
                r.root.step();
                if (start < 0 && r.conv.isFlowing()) start = s;
                if (start >= 0 && s == start + 3) p3 = r.conv.getOutputP();
            }
            assertTrue(start >= 0 && start <= 6, "starts within 6 steps of an open input: " + start);
            String ctx = (first ? "converter first" : "panel first") + ": in " + r.conv.getInputU() + " V " + r.conv.getInputI()
                + " A, out " + r.conv.getOutputU() + " V " + r.conv.getOutputI() + " A, panel " + r.panelU() + " V " + r.panelP() + " W";
            assertTrue(r.conv.isFlowing(), "flows: " + ctx);
            assertEquals(ETA * r.conv.getInputP(), r.conv.getOutputP(), 0.002 * r.conv.getOutputP(), "out = eta x in: " + ctx);
            double cable = Math.pow(r.conv.getInputI(), 2) * 0.1;
            assertEquals(r.panelP(), r.conv.getInputP() + cable, 0.01 * r.panelP(), "panel = converter in + cable: " + ctx);
            double battIn = -r.battery.getI() * 60;
            assertEquals(r.conv.getOutputP(), battIn, 0.01 * battIn + 1, "the battery gets the output: " + ctx);
            assertEquals(r.curve.current(r.panelU()), r.conv.getInputI(), 0.01 * r.conv.getInputI(), "panel on its curve: " + ctx);
            assertEquals(r.conv.getOutputP(), p3, 0.01 * p3, "settled 3 steps after it starts: " + ctx);
        }
    }

    @Test
    void outputLimitHoldsTheVoltage() {
        Rig r = new Rig(-40, false);   // 40 ohm, no battery: at ratio 1 the output would sit near the panel's ~120 V
        r.conv.setRatio(1);
        r.conv.setOutputLimit(80);
        for (int s = 0; s < 40; s++) r.root.step();
        double v = r.outP.state - r.outN.state;
        assertTrue(r.conv.isLimited(), "the limit applies");
        assertTrue(v <= 80.5 && v > 75, "output at its limit: " + v);
        assertEquals(ETA * r.conv.getInputP(), r.conv.getOutputP(), 0.005 * r.conv.getOutputP(), "power still balances");
    }

    @Test
    void anOpenOutputIsFloating() {
        Rig r = new Rig(60, false);
        r.root.removeComponent(r.battery);
        r.battery.breakConnection();
        r.conv.setRatio(0.6);
        for (int s = 0; s < 10; s++) r.root.step();
        assertTrue(r.conv.isOutputFloating() && !r.conv.isFlowing(), "no battery: floating " + r.conv.isOutputFloating() + ", flowing " + r.conv.isFlowing() + ", out " + r.conv.getOutputU() + " V " + r.conv.getOutputI() + " A");
    }

    @Test
    void nothingFlowsBackAtNight() {
        Rig r = new Rig(60, false);
        r.curve.at(0, PvCurve.REF_C);
        r.conv.setRatio(0.9);
        for (int s = 0; s < 20; s++) r.root.step();
        assertTrue(!r.conv.isFlowing(), "off");
        assertTrue(Math.abs(r.battery.getI()) < 1e-6, "no current from the battery: " + r.battery.getI());
    }

    /** Runs the tracker for {@code seconds}, returning the panel's mean power over the last 5 s. */
    /** One electrical step, then the tracker (as MpptElement does). */
    static void step(Rig r, MpptTracker t) {
        r.root.step();
        var c = r.conv;
        c.setRatio(t.next(c.getInputU(), c.getInputI(), c.getOutputU(), c.getRatio(), c.getRatioUsed(),
            c.isLimited() || c.isPowerLimited(), c.isFlowing(), DT));
    }

    static double track(Rig r, MpptTracker t, double seconds) {
        int steps = (int) Math.round(seconds / DT), tail = (int) Math.round(5 / DT);
        double sum = 0;
        for (int s = 0; s < steps; s++) {
            step(r, t);
            if (s >= steps - tail) sum += r.panelP();
        }
        return sum / tail;
    }

    @Test
    void incrementalConductanceSitsOnThePeakAndFollowsTheLight() {
        Rig r = new Rig(60, false);
        r.conv.setRatio(0.5);   // far off: the input near 120 V is above Voc, so it starts from 80 % of the open voltage
        MpptTracker t = MpptTracker.incrementalConductance();
        double peak = r.curve.maxPowerPoint()[1];
        double p = track(r, t, 10);
        assertTrue(p > 0.99 * peak, "tier 2 within 1 % of the peak: " + p + " of " + peak);
        r.curve.at(0.4, PvCurve.REF_C - 10);   // a cloud, and the panel cools
        peak = r.curve.maxPowerPoint()[1];
        p = track(r, t, 6);   // the mean over its last 5 s: it has 1 s to get there
        assertTrue(p > 0.985 * peak, "follows the cloud within a second: " + p + " of " + peak);
    }

    @Test
    void perturbAndObserveGetsCloseButHunts() {
        Rig r = new Rig(60, false);
        r.conv.setRatio(0.5);
        MpptTracker t = MpptTracker.perturbAndObserve();
        double peak = r.curve.maxPowerPoint()[1];
        double p = track(r, t, 60);
        assertTrue(p > 0.95 * peak, "tier 1 within 5 %: " + p + " of " + peak);
        assertTrue(p < 0.999 * peak, "and not exactly on it: " + p + " of " + peak);
    }

    /** Both trackers, in every pre-step order and with a common negative, on a battery: on the peak (the advanced one
     *  locked at the buck's end stop at 89 % before it turned back there, found in review). */
    @Test
    void trackersFindThePeakInEveryWiringAndOrder() {
        for (boolean first : new boolean[]{false, true})
            for (boolean common : new boolean[]{false, true}) {
                Rig r = new Rig(60, first, common);
                r.conv.setRatio(0.5);
                double peak = r.curve.maxPowerPoint()[1];
                double p = track(r, MpptTracker.incrementalConductance(), 10);
                assertTrue(p > 0.99 * peak, "IncCond, converter first " + first + ", common - " + common + ": " + p + " of " + peak);
                Rig q = new Rig(60, first, common);
                q.conv.setRatio(0.5);
                double pq = track(q, MpptTracker.perturbAndObserve(), 60);
                assertTrue(pq > 0.95 * peak, "P&O, converter first " + first + ", common - " + common + ": " + pq + " of " + peak);
            }
    }

    @Test
    void incrementalConductanceTurnsBackAtTheEndStop() {
        MpptTracker t = MpptTracker.incrementalConductance();
        double ratio = 1;
        for (int k = 0; k < 40; k++) ratio = t.next(60.6, 5.9, 60, ratio, ratio, false, true, 0.05);
        assertTrue(ratio < 0.999, "left the end stop: " + ratio);
    }

    /** On a resistor (no battery holding the output) the advanced tracker stays near the peak (it hunted there). */
    @Test
    void incrementalConductanceOnAResistor() {
        Rig r = new Rig(-10, false);
        r.conv.setRatio(0.7);
        double peak = r.curve.maxPowerPoint()[1];
        double p = track(r, MpptTracker.incrementalConductance(), 15);
        assertTrue(p > 0.97 * peak, "IncCond on 10 ohm: " + p + " of " + peak);
    }

    /**
     * The rated power, held within the converter's solve: steady at the rating for arrays from a little to many times
     * bigger than it, while the light falls, and with the output limit at the same time (a tracker stepping the ratio
     * from outside was unstable for big arrays: review round 3's repro, 300 and 250 W on one 520 W panel).
     */
    @Test
    void powerLimitHoldsForAnyArraySize() {
        record Case(int panels, double limit, double bank) {
        }
        for (Case k : new Case[]{new Case(1, 400, 60), new Case(1, 300, 60), new Case(1, 250, 60), new Case(8, 2000, 60),
            new Case(16, 2000, 48)})
            for (boolean inc : new boolean[]{true, false}) {
                Rig r = new Rig(k.bank, false, false, k.panels);
                r.conv.setPowerLimit(k.limit);
                r.conv.setRatio(0.8);
                MpptTracker t = inc ? MpptTracker.incrementalConductance() : MpptTracker.perturbAndObserve();
                track(r, t, 10);
                double lo = Double.MAX_VALUE, hi = 0;
                for (int s = 0; s < 600; s++) {   // 30 s, the light falling to 0.7
                    r.curve.at(1 - 0.3 * s / 600.0, PvCurve.REF_C);
                    step(r, t);
                    if (k.panels * r.curve.maxPowerPoint()[1] < 1.05 * k.limit) continue;   // (the array can't give it)
                    lo = Math.min(lo, r.conv.getInputP());
                    hi = Math.max(hi, r.conv.getInputP());
                }
                String what = (inc ? "IncCond" : "P&O") + ", " + k.panels + " panel(s), " + k.limit + " W on a " + k.bank + " V bank";
                assertTrue(lo > 0.97 * k.limit && hi < 1.01 * k.limit, what + ": " + lo + " .. " + hi);
            }
    }

    @Test
    void powerAndOutputLimitTogether() {
        Rig r = new Rig(-20, false, false, 4);   // 20 ohm: at 60 V it takes 180 W
        r.conv.setPowerLimit(500);
        r.conv.setOutputLimit(60);
        r.conv.setRatio(0.8);
        MpptTracker t = MpptTracker.incrementalConductance();
        track(r, t, 10);
        double v = r.outP.state - r.outN.state;
        assertTrue(r.conv.isLimited() && v <= 60.5 && v > 57, "the output limit wins: " + v + " V, " + r.conv.getInputP() + " W in");
        r.outLoad.setR(2);   // a load comes on: 1.8 kW at 60 V, more than the 500 W rating
        track(r, t, 10);
        assertTrue(r.conv.isPowerLimited() && Math.abs(r.conv.getInputP() - 500) < 10, "then the rating: " + r.conv.getInputP() + " W");
    }

    /** Dusk: tracking at noon, then dark. Nothing may reach the battery from dark panels; log the input's voltage. */
    @Test
    void duskLeavesNoEnergy() {
        for (boolean first : new boolean[]{false, true}) {
            Rig r = new Rig(60, first);
            r.conv.setRatio(0.8);
            MpptTracker t = MpptTracker.incrementalConductance();
            track(r, t, 10);
            r.curve.at(0, PvCurve.REF_C);
            double energy = 0;
            for (int s = 0; s < 600; s++) {
                step(r, t);
                energy += -r.battery.getI() * 60 * DT;
            }
            System.out.printf("dusk (converter first %b): input %.2f V after 30 s dark, %.3f J into the battery, flowing %b%n",
                first, r.conv.getInputU(), energy, r.conv.isFlowing());
            assertTrue(Math.abs(energy) < 1, "no energy from dark panels: " + energy + " J");
            assertTrue(!r.conv.isOutputFloating(), "the battery is still there (so it reads \"No input\", not \"No battery\")");
        }
    }

    /** With the battery above the panels' best voltage the peak is out of reach; ratio 1 is best and it stays there. */
    @Test
    void atTheEndStopItKeepsTheBestReachablePower() {
        Rig fixed = new Rig(92, false);
        fixed.conv.setRatio(1);
        double best = 0;
        for (int s = 0; s < 100; s++) {
            fixed.root.step();
            best = fixed.panelP();
        }
        for (boolean inc : new boolean[]{true, false}) {
            Rig r = new Rig(92, false);
            r.conv.setRatio(0.9);
            double p = track(r, inc ? MpptTracker.incrementalConductance() : MpptTracker.perturbAndObserve(), 30);
            assertTrue(p > (inc ? 0.99 : 0.9) * best, (inc ? "IncCond" : "P&O") + " on a 92 V bank: " + p + " of " + best + " at ratio 1");
        }
    }

    @Test
    void costOfAController() {
        long best = Long.MAX_VALUE;
        for (int rep = 0; rep < 3; rep++) {
            Rig r = new Rig(60, false);
            r.conv.setRatio(0.85);
            for (int s = 0; s < 10; s++) r.root.step();
            long t0 = System.nanoTime();
            for (int s = 0; s < 200; s++) r.root.step();
            best = Math.min(best, System.nanoTime() - t0);
        }
        System.out.printf("MPPT rig (1 panel + converter + battery): %.3f ms per step%n", best / 200 / 1e6);
    }
}
