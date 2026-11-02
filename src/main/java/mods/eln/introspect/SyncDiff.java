package mods.eln.introspect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Compares the sync sections of a server half and a client half (as JSON, so it works on reports that crossed the
 * network). A number differs when |server - client| is over the server's tolerance; anything else when it isn't equal;
 * a key on one side only is a difference too. Works on nested sections, by path.
 */
public final class SyncDiff {
    private SyncDiff() {
    }

    public record Difference(String key, String server, String client, String why) {
    }

    public static List<Difference> of(JsonObject server, JsonObject client) {
        List<Difference> out = new ArrayList<>();
        compare("", obj(server, "sync"), obj(client, "sync"), out);
        return out;
    }

    private static JsonObject obj(JsonObject o, String key) {
        return o != null && o.get(key) instanceof JsonObject j ? j : new JsonObject();
    }

    private static void compare(String path, JsonObject s, JsonObject c, List<Difference> out) {
        for (String k : s.keySet()) {
            JsonElement sv = s.get(k), cv = c.get(k);
            String key = path + k;
            if (cv == null) {
                out.add(new Difference(key, String.valueOf(sv), "", "missing on the client"));
                continue;
            }
            if (sv instanceof JsonObject so && so.get("section") instanceof JsonObject ss) {
                if (cv instanceof JsonObject co && co.get("section") instanceof JsonObject cs) compare(key + ".", ss, cs, out);
                else out.add(new Difference(key, String.valueOf(sv), String.valueOf(cv), "a section on the server only"));
                continue;
            }
            if (sv instanceof JsonObject so && so.has("num") && so.get("num").getAsJsonPrimitive().isNumber()
                && cv instanceof JsonObject co && co.has("num") && co.get("num").getAsJsonPrimitive().isNumber()) {
                double a = so.get("num").getAsDouble(), b = co.get("num").getAsDouble();
                double tol = so.has("tol") ? so.get("tol").getAsDouble() : 0;
                if (!(Math.abs(a - b) <= tol))
                    out.add(new Difference(key, Double.toString(a), Double.toString(b), "off by " + Math.abs(a - b) + " (tol " + tol + ")"));
                continue;
            }
            if (!sv.equals(cv)) out.add(new Difference(key, String.valueOf(sv), String.valueOf(cv), "not equal"));
        }
        for (String k : c.keySet())
            if (!s.has(k)) out.add(new Difference(path + k, "", String.valueOf(c.get(k)), "missing on the server"));
    }
}
