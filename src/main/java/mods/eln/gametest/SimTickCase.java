package mods.eln.gametest;

import mods.eln.Eln;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;

/**
 * P0 smoke test of the harness and of the simulator on the 26.x server tick: a 50 V source across 10 ohm, built
 * straight in the simulator (no blocks yet), gives 5 A after the run.
 */
final class SimTickCase implements TestCase {
    ElectricalLoad load;
    VoltageSource source;
    Resistor resistor;

    public String id() {
        return "sim_ticks";
    }

    public int width() {
        return 3;
    }

    public void build(TestContext ctx) {
        load = new ElectricalLoad();
        load.setRs(1e-3);
        source = new VoltageSource("p0", load, null).setU(50);
        resistor = new Resistor(load, null).setR(10);
        Eln.simulator.addElectricalLoad(load);
        Eln.simulator.addElectricalComponent(source);
        Eln.simulator.addElectricalComponent(resistor);
    }

    public void measure(TestContext ctx) {
        try {
            ctx.checkValue("sim: 50 V across 10 ohm on the server tick [A]", resistor.getCurrent(), 5.0);
        } finally {
            Eln.simulator.removeElectricalComponent(resistor);
            Eln.simulator.removeElectricalComponent(source);
            Eln.simulator.removeElectricalLoad(load);
        }
    }
}
