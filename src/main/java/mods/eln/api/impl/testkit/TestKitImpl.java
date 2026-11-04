package mods.eln.api.impl.testkit;

import mods.eln.api.MachineSpec;
import mods.eln.api.testkit.Bench;
import mods.eln.api.testkit.SpecReport;
import mods.eln.api.testkit.spi.TestKitProvider;

/** EA's implementation behind ElnTestKit, found by name. */
public final class TestKitImpl implements TestKitProvider {
    public static final TestKitImpl INSTANCE = new TestKitImpl();

    private TestKitImpl() {
    }

    @Override
    public Bench bench() {
        return new BenchImpl();
    }

    @Override
    public SpecReport check(MachineSpec spec) {
        return SpecSuite.run(spec, spec);
    }
}
