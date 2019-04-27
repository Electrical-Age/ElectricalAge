package mods.eln.sim;

import mods.eln.Eln;
import mods.eln.init.Cable;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.state.State;

public class SignalRp extends Resistor {
    public SignalRp(State aPin) {
        super(aPin, null);
        setR(Cable.SVU / Cable.SVIinv);
    }
}
