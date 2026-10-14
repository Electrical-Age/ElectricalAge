package mods.eln.gametest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mods.eln.Eln;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.Bipole;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sim.mna.component.Line;
import mods.eln.sim.mna.component.Monopole;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sim.mna.component.Transformer;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.CurrentState;
import mods.eln.sim.mna.state.State;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * /elnlab netdump (Baughn 2026-10-14): the solved electrical network as the simulator runs it, i.e. after line
 * optimisation (a chain of cable resistors is one Line) and with sub-systems split by inter-system abstractions.
 * Writes eln-netdump/&lt;UTC stamp&gt;.json (everything: per sub-system its states with voltage and owner, its
 * components with type, value, U, I) and &lt;stamp&gt;.falstad.txt (importable into Falstad's circuit simulator: one
 * node per state, laid out by world position, the nearest Falstad element per component, a voltage label per node).
 * Run on the server thread (a command), between simulator steps.
 */
public final class NetDump {
    private NetDump() {
    }

    /** Where a state lives: the device and field that hold it. */
    record Owner(NodeBase node, String device, String field) {
    }

    public static Path dump(Path dir) throws IOException {
        var root = Eln.simulator.mna;
        if (root == null) throw new IllegalStateException("no simulator");
        Map<State, Owner> owners = owners();
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(Instant.now());
        Files.createDirectories(dir);

        JsonObject json = new JsonObject();
        json.addProperty("time", Instant.now().toString());
        json.addProperty("format", "eln-netdump 1");
        JsonArray subs = new JsonArray();
        Falstad f = new Falstad();
        int si = 0;
        for (SubSystem sub : root.systems()) {
            JsonObject js = new JsonObject();
            js.addProperty("index", si);
            JsonArray states = new JsonArray();
            for (State s : sub.states) {
                JsonObject o = new JsonObject();
                o.addProperty("id", id(s));
                o.addProperty("kind", s instanceof CurrentState ? "current" : "voltage");
                o.addProperty("value", s.state);
                Owner w = owners.get(s);
                if (w != null) owner(o, w);
                states.add(o);
                if (!(s instanceof CurrentState)) f.node(s, w, si);
            }
            js.add("states", states);
            JsonArray comps = new JsonArray();
            for (Component c : sub.component) {
                comps.add(component(c));
                f.component(c);
            }
            js.add("components", comps);
            JsonArray links = new JsonArray();
            for (SubSystem other : sub.interSystemConnectivity) links.add(root.systems().indexOf(other));
            js.add("interSystems", links);
            subs.add(js);
            si++;
        }
        json.add("subSystems", subs);
        json.addProperty("subSystemCount", si);

        Path jsonFile = dir.resolve(stamp + ".json");
        try (Writer w = Files.newBufferedWriter(jsonFile)) {
            new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create().toJson(json, w);
        }
        Files.writeString(dir.resolve(stamp + ".falstad.txt"), f.text());
        return jsonFile;
    }

    static String id(State s) {
        return "s" + Integer.toHexString(System.identityHashCode(s));
    }

    static void owner(JsonObject o, Owner w) {
        var c = w.node().coordonate;
        o.addProperty("device", w.device());
        o.addProperty("field", w.field());
        o.addProperty("pos", c.x + " " + c.y + " " + c.z);
        o.addProperty("dim", c.dimention);
    }

    static JsonObject component(Component c) {
        JsonObject o = new JsonObject();
        o.addProperty("type", c.getClass().getSimpleName());
        State[] pins = c.getConnectedStates();
        JsonArray p = new JsonArray();
        if (pins != null) for (State s : pins) p.add(s == null ? "ground" : id(s));
        o.add("pins", p);
        if (c instanceof Resistor r) o.addProperty("R", r.getR());
        if (c instanceof ResistorSwitch rs) {
            o.addProperty("on", rs.getState());
            o.addProperty("diode", rs.isDiode());
        }
        if (c instanceof Line l) {
            o.addProperty("lineResistors", l.resistors.size());
            o.addProperty("lineStates", l.states.size());
        }
        if (c instanceof VoltageSource v) o.addProperty("Usource", v.getU());
        if (c instanceof Capacitor cap) o.addProperty("C", cap.getC());
        if (c instanceof Inductor ind) o.addProperty("L", ind.getL());
        if (c instanceof Transformer t) o.addProperty("ratio", t.getRatio());
        if (c instanceof PowerSource ps) o.addProperty("Pset", ps.getP());
        if (c instanceof Bipole b) {
            o.addProperty("U", b.getU());
            o.addProperty("I", b.getCurrent());
        }
        return o;
    }

    /** States by owner: every State held by a node, its elements, or collections of them (by reflection). */
    static Map<State, Owner> owners() {
        Map<State, Owner> m = new IdentityHashMap<>();
        for (NodeBase n : new ArrayList<>(NodeManager.instance.getNodes())) {
            scan(m, n, n, n.getClass().getSimpleName());
            if (n instanceof SixNode six)
                for (SixNodeElement e : six.sideElementList) if (e != null) scan(m, n, e, e.getClass().getSimpleName() + "@" + e.side);
            if (n instanceof TransparentNode t && t.element != null) scan(m, n, t.element, t.element.getClass().getSimpleName());
        }
        return m;
    }

