package mods.eln.datagen;

import mods.eln.Eln;
import mods.eln.generic.IDescriptorItem;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Map;

/**
 * en_us: every EA item and block. Descriptor items take their descriptor's name (the 1.12 TR_NAME key, which is the
 * English name); the plain items (E-Coal armour, spawn egg, tab icon) take their 1.12 names.
 */
public class ElnLang extends LanguageProvider {
    /** 1.12 TR_NAME of the items that have no descriptor. */
    static final Map<String, String> PLAIN = Map.ofEntries(
        Map.entry("e_coal_helmet", "E-Coal Helmet"), Map.entry("e_coal_chestplate", "E-Coal Chestplate"),
        Map.entry("e_coal_leggings", "E-Coal Leggings"), Map.entry("e_coal_boots", "E-Coal Boots"),
        Map.entry("replicator_spawn_egg", "Replicator Spawn Egg"),
        Map.entry("pipe_kit_iron", "Iron Pipe Kit"), Map.entry("pipe_kit_alloy", "Alloy Pipe Kit"), Map.entry("pipe_kit_large_alloy", "Large Alloy Pipe Kit"), Map.entry("pipe_valve", "Pipe Valve"), Map.entry("pipe_control_valve", "Pipe Control Valve"), Map.entry("pipe_throttle_valve", "Pipe Throttle Valve"), Map.entry("pipe_adapter", "Pipe Adapter"), Map.entry("pipe_safety_valve", "Pipe Safety Valve"), Map.entry("pipe_purge_valve", "Pipe Purge Valve"), Map.entry("pipe_breather_valve", "Pipe Breather Valve"), Map.entry("pipe_pressure_sensor", "Pipe Pressure Sensor"), Map.entry("pipe_temperature_sensor", "Pipe Temperature Sensor"), Map.entry("pipe_quality_sensor", "Pipe Steam Quality Sensor"), Map.entry("pipe_pressure_valve", "Pipe Pressure Valve"), Map.entry("battery_belt", "Battery Belt"), Map.entry("toolbag", "Toolbag"), Map.entry("headlamp", "Headlamp"), Map.entry("electric_bike", "Electric Bike"), Map.entry("terrain_bike", "Terrain Bike"),
        Map.entry("cable_kit_signal", "Signal Cable Kit"), Map.entry("cable_kit_lv", "Low Voltage Cable Kit"), Map.entry("cable_kit_mv", "Medium Voltage Cable Kit"),
        Map.entry("cable_kit_hv", "High Voltage Cable Kit"), Map.entry("cable_kit_vhv", "Very High Voltage Cable Kit"), Map.entry("cable_kit_thermal", "Copper Thermal Cable Kit"), Map.entry("cable_kit_bus", "Field Bus Cable Kit"), Map.entry("cable_kit_1kv", "1kV Cable Kit"), Map.entry("field_bus_cable", "Field Bus Cable"), Map.entry("winding", "Winding"), Map.entry("overhead_cable", "Overhead Cable"), Map.entry("pole_transformer", "Pole Transformer"), Map.entry("pole_streetlight", "Pole Streetlight"), Map.entry("pole_stasis_anchor", "Pole Stasis Anchor"), Map.entry("chisel", "Chisel"), Map.entry("handbook", "Electrical Engineer's Ledger"), Map.entry("raw_lead", "Raw Lead"), Map.entry("raw_tungsten", "Raw Tungsten"), Map.entry("capacitor_cell", "Capacitor Cell"), Map.entry("sulfur_dust", "Sulfur Dust"), Map.entry("soul_dust", "Soul Dust"), Map.entry("raw_rubber", "Raw Rubber"), Map.entry("rubber_compound", "Rubber Compound"), Map.entry("carbon_black", "Carbon Black"), Map.entry("carbon_black_rubber_compound", "Carbon Black Rubber Compound"), Map.entry("charcoal_nugget", "Charcoal Nugget"),
        Map.entry("servo_rail", "Servo Rail"), Map.entry("ram_chip", "RAM Chip (1 MB)"), Map.entry("janet_rom_chip", "ROM Chip: Janet"), Map.entry("boot_rom_chip", "ROM Chip: Floppy Boot"), Map.entry("floppy_disk", "Floppy Disk"), Map.entry("elevator_disk", "Floppy Disk: Elevator"), Map.entry("applications_disk", "Floppy Disk: Applications"), Map.entry("station_marker", "Station Marker"),
        Map.entry("iron_tank_minecart", "Iron Tank Minecart"), Map.entry("alloy_tank_minecart", "Alloy Tank Minecart"),
        Map.entry("tank_loading_station", "Tank Loading Station"),
        Map.entry("signal_cable_switch", "Signal Cable Switch"), Map.entry("signal_switch", "Signal Toggle Switch"),
        Map.entry("item_creative_tab", "Electrical Age"));

