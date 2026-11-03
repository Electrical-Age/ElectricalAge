package mods.eln.api.impl.testkit;

import mods.eln.api.Brownout;
import mods.eln.api.MachineSpec;
import mods.eln.api.RelativeFace;
import mods.eln.api.Tier;
import mods.eln.api.testkit.ElnTestKit;
import mods.eln.api.testkit.SpecReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ElnTestKit.verifySpec: EA's own specs and a spread of others pass it, and a machine that doesn't behave as its spec
 * says (the bench runs other ratings than the spec checked) fails it with a readable report.
 */
public class VerifySpecTest {
    /** The Linear Servo Drive's spec, as its descriptor builds it (SLOW, a heat model, signals at cable positions). */
    static MachineSpec drive() {
        return mods.eln.robotics.servo.LinearServoDriveDescriptor.spec();
    }

    static final List<MachineSpec> SPECS = List.of(
        MachineSpec.consumer(Tier.V50).ratedWatts(100).build(),
        MachineSpec.consumer(Tier.V200).ratedWatts(2000).idleWatts(20).build(),
        MachineSpec.consumer(Tier.V50).ratedWatts(200).onBrownout(Brownout.SLOW).burnsOnOvervoltage(mods.eln.api.element.Machines.BURNS_OUT)
            .power(RelativeFace.LEFT, RelativeFace.RIGHT, RelativeFace.UP, RelativeFace.DOWN).build(),   // EA's macerator's shape
        MachineSpec.consumer(Tier.V200).ratedWatts(1000).onBrownout(Brownout.SLOW).burnsOnOvervoltage(mods.eln.api.element.Machines.BURNS_OUT).power(RelativeFace.FRONT).build(),
        MachineSpec.consumer(Tier.V800).ratedWatts(10000).idleWatts(500).minVoltageRatio(0.9).tripCooldownSeconds(1).build(),
        MachineSpec.consumer(Tier.V3200).ratedWatts(50000).heatCapacityJPerK(2000).heatLossWPerK(50).overheatRiseKelvin(70).build(),
        MachineSpec.consumer(12).ratedWatts(5).idleWatts(1).onBrownout(Brownout.SLOW).build(),
        MachineSpec.producer(Tier.V200).ratedWatts(1500).build(),
        MachineSpec.producer(Tier.V50).ratedWatts(300).heatCapacityJPerK(500).heatLossWPerK(5).overheatRiseKelvin(40).build(),
        MachineSpec.producer(Tier.V800).ratedWatts(20000).burnsOnOvervoltage(mods.eln.api.element.Machines.BURNS_OUT).build());

    @Test
    void easSpecsAndASpreadOfOthersPass() {
        for (MachineSpec s : SPECS) {
            SpecReport r = ElnTestKit.verifySpec(s);
            assertTrue(r.passed(), r::toString);
            assertTrue(r.checks().size() >= 20, r::toString);
        }
    }

    @Test
    void theDrivesSpecPassesWithItsHeatModelRunToTheReset() {
        SpecReport r = ElnTestKit.verifySpec(drive());
        SpecReport.Check heat = r.checks().stream().filter(c -> c.name().startsWith("overheat")).findFirst().orElseThrow();
        assertEquals(SpecReport.Result.PASS, heat.result(), r::toString);
        assertEquals("overheat trip and reset", heat.name());
        SpecReport.Check hot = r.checks().stream().filter(c -> c.name().equals("overheat trip and reset at 90 C ambient"))
            .findFirst().orElseThrow(() -> new AssertionError(r.toString()));
        assertEquals(SpecReport.Result.PASS, hot.result(), r::toString);
        assertTrue(hot.detail().contains("limit 170.0 C"), hot::detail);
    }

    @Test
    void aSpecWithoutAHeatModelSkipsTheOverheatCheckSayingWhy() {
        SpecReport r = ElnTestKit.verifySpec(SPECS.get(0));
        SpecReport.Check heat = r.checks().stream().filter(c -> c.name().equals("overheat")).findFirst().orElseThrow();
        assertEquals(SpecReport.Result.SKIP, heat.result());
        assertEquals("no heat model", heat.detail());
        assertTrue(r.toString().contains("SKIP overheat: no heat model"), r::toString);
    }

    @Test
    void aMachineThatDoesntMatchItsSpecFailsWithTheTraceInTheReport() {
        MachineSpec claimed = MachineSpec.consumer(Tier.V200).ratedWatts(2000).build();
        // a higher undervoltage floor than claimed: it stops at 0.8 x nominal where the spec says it runs
        SpecReport floor = SpecSuite.run(claimed, MachineSpec.consumer(Tier.V200).ratedWatts(2000).minVoltageRatio(0.9).build());
        assertFalse(floor.passed());
        assertTrue(floor.failures().stream().anyMatch(c -> c.name().equals("sweep 0.80 x nominal") && c.detail().contains("expected RUNNING")),
            floor::toString);
        // less power than claimed: the work at nominal falls short
        SpecReport weak = SpecSuite.run(claimed, MachineSpec.consumer(Tier.V200).ratedWatts(1500).build());
        assertTrue(weak.failures().stream().anyMatch(c -> c.name().equals("sweep 1.00 x nominal")), weak::toString);
        // burns where the spec says it trips and resets
        SpecReport burns = SpecSuite.run(claimed, MachineSpec.consumer(Tier.V200).ratedWatts(2000).burnsOnOvervoltage(mods.eln.api.element.Machines.BURNS_OUT).build());
        assertTrue(burns.failures().stream().anyMatch(c -> c.name().equals("overvoltage trip and cooldown")), burns::toString);
        // a slower cooldown than claimed
        SpecReport slow = SpecSuite.run(claimed, MachineSpec.consumer(Tier.V200).ratedWatts(2000).tripCooldownSeconds(5).build());
        assertTrue(slow.failures().stream().anyMatch(c -> c.name().equals("overvoltage trip and cooldown")), slow::toString);
        // a producer weaker than claimed
        SpecReport dynamo = SpecSuite.run(MachineSpec.producer(Tier.V200).ratedWatts(1500).build(),
            MachineSpec.producer(Tier.V200).ratedWatts(1000).build());
        assertTrue(dynamo.failures().stream().anyMatch(c -> c.name().equals("droop: rated power at nominal volts")), dynamo::toString);
        String text = floor.toString();
        assertTrue(text.startsWith("verifySpec(consumer 200 V 2000 W): "), text);
        assertTrue(text.contains("  FAIL sweep 0.80 x nominal: UNDERVOLTAGE, expected RUNNING"), text);
        assertTrue(text.contains("trace (tick volts | amps buffer state):"), text);
    }

    @Test
    void verifySpecThrowsTheReport() {
        // (verifySpec checks the spec against itself; a failure is shown here through the seam's report)
        SpecReport bad = SpecSuite.run(MachineSpec.consumer(Tier.V200).ratedWatts(2000).build(),
            MachineSpec.consumer(Tier.V200).ratedWatts(2000).minVoltageRatio(0.9).build());
        ElnTestKit.SpecFailure f = new ElnTestKit.SpecFailure(bad);
        assertTrue(f instanceof AssertionError);
        assertEquals(bad.toString(), f.getMessage());
        assertThrows(NullPointerException.class, () -> ElnTestKit.verifySpec(null));
    }
}
