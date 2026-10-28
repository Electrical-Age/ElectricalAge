package mods.eln.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mods.eln.Eln;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.gui.MenuSpec;
import mods.eln.gui.MenuSpecs;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.NodeMenus;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Device menus (P4) against golden/1.12/slots.json: every 1.12 device with a container is placed, its menu built
 * server-side (NodeMenus.menu, as a player opening it), and its device slots compared in order: inventory index,
 * stack limit, and which probes the slot accepts. The probes are the 1.12 dump's: every shared and six-node item plus
 * nine vanilla items, mapped to 26.x (unmapped ones skipped). The client spec (from the open data) must give the same
 * slots, and the element inventory must hold every slot index.
 */
final class GoldenSlotsCase implements TestCase {
    public String id() {
        return "golden_slots";
    }

    public int width() {
        return 9;
    }

    public int height() {
        return 7;
    }

    public int depth() {
        return 7;
    }

    public int ticks() {
        return 1;
    }

    public void build(TestContext ctx) {
    }

    /** Probes 1.12 accepted and 26.x rightly refuses (golden/README.md). */
    static final Map<String, String> KNOWN_MISSING = Map.of(
        "eln:shared_item@520", "1.12 bug: ItemStackFilter.OreDict masked the damage to 8 bits, so Mercury (520) passed as Coal Dust (8)",
        "eln:shared_item@7688", "1.12 bug: ItemStackFilter.OreDict masked the damage to 8 bits, so Gold Plate (7688) passed as Coal Dust (8)",
        "minecraft:paper@0", "the E36 pack made paper a furnace fuel; vanilla has no paper fuel");

    /** 1.12 devices dropped on purpose (Baughn 2026-10-11: hidden, uncraftable, "Obselete, must be deleted"). */
    static final Set<String> REMOVED = Set.of("50V Condensator", "200V Condensator");

    /**
     * 1.12 cable slots removed on purpose (Baughn 2026-10-17, notes/port26/cable-slots.md): the device has a built-in
     * cable. By descriptor: the inventory indices of 1.12 slots that are gone (those golden slots are skipped).
     */
    static java.util.Set<Integer> removedCableSlots(Object d) {
        if (d instanceof mods.eln.sixnode.energymeter.EnergyMeterDescriptor
            || d instanceof mods.eln.sixnode.electricalsensor.ElectricalSensorDescriptor
            || d instanceof mods.eln.sixnode.thermalsensor.ThermalSensorDescriptor t && !t.temperatureOnly) return Set.of(0);
        if (d instanceof mods.eln.sixnode.lampsocket.LampSocketDescriptor) return Set.of(1);
        return Set.of();
    }

    /** Devices whose only 1.12 slot was the cable slot and whose GUI went with it (Baughn 2026-10-17). */
    static final Set<String> GUI_REMOVED = Set.of("Ground Cable");

    static final String[] VANILLA = {"coal", "redstone", "iron_ingot", "egg", "paper", "water_bucket", "lava_bucket",
        "cobblestone", "dirt"};

    /** The 26.x probes by golden key ("eln:six_node@2048", "minecraft:coal@0"). */
    static Map<String, Item> probes(List<String> unknown) throws Exception {
        Map<String, Item> m = new LinkedHashMap<>();
        JsonObject ids;
        try (Reader r = new InputStreamReader(GoldenSlotsCase.class.getResourceAsStream("/eln-ids.json"), StandardCharsets.UTF_8)) {
            ids = JsonParser.parseReader(r).getAsJsonObject();
        }
        for (String key : ids.keySet()) {
            String item = key.substring(0, key.indexOf('@'));
            if (!item.equals("eln:shared_item") && !item.equals("eln:shared_item_stack_one") && !item.equals("eln:six_node")) continue;
            if (GoldenCase.NEW_IN_26.contains(key)) continue;      // no 1.12 twin to compare with
            put(m, key, unknown);
        }
        for (String v : VANILLA) put(m, "minecraft:" + v + "@0", unknown);
        return m;
    }

    static void put(Map<String, Item> m, String key, List<String> unknown) {
        JsonObject o = new JsonObject();
        o.addProperty("item", key.substring(0, key.indexOf('@')));
        o.addProperty("damage", Integer.parseInt(key.substring(key.indexOf('@') + 1)));
        Item it = GoldenRecipesCase.map(o, unknown);
        if (it != null) m.put(key, it);
    }

    static String key(String accepted) {
        return accepted.substring(0, accepted.indexOf(' '));
    }

