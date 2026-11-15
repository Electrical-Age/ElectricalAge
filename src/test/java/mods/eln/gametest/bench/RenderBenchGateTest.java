package mods.eln.gametest.bench;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderBenchGateTest {
    private static JsonObject device(String name, long vertices, long renderer, long recordNs) {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("vertices", vertices);
        o.addProperty("rendererVertices", renderer);
        o.addProperty("recordNs", recordNs);
        return o;
    }

    private static JsonArray of(JsonObject... devices) {
        JsonArray a = new JsonArray();
        for (JsonObject d : devices) a.add(d);
        return a;
    }

    @Test
    void timesDoNotGate() {
        assertEquals(List.of(), RenderBenchGate.differences(of(device("A", 100, 4, 1000)), of(device("A", 100, 4, 9000))));
    }

    @Test
    void exactFieldsGate() {
        assertEquals(List.of("A: vertices 100 -> 104, rendererVertices 4 -> 8"),
            RenderBenchGate.differences(of(device("A", 100, 4, 1000)), of(device("A", 104, 8, 1000))));
    }

    @Test
    void newAndGoneDevicesAreNamed() {
        assertEquals(List.of("C: not in the base", "A: gone"),
            RenderBenchGate.differences(of(device("A", 1, 0, 0), device("B", 2, 0, 0)), of(device("B", 2, 0, 0), device("C", 3, 0, 0))));
    }

    @Test
    void devicesThatShareANameAreComparedInOrder() {
        JsonArray base = of(device("S", 108, 12, 0), device("S", 424, 8, 0));
        assertEquals(List.of(), RenderBenchGate.differences(base, of(device("S", 108, 12, 0), device("S", 424, 8, 0))));
        assertEquals(List.of("S #2: vertices 424 -> 108"), RenderBenchGate.differences(base, of(device("S", 108, 12, 0), device("S", 108, 8, 0))));
    }
}
