package mods.eln.introspect;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Server entry points: the reports for what is at a block, or for an entity. Every device gets the defaults (id, ports,
 * electrical and thermal loads, its Jade lines under legacy) from what all elements already have; then its descriptor
 * adds spec ({@link DescribableSpec}) and the element adds the rest ({@link Describable}). A throwing device gets a
 * fault {@code describe_failed} instead of breaking the caller. Server thread only.
 */
public final class Describe {
    private Describe() {
    }

    /** Everything EA has at pos: a transparent device, each six-node element (by face or slot), each voxel part. A ghost
     *  block answers with its device, marked {@code via_ghost}. Empty: nothing of EA's there. */
    public static List<Report> at(ServerLevel level, BlockPos pos) {
        List<Report> out = new ArrayList<>();
        Coordonate at = new Coordonate(pos, level);
        NodeBase node = NodeManager.instance == null ? null : NodeManager.instance.getNodeFromCoordonate(at);
        String viaGhost = null;
        if (node == null && Eln.ghostManager != null) {
            var ghost = Eln.ghostManager.getGhost(at);
            if (ghost != null && ghost.getObservatorCoordonate() != null) {
                node = NodeManager.instance.getNodeFromCoordonate(ghost.getObservatorCoordonate());
                viaGhost = pos.getX() + "," + pos.getY() + "," + pos.getZ();
            }
        }
        if (node == null) return out;
        if (node instanceof TransparentNode tn && tn.element != null) out.add(transparent(tn.element, node));
        if (node instanceof SixNode six)
            for (SixNodeElement e : six.sideElementList) if (e != null) out.add(six(e, node));
        if (node instanceof VoxelNode v)
            for (int i = 0; i < v.parts.size(); i++) out.add(part(v, i));
        if (viaGhost != null) for (Report r : out) r.id().ref("via_ghost", viaGhost);
        return out;
    }

