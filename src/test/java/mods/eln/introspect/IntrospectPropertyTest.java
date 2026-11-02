package mods.eln.introspect;

import com.google.gson.JsonObject;
import net.jqwik.api.*;
import net.jqwik.api.constraints.DoubleRange;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Properties of names and the sync comparison. */
class IntrospectPropertyTest {
    @Property
    void snakeGivesAValidKeyOrNothing(@ForAll String any) {
        String k = Describe.snake(any);
        assertTrue(k.isEmpty() || Keys.valid(k), () -> "'" + any + "' -> '" + k + "'");
    }

    @Property
    void aHalfAgreesWithItself(@ForAll @DoubleRange(min = -1e6, max = 1e6) double pos, @ForAll @DoubleRange(min = 0, max = 10) double tol,
                               @ForAll boolean moving) {
        JsonObject s = half(Report.Half.SERVER, pos, tol, moving).toJson();
        assertEquals(List.of(), SyncDiff.of(s, s));
    }

    @Property
    void numbersDifferOnlyBeyondTheTolerance(@ForAll @DoubleRange(min = -1e3, max = 1e3) double pos,
                                             @ForAll @DoubleRange(min = 0, max = 1) double tol,
                                             @ForAll @DoubleRange(min = -2, max = 2) double off) {
        JsonObject s = half(Report.Half.SERVER, pos, tol, true).toJson();
        JsonObject c = half(Report.Half.CLIENT, pos + off, tol, true).toJson();
        List<SyncDiff.Difference> d = SyncDiff.of(s, c);
        // compare as SyncDiff does, on the values it reads back
        double a = s.getAsJsonObject("sync").getAsJsonObject("position").get("num").getAsDouble();
        double b = c.getAsJsonObject("sync").getAsJsonObject("position").get("num").getAsDouble();
        assertEquals(Math.abs(a - b) > tol, d.stream().anyMatch(x -> x.key().equals("position")), d::toString);
        assertTrue(d.stream().allMatch(x -> x.key().equals("position")), d::toString);
    }

    @Property
    void aKeyOnOneSideIsADifference(@ForAll boolean serverOnly) {
        Report s = half(Report.Half.SERVER, 1, 0.1, true), c = half(Report.Half.CLIENT, 1, 0.1, true);
        (serverOnly ? s : c).sync().section("segment").num("extra", 1, Unit.TICK).tol(0);
        List<SyncDiff.Difference> d = SyncDiff.of(s.toJson(), c.toJson());
        assertEquals(1, d.size(), d::toString);
        assertEquals("segment.extra", d.get(0).key());
    }

    @Example
    void otherValuesMustBeEqual() {
        JsonObject s = half(Report.Half.SERVER, 1, 0.1, true).toJson(), c = half(Report.Half.CLIENT, 1, 0.1, false).toJson();
        List<SyncDiff.Difference> d = SyncDiff.of(s, c);
        assertEquals(1, d.size(), d::toString);
        assertEquals("moving", d.get(0).key());
    }

    private static Report half(Report.Half h, double pos, double tol, boolean moving) {
        Report r = new Report(h);
        r.id().text(Keys.KIND, "eln:test");
        r.sync().num(Keys.POSITION, pos, Unit.M).tol(tol);
        r.sync().bool("moving", moving);
        r.sync().section("segment").num("t0", 100, Unit.TICK).tol(0);
        return r;
    }
}
