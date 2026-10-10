package mods.eln.sim.mna;

import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.VoltageState;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * RootSystem partitions the circuit into independent SubSystems (connected components, each with its own
 * dense matrix), rebuilds them lazily on the next step() after any add/remove, collapses chains of cable
 * loads into a single Line resistor, and splits circuits larger than 100 states at InterSystem resistors
 * (ElectricalConnection), coupling the parts through Thevenin-equivalent voltage sources. The physics must not
 * depend on any of that bookkeeping.
 */
public class TopologyTest {
    static final double DT = 0.05;
    static final double EPS = 1e-9;

    RootSystem root = new RootSystem(DT, 1);

    VoltageState node() {
        VoltageState s = new VoltageState();
        root.addState(s);
        return s;
    }

    @Test
    public void mergeAndSplitSubsystems() {
        // circuit 1: 10 V -- 10R -- x -- 10R -- gnd ; circuit 2: 20 V -- 10R -- y -- 30R -- gnd
        VoltageState a = node(), x = node(), b = node(), y = node();
        root.addComponent(new VoltageSource("v1", a, null).setU(10));
        root.addComponent(new Resistor(a, x).setR(10));
        root.addComponent(new Resistor(x, null).setR(10));
        root.addComponent(new VoltageSource("v2", b, null).setU(20));
        root.addComponent(new Resistor(b, y).setR(10));
        root.addComponent(new Resistor(y, null).setR(30));
        root.step();
        assertEquals(2, root.getSubSystemCount());
        assertEquals(5, x.state, EPS);
        assertEquals(15, y.state, EPS);
        assertNotSame(x.getSubSystem(), y.getSubSystem());

        // bridge x and y with 20R: nodal analysis
        // x: (10-x)/10 = x/10 + (x-y)/20 ; y: (20-y)/10 = y/30 + (y-x)/20
        Resistor bridge = new Resistor(x, y).setR(20);
        root.addComponent(bridge);
        root.step();
        assertEquals(1, root.getSubSystemCount());
        assertSame(x.getSubSystem(), y.getSubSystem());
        double[] xy = solve2(1 / 10.0 + 1 / 10.0 + 1 / 20.0, -1 / 20.0, -1 / 20.0, 1 / 10.0 + 1 / 30.0 + 1 / 20.0, 1, 2);
        assertEquals(xy[0], x.state, EPS);
        assertEquals(xy[1], y.state, EPS);

        // remove it again: back to two independent circuits with the original answers
        root.removeComponent(bridge);
        bridge.breakConnection(); // what a device's disconnect does after removing its components
        root.step();
        assertEquals(2, root.getSubSystemCount());
        assertEquals(5, x.state, EPS);
        assertEquals(15, y.state, EPS);
    }

    @Test
    public void changingResistanceMidRunRebuildsTheMatrix() {
        VoltageState a = node(), b = node();
        root.addComponent(new VoltageSource("v", a, null).setU(9));
        Resistor top = new Resistor(a, b).setR(1);
        root.addComponent(top);
        root.addComponent(new Resistor(b, null).setR(2));
        root.step();
        assertEquals(6, b.state, EPS);
        top.setR(7);
        root.step();
        assertEquals(2, b.state, EPS);
        assertEquals(1, root.getSubSystemCount()); // re-stamp, not re-partition
    }

    /** A device removed and placed again (all its loads and components) gives the same answer as before. */
    @Test
    public void removeAndReAddWholeDevice() {
        ElectricalLoad src = new ElectricalLoad(), dev = new ElectricalLoad();
        src.setRs(0.5);
        dev.setRs(0.5);
        root.addState(src);
        root.addComponent(new VoltageSource("v", src, null).setU(12));
        root.addState(dev);
        ElectricalConnection con = new ElectricalConnection(src, dev);
        root.addComponent(con);
        Resistor heater = new Resistor(dev, null).setR(5);
        root.addComponent(heater);
        root.step();
        assertEquals(2, heater.getCurrent(), EPS);

        root.removeComponent(con);
        root.removeComponent(heater);
        root.removeState(dev);
        root.step();
        assertEquals(0, src.getI(), EPS);

        root.addState(dev);
        root.addComponent(heater);
        root.addComponent(con);
        root.step();
        assertEquals(2, heater.getCurrent(), EPS);
    }

