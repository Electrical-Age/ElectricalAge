package mods.eln.transparentnode.battery;

import mods.eln.sim.IProcess;

/**
 * The battery's protection chips (Baughn 2026-10-27, after a protected battery burnt: nearly empty, ~4 V, put on a 50 V
 * bus, its inrush heated it ~70 K inside one 0.05 s check of the old on/off chip). The slow part (this process, every
 * 0.05 s) keeps the flags with real hysteresis; {@link Step}, every electrical step, sets the cut switch:
 * <ul>
 * <li>Overheating protection: a charge limiter. It holds the charging current to the battery's rated current
 * ({@code electricalStdI}), less as it warms from {@link #TAPER_K} under the cut down to none {@link #TAPER_END_K} under
 * it, by the cut switch's resistance; and still cuts at {@link #CUT_K} under ⅔ of the warm limit (60 C for the stock
 * batteries), reconnecting {@link #RECONNECT_K} under it.</li>
 * <li>Overvoltage protection: no charging over U·1.1 > Umax (as before), and a low-voltage disconnect: below
 * {@link #LVD_OFF} of its energy (as its screen shows it) it stops discharging (charging still goes in) until it is
 * back over {@link #LVD_ON}.</li>
 * </ul>
 */
public class BatteryInventoryProcess implements IProcess {
    public static final double CUT_K = 4, RECONNECT_K = 15, TAPER_K = 8, TAPER_END_K = 1, LVD_OFF = 0.10, LVD_ON = 0.15;

    BatteryElement battery;

    boolean thermalCut = false, lowVoltage = false;
    /** What the protection does now, for Jade: 0 nothing, 1 limiting charge, 2 cut (too hot), 3 cut (overvoltage),
     *  4 low-voltage disconnect (charge only). */
    int state;
    /** The charge current the limiter allows now (A). */
    double chargeLimit;

    public BatteryInventoryProcess(BatteryElement battery) {
        this.battery = battery;
    }

    /** The temperature (Tc scale) the overheating chip cuts at. */
    double cutT() {
        return battery.descriptor.thermalWarmLimit * 2 / 3 - CUT_K;
    }

    @Override
    public void process(double time) {
        if (!battery.descriptor.lifeEnable)
            battery.batteryProcess.life = 1.0;

        double t = battery.thermalLoad.Tc;
        if (battery.hasOverHeatingProtection()) {
            if (t > cutT()) thermalCut = true;
            if (t < cutT() + CUT_K - RECONNECT_K) thermalCut = false;
        } else {
            thermalCut = false;
        }
        if (battery.hasOverVoltageProtection()) {
            // (by energy, the share the battery's screen shows: Baughn read 5% there while the charge, which the
            // voltage curve makes run ahead of the energy, was still over 10%, and took the chip for broken)
            double q = battery.batteryProcess.getEnergy() / Math.max(1e-9, battery.batteryProcess.getEnergyMax());
            if (q < LVD_OFF) lowVoltage = true;
            if (q > LVD_ON) lowVoltage = false;
        } else {
            lowVoltage = false;
        }

        double U = battery.positiveLoad.getU() - battery.negativeLoad.getU();

        double currentDropPower = (U - battery.descriptor.currentDropVoltage) / battery.descriptor.electricalU * battery.descriptor.currentDropFactor;
        if (currentDropPower > 0.1) {
            battery.dischargeResistor.setR(1 / (1 / battery.descriptor.electricalRp + 1 / ((U * U) / currentDropPower)));
        } else {
            battery.dischargeResistor.setR(battery.descriptor.electricalRp);
        }
    }

    /** Every electrical step: the cut switch's state and resistance from the flags and the terminal voltage. */
    public class Step implements IProcess {
        @Override
        public void process(double time) {
            BatteryDescriptor d = battery.descriptor;
            double emf = battery.batteryProcess.getU();
            double uExt = battery.cutLoad.getU() - battery.negativeLoad.getU();
            boolean charging = uExt > emf;
            boolean cut = false;
            int s = 0;
            if (thermalCut) {
                cut = true;
                s = 2;
            } else if (battery.hasOverVoltageProtection()) {
                if (charging && emf * 1.1 > battery.batterySlowProcess.getUMax()) {
                    cut = true;
                    s = 3;
                } else if (!charging && (lowVoltage || emf < battery.batteryProcess.uNominal * 0.001)) {
                    cut = true;
                    s = 4;
                }
            }
            double rOn = d.electricalRs / 2, r = rOn;
            chargeLimit = Double.POSITIVE_INFINITY;
            if (!cut && battery.hasOverHeatingProtection()) {
                double taper = (cutT() - battery.thermalLoad.Tc - TAPER_END_K) / (TAPER_K - TAPER_END_K);
                chargeLimit = d.electricalStdI * Math.max(0, Math.min(1, taper));
                if (charging) {
                    // the resistance that lets chargeLimit through from this terminal voltage (internal 2 Rs besides)
                    double need = chargeLimit <= 1e-6 ? Double.POSITIVE_INFINITY : (uExt - emf) / chargeLimit - 2 * d.electricalRs;
                    if (need > rOn) {
                        r = Math.min(need, 1e9);
                        s = 1;
                    }
                }
            }
            if (s == 0 && lowVoltage) s = 4;   // (charging in while it is low)
            state = s;
            if (battery.cutSwitch.getState() == cut) battery.cutSwitch.setState(!cut);
            double now = battery.cutSwitch.getBaseR();
            if (Math.abs(r - now) > 0.02 * Math.min(r, now)) battery.cutSwitch.setR(r);
        }
    }

    /** What the protection does now, for Jade; null when it does nothing. */
    public String describe() {
        return switch (state) {
            case 1 -> String.format("limiting charge to %.1f A", chargeLimit);
            case 2 -> String.format("cut: too hot (%.0f °C)", battery.thermalLoad.Tc + mods.eln.sim.PhysicalConstant.Tamb);
            case 3 -> "cut: full (overvoltage)";
            case 4 -> "low-voltage disconnect: charging only";
            default -> null;
        };
    }
}
