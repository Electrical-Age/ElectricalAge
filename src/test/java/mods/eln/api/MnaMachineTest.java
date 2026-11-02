package mods.eln.api;

import mods.eln.api.impl.MachineCore;
import mods.eln.api.impl.MnaMachine;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.Simulator;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.MnaConst;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MnaMachine in a bare simulator: steady machines don't change the circuit tick to tick, and the step rate is checked. */
public class MnaMachineTest {
    final Simulator sim = new Simulator(0.05, 0.05, 50, 1 / 400.0);
    final ElectricalLoad bus = new ElectricalLoad();

    MnaMachineTest() {
        sim.init();
        sim.addElectricalLoad(bus);
    }

    /** Ticks n times in the game's order with owner, returning the machine's resistor after each tick. */
    double[] run(MnaMachine m, Consumer<MachineCore> owner, int n) {
        double[] ohms = new double[n];
        for (int i = 0; i < n; i++) {
            sim.tick();
            m.readAfterStep();
            owner.accept(m.core());
            m.applyBeforeStep();
            ohms[i] = m.ohms();
        }
        return ohms;
    }

    /** A steady load on a weak line: once the line has settled, the resistor (and so the matrix) stays put. */
    @Test
    void steadyConsumerKeepsItsResistance() {
        ElectricalLoad emf = new ElectricalLoad();
        sim.addElectricalLoad(emf);
        sim.addElectricalComponent(new VoltageSource("v", emf, null).setU(200));
        sim.addElectricalComponent(new Resistor(emf, bus).setR(2));
        MnaMachine m = new MnaMachine(new MachineCore(BenchTest.MILL), bus);
        m.addTo(sim);
        double[] ohms = run(m, c -> c.takeJoules(50), 200); // (the sag holds the refill back a second, then it fills the buffer)
        for (int i = 100; i < 200; i++) assertEquals(ohms[99], ohms[i], "tick " + i);
        assertTrue(ohms[99] < 100, "a real load: " + ohms[99]);
        // still exact: the books read the resistor as it is
        MachineCore c = m.core();
        assertEquals(c.joulesIn(), c.joulesOut() + c.bufferJoules() + c.joulesLost(), 1e-9 * c.joulesIn());
    }

    /** A producer given energy every third tick stays connected rather than switching each time. */
    @Test
    void intermittentProducerStaysConnected() {
        sim.addElectricalComponent(new Resistor(bus, null).setR(100));
        MnaMachine m = new MnaMachine(new MachineCore(BenchTest.DYNAMO), bus);
        m.addTo(sim);
        int[] tick = {0};
        double[] ohms = run(m, c -> {
            if (tick[0]++ % 3 == 0) c.giveJoules(30);
        }, 60);
        for (int i = 0; i < 60; i++) assertEquals(m.core().sourceOhms(), ohms[i], "tick " + i);
        assertTrue(m.core().joulesOut() <= m.core().joulesIn() + 1e-6);
        // given nothing more, it opens SOURCE_HOLD_TICKS after its queue runs dry
        ohms = run(m, c -> {
        }, 80);
        int open = 0;
        while (ohms[open] != MnaConst.highImpedance) open++;
        assertTrue(m.core().bufferJoules() <= 1e-6);
        assertTrue(open >= MachineCore.SOURCE_HOLD_TICKS, "opened after " + open);
        for (int i = open; i < 80; i++) assertEquals(MnaConst.highImpedance, ohms[i]);
    }

    @Test
    void refusesAnotherStepRate() {
        Simulator fast = new Simulator(0.05, 0.025, 50, 1 / 400.0);
        fast.init();
        MnaMachine m = new MnaMachine(new MachineCore(BenchTest.MILL), new ElectricalLoad());
        String msg = assertThrows(IllegalStateException.class, () -> m.addTo(fast)).getMessage();
        assertTrue(msg.contains("one electrical step per 0.05 s tick"), msg);
    }
}
