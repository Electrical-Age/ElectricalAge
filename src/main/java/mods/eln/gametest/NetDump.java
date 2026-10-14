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

    /** Where a state or component lives: the device (node + element) and the field that holds it. */
    record Owner(NodeBase node, String device, String field) {
        String where() {
            var c = node.coordonate;
            return device + "(" + c.x + "," + c.y + "," + c.z + (c.dimention != 0 ? " dim " + c.dimention : "") + ")." + field;
        }
    }

    /** A voltage watchdog's limits (the device burns outside them). */
    record Limit(double min, double max, String device) {
    }

    /** Dump everything, or only the sub-systems with a state owned by a node within r of pos (pos null: all). */
    public static Path dump(Path dir, mods.eln.misc.Coordonate pos, int r) throws IOException {
        var root = Eln.simulator.mna;
        if (root == null) throw new IllegalStateException("no simulator");
        Map<Object, Owner> owners = owners();
        Map<State, Limit> limits = limits();
        Instant now = Instant.now();
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(now)
            + (pos == null ? "" : "-at-" + pos.x + "_" + pos.y + "_" + pos.z);
        Files.createDirectories(dir);
        List<SubSystem> all = root.systems();
        List<SubSystem> subs = new ArrayList<>();
        for (SubSystem sub : all) if (pos == null || near(sub, owners, pos, r)) subs.add(sub);

        Report rep = new Report(owners, limits, all);
        JsonObject json = new JsonObject();
        json.addProperty("time", now.toString());
        json.addProperty("format", "eln-netdump 2");
        if (pos != null) json.addProperty("filter", pos.x + " " + pos.y + " " + pos.z + " r " + r);
        JsonArray js = new JsonArray();
        Falstad f = new Falstad();
        for (SubSystem sub : subs) {
            int si = all.indexOf(sub);
            JsonObject o = new JsonObject();
            o.addProperty("index", si);
            JsonArray states = new JsonArray();
            for (State st : sub.states) {
                JsonObject so = new JsonObject();
                so.addProperty("id", id(st));
                so.addProperty("kind", st instanceof CurrentState ? "current" : "voltage");
                so.addProperty("value", st.state);
                Owner w = owners.get(st);
                if (w != null) owner(so, w);
                Limit l = limits.get(st);
                if (l != null) {
                    so.addProperty("min", l.min());
                    so.addProperty("max", l.max());
                }
                states.add(so);
                if (!(st instanceof CurrentState)) f.node(st, w, si);
            }
            o.add("states", states);
            JsonArray comps = new JsonArray();
            for (Component c : sub.component) {
                JsonObject co = component(c);
                Owner w = owners.get(c);
                if (w != null) co.addProperty("owner", w.where());
                if (c instanceof Line l) co.addProperty("cables", rep.lineCables(l));
                comps.add(co);
                f.component(c);
            }
            o.add("components", comps);
            JsonArray links = new JsonArray();
            for (SubSystem other : sub.interSystemConnectivity) links.add(all.indexOf(other));
            o.add("interSystems", links);
            js.add(o);
            rep.subSystem(sub, si);
        }
        json.add("subSystems", js);
        json.addProperty("subSystemCount", all.size());
        rep.devices(pos, r);

        Path txt = dir.resolve(stamp + ".txt");
        Files.writeString(txt, rep.text(now, all.size(), subs.size(), pos, r));
        try (Writer w = Files.newBufferedWriter(dir.resolve(stamp + ".json"))) {
            new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create().toJson(json, w);
        }
        Files.writeString(dir.resolve(stamp + ".falstad.txt"), f.text());
        return txt;
    }

    public static Path dump(Path dir) throws IOException {
        return dump(dir, null, 0);
    }

    static boolean near(SubSystem sub, Map<Object, Owner> owners, mods.eln.misc.Coordonate pos, int r) {
        for (State s : sub.states) {
            Owner w = owners.get(s);
            if (w == null) continue;
            var c = w.node().coordonate;
            if (c.dimention == pos.dimention && Math.abs(c.x - pos.x) <= r && Math.abs(c.y - pos.y) <= r && Math.abs(c.z - pos.z) <= r)
                return true;
        }
        return false;
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

    /** States and components by owner: everything a node, its elements, or their collections hold (by reflection). */
    static Map<Object, Owner> owners() {
        Map<Object, Owner> m = new IdentityHashMap<>();
        for (NodeBase n : new ArrayList<>(NodeManager.instance.getNodes())) {
            scan(m, n, n, n.getClass().getSimpleName());
            if (n instanceof SixNode six)
                for (SixNodeElement e : six.sideElementList) if (e != null) scan(m, n, e, e.getClass().getSimpleName() + "@" + e.side);
            if (n instanceof TransparentNode t && t.element != null) scan(m, n, t.element, t.element.getClass().getSimpleName());
        }
        return m;
    }

    static boolean wanted(Object x) {
        return x instanceof State || x instanceof Component;
    }

    static void scan(Map<Object, Owner> m, NodeBase node, Object holder, String device) {
        for (Class<?> c = holder.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field fd : c.getDeclaredFields()) {
                if (Modifier.isStatic(fd.getModifiers())) continue;
                Class<?> t = fd.getType();
                if (!State.class.isAssignableFrom(t) && !Component.class.isAssignableFrom(t)
                    && !Collection.class.isAssignableFrom(t) && !t.isArray() && t != Object.class) continue;
                Object v;
                try {
                    fd.setAccessible(true);
                    v = fd.get(holder);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    continue;
                }
                if (wanted(v)) m.putIfAbsent(v, new Owner(node, device, fd.getName()));
                else if (v instanceof Collection<?> col) {
                    int i = 0;
                    for (Object x : col) {
                        if (wanted(x)) m.putIfAbsent(x, new Owner(node, device, fd.getName() + "[" + i + "]"));
                        i++;
                    }
                } else if (v instanceof Object[] arr) {
                    for (int i = 0; i < arr.length; i++)
                        if (wanted(arr[i])) m.putIfAbsent(arr[i], new Owner(node, device, fd.getName() + "[" + i + "]"));
                }
            }
        }
    }

    /** Voltage watchdog limits by state: every element's VoltageStateWatchDog in its slow processes. */
    static Map<State, Limit> limits() {
        Map<State, Limit> m = new IdentityHashMap<>();
        for (NodeBase n : new ArrayList<>(NodeManager.instance.getNodes())) {
            List<Object[]> els = new ArrayList<>();
            if (n instanceof SixNode six)
                for (SixNodeElement e : six.sideElementList) if (e != null) els.add(new Object[]{e, e.slowProcessList});
            if (n instanceof TransparentNode t && t.element != null) els.add(new Object[]{t.element, t.element.slowProcessList});
            for (Object[] e : els)
                for (Object p : (Collection<?>) e[1])
                    if (p instanceof mods.eln.sim.process.destruct.VoltageStateWatchDog w) {
                        try {
                            State s = (State) LabCommands.field(w, "state");
                            double min = (Double) LabCommands.field(w, "min"), max = (Double) LabCommands.field(w, "max");
                            if (s != null) m.putIfAbsent(s, new Limit(min, max, e[0].getClass().getSimpleName()));
                        } catch (RuntimeException ex) {
                            // a watchdog of another shape
                        }
                    }
        }
        return m;
    }

    /** The readable report: checks first, then one block per sub-system, then the devices. */
    static final class Report {
        final Map<Object, Owner> owners;
        final Map<State, Limit> limits;
        final List<SubSystem> all;
        final List<String> checks = new ArrayList<>();
        final List<Integer> unpowered = new ArrayList<>(), noGround = new ArrayList<>();
        final StringBuilder body = new StringBuilder();
        final StringBuilder devices = new StringBuilder();
        int nodes, simulated;

        Report(Map<Object, Owner> owners, Map<State, Limit> limits, List<SubSystem> all) {
            this.owners = owners;
            this.limits = limits;
            this.all = all;
        }

        String lineCables(Line l) {
            java.util.LinkedHashSet<String> s = new java.util.LinkedHashSet<>();
            for (State st : l.states) {
                Owner w = owners.get(st);
                if (w != null) {
                    var c = w.node().coordonate;
                    s.add(w.device().replaceAll("Element@.*", "") + "(" + c.x + "," + c.y + "," + c.z + ")");
                }
            }
            return String.join(" ", s);
        }

        String v(double x) {
            return String.format("%.4gV", x);
        }

        void subSystem(SubSystem sub, int si) {
            Map<State, String> name = new IdentityHashMap<>();
            int k = 0;
            for (State s : sub.states) if (!(s instanceof CurrentState)) name.put(s, "N" + si + "." + k++);
            boolean source = false, ground = false;
            Map<State, Double> kcl = new IdentityHashMap<>();
            StringBuilder comps = new StringBuilder();
            for (Component c : sub.component) {
                if (c instanceof VoltageSource || c instanceof PowerSource || c instanceof Capacitor || c instanceof Inductor) source = true;
                State[] pins = c.getConnectedStates();
                if (pins != null) for (State p : pins) if (p == null) ground = true;
                Owner w = owners.get(c);
                String what = c.getClass().getSimpleName();
                String val = "";
                if (c instanceof VoltageSource vs) val = v(vs.getU());
                else if (c instanceof Capacitor cap) val = String.format("%.3gF", cap.getC());
                else if (c instanceof Inductor ind) val = String.format("%.3gH", ind.getL());
                else if (c instanceof Transformer t) val = "ratio " + String.format("%.4g", t.getRatio());
                else if (c instanceof PowerSource ps) val = String.format("P %.4gW", ps.getP());
                else if (c instanceof Resistor r) val = String.format("%.4gΩ", r.getR());
                if (c instanceof ResistorSwitch rs) val += rs.isDiode() ? " diode" : rs.getState() ? " closed" : " open";
                String pinStr = "";
                if (c instanceof Bipole b) {
                    pinStr = pinName(b.aPin, name) + " → " + pinName(b.bPin, name);
                    double i = b.getCurrent();
                    // current into each pin node; a voltage source's current runs the other way (out of aPin)
                    double into = c instanceof VoltageSource ? i : -i;
                    if (b.aPin != null && !(c instanceof Transformer)) kcl.merge(b.aPin, into, Double::sum);
                    if (b.bPin != null && !(c instanceof Transformer)) kcl.merge(b.bPin, -into, Double::sum);
                    pinStr += String.format("  U %s  I %.4gA  P %.4gW", v(b.getU()), i, b.getU() * i);
                    if (c instanceof Resistor r && !(c instanceof Line)) {
                        if (r.getR() <= 1e-5) checks.add("near-short " + what + " " + val + " at " + (w == null ? "?" : w.where()));
                    }
                }
                if (!Double.isFinite(c instanceof Bipole b2 ? b2.getCurrent() : 0)) checks.add("NaN/inf current in " + what + " at " + (w == null ? "?" : w.where()));
                String extra = c instanceof Line l ? "  through " + l.resistors.size() + " links, " + l.states.size() + " cable nodes: " + lineCables(l) : "";
                comps.append(String.format("    %-18s %-12s %s  [%s]%s%n", what, val, pinStr, w == null ? "-" : w.where(), extra));
            }
            StringBuilder ns = new StringBuilder();
            for (State s : sub.states) {
                if (s instanceof CurrentState) continue;
                Owner w = owners.get(s);
                Limit l = limits.get(s);
                String lim = "";
                if (l != null) {
                    lim = String.format("  limit %s..%s", v(l.min()), v(l.max()));
                    if (s.state > l.max() || s.state < l.min())
                        checks.add(String.format("OVER LIMIT %s = %s (limit %s..%s) at %s", name.get(s), v(s.state), v(l.min()), v(l.max()), w == null ? "?" : w.where()));
                    else if (s.state > 0.9 * l.max())
                        checks.add(String.format("near limit %s = %s (max %s) at %s", name.get(s), v(s.state), v(l.max()), w == null ? "?" : w.where()));
                }
                if (!Double.isFinite(s.state)) checks.add("NaN/inf voltage " + name.get(s) + " at " + (w == null ? "?" : w.where()));
                Double res = kcl.get(s);
                if (res != null && Math.abs(res) > 1e-3 * Math.max(1, maxCurrent(sub)))
                    checks.add(String.format("KCL residual %.4gA at %s (%s)", res, name.get(s), w == null ? "?" : w.where()));
                ns.append(String.format("    %-8s %12s  %s%s%n", name.get(s), v(s.state), w == null ? "(internal)" : w.where(), lim));
            }
            boolean live = false;
            for (State st : sub.states) if (Math.abs(st.state) > 1e-6) live = true;
            if (!source && live) checks.add("sub-network #" + si + " has no source but non-zero voltages");
            if (!source) unpowered.add(si);
            if (!ground) noGround.add(si);
            List<Integer> links = new ArrayList<>();
            for (SubSystem o : sub.interSystemConnectivity) links.add(all.indexOf(o));
            body.append(String.format("== Sub-network #%d: %d nodes, %d components%s%n", si, name.size(), sub.component.size(),
                links.isEmpty() ? "" : ", linked to " + links));
            body.append("  nodes:\n").append(ns).append("  components:\n").append(comps).append('\n');
        }

        static double maxCurrent(SubSystem sub) {
            double m = 0;
            for (Component c : sub.component) if (c instanceof Bipole b && Double.isFinite(b.getCurrent())) m = Math.max(m, Math.abs(b.getCurrent()));
            return m;
        }

        static String pinName(State s, Map<State, String> name) {
            if (s == null) return "GND";
            String n = name.get(s);
            return n != null ? n : "ext:" + id(s);
        }

        /** Every EA node: in the simulation or not (and why), and what its elements say. */
        void devices(mods.eln.misc.Coordonate pos, int r) {
            var veto = NodeBase.simulationVeto;
            for (NodeBase n : new ArrayList<>(NodeManager.instance.getNodes())) {
                var c = n.coordonate;
                if (pos != null && (c.dimention != pos.dimention || Math.abs(c.x - pos.x) > r || Math.abs(c.y - pos.y) > r || Math.abs(c.z - pos.z) > r))
                    continue;
                nodes++;
                String state;
                if (n.isAdded()) {
                    simulated++;
                    state = "simulated";
                } else if (n.isDestructing()) state = "being destroyed";
                else if (veto != null && veto.test(n)) state = "NOT simulated: held out by the veto (Stasis: stopped land)";
                else {
                    state = "NOT simulated (no veto: removed, burnt, or not connected yet)";
                    checks.add("not simulated without a reason: " + n.getClass().getSimpleName() + " at " + c.x + "," + c.y + "," + c.z);
                }
                devices.append(String.format("  %d,%d,%d%s %s: %s%n", c.x, c.y, c.z, c.dimention != 0 ? " dim " + c.dimention : "", n.getClass().getSimpleName(), state));
                if (n instanceof SixNode six)
                    for (SixNodeElement e : six.sideElementList) if (e != null) devices.append(String.format("      %s %s %s%n", e.side, e.getClass().getSimpleName(), new java.util.TreeMap<>(e.getWaila())));
                if (n instanceof TransparentNode t && t.element != null)
                    devices.append(String.format("      %s %s%n", t.element.getClass().getSimpleName(), new java.util.TreeMap<>(t.element.getWaila())));
            }
        }

        String text(Instant now, int total, int shown, mods.eln.misc.Coordonate pos, int r) {
            StringBuilder sb = new StringBuilder();
            sb.append("EA netdump ").append(now).append(pos == null ? "" : String.format("  (only near %d,%d,%d r %d)", pos.x, pos.y, pos.z, r)).append('\n');
            sb.append(String.format("%d of %d sub-networks; EA nodes %d, simulated %d%n", shown, total, nodes, simulated));
            sb.append("Node names: N<sub-network>.<k>; GND = ground; ext: = a state in another sub-network. Owner = device(x,y,z).field.\n\n");
            sb.append("== Checks (").append(checks.size()).append(")\n");
            if (checks.isEmpty()) sb.append("  none\n");
            for (String c : checks) sb.append("  ").append(c).append('\n');
            if (!unpowered.isEmpty()) sb.append("  (info) no source in sub-networks ").append(unpowered).append('\n');
            if (!noGround.isEmpty()) sb.append("  (info) no path to ground in sub-networks ").append(noGround).append('\n');
            sb.append('\n').append(body);
            sb.append("== Devices\n").append(devices);
            return sb.toString();
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
