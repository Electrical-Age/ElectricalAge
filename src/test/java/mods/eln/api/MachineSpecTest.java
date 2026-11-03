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
        assertEquals(3, s.tripCooldownSeconds());
        assertFalse(s.burnsOnOvervoltage());
        assertTrue(s.burnHandler().isEmpty());
        assertTrue(s.thermal().isEmpty());
        assertEquals(EnumSet.allOf(RelativeFace.class), s.powerFaces());
        assertTrue(s.busFace().isEmpty());
    }

    @Test
    void brownout() {
        assertEquals(Brownout.STOP, MachineSpec.consumer(Tier.V200).ratedWatts(100).build().brownout());
        MachineSpec slow = MachineSpec.consumer(Tier.V200).ratedWatts(100).onBrownout(Brownout.SLOW).build();
        assertEquals(Brownout.SLOW, slow.brownout());
        assertEquals(0, slow.minVoltageRatio());
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).onBrownout(Brownout.SLOW).minVoltageRatio(0.5),
            "minVoltageRatio is the floor of Brownout.STOP; Brownout.SLOW has none");
        assertError(MachineSpec.producer(Tier.V200).ratedWatts(100).onBrownout(Brownout.STOP), "onBrownout is for consumers");
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
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(Double.NaN), "ratedWatts must be positive and finite, got NaN");
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
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).minVoltageRatio(0.96), "minVoltageRatio must be above 0 and at most 0.95");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).minVoltageRatio(0), "minVoltageRatio must be above 0 and at most 0.95");
        assertEquals(0.95, MachineSpec.consumer(Tier.V200).ratedWatts(100).minVoltageRatio(0.95).build().minVoltageRatio());
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).tripCooldownSeconds(0.1), "tripCooldownSeconds must be at least 0.5 s");
    }

    @Test
    void thermal() {
        MachineSpec s = MachineSpec.consumer(Tier.V200).ratedWatts(100)
            .heatCapacityJPerK(1000).heatLossWPerK(2).overheatRiseKelvin(60).build();
        assertEquals(new MachineSpec.Thermal(1000, 2, 60, MachineSpec.OVERHEAT_RESET_KELVIN), s.thermal().orElseThrow());
        assertEquals(30, MachineSpec.consumer(Tier.V200).ratedWatts(100).heatCapacityJPerK(1000).heatLossWPerK(2).overheatRiseKelvin(60)
            .overheatResetKelvin(30).build().thermal().orElseThrow().overheatResetKelvin());
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).heatCapacityJPerK(10).heatLossWPerK(2).overheatRiseKelvin(60)
            .overheatResetKelvin(0), "overheatResetKelvin must be positive and finite, got 0.0");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).heatCapacityJPerK(10).heatLossWPerK(2).overheatRiseKelvin(25)
            .overheatResetKelvin(30), "overheatRiseKelvin must be more than the 30.0 K reset band");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).overheatResetKelvin(5), "overheatResetKelvin is part of the heat model");
        MachineSpec.Builder ok = MachineSpec.consumer(Tier.V200).ratedWatts(100);
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).heatCapacityJPerK(0).heatLossWPerK(2).overheatRiseKelvin(60),
            "heatCapacityJPerK must be positive and finite, got 0.0");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).heatCapacityJPerK(10).heatLossWPerK(-2).overheatRiseKelvin(60),
            "heatLossWPerK must be positive");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).heatCapacityJPerK(10).heatLossWPerK(2).overheatRiseKelvin(5),
            "overheatRiseKelvin must be more than the 10.0 K reset band (it is K over the ambient");
        // the three go together
        String m = error(ok.heatCapacityJPerK(10));
        assertTrue(m.contains("heatLossWPerK must be positive and finite (not set)") && m.contains("overheatRiseKelvin must be more than"), m);
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
        // cable positions: two signals on one side face, at different positions
        MachineSpec shared = MachineSpec.consumer(Tier.V200).ratedWatts(100)
            .signalIn("a", RelativeFace.LEFT, CablePosition.FLOOR, 0).signalOut("b", RelativeFace.LEFT, CablePosition.CEILING).build();
        assertEquals(EnumSet.complementOf(EnumSet.of(RelativeFace.LEFT)), shared.powerFaces());
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100)
                .signalIn("a", RelativeFace.LEFT, CablePosition.FLOOR, 0).signalOut("b", RelativeFace.LEFT, CablePosition.FLOOR),
            "the FLOOR cable of face LEFT is given to both signal 'a' and signal 'b'");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100)
                .signalIn("a", RelativeFace.LEFT, CablePosition.FLOOR, 0).signalOut("b", RelativeFace.LEFT),
            "face LEFT is given to both signal 'a' and signal 'b'");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).power(RelativeFace.BACK)
                .signalIn("a", RelativeFace.BACK, CablePosition.FLOOR, 0),
            "face BACK is given to both signal 'a' and power");
        assertError(MachineSpec.consumer(Tier.V200).ratedWatts(100).signalIn("a", RelativeFace.UP, CablePosition.CEILING, 0),
            "signal 'a': CEILING is a cable position on a side face (FRONT, BACK, LEFT, RIGHT), not UP");
        MachineSpec.Builder full = MachineSpec.consumer(Tier.V200).ratedWatts(100);
        int i = 0;
        for (RelativeFace f : RelativeFace.values()) full.signalIn("s" + i++, f);
        assertError(full, "no face is left for power");
    }

    /** All problems in one message, so a modder fixes them in one go. */
    @Test
    void listsEveryProblem() {
        String m = error(MachineSpec.consumer(Tier.V200).idleWatts(-1).tripCooldownSeconds(0));
        assertTrue(m.startsWith("Invalid consumer spec: "), m);
        assertTrue(m.contains("ratedWatts is required") && m.contains("idleWatts") && m.contains("tripCooldownSeconds"), m);
    }

    @Test
    void tripCause() {
        assertEquals("overvoltage: 270.0 V over 260.0 V",
            new TripCause(MachineState.OVERVOLTAGE_TRIPPED, 270, 260, false).toString());
        assertThrows(IllegalArgumentException.class, () -> new TripCause(MachineState.RUNNING, 0, 0, false));
    }

    /**
     * A burn always has a handler: the builder has no burn without one (so a machine at another mod's block entity
     * can't burn with nothing to say what that does), and a null handler throws; given once only.
     */
    @Test
    void burnHandler() throws NoSuchMethodException {
        BurnHandler h = burn -> {
        };
        MachineSpec with = MachineSpec.consumer(Tier.V50).ratedWatts(100).burnsOnOvervoltage(h).build();
        assertTrue(with.burnsOnOvervoltage());
        assertEquals(h, with.burnHandler().orElseThrow());
        MachineSpec none = MachineSpec.consumer(Tier.V50).ratedWatts(100).build();
        assertFalse(none.burnsOnOvervoltage());
        assertTrue(none.burnHandler().isEmpty());
        for (var m : MachineSpec.Builder.class.getMethods())
            assertFalse(m.getName().equals("burnsOnOvervoltage") && m.getParameterCount() == 0, "a burn without a handler: " + m);
        assertEquals(MachineSpec.Builder.class, MachineSpec.Builder.class.getMethod("burnsOnOvervoltage", BurnHandler.class).getReturnType());
        assertThrows(NullPointerException.class, () -> MachineSpec.consumer(Tier.V50).burnsOnOvervoltage(null));
        assertError(MachineSpec.consumer(Tier.V50).ratedWatts(100).burnsOnOvervoltage(h).burnsOnOvervoltage(h),
            "burnsOnOvervoltage is given twice");
    }
}
