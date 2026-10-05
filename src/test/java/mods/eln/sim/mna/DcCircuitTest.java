package mods.eln.sim.mna;

import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.TestCurrentSource;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.VoltageState;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Resistive (DC) circuits solved by the MNA RootSystem/SubSystem, checked against Ohm's and Kirchhoff's laws.
 * A purely resistive circuit has no history terms, so one step must give the exact answer (to QR precision).
 */
public class DcCircuitTest {
    static final double DT = 0.05; // Eln default electrical period (20 Hz)
    static final double EPS = 1e-9;

    RootSystem root = new RootSystem(DT, 1);

    VoltageState node() {
        VoltageState s = new VoltageState();
        root.addState(s);
        return s;
    }

    Resistor r(VoltageState a, VoltageState b, double ohms) {
        Resistor r = new Resistor(a, b).setR(ohms);
        root.addComponent(r);
        return r;
    }

    VoltageSource vs(VoltageState a, VoltageState b, double volts) {
        VoltageSource v = new VoltageSource("vs", a, b).setU(volts);
        root.addComponent(v);
        return v;
    }

    @Test
    public void voltageDivider() {
        double[][] cases = {{10, 10, 30}, {230, 1000, 1}, {5, 1e-3, 1e3}, {-12, 47, 47}};
        for (double[] c : cases) {
            root = new RootSystem(DT, 1);
            VoltageState a = node(), b = node();
            vs(a, null, c[0]);
            r(a, b, c[1]);
            r(b, null, c[2]);
            root.step();
            assertEquals(c[0], a.state, EPS);
            assertEquals(c[0] * c[2] / (c[1] + c[2]), b.state, Math.abs(c[0]) * EPS);
        }
    }

    @Test
    public void seriesResistorsAddUp() {
        VoltageState a = node(), b = node(), c = node();
        VoltageSource v = vs(a, null, 24);
        Resistor r1 = r(a, b, 2), r2 = r(b, c, 4), r3 = r(c, null, 6);
        root.step();
        double i = 24.0 / (2 + 4 + 6);
        assertEquals(i, r1.getCurrent(), EPS);
        assertEquals(i, r2.getCurrent(), EPS);
        assertEquals(i, r3.getCurrent(), EPS);
        assertEquals(i, v.getI(), EPS);
        assertEquals(24 - 2 * i, b.state, EPS);
        assertEquals(6 * i, c.state, EPS);
    }

    @Test
    public void parallelResistorsAddConductances() {
        VoltageState a = node();
        VoltageSource v = vs(a, null, 12);
        r(a, null, 3);
        r(a, null, 6);
        r(a, null, 12);
        root.step();
        double req = 1 / (1 / 3.0 + 1 / 6.0 + 1 / 12.0);
        assertEquals(12 / req, v.getI(), EPS);
        assertEquals(12 * 12 / req, v.getP(), 1e-7);
    }

    /** Source EMF E with internal resistance Ri: U = E*RL/(RL+Ri); load power peaks at RL = Ri with E^2/(4Ri). */
    @Test
    public void sourceWithInternalResistance() {
        double e = 12, ri = 0.5;
        double bestP = 0, bestRl = 0;
        for (double rl = 0.05; rl <= 5.0; rl += 0.05) {
            root = new RootSystem(DT, 1);
            VoltageState emf = node(), out = node();
            vs(emf, null, e);
            r(emf, out, ri);
            Resistor load = r(out, null, rl);
            root.step();
            assertEquals(e * rl / (rl + ri), out.state, EPS);
            if (load.getP() > bestP) {
                bestP = load.getP();
                bestRl = rl;
            }
        }
        assertEquals(ri, bestRl, 1e-9 + 0.05 / 2);
        assertEquals(e * e / (4 * ri), bestP, 1e-6);
    }

    @Test
    public void voltageSourceSignConventions() {
        VoltageState a = node(), b = node();
        VoltageSource v = vs(a, b, 10); // floating source: a is 10 V above b
        r(a, null, 10);
        r(b, null, 30);
        root.step();
        assertEquals(10, a.state - b.state, EPS);
        assertEquals(10.0 / 40, v.getI(), EPS); // current delivered out of the + terminal
        assertEquals(-7.5, b.state, EPS);
        assertEquals(10 * 10.0 / 40, v.getP(), EPS); // power delivered
    }

    @Test
    public void currentSource() {
        VoltageState a = node(), b = node();
        root.addComponent(new TestCurrentSource(a, null, 2));
        r(a, b, 5);
        r(b, null, 5);
        root.step();
        assertEquals(2 * 10, a.state, EPS);
        assertEquals(2 * 5, b.state, EPS);
    }

