package mods.eln.api.testkit;

import mods.eln.api.MachineSpec;

import java.util.List;

/**
 * What ElnTestKit.verifySpec found: one line per check, PASS, FAIL or SKIP with what was measured, and for a failure
 * the end of its bench trace. toString() is the readable report.
 */
public final class SpecReport {
    /** How a check came out. SKIP: it can't be run for this spec (the reason is in the detail). */
    public enum Result {PASS, FAIL, SKIP}

    /** One check: its name, result, what was measured, and (failures) the last ticks of its bench trace. */
    public record Check(String name, Result result, String detail, String trace) {
    }

    private final MachineSpec spec;
    private final List<Check> checks;

    SpecReport(MachineSpec spec, List<Check> checks) {
        this.spec = spec;
        this.checks = List.copyOf(checks);
    }

    public MachineSpec spec() {
        return spec;
    }

    public List<Check> checks() {
        return checks;
    }

    /** No check failed. */
    public boolean passed() {
        return checks.stream().noneMatch(c -> c.result() == Result.FAIL);
    }

    public List<Check> failures() {
        return checks.stream().filter(c -> c.result() == Result.FAIL).toList();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("verifySpec(").append(spec).append("): ").append(checks.size()).append(" checks, ")
            .append(failures().size()).append(" failed\n");
        for (Check c : checks) {
            b.append(String.format("  %-4s %s: %s%n", c.result(), c.name(), c.detail()));
            if (c.result() == Result.FAIL && !c.trace().isEmpty()) {
                b.append("       trace (tick volts | amps buffer state):\n");
                for (String l : c.trace().split("\n")) b.append("       ").append(l).append('\n');
            }
        }
        return b.toString();
    }
}
