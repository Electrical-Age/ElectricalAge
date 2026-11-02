package mods.eln.introspect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An ordered set of named values. Keys follow {@link Keys#valid} and are unique: adding a bad or repeated key throws,
 * so a mistake fails the first test that describes the device rather than producing a report nobody can rely on.
 */
public final class Section implements Value {
    private final Map<String, Value> entries = new LinkedHashMap<>();

    public Num num(String key, double v, Unit unit) {
        Num n = new Num(v, unit);
        put(key, n);
        return n;
    }

    public void bool(String key, boolean v) {
        put(key, new Value.Bool(v));
    }

    public void integer(String key, long v) {
        put(key, new Value.Int(v));
    }

    /** The value of an enum; the set is all its constants, lower-cased. */
    public <E extends Enum<E>> void choice(String key, E v) {
        List<String> of = new java.util.ArrayList<>();
        for (E e : v.getDeclaringClass().getEnumConstants()) of.add(e.name().toLowerCase(Locale.ROOT));
        put(key, new Value.Choice(v.name().toLowerCase(Locale.ROOT), of));
    }

    /** One of a fixed set of names (when there is no enum to hand). */
    public void choice(String key, String v, List<String> of) {
        put(key, new Value.Choice(v, of));
    }

    public void text(String key, String v) {
        put(key, new Value.Text(v == null ? "" : v));
    }

    public void ref(String key, String v) {
        put(key, new Value.Ref(v));
    }

    public void list(String key, List<Value> v) {
        put(key, new Value.Many(v));
    }

    /** A nested section (made on first use, the same one after). */
    public Section section(String key) {
        if (entries.get(key) instanceof Section s) return s;
        Section s = new Section();
        put(key, s);
        return s;
    }

    public Value get(String key) {
        return entries.get(key);
    }

    public Map<String, Value> entries() {
        return Collections.unmodifiableMap(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    private void put(String key, Value v) {
        if (!Keys.valid(key)) throw new IllegalArgumentException("bad key '" + key + "': lower-case letters, digits and _");
        if (entries.containsKey(key)) throw new IllegalArgumentException("key '" + key + "' given twice");
        entries.put(key, v);
    }

    public JsonElement json() {
        JsonObject o = new JsonObject();
        entries.forEach((k, v) -> o.add(k, Value.nested(v)));
        return o;
    }
}
