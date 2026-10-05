package mods.eln;

import mods.eln.sim.Simulator;

/**
 * TEST STUB for the simulator test harness (tools/simtest.sh). Replaces the real mod class, of which the
 * simulator closure only touches the static {@code simulator} field (ThermalLoadInitializer*.setMaximalPower
 * calls Eln.simulator.checkThermalLoad). Not on the mod's classpath.
 */
public class Eln {
    public static Simulator simulator;
}
