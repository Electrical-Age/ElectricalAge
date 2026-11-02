package mods.eln.introspect;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Map;

/** A report for people: numbers with their unit symbol ("197.3 V"), empty sections left out. */
final class Text {
    private Text() {
    }

    static JsonObject readable(Report r) {
        JsonObject o = new JsonObject();
        put(o, "id", r.id());
        put(o, "spec", r.spec());
        put(o, "state", r.state());
        put(o, "sync", r.sync());
        if (!r.faults().isEmpty()) {
            JsonArray f = new JsonArray();
            r.faults().forEach(x -> f.add(x.code() + ": " + x.text()));
            o.add("faults", f);
        }
        if (!r.ports().isEmpty()) {
            JsonObject p = new JsonObject();
            for (Port x : r.ports())
                p.addProperty(x.name(), x.kind().name().toLowerCase(java.util.Locale.ROOT) + (x.value() == null ? "" : " " + show(x.value()))
                    + (x.connected() == null ? "" : x.connected() ? ", connected" : ", free"));
            o.add("ports", p);
        }
        put(o, "electrical", r.electrical());
        put(o, "thermal", r.thermal());
        put(o, "legacy", r.legacy());
        if (r.half == Report.Half.CLIENT) put(o, "client", r.client());
        return o;
    }

    private static void put(JsonObject o, String key, Section s) {
        if (!s.isEmpty()) o.add(key, section(s));
    }

    private static JsonObject section(Section s) {
        JsonObject o = new JsonObject();
        for (Map.Entry<String, Value> e : s.entries().entrySet()) {
            Value v = e.getValue();
            if (v instanceof Section sub) o.add(e.getKey(), section(sub));
            else if (v instanceof Num n) o.addProperty(e.getKey(), show(n));
            else if (v instanceof Value.Bool b) o.addProperty(e.getKey(), b.v());
            else if (v instanceof Value.Int i) o.addProperty(e.getKey(), i.v());
            else if (v instanceof Value.Choice c) o.addProperty(e.getKey(), c.v());
            else if (v instanceof Value.Text t) o.addProperty(e.getKey(), t.v());
            else if (v instanceof Value.Ref ref) o.addProperty(e.getKey(), ref.v());
            else if (v instanceof Value.Many m) {
                JsonArray a = new JsonArray();
                for (Value x : m.v()) a.add(x instanceof Value.Text t ? new com.google.gson.JsonPrimitive(t.v()) : x.json());
                o.add(e.getKey(), a);
            }
        }
        return o;
    }

    static String show(Num n) {
        String v = Math.abs(n.v) >= 1e4 || n.v == Math.rint(n.v) ? String.valueOf(n.v == Math.rint(n.v) && Math.abs(n.v) < 1e15 ? (long) n.v : n.v)
            : String.format(java.util.Locale.ROOT, "%.4g", n.v);
        return n.unit.symbol.isEmpty() ? v : v + " " + n.unit.symbol;
    }
}
