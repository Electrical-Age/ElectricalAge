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
        "X-Ray Scanner", java.util.Set.of("energyStorage", "chargePower", "dischargePower"), // Baughn 2026-10-11
        // rebuilt on the steam network (Baughn 2026-10-29): steam by pipe, work by the expansion; no 1.12 number keeps
        // its meaning (it took 7,200 mB/s of capability steam at a fixed 2.257 J/mB)
        "Steam Turbine", java.util.Set.of(),
        // the same for the Gas Turbine (Baughn 2026-10-29): its 4 mB of capability fuel at a fixed rate gave way to a cycle
        "Gas Turbine", java.util.Set.of());

    /** Fields changed on purpose (golden/README.md): name -> field -> why. */
    static final Map<String, Map<String, String>> CHANGED = changed();

    static Map<String, Map<String, String>> changed() {
        Map<String, Map<String, String>> m = new HashMap<>();
        m.put("Lead Ore", Map.of("spawnRate", "lead generates on 26.x (Baughn 2026-10-11): no other mod brings lead"));
        m.put("Stone Heat Furnace", stoneHeatFurnace());
        m.put("Generator", Map.of("sound", "no single shaft loop: GeneratorRender plays MachineSounds' hum, rotor and air (Baughn 2026-10-28)"));
        m.put("Power Capacitor", Map.of("dischargeTao", "leaks with RC 30 min, as the capacitor bank (Baughn 2026-10-28: batteries stay the bulk store)"));
        m.put("Signal Processor", Map.of("alphaOff", "restyled with a logger screen on top, no door (Baughn 2026-10-30, board #45)"));
        // the voltage tiers (Eln.LVU, MVU, HVU, VVU, SVU; MVP 3 kW) and every number derived from them
        for (String solar : new String[]{"Small Solar Panel", "Small Rotating Solar Panel", "2x3 Solar Panel", "2x3 Rotating Solar Panel"})
            tier(m, solar, "electricalUmax", "electricalRs");   // (LVSolarU 59 -> 57 for the 48 V tier)
        tier(m, "Signal Cable", "electricalNominalRs", "electricalNominalVoltage", "electricalNominalPower", "electricalNominalPowerDropFactor", "electricalMaximalVoltage", "electricalRs", "thermalRp", "thermalRs", "thermalC", "electricalRsMin", "dielectricBreakOhm", "dielectricVoltage", "dielectricBreakOhmMin");
        tier(m, "Low Voltage Cable", "electricalNominalRs", "electricalNominalVoltage", "electricalMaximalVoltage", "electricalMaximalCurrent", "electricalRs", "electricalMaximalI", "electricalRsMin", "dielectricBreakOhm", "dielectricVoltage", "dielectricBreakOhmMin");
        tier(m, "Medium Voltage Cable", "electricalNominalRs", "electricalNominalVoltage", "electricalNominalPower", "electricalMaximalVoltage", "electricalMaximalCurrent", "electricalRs", "thermalRp", "thermalRs", "thermalC", "electricalMaximalI", "electricalRsMin", "dielectricBreakOhm", "dielectricVoltage", "dielectricBreakOhmMin");
        tier(m, "High Voltage Cable", "electricalNominalRs", "electricalNominalVoltage", "electricalMaximalVoltage", "electricalMaximalCurrent", "electricalRs", "electricalMaximalI", "electricalRsMin", "dielectricBreakOhm", "dielectricVoltage", "dielectricBreakOhmMin");
        tier(m, "Very High Voltage Cable", "electricalNominalRs", "electricalNominalVoltage", "electricalMaximalVoltage", "electricalMaximalCurrent", "electricalRs", "electricalMaximalI", "electricalRsMin", "dielectricBreakOhm", "dielectricVoltage", "dielectricBreakOhmMin");
        tier(m, "Weak 50V Battery Charger", "nominalVoltage", "Rp");
        tier(m, "50V Battery Charger", "nominalVoltage", "Rp");
        tier(m, "200V Battery Charger", "nominalVoltage", "Rp");
        tier(m, "High Voltage Switch", "electricalRs", "nominalVoltage", "maximalVoltage");
        tier(m, "Low Voltage Switch", "electricalRs", "nominalVoltage", "maximalVoltage");
        tier(m, "Medium Voltage Switch", "electricalRs", "nominalVoltage", "nominalPower", "maximalVoltage", "maximalPower");
        tier(m, "Signal Switch", "nominalVoltage", "nominalPower", "maximalVoltage", "maximalPower");
        tier(m, "Very High Voltage Switch", "electricalRs", "nominalVoltage", "maximalVoltage");
        tier(m, "Signal Switch with LED", "nominalVoltage", "nominalPower", "maximalVoltage", "maximalPower");
        tier(m, "50V Turbine", "powerOutPerDeltaU", "nominalU", "electricalRs");
        tier(m, "200V Turbine", "powerOutPerDeltaU", "nominalU", "electricalRs");
        tier(m, "Low Power Transmitter Antenna", "electricalNominalVoltage", "electricalMaximalVoltage", "electricalNominalInputR");
        tier(m, "Low Power Receiver Antenna", "electricalNominalVoltage", "electricalMaximalVoltage");
        tier(m, "Medium Power Transmitter Antenna", "electricalNominalVoltage", "electricalMaximalVoltage", "electricalNominalInputR");
        tier(m, "Medium Power Receiver Antenna", "electricalNominalVoltage", "electricalMaximalVoltage");
        tier(m, "High Power Transmitter Antenna", "electricalNominalVoltage", "electricalMaximalVoltage", "electricalNominalInputR");
        tier(m, "High Power Receiver Antenna", "electricalNominalVoltage", "electricalMaximalVoltage");
        tier(m, "Cost Oriented Battery", "electricalU", "electricalStdI", "electricalQ", "electricalRs", "lifeNominalCurrent", "IMax", "currentDropVoltage");
        tier(m, "Capacity Oriented Battery", "electricalU", "electricalStdI", "electricalQ", "electricalRs", "lifeNominalCurrent", "IMax", "currentDropVoltage");
        tier(m, "Voltage Oriented Battery", "electricalU", "electricalStdI", "electricalQ", "electricalRs", "lifeNominalCurrent", "IMax", "currentDropVoltage");
        tier(m, "Current Oriented Battery", "electricalU", "electricalStdI", "electricalQ", "electricalRs", "lifeNominalCurrent", "IMax", "currentDropVoltage");
        tier(m, "Life Oriented Battery", "electricalU", "electricalStdI", "electricalQ", "electricalRs", "lifeNominalCurrent", "IMax", "currentDropVoltage");
        tier(m, "Single-use Battery", "electricalU", "electricalStdI", "electricalQ", "electricalRs", "lifeNominalCurrent", "IMax");
        tier(m, "50V Macerator", "nominalU", "resistorR");
        tier(m, "200V Macerator", "nominalU", "resistorR");
        tier(m, "50V Compressor", "nominalU", "resistorR");
        tier(m, "200V Compressor", "nominalU", "resistorR");
        tier(m, "50V Magnetizer", "nominalU", "resistorR");
        tier(m, "200V Magnetizer", "nominalU", "resistorR");
        tier(m, "50V Plate Machine", "nominalU", "resistorR");
        tier(m, "200V Plate Machine", "nominalU", "resistorR");
        tier(m, "50V Egg Incubator", "nominalVoltage", "Rp");
        tier(m, "Auto Miner", "nominalVoltage", "pipeOperationRp");
        tier(m, "Wind Turbine", "maxVoltage");
        tier(m, "Water Turbine", "maxVoltage");
        tier(m, "Small Active Thermal Dissipator", "nominalElectricalU", "electricalRp");
        tier(m, "200V Active Thermal Dissipator", "nominalElectricalU", "electricalRp");
        tier(m, "50V Fuel Generator", "maxVoltage");
        tier(m, "200V Fuel Generator", "maxVoltage");
        tier(m, "Small 50V Copper Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "50V Copper Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "Small 200V Copper Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "200V Copper Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "Small 50V Iron Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "50V Iron Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "Small 200V Iron Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "200V Iron Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "Small 50V Tungsten Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "50V Tungsten Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "Small 200V Tungsten Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "200V Tungsten Heating Corp", "electricalNominalU", "electricalR", "Umax");
        tier(m, "Small 50V Incandescent Light Bulb", "nominalU", "minimalU");
        tier(m, "50V Incandescent Light Bulb", "nominalU", "minimalU");
        tier(m, "200V Incandescent Light Bulb", "nominalU", "minimalU");
        tier(m, "Small 50V Carbon Incandescent Light Bulb", "nominalU", "minimalU");
        tier(m, "50V Carbon Incandescent Light Bulb", "nominalU", "minimalU");
        tier(m, "Small 50V Economic Light Bulb", "nominalU", "minimalU", "stableU");
        tier(m, "50V Economic Light Bulb", "nominalU", "minimalU", "stableU");
        tier(m, "200V Economic Light Bulb", "nominalU", "minimalU", "stableU");
        tier(m, "50V Farming Lamp", "nominalU", "minimalU");
        tier(m, "200V Farming Lamp", "nominalU", "minimalU");
        tier(m, "50V LED Bulb", "nominalU", "minimalU");
        tier(m, "200V LED Bulb", "nominalU", "minimalU");
        tier(m, "Dielectric", "uNominal");
        // generators at HVU, not 1.12's 3200 V (Baughn 2026-11-06: 4-8 kW is a 400 V machine)
        tier(m, "Generator", "nominalU", "powerOutPerDeltaU");
        return Map.copyOf(m);
    }

    /** Why the tier-derived fields differ: Eln's voltage levels are real-world values now. */
    static final String TIERS = "real-world voltages (Baughn 2026-11-06)";

    /** Records fields of the descriptor called name as changed by the voltage tiers (added to what it already has). */
    static void tier(Map<String, Map<String, String>> m, String name, String... fields) {
        Map<String, String> f = new java.util.LinkedHashMap<>(m.getOrDefault(name, Map.of()));
        for (String field : fields) f.putIfAbsent(field, TIERS);
        m.put(name, f);
    }

    /** The Stone Heat Furnace's changes: more chambers, and the coal oven model's fields (its 1.12 drawer and flame are gone). */
    static Map<String, String> stoneHeatFurnace() {
        Map<String, String> m = new java.util.LinkedHashMap<>();
        m.put("combustionChamberMax", "4 combustion chambers, was 2 (Baughn 2026-10-26: steam turbines by the expansion)");
        String model = "remodelled as a coal oven with a firebox window, no opening drawer (Baughn 2026-11-03)";
        for (String f : new String[]{"alphaClose", "alphaOpen", "flameStartX", "flameStartY", "flameStartZ", "flameEndX", "flameEndY",
            "flameEndZ", "flameDeltaX", "flameDeltaY", "flameDeltaZ", "flamePopRate"}) m.put(f, model);
        return m;
    }

    /** Items new in 26.x (no 1.12 twin): Copper Coil, the thin winding wire (Baughn 2026-10-13). */
    static final java.util.Set<String> NEW_IN_26 = java.util.Set.of("eln:shared_item@4163",
        "eln:six_node@5955", "eln:six_node@5956",   // the three-channel data logger, the tilted terminal (Baughn 2026-10-30, board #45)
        "eln:six_node@7489",   // the Bus I/O Module (the field bus)
        "eln:six_node@7490", "eln:six_node@7491", "eln:six_node@7494",   // the Addressable Button and Light, the Floor Sensor (RS485, Baughn 2026-11-05)
        "eln:six_node@6402", "eln:six_node@6403", "eln:six_node@6404",   // the pipe sensors as devices (Baughn 2026-11-02)
        "eln:transparent_node@5120", "eln:transparent_node@5124",   // 26.x oil: tar sand separators
        "eln:transparent_node@5184", "eln:transparent_node@5188",   // 26.x oil: pumps
        "eln:transparent_node@5192", "eln:transparent_node@5196",   // submersible pumps (Baughn 2026-10-25)
        "eln:transparent_node@5248", "eln:transparent_node@5252", "eln:transparent_node@5256",  // heaters, boiler
        "eln:transparent_node@5260", "eln:transparent_node@5264",  // small steam turbines
        "eln:transparent_node@5268", "eln:transparent_node@5272",  // the steam oven and the smelter (Baughn 2026-10-23)
        "eln:transparent_node@5276", "eln:transparent_node@5280",  // the turbines' condensers (Baughn 2026-10-26)
        "eln:transparent_node@5284",  // the compact steam turbine (Baughn 2026-10-27)
        "eln:transparent_node@5312", "eln:transparent_node@5316", "eln:transparent_node@5320",  // column, flare stack
        // the FE exporters: 1.12 simple-node blocks (eln:energy_converter_eln_to_other_*block), transparent nodes since 26.x
        "eln:transparent_node@5376", "eln:transparent_node@5380", "eln:transparent_node@5384",
        "eln:transparent_node@5440", "eln:transparent_node@5444",   // the electrical anvil, the capacitor bank (Baughn 2026-10-28)
        "eln:transparent_node@272", "eln:transparent_node@273", "eln:shared_item@4164",   // 8 kW generator, shaft motor, blades (Baughn 2026-10-29)
        "eln:transparent_node@5288", "eln:transparent_node@5292", "eln:transparent_node@5296",   // tanks, inline pump (Baughn 2026-10-29)
        "eln:transparent_node@274", "eln:transparent_node@5300",   // LP turbine, plant condenser (Baughn 2026-10-29)
        "eln:transparent_node@5304",   // the counterflow heat exchanger (Baughn 2026-10-29)
        "eln:transparent_node@5200",   // the steam injection wellhead (oil lakes, todo #138; Baughn 2026-10-30)
        "eln:transparent_node@5504",   // the Linear Servo Drive (the servo layer, Baughn 2026-11-02)
        "eln:transparent_node@5568",   // the computer (robotics phase 3, Baughn 2026-11-05)
        "eln:transparent_node@129",   // the Variable DC-DC Converter (real-world voltages, Baughn 2026-11-06)
        "eln:ore@7");   // Deepslate Lead Ore: lead in deepslate's middle (Baughn 2026-10-20)
    /** Devices new in 26.x by display name (no 1.12 container to compare slots with). */
    static final java.util.Set<String> NEW_IN_26_NAMES = java.util.Set.of("48V Tar Sand Separator", "230V Tar Sand Separator", "Variable DC-DC Converter",
        "48V Electric Heater", "230V Electric Heater", "Smelter", "Steam Oven", "Small Steam Condenser", "Steam Condenser",
        "48V Steam Turbine", "230V Steam Turbine", "48V Compact Steam Turbine",
        "Electrical Anvil",   // (their screen: Baughn 2026-10-26)
        "Three-Channel Data Logger", "Tilted Terminal Logger",   // (Baughn 2026-10-30, board #45)
        "Bus I/O Module",   // (the field bus)
        "Addressable Button", "Addressable Light", "Floor Sensor", "Linear Servo Drive",   // (their address screen)
        "Computer",   // (its terminal, chips and drives)
        "Steam Turbine", "LP Steam Turbine");   // (the shaft ones' controls, as the 50V/200V's: Baughn 2026-10-29)

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
            if (f.equals("name") && a.isJsonPrimitive()) a = new com.google.gson.JsonPrimitive(renamed(a.getAsString()));
            if (b == null) diff.append(f).append(" missing; ");
            else if (!same(a, b)) diff.append(f).append(": ").append(a).append(" -> ").append(b).append("; ");
        }
        ctx.check("golden " + name + ": descriptor numbers as in 1.12", diff.length() == 0, diff.toString());
        return 1;
    }

    private static final java.util.regex.Pattern TIER_NAME = java.util.regex.Pattern.compile("^((?:Small |Weak )?)(50|200|800)V ");

    /** A 1.12 display name as it is now: "50V X" -> "48V X", "200V" -> "230V", "800V" -> "400V" (real-world voltages, Baughn 2026-11-06). */
    static String renamed(String name) {
        var m = TIER_NAME.matcher(name);
        if (!m.find()) return name;
        String v = switch (m.group(2)) {
            case "50" -> "48";
            case "200" -> "230";
            default -> "400";
        };
        return m.group(1) + v + "V " + name.substring(m.end());
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
