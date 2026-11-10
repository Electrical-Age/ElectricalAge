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
 * A device that can't be placed here fails its case. A last case loads an exporter saved with a NaN, negative, infinite
 * or too large power factor (a save the old handler could write) and steps it.
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
        }
        for (Map.Entry<Integer, TransparentNodeDescriptor> e : new TreeMap<>(Eln.transparentNodeItem.all()).entrySet()) {
            Class<?> c = e.getValue().ElementClass;
            if (c == null || !handles(c, TransparentNodeElement.class) || !seen.add(c)) continue;
            cases.add(new Device(e.getKey(), false, unique(ids, c.getSimpleName())));
        }
        cases.add(new SavedExporter());
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

    static final class Device implements TestCase {
        final int damage;
        final boolean six;
        final String kind;
        Object element;
        NodeBase node;
        String placeError;
        final List<String> thrown = new ArrayList<>();
        final List<String> outOfRange = new ArrayList<>();
        final Set<String> bad = new java.util.LinkedHashSet<>();
        Set<String> badBefore = Set.of();
        int calls;
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
                element = six ? placeSix(ctx) : placeTransparent(ctx);
                node = six ? ((SixNodeElement) element).sixNode : ((TransparentNodeElement) element).node;
            } catch (RuntimeException ex) {
                placeError = ex.toString();
                return;
            }
            ctx.after(5, () -> fuzz(ctx));
        }

        SixNodeElement placeSix(TestContext ctx) {
            BlockPos p = ctx.at(5, 1, 1);
            try {
                return ctx.placeSix(damage, p, Direction.YN);
            } catch (RuntimeException floorRefused) {
                // (walls only: the power sockets)
                ctx.floor(p.north());
                return ctx.placeSix(damage, p, Direction.ZN);
            }
        }

        TransparentNodeElement placeTransparent(TestContext ctx) {
            try {
                // (front -X: a multiblock such as the transporter grows toward +X)
                return Wp10bCases.placeFacing(ctx, damage, ctx.at(2, 1, 1), Direction.XN);
            } catch (IllegalStateException noSuchFront) {
                return ctx.placeTransparent(damage, ctx.at(5, 1, 1));
            }
        }

        void fuzz(TestContext ctx) {
            FakePlayer fp = FakePlayerFactory.getMinecraft(ctx.level());
            BlockPos at = node.coordonate.pos();
            fp.setPos(at.getX() + 0.5, at.getY() + 1, at.getZ() + 2.5);
            badBefore = scan(node);
            var range = RANGES.get(element.getClass().getSimpleName());
            String ownLimit = OWN_LIMIT.get(element.getClass().getSimpleName());
            for (int id = -128; id < 128 && thrown.size() < 8; id++) {
                for (Map.Entry<String, Payload> p : PAYLOADS.entrySet()) {
                    String what = "packet " + id + " (" + p.getKey() + ")";
                    int read = send(fp, (byte) id, what, p.getValue());
                    // (a packet id that reads nothing more is a toggle or nothing: once is enough)
                    if (read <= 1) break;
                    // (each value as it is stored: the next packet would overwrite it)
                    step(0.05, 1, what);
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
            step(0.05, 5, "the last packet");
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

        void step(double dt, int times, String after) {
            List<List<IProcess>> lists = new ArrayList<>();
            if (six) {
                SixNodeElement e = (SixNodeElement) element;
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

        public void measure(TestContext ctx) {
            ctx.check(kind + ": placed", element != null, "" + placeError);
            if (element == null) return;
            ctx.line(kind + ": " + calls + " packets");
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

    /**
     * An FE exporter saved with a NaN, then a negative power factor (what the unchecked handler could store): it
     * loads with a factor in 0..1 and its process steps without throwing.
     */
    static final class SavedExporter implements TestCase {
        final List<String> results = new ArrayList<>();

        public String id() {
            return "gui_packets_reject_bad_values_saved_exporter";
        }

        public int width() {
            return 5;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(2, 1, 1);
            ctx.placeTransparent(mods.eln.transparentnode.energyconverter.ExporterSaveUpgrade.LVU_ID, p);
            ctx.after(2, () -> {
                for (double bad : new double[]{Double.NaN, -1, Double.POSITIVE_INFINITY, 7}) {
                    try {
                        Reload r = Reload.node(ctx.level(), p, t -> {
                            t.getCompoundOrEmpty("element").putDouble("inPowerFactor", bad);
                            return t;
                        });
                        var e = (mods.eln.transparentnode.energyconverter.EnergyConverterElnToOtherElement) ((mods.eln.node.transparent.TransparentNode) r.fresh).element;
                        String err = range(e, "inPowerFactor", 0, 1);
                        try {
                            for (IProcess q : e.slowProcessList) q.process(0.05);
                        } catch (RuntimeException ex) {
                            err = (err == null ? "" : err + "; ") + ex;
                            ctx.world().removeBlock(p, false);
                        }
                        results.add(bad + ": " + (err == null ? "ok" : err));
                        if (err != null) return;
                    } catch (ReflectiveOperationException ex) {
                        results.add(bad + ": " + ex);
                        return;
                    }
                }
            });
        }

        public void measure(TestContext ctx) {
            ctx.check("saved exporter: loads a NaN, negative, infinite or too large power factor as one in 0..1, and steps",
                results.size() == 4 && results.stream().allMatch(s -> s.endsWith(": ok")), String.join("; ", results));
        }
    }
}
