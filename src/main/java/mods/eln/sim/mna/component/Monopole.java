package mods.eln.sim.mna.component;

import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;

public abstract class Monopole extends Component {

    VoltageState pin;

    public Monopole connectTo(VoltageState pin) {
        breakConnection();
        this.pin = pin;
        if (pin != null) pin.add(this);
        return this;
    }

    /** Leave the pin's component list (as Bipole does), so a removed monopole is not rebuilt into the circuit. */
    @Override
    public void breakConnection() {
        if (pin != null) pin.remove(this);
    }

    @Override
    public State[] getConnectedStates() {
        return new State[]{pin};
    }
}
