package mods.eln.transparentnode.turbine;

import mods.eln.Eln;
import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.mna.component.VoltageSource;


public class TurbineThermalProcess implements IProcess {
    private final TurbineElement turbine;

    public TurbineThermalProcess(TurbineElement t) {
        this.turbine = t;
    }

    @Override
    public void process(double time) {
        TurbineDescriptor descriptor = turbine.descriptor;

        VoltageSource src = turbine.electricalPowerSourceProcess;

        double eff = Math.abs(1 - (turbine.coolLoad.Tc + PhysicalConstant.Tref) / (turbine.warmLoad.Tc + PhysicalConstant.Tref));
        if (eff < 0.05) eff = 0.05;

        double E = src.getP() * time / Eln.instance.heatTurbinePowerFactor;

        double Pout = E / time;
        double Pin = descriptor.PoutToPin.getValue(Pout) / eff;
        turbine.warmLoad.movePowerTo(-Pin);
        turbine.coolLoad.movePowerTo(Pin * (1 - eff));
    }
}
