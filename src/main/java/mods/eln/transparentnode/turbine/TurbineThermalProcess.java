package mods.eln.transparentnode.turbine;

import mods.eln.init.Config;
import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.mna.component.VoltageSource;


public class TurbineThermalProcess implements IProcess {
    private final TurbineElement turbine;

    private double efficiency = 0.0;

    public TurbineThermalProcess(TurbineElement t) {
        this.turbine = t;
    }

    public double getEfficiency() {
        return efficiency;
    }

    @Override
    public void process(double time) {
        TurbineDescriptor descriptor = turbine.descriptor;

        VoltageSource src = turbine.electricalPowerSourceProcess;

        efficiency = Math.abs(1 - (turbine.coolLoad.Tc + PhysicalConstant.Tref) / (turbine.warmLoad.Tc + PhysicalConstant.Tref));
        if (efficiency < 0.05) efficiency = 0.05;

        double E = src.getP() * time / Config.INSTANCE.getHeatTurbinePowerFactor();

        double Pout = E / time;
        double Pin = descriptor.PoutToPin.getValue(Pout) / efficiency;
        turbine.warmLoad.movePowerTo(-Pin);
        turbine.coolLoad.movePowerTo(Pin * (1 - efficiency));
    }
}
