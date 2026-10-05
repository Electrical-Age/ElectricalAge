package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.State;

/**
 * Test-only ideal current source (EA has no current-source component). It injects a fixed current into
 * aPin and draws it from bPin (null = ground) through the right-hand side vector, which is the textbook
 * MNA stamp and the same mechanism Capacitor/Inductor history terms use (SubSystem.addToI).
 */
public class TestCurrentSource extends Bipole implements ISubSystemProcessI {
    public double i;

    public TestCurrentSource(State aPin, State bPin, double i) {
        super(aPin, bPin);
        this.i = i;
    }

    @Override
    public void applyTo(SubSystem s) {
    }

    @Override
    public void simProcessI(SubSystem s) {
        s.addToI(aPin, i);
        s.addToI(bPin, -i);
    }

    @Override
    public void addedTo(SubSystem s) {
        super.addedTo(s);
        s.addProcess(this);
    }

    @Override
    public void quitSubSystem() {
        subSystem.removeProcess(this);
        super.quitSubSystem();
    }

    @Override
    public double getCurrent() {
        return i;
    }
}
