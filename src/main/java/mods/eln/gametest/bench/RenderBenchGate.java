package mods.eln.gametest.bench;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The render benchmark's gate: the fields of a device that must not change from one build to the next unless its
 * draw code or its baking did (vertex and call counts), compared with an earlier result. Times never gate: they are
 * compared by tools/bench/render-bench-compare.py with a wide band.
 */
public final class RenderBenchGate {
    private RenderBenchGate() {
    }

    /** The fields that are the same in every run of one build. */
    public static final String[] EXACT = {"vertices", "rendererVertices", "chunkVertices", "meshVertices", "settledCalls"};

    /** name -> device; two devices may share a name (two "Signal Switch"): the second is "Signal Switch #2". */
    static Map<String, JsonObject> keyed(JsonArray devices) {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        for (var e : devices) {
            JsonObject d = e.getAsJsonObject();
            String name = d.get("name").getAsString(), key = name;
            for (int n = 2; out.containsKey(key); n++) key = name + " #" + n;
            out.put(key, d);
        }
        return out;
    }

    /** One line per device that differs from the base in an exact field, is new, or is gone; empty = the same. */
    public static List<String> differences(JsonArray base, JsonArray now) {
        List<String> out = new ArrayList<>();
        Map<String, JsonObject> a = keyed(base), b = keyed(now);
        for (var e : b.entrySet()) {
            JsonObject was = a.get(e.getKey());
            if (was == null) {
                out.add(e.getKey() + ": not in the base");
                continue;
            }
            StringBuilder s = new StringBuilder();
            for (String k : EXACT) {
                if (!was.has(k) || !e.getValue().has(k)) continue;
                long x = was.get(k).getAsLong(), y = e.getValue().get(k).getAsLong();
                if (x != y) s.append(s.length() == 0 ? "" : ", ").append(k).append(' ').append(x).append(" -> ").append(y);
            }
            if (s.length() > 0) out.add(e.getKey() + ": " + s);
        }
        for (String k : a.keySet()) if (!b.containsKey(k)) out.add(k + ": gone");
        return out;
    }
}
