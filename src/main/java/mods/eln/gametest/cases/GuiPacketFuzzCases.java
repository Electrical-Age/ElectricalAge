package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.IProcess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * GUI packets with values no screen sends (review of 2026-11-09: the FE exporter stored a NaN power factor from a
 * packet, its slow process then threw in every simulator tick, and the value was saved, so the server crashed again on
 * start). Every six-node and transparent device whose element handles GUI packets is placed by enumeration (one per
 * element class), so a new handler is covered without being listed. Each packet id -128..127 is sent with crafted
 * payloads: NaN, infinities, negative and huge floats, doubles and ints, byte pairs out of every list's range, and an
 * over-long string. Then:
 * <ul>
 * <li>no handler throws (an IOException at the end of a short payload is what the node's dispatcher expects);</li>
 * <li>after each packet that read a value: the element's processes step without throwing, its saved tag holds no NaN
 *     or infinity that was not there before and no string from a packet longer than a screen's text box (64), and
 *     the values the review named are in their screens' ranges ({@link #RANGES});</li>
 * <li>the simulator then runs on with it, and networkSerialize works.</li>
 * </ul>
 * Before the bad values, each value in {@link #VALID} (what the screen sends) must be stored as sent. A device that
 * can't be placed here fails its case. The {@link #SAVED} cases load a device saved with a bad value an older handler
 * could store (the exporter's power factor, the transporter's and turret's power, the aggregator indices) and step it.
 */
public final class GuiPacketFuzzCases {
    private GuiPacketFuzzCases() {
    }

    /** The over-long string sent where a handler reads one: no screen sends more than 64 characters. */
    static final String LONG = "Z".repeat(3000);
    static final String STORED_LONG = "Z".repeat(65);
    /** Strings a handler bounds by its own, larger limit (an expression of up to Program.MAX_CHARS), by class. */
    static final Map<String, String> OWN_LIMIT = Map.of("ElectricalMathElement", "/expression");

    public static void addTo(List<TestCase> cases) {
        Set<Class<?>> seen = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (Map.Entry<Integer, SixNodeDescriptor> e : new TreeMap<>(Eln.sixNodeItem.all()).entrySet()) {
            Class<?> c = e.getValue().ElementClass;
            if (c == null || !handles(c, SixNodeElement.class) || !seen.add(c)) continue;
            cases.add(new Device(e.getKey(), true, unique(ids, c.getSimpleName())));
            if (SAVED.containsKey(c.getSimpleName())) cases.add(new Saved(e.getKey(), true, c.getSimpleName().replace("Element", "").toLowerCase(), SAVED.get(c.getSimpleName())));
        }
        for (Map.Entry<Integer, TransparentNodeDescriptor> e : new TreeMap<>(Eln.transparentNodeItem.all()).entrySet()) {
            Class<?> c = e.getValue().ElementClass;
            if (c == null || !handles(c, TransparentNodeElement.class) || !seen.add(c)) continue;
            cases.add(new Device(e.getKey(), false, unique(ids, c.getSimpleName())));
            if (SAVED.containsKey(c.getSimpleName())) cases.add(new Saved(e.getKey(), false, c.getSimpleName().replace("Element", "").toLowerCase(), SAVED.get(c.getSimpleName())));
        }
    }

    static String unique(Set<String> ids, String name) {
        String id = name.replace("Element", "").toLowerCase();
        while (!ids.add(id)) id += "_";
        return id;
    }

    /** Whether c (or a class between it and base) has its own networkUnserialize. */
    static boolean handles(Class<?> c, Class<?> base) {
        for (Class<?> k = c; k != null && k != base; k = k.getSuperclass())
            for (var m : k.getDeclaredMethods()) if (m.getName().equals("networkUnserialize")) return true;
        return false;
    }

    interface Payload {
        void write(DataOutputStream out) throws IOException;
    }

    /** The payloads after the packet id, by name. */
    static final Map<String, Payload> PAYLOADS = new LinkedHashMap<>();

    static {
        float[] floats = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1e30f, 1e30f, -1f, 3e38f};
        for (float f : floats) PAYLOADS.put("floats " + f, o -> {
            for (int i = 0; i < 16; i++) o.writeFloat(f);
        });
        double[] doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1e300, 1e300};
        for (double d : doubles) PAYLOADS.put("doubles " + d, o -> {
            for (int i = 0; i < 8; i++) o.writeDouble(d);
        });
        int[] ints = {-1, 127, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int n : ints) PAYLOADS.put("ints " + n, o -> {
            for (int i = 0; i < 16; i++) o.writeInt(n);
        });
        int[] bytes = {-128, -1, 0, 3, 6, 7, 127};
        for (int a : bytes)
            for (int b : bytes) PAYLOADS.put("bytes " + a + " " + b, o -> {
                o.writeByte(a);
                o.writeByte(b);
                for (int i = 0; i < 4; i++) o.writeFloat(Float.NaN);
            });
        for (int a : bytes) PAYLOADS.put("byte " + a + " long string", o -> {
            o.writeByte(a);
            o.writeUTF(LONG);
        });
        PAYLOADS.put("long string", o -> {
            o.writeUTF(LONG);
            o.writeInt(-1);
            o.writeUTF(LONG);
        });
        for (int n : new int[]{0, 1, 2})
            PAYLOADS.put("int " + n + ", long string, int -1", o -> {
                o.writeInt(n);
                o.writeUTF(LONG);
                o.writeInt(-1);
            });
        PAYLOADS.put("odd string", o -> o.writeUTF("NO_SUCH_MODE ../x"));
        PAYLOADS.put("nothing", o -> {
        });
    }

    /**
     * The stored values the review named, per element class: in their screens' ranges after the fuzz. (A finite but
     * out-of-range value is the crash or the solver's negative resistance; the tag scan only sees non-finite ones.)
     */
    static final Map<String, Function<Object, String>> RANGES = Map.ofEntries(
        Map.entry("EnergyConverterElnToOtherElement", e -> range(e, "inPowerFactor", 0, 1)),
        Map.entry("TeleporterElement", e -> range(e, "powerCharge", 2000, 20000)),
        Map.entry("TurretElement", e -> range(e, "chargePower", 100, 5000)),
        Map.entry("WirelessSignalRxElement", e -> range(e, "selectedAggregator", 0, 2)),
        Map.entry("LampSupplyElement", e -> {
            for (Object entry : (List<?>) get(e, "entries")) {
                String r = range(entry, "aggregator", 0, 2);
                if (r != null) return r;
            }
            return null;
        }),
        Map.entry("EnergyMeterElement", e -> get(e, "mod") == null ? "mod is null" : null),
        Map.entry("SolarPanelElement", e -> {
            Object d = get(e, "descriptor");
            return range(e, "panelAlpha", ((Number) get(d, "alphaMin")).doubleValue() - 1e-6, ((Number) get(d, "alphaMax")).doubleValue() + 1e-6);
        }),
        Map.entry("ModbusRtuElement", e -> {
            int n = ((Map<?, ?>) get(e, "wirelessTxStatusList")).size() + ((Map<?, ?>) get(e, "wirelessRxStatusList")).size();
            return n > 2 * mods.eln.sixnode.modbusrtu.ModbusRtuElement.MAX_ENTRIES ? n + " Tx/Rx entries" : null;
        }));

    static Object get(Object o, String name) {
        for (Class<?> k = o.getClass(); k != null; k = k.getSuperclass()) {
            try {
                var f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
        throw new IllegalStateException("no field " + name + " in " + o.getClass().getSimpleName());
    }

    static String range(Object o, String name, double min, double max) {
        double v = ((Number) get(o, name)).doubleValue();
        return v >= min && v <= max ? null : name + " = " + v + ", not in " + min + ".." + max;
    }

    /** Non-finite numbers and packet strings in a tag, as paths. */
    static void scan(String path, Tag t, Set<String> out) {
        if (t instanceof CompoundTag c) {
            for (String k : c.keySet()) scan(path + "/" + k, c.get(k), out);
        } else if (t instanceof ListTag l) {
            for (int i = 0; i < l.size(); i++) scan(path + "[" + i + "]", l.get(i), out);
        } else if (t instanceof DoubleTag d) {
            if (!Double.isFinite(d.doubleValue())) out.add(path + " = " + d.doubleValue());
        } else if (t instanceof FloatTag f) {
            if (!Float.isFinite(f.floatValue())) out.add(path + " = " + f.floatValue());
        } else if (t instanceof StringTag s) {
            if (s.value().contains(STORED_LONG)) out.add(path + ": a string of " + s.value().length() + " characters");
        }
    }

    static Set<String> scan(NodeBase node) {
        CompoundTag t = new CompoundTag();
        node.writeToNBT(t);
        Set<String> out = new TreeSet<>();
        scan("", t, out);
        return out;
    }

    /** The first number in t under a key ending in key (an element's NBT sits under "element" or "ED<slot>"). */
    static Double number(Tag t, String key) {
        if (!(t instanceof CompoundTag c)) return null;
        for (String k : c.keySet()) {
            Tag v = c.get(k);
            if (k.endsWith(key) && v instanceof net.minecraft.nbt.NumericTag n) return n.doubleValue();
        }
        for (String k : c.keySet()) {
            Double d = number(c.get(k), key);
            if (d != null) return d;
        }
        return null;
    }

    /** A value a screen does send, by packet id and payload, and the saved key it lands in. */
    record Valid(int id, Payload payload, String key, Function<Object, Double> expected) {
        Valid(int id, Payload payload, String key, double expected) {
            this(id, payload, key, e -> expected);
        }
    }

    static Payload floats(float... f) {
        return o -> {
            for (float x : f) o.writeFloat(x);
        };
    }

    static double mid(Object o, String min, String max) {
        return (((Number) get(o, min)).doubleValue() + ((Number) get(o, max)).doubleValue()) / 2;
    }

    /**
     * Per element class, values its screen sends, which must be stored as sent: a handler that refused everything would
     * pass the fuzz. Checked on the saved tag, right after the packet.
     */
    static final Map<String, List<Valid>> VALID = Map.ofEntries(
        Map.entry("EnergyConverterElnToOtherElement", List.of(new Valid(1, floats(0.3f), "inPowerFactor", 0.3f))),
        Map.entry("TeleporterElement", List.of(new Valid(4, floats(5000), "powerCharge", 5000))),
        Map.entry("TurretElement", List.of(new Valid(2, floats(800), "chargePower", 800))),
        Map.entry("WirelessSignalRxElement", List.of(new Valid(2, o -> o.writeByte(1), "selectedAggregator", 1))),
        Map.entry("LampSupplyElement", List.of(new Valid(3, o -> {
            o.writeByte(0);
            o.writeByte(1);
        }, "selectedAggregator0", 1))),
        Map.entry("SolarPanelElement", List.of(new Valid(0, null, "panelAlpha", e -> (double) (float) mid(get(e, "descriptor"), "alphaMin", "alphaMax")))),
        Map.entry("LampSocketElement", List.of(new Valid(2, null, "alphaZ", e -> (double) (float) mid(get(e, "socketDescriptor"), "alphaZMin", "alphaZMax")))),
        Map.entry("PIDRegulatorElement", List.of(new Valid(1, floats(10), "Kp", 10), new Valid(2, floats(2), "Ki", 2), new Valid(3, floats(1), "Kd", 1))),
        Map.entry("AmplifierElement", List.of(new Valid(1, floats(-2.5f), "gain", -2.5))),
        Map.entry("SummingUnitElement", List.of(new Valid(1, floats(2.5f), "gain0", 2.5))),
        Map.entry("FilterElement", List.of(new Valid(1, floats(0.5f), "feedback", 2 * Math.PI * 0.5))),
        Map.entry("TachometerElement", List.of(new Valid(1, floats(10, 100), "minRads", 10), new Valid(1, floats(10, 100), "maxRads", 100))),
        Map.entry("HeatFurnaceElement", List.of(new Valid(2, floats(500), "regulatortarget", 500))),
        Map.entry("ElectricalFurnaceElement", List.of(new Valid(2, floats(400), "thermalRegulatortarget", 400))),
        Map.entry("FuelHeatFurnaceElement", List.of(new Valid(3, floats(600), "setTemperature", 600), new Valid(2, floats(0.4f), "manualControl", 0.4f))),
        Map.entry("ElectricHeaterElement", List.of(new Valid(1, floats(300), "regulatortarget", 300))),
        Map.entry("SteamGeneratorElement", List.of(new Valid(2, floats(0.4f), "throttle", 0.4f))),
        Map.entry("ShaftSteamTurbineElement", List.of(new Valid(2, floats(0.4f), "throttle", 0.4f))),
        Map.entry("ElectricalSensorElement", List.of(new Valid(2, floats(-10, 30), "lowValue", -10), new Valid(2, floats(-10, 30), "highValue", 30))),
        Map.entry("ThermalSensorElement", List.of(new Valid(2, floats(-10, 30), "lowValue", -10), new Valid(2, floats(-10, 30), "highValue", 30))),
        Map.entry("ElectricalSourceElement", List.of(new Valid(1, floats(-120), "voltage", -120))),
        Map.entry("ElectricalGateSourceElement", List.of(new Valid(1, null, "outputGateProcessU", e -> (double) (float) (0.4 * Eln.SVU)))),
        Map.entry("ElectricalBreakerElement", List.of(new Valid(1, floats(200), "voltageMax", 200))),
        Map.entry("ElectricalTimeoutElement", List.of(new Valid(2, floats(5), "timeOutValue", 5))),
        Map.entry("EnergyMeterElement", List.of(new Valid(1, o -> o.writeDouble(1234), "energyStack", 1234))));

    /** Place a device of that descriptor: transparent with its front -X, else where it fits; six-node on the floor, else a wall. */
    static Object place(TestContext ctx, int damage, boolean six) {
        BlockPos p = ctx.at(5, 1, 1);
        if (six) {
            try {
                return ctx.placeSix(damage, p, Direction.YN);
            } catch (RuntimeException floorRefused) {
                // (walls only: the power sockets)
                ctx.floor(p.north());
                return ctx.placeSix(damage, p, Direction.ZN);
            }
        }
        try {
            // (front -X: a multiblock such as the transporter grows toward +X)
            return Wp10bCases.placeFacing(ctx, damage, ctx.at(2, 1, 1), Direction.XN);
        } catch (IllegalStateException noSuchFront) {
            return ctx.placeTransparent(damage, p);
        }
    }

    static NodeBase nodeOf(Object element) {
        return element instanceof SixNodeElement s ? s.sixNode : ((TransparentNodeElement) element).node;
    }

    /** Runs the element's processes (its simulator lists) times steps of dt; what threw goes into thrown (at most 8). */
    static void step(Object element, double dt, int times, String after, List<String> thrown) {
        List<List<IProcess>> lists = new ArrayList<>();
        if (element instanceof SixNodeElement e) {
            lists.add(e.slowProcessList);
            lists.add(e.electricalProcessList);
            lists.add(e.thermalProcessList);
            lists.add(e.thermalSlowProcessList);
        } else {
            TransparentNodeElement e = (TransparentNodeElement) element;
            lists.add(e.slowPreProcessList);
            lists.add(e.slowProcessList);
            lists.add(e.electricalProcessList);
            lists.add(e.thermalFastProcessList);
        }
        for (int i = 0; i < times; i++)
            for (List<IProcess> l : lists)
                for (IProcess p : new ArrayList<>(l)) {
                    try {
                        p.process(dt);
                    } catch (Throwable t) {
                        if (thrown.size() < 8) thrown.add("process " + p.getClass().getSimpleName() + " after " + after + ": " + t);
                    }
                }
    }

    static final class Device implements TestCase {
        final int damage;
        final boolean six;
        final String kind;
        Object element;
        NodeBase node;
        String placeError;
        final List<String> thrown = new ArrayList<>();
        final List<String> outOfRange = new ArrayList<>();
        final List<String> refused = new ArrayList<>();
        final Set<String> bad = new java.util.LinkedHashSet<>();
        Set<String> badBefore = Set.of();
        int calls, accepted;
        boolean removed;

        Device(int damage, boolean six, String kind) {
            this.damage = damage;
            this.six = six;
            this.kind = kind;
        }

        public String id() {
            return "gui_packets_reject_bad_values_" + kind;
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int ticks() {
            return 30;
        }

        public void build(TestContext ctx) {
            try {
                element = place(ctx, damage, six);
                node = nodeOf(element);
            } catch (RuntimeException ex) {
                placeError = ex.toString();
                return;
            }
            ctx.after(5, () -> fuzz(ctx));
        }

        void fuzz(TestContext ctx) {
            FakePlayer fp = FakePlayerFactory.getMinecraft(ctx.level());
            BlockPos at = node.coordonate.pos();
            fp.setPos(at.getX() + 0.5, at.getY() + 1, at.getZ() + 2.5);
            String name = element.getClass().getSimpleName();
            for (Valid v : VALID.getOrDefault(name, List.of())) {
                double want;
                try {
                    want = v.expected().apply(element);
                } catch (RuntimeException ex) {
                    refused.add(v.key() + ": " + ex);
                    continue;
                }
                Payload pl = v.payload() != null ? v.payload() : floats((float) want);
                send(fp, (byte) v.id(), "valid " + v.key(), pl);
                CompoundTag t = new CompoundTag();
                node.writeToNBT(t);
                Double got = number(t, v.key());
                if (got == null || Math.abs(got - want) > 1e-6 * Math.max(1, Math.abs(want))) refused.add(v.key() + " = " + got + ", sent " + want);
                else accepted++;
            }
            badBefore = scan(node);
            var range = RANGES.get(name);
            String ownLimit = OWN_LIMIT.get(name);
            for (int id = -128; id < 128 && thrown.size() < 8; id++) {
                for (Map.Entry<String, Payload> p : PAYLOADS.entrySet()) {
                    String what = "packet " + id + " (" + p.getKey() + ")";
                    int read = send(fp, (byte) id, what, p.getValue());
                    // (a packet id that reads nothing more is a toggle or nothing: once is enough)
                    if (read <= 1) break;
                    // (each value as it is stored: the next packet would overwrite it)
                    step(element, 0.05, 1, what, thrown);
                    try {
                        String r = range == null ? null : range.apply(element);
                        if (r != null && outOfRange.size() < 8) outOfRange.add(what + ": " + r);
                    } catch (RuntimeException ex) {
                        if (outOfRange.size() < 8) outOfRange.add(what + ": " + ex);
                    }
                    for (String b : scan(node))
                        if (!badBefore.contains(b) && !(ownLimit != null && b.contains(ownLimit + ": a string")) && bad.size() < 8)
                            bad.add(what + ": " + b);
                }
            }
            step(element, 0.05, 5, "the last packet", thrown);
            if (!thrown.isEmpty() || !bad.isEmpty() || !outOfRange.isEmpty()) {
                // (so the simulator doesn't meet what failed here: a throw in its tick would stop the test server)
                ctx.world().removeBlock(at, false);
                removed = true;
            }
        }

        /** The bytes the handler read (the id included). */
        int send(FakePlayer fp, byte id, String name, Payload p) {
            byte[] bytes;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bos);
                out.writeByte(id);
                p.write(out);
                bytes = bos.toByteArray();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            calls++;
            try {
                if (six) ((SixNodeElement) element).networkUnserialize(new DataInputStream(in), fp);
                else ((TransparentNodeElement) element).networkUnserialize(new DataInputStream(in), fp);
            } catch (Throwable t) {
                // (the node's dispatcher catches an IOException, a Kotlin handler may let one through)
                if (!(t instanceof IOException) && thrown.size() < 8) thrown.add(name + ": " + t);
            }
            return bytes.length - in.available();
        }

        public void measure(TestContext ctx) {
            ctx.check(kind + ": placed", element != null, "" + placeError);
            if (element == null) return;
            ctx.line(kind + ": " + calls + " packets, " + accepted + " valid values stored as sent");
            ctx.check(kind + ": the values its screen sends are stored as sent", refused.isEmpty(), String.join("; ", refused));
            ctx.check(kind + ": no handler or process threw on bad values", thrown.isEmpty(), String.join("; ", thrown));
            ctx.check(kind + ": its saved tag holds no NaN, infinity or over-long packet string", bad.isEmpty(), String.join("; ", bad));
            ctx.check(kind + ": the values the review named stay in their screens' ranges", outOfRange.isEmpty(), String.join("; ", outOfRange));
            if (removed) return;
            String serialize = null;
            try {
                DataOutputStream out = new DataOutputStream(new ByteArrayOutputStream());
                if (six) ((SixNodeElement) element).networkSerialize(out);
                else ((TransparentNodeElement) element).networkSerialize(out);
            } catch (RuntimeException ex) {
                serialize = ex.toString();
            }
            ctx.check(kind + ": networkSerialize works after the packets", serialize == null, "" + serialize);
        }
    }

    /** Saved keys an older handler let a bad value into, by element class: the key, the bad values, as an int tag. */
    record SavedBad(String key, double[] values, boolean asInt) {
    }

    static final Map<String, SavedBad> SAVED = Map.of(
        "EnergyConverterElnToOtherElement", new SavedBad("inPowerFactor", new double[]{Double.NaN, -1, Double.POSITIVE_INFINITY, 7}, false),
        "TeleporterElement", new SavedBad("powerCharge", new double[]{Double.NaN, -5000, 1e9}, false),
        "TurretElement", new SavedBad("chargePower", new double[]{Double.NaN, -100, 1e9}, false),
        "WirelessSignalRxElement", new SavedBad("selectedAggregator", new double[]{127, -1}, true),
        "LampSupplyElement", new SavedBad("selectedAggregator0", new double[]{127, -1}, true));

    /** In every compound of t that has key, set it to v. */
    static void putWherever(Tag t, String key, double v, boolean asInt) {
        if (!(t instanceof CompoundTag c)) return;
        for (String k : new ArrayList<>(c.keySet())) {
            if (k.equals(key)) {
                if (asInt) c.putInt(k, (int) v);
                else c.putDouble(k, v);
            } else putWherever(c.get(k), key, v, asInt);
        }
    }

    /**
     * A device saved with a value an older handler could store (the exporter's NaN power factor crashed the server
     * again on start; the receiver's and lamp supply's aggregator threw in the tick): it loads with the value in its
     * screen's range ({@link #RANGES}) and its processes step without throwing.
     */
    static final class Saved implements TestCase {
        final int damage;
        final boolean six;
        final String kind;
        final SavedBad bad;
        final List<String> results = new ArrayList<>();

        Saved(int damage, boolean six, String kind, SavedBad bad) {
            this.damage = damage;
            this.six = six;
            this.kind = kind;
            this.bad = bad;
        }

        public String id() {
            return "gui_packets_reject_bad_values_saved_" + kind;
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public void build(TestContext ctx) {
            Object placed = place(ctx, damage, six);
            BlockPos p = nodeOf(placed).coordonate.pos();
            Class<?> c = placed.getClass();
            ctx.after(2, () -> {
                for (double v : bad.values()) {
                    try {
                        Reload r = Reload.node(ctx.level(), p, t -> {
                            putWherever(t, bad.key(), v, bad.asInt());
                            return t;
                        });
                        Object e = r.fresh instanceof mods.eln.node.six.SixNode s
                            ? java.util.Arrays.stream(s.sideElementList).filter(c::isInstance).findFirst().orElseThrow()
                            : ((mods.eln.node.transparent.TransparentNode) r.fresh).element;
                        String err = RANGES.get(c.getSimpleName()).apply(e);
                        List<String> thrown = new ArrayList<>();
                        step(e, 0.05, 5, "the load", thrown);
                        if (!thrown.isEmpty()) {
                            err = (err == null ? "" : err + "; ") + thrown.get(0);
                            ctx.world().removeBlock(p, false);
                        }
                        results.add(v + ": " + (err == null ? "ok" : err));
                        if (err != null) return;
                    } catch (ReflectiveOperationException | RuntimeException ex) {
                        results.add(v + ": " + ex);
                        return;
                    }
                }
            });
        }

        public void measure(TestContext ctx) {
            ctx.check(kind + " saved with " + bad.key() + " out of range or not a number: loads it in range, and steps",
                results.size() == bad.values().length && results.stream().allMatch(s -> s.endsWith(": ok")), String.join("; ", results));
        }
    }
}
