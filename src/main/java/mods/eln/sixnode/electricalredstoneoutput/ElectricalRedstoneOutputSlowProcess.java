package mods.eln.sixnode.electricalredstoneoutput;

import mods.eln.sim.IProcess;

public class ElectricalRedstoneOutputSlowProcess implements IProcess {

    ElectricalRedstoneOutputElement element;

    double sleepCounter = 0;
    static final double sleepDuration = 0.2;

    public ElectricalRedstoneOutputSlowProcess(ElectricalRedstoneOutputElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        if (sleepCounter == 0.0) {
            if (element.refreshRedstone())
                sleepCounter = sleepDuration;
        } else {
            sleepCounter -= time;
            if (sleepCounter < 0.0) sleepCounter = 0.0;
        }
    }
}
