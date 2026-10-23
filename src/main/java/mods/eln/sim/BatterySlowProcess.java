package mods.eln.sim;

import mods.eln.server.SaveConfig;

public abstract class BatterySlowProcess implements IProcess {

    protected BatteryProcess batteryProcess;

    ThermalLoad thermalLoad;

    public double lifeNominalCurrent, lifeNominalLost;

    public BatterySlowProcess(BatteryProcess batteryProcess, ThermalLoad thermalLoad) {
        this.batteryProcess = batteryProcess;
        this.thermalLoad = thermalLoad;
    }

    @Override
    public void process(double time) {
        double U = batteryProcess.getU();
        if (U < -0.1 * batteryProcess.uNominal) {
            destroy();
            return;
        }
        if (U > getUMax()) {
            destroy();
            return;
        }
        if (SaveConfig.instance.batteryAging) {
            double newLife = batteryProcess.life;
            double normalisedCurrent = Math.abs(batteryProcess.getDischargeCurrent()) / lifeNominalCurrent;
            // (Baughn 2026-10-17: lead-acid wore out far too fast: config batteryLifeFactor, default 25; and more like the real
            // thing: wear doubles every 10 C above ambient, and a battery cycled low wears faster, up to 4x when empty)
            double hot = thermalLoad == null ? 1 : Math.min(16, Math.pow(2, Math.max(0, thermalLoad.getT()) / 10.0));
            double soc = Math.max(0, Math.min(1, batteryProcess.getCharge()));
            double low = 1 + 3 * (1 - soc) * (1 - soc);
            newLife -= normalisedCurrent * normalisedCurrent * lifeNominalLost * time * hot * low / mods.eln.ElnConfig.batteryLifeFactor;

            if (newLife < 0.1) newLife = 0.1;
            batteryProcess.changeLife(newLife);
        }
    }

    public double getUMax() {
        return 1.3 * batteryProcess.uNominal;
    }

    public abstract void destroy();
}