    /**
     * Superposition: two 1 A and 2 A sources into one node of 5 ohm must give 15 V.
     * Was an EA bug (fixed in WP16): SubSystem.addToI assigned (Idata[id] = v) instead of accumulating (+=), so the second
     * right-hand-side contribution to a node overwrites the first. Every ISubSystemProcessI writes through
     * addToI (Capacitor and Delay history terms on voltage nodes; VoltageSource/Inductor only on their own
     * private current state, which is why ordinary circuits don't notice). See also
     * TransientTest.parallelCapacitorsAddUp.
     */
    @Test
    public void twoCurrentSourcesIntoOneNodeSuperpose() {
        VoltageState a = node();
        root.addComponent(new TestCurrentSource(a, null, 1));
        root.addComponent(new TestCurrentSource(a, null, 2));
        r(a, null, 5);
        root.step();
        assertEquals(15, a.state, EPS);
    }

    /**
     * Unbalanced Wheatstone bridge with two sources: KCL holds at every node and the power delivered by the
     * sources equals the power dissipated in the resistors (Tellegen's theorem).
     */
    @Test
    public void bridgeNetworkConservesPowerAndCurrent() {
        VoltageState top = node(), l = node(), rr = node(), bot = node(), aux = node();
        VoltageSource v1 = vs(top, bot, 20);
        Resistor r1 = r(top, l, 10), r2 = r(top, rr, 22), r3 = r(l, bot, 33), r4 = r(rr, bot, 4.7),
            r5 = r(l, rr, 15), r6 = r(bot, null, 1);
        VoltageSource v2 = vs(aux, null, 5);
        Resistor r7 = r(aux, rr, 8);
        root.step();

        Resistor[] rs = {r1, r2, r3, r4, r5, r6, r7};
        double pr = 0;
        for (Resistor x : rs) pr += x.getP();
        double ps = v1.getP() + v2.getP();
        assertEquals(ps, pr, 1e-9 * Math.abs(ps));

        // KCL: sum of currents leaving each internal node is zero (Bipole current flows aPin -> bPin)
        assertEquals(0, r1.getCurrent() - r3.getCurrent() - r5.getCurrent(), EPS); // node l
        assertEquals(0, r2.getCurrent() + r5.getCurrent() + r7.getCurrent() - r4.getCurrent(), EPS); // node rr
        assertEquals(0, r3.getCurrent() + r4.getCurrent() - r6.getCurrent() - v1.getI(), EPS); // node bot
    }

    /** ElectricalConnection (what NodeBase creates between adjacent devices) has R = Rs(L1) + Rs(L2). */
    @Test
    public void electricalConnectionUsesSumOfLoadSeriesResistances() {
        ElectricalLoad src = new ElectricalLoad(), dst = new ElectricalLoad();
        src.setRs(0.25);
        dst.setRs(0.75);
        root.addState(src);
        root.addState(dst);
        root.addComponent(new VoltageSource("vs", src, null).setU(10));
        root.addComponent(new ElectricalConnection(src, dst));
        Resistor load = new Resistor(dst, null).setR(4);
        root.addComponent(load);
        root.step();
        assertEquals(2.0, load.getCurrent(), EPS);
        assertEquals(8.0, dst.getU(), EPS);
        assertEquals(2.0, dst.getI(), EPS); // ElectricalLoad.getI = sum|I|/2 over attached bipoles

        // changing Rs re-stamps the connection
        dst.setRs(1.75);
        root.step();
        assertEquals(10.0 / 6, load.getCurrent(), EPS);
    }

    /** PowerSource regulates its voltage to deliver P into the Thevenin equivalent it sees: P into R gives U = sqrt(PR). */
    @Test
    public void constantPowerSource() {
        root = new RootSystem(DT, 50);
        ElectricalLoad out = new ElectricalLoad();
        root.addState(out);
        PowerSource ps = new PowerSource("ps", out);
        ps.setP(100);
        ps.setUmax(1000);
        ps.setImax(1000);
        root.addComponent(ps);
        Resistor load = new Resistor(out, null).setR(4);
        root.addComponent(load);
        for (int i = 0; i < 3; i++) root.step();
        assertEquals(20, out.getU(), 1e-6);
        assertEquals(100, load.getP(), 1e-6);

        // voltage limit
        ps.setUmax(10);
        root.step();
        assertEquals(10, out.getU(), 1e-6);
        // current limit
        ps.setUmax(1000);
        ps.setImax(2);
        root.step();
        assertEquals(2, load.getCurrent(), 1e-6);
    }
}
