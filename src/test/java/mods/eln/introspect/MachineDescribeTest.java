package mods.eln.introspect;

import com.google.gson.JsonObject;
import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.RelativeFace;
import mods.eln.api.Tier;
import mods.eln.api.TripCause;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** A machine on the API in a report: its spec and state under "machine", a trip as a fault, ports from the spec. */
class MachineDescribeTest {
    static final MachineSpec SPEC = MachineSpec.consumer(50).ratedWatts(100)
        .power(RelativeFace.FRONT, RelativeFace.BACK)
        .signalIn("enable", RelativeFace.LEFT, 1).signalOut("busy", RelativeFace.UP).bus(RelativeFace.RIGHT).build();

    static MachineView view(MachineState state, Optional<TripCause> trip) {
        return new MachineView() {
            public MachineSpec spec() { return SPEC; }
            public MachineState state() { return state; }
            public Optional<TripCause> lastTrip() { return trip; }
            public double volts() { return 49.85; }
            public double voltageRatio() { return state == MachineState.RUNNING ? 0.997 : 0; }
            public double amps() { return 2; }
            public double watts() { return 99.7; }
            public double bufferJoules() { return 3; }
            public double bufferCapacityJoules() { return 10; }
            public double joulesDelivered() { return 120; }
            public double joulesTaken() { return 117; }
            public double joulesGiven() { return 0; }
            public double joulesLost() { return 0; }
            public double celsius() { return 21; }
        };
    }

    @Test
    void aRunningMachine() {
        Report r = new Report(Report.Half.SERVER);
        r.id().text(Keys.KIND, "eln:api_test_machine");
        MachineDescribe.into(r, view(MachineState.RUNNING, Optional.empty()));
        MachineDescribe.ports(r, SPEC);
        MachineDescribe.sync(r, view(MachineState.RUNNING, Optional.empty()));
        assertEquals(java.util.List.of(), r.problems());
        JsonObject o = r.toJson();
        JsonObject spec = o.getAsJsonObject("spec").getAsJsonObject("machine").getAsJsonObject("section");
        assertEquals(50.0, spec.getAsJsonObject("nominal_voltage").get("num").getAsDouble());
        assertEquals("consumer", spec.getAsJsonObject("kind").get("enum").getAsString());
        JsonObject state = o.getAsJsonObject("state").getAsJsonObject("machine").getAsJsonObject("section");
        assertEquals("running", state.getAsJsonObject("machine_state").get("enum").getAsString());
        assertEquals(49.85, state.getAsJsonObject("voltage").get("num").getAsDouble());
        assertTrue(r.faults().isEmpty());
        assertEquals(java.util.List.of("power", "in_enable", "out_busy", "bus"), r.ports().stream().map(Port::name).toList());
        assertEquals("front back", r.ports().get(0).side());
        assertEquals("running", ((Value.Choice) r.sync().get("machine_state")).v());
    }

    @Test
    void aTrippedMachineHasAFault() {
        Report r = new Report(Report.Half.SERVER);
        r.id().text(Keys.KIND, "eln:x");
        TripCause t = new TripCause(MachineState.OVERVOLTAGE_TRIPPED, 210, 62.5, false);
        MachineDescribe.into(r, view(MachineState.OVERVOLTAGE_TRIPPED, Optional.of(t)));
        assertEquals("overvoltage_tripped", r.faults().get(0).code());
        assertEquals(java.util.List.of(), r.problems());
        JsonObject trip = r.toJson().getAsJsonObject("state").getAsJsonObject("machine").getAsJsonObject("section")
            .getAsJsonObject("last_trip").getAsJsonObject("section");
        assertEquals(210.0, trip.getAsJsonObject("measured").get("num").getAsDouble());
        assertEquals("V", trip.getAsJsonObject("measured").get("unit").getAsString());
    }

    @Test
    void aDeviceThatDescribesItselfToo() {
        // the machine's keys sit under "machine", so a device's own spec/state keys can't collide with them
        Report r = new Report(Report.Half.SERVER);
        r.id().text(Keys.KIND, "eln:x");
        r.state().num(Keys.VOLTAGE, 1, Unit.V);
        r.spec().num(Keys.RATED_POWER, 1, Unit.W);
        assertDoesNotThrow(() -> MachineDescribe.into(r, view(MachineState.RUNNING, Optional.empty())));
    }
}
