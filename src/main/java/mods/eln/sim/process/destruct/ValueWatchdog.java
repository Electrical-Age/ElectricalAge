package mods.eln.sim.process.destruct;

import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;

public abstract class ValueWatchdog implements IProcess {

    IDestructable destructable;
    double perOverflowStrenght = 1;
    double min;
    double max;

    double timeoutReset = 2;

    double timeout = 0;
    boolean boot = true;
    boolean joker = true;

    double rand = Utils.rand(0.5, 1.5);

    @Override
    public void process(double time) {
        if (boot) {
            boot = false;
            timeout = timeoutReset;
        }
        double value = getValue();
        double overflow = Math.max(value - max, min - value);
        if (overflow > 0) {
            if (joker) {
                joker = false;
                overflow = 0;
            }
        } else {
            joker = true;
        }

        timeout -= time * overflow * rand;
        if (timeout > timeoutReset) {
            timeout = timeoutReset;
        }
        if (timeout < 0) {
            Utils.print("%s destroying %s",
                getClass().getName(),
                destructable.describe());
            // severity: overflow as a rate of the budget, comparable across watchdogs of different units
            destructable.fail(cause(), value, overflow * rand / timeoutReset);
            // re-arm: if the failure is deferred (a worse one in the circuit went first), trip again only if the
            // fault lasts another full budget
            timeout = timeoutReset;
        }
    }

    public ValueWatchdog set(IDestructable d) {
        this.destructable = d;
        return this;
    }

    abstract double getValue();

    abstract FailureCause cause();

    public void disable() {
        this.max = 100000000;
        this.min = -max;
        this.timeoutReset = 10000000;
    }

    /** Another device of the network failed first (FaultNetwork.relieve): back to the limit, full budget. */
    public void relieve() {
        timeout = timeoutReset;
        clampToLimit();
    }

    /** Set the watched value back to the limit when it is a stored state (temperature); default: nothing. */
    void clampToLimit() {
    }

    public void reset() {
        boot = true;
    }
}
