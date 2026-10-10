package mods.eln.transparentnode.turbine;

import mods.eln.sim.IProcess;
import mods.eln.sim.mna.SubSystem.Th;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;


public class TurbineElectricalProcess implements IProcess, IRootSystemPreStepProcess {
    private final TurbineElement turbine;

    public TurbineElectricalProcess(TurbineElement turbine) {
        this.turbine = turbine;
    }

    @Override
    public void process(double time) {
        TurbineDescriptor descriptor = turbine.descriptor;
        double deltaT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
        double targetU = descriptor.TtoU.getValue(deltaT);

        Th th = turbine.positiveLoad.getSubSystem().getTh(turbine.positiveLoad, turbine.electricalPowerSourceProcess);
        double Ut;
        if (targetU < th.U) {
            Ut = th.U;
        } else if (th.isHighImpedance()) {
            Ut = targetU;
        } else {
            double a = 1 / th.R;
            double b = descriptor.powerOutPerDeltaU - th.U / th.R;
            double c = -descriptor.powerOutPerDeltaU * targetU;
            Ut = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
        }

        double i = (Ut - th.U) / th.R;
        double p = i * Ut;
        double pMax = descriptor.nominalP * 1.5;
        if (p > pMax) {
            Ut = (Math.sqrt(th.U * th.U + 4 * pMax * th.R) + th.U) / 2;
            Ut = Math.min(Ut, targetU);
            if (Double.isNaN(Ut)) Ut = 0;
            if (Ut < th.U) Ut = th.U;
        }

        turbine.electricalPowerSourceProcess.setU(Ut);
    }

    @Override
    public void rootSystemPreStepProcess() {
        process(0);
    }
}
