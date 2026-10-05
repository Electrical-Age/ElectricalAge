package mods.eln.sim;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sim.mna.component.Transformer;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.process.TransformerInterSystemProcess;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Device-level models built the way the device classes build them (DiodeElement, TransformerElement). */
public class ElectricalDevicesTest {
    static final double EPS = 1e-9;

    SimHarness h = new SimHarness();

    ElectricalLoad load() {
        ElectricalLoad l = new ElectricalLoad();
        h.sim.addElectricalLoad(l);
        return l;
    }

    /**
     * EA diode (DiodeElement): a ResistorSwitch whose state DiodeProcess sets to (U > 0) after every electrical
     * step; on-resistance stdU/stdI (10 A diode: 1 V / 10 A = 0.1 ohm), off = 1e9 ohm. It is a linear
     * on/off switch: no forward threshold (a real Si diode drops ~0.6-0.7 V regardless of current).
     */
    ResistorSwitch diode(ElectricalLoad anode, ElectricalLoad cathode) {
        ResistorSwitch d = new ResistorSwitch("d", anode, cathode);
        d.setR(1.0 / 10);
        h.sim.addElectricalComponent(d);
        h.sim.addElectricalProcess(new DiodeProcess(d));
        return d;
    }

    @Test
    public void diodeForwardConducts() {
        ElectricalLoad a = load(), k = load();
        VoltageSource v = new VoltageSource("v", a, null).setU(10);
        h.sim.addElectricalComponent(v);
        ResistorSwitch d = diode(a, k);
        h.sim.addElectricalComponent(new Resistor(k, null).setR(10));
        h.ticks(2); // first step is solved with the switch still open; the process closes it for the next one
        assertTrue(d.getState());
        assertEquals(10 / 10.1, d.getCurrent(), EPS);
        assertEquals(10 * 10 / 10.1, k.getU(), EPS);
    }

    @Test
    public void diodeReverseBlocks() {
        ElectricalLoad a = load(), k = load();
        h.sim.addElectricalComponent(new VoltageSource("v", a, null).setU(-10));
        ResistorSwitch d = diode(a, k);
        h.sim.addElectricalComponent(new Resistor(k, null).setR(10));
        h.ticks(5);
        assertTrue(!d.getState());
        assertEquals(0, d.getCurrent(), 10 / MnaConst.highImpedance * 1.0001); // leakage through 1e9 ohm only
    }

    /**
     * A diode must never conduct backwards. EA decides the switch state from the PREVIOUS step's voltage, so
     * when the source polarity flips the diode stays closed for one whole electrical step (50 ms) and passes
     * the full reverse current -U/(R+Ron). Physically zero. Matters for anything that reverses polarity
     * (batteries swapped, H-bridges, generators with reversing direction): a 50 ms reverse pulse per flip,
     * which also heats the diode via DiodeHeatThermalLoad.
     */
    @Ignore("EA bug: DiodeProcess uses last step's voltage, so a diode conducts in reverse for one step after polarity flips")
    @Test
    public void diodeNeverConductsBackwards() {
        ElectricalLoad a = load(), k = load();
        VoltageSource v = new VoltageSource("v", a, null).setU(10);
        h.sim.addElectricalComponent(v);
        diode(a, k);
        Resistor rl = new Resistor(k, null).setR(10);
        h.sim.addElectricalComponent(rl);
        for (int t = 0; t < 20; t++) {
            v.setU(t % 4 < 2 ? 10 : -10);
            h.tick();
            // measure on the series load: the switch's own getCurrent() is evaluated with the R the
            // DiodeProcess has just set for the NEXT step, so it misreports (100 A / 1e-10 A) right after a flip
            assertTrue("tick " + t + ": reverse current " + rl.getCurrent(), rl.getCurrent() > -1e-6);
        }
    }