    /** A cable run (line-simplifiable loads with exactly two resistive neighbours) becomes one Line component. */
    @Test
    public void cableChainCollapsesIntoLineWithCorrectInnerVoltages() {
        int n = 10;
        double rs = 0.5; // each ElectricalConnection = rs + rs = 1 ohm
        ElectricalLoad src = new ElectricalLoad();
        src.setRs(rs);
        root.addState(src);
        root.addComponent(new VoltageSource("v", src, null).setU(100));
        List<ElectricalLoad> cable = new ArrayList<ElectricalLoad>();
        ElectricalLoad prev = src;
        for (int i = 0; i < n; i++) {
            ElectricalLoad c = new ElectricalLoad();
            c.setRs(rs);
            c.setCanBeSimplifiedByLine(true);
            root.addState(c);
            root.addComponent(new ElectricalConnection(prev, c));
            cable.add(c);
            prev = c;
        }
        ElectricalLoad sink = new ElectricalLoad();
        sink.setRs(rs);
        root.addState(sink);
        root.addComponent(new ElectricalConnection(prev, sink));
        Resistor load = new Resistor(sink, null).setR(89);
        root.addComponent(load);
        root.step();

        // n+1 connections of 1 ohm + 89 ohm load: 1 A
        assertEquals(1.0, load.getCurrent(), EPS);
        for (ElectricalLoad c : cable) assertTrue("cable load should be abstracted into a Line", c.isAbstracted());
        for (int i = 0; i < n; i++) assertEquals("cable node " + i, 100 - (i + 1), cable.get(i).getU(), EPS);

        // tap the middle of the cable: the line must break up and the physics follow
        ElectricalLoad mid = cable.get(4);
        Resistor tap = new Resistor(mid, null).setR(10);
        root.addComponent(tap);
        root.step();
        assertFalse(mid.isAbstracted());
        // source side: 5 ohm to mid, then mid || (6 ohm + 89 ohm) to ground
        double rDown = 6 + 89;
        double rMid = 1 / (1 / 10.0 + 1 / rDown);
        double vMid = 100 * rMid / (5 + rMid);
        assertEquals(vMid, mid.getU(), 1e-9);
        assertEquals(vMid * 89 / rDown, sink.getU(), 1e-9);
        assertEquals(vMid / 10, tap.getCurrent(), 1e-9);
    }

    /**
     * More than 100 states: RootSystem stops growing a SubSystem at an InterSystem (ElectricalConnection)
     * and couples the pieces with DelayInterSystem2 Thevenin sources (re-solved interSystemOverSampling times
     * per step). In DC this must still converge to the plain series-circuit answer.
     */
    @Test
    public void largeChainIsSplitButStillObeysOhm() {
        root = new RootSystem(DT, 50); // Eln default oversampling
        int n = 250;
        ElectricalLoad[] loads = new ElectricalLoad[n];
        for (int i = 0; i < n; i++) {
            loads[i] = new ElectricalLoad();
            loads[i].setRs(0.5);
            root.addState(loads[i]);
            if (i > 0) root.addComponent(new ElectricalConnection(loads[i - 1], loads[i]));
        }
        root.addComponent(new VoltageSource("v", loads[0], null).setU(500));
        Resistor load = new Resistor(loads[n - 1], null).setR(251);
        root.addComponent(load);
        for (int i = 0; i < 200; i++) root.step();

        assertTrue("expected the chain to be split, got " + root.getSubSystemCount(), root.getSubSystemCount() > 1);
        double i = 500.0 / (n - 1 + 251); // 1 A
        assertEquals(i, load.getCurrent(), 1e-3 * i);
        for (int k = 0; k < n; k += 25) assertEquals("node " + k, 500 - k * i, loads[k].getU(), 0.5);
    }

    static double[] solve2(double a, double b, double c, double d, double e, double f) {
        double det = a * d - b * c;
        return new double[]{(e * d - b * f) / det, (a * f - e * c) / det};
    }
}
