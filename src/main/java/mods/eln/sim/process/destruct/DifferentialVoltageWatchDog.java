package mods.eln.sim.process.destruct;

import mods.eln.sim.mna.state.VoltageState;

/**
 * The voltage between two states (a floating port's + and -) within +-max: the usual watchdog's budget (timeout grows
 * with the overflow), for a device rated on what it stands across its terminals (the MPPT controller's panel input).
 */
public class DifferentialVoltageWatchDog extends ValueWatchdog {
    VoltageState pos, neg;

    public DifferentialVoltageWatchDog set(VoltageState pos, VoltageState neg) {
        this.pos = pos;
        this.neg = neg;
        return this;
    }

    /** Fails beyond +-uMax (no margin: uMax is the rating itself), with VoltageStateWatchDog's budget. */
    public DifferentialVoltageWatchDog setUMax(double uMax) {
        this.max = uMax;
        this.min = -uMax;
        this.timeoutReset = uMax * 0.05 * 5;
        return this;
    }

    @Override
    double getValue() {
        return pos.getU() - neg.getU();
    }

    @Override
    FailureCause cause() {
        return FailureCause.OVERVOLTAGE;
    }
}
