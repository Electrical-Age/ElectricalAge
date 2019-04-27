package mods.eln.sim.nbt;

import mods.eln.Eln;
import mods.eln.init.Cable;
import mods.eln.misc.Utils;

public class NbtElectricalGateInputOutput extends NbtElectricalLoad {

    public NbtElectricalGateInputOutput(String name) {
        super(name);
        Cable.Companion.getSignal().descriptor.applyTo(this);
    }

    public String plot(String str) {
        return str + " " + Utils.plotVolt("", getU()) + Utils.plotAmpere("", getCurrent());
    }

    public boolean isInputHigh() {
        return getU() > Cable.SVU * 0.6;
    }

    public boolean isInputLow() {
        return getU() < Cable.SVU * 0.2;
    }

    public double getInputNormalized() {
        double norm = getU() * Cable.SVUinv;
        if (norm < 0.0) norm = 0.0;
        if (norm > 1.0) norm = 1.0;
        return norm;
    }

    public double getInputBornedU() {
        double U = this.getU();
        if (U < 0.0) U = 0.0;
        if (U > Cable.SVU) U = Cable.SVU;
        return U;
    }
}
