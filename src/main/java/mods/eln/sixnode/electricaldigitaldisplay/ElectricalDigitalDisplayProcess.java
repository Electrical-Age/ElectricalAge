package mods.eln.sixnode.electricaldigitaldisplay;

import mods.eln.sim.IProcess;

public class ElectricalDigitalDisplayProcess implements IProcess {
    ElectricalDigitalDisplayElement element;

    public ElectricalDigitalDisplayProcess(ElectricalDigitalDisplayElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        element.current = (float) element.input.getNormalized();
        if(element.current != element.last) {
            element.needPublish();
            element.last = element.current;
        }
    }
}
