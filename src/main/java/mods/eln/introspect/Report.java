package mods.eln.introspect;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What one device, entity or element says about itself. The sections are fixed and always written in this order:
 * <ul>
 * <li>{@code id}: what and where it is (filled by {@link Describe});</li>
 * <li>{@code spec}: its ratings, from the descriptor alone; the same for every device of a kind and config;</li>
 * <li>{@code state}: live values the device chooses;</li>
 * <li>{@code sync}: values the server and each client must agree on, each number with a tolerance;</li>
 * <li>{@code faults}: why it isn't working, as codes (empty: healthy);</li>
 * <li>{@code ports}: its connections;</li>
 * <li>{@code electrical}, {@code thermal}: each named load's voltage and current, temperature and power;</li>
 * <li>{@code legacy}: its Jade lines as text, until the device describes itself;</li>
 * <li>{@code client}: on a client's half only, what the render or entity believes besides sync.</li>
 * </ul>
 * The server half and a client half of the same thing are two Reports; {@link SyncDiff} compares their sync sections.
 */
public final class Report {
    public enum Half {SERVER, CLIENT}

    public final Half half;
    private final Section id = new Section(), spec = new Section(), state = new Section(), sync = new Section(),
        electrical = new Section(), thermal = new Section(), legacy = new Section(), client = new Section();
    private final List<Fault> faults = new ArrayList<>();
    private final List<Port> ports = new ArrayList<>();

    public Report(Half half) {
        this.half = half;
    }

    public Section id() {
        return id;
    }

    public Section spec() {
        return spec;
    }

    public Section state() {
        return state;
    }

    public Section sync() {
        return sync;
    }

    public Section electrical() {
        return electrical;
    }

    public Section thermal() {
        return thermal;
    }

    public Section legacy() {
        return legacy;
    }

    /** What a client believes besides sync. Only on a client's half. */
    public Section client() {
        if (half != Half.CLIENT) throw new IllegalStateException("the client section is on a client's half only");
        return client;
    }

    /** A reason it isn't working: a stable code (a key, e.g. "obstructed") and words for people. */
    public void fault(String code, String text) {
        if (!Keys.valid(code)) throw new IllegalArgumentException("bad fault code '" + code + "'");
        faults.add(new Fault(code, text == null ? "" : text));
    }

    /** A fault from an enum constant (its name, lower-cased, is the code). */
    public void fault(Enum<?> code, String text) {
        fault(code.name().toLowerCase(Locale.ROOT), text);
    }

    public List<Fault> faults() {
        return Collections.unmodifiableList(faults);
    }

    /** A connection: its name (a key), kind and side, whether something is connected (null: unknown), its value (or null). */
    public Port port(String name, Port.Kind kind, String side, Boolean connected, Num value) {
        Port p = new Port(name, kind, side, connected, value);
        for (Port q : ports) if (q.name().equals(name)) throw new IllegalArgumentException("port '" + name + "' given twice");
        ports.add(p);
        return p;
    }

    /** A number for a port's value (not added to any section). */
    public static Num value(double v, Unit unit) {
        return new Num(v, unit);
    }

    public List<Port> ports() {
        return Collections.unmodifiableList(ports);
    }

    public record Fault(String code, String text) {
    }

    /** The checks a report must pass (empty: fine). GameTests run it on every device. */
    public List<String> problems() {
        List<String> out = new ArrayList<>();
        Map<String, Section> named = new java.util.LinkedHashMap<>();
        named.put("id", id);
        named.put("spec", spec);
        named.put("state", state);
        named.put("sync", sync);
        named.put("electrical", electrical);
        named.put("thermal", thermal);
        named.put("legacy", legacy);
        named.put("client", client);
        named.forEach((n, s) -> check(n, s, out));
        if (!id.entries().containsKey(Keys.KIND)) out.add("id.kind missing");
        checkSync("sync", sync, out);
        for (Port p : ports)
            if (p.value() != null && !Double.isFinite(p.value().v)) out.add("ports." + p.name() + " is " + p.value().v);
        return out;
    }

    private static void checkSync(String path, Section s, List<String> out) {
        s.entries().forEach((k, v) -> {
            if (v instanceof Value.Text) out.add(path + "." + k + " is text: sync holds values both halves compare");
            if (v instanceof Num n && Double.isNaN(n.tol())) out.add(path + "." + k + " has no tolerance");
            if (v instanceof Section sub) checkSync(path + "." + k, sub, out);
        });
    }

    private static void check(String path, Section s, List<String> out) {
        s.entries().forEach((k, v) -> {
            if (v instanceof Num n && !Double.isFinite(n.v)) out.add(path + "." + k + " is " + n.v);
            if (v instanceof Section sub) check(path + "." + k, sub, out);
        });
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("half", half.name().toLowerCase(Locale.ROOT));
        o.add("id", id.json());
        o.add("spec", spec.json());
        o.add("state", state.json());
        o.add("sync", sync.json());
        JsonArray f = new JsonArray();
        for (Fault x : faults) {
            JsonObject e = new JsonObject();
            e.addProperty("code", x.code());
            e.addProperty("text", x.text());
            f.add(e);
        }
        o.add("faults", f);
        JsonArray p = new JsonArray();
        for (Port x : ports) p.add(x.json());
        o.add("ports", p);
        o.add("electrical", electrical.json());
        o.add("thermal", thermal.json());
        o.add("legacy", legacy.json());
        if (half == Half.CLIENT) o.add("client", client.json());
        return o;
    }
}