    /** Ideal transformer (Transformer component): Us = n Up, Ip = n Is, Pin = Pout, load reflected as R/n^2. */
    @Test
    public void transformerRatioAndPower() {
        for (double n : new double[]{2, 0.25, 10}) {
            h = new SimHarness();
            ElectricalLoad emf = load(), p = load(), s = load();
            VoltageSource v = new VoltageSource("v", emf, null).setU(100);
            h.sim.addElectricalComponent(v);
            h.sim.addElectricalComponent(new Resistor(emf, p).setR(1)); // source internal resistance
            Transformer t = new Transformer(p, s);
            t.setRatio(n);
            h.sim.addElectricalComponent(t);
            double rl = 8;
            Resistor load = new Resistor(s, null).setR(rl);
            h.sim.addElectricalComponent(load);
            h.tick();

            double reflected = rl / (n * n);
            double up = 100 * reflected / (1 + reflected);
            assertEquals("n=" + n, up, p.getU(), 1e-9 * up);
            assertEquals("n=" + n, n * up, s.getU(), 1e-9 * up * n);
            assertEquals("n=" + n, v.getI(), n * load.getCurrent(), 1e-9 * v.getI());
            assertEquals("n=" + n, v.getP(), load.getP() + v.getI() * v.getI() * 1, 1e-9 * v.getP());
            // transformer itself neither stores nor dissipates: Va*Ia + Vb*Ib = 0
            assertEquals(0, p.getU() * t.aCurrentState.state + s.getU() * t.bCurrentState.state, 1e-9 * v.getP());
            // ElectricalLoad.getI() (multimeter, cable heating, TransformerElement load) sees each winding's own
            // current (1.7.10: Transformer.getCurrent() = 0, so both read half the true current)
            assertEquals("n=" + n, Math.abs(v.getI()), p.getI(), 1e-9 * Math.abs(v.getI()));
            assertEquals("n=" + n, Math.abs(load.getCurrent()), s.getI(), 1e-9 * Math.abs(load.getCurrent()));
            assertEquals("n=" + n, v.getI(), t.getCurrent(), 1e-9 * Math.abs(v.getI()));
        }
    }

    /**
     * Isolating transformer (TransformerElement with isIsolator, GridTransformer, utility pole): primary and
     * secondary are separate SubSystems, each with a VoltageSource that TransformerInterSystemProcess sets from
     * both sides' Thevenin equivalents every pre-step. For linear circuits it must match the ideal transformer.
     */
    @Test
    public void isolatingTransformerMatchesIdealTransformer() {
        double n = 2, rl = 8;
        ElectricalLoad emf = load(), p = load(), s = load();
        VoltageSource v = new VoltageSource("v", emf, null).setU(100);
        h.sim.addElectricalComponent(v);
        h.sim.addElectricalComponent(new Resistor(emf, p).setR(1));
        VoltageSource pv = new VoltageSource("pv", p, null), sv = new VoltageSource("sv", s, null);
        h.sim.addElectricalComponent(pv);
        h.sim.addElectricalComponent(sv);
        TransformerInterSystemProcess tp = new TransformerInterSystemProcess(p, s, pv, sv);
        tp.setRatio(n);
        Resistor load = new Resistor(s, null).setR(rl);
        h.sim.addElectricalComponent(load);
        h.sim.mna.addProcess(tp);
        h.ticks(2);

        assertEquals(2, h.sim.mna.getSubSystemCount());
        double reflected = rl / (n * n);
        double up = 100 * reflected / (1 + reflected);
        assertEquals(up, p.getU(), 1e-6);
        assertEquals(n * up, s.getU(), 1e-6);
        assertEquals(v.getI() * up, load.getP(), 1e-6); // power through the transformer is conserved
    }

    /** Joule heating: a resistor's I^2 R goes into its thermal load; at steady state T = P * Rp. */
    @Test
    public void resistorHeatsThermalLoadToSteadyState() {
        ElectricalLoad a = load();
        h.sim.addElectricalComponent(new VoltageSource("v", a, null).setU(10));
        Resistor r = new Resistor(a, null).setR(10); // 10 W
        h.sim.addElectricalComponent(r);
        ThermalLoad tl = new ThermalLoad(0, 2, 1, 5); // Rp = 2 K/W, C = 5 J/K: tau = 10 s, Tss = 20 K
        tl.setAsSlow();
        h.sim.addThermalLoad(tl);
        h.sim.addThermalSlowProcess(new mods.eln.sim.process.heater.ResistorHeatThermalLoad(r, tl));
        h.seconds(100);
        assertEquals(20, tl.Tc, 1e-3);
        assertEquals(0, tl.Pc, 1e-3); // net heat flow (in - loss) vanishes at steady state
    }
}
