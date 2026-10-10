package mods.eln.sim.process.destruct;

public interface IDestructable {
    void destructImpl();

    String describe();

    /** Called by watchdogs; value is the watched value at the moment it tripped (V, K, W or rad/s). */
    default void fail(FailureCause cause, double value) {
        destructImpl();
    }
}
