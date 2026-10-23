package mods.eln.sim.process.destruct;

public interface IDestructable {
    void destructImpl();

    String describe();

    /** Called by watchdogs; value is the watched value at the moment it tripped (V, K, W or rad/s). */
    default void fail(FailureCause cause, double value) {
        destructImpl();
    }

    /**
     * As fail(cause, value), with how far past its limit the watchdog was (overflow per second of its budget). The
     * simulator lets only the worst failure of a circuit happen in one tick; see Simulator.queueDestruct.
     */
    default void fail(FailureCause cause, double value, double severity) {
        fail(cause, value);
    }

    /** Ranks queued failures of one circuit; the highest goes. */
    default double severity() {
        return 0;
    }

    /** The device's node (failures in one network of connected nodes: one per tick), or null to fail on its own. */
    default Object circuit() {
        return null;
    }

    /** A queued failure lost to a worse one in its circuit: forget it (its watchdog has re-armed). */
    default void cancelFail() {
    }
}
