package mods.eln.transparentnode.battery;

import mods.eln.sim.IProcess;

public class BatteryInventoryProcess implements IProcess {

    BatteryElement battery;

    boolean thermalCut = false;

    public BatteryInventoryProcess(BatteryElement battery) {
        this.battery = battery;
    }

    @Override
    public void process(double time) {
        if (!battery.descriptor.lifeEnable)
            battery.batteryProcess.life = 1.0;

        boolean cut = false;
        if (battery.hasOverHeatingProtection()) {
            // WP16b: 1.7.10 compared the temperature (K) to thermalHeatTime (30 s); compare to the warm limit
            // instead, at half of it (= 30 K for every EA battery: warm limit 60), so the trip point is unchanged.
            double cutT = battery.descriptor.thermalWarmLimit * 0.5;
            if (battery.thermalLoad.Tc * 1.1 > cutT) {
                thermalCut = true;
            }
            if (battery.thermalLoad.Tc * 1.15 < cutT) {
                thermalCut = false;
            }
        } else {
            thermalCut = false;
        }

        if (thermalCut) cut = true;

        if (battery.hasOverVoltageProtection()) {
            if (battery.batteryProcess.getU() * 1.1 > battery.batterySlowProcess.getUMax()) {
                if (battery.batteryProcess.getU() < battery.cutLoad.getU() - battery.negativeLoad.getU()) {
                    cut = true;
                }
            }
            if (battery.batteryProcess.getU() < battery.batteryProcess.uNominal * 0.001) {
                if (battery.batteryProcess.getU() > battery.cutLoad.getU() - battery.negativeLoad.getU()) {
                    cut = true;
                }
            }
        }
        battery.cutSwitch.setState(!cut);

        double U = battery.positiveLoad.getU() - battery.negativeLoad.getU();

        double currentDropPower = (U - battery.descriptor.currentDropVoltage) / battery.descriptor.electricalU * battery.descriptor.currentDropFactor;
        if (currentDropPower > 0.1) {
            battery.dischargeResistor.setR(1 / (1 / battery.descriptor.electricalRp + 1 / ((U * U) / currentDropPower)));
        } else {
            battery.dischargeResistor.setR(battery.descriptor.electricalRp);
        }
    }
}
