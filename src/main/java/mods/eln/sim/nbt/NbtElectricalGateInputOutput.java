package mods.eln.sim.nbt;


import mods.eln.misc.Utils;

/** A signal pin that is read and driven (the Modbus RTU's I/O): a gate input that plots as volts and amps. */
public class NbtElectricalGateInputOutput extends NbtElectricalGateInput {

    public NbtElectricalGateInputOutput(String name) {
        super(name);
    }

    @Override
    public String plot(String str) {
        return str + " " + Utils.plotVolt("", getU()) + Utils.plotAmpere("", getCurrent());
    }
}
