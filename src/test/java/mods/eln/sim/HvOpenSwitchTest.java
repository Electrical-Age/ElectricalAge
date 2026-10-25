package mods.eln.sim;

import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sim.mna.component.VoltageSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An open breaker (ResistorSwitch at ultraImpedance) on the 51.2 kV grid. AGE dropped ultraImpedance from its HV
 * breakers as "unstable above 5 kV", and so it was: at 1e16 ohm a floating line behind an open breaker solved to
 * -3e20 V with 32 kA through the breaker. MnaConst.ultraImpedance is 1e12 now; every voltage is finite and no current
 * flows, with or without a load behind the breaker.
 */
public class HvOpenSwitchTest {
    final SimHarness h = new SimHarness();

    ElectricalLoad load() {
        ElectricalLoad l = new ElectricalLoad();
        l.setRs(0.01);
        h.sim.addElectricalLoad(l);
        return l;
    }

    void check(boolean withLoad) {
        ElectricalLoad src = load(), a = load(), b = load(), c = load();
        h.sim.addElectricalComponent(new VoltageSource("v", src, null).setU(51200));
        h.sim.addElectricalComponent(new Resistor(src, a).setR(0.5));          // line in
        ResistorSwitch breaker = new ResistorSwitch("breaker", a, b);
        breaker.mustUseUltraImpedance();   // (as ElectricalBreakerElement)
        breaker.setR(0.01);
        breaker.setState(false);
        h.sim.addElectricalComponent(breaker);
        h.sim.addElectricalComponent(new Resistor(b, c).setR(0.5));            // line out
        if (withLoad) h.sim.addElectricalComponent(new Resistor(c, null).setR(1000));
        h.ticks(20);
        String s = String.format("a %.6g V, b %.6g V, c %.6g V, breaker %.3g A", a.getU(), b.getU(), c.getU(), breaker.getCurrent());
        assertTrue(Double.isFinite(a.getU()) && Double.isFinite(b.getU()) && Double.isFinite(c.getU()), s);
        assertTrue(Math.abs(a.getU() - 51200) < 1, s);
        // behind the open breaker: ~0 V with a load; floating (no path to ground) it sits at the line's potential, which
        // is the right answer. At 1e16 ohm it read -3e20 V with 32 kA through the "open" breaker (badly conditioned).
        if (withLoad) assertTrue(Math.abs(c.getU()) < 1, s);
        else assertTrue(Math.abs(c.getU() - 51200) < 100, s);
        assertTrue(Math.abs(breaker.getCurrent()) < 1e-6, s);
    }

    @Test
    public void openBreakerWithLoad() {
        check(true);
    }

    @Test
    public void openBreakerFloating() {
        check(false);
    }
}