    public static Report entity(Entity e) {
        Report r = new Report(Report.Half.SERVER);
        r.id().text(Keys.KIND, String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType())));
        r.id().text(Keys.NODE, "entity");
        r.id().ref("uuid", e.getStringUUID());
        r.id().ref(Keys.POS, e.getX() + "," + e.getY() + "," + e.getZ());
        r.id().text(Keys.ELEMENT, e.getClass().getSimpleName());
        if (e instanceof Describable d) own(r, d);
        return r;
    }

    static Report transparent(TransparentNodeElement e, NodeBase node) {
        Report r = new Report(Report.Half.SERVER);
        id(r, e.transparentNodeDescriptor == null ? null : e.transparentNodeDescriptor.newItemStack(), "transparent", node);
        if (e.front != null) r.id().text("front", e.front.name().toLowerCase(Locale.ROOT));
        r.id().text(Keys.ELEMENT, e.getClass().getSimpleName());
        guard(r, () -> {
            loads(r, e.electricalLoadList, e.thermalLoadList);
            Map<Object, String> names = loadNames(e.electricalLoadList, e.thermalLoadList);
            for (mods.eln.misc.Direction d : mods.eln.misc.Direction.values())
                for (LRDU l : LRDU.values())
                    port(r, names, d.name().toLowerCase(Locale.ROOT) + "_" + l.name().toLowerCase(Locale.ROOT),
                        e.getConnectionMask(d, l), e.getElectricalLoad(d, l), e.getThermalLoad(d, l));
        });
        guard(r, () -> legacy(r, e.getWaila()));
        if (e.transparentNodeDescriptor instanceof DescribableSpec s) guard(r, () -> s.describeSpec(r.spec()));
        if (e instanceof Describable d) own(r, d);
        return r;
    }

    static Report six(SixNodeElement e, NodeBase node) {
        Report r = new Report(Report.Half.SERVER);
        id(r, e.sixNodeElementDescriptor == null ? null : e.sixNodeElementDescriptor.newItemStack(), "six", node);
        if (e.side != null) r.id().text(Keys.FACE, e.side.name().toLowerCase(Locale.ROOT));
        r.id().integer(Keys.SLOT, e.slot);
        if (e.slot >= 6) r.id().text("offset", e.ox + "," + e.oy + "," + e.oz);
        r.id().text(Keys.ELEMENT, e.getClass().getSimpleName());
        guard(r, () -> {
            loads(r, e.electricalLoadList, e.thermalLoadList);
            Map<Object, String> names = loadNames(e.electricalLoadList, e.thermalLoadList);
            for (LRDU l : LRDU.values())
                port(r, names, l.name().toLowerCase(Locale.ROOT), e.getConnectionMask(l), e.getElectricalLoad(l), e.getThermalLoad(l));
        });
        guard(r, () -> legacy(r, e.getWaila()));
        if (e.sixNodeElementDescriptor instanceof DescribableSpec s) guard(r, () -> s.describeSpec(r.spec()));
        if (e instanceof Describable d) own(r, d);
        return r;
    }

    static Report part(VoxelNode v, int i) {
        Report r = new Report(Report.Half.SERVER);
        var p = v.parts.get(i);
        r.id().text(Keys.KIND, p.kind());
        r.id().text(Keys.NODE, "voxel_part");
        r.id().ref(Keys.POS, v.coordonate.x + "," + v.coordonate.y + "," + v.coordonate.z);
        r.id().integer("index", i);
        r.id().text(Keys.ELEMENT, p.getClass().getSimpleName());
        guard(r, () -> {
            CompoundTag t = new CompoundTag();
            p.write(t);
            r.legacy().text("nbt", t.toString());
        });
        if (p instanceof Describable d) own(r, d);
        return r;
    }

    private static void id(Report r, ItemStack stack, String kind, NodeBase node) {
        String name = stack == null || stack.isEmpty() ? "unknown" : String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        r.id().text(Keys.KIND, name);
        r.id().text(Keys.NODE, kind);
        r.id().ref(Keys.POS, node.coordonate.x + "," + node.coordonate.y + "," + node.coordonate.z);
        r.id().bool("in_simulation", node.isAdded());
    }

    private static void own(Report r, Describable d) {
        guard(r, () -> d.describe(r));
    }

    /** Runs a step; if it throws, the report says so (fault describe_failed) and keeps what it had. */
    private static void guard(Report r, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException ex) {
            r.fault("describe_failed", ex.toString());
        }
    }

    /** Each named electrical load's voltage and current, each thermal load's temperature (C) and power. */
    static void loads(Report r, List<? extends State> electrical, List<? extends NbtThermalLoad> thermal) {
        Map<Object, String> names = loadNames(electrical, thermal);
        for (State s : electrical) {
            Section e = r.electrical().section(names.get(s));
            if (s instanceof VoltageState v) e.num(Keys.U, v.getU(), Unit.V);
            if (s instanceof ElectricalLoad l) e.num(Keys.I, l.getI(), Unit.A);
        }
        for (NbtThermalLoad t : thermal) {
            Section e = r.thermal().section(names.get(t));
            e.num(Keys.T, t.Tc + PhysicalConstant.Tamb, Unit.C);
            e.num(Keys.P, t.getPower(), Unit.W);
        }
    }

    /** Report names for an element's loads: their NBT names in snake_case, made unique; "load_N" for unnamed ones. */
    static Map<Object, String> loadNames(List<? extends State> electrical, List<? extends NbtThermalLoad> thermal) {
        Map<Object, String> out = new IdentityHashMap<>();
        java.util.Set<String> taken = new java.util.HashSet<>();
        int n = 0;
        for (State s : electrical) out.put(s, unique(s instanceof NbtElectricalLoad l ? snake(l.name()) : "", "load_" + n++, taken));
        java.util.Set<String> takenT = new java.util.HashSet<>();
        n = 0;
        for (NbtThermalLoad t : thermal) out.put(t, unique(snake(t.name()), "thermal_" + n++, takenT));
        return out;
    }

    private static String unique(String name, String fallback, java.util.Set<String> taken) {
        String base = Keys.valid(name) ? name : fallback;
        String k = base;
        for (int i = 2; !taken.add(k); i++) k = base + "_" + i;
        return k;
    }

    /** "inPosition" -> "in_position", "ElectricalLoad 1" -> "electrical_load_1"; anything left invalid -> "". */
    static String snake(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                if (b.length() > 0 && b.charAt(b.length() - 1) != '_') b.append('_');
                b.append(Character.toLowerCase(c));
            } else if (Character.isLetterOrDigit(c) && c < 128) b.append(c);
            else if (b.length() > 0 && b.charAt(b.length() - 1) != '_') b.append('_');
        }
        while (b.length() > 0 && b.charAt(b.length() - 1) == '_') b.setLength(b.length() - 1);
        String k = b.toString();
        return Keys.valid(k) ? k : "";
    }

    /** A port where the element answers with a load or a mask on that side. Connection state isn't known here (null). */
    private static void port(Report r, Map<Object, String> names, String side, int mask, ElectricalLoad el, ThermalLoad th) {
        if (mask == 0 && el == null && th == null) return;
        Port.Kind kind = (mask & NodeBase.maskThermal) != 0 && el == null ? Port.Kind.THERMAL
            : (mask & (NodeBase.maskElectricalGate | NodeBase.maskSignal)) != 0 ? Port.Kind.SIGNAL
            : (mask & NodeBase.maskElectricalPower) != 0 ? Port.Kind.POWER
            : th != null && el == null ? Port.Kind.THERMAL : Port.Kind.POWER;
        Num value = el != null ? Report.value(el.getU(), Unit.V) : th != null ? Report.value(th.getT() + PhysicalConstant.Tamb, Unit.C) : null;
        String load = el != null ? names.get(el) : th != null ? names.get(th) : null;
        String name = (load != null ? load : kind.name().toLowerCase(Locale.ROOT)) + "_" + side;
        r.port(name, kind, side, null, value);
    }

    /** The Jade lines, as "label: value" texts in order (legacy.lines). */
    private static void legacy(Report r, Map<String, String> waila) {
        if (waila == null || waila.isEmpty()) return;
        List<Value> lines = new ArrayList<>();
        Map<String, String> ordered = waila instanceof java.util.LinkedHashMap ? waila : new TreeMap<>(waila);
        ordered.forEach((k, v) -> lines.add(new Value.Text(k + ": " + v)));
        r.legacy().list("lines", lines);
    }
}
