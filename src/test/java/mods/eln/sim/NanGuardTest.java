package mods.eln.sim;

import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.MnaConst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Non-finite values are kept out of the sim: one NaN resistance used to turn the whole network NaN. */
public class NanGuardTest {
    final SimHarness h = new SimHarness();

    ElectricalLoad load() {
        ElectricalLoad l = new ElectricalLoad();
        h.sim.addElectricalLoad(l);
        return l;
    }

    @Test
    public void nanResistanceIsAnOpenCircuit() {
        int before = NanGuard.count();
        ElectricalLoad a = load(), b = load();
        h.sim.addElectricalComponent(new VoltageSource("v", a, null).setU(10));
        h.sim.addElectricalComponent(new Resistor(a, b).setR(Double.NaN));
        h.sim.addElectricalComponent(new Resistor(b, null).setR(10));
        h.ticks(3);
        assertTrue(Double.isFinite(a.getU()) && Double.isFinite(b.getU()), a.getU() + " " + b.getU());
        assertEquals(10, a.getU(), 1e-6);
        assertEquals(0, b.getU(), 1e-6);   // (open: 10 V over 1e9 + 10 ohm)
        assertTrue(NanGuard.count() > before);
    }

    @Test
    public void infiniteResistanceIsOpenAndQuiet() {
        int before = NanGuard.count();
        new Resistor(null, null).setR(1 / 0.0);
        assertEquals(before, NanGuard.count());
    }

    @Test
    public void nanHeatIsDropped() {
        ThermalLoad t = new ThermalLoad(0, 1e30, 1, 50);
        t.movePowerTo(Double.NaN);
        t.moveEnergyTo(Double.POSITIVE_INFINITY);
        assertEquals(0, t.PcTemp, 0);
        assertEquals(0, t.EnergyTemp, 0);
        assertEquals(1e9, MnaConst.highImpedance, 0);
    }
}
