package mods.eln.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mods.eln.Eln;

import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Every ported device's descriptor numbers against golden/1.12/inventory.json (port step T4): a value changed while
 * porting (a constructor argument shuffled by an edit...) is invisible to behaviour cases unless asserted, this
 * catches it. Golden dir: system property eln.golden (set by the gameTestServer run). Fields that only described 1.12
 * items or renders are ignored.
 */
final class GoldenCase implements TestCase {
    /** 1.12-only or render-only fields: no meaning on 26.x. */
    static final Set<String> IGNORED = Set.of("iconName", "IconName", "parentItemDamage", "voltageLevelColor",
        "tileEntityMetaTag");

    public String id() {
        return "golden_descriptors";
    }

    public int width() {
        return 3;
    }

    public int ticks() {
        return 1;
    }

    public void build(TestContext ctx) {
    }

    public void measure(TestContext ctx) {
        String dir = System.getProperty("eln.golden");
        if (dir == null) {
            ctx.line("INFO golden: eln.golden not set, skipped");
            return;
        }
        Map<String, JsonObject> golden = new HashMap<>();
        try (Reader r = Files.newBufferedReader(Paths.get(dir, "inventory.json"))) {
            for (JsonElement e : JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("items")) {
                JsonObject o = e.getAsJsonObject();
                golden.put(o.get("id").getAsString() + "@" + o.get("damage").getAsInt(), o);
            }
        } catch (Exception e) {
            ctx.check("golden: read inventory.json", false, e.toString());
            return;
        }
        int compared = 0;
        for (Map.Entry<Integer, ?> e : Eln.sixNodeItem.all().entrySet()) compared += compare(ctx, golden, "eln:six_node@" + e.getKey(), e.getValue());
        for (Map.Entry<Integer, ?> e : Eln.transparentNodeItem.all().entrySet()) compared += compare(ctx, golden, "eln:transparent_node@" + e.getKey(), e.getValue());
        for (Map.Entry<Integer, ?> e : Eln.sharedItem.all().entrySet()) compared += compare(ctx, golden, "eln:shared_item@" + e.getKey(), e.getValue());
        for (Map.Entry<Integer, ?> e : Eln.sharedItemStackOne.all().entrySet()) compared += compare(ctx, golden, "eln:shared_item_stack_one@" + e.getKey(), e.getValue());
        for (Map.Entry<Integer, ?> e : mods.eln.registry.batch.Wp12cContent.ores.all().entrySet()) compared += compare(ctx, golden, "eln:ore@" + e.getKey(), e.getValue());
        ctx.check("golden: descriptors compared with 1.12", compared > 0, compared + " descriptor(s)");
    }

    /** Items recreated rather than ported (new class): the 1.12 fields still compared. */
    static final Map<String, java.util.Set<String>> RECREATED = Map.of(
        "X-Ray Scanner", java.util.Set.of("energyStorage", "chargePower", "dischargePower")); // Baughn 2026-10-11

    /** Fields changed on purpose (golden/README.md): name -> field -> why. */
    static final Map<String, Map<String, String>> CHANGED = Map.of(
        "Lead Ore", Map.of("spawnRate", "lead generates on 26.x (Baughn 2026-10-11): no other mod brings lead"),
        "Stone Heat Furnace", Map.of("combustionChamberMax", "4 combustion chambers, was 2 (Baughn 2026-10-26: steam turbines by the expansion)"));

    /** Items new in 26.x (no 1.12 twin): Copper Coil, the thin winding wire (Baughn 2026-10-13). */
    static final java.util.Set<String> NEW_IN_26 = java.util.Set.of("eln:shared_item@4163",
        "eln:transparent_node@5120", "eln:transparent_node@5124",   // 26.x oil: tar sand separators
        "eln:transparent_node@5184", "eln:transparent_node@5188",   // 26.x oil: pumps
        "eln:transparent_node@5192", "eln:transparent_node@5196",   // submersible pumps (Baughn 2026-10-25)
        "eln:transparent_node@5248", "eln:transparent_node@5252", "eln:transparent_node@5256",  // heaters, boiler
        "eln:transparent_node@5260", "eln:transparent_node@5264",  // small steam turbines
        "eln:transparent_node@5268", "eln:transparent_node@5272",  // the steam oven and the smelter (Baughn 2026-10-23)
        "eln:transparent_node@5276", "eln:transparent_node@5280",  // the turbines' condensers (Baughn 2026-10-26)
        "eln:transparent_node@5312", "eln:transparent_node@5316", "eln:transparent_node@5320",  // column, flare stack
        // the FE exporters: 1.12 simple-node blocks (eln:energy_converter_eln_to_other_*block), transparent nodes since 26.x
        "eln:transparent_node@5376", "eln:transparent_node@5380", "eln:transparent_node@5384",
        "eln:ore@7");   // Deepslate Lead Ore: lead in deepslate's middle (Baughn 2026-10-20)
    /** Devices new in 26.x by display name (no 1.12 container to compare slots with). */
    static final java.util.Set<String> NEW_IN_26_NAMES = java.util.Set.of("50V Tar Sand Separator", "200V Tar Sand Separator",
        "50V Electric Heater", "200V Electric Heater", "Smelter", "Steam Oven", "Small Steam Condenser", "Steam Condenser",
        "50V Steam Turbine", "200V Steam Turbine");   // (their screen: Baughn 2026-10-26)

