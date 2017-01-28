package mods.eln.transparentnode.electricalmachine;

import mods.eln.sim.IProcess;

public class ElectricalMachineSlowProcess implements IProcess {
    private final ElectricalMachineElement element;

    private double lastPublishAt = 0;
    private double lastUpdate = 0;
    private boolean boot = true;

    public ElectricalMachineSlowProcess(ElectricalMachineElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        double P = element.electricalResistor.getP();
        lastUpdate += time;
        if (!boot) {
            if (Math.abs((P - lastPublishAt) / (lastPublishAt + 1.0)) > 1 / 32.0 && lastUpdate > 0.2) {
                element.needPublish();
                lastPublishAt = P;
                lastUpdate = 0;
            }
        }

        boot = false;
    }
}
