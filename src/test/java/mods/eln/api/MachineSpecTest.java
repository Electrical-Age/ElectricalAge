package mods.eln.api;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MachineSpec defaults, and build()'s messages for every rule it checks. */
public class MachineSpecTest {
    static String error(MachineSpec.Builder b) {
        return assertThrows(IllegalArgumentException.class, b::build).getMessage();
    }

    static void assertError(MachineSpec.Builder b, String expected) {
        String m = error(b);
        assertTrue(m.contains(expected), "expected \"" + expected + "\" in: " + m);
    }

    @Test
    void defaults() {
        MachineSpec s = MachineSpec.consumer(Tier.V200).ratedWatts(2000).build();
        assertEquals(MachineSpec.Kind.CONSUMER, s.kind());
        assertEquals(200, s.nominalVolts());
        assertEquals(0, s.idleWatts());
        assertEquals(0.75, s.minVoltageRatio());
        assertEquals(2500, s.maxWatts());
        assertEquals(3, s.tripCooldownSeconds());
        assertFalse(s.burnsOnOvervoltage());
        assertTrue(s.thermal().isEmpty());
        assertEquals(EnumSet.allOf(RelativeFace.class), s.powerFaces());
        assertTrue(s.busFace().isEmpty());
    }

    @Test
    void tiers() {
        assertEquals(50, Tier.V50.volts());
        assertEquals(200, Tier.V200.volts());
        assertEquals(800, Tier.V800.volts());
        assertEquals(3200, Tier.V3200.volts());
    }

    /** Tiers are a convenience: any positive voltage makes a spec. */
    @Test
    void anyNominalVolts() {
        MachineSpec s = MachineSpec.producer(230).ratedWatts(500).build();
        assertEquals(230, s.nominalVolts());
        assertEquals("producer 230 V 500 W", s.toString());
        assertError(MachineSpec.consumer(0).ratedWatts(1), "nominal volts must be positive and finite, got 0.0");
        assertError(MachineSpec.consumer(Double.NaN).ratedWatts(1), "nominal volts must be positive and finite");
        assertError(MachineSpec.consumer(Double.POSITIVE_INFINITY).ratedWatts(1), "nominal volts must be positive and finite");
    }

    @Test
    void defaultPowerLeavesSignalAndBusFaces() {
        MachineSpec s = MachineSpec.consumer(Tier.V50).ratedWatts(100)
            .signalIn("enable", RelativeFace.UP, 1).signalOut("done", RelativeFace.LEFT).bus(RelativeFace.BACK).build();
        assertEquals(EnumSet.of(RelativeFace.FRONT, RelativeFace.RIGHT, RelativeFace.DOWN), s.powerFaces());
        assertEquals(1, s.signalsIn().get(0).defaultValue());
        assertEquals(RelativeFace.BACK, s.busFace().orElseThrow());
    }

    @Test
    void explicitPower() {
        MachineSpec s = MachineSpec.producer(Tier.V800).ratedWatts(10000).power(RelativeFace.BACK).build();
        assertEquals(Set.of(RelativeFace.BACK), s.powerFaces());
        assertEquals(MachineSpec.Kind.PRODUCER, s.kind());
    }

    @Test
    void ratedWatts() {
        assertError(MachineSpec.consumer(Tier.V200), "ratedWatts is required");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(0), "ratedWatts must be positive and finite, got 0.0");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(-5), "ratedWatts must be positive and finite");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(Double.POSITIVE_INFINITY), "ratedWatts must be positive and finite");
    }

    @Test
    void idleWatts() {
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).idleWatts(100),
            "idleWatts (100.0) must be less than ratedWatts (100.0)");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).idleWatts(-1), "idleWatts must be 0 or more");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).idleWatts(Double.NaN), "idleWatts must be 0 or more");
        assertError(MachineSpec.producer(Tier.V200).ratedWatts(100).idleWatts(1), "a producer has no idle draw");
    }

    @Test
    void limits() {
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).minVoltageRatio(1), "minVoltageRatio must be between 0 and 1");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).minVoltageRatio(0), "minVoltageRatio must be between 0 and 1");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).maxWatts(100), "or the machine trips at its rating");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).maxWatts(Double.NaN), "maxWatts must be finite");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).tripCooldown(0.1), "tripCooldown must be at least 0.5 s");
        assertEquals(150, MachineSpec.consumer(Tier.V200).ratedWatts(100).maxWatts(150).build().maxWatts());
    }

    @Test
    void thermal() {
        MachineSpec s = MachineSpec.consumer(Tier.V200).ratedWatts(100).thermal(1000, 2, 80).build();
        assertEquals(80, s.thermal().orElseThrow().tripCelsius());
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).thermal(0, 2, 80), "heatCapacityJperK must be positive");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).thermal(10, -2, 80), "lossWperK must be positive");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).thermal(10, 2, 25), "tripCelsius must be above 30.0 C");
    }

    @Test
    void signals() {
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100)
            .signalIn("a", RelativeFace.UP).signalOut("a", RelativeFace.DOWN), "signal name 'a' is used twice");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).signalIn(" ", RelativeFace.UP), "a signal needs a name");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).signalIn("a", RelativeFace.UP, 5),
            "signal 'a' default must be 0..1 (a signal level, not volts), got 5.0");
    }

    @Test
    void faceRoles() {
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100)
                .signalIn("a", RelativeFace.UP).signalOut("b", RelativeFace.UP),
            "face UP is given to both signal 'a' and signal 'b'");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100)
            .power(RelativeFace.BACK).bus(RelativeFace.BACK), "face BACK is given to both the bus and power");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).power(), "power needs at least one face");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).power(RelativeFace.UP).power(RelativeFace.DOWN),
            "power faces are given twice");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).bus(RelativeFace.UP).bus(RelativeFace.DOWN),
            "bus is given twice");
        MachineSpec.Builder full = MachineSpec.consumer(Tier.V200).ratedWatts(100);
        int i = 0;
        for (RelativeFace f : RelativeFace.values()) full.signalIn("s" + i++, f);
        assertError(full, "no face is left for power");
    }

    /** All problems in one message, so a modder fixes them in one go. */
    @Test
    void listsEveryProblem() {
        String m = error(MachineSpec.consumer(Tier.V200).idleWatts(-1).tripCooldown(0));
        assertTrue(m.startsWith("Invalid consumer spec: "), m);
        assertTrue(m.contains("ratedWatts is required") && m.contains("idleWatts") && m.contains("tripCooldown"), m);
    }

    @Test
    void tripCause() {
        assertEquals("overvoltage: 270.0 V over 260.0 V",
            new TripCause(MachineState.OVERVOLTAGE_TRIPPED, 270, 260, false).toString());
        assertThrows(IllegalArgumentException.class, () -> new TripCause(MachineState.RUNNING, 0, 0, false));
    }
}
