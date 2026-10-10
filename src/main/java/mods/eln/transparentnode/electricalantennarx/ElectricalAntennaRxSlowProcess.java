package mods.eln.transparentnode.electricalantennarx;

import mods.eln.sim.IProcess;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.FailureCause;

public class ElectricalAntennaRxSlowProcess implements IProcess {

    ElectricalAntennaRxElement element;
    private final Failure failure;

    public ElectricalAntennaRxSlowProcess(ElectricalAntennaRxElement element) {
        this.element = element;
        this.failure = new Failure(element).electronics();
    }

    @Override
    public void process(double time) {
        if (element.powerSrc.getP() > element.descriptor.electricalMaximalPower) {
            failure.fail(FailureCause.OVERHEAT, element.powerSrc.getP());
        } else if (element.powerOut.getU() > element.descriptor.electricalMaximalVoltage) {
            failure.fail(FailureCause.OVERVOLTAGE, element.powerOut.getU());
        }
    }
}
