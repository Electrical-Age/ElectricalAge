package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;

/** Test-only Monopole (EA's main code has none left): a conductance g from its pin to ground. */
public class TestConductance extends Monopole {
    public final double g;

    public TestConductance(double g) {
        this.g = g;
    }

    @Override
    public void applyTo(SubSystem s) {
        s.addToA(pin, pin, g);
    }

    public double getCurrent() {
        return pin == null ? 0 : pin.state * g;
    }
}
