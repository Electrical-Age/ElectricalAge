package mods.eln.api.testkit;

import mods.eln.api.MachineSpec;
import mods.eln.api.spi.Implementations;
import mods.eln.api.testkit.spi.TestKitProvider;

import java.util.Objects;

/**
 * Test helpers for machines built on the API, for JUnit with no Minecraft world: bench() runs machines in EA's
 * real circuit simulator, tick by tick in the game's order; verifySpec(spec) runs a canned property suite on a spec.
 * They run EA's own code: a test compiled against the eln-api jars runs with EA's mod jar on its runtime classpath
 * (docs/api.md, "Testing"); without it they throw IllegalStateException.
 */
public final class ElnTestKit {
    private ElnTestKit() {
    }

    private static volatile TestKitProvider impl;

    private static TestKitProvider impl() {
        TestKitProvider p = impl;
        if (p == null) impl = p = Implementations.load(TestKitProvider.class, "mods.eln.api.impl.testkit.TestKitImpl");
        return p;
    }

    /** An empty bench: no supply until supply(volts, ohms), no line resistance until line(ohms). */
    public static Bench bench() {
        return impl().bench();
    }

    /**
     * Runs the spec through EA's canned checks, each on a fresh bench with the machine and a standard owner (it asks
     * a tick's rated energy every tick, or keeps a producer's queue full): a voltage sweep 0..2 x nominal (the state
     * at each point, the work it gets), rated power at nominal, step loads (the rise a tick after the ask, the fall
     * within two, idle draw between), tryTakeJoules all or nothing, brownout (STOP stops and recovers without
     * chatter, SLOW runs slower), a weak line, the overvoltage trip and cooldown (or the burn), overheat and its reset
     * (with a heat model), and a producer's droop, current limit and pacing; on every tick the energy books balance
     * exactly, the buffer stays in bounds and the draw under rated x (U/U_nom)^2. Returns the report when every check
     * passes (some may be SKIP, with the reason); else throws SpecFailure, an AssertionError whose message is the
     * report, failures with the end of their bench trace. For a mod's JUnit tests: {@code ElnTestKit.verifySpec(MY_SPEC);}
     */
    public static SpecReport verifySpec(MachineSpec spec) {
        SpecReport r = impl().check(Objects.requireNonNull(spec, "spec"));
        if (!r.passed()) throw new SpecFailure(r);
        return r;
    }

    /** verifySpec's failure: the report is its message, and report() has the checks. */
    public static final class SpecFailure extends AssertionError {
        private final transient SpecReport report;

        public SpecFailure(SpecReport report) {
            super(report.toString());
            this.report = report;
        }

        public SpecReport report() {
            return report;
        }
    }
}