    public void measure(TestContext ctx) {
        String dir = System.getProperty("eln.golden");
        if (dir == null) {
            ctx.line("INFO golden: eln.golden not set, skipped");
            return;
        }
        com.google.gson.JsonArray golden;
        Map<String, Item> probes;
        List<String> unknown = new ArrayList<>();
        try (Reader rd = Files.newBufferedReader(Paths.get(dir, "slots.json"))) {
            golden = JsonParser.parseReader(rd).getAsJsonArray();
            probes = probes(unknown);
        } catch (Exception e) {
            ctx.check("golden slots: read slots.json", false, e.toString());
            return;
        }
        ctx.line("INFO golden slots: " + probes.size() + " probes on 26.x, " + unknown.size() + " not (skipped)");

        Map<String, Object> byName = new LinkedHashMap<>();
        for (SixNodeDescriptor d : Eln.sixNodeItem.all().values()) byName.putIfAbsent(d.name, d);
        for (TransparentNodeDescriptor d : Eln.transparentNodeItem.all().values()) byName.putIfAbsent(d.name, d);

        ServerLevel level = ctx.level();
        Player player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        BlockPos p = ctx.at(4, 1, 1); // the floor is z 0..2
        Set<Class<?>> seen = new HashSet<>();
        int devices = 0, slotsOk = 0, slotsAll = 0;
        List<String> known = new ArrayList<>();
        for (JsonElement e : golden) {
            JsonObject dev = e.getAsJsonObject();
            String name = dev.get("device").getAsString();
            Object d = byName.get(name);
            if (d == null && REMOVED.contains(name)) continue;
            if (GUI_REMOVED.contains(name)) {
                ctx.line("INFO golden slots: known difference: " + name + " has no GUI (its cable slot was removed)");
                continue;
            }
            if (d == null) {
                ctx.check("golden slots: " + name + " exists", false, "no descriptor with that name");
                continue;
            }
            seen.add(MenuSpecs.registeredClass(d));
            NodeBase node = null;
            Direction side = Direction.YN;
            String why = null;
            if (d instanceof SixNodeDescriptor s) {
                // floor first, then a wall, then the ceiling (sconces, power sockets, suspended lamps)
                for (Direction tryside : new Direction[]{Direction.YN, Direction.XN, Direction.YP}) {
                    try {
                        if (tryside != Direction.YN) ctx.floor(p.relative(tryside.toEnumFacing()));
                        node = ctx.placeSix(s.parentItemDamage, p, tryside).sixNode;
                        side = tryside;
                        break;
                    } catch (RuntimeException ex) {
                        if (why == null) why = ex.getMessage();
                        clear(ctx);
                    }
                }
            } else {
                try {
                    node = ctx.placeTransparent(((TransparentNodeDescriptor) d).parentItemDamage, p).node;
                } catch (RuntimeException ex) {
                    why = ex.getMessage();
                }
            }
            if (node == null) {
                ctx.check("golden slots: place " + name, false, why);
                clear(ctx);
                continue;
            }
            ElnNodeMenu menu = NodeMenus.menu(1, player, node, side);
            devices++;
            if (menu == null) {
                ctx.check("golden slots: " + name + " has a menu", false, "NodeMenus.menu gave null");
                clear(ctx);
                continue;
            }
            Container elementInv = d instanceof SixNodeDescriptor ? ((mods.eln.node.six.SixNode) node).getInventory(side)
                : ((mods.eln.node.transparent.TransparentNode) node).getInventory(side);
            ctx.check("golden slots: " + name + " inventory holds the slots", elementInv != null
                    && elementInv.getContainerSize() >= menu.spec.inventorySize(),
                "element inventory " + (elementInv == null ? "null" : elementInv.getContainerSize()) + ", spec needs " + menu.spec.inventorySize());
            MenuSpec client = NodeMenus.spec(menu.kind, menu.descriptorId);
            ctx.check("golden slots: " + name + " client spec", shape(client).equals(shape(menu.spec))
                && client.playerInventory() == menu.spec.playerInventory(), "client " + client.slots().size() + " slots");

            List<Slot> device = new ArrayList<>();
            int playerSlots = 0;
            for (Slot s : menu.slots) {
                if (s.container instanceof Inventory) playerSlots++;
                else device.add(s);
            }
            ctx.check("golden slots: " + name + " player inventory", playerSlots == 36, playerSlots + " player slots");
            var gs = new com.google.gson.JsonArray();
            Set<Integer> removedSlots = removedCableSlots(d);
            for (JsonElement g : dev.getAsJsonArray("slots"))
                if (!removedSlots.contains(g.getAsJsonObject().get("index").getAsInt())) gs.add(g);
            if (!removedSlots.isEmpty())
                known.add(name + " cable slot " + removedSlots + " removed (built-in cable)");
            ctx.check("golden slots: " + name + " slot count", gs.size() == device.size(), device.size() + " vs 1.12 " + gs.size());
            for (int i = 0; i < Math.min(gs.size(), device.size()); i++) {
                JsonObject g = gs.get(i).getAsJsonObject();
                Slot s = device.get(i);
                Set<String> want = new TreeSet<>();
                for (JsonElement a : g.getAsJsonArray("accepts")) {
                    String k = key(a.getAsString());
                    if (probes.containsKey(k)) want.add(k);
                }
                Set<String> got = new TreeSet<>();
                for (var pr : probes.entrySet()) if (s.mayPlace(new ItemStack(pr.getValue()))) got.add(pr.getKey());
                int index = s.getContainerSlot(), limit = s.getMaxStackSize();
                for (String k : KNOWN_MISSING.keySet())
                    if (want.contains(k) && !got.contains(k)) {
                        want.remove(k);
                        known.add(name + " slot " + i + " no longer takes " + probes.get(k) + ": " + KNOWN_MISSING.get(k));
                    }
                // narrowed on purpose (Baughn 2026-10-22: shift-click filled outputs and ignored which slot made sense):
                // outputs take nothing, a machine's input only what it has a recipe for, a furnace's only smeltables
                boolean narrowed = s instanceof mods.eln.gui.ElnSlot es && ("Output".equals(es.spec.kind()) || "Smeltable".equals(es.spec.kind())
                    || d instanceof mods.eln.transparentnode.electricalmachine.ElectricalMachineDescriptor md && es.spec.index() == md.outStackCount);
                if (narrowed && want.containsAll(got) && !got.equals(want)) {
                    known.add(name + " slot " + i + " narrowed: takes " + got.size() + " of 1.12's " + want.size() + " probes");
                    want = got;
                }
                int wantLimit = g.get("limit").getAsInt();
                // (Baughn 2026-10-26: 4 combustion chambers, was 2, with steam turbines by the expansion)
                if ("Stone Heat Furnace".equals(name) && i == 2 && wantLimit == 2 && limit == 4) {
                    known.add(name + " slot 2 takes 4 combustion chambers (1.12: 2; Baughn 2026-10-26)");
                    wantLimit = 4;
                }
                boolean ok = index == g.get("index").getAsInt() && limit == wantLimit && got.equals(want);
                slotsAll++;
                if (ok) slotsOk++;
                else {
                    Set<String> extra = new TreeSet<>(got), missing = new TreeSet<>(want);
                    extra.removeAll(want);
                    missing.removeAll(got);
                    ctx.check("golden slots: " + name + " slot " + i, false, "index " + index + " vs " + g.get("index")
                        + ", limit " + limit + " vs " + g.get("limit") + ", extra " + names(extra, probes) + ", missing " + names(missing, probes));
                }
            }
            clear(ctx);
            ctx.check("golden slots: " + name + " removed", NodeManager.instance.getNodeFromCoordonate(new Coordonate(p, level)) == null, "node left at " + p);
        }
        for (String k : known) ctx.line("INFO golden slots: known difference: " + k);
        int expected = golden.size() - REMOVED.size() - GUI_REMOVED.size();
        ctx.check("golden slots: devices", devices == expected, devices + " of " + expected + " (1.12: " + golden.size() + ", removed " + REMOVED + ")");
        ctx.check("golden slots: slots match", slotsOk == slotsAll, slotsOk + " of " + slotsAll);
        // every descriptor with slots is a golden device: a subclass inheriting a spec 1.12 didn't give it would show here
        Set<String> goldenNames = new HashSet<>();
        for (JsonElement e : golden) goldenNames.add(e.getAsJsonObject().get("device").getAsString());
        for (var e : byName.entrySet())
            if (MenuSpecs.of(e.getValue()) != MenuSpec.SCREEN_ONLY && !goldenNames.contains(e.getKey()) && !GoldenCase.NEW_IN_26_NAMES.contains(e.getKey()))
                ctx.check("golden slots: " + e.getKey() + " has no 1.12 container", false, "spec from " + MenuSpecs.registeredClass(e.getValue()).getSimpleName());
        // a spec the 1.12 dump doesn't know would be a container 1.12 didn't have
        for (Class<?> c : MenuSpecs.registered())
            if (!c.getName().startsWith("mods.eln.oil.") && !NEW_SPECS.contains(c))   // 26.x oil & steam devices are new
                ctx.check("golden slots: " + c.getSimpleName() + " in the 1.12 dump", seen.contains(c), "spec without a 1.12 container");
    }

