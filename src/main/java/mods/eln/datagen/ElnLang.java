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
        Map.entry("pipe_kit_iron", "Iron Pipe Kit"), Map.entry("pipe_kit_alloy", "Alloy Pipe Kit"), Map.entry("pipe_kit_large_alloy", "Large Alloy Pipe Kit"), Map.entry("pipe_valve", "Pipe Valve"), Map.entry("pipe_control_valve", "Pipe Control Valve"), Map.entry("pipe_throttle_valve", "Pipe Throttle Valve"), Map.entry("pipe_adapter", "Pipe Adapter"), Map.entry("pipe_safety_valve", "Pipe Safety Valve"), Map.entry("pipe_pressure_sensor", "Pipe Pressure Sensor"), Map.entry("pipe_temperature_sensor", "Pipe Temperature Sensor"), Map.entry("pipe_quality_sensor", "Pipe Steam Quality Sensor"), Map.entry("pipe_pressure_valve", "Pipe Pressure Valve"), Map.entry("battery_belt", "Battery Belt"), Map.entry("headlamp", "Headlamp"), Map.entry("electric_bike", "Electric Bike"), Map.entry("terrain_bike", "Terrain Bike"),
        Map.entry("cable_kit_signal", "Signal Cable Kit"), Map.entry("cable_kit_lv", "Low Voltage Cable Kit"), Map.entry("cable_kit_mv", "Medium Voltage Cable Kit"),
        Map.entry("cable_kit_hv", "High Voltage Cable Kit"), Map.entry("cable_kit_vhv", "Very High Voltage Cable Kit"), Map.entry("cable_kit_thermal", "Copper Thermal Cable Kit"), Map.entry("cable_kit_bus", "Field Bus Cable Kit"), Map.entry("field_bus_cable", "Field Bus Cable"), Map.entry("winding", "Winding"), Map.entry("chisel", "Chisel"), Map.entry("handbook", "Electrical Engineer's Ledger"), Map.entry("raw_lead", "Raw Lead"), Map.entry("raw_tungsten", "Raw Tungsten"), Map.entry("capacitor_cell", "Capacitor Cell"), Map.entry("sulfur_dust", "Sulfur Dust"), Map.entry("soul_dust", "Soul Dust"), Map.entry("raw_rubber", "Raw Rubber"), Map.entry("rubber_compound", "Rubber Compound"), Map.entry("charcoal_nugget", "Charcoal Nugget"),
        Map.entry("servo_rail", "Servo Rail"), Map.entry("ram_chip", "RAM Chip (1 MB)"), Map.entry("janet_rom_chip", "ROM Chip: Janet"), Map.entry("boot_rom_chip", "ROM Chip: Floppy Boot"), Map.entry("floppy_disk", "Floppy Disk"), Map.entry("elevator_disk", "Floppy Disk: Elevator"), Map.entry("applications_disk", "Floppy Disk: Applications"), Map.entry("station_marker", "Station Marker"),
        Map.entry("item_creative_tab", "Electrical Age"));

    public ElnLang(PackOutput output) {
        super(output, Eln.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("item.eln.electric_bike.tooltip", "Not a motorcycle");   // (Baughn)
        add("item.eln.terrain_bike.tooltip", "Still not a motorcycle");
        add("container.eln.electric_bike", "Bike Trunk");
        add("message.eln.electric_bike.no_battery", "No battery: pedalling only");
        add("message.eln.electric_bike.flat_battery", "The battery is flat: pedalling only");
        add("key.eln.wrench", "Wrench (hold: a switch's screen on click; the chisel's brush menu)");
        add("key.eln.headlamp", "Lights on/off (headlamp, flashlights in hand)");
        add("eln.headlamp.none", "No headlamp worn, no flashlight in hand");
        add("eln.headlamp.tip.wear", "Worn on the head: the helmet slot or the head slot");
        add("eln.headlamp.tip.key", "%s switches it on and off");
        add("eln.headlamp.tip.power", "Discharge power: %s W");
        add("eln.headlamp.tip.energy", "Stored energy: %s J (%s%%)");
        add("eln.headlamp.state.on", "State: on");
        add("eln.headlamp.state.off", "State: off");
        add("item.eln.chisel.shape.cube", "cube");
        add("item.eln.chisel.shape.flat", "flat");
        add("item.eln.chisel.shape.tunnel", "tunnel");
        add("gui.eln.chisel.brush", "Chisel brush");
        add("item.eln.handbook.no_guideme", "The Ledger's pages are blank: it needs the GuideME mod.");
        // JEI (compat/jei/ResinCategory)
        add("jei.eln.tree_resin_collector", "Tree Resin Collector");
        add("jei.eln.anneal", "Heat treatment");
        add("jei.eln.anneal.how", "Between %s and %s °C (%s kJ)");
        // information pages (compat.JeiPages.infos): what no recipe makes
        add("jei.eln.info.lead_ore", "Lead ore lies deep, mostly in deepslate: from y -64 to 0, most of it around -32. Mine it for raw lead.");
        add("jei.eln.info.tungsten_ore", "Tungsten ore lies in stone from y 0 to 31. Mine it for raw tungsten.");
        add("jei.eln.info.sulfur_dust", "Macerate vanilla sulfur from the Sulfur Caves: a sulfur block gives 4 to 5 dust, a spike 1. Mixed into Rubber Compound, it vulcanises raw rubber.");
        add("jei.eln.info.soul_dust", "Macerate soul sand or soul soil. The activator in Rubber Compound.");
        add("jei.eln.info.rubber", "Smelt Tree Resin to Raw Rubber. Raw Rubber smelts to 1 Rubber; mixed with Sulfur Dust and Soul Dust into Rubber Compound, it vulcanises to twice as much in an Electrical Furnace held at 130-200 °C (above 230 °C it chars).");
        add("jei.eln.info.rubber_sapling", "Rubber figs grow wild in small groves in jungles: tall trees with buttressed trunks, broad crowns and aerial roots. Craft a sapling from a jungle, an oak, a birch and an acacia sapling. A Tree Resin Collector on a rubber tree collects three times what it gets from any other tree.");
        add("jei.eln.info.tar_sand", "Rims the small oil ponds dug over some oil lakes. A Tar Sand Separator gets crude oil out of it.");
        add("jei.eln.info.crude_oil", "From oil lakes underground, through an Oil Pump, or from the small ponds over some of them. An empty bucket takes it from a pond, or 1000 mB from a pump or tank that holds that much.");
        add("jei.eln.info.fraction", "Drawn off a Fractionating Column fed with hot crude oil: each segment draws off its own cut, the base the residue. An empty bucket takes 1000 mB from any EA device holding that much.");
        add("jei.eln.info.blown_fuse", "What is left of a Lead Fuse after it blows. Take it out of the holder and put in a new fuse.");
        add("jei.eln.info.data_logger_print", "A Data Logger prints its graph when you press Print on its screen, on paper in its paper slot.");
        add("jei.eln.info.creative", "Creative only: no recipe makes it.");
        add("jei.eln.wire_drawing", "Wire Drawing");
        add("jei.eln.wire_drawing.either", "48V or 230V machine, %s kJ");
        add("jei.eln.wire_drawing.big", "230V machine only, %s kJ");
        add("jei.eln.wire_drawing.how", "Any spec: set on its screen");
        add("jei.eln.machine.macerator", "Macerator");
        add("jei.eln.machine.compressor", "Compressor");
        add("jei.eln.machine.plate_machine", "Plate Machine");
        add("jei.eln.machine.magnetiser", "Magnetizer");
        add("jei.eln.machine.separator", "Separator");
        add("jei.eln.machine.extra", "(sometimes more)");
        add("jei.eln.tree_resin_collector.how", "On the side of a log under leaves. Right-click it to empty it. A rubber tree gives three times as much.");
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
        add("advancements.eln.guide.low_pressure.description", "Your steam turbine asks for more steam than its boiler makes: the pressure falls, and it gets little from each kg. Add heat, lighten its load, or give it a regulator to hold a target pressure.");
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
        add("advancements.eln.guide.chisel.description", "It's not a wrench. Press C anyway.");
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
            add(item, name);
            // (a plain BlockItem's name is its block's: one key for both)
            if (item instanceof BlockItem b && BuiltInRegistries.BLOCK.getKey(b.getBlock()).getNamespace().equals(Eln.MODID)
                && !b.getBlock().getDescriptionId().equals(item.getDescriptionId()))
                add(b.getBlock(), name);
        }
        add(ElnRegistry.REPLICATOR.get(), "Replicator");
        add(ElnRegistry.ELECTRIC_BIKE_ENTITY.get(), "Electric Bike");
        add(ElnRegistry.TERRAIN_BIKE_ENTITY.get(), "Terrain Bike");
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
            add(b.get(), d.name);
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
        add("jei.eln.info.latex", "Rubber trees' milky sap. An Industrial Resin Collector taps it from a trunk, and a 230V Compressor presses it from dandelions; the 230V Compressor presses 250 mB of it, fed into its blue IN flange, to a Raw Rubber.");
        for (var c : mods.eln.oil.Oil.Cut.values()) {
            add("fluid.eln." + c.id, c.display);
            add("fluid_type.eln." + c.id, c.display);
            if (c.bucket) add("item.eln." + c.id + "_bucket", c.display + " Bucket");
        }
    }

    static String descriptorName(Object d) {
        if (d instanceof mods.eln.generic.GenericItemUsingDamageDescriptor g) return g.name;
        if (d instanceof mods.eln.generic.GenericItemBlockUsingDamageDescriptor g) return g.name;
        if (d instanceof mods.eln.misc.DescriptorBase b) return b.descriptorKey;
        return null;
    }
}
