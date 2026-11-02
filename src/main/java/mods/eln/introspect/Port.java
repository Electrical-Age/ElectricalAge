package mods.eln.introspect;

import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * A device's connection: a name (a key), its kind, the side it is on, whether something is connected (null: not known,
 * left out of the JSON) and its value (or null).
 */
public record Port(String name, Kind kind, String side, Boolean connected, Num value) {
    public enum Kind {POWER, SIGNAL, BUS, THERMAL, PIPE, SHAFT}

    public Port {
        if (!Keys.valid(name)) throw new IllegalArgumentException("bad port name '" + name + "'");
        if (kind == null) throw new IllegalArgumentException("port " + name + " needs a kind");
        side = side == null ? "" : side;
    }

    public JsonObject json() {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("kind", kind.name().toLowerCase(Locale.ROOT));
        o.addProperty("side", side);
        if (connected != null) o.addProperty("connected", connected);
        if (value != null) o.add("value", value.json());
        return o;
    }
}
