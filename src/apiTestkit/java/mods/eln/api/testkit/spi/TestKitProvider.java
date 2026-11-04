package mods.eln.api.testkit.spi;

import mods.eln.api.MachineSpec;
import mods.eln.api.testkit.Bench;
import mods.eln.api.testkit.SpecReport;

/**
 * EA's side of ElnTestKit, found by name at first use (Implementations.load). EA implements it; mods neither call
 * nor implement it.
 */
public interface TestKitProvider {
    /** ElnTestKit.bench(). */
    Bench bench();

    /** verifySpec's checks on spec, passed or not. */
    SpecReport check(MachineSpec spec);
}
