package mods.eln.sim.mna;

import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.VoltageState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SmokeTest {
    @Test
    public void divider() {
        RootSystem root = new RootSystem(0.05, 1);
        VoltageState a = new VoltageState(), b = new VoltageState();
        root.addState(a);
        root.addState(b);
        root.addComponent(new VoltageSource("u", a, null).setU(10));
        root.addComponent(new Resistor(a, b).setR(10));
        root.addComponent(new Resistor(b, null).setR(30));
        root.step();
        assertEquals(7.5, b.state, 1e-9);
    }
}