    static int compare(TestContext ctx, Map<String, JsonObject> golden, String key, Object descriptor) {
        JsonObject g = golden.get(key);
        if (g == null && NEW_IN_26.contains(key)) return 0;
        if (g == null) {
            ctx.check("golden " + key + ": known in 1.12", false, "no such item in golden/1.12/inventory.json");
            return 0;
        }
        String name = g.get("name").getAsString();
        java.util.Set<String> kept = RECREATED.get(name);
        if (kept != null) {
            // recreated on purpose: a new class; only the numbers that keep their meaning are compared
            JsonObject now = fields(descriptor), was = g.getAsJsonObject("fields");
            StringBuilder diff = new StringBuilder();
            for (String f : kept)
                if (now.get(f) == null || !same(was.get(f), now.get(f))) diff.append(f).append(": ").append(was.get(f)).append(" -> ").append(now.get(f)).append("; ");
            ctx.check("golden " + name + ": recreated, kept numbers as in 1.12", diff.length() == 0, diff.toString());
            return 1;
        }
        if (!g.has("descriptorClass") || !g.get("descriptorClass").getAsString().endsWith("." + descriptor.getClass().getSimpleName())) {
            ctx.check("golden " + name + ": same descriptor class", false,
                (g.has("descriptorClass") ? g.get("descriptorClass").getAsString() : "none") + " vs " + descriptor.getClass().getName());
            return 1;
        }
        JsonObject now = fields(descriptor);
        JsonObject was = g.getAsJsonObject("fields");
        StringBuilder diff = new StringBuilder();
        Map<String, String> changed = CHANGED.getOrDefault(name, Map.of());
        for (String f : was.keySet()) {
            if (IGNORED.contains(f) || changed.containsKey(f)) continue;
            JsonElement a = was.get(f), b = now.get(f);
            if (b == null) diff.append(f).append(" missing; ");
            else if (!same(a, b)) diff.append(f).append(": ").append(a).append(" -> ").append(b).append("; ");
        }
        ctx.check("golden " + name + ": descriptor numbers as in 1.12", diff.length() == 0, diff.toString());
        return 1;
    }

    static boolean same(JsonElement a, JsonElement b) {
        if (a.isJsonPrimitive() && b.isJsonPrimitive() && a.getAsJsonPrimitive().isNumber() && b.getAsJsonPrimitive().isNumber()) {
            double x = a.getAsDouble(), y = b.getAsDouble();
            // float fields went through float -> JSON -> double in 1.12: compare at float precision
            return x == y || Math.abs(x - y) <= 1e-6 * Math.max(Math.abs(x), Math.abs(y));
        }
        return a.equals(b);
    }

    /** Primitive, String, enum and primitive-array fields of the descriptor and its superclasses (as 1.12 Inventory). */
    static JsonObject fields(Object d) {
        JsonObject f = new JsonObject();
        for (Class<?> c = d.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field fl : c.getDeclaredFields()) {
                if (Modifier.isStatic(fl.getModifiers()) || f.has(fl.getName())) continue;
                Class<?> t = fl.getType();
                try {
                    fl.setAccessible(true);
                    Object v = fl.get(d);
                    if (v == null) continue;
                    if (v instanceof Boolean) f.addProperty(fl.getName(), (Boolean) v);
                    else if (v instanceof Character) f.addProperty(fl.getName(), v.toString());
                    else if (t.isPrimitive() || v instanceof Number) f.addProperty(fl.getName(), (Number) v);
                    else if (v instanceof String || t.isEnum()) f.addProperty(fl.getName(), v.toString());
                    else if (v instanceof double[]) {
                        JsonArray a = new JsonArray();
                        for (double x : (double[]) v) a.add(x);
                        f.add(fl.getName(), a);
                    } else if (v instanceof int[]) {
                        JsonArray a = new JsonArray();
                        for (int x : (int[]) v) a.add(x);
                        f.add(fl.getName(), a);
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // inaccessible: skip
                }
            }
        }
        return f;
    }
}
