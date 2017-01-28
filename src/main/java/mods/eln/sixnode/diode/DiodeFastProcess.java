package mods.eln.sixnode.diode;

import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;

public class DiodeFastProcess implements IRootSystemPreStepProcess {

    VoltageSource source;

    DiodeFastProcess(VoltageSource source) {
        this.source = source;
    }

    @Override
    public void rootSystemPreStepProcess() {
        double u1 = 0, u2 = 0;
        if (source.aPin != null) {
            //source.getSubSystem().getTh(source.aPin, voltageSource)
        }
    }
}