    static void scan(Map<State, Owner> m, NodeBase node, Object holder, String device) {
        for (Class<?> c = holder.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field fd : c.getDeclaredFields()) {
                if (Modifier.isStatic(fd.getModifiers())) continue;
                Class<?> t = fd.getType();
                if (!State.class.isAssignableFrom(t) && !Collection.class.isAssignableFrom(t) && !t.isArray()) continue;
                Object v;
                try {
                    fd.setAccessible(true);
                    v = fd.get(holder);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    continue;
                }
                if (v instanceof State s) m.putIfAbsent(s, new Owner(node, device, fd.getName()));
                else if (v instanceof Collection<?> col) {
                    int i = 0;
                    for (Object x : col) {
                        if (x instanceof State s) m.putIfAbsent(s, new Owner(node, device, fd.getName() + "[" + i + "]"));
                        i++;
                    }
                } else if (v instanceof Object[] arr) {
                    for (int i = 0; i < arr.length; i++)
                        if (arr[i] instanceof State s) m.putIfAbsent(s, new Owner(node, device, fd.getName() + "[" + i + "]"));
                }
            }
        }
    }

    /**
     * Falstad text (falstad.com/circuit, "Import From Text"). Nodes on a 16-unit grid: x from world x, y from world z
     * (plus a few rows for height and several states in one block); states with no device go below, per sub-system.
     * Ground pins get a ground symbol each. Transformers have no clean two-post form: left out (they are in the JSON).
     */
    static final class Falstad {
        final StringBuilder sb = new StringBuilder("$ 1 5.0E-6 10.2 50 5.0 50 5.0E-11\n");
        final Map<State, int[]> at = new IdentityHashMap<>();
        final Map<Long, Integer> used = new java.util.HashMap<>();
        int free = 0, grounds = 0;
        Integer minX, minZ;
        final List<Object[]> pending = new ArrayList<>();

        void node(State s, Owner w, int sub) {
            pending.add(new Object[]{s, w, sub});
            if (w != null) {
                var c = w.node().coordonate;
                minX = minX == null ? c.x : Math.min(minX, c.x);
                minZ = minZ == null ? c.z : Math.min(minZ, c.z);
            }
        }

        int[] place(State s) {
            int[] p = at.get(s);
            if (p != null) return p;
            // a state not seen as a node (a line's end in another sub-system, etc.): free area
            p = new int[]{16 * (free % 64), -64 - 16 * (free / 64)};
            free++;
            at.put(s, p);
            return p;
        }

        void layout() {
            for (Object[] e : pending) {
                State s = (State) e[0];
                Owner w = (Owner) e[1];
                int[] p;
                if (w != null) {
                    var c = w.node().coordonate;
                    long key = ((long) c.x << 32) ^ (c.z & 0xffffffffL);
                    int k = used.merge(key, 1, Integer::sum) - 1;
                    p = new int[]{(c.x - minX) * 96 + 16 * (k % 4), (c.z - minZ) * 96 + 16 * (k / 4)};
                } else {
                    int sub = (Integer) e[2];
                    p = new int[]{16 * (free % 64), -64 - 16 * (free / 64) - 32 * sub};
                    free++;
                }
                at.put(s, p);
                sb.append(String.format("x %d %d %d %d 0 8 %s\n", p[0] + 4, p[1] - 4, p[0] + 40, p[1] - 4,
                    String.format("%.3gV", s.state).replace(' ', '_')));
            }
            pending.clear();
        }

        final List<Component> comps = new ArrayList<>();

        void component(Component c) {
            comps.add(c);
        }

        int[] pin(State s) {
            if (s != null) return place(s);
            return null;
        }

        /** A ground point near p: its own post with a ground symbol, joined to nothing else. */
        int[] groundNear(int[] p) {
            int[] g = new int[]{p[0] + 8 + 16 * (grounds % 3), p[1] + 48};
            grounds++;
            sb.append(String.format("g %d %d %d %d 0 0\n", g[0], g[1], g[0], g[1] + 16));
            return g;
        }

        String text() {
            layout();
            for (Component c : comps) {
                // no clean two-post Falstad form (the JSON has them)
                if (c instanceof Transformer || c instanceof Monopole || !(c instanceof Bipole b)) continue;
                int[] a = pin(b.aPin), bb = pin(b.bPin);
                if (a == null && bb == null) continue;
                if (a == null) a = groundNear(bb);
                if (bb == null) bb = groundNear(a);
                if (a[0] == bb[0] && a[1] == bb[1]) continue;
                String line;
                if (c instanceof VoltageSource v)          // Falstad: post 1 is -, post 2 is +; EA: U = a - b
                    line = String.format("v %d %d %d %d 0 0 40 %s 0 0 0.5", bb[0], bb[1], a[0], a[1], num(v.getU()));
                else if (c instanceof Capacitor cap)
                    line = String.format("c %d %d %d %d 0 %s %s", a[0], a[1], bb[0], bb[1], num(cap.getC()), num(b.getU()));
                else if (c instanceof Inductor ind)
                    line = String.format("l %d %d %d %d 0 %s %s", a[0], a[1], bb[0], bb[1], num(ind.getL()), num(b.getCurrent()));
                else if (c instanceof PowerSource)        // as a current source with the present current
                    line = String.format("i %d %d %d %d 0 %s", a[0], a[1], bb[0], bb[1], num(b.getCurrent()));
                else if (c instanceof Resistor r)
                    line = String.format("r %d %d %d %d 0 %s", a[0], a[1], bb[0], bb[1], num(r.getR()));
                else continue;
                sb.append(line).append('\n');
            }
            return sb.toString();
        }

        static String num(double v) {
            return Double.isFinite(v) ? Double.toString(v) : "0";
        }
    }
}
