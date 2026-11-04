package mods.eln.introspect;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The report's rules: keys, units, the JSON shape and the checks GameTests rely on. */
class ReportTest {
    enum Status {IDLE, IN_POSITION, MOVING, FAULT}

    @Test
    void badOrRepeatedKeysThrow() {
        Section s = new Section();
        assertThrows(IllegalArgumentException.class, () -> s.num("Position", 1, Unit.M));
        assertThrows(IllegalArgumentException.class, () -> s.num("position_m ", 1, Unit.M));
        assertThrows(IllegalArgumentException.class, () -> s.bool("", true));
        s.num("position", 1, Unit.M);
        assertThrows(IllegalArgumentException.class, () -> s.num("position", 2, Unit.M));
        assertThrows(IllegalArgumentException.class, () -> s.num("speed", 1, null));
        Report r = new Report(Report.Half.SERVER);
        assertThrows(IllegalArgumentException.class, () -> r.fault("Too hot", "x"));
        r.port("power", Port.Kind.POWER, "back", true, null);
        assertThrows(IllegalArgumentException.class, () -> r.port("power", Port.Kind.SIGNAL, "left", null, null));
    }

    @Test
    void clientSectionOnlyOnAClientHalf() {
        assertThrows(IllegalStateException.class, () -> new Report(Report.Half.SERVER).client());
        Report c = new Report(Report.Half.CLIENT);
        c.client().bool("carried", true);
        assertTrue(c.toJson().has("client"));
        assertFalse(new Report(Report.Half.SERVER).toJson().has("client"));
    }

    @Test
    void jsonShape() {
        Report r = new Report(Report.Half.SERVER);
        r.id().text(Keys.KIND, "eln:linear_servo_drive");
        r.spec().num(Keys.NOMINAL_VOLTAGE, 200, Unit.V);
        r.state().choice(Keys.STATUS, Status.IN_POSITION);
        r.sync().num(Keys.POSITION, 3.25, Unit.M).tol(0.05);
        r.sync().section("segment").num("t0", 12010, Unit.TICK).tol(0);
        r.fault("obstructed", "Something is in the way");
        r.port("setpoint_left", Port.Kind.SIGNAL, "left", null, Report.value(18.75, Unit.V));
        JsonObject o = r.toJson();
        assertEquals(List.of("half", "id", "spec", "state", "sync", "faults", "ports", "electrical", "thermal", "legacy"),
            List.copyOf(o.keySet()));
        assertEquals("{\"num\":200.0,\"unit\":\"V\"}", o.getAsJsonObject("spec").get("nominal_voltage").toString());
        JsonObject status = o.getAsJsonObject("state").getAsJsonObject("status");
        assertEquals("in_position", status.get("enum").getAsString());
        assertEquals(4, status.getAsJsonArray("of").size());
        assertEquals(0.05, o.getAsJsonObject("sync").getAsJsonObject("position").get("tol").getAsDouble());
        assertTrue(o.getAsJsonObject("sync").getAsJsonObject("segment").has("section"), "a nested section is wrapped");
        assertEquals("obstructed", o.getAsJsonArray("faults").get(0).getAsJsonObject().get("code").getAsString());
        JsonObject port = o.getAsJsonArray("ports").get(0).getAsJsonObject();
        assertFalse(port.has("connected"), "unknown connection state is left out");
        assertEquals("signal", port.get("kind").getAsString());
        assertTrue(r.problems().isEmpty(), r.problems().toString());
    }

    @Test
    void problems() {
        Report r = new Report(Report.Half.SERVER);
        assertTrue(r.problems().contains("id.kind missing"));
        r.id().text(Keys.KIND, "eln:x");
        r.state().num(Keys.SPEED, Double.NaN, Unit.M_PER_S);
        r.thermal().section("core").num(Keys.T, Double.POSITIVE_INFINITY, Unit.C);
        r.sync().num(Keys.POSITION, 1, Unit.M);
        r.sync().text("mode", "up");
        r.sync().section("segment").num("t0", 1, Unit.TICK);
        r.port("power_back", Port.Kind.POWER, "back", true, Report.value(Double.NaN, Unit.V));
        List<String> p = r.problems();
        assertTrue(p.contains("state.speed is NaN"), p.toString());
        assertTrue(p.contains("thermal.core.t is Infinity"), p.toString());
        assertTrue(p.contains("sync.position has no tolerance"), p.toString());
        assertTrue(p.contains("sync.segment.t0 has no tolerance"), p.toString());
        assertTrue(p.stream().anyMatch(x -> x.startsWith("sync.mode is text")), p.toString());
        assertTrue(p.contains("ports.power_back is NaN"), p.toString());
        // a non-finite number still makes valid JSON
        assertEquals("NaN", r.toJson().getAsJsonObject("state").getAsJsonObject("speed").get("num").getAsString());
    }

    @Test
    void snakeNames() {
        assertEquals("in_position", Describe.snake("inPosition"));
        assertEquals("fault_out", Describe.snake("faultOut"));
        assertEquals("electrical_load_1", Describe.snake("ElectricalLoad 1"));
        assertEquals("", Describe.snake("1st"));
        assertEquals("", Describe.snake(""));
        assertEquals("", Describe.snake(null));
    }
}
