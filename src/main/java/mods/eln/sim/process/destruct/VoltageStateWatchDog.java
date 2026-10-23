package mods.eln.sim.process.destruct;

import mods.eln.sim.mna.state.VoltageState;

public class VoltageStateWatchDog extends ValueWatchdog {

    VoltageState state;

    @Override
    double getValue() {
        return state.getU();
    }

    public VoltageStateWatchDog set(VoltageState state) {
        this.state = state;
        return this;
    }

    public VoltageStateWatchDog setUNominal(double uNominal) {
        this.max = uNominal * 1.3;
        this.min = -uNominal * 0.2;
        this.timeoutReset = uNominal * 0.05 * 5;
        return this;
    }

    public VoltageStateWatchDog setUNominalMirror(double uNominal) {
        this.max = uNominal * 1.3;
        this.min = -max;
        this.timeoutReset = uNominal * 0.05 * 5;
        return this;
    }

    public VoltageStateWatchDog setUMaxMin(double uNominal) {
        this.max = uNominal * 1.3;
        this.min = -uNominal * 1.3;
        this.timeoutReset = uNominal * 0.05 * 5;
        return this;
    }

    @Override
    FailureCause cause() {
        return FailureCause.OVERVOLTAGE;
    }

    /** (Tests: the cause a failure at value would have.) */
    public mods.eln.sim.process.destruct.FailureCause causeFor(double value) {
        return cause(value);
    }

    /** Below the minimum (a negative voltage on a polar device): reverse voltage, not overvoltage. */
    @Override
    FailureCause cause(double value) {
        return value < 0 && value < min ? FailureCause.REVERSE_VOLTAGE : FailureCause.OVERVOLTAGE;
    }
}
