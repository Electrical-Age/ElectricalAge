package mods.eln.introspect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

/** One value in a report. Each kind writes itself as a one-key JSON object, e.g. {"num":3.2,"unit":"M"}. */
public sealed interface Value permits Num, Section, Value.Bool, Value.Int, Value.Choice, Value.Text, Value.Ref, Value.Many {
    JsonElement json();

    record Bool(boolean v) implements Value {
        public JsonElement json() {
            JsonObject o = new JsonObject();
            o.addProperty("bool", v);
            return o;
        }
    }

    record Int(long v) implements Value {
        public JsonElement json() {
            JsonObject o = new JsonObject();
            o.addProperty("int", v);
            return o;
        }
    }

    /** One of a fixed set (lower-case names); tests compare the name. */
    record Choice(String v, List<String> of) implements Value {
        public Choice {
            of = List.copyOf(of);
            if (!of.contains(v)) throw new IllegalArgumentException(v + " is not one of " + of);
        }

        public JsonElement json() {
            JsonObject o = new JsonObject();
            o.addProperty("enum", v);
            JsonArray a = new JsonArray();
            of.forEach(a::add);
            o.add("of", a);
            return o;
        }
    }

    /** Words for people. Tests must not assert on it, and it has no place under sync. */
    record Text(String v) implements Value {
        public JsonElement json() {
            JsonObject o = new JsonObject();
            o.addProperty("text", v);
            return o;
        }
    }

    /** A block position ("x,y,z") or an entity UUID. */
    record Ref(String v) implements Value {
        public JsonElement json() {
            JsonObject o = new JsonObject();
            o.addProperty("ref", v);
            return o;
        }
    }

    record Many(List<Value> v) implements Value {
        public Many {
            v = List.copyOf(v);
        }

        public JsonElement json() {
            JsonObject o = new JsonObject();
            JsonArray a = new JsonArray();
            v.forEach(x -> a.add(Value.nested(x)));
            o.add("list", a);
            return o;
        }
    }

    /** A value inside a section or list: a nested section is wrapped as {"section":{...}}, so it can't be read as a value. */
    static JsonElement nested(Value v) {
        if (!(v instanceof Section)) return v.json();
        JsonObject o = new JsonObject();
        o.add("section", v.json());
        return o;
    }
}
