package mods.eln.sim;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.PowerSourceBipole;
import mods.eln.sim.mna.process.PvSourceBipole;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The diode panel curve (board #79, Baughn's option B+) and its pre-step solve. */
public class PvCurveTest {
    static final double DT = 0.05;

    @Test
    void ratingIsTheOpenCircuitVoltageAt10CAndThePeakPowerOnAHotNoon() {
        for (double[] r : new double[][]{{114, 520}, {14.25, 65}}) {
            PvCurve c = new PvCurve(r[0], r[1]);
            assertEquals(r[0], c.at(1, PvCurve.VOC_RATED_C).voc(), 1e-6 * r[0], "Voc at 10 C (Baughn: B, at 10 C)");
            double[] mpp = c.at(1, PvCurve.REF_C).maxPowerPoint();
            assertEquals(r[1], mpp[1], 1e-4 * r[1], "Pmp on a hot noon");
            assertEquals(0.77, mpp[0] / r[0], 0.01, "Vmp / rating");
        }
    }

    @Test
    void colderPanelsGiveMoreVoltageHotterLess() {
        PvCurve c = new PvCurve(114, 520);
        double vocRef = c.at(1, PvCurve.REF_C).voc();
        double cold = c.at(1, PvCurve.REF_C - 30).voc(), hot = c.at(1, PvCurve.REF_C + 30).voc();
        assertEquals(vocRef * (1 + 30 * 0.0035), cold, 0.01 * vocRef);
        assertEquals(vocRef * (1 - 30 * 0.0035), hot, 0.01 * vocRef);
        assertTrue(c.at(1, PvCurve.REF_C - 30).maxPowerPoint()[1] > 520, "a cold panel makes more power");
        assertTrue(c.at(1, PvCurve.REF_C + 30).maxPowerPoint()[1] < 520, "a hot one less");
    }

    @Test
    void weakLightLowersCurrentInProportionAndVoltageALittle() {
        PvCurve c = new PvCurve(114, 520);
        double isc = c.at(1, PvCurve.REF_C).iph(), voc = c.voc();
        c.at(0.25, PvCurve.REF_C);
        assertEquals(isc / 4, c.iph(), 1e-9);
        assertTrue(c.voc() < voc && c.voc() > 0.85 * voc, "Voc at quarter light: " + c.voc() + " of " + voc);
        assertEquals(0, c.at(0, PvCurve.REF_C).voc());
    }

    /** One root on any load line; current never above the photocurrent at u >= 0; matches bisection. */
    @Property(tries = 500)
    void solveFindsTheOneOperatingPoint(@ForAll @DoubleRange(min = 0.01, max = 1.1) double light,
                                        @ForAll @DoubleRange(min = -20, max = 110) double panelC,
                                        @ForAll @DoubleRange(min = -300, max = 200) double uth,
                                        @ForAll @DoubleRange(min = 1e-4, max = 1e6) @net.jqwik.api.constraints.Scale(5) double rth,
                                        @ForAll @DoubleRange(min = -500, max = 500) double guess) {
        PvCurve c = new PvCurve(114, 520).at(light, panelC);
        if (uth >= c.voc()) return;
        double u = c.solve(uth, rth, guess);
        assertTrue(Double.isFinite(u), "finite");
        assertTrue(u <= c.voc() + 1e-9, "below Voc");
        double residual = (u - uth) / rth - c.current(u);
        double scale = Math.max(1, c.iph()) + Math.abs(uth) / rth;
        assertTrue(Math.abs(residual) <= 1e-6 * scale, "residual " + residual + " at u " + u);
        if (u >= 0) assertTrue(c.current(u) <= c.iph() + 1e-12);
        // bisection reference
        double lo = Math.min(uth, 0) - 1e4, hi = c.voc();
        for (int k = 0; k < 200; k++) {
            double m = 0.5 * (lo + hi);
            if ((m - uth) / rth - c.current(m) > 0) hi = m; else lo = m;
        }
        assertEquals(0.5 * (lo + hi), u, 1e-6 * Math.max(1, Math.abs(u)));
    }

    // ---------------------------------------------------------------- in the MNA solver

    static final class Panel {
        final ElectricalLoad pos = new ElectricalLoad(), neg = new ElectricalLoad();
        final VoltageSource ps = new VoltageSource("pos", pos, null), ns = new VoltageSource("neg", neg, null);
        final PvSourceBipole src;

        Panel(RootSystem root, PvCurve curve, double rs) {
            pos.setRs(rs);
            neg.setRs(rs);
            root.addState(pos);
            root.addState(neg);
            root.addComponent(ps);
            root.addComponent(ns);
            src = new PvSourceBipole(pos, neg, ps, ns, curve);
            root.addProcess(src);
        }

        double delivered() {
            return pos.state * ps.getI() + neg.state * ns.getI();
        }
    }

    /** The operating point of n identical panels in parallel on a resistor, by bisection. */
    static double parallelReference(PvCurve c, int n, double r) {
        double lo = 0, hi = c.voc();
        for (int k = 0; k < 200; k++) {
            double m = 0.5 * (lo + hi);
            if (m / r - n * c.current(m) > 0) hi = m; else lo = m;
        }
        return 0.5 * (lo + hi);
    }

    @Test
    void onePanelOnAResistorSitsWhereTheCurveMeetsTheLoadLine() {
        for (double r : new double[]{1, 10, 22, 50, 1000}) {
            RootSystem root = new RootSystem(DT, 50);
            PvCurve curve = new PvCurve(114, 520).at(1, PvCurve.REF_C);
            Panel p = new Panel(root, curve, 1e-3);
            root.addComponent(new Resistor(p.pos, p.neg).setR(r));
            for (int s = 0; s < 20; s++) root.step();
            double u = parallelReference(curve, 1, r + 2e-3);
            assertEquals(u, p.pos.state - p.neg.state, 3e-3 * u + 1e-6, "R " + r);
            assertEquals(u * u / (r + 2e-3), p.delivered(), 1e-2 * u * u / r + 1e-6, "power on R " + r);
        }
    }

    /** Many panels in parallel on one bus: each sees the others as stiff sources, so check it still settles. */
    @Test
    void manyPanelsInParallelSettle() {
        int n = 20;
        double r = 2.0;   // near the combined max power point: 20 x 520 W at ~88 V ~ 0.74 ohm; 2 ohm is above it
        RootSystem root = new RootSystem(DT, 50);
        ElectricalLoad x = new ElectricalLoad(), y = new ElectricalLoad();
        x.setRs(1e-3);
        y.setRs(1e-3);
        root.addState(x);
        root.addState(y);
        PvCurve curve = new PvCurve(114, 520).at(1, PvCurve.REF_C);
        List<Panel> panels = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            Panel p = new Panel(root, curve, 0.05);   // a few blocks of cable each
            root.addComponent(new ElectricalConnection(p.pos, x));
            root.addComponent(new ElectricalConnection(p.neg, y));
            panels.add(p);
        }
        Resistor load = new Resistor(x, y).setR(r);
        root.addComponent(load);
        double ref = parallelReference(curve, n, r);
        int settled = -1;
        for (int s = 0; s < 200; s++) {
            root.step();
            if (settled < 0 && Math.abs((x.state - y.state) - ref) < 0.01 * ref) settled = s + 1;
        }
        assertTrue(settled > 0 && settled <= 20, "20 panels on a bus settle within 1 s (20 steps); took " + settled
            + " steps, bus " + (x.state - y.state) + " V vs " + ref);
        double total = 0;
        for (Panel p : panels) total += p.delivered();
        assertTrue(total <= n * curve.maxPowerPoint()[1] * 1.001, "no more than the panels' peak: " + total);
    }

    /** A series string with one panel in shade: the string current passes its bypass, the rest still deliver. */
    @Test
    void shadedPanelInAStringBypasses() {
        RootSystem root = new RootSystem(DT, 50);
        PvCurve sun = new PvCurve(114, 520).at(1, PvCurve.REF_C);
        PvCurve shade = new PvCurve(114, 520).at(0.1, PvCurve.REF_C);
        Panel a = new Panel(root, sun, 0.01), b = new Panel(root, shade, 0.01), c = new Panel(root, sun, 0.01);
        root.addComponent(new ElectricalConnection(a.neg, b.pos));
        root.addComponent(new ElectricalConnection(b.neg, c.pos));
        root.addComponent(new Resistor(a.pos, c.neg).setR(30));   // two sunny panels near their peak (2 x 88 V at 5.9 A)
        for (int s = 0; s < 100; s++) root.step();
        double ub = b.pos.state - b.neg.state, ua = a.pos.state - a.neg.state;
        double total = a.delivered() + b.delivered() + c.delivered();
        assertTrue(Double.isFinite(total), "finite");
        assertTrue(ub < 0.5 && ub > -2, "the shaded panel sits near 0 V on its bypass: " + ub);
        assertTrue(ua > 70, "the sunny ones still make voltage: " + ua);
        assertTrue(total > 0.9 * 2 * 520, "two sunny panels' worth reaches the load: " + total);
    }

    /** Onto a stiff battery-like source below Vmp it works as a current source (a PWM controller's share). */
    @Test
    void onAStiffBatteryItDeliversItsCurrentAtTheBatteryVoltage() {
        RootSystem root = new RootSystem(DT, 50);
        PvCurve curve = new PvCurve(114, 520).at(1, PvCurve.REF_C);
        Panel p = new Panel(root, curve, 1e-3);
        ElectricalLoad bat = new ElectricalLoad();
        bat.setRs(1e-3);
        root.addState(bat);
        VoltageSource battery = new VoltageSource("bat", bat, null).setU(75);
        root.addComponent(battery);
        root.addComponent(new Resistor(p.pos, bat).setR(0.05));
        root.addComponent(new Resistor(p.neg, null).setR(1e-3));
        for (int s = 0; s < 20; s++) root.step();
        double i = -battery.getI();
        double expected = curve.current(75 + 0.05 * i);
        assertEquals(expected, i, 0.01 * expected, "charging current");
        assertTrue(75 * i > 0.7 * 520 && 75 * i < 520, "70-100 % of the peak at 75 V: " + 75 * i);
    }

    /** Baughn: it has to be cheap. 100 panels on a bus, new curve vs the old constant-power source. */
    @Test
    void costComparedToTheConstantPowerPanel() {
        long tNew = time(true), tOld = time(false);
        System.out.printf("PV cost: 100 panels, 100 steps: diode %.1f ms, constant power %.1f ms (%.2fx)%n",
            tNew / 1e6, tOld / 1e6, (double) tNew / tOld);
        assertTrue(tNew < 3 * tOld + 50_000_000L, "diode panels within 3x of the old ones: " + tNew + " vs " + tOld);
    }

    private static long time(boolean diode) {
        long best = Long.MAX_VALUE;
        for (int rep = 0; rep < 3; rep++) {
            RootSystem root = new RootSystem(DT, 50);
            ElectricalLoad x = new ElectricalLoad(), y = new ElectricalLoad();
            root.addState(x);
            root.addState(y);
            for (int k = 0; k < 100; k++) {
                ElectricalLoad pos = new ElectricalLoad(), neg = new ElectricalLoad();
                pos.setRs(0.05);
                neg.setRs(0.05);
                root.addState(pos);
                root.addState(neg);
                VoltageSource ps = new VoltageSource("pos", pos, null), ns = new VoltageSource("neg", neg, null);
                root.addComponent(ps);
                root.addComponent(ns);
                root.addComponent(new ElectricalConnection(pos, x));
                root.addComponent(new ElectricalConnection(neg, y));
                if (diode) {
                    root.addProcess(new PvSourceBipole(pos, neg, ps, ns, new PvCurve(114, 520).at(0.9, 40)));
                } else {
                    PowerSourceBipole p = new PowerSourceBipole(pos, neg, ps, ns);
                    p.setUmax(114);
                    p.setImax(520 / 114.0 * 1.5);
                    p.setP(470);
                    root.addProcess(p);
                }
            }
            root.addComponent(new Resistor(x, y).setR(0.3));
            for (int s = 0; s < 10; s++) root.step();   // warm up
            long t0 = System.nanoTime();
            for (int s = 0; s < 100; s++) root.step();
            best = Math.min(best, System.nanoTime() - t0);
        }
        return best;
    }
}