    /** Other device classes new in 26.x with a menu spec (no 1.12 container to match). */
    static final Set<Class<?>> NEW_SPECS = Set.of(mods.eln.transparentnode.smelter.SmelterDescriptor.class,
        mods.eln.anvil.ElectricalAnvilDescriptor.class);   // (the electrical anvil: Baughn 2026-10-28)

    /** A spec without its filters (lambdas): kind, index, position, limit per slot. */
    static List<String> shape(MenuSpec spec) {
        List<String> out = new ArrayList<>();
        for (var s : spec.slots()) out.add(s.kind() + " " + s.index() + " " + s.x() + "," + s.y() + " " + s.limit());
        out.add("player " + spec.playerInventory());
        return out;
    }

    static String names(Set<String> keys, Map<String, Item> probes) {
        List<String> out = new ArrayList<>();
        for (String k : keys) out.add(probes.get(k).toString());
        return out.size() > 6 ? out.subList(0, 6) + " +" + (out.size() - 6) : out.toString();
    }

    static void clear(TestContext ctx) {
        ServerLevel w = ctx.level();
        for (BlockPos q : BlockPos.betweenClosed(ctx.at(0, 1, 0), ctx.at(8, 6, 6))) if (!w.getBlockState(q).isAir()) w.removeBlock(q, false);
    }
}