    public ElnLang(PackOutput output) {
        super(output, Eln.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("item.eln.electric_bike.tooltip", "Not a motorcycle");   // (Baughn)
        // the Tank Loading Station (Baughn 2026-11-25, todo #611)
        add("item.eln.tank_loading_station.tip1", "Rail station that couples stopped Tank Minecarts to side pipes.");
        add("item.eln.tank_loading_station.tip2", "Unpowered holds the cart; redstone releases it.");
        add("item.eln.tank_loading_station.tip3", "Comparator: 0 no cart, 1 empty, up to 15 at 90% liquid.");
        add("item.eln.terrain_bike.tooltip", "Still not a motorcycle");
        add("container.eln.electric_bike", "Bike Trunk");
        add("message.eln.electric_bike.no_battery", "No battery: pedalling only");
        add("message.eln.electric_bike.flat_battery", "Battery flat: pedalling only");
        add("key.eln.wrench", "Wrench / Tool Menu");
        add("key.eln.headlamp", "Toggle Headlamp / Flashlight");
        add("key.eln.radar_band", "Radar Goggles: Next Band");
        add("key.eln.radar_low_down", "Radar Goggles: Min Depth -");
        add("key.eln.radar_low_up", "Radar Goggles: Min Depth +");
        add("key.eln.radar_high_down", "Radar Goggles: Max Depth -");
        add("key.eln.radar_high_up", "Radar Goggles: Max Depth +");
        add("eln.headlamp.none", "No headlamp or flashlight");
        add("eln.headlamp.tip.wear", "Wear in helmet or head slot");
        add("eln.headlamp.tip.key", "Press %s to toggle");
        add("eln.headlamp.tip.power", "Power: %s W");
        add("eln.headlamp.tip.energy", "Stored energy: %s J (%s%%)");
        add("eln.headlamp.state.on", "State: On");
        add("eln.headlamp.state.off", "State: Off");
        add("item.eln.chisel.shape.cube", "cube");
        add("item.eln.chisel.shape.flat", "flat");
        add("item.eln.chisel.shape.tunnel", "tunnel");
        add("gui.eln.chisel.brush", "Chisel brush");
        add("gui.eln.toolbag.shears", "Shears");
        add("gui.eln.toolbag.radial", "Toolbag");
        add("item.eln.handbook.no_guideme", "The Ledger's pages are blank: it needs the GuideME mod.");
        // JEI (compat/jei/ResinCategory)
        add("jei.eln.tree_resin_collector", "Tree Resin Collector");
        add("jei.eln.anneal", "Heat Treatment");
        add("item.eln.oil_survey_map", "Oil Survey Map");
        add("item.eln.oil_survey_map.lore", "Oil under X %s, Z %s");
        add("item.eln.winding.wire", "%s turns of %s, %s A, %s");   // (the last: the wire's colour, a vanilla color.minecraft.* name)
        add("jei.eln.oil_rubber", "Oil to Rubber");
        add("jei.eln.oil.energy", "%s kJ —");
        add("jei.eln.oil.desulfuriser", "230 V, 1 kW. Cut passes through unchanged.");
        add("jei.eln.oil.steam_cracker", "400 V, 4 kW. In: back; out: front (separate pipes).");
        add("jei.eln.oil.polymeriser", "230 V, 1 kW. Butadiene: back; Soul Dust: top/sides; output: below.");
        add("jei.eln.oil.carbon_black_burner", "No power. Fuel: back; output: below or right-click.");
        add("jei.eln.anneal.how", "%s–%s °C (%s kJ)");
        // information pages (compat.JeiPages.infos): what no recipe makes
        add("jei.eln.info.lead_ore", "Lead ore lies deep, mostly in deepslate: from y -64 to 0, most of it around -32. Mine it for raw lead.");
        add("jei.eln.info.tungsten_ore", "Tungsten ore lies in stone from y 0 to 31. Mine it for raw tungsten.");
        add("jei.eln.info.sulfur_dust", "Macerate sulfur blocks or spikes from the Sulfur Caves, or extract from oil in a Desulfuriser. Vulcanises raw rubber.");
        add("jei.eln.info.soul_dust", "Macerate soul sand or soul soil. Activator in Rubber Compound.");
        add("jei.eln.info.rubber", "Smelt Tree Resin to Raw Rubber. Vulcanising Rubber Compound in an Electrical Furnace (130–200 °C) doubles yield; above 230 °C it chars.");
        add("jei.eln.info.rubber_sapling", "Grows wild in small jungle groves. Yields three times as much resin or latex when tapped.");
        add("jei.eln.info.tar_sand", "Rims the small oil ponds above some oil lakes. A Tar Sand Separator extracts crude oil from it.");
        add("jei.eln.info.crude_oil", "Found in underground oil lakes (via Oil Pump) or surface ponds. An empty bucket draws 1000 mB from a pond, pump, or tank.");
        add("jei.eln.info.fraction", "Drawn off a Fractionating Column fed with hot crude oil (residue stays in the base). An empty bucket draws 1000 mB from any tank or machine.");
        add("jei.eln.info.blown_fuse", "What is left of a Lead Fuse after it blows. Remove it from the holder and fit a new fuse.");
        add("jei.eln.info.data_logger_print", "Printed from a Data Logger GUI when paper is inserted.");
        add("jei.eln.info.creative", "Creative only: no recipe makes it.");
        add("jei.eln.wire_drawing", "Wire Drawing");
        add("jei.eln.wire_drawing.either", "48V or 230V machine, %s kJ");
        add("jei.eln.wire_drawing.big", "230V machine only, %s kJ");
        add("jei.eln.wire_drawing.how", "Set gauge, voltage and colour in the machine GUI.");
        add("jei.eln.machine.macerator", "Macerator");
        add("jei.eln.machine.compressor", "Compressor");
        add("jei.eln.machine.plate_machine", "Plate Machine");
        add("jei.eln.machine.magnetiser", "Magnetiser");
        add("jei.eln.machine.separator", "Separator");
        add("jei.eln.machine.extra", "(sometimes more)");
        add("jei.eln.tree_resin_collector.how", "Mount on a living tree trunk; right-click to collect (3× on a Rubber Fig).");
        // the guide advancements' hints (Baughn's words, 2026-10-20; data/eln/advancement/guide, tools/advancements/make_guide.py)
        add("advancements.eln.guide.root.title", "Electrical Age");
        add("advancements.eln.guide.root.description", "Every tree gives a little resin; rubber trees give the most.");
        add("advancements.eln.guide.copper_cable.title", "Live Wire");
        add("advancements.eln.guide.copper_cable.description", "It all starts with three copper in a row.");
        add("advancements.eln.guide.heat_furnace.title", "Fire Starter");
        add("advancements.eln.guide.heat_furnace.description", "Ouch - this one is hot.");
        add("advancements.eln.guide.boiler.title", "Boiling Point");
        add("advancements.eln.guide.boiler.description", "Pipe in your water.");
        add("advancements.eln.guide.safety_valve.title", "Safety First");
        add("advancements.eln.guide.safety_valve.description", "A safety valve beats a burst.");
        add("advancements.eln.guide.steam_turbine.title", "Full Steam Ahead");
        // (Baughn 2026-10-26: an anti-achievement with the cure; SteamGeneratorElement.awardLowPressure)
        add("advancements.eln.guide.low_pressure.title", "Under Pressure");
        add("advancements.eln.guide.low_pressure.description", "Your turbine is outrunning its boiler.");
        add("advancements.eln.guide.steam_turbine.description", "Set it under the boiler. Or don't.");
        add("advancements.eln.guide.big_iron.title", "Big Iron");
        add("advancements.eln.guide.big_iron.description", "Mind the rads.");
        add("advancements.eln.guide.machine_block.title", "Machine Shop");
        add("advancements.eln.guide.machine_block.description", "Most machines start as a block.");
        add("advancements.eln.guide.macerator.title", "Grinding Gears");
        add("advancements.eln.guide.macerator.description", "Better than cheap magic. (Worse than expensive magic.)");
        add("advancements.eln.guide.electrical_furnace.title", "Electric Oven");
        add("advancements.eln.guide.electrical_furnace.description", "An oven for your kitchen. Put your roast next to your ore!");
        add("advancements.eln.guide.auto_miner.title", "Hands Off");
        add("advancements.eln.guide.auto_miner.description", "Let machines do the mining, while you do the cooking.");
        add("advancements.eln.guide.cable_kit.title", "Bundled");
        add("advancements.eln.guide.cable_kit.description", "Sensible electricians bundle their cables.");
        add("advancements.eln.guide.lead.title", "Heavy Metal");
        add("advancements.eln.guide.lead.description", "Lead hides down below.");
        add("advancements.eln.guide.battery.title", "Bottled Lightning");
        add("advancements.eln.guide.battery.description", "Don't stress them.");
        add("advancements.eln.guide.solar.title", "Sunbather");
        add("advancements.eln.guide.solar.description", "Free power, then no power.");
        add("advancements.eln.guide.chisel.title", "Chip Off the Old Block");
        add("advancements.eln.guide.chisel.description", "It's not a wrench. Hold the wrench key anyway.");
        add("advancements.eln.guide.multimeter.title", "Measure Twice");
        add("advancements.eln.guide.multimeter.description", "Can you imagine needing one of these?");
        add("advancements.eln.guide.converter.title", "Step Up");
        add("advancements.eln.guide.converter.description", "Place the cables, then connect the power.");
        add("advancements.eln.guide.fuse.title", "Fuse Box");
        add("advancements.eln.guide.fuse.description", "A fuse dies so your machines don't.");
        add("advancements.eln.guide.lamp.title", "Light Bulb Moment");
        add("advancements.eln.guide.lamp.description", "Bulbs wear out.");
        add("key.category.eln.keys", "Electrical Age");
        for (var h : ElnRegistry.ALL_ITEMS) {
            Item item = h.get();
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            String name;
            name = PLAIN.get(path);
            if (name == null && item instanceof IDescriptorItem d && d.getEaDescriptor() != null) name = descriptorName(d.getEaDescriptor());
            if (name == null) throw new IllegalStateException("ElnLang: no English name for eln:" + path);
            name = shown(name);
            add(item, name);
            // (a plain BlockItem's name is its block's: one key for both)
            if (item instanceof BlockItem b && BuiltInRegistries.BLOCK.getKey(b.getBlock()).getNamespace().equals(Eln.MODID)
                && !b.getBlock().getDescriptionId().equals(item.getDescriptionId()))
                add(b.getBlock(), name);
        }
        add(ElnRegistry.REPLICATOR.get(), "Replicator");
        add(ElnRegistry.ELECTRIC_BIKE_ENTITY.get(), "Electric Bike");
        add(ElnRegistry.TERRAIN_BIKE_ENTITY.get(), "Terrain Bike");
        add(ElnRegistry.IRON_TANK_MINECART_ENTITY.get(), "Iron Tank Minecart");
        add(ElnRegistry.ALLOY_TANK_MINECART_ENTITY.get(), "Alloy Tank Minecart");
        add(ElnRegistry.TANK_CART_LOAD.get(), "Tank Minecart's Tank");
        add(mods.eln.robotics.Robotics.ELEVATOR_CAR.get(), "Elevator Car");
        add(ElnRegistry.SIX_NODE_BLOCK.get(), "Electrical Age Device");
        add(ElnRegistry.VOXEL_NODE_BLOCK.get(), "Pipes and Cables");
        add(ElnRegistry.VOXEL_NODE_WOODEN_FENCE.get(), "Pipes and Cables");
        add(ElnRegistry.VOXEL_NODE_FENCE.get(), "Pipes and Cables");
        add(ElnRegistry.VOXEL_NODE_WALL.get(), "Pipes and Cables");
        add(ElnRegistry.GHOST_BLOCK.get(), "Electrical Age Multiblock Part");
        add(ElnRegistry.LIGHT_BLOCK.get(), "Electrical Age Light");
        add(mods.eln.apitest.ApiTestMachines.BLOCK.get(), "API Test Machine");   // (the GameTests' hidden fixture)
        for (var b : ElnRegistry.TRANSPARENT_BLOCKS) {
            var d = b.get().descriptor;
            add(b.get(), shown(d.name));
        }
        add("itemGroup.eln", "Electrical Age");
        add("death.attack.eln.scald", "%1$s was scalded by steam");
        add("death.attack.eln.scald.player", "%1$s was scalded by steam while fighting %2$s");
        add("block.eln.crude_oil", "Crude Oil");
        add("block.eln.reservoir_oil", "Crude Oil");
        add("item.eln.crude_oil_bucket", "Crude Oil Bucket");
        add("block.eln.tar_sand", "Tar Sand");
        add("block.eln.rubber_log", "Rubber Fig Log");
        add("block.eln.rubber_wood", "Rubber Fig Wood");
        add("block.eln.rubber_leaves", "Rubber Fig Leaves");
        add("block.eln.rubber_aerial_root", "Rubber Fig Aerial Root");
        add("block.eln.rubber_sapling", "Rubber Fig Sapling");
        add("block.eln.well_casing", "Well Casing");
        add("fluid.eln.steam", "Steam");
        add("fluid_type.eln.steam", "Steam");
        add("fluid.eln.latex", "Latex");
        add("fluid_type.eln.latex", "Latex");
        add("item.eln.latex_bucket", "Latex Bucket");
        add("fluid.eln.butadiene", "Butadiene");
        add("fluid_type.eln.butadiene", "Butadiene");
        add("fluid.eln.butadiene_gas", "Butadiene Vapour");
        add("fluid_type.eln.butadiene_gas", "Butadiene Vapour");
        add("jei.eln.info.polymeriser", "Converts butadiene and Soul Dust into Raw Rubber (230 V, 1 kW). Outputs to a hopper below or on right-click.");
        add("jei.eln.info.steam_cracker", "Cracks gasoline with steam into butadiene and natural gas (400 V, 4 kW). Both outlets must be piped; stalls if either backs up.");
        add("jei.eln.info.industrial_resin_collector", "Powered latex tap (230 V): ten times faster than a Tree Resin Collector, and three times faster on a Rubber Fig.");
        add("jei.eln.info.carbon_black", "Made in a Carbon Black Burner from heavy oil, residue, or crude. Black dye and rubber filler (vulcanises Rubber Compound to 11 Rubber).");
        add("jei.eln.info.latex", "Tapped by an Industrial Resin Collector or pressed from dandelions in a 230V Compressor. Press 250 mB into Raw Rubber in a 230V Compressor.");
        for (var c : mods.eln.oil.Oil.Cut.values()) {
            add("fluid.eln." + c.id, c.display);
            add("fluid_type.eln." + c.id, c.display);
            if (c.bucket) add("item.eln." + c.id + "_bucket", c.display + " Bucket");
        }
    }

    /** Names players see where they differ from the descriptor's (which keys saves and recipes, so it stays). */
    static final java.util.Map<String, String> SHOWN = java.util.Map.ofEntries(
        // the 400 V plant's shaft machines were anonymous beside the 48 V and 230 V ones; HP and LP turbines (Baughn 2026-11-26)
        Map.entry("Steam Turbine", "400V HP Steam Turbine"),
        Map.entry("LP Steam Turbine", "400V LP Steam Turbine"),
        Map.entry("Gas Turbine", "400V Gas Turbine"),
        Map.entry("Generator", "400V Generator"),
        Map.entry("Analogic Regulator", "Analogue Regulator"),
        Map.entry("Analog Watch", "Analogue Watch"),
        Map.entry("On/OFF Regulator 1 Percent", "On/Off Regulator (1%)"),
        Map.entry("On/OFF Regulator 10 Percent", "On/Off Regulator (10%)"),
        Map.entry("Portable Condensator", "Portable Capacitor"),
        Map.entry("Portable Condensator Pack", "Portable Capacitor Pack"),
        Map.entry("48V Electrical Age to other energy exporter", "48V Energy Exporter"),
        Map.entry("230V Electrical Age to other energy exporter", "230V Energy Exporter"),
        Map.entry("400V Electrical Age to other energy exporter", "400V Energy Exporter"),
        Map.entry("Analog vuMeter", "Analogue VU Meter"),
        Map.entry("LED vuMeter", "LED VU Meter"),
        Map.entry("MultiMeter", "Multimeter"),
        Map.entry("Power capacitor", "Power Capacitor Block"),
        Map.entry("Power inductor", "Power Inductor Block"),
        Map.entry("Ground Radar w/Goggles", "Ground Radar (with Goggles)"),
        Map.entry("Utility Pole w/DC-DC Converter", "Utility Pole (with Transformer)"),
        Map.entry("Street Light", "Streetlight"),
        Map.entry("Joint hub", "Joint Hub"),
        Map.entry("Configurable summing unit", "Configurable Summing Unit"),
        Map.entry("Lowpass filter", "Low-Pass Filter"),
        Map.entry("Sample and hold", "Sample and Hold"),
        Map.entry("Signal 20H inductor", "Signal 20H Inductor"),
        Map.entry("Voltage controlled amplifier", "Voltage-Controlled Amplifier"),
        Map.entry("Voltage controlled sawtooth oscillator", "Voltage-Controlled Sawtooth Oscillator"),
        Map.entry("Voltage controlled sine oscillator", "Voltage-Controlled Sine Oscillator"),
        Map.entry("Lead Fuse for medium voltage cables", "Lead Fuse for Medium Voltage Cables"),
        Map.entry("Lead Fuse for high voltage cables", "Lead Fuse for High Voltage Cables"),
        Map.entry("Lead Fuse for very high voltage cables", "Lead Fuse for Very High Voltage Cables"),
        Map.entry("Gray Brush", "Grey Brush"),
        Map.entry("Single-use Battery", "Single-Use Battery"));

    static String shown(String name) {
        return SHOWN.getOrDefault(name, name)
            .replace("DC-DC Converter", "Transformer (DC-DC)")
            .replace("Heating Corp", "Heating Element")
            .replace("Magnetizer", "Magnetiser");
    }

    static String descriptorName(Object d) {
        if (d instanceof mods.eln.generic.GenericItemUsingDamageDescriptor g) return g.name;
        if (d instanceof mods.eln.generic.GenericItemBlockUsingDamageDescriptor g) return g.name;
        if (d instanceof mods.eln.misc.DescriptorBase b) return b.descriptorKey;
        return null;
    }
}
