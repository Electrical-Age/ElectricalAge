package mods.eln.sixnode.electricalvumeter;

import mods.eln.sim.IProcess;

public class ElectricalVuMeterSlowProcess implements IProcess {

    ElectricalVuMeterElement element;

    double timeCounter = 0;
    static final double refreshPeriode = 0.25;
    boolean lastState;

    public ElectricalVuMeterSlowProcess(ElectricalVuMeterElement element) {
        this.element = element;
        lastState = element.inputGate.stateHigh();
    }

    @Override
    public void process(double time) {
        if (element.descriptor.onOffOnly) {
            if (lastState) {
                if (element.inputGate.stateLow()) {
                    lastState = false;
                    element.needPublish();
                }
            } else {
                if (element.inputGate.stateHigh()) {
                    lastState = true;
                    element.needPublish();
                }
            }
        } else {
            timeCounter += time;
            if (timeCounter >= refreshPeriode) {
                timeCounter -= refreshPeriode;
                element.needPublish();
            }
        }
    }
}
