package mods.eln.datagen;

import mods.eln.Eln;
import mods.eln.item.BrushDescriptor;
import mods.eln.registry.ElnDeviceRegistry;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import mods.eln.misc.OreDictTags;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Electrical Age crafting and smelting recipes (P5), made by datagen (ElnRecipeProvider): the 1.12 ElnRecipes recipe
 * methods (1.7.10 Eln.java recipes), unchanged apart from generated edits (tools/port26-recipes.py): 1.12 meta stacks
 * through wool()/dye()/planks(), renamed vanilla items, the armour/tool/converter statics. The framework (addRecipe,
 * ore names -> tags, 1.7.10 pattern fixes, recipe ids) is ElnRecipesBase. The machine lists (macerator, compressor,
 * plate machine, magnetizer) are runtime code, ElnMachineRecipes.
 */
@SuppressWarnings({"SameParameterValue", "PointlessArithmeticExpression", "unused"})
public final class ElnRecipes extends ElnRecipesBase {
    private ElnRecipes() {
    }

    /** The 1.7.10 Eln.load() recipe sequence (1.12 registerAll), minus the machine lists. */
    static void registerAll() {
        run("recipeBrush", ElnRecipes::recipeBrush);
        run("recipeEnergyConverter", ElnRecipes::recipeEnergyConverter);
        run("recipeArmor", ElnRecipes::recipeArmor);
        run("recipeTool", ElnRecipes::recipeTool);
        run("recipeGround", ElnRecipes::recipeGround);
        run("recipeElectricalSource", ElnRecipes::recipeElectricalSource);
        run("recipeElectricalCable", ElnRecipes::recipeElectricalCable);
        run("recipeThermalCable", ElnRecipes::recipeThermalCable);
        run("recipeLampSocket", ElnRecipes::recipeLampSocket);
        run("recipeLampSupply", ElnRecipes::recipeLampSupply);
        run("recipePowerSocket", ElnRecipes::recipePowerSocket);
        run("recipePassiveComponent", ElnRecipes::recipePassiveComponent);
        run("recipeSwitch", ElnRecipes::recipeSwitch);
        run("recipeWirelessSignal", ElnRecipes::recipeWirelessSignal);
        run("recipeElectricalRelay", ElnRecipes::recipeElectricalRelay);
        run("recipeElectricalDataLogger", ElnRecipes::recipeElectricalDataLogger);
        run("recipeElectricalGateSource", ElnRecipes::recipeElectricalGateSource);
        run("recipeElectricalBreaker", ElnRecipes::recipeElectricalBreaker);
        run("recipeFuses", ElnRecipes::recipeFuses);
        run("recipeElectricalVuMeter", ElnRecipes::recipeElectricalVuMeter);
        run("recipeElectricalEnvironmentalSensor", ElnRecipes::recipeElectricalEnvironmentalSensor);
        run("recipeElectricalRedstone", ElnRecipes::recipeElectricalRedstone);
        run("recipeElectricalGate", ElnRecipes::recipeElectricalGate);
        run("recipeElectricalAlarm", ElnRecipes::recipeElectricalAlarm);
        run("recipeSixNodeCache", ElnRecipes::recipeSixNodeCache);
        run("recipeElectricalSensor", ElnRecipes::recipeElectricalSensor);
        run("recipeThermalSensor", ElnRecipes::recipeThermalSensor);
        run("recipeSixNodeMisc", ElnRecipes::recipeSixNodeMisc);
        run("recipeTurret", ElnRecipes::recipeTurret);
        run("recipeMachine", ElnRecipes::recipeMachine);
        run("recipeChips", ElnRecipes::recipeChips);
        run("recipeTransformer", ElnRecipes::recipeTransformer);
        run("recipeHeatFurnace", ElnRecipes::recipeHeatFurnace);
        run("recipeTurbine", ElnRecipes::recipeTurbine);
        run("recipeBattery", ElnRecipes::recipeBattery);
        run("recipeElectricalFurnace", ElnRecipes::recipeElectricalFurnace);
        run("recipeAutoMiner", ElnRecipes::recipeAutoMiner);
        run("recipeSolarPanel", ElnRecipes::recipeSolarPanel);
        run("recipeThermalDissipatorPassiveAndActive", ElnRecipes::recipeThermalDissipatorPassiveAndActive);
        run("recipeElectricalAntenna", ElnRecipes::recipeElectricalAntenna);
        run("recipeEggIncubator", ElnRecipes::recipeEggIncubator);
        run("recipeBatteryCharger", ElnRecipes::recipeBatteryCharger);
        run("recipeTransporter", ElnRecipes::recipeTransporter);
        run("recipeWindTurbine", ElnRecipes::recipeWindTurbine);
        run("recipeFuelGenerator", ElnRecipes::recipeFuelGenerator);
        run("recipeGeneral", ElnRecipes::recipeGeneral);
        run("recipeHeatingCorp", ElnRecipes::recipeHeatingCorp);
        run("recipeRegulatorItem", ElnRecipes::recipeRegulatorItem);
        run("recipeLampItem", ElnRecipes::recipeLampItem);
        run("recipeProtection", ElnRecipes::recipeProtection);
        run("recipeCombustionChamber", ElnRecipes::recipeCombustionChamber);
        run("recipeFerromagneticCore", ElnRecipes::recipeFerromagneticCore);
        run("recipeIngot", ElnRecipes::recipeIngot);
        run("recipeDust", ElnRecipes::recipeDust);
        run("recipeElectricalMotor", ElnRecipes::recipeElectricalMotor);
        run("recipeSolarTracker", ElnRecipes::recipeSolarTracker);
        run("recipeDynamo", ElnRecipes::recipeDynamo);
        run("recipeWindRotor", ElnRecipes::recipeWindRotor);
        run("recipeMeter", ElnRecipes::recipeMeter);
        run("recipeElectricalDrill", ElnRecipes::recipeElectricalDrill);
        run("recipeOreScanner", ElnRecipes::recipeOreScanner);
        run("recipeMiningPipe", ElnRecipes::recipeMiningPipe);
        run("recipeTreeResinAndRubber", ElnRecipes::recipeTreeResinAndRubber);
        run("recipeRawCable", ElnRecipes::recipeRawCable);
        run("recipeMiscItem", ElnRecipes::recipeMiscItem);
        run("recipeBatteryItem", ElnRecipes::recipeBatteryItem);
        run("recipeElectricalTool", ElnRecipes::recipeElectricalTool);
        run("recipePortableCapacitor", ElnRecipes::recipePortableCapacitor);
        run("recipeFurnace", ElnRecipes::recipeFurnace);
        run("recipeFuelBurnerItem", ElnRecipes::recipeFuelBurnerItem);
        run("recipeECoal", ElnRecipes::recipeECoal);
        run("recipeGridDevices", ElnRecipes::recipeGridDevices);
    }

    // ------------------------------------------------------------------ recipes (1.7.10)

    /** 1.7.10 registerBrush: empty white brush from wool + iron, coloured brushes from dye + empty white brush. */
    public static void recipeBrush() {
        String[] subNames = {"Black Brush", "Red Brush", "Green Brush", "Brown Brush", "Blue Brush", "Purple Brush",
            "Cyan Brush", "Silver Brush", "Gray Brush", "Pink Brush", "Lime Brush", "Yellow Brush", "Light Blue Brush",
            "Magenta Brush", "Orange Brush", "White Brush"};
        RStack emptyStack = findItemStack("White Brush");
        if (!emptyStack.isEmpty()) emptyStack = withLife(emptyStack, 0); // 1.12 BrushDescriptor.setLife(stack, 0)

        for (int idx = 0; idx < 16; idx++) {
            addShapelessRecipe(emptyStack.copy(),
                wool(idx),
                stack(Items.IRON_INGOT));
        }

        for (int idx = 0; idx < 16; idx++) {
            addShapelessRecipe(findItemStack(subNames[idx], 1),
                dye(idx),
                emptyStack.copy());
        }
    }



    public static void recipeGround() {
        addRecipe(findItemStack("Ground Cable"),
            " C ",
            " C ",
            "CCC",
            'C', findItemStack("Copper Cable"));
    }

    public static void recipeElectricalSource() {
        // Trololol
    }

    public static void recipeElectricalCable() {
        addRecipe(stack(signalCableDescriptor, 1),
            "R",
            "C",
            'C', findItemStack("Iron Cable"),
            'R', "itemRubber");

        addRecipe(stack(lowVoltageCableDescriptor, 1),
            "R",
            "C",
            'C', findItemStack("Copper Cable"),
            'R', "itemRubber");

        addRecipe(stack(meduimVoltageCableDescriptor, 1),
            "R",
            "C",
            'C', stack(lowVoltageCableDescriptor, 1),
            'R', "itemRubber");

        addRecipe(stack(highVoltageCableDescriptor, 1),
            "R",
            "C",
            'C', stack(meduimVoltageCableDescriptor, 1),
            'R', "itemRubber");

        addRecipe(stack(signalCableDescriptor, 6),
            "RRR",
            "CCC",
            "RRR",
            'C', stack(Items.IRON_INGOT),
            'R', "itemRubber");

        addRecipe(stack(lowVoltageCableDescriptor, 6),
            "RRR",
            "CCC",
            "RRR",
            'C', "ingotCopper",
            'R', "itemRubber");


        addRecipe(stack(veryHighVoltageCableDescriptor, 6),
            "RRR",
            "CCC",
            "RRR",
            'C', "ingotAlloy",
            'R', "itemRubber");

    }

    public static void recipeThermalCable() {
        addRecipe(findItemStack("Copper Thermal Cable", 6),
            "SSS",
            "CCC",
            "SSS",
            'S', stack(Blocks.COBBLESTONE),
            'C', "ingotCopper");

        addRecipe(findItemStack("Copper Thermal Cable", 1),
            "S",
            "C",
            'S', stack(Blocks.COBBLESTONE),
            'C', findItemStack("Copper Cable"));
    }

    public static void recipeLampSocket() {
        addRecipe(findItemStack("Lamp Socket A", 3),
            "G ",
            "IG",
            "G ",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Lamp Socket B Projector", 3),
            " I",
            "IG",
            " I",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Street Light", 1),
            "G",
            "I",
            "I",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Robust Lamp Socket", 3),
            "GIG",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Flat Lamp Socket", 3),
            "IGI",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Simple Lamp Socket", 3),
            " I ",
            "GGG",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Fluorescent Lamp Socket", 3),
            " I ",
            "I I",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));


        addRecipe(findItemStack("Suspended Lamp Socket", 2),
            "I",
            "G",
            'G', findItemStack("Robust Lamp Socket"),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Long Suspended Lamp Socket", 2),
            "I",
            "I",
            "G",
            'G', findItemStack("Robust Lamp Socket"),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Sconce Lamp Socket", 2),
            "GCG",
            "GIG",
            'G', stack(Blocks.GLASS_PANE),
            'C', "dustCoal",
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("50V Emergency Lamp"),
            "cbc",
            " l ",
            " g ",
            'c', findItemStack("Low Voltage Cable"),
            'b', findItemStack("Portable Battery Pack"),
            'l', findItemStack("50V LED Bulb"),
            'g', stack(Blocks.GLASS_PANE));

        addRecipe(findItemStack("200V Emergency Lamp"),
            "cbc",
            " l ",
            " g ",
            'c', findItemStack("Medium Voltage Cable"),
            'b', findItemStack("Portable Battery Pack"),
            'l', findItemStack("200V LED Bulb"),
            'g', stack(Blocks.GLASS_PANE));
    }

    public static void recipeLampSupply() {
        addRecipe(findItemStack("Lamp Supply", 1),
            " I ",
            "ICI",
            " I ",
            'C', "ingotCopper",
            'I', stack(Items.IRON_INGOT));

    }

    public static void recipePowerSocket() {
        addRecipe(findItemStack("50V Power Socket", 16),
            "RUR",
            "ACA",
            'R', "itemRubber",
            'U', "plateCopper",
            'A', "plateAlloy",
            'C', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("200V Power Socket", 16),
            "RUR",
            "ACA",
            'R', "itemRubber",
            'U', "plateCopper",
            'A', "plateAlloy",
            'C', findItemStack("Medium Voltage Cable"));
    }

    public static void recipePassiveComponent() {
        addRecipe(findItemStack("Signal Diode", 4),
            " RB",
            "IIR",
            " RB",
            'R', stack(Items.REDSTONE),
            'I', findItemStack("Iron Cable"),
            'B', "itemRubber");

        addRecipe(findItemStack("10A Diode", 3),
            " RB",
            "IIR",
            " RB",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'B', "itemRubber");

        addRecipe(findItemStack("25A Diode"),
            "D",
            "D",
            "D",
            'D', findItemStack("10A Diode"));


        addRecipe(findItemStack("Power Capacitor"),
            "cPc",
            "III",
            'I', stack(Items.IRON_INGOT),
            'c', findItemStack("Iron Cable"),
            'P', "plateIron");

        addRecipe(findItemStack("Power Inductor"),
            " P ",
            "cIc",
            "IPI",
            'I', stack(Items.IRON_INGOT),
            'c', findItemStack("Copper Coil"),
            'P', "plateIron");

        addRecipe(findItemStack("Power Resistor"),
            " P ",
            "c c",
            "IPI",
            'I', stack(Items.IRON_INGOT),
            'c', findItemStack("Copper Coil"),
            'P', "plateCopper");

        addRecipe(findItemStack("Rheostat"),
            " R ",
            " MS",
            "cmc",
            'R', findItemStack("Power Resistor"),
            'c', findItemStack("Copper Coil"),
            'm', findItemStack("Machine Block"),
            'M', findItemStack("Electrical Motor"),
            'S', findItemStack("Signal Cable")
        );

        addRecipe(findItemStack("Thermistor"),
            " P ",
            "csc",
            "IPI",
            's', "dustSilicon",
            'I', stack(Items.IRON_INGOT),
            'c', findItemStack("Copper Coil"),
            'P', "plateCopper");

        addRecipe(findItemStack("Large Rheostat"),
            "   ",
            " D ",
            "CRC",
            'R', findItemStack("Rheostat"),
            'C', findItemStack("Copper Thermal Cable"),
            'D', findItemStack("Small Passive Thermal Dissipator")
        );
    }

    public static void recipeSwitch() {
		/*
		 * addRecipe(findItemStack("Signal Switch"), "  I", " I ", "CAC", 'R', stack(Items.REDSTONE), 'A', "itemRubber", 'I', findItemStack("Copper Cable"), 'C', findItemStack("Signal Cable"));
		 *
		 * addRecipe(findItemStack("Signal Switch with LED"), " RI", " I ", "CAC", 'R', stack(Items.REDSTONE), 'A', "itemRubber", 'I', findItemStack("Copper Cable"), 'C', findItemStack("Signal Cable"));
		 */

        addRecipe(findItemStack("Low Voltage Switch"),
            "  I",
            " I ",
            "CAC",
            'R', stack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("Medium Voltage Switch"),
            "  I",
            "AIA",
            "CAC",
            'R', stack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Medium Voltage Cable"));

        addRecipe(findItemStack("High Voltage Switch"),
            "AAI",
            "AIA",
            "CAC",
            'R', stack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("High Voltage Cable"));

        addRecipe(findItemStack("Very High Voltage Switch"),
            "AAI",
            "AIA",
            "CAC",
            'R', stack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Very High Voltage Cable"));

    }

    public static void recipeElectricalRelay() {
        addRecipe(findItemStack("Low Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', stack(Items.REDSTONE),
            'O', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("Medium Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', stack(Items.REDSTONE),
            'O', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Medium Voltage Cable"));

        addRecipe(findItemStack("High Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', stack(Items.REDSTONE),
            'O', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("High Voltage Cable"));

        addRecipe(findItemStack("Very High Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', stack(Items.REDSTONE),
            'O', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Very High Voltage Cable"));

        addRecipe(findItemStack("Signal Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', stack(Items.REDSTONE),
            'O', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE),
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Signal Cable"));
    }

    public static void recipeWirelessSignal() {
        addRecipe(findItemStack("Wireless Signal Transmitter"),
            " S ",
            " R ",
            "ICI",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'C', dictCheapChip,
            'S', findItemStack("Signal Antenna"));

        addRecipe(findItemStack("Wireless Signal Repeater"),
            "S S",
            "R R",
            "ICI",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'C', dictCheapChip,
            'S', findItemStack("Signal Antenna"));

        addRecipe(findItemStack("Wireless Signal Receiver"),
            " S ",
            "ICI",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'C', dictCheapChip,
            'S', findItemStack("Signal Antenna"));
    }

    public static void recipeChips() {
        addRecipe(findItemStack("NOT Chip"),
            "   ",
            "cCr",
            "   ",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("AND Chip"),
            " c ",
            "cCc",
            " c ",
            'C', dictCheapChip,
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("NAND Chip"),
            " c ",
            "cCr",
            " c ",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("OR Chip"),
            " r ",
            "rCr",
            " r ",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE));

        addRecipe(findItemStack("NOR Chip"),
            " r ",
            "rCc",
            " r ",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("XOR Chip"),
            " rr",
            "rCr",
            " rr",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE));

        addRecipe(findItemStack("XNOR Chip"),
            " rr",
            "rCc",
            " rr",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("PAL Chip"),
            "rcr",
            "cCc",
            "rcr",
            'C', dictAdvancedChip,
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Schmitt Trigger Chip"),
            "   ",
            "cCc",
            "   ",
            'C', dictAdvancedChip,
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("D Flip Flop Chip"),
            "   ",
            "cCc",
            " p ",
            'C', dictAdvancedChip,
            'p', "plateCopper",
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Oscillator Chip"),
            "pdp",
            "cCc",
            "   ",
            'C', dictAdvancedChip,
            'p', "plateCopper",
            'c', findItemStack("Copper Cable"),
            'd', findItemStack("Dielectric"));

        addRecipe(findItemStack("JK Flip Flop Chip"),
            " p ",
            "cCc",
            " p ",
            'C', dictAdvancedChip,
            'p', "plateCopper",
            'c', findItemStack("Copper Cable"));


        addRecipe(findItemStack("Amplifier"),
            "  r",
            "cCc",
            "   ",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("OpAmp"),
            "  r",
            "cCc",
            " c ",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Configurable summing unit"),
            " cr",
            "cCc",
            " c ",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        // (1.12 registered the VCA without a recipe; AGE's: the amplifier's, plus a signal cable for the control input)
        addRecipe(findItemStack("Voltage controlled amplifier"),
            " sr",
            "cCc",
            "   ",
            'r', stack(Items.REDSTONE),
            's', findItemStack("Signal Cable"),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);
        addRecipe(findItemStack("Sample and hold"),
            " rr",
            "cCc",
            " c ",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Voltage controlled sine oscillator"),
            "rrr",
            "cCc",
            "   ",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Voltage controlled sawtooth oscillator"),
            "   ",
            "cCc",
            "rrr",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("PID Regulator"),
            "rrr",
            "cCc",
            "rcr",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Lowpass filter"),
            "CdC",
            "cDc",
            " s ",
            'd', findItemStack("Dielectric"),
            'c', findItemStack("Copper Cable"),
            'C', "plateCopper",
            'D', findItemStack("Coal Dust"),
            's', dictCheapChip);
    }

    public static void recipeTransformer() {
        addRecipe(findItemStack("DC-DC Converter"),
            "C C",
            "III",
            'C', findItemStack("Copper Coil"),
            'I', stack(Items.IRON_INGOT));
    }

    public static void recipeHeatFurnace() {
        addRecipe(findItemStack("Stone Heat Furnace"),
            "BBB",
            "BIB",
            "BiB",
            'B', stack(Blocks.STONE),
            'i', findItemStack("Copper Thermal Cable"),
            'I', findItemStack("Combustion Chamber"));

        addRecipe(findItemStack("Fuel Heat Furnace"),
            "IcI",
            "mCI",
            "IiI",
            'c', findItemStack("Cheap Chip"),
            'm', findItemStack("Electrical Motor"),
            'C', stack(Items.CAULDRON),
            'I', stack(Items.IRON_INGOT),
            'i', findItemStack("Copper Thermal Cable"));
    }

    public static void recipeTurbine() {
        // 26.x (Baughn): the 50V/200V heat turbines are replaced by the small steam turbines (boiler + 50V/200V Steam
        // Turbine); placed ones keep working, but they're no longer made
        addRecipe(findItemStack("Generator"),
            "mmm",
            "ama",
            " ME",
            'm', findItemStack("Advanced Electrical Motor"),
            'M', findItemStack("Advanced Machine Block"),
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'E', findItemStack("High Voltage Cable")
        );
        addRecipe(findItemStack("Steam Turbine"),
            " a ",
            "aAa",
            " M ",
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'A', firstExistingOre("blockAluminum", "blockIron"),
            'M', findItemStack("Advanced Machine Block")
        );
        // Baughn 2026-10-29: the 8 kW generator (two 4 kW ones' worth, a bigger winding) and the small shaft motor
        addRecipe(findItemStack("8kW Generator"),
            "mmm",
            "GaG",
            " ME",
            'm', findItemStack("Advanced Electrical Motor"),
            'G', findItemStack("Generator"),
            'M', findItemStack("Advanced Machine Block"),
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'E', findItemStack("High Voltage Cable")
        );
        addRecipe(findItemStack("200V Shaft Motor"),
            " m ",
            "iMi",
            " E ",
            'm', findItemStack("Electrical Motor"),
            'i', stack(Items.IRON_INGOT),
            'M', findItemStack("Machine Block"),
            'E', findItemStack("Medium Voltage Cable")
        );
        // the shaft redo's plumbing (Baughn 2026-10-29): tanks and the inline pump
        addRecipe(findItemStack("Iron Tank"),
            "iPi",
            "i i",
            "iPi",
            'i', stack(Items.IRON_INGOT),
            'P', stack(Items.GLASS_PANE)
        );
        addRecipe(findItemStack("Alloy Tank"),
            "aPa",
            "aTa",
            "aPa",
            'a', findItemStack("Alloy Ingot"),
            'P', stack(Items.GLASS_PANE),
            'T', findItemStack("Iron Tank")
        );
        addRecipe(findItemStack("200V Inline Pump"),
            " m ",
            "pMp",
            " E ",
            'm', findItemStack("Electrical Motor"),
            'p', stack(Items.IRON_INGOT),
            'M', findItemStack("Machine Block"),
            'E', findItemStack("Medium Voltage Cable")
        );
        addRecipe(findItemStack("LP Steam Turbine"),
            "aAa",
            "ASA",
            "aMa",
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'A', firstExistingOre("blockAluminum", "blockIron"),
            'S', findItemStack("Steam Turbine"),
            'M', findItemStack("Advanced Machine Block")
        );
        addRecipe(findItemStack("Plant Steam Condenser"),
            "cTc",
            "cCc",
            "cMc",
            'c', findItemStack("Copper Thermal Cable"),
            'T', findItemStack("Iron Tank"),
            'C', findItemStack("Steam Condenser"),
            'M', findItemStack("Advanced Machine Block")
        );
        addRecipe(findItemStack("Counterflow Heat Exchanger"),
            "cIc",
            "ccc",
            "cIc",
            'c', findItemStack("Copper Thermal Cable"),
            'I', findItemStack("Iron Tank")
        );
        // cheap on purpose (Baughn 2026-10-29): what a water slug costs
        addRecipe(findItemStack("Turbine Blade Set"),
            "I I",
            " S ",
            "I I",
            'I', stack(Items.IRON_INGOT),
            'S', stack(Items.STICK)
        );
        addRecipe(findItemStack("Gas Turbine"),
            "msH",
            "sSs",
            " M ",
            'm', findItemStack("Advanced Electrical Motor"),
            'H', findItemStack("Copper Thermal Cable"),
            's', firstExistingOre("ingotSteel", "ingotIron"),
            'S', firstExistingOre("blockSteel", "blockIron"),
            'M', findItemStack("Advanced Machine Block")
        );

        addRecipe(findItemStack("Joint"),
            "   ",
            "iii",
            " m ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block")
        );

        addRecipe(findItemStack("Joint hub"),
            " i ",
            "iii",
            " m ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block")
        );

        addRecipe(findItemStack("Flywheel"),
            "iIi",
            "ImI",
            "iIi",
            'i', "ingotIron",
            'I', "blockIron",
            'm', findItemStack("Machine Block")
        );

        addRecipe(findItemStack("Tachometer"),
            "p  ",
            "iii",
            "cm ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block"),
            'p', findItemStack("Electrical Probe Chip"),
            'c', findItemStack("Signal Cable")
        );
    }

    public static void recipeBattery() {
        addRecipe(findItemStack("Cost Oriented Battery"),
            "C C",
            "PPP",
            "PPP",
            'C', findItemStack("Low Voltage Cable"),
            'P', "ingotLead",
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Capacity Oriented Battery"),
            "PPP",
            "PBP",
            "PPP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', "ingotLead");

        addRecipe(findItemStack("Voltage Oriented Battery"),
            "PPP",
            "PBP",
            "PPP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Current Oriented Battery"),
            "PPP",
            "PBP",
            "PPP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', "ingotCopper");

        addRecipe(findItemStack("Life Oriented Battery"),
            "P P",
            " B ",
            "P P",
            'B', findItemStack("Cost Oriented Battery"),
            'P', stack(Items.GOLD_INGOT));

        addRecipe(findItemStack("Single-use Battery"),
            "ppp",
            "III",
            "ppp",
            'C', findItemStack("Low Voltage Cable"),
            'p', stack(Items.COAL),
            'I', "ingotCopper");

        addRecipe(findItemStack("Single-use Battery"),
            "ppp",
            "III",
            "ppp",
            'C', findItemStack("Low Voltage Cable"),
            'p', stack(Items.CHARCOAL),
            'I', "ingotCopper");
    }

    /**
     * 1.12 made one Utility Pole recipe per ore name present at startup (aluminium, steel), and an iron one when there
     * was none. 26.x: the same, decided when the recipes load (NeoForge conditions on the tags).
     */
    public static void recipeGridDevices() {
        var aluminum = OreDictTags.tag("ingotAluminum");
        var steel = OreDictTags.tag("ingotSteel");
        for (var t : new String[]{"ingotAluminum", "ingotSteel"}) {
            addRecipeIf(findItemStack("Utility Pole"),
                new ICondition[]{new NotCondition(new TagEmptyCondition<>(OreDictTags.tag(t)))},
                "WWW",
                "IWI",
                " W ",
                'W', "logWood",
                'I', t
            );
        }
        // Really? (1.12: with neither, iron.) 1.7.10 forgot the 'W' key here, which made two iron ingots a pole; the E36
        // pack never loaded this fallback (it had aluminium). On 26.x without those mods it is the default, so the
        // logs the other variants use are keyed in (26.x port fix).
        addRecipeIf(findItemStack("Utility Pole"),
            new ICondition[]{new TagEmptyCondition<>(aluminum), new TagEmptyCondition<>(steel)},
            "WWW",
            "IWI",
            " W ",
            'W', "logWood",
            'I', "ingotIron"
        );
        addRecipe(findItemStack("Utility Pole w/DC-DC Converter"),
            "HHH",
            " TC",
            " PH",
            'P', findItemStack("Utility Pole"),
            'H', findItemStack("High Voltage Cable"),
            'C', findItemStack("Optimal Ferromagnetic Core"),
            'T', findItemStack("DC-DC Converter")
        );
//		if (oreNames.contains("sheetPlastic")) {
//			addRecipe(findItemStack("Downlink"),
//					"H H",
//					"PMP",
//					"PPP",
//					'P', "sheetPlastic",
//					'M', findItemStack("Machine Block"),
//					'H', findItemStack("High Voltage Cable")
//			);
//		} else {
//			addRecipe(findItemStack("Downlink"),
//					"H H",
//					"PMP",
//					"PPP",
//					'P', "itemRubber",
//					'M', findItemStack("Machine Block"),
//					'H', findItemStack("High Voltage Cable")
//			);
//		}
    }


    public static void recipeElectricalFurnace() {
        addRecipe(findItemStack("Electrical Furnace"),
            "III",
            "IFI",
            "ICI",
            'C', findItemStack("Low Voltage Cable"),
            'F', stack(Blocks.FURNACE),
            'I', stack(Items.IRON_INGOT));
    }

    public static void recipeSixNodeMisc() {
        addRecipe(findItemStack("Analog Watch"),
            "crc",
            "III",
            'c', findItemStack("Iron Cable"),
            'r', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Digital Watch"),
            "rcr",
            "III",
            'c', findItemStack("Iron Cable"),
            'r', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Hub"),
            "I I",
            " c ",
            "I I",
            'c', findItemStack("Copper Cable"),
            'I', stack(Items.IRON_INGOT));


        addRecipe(findItemStack("Energy Meter"),
            "IcI",
            "IRI",
            "IcI",
            'c', findItemStack("Copper Cable"),
            'R', dictCheapChip,
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Advanced Energy Meter"),
            " c ",
            "PRP",
            " c ",
            'c', findItemStack("Copper Cable"),
            'R', dictAdvancedChip,
            'P', "plateIron");

    }

    public static void recipeAutoMiner() {
        addRecipe(findItemStack("Auto Miner"),
            "MCM",
            "BOB",
            " P ",
            'C', dictAdvancedChip,
            'O', findItemStack("Ore Scanner"),
            'B', findItemStack("Advanced Machine Block"),
            'M', findItemStack("Advanced Electrical Motor"),
            'P', findItemStack("Mining Pipe"));
    }

    public static void recipeWindTurbine() {
        addRecipe(findItemStack("Wind Turbine"),
            " I ",
            "IMI",
            " B ",
            'B', findItemStack("Machine Block"),
            'I', "plateIron",
            'M', findItemStack("Electrical Motor"));

        addRecipe(findItemStack("Water Turbine"),
            "  I",
            "BMI",
            "  I",
            'I', "plateIron",
            'B', findItemStack("Machine Block"),
            'M', findItemStack("Electrical Motor"));

    }

    public static void recipeFuelGenerator() {
        addRecipe(findItemStack("50V Fuel Generator"),
            "III",
            " BA",
            "CMC",
            'I', "plateIron",
            'B', findItemStack("Machine Block"),
            'A', findItemStack("Analogic Regulator"),
            'C', findItemStack("Low Voltage Cable"),
            'M', findItemStack("Electrical Motor"));

        addRecipe(findItemStack("200V Fuel Generator"),
            "III",
            " BA",
            "CMC",
            'I', "plateIron",
            'B', findItemStack("Advanced Machine Block"),
            'A', findItemStack("Analogic Regulator"),
            'C', findItemStack("Medium Voltage Cable"),
            'M', findItemStack("Advanced Electrical Motor"));
    }

    public static void recipeSolarPanel() {
        addRecipe(findItemStack("Small Solar Panel"),
            "III",
            "CSC",
            "III",
            'S', "plateSilicon",
            'I', stack(Items.IRON_INGOT),
            'C', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("Small Rotating Solar Panel"),
            "ISI",
            "I I",
            'S', findItemStack("Small Solar Panel"),
            'M', findItemStack("Electrical Motor"),
            'I', stack(Items.IRON_INGOT));

        for (String metal : new String[] { "blockSteel", "blockAluminum", "blockAluminium", "casingMachineAdvanced" }) {
            for (String panel : new String[] {"Small Solar Panel", "Small Rotating Solar Panel"}) {
                addRecipe(findItemStack("2x3 Solar Panel"),
                    "PPP",
                    "PPP",
                    "I I",
                    'P', findItemStack(panel),
                    'I', metal);
            }
        }
        addRecipe(findItemStack("2x3 Rotating Solar Panel"),
            "ISI",
            "IMI",
            "I I",
            'S', findItemStack("2x3 Solar Panel"),
            'M', findItemStack("Electrical Motor"),
            'I', stack(Items.IRON_INGOT));
    }

    public static void recipeThermalDissipatorPassiveAndActive() {
        addRecipe(
            findItemStack("Small Passive Thermal Dissipator"),
            "I I",
            "III",
            "CIC",
            'I', "ingotCopper",
            'C', findItemStack("Copper Thermal Cable"));

	/*	addRecipe(
				findItemStack("Small Active Thermal Dissipator"),
				"RMR",
				"I I",
				"III",
				'I', "ingotCopper",
				'M', findItemStack("Electrical Motor"),
				'R', "itemRubber",
				'C', findItemStack("Copper Thermal Cable"));*/

        addRecipe(
            findItemStack("Small Active Thermal Dissipator"),
            "RMR",
            " D ",
            'D', findItemStack("Small Passive Thermal Dissipator"),
            'M', findItemStack("Electrical Motor"),
            'R', "itemRubber");

	/*	addRecipe(
				findItemStack("200V Active Thermal Dissipator"),
				"RMR",
				"I I",
				"III",
				'I', "ingotCopper",
				'M', findItemStack("Advanced Electrical Motor"),
				'R', "itemRubber",
				'C', findItemStack("Copper Thermal Cable"));*/

        addRecipe(
            findItemStack("200V Active Thermal Dissipator"),
            "RMR",
            " D ",
            'D', findItemStack("Small Passive Thermal Dissipator"),
            'M', findItemStack("Advanced Electrical Motor"),
            'R', "itemRubber");

    }

    public static void recipeGeneral() {
        addSmelting(treeResin.parentItem,
            treeResin.parentItemDamage, findItemStack("Rubber", 1), 0f);

    }

    public static void recipeHeatingCorp() {
        addRecipe(findItemStack("Small 50V Copper Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Copper Coil"));

        addRecipe(findItemStack("50V Copper Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Copper Coil"));

        addRecipe(findItemStack("Small 200V Copper Heating Corp"),
            "CC",
            'C', findItemStack("50V Copper Heating Corp"));

        addRecipe(findItemStack("200V Copper Heating Corp"),
            "CC",
            'C', findItemStack("Small 200V Copper Heating Corp"));

        addRecipe(findItemStack("Small 50V Iron Heating Corp"),
            "C C",
            "CCC",
            "C C", 'C', findItemStack("Iron Cable"));

        addRecipe(findItemStack("50V Iron Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Small 200V Iron Heating Corp"),
            "CC",
            'C', findItemStack("50V Iron Heating Corp"));

        addRecipe(findItemStack("200V Iron Heating Corp"),
            "CC",
            'C', findItemStack("Small 200V Iron Heating Corp"));

        addRecipe(findItemStack("Small 50V Tungsten Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Tungsten Cable"));

        addRecipe(findItemStack("50V Tungsten Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Tungsten Ingot"));

        addRecipe(findItemStack("Small 200V Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("50V Tungsten Heating Corp"));
        addRecipe(findItemStack("200V Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("Small 200V Tungsten Heating Corp"));
    }

    public static void recipeRegulatorItem() {
        addRecipe(findItemStack("On/OFF Regulator 10 Percent", 1),
            "R R",
            " R ",
            " I ",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("On/OFF Regulator 1 Percent", 1),
            "RRR",
            " I ",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Analogic Regulator", 1),
            "R R",
            " C ",
            " I ",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'C', dictCheapChip);
    }

    public static void recipeLampItem() {
        // Tungsten
        addRecipe(
            findItemStack("Small 50V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("50V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("200V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Medium Voltage Cable"));

        // CARBON
        addRecipe(findItemStack("Small 50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.COAL),
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Small 50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.CHARCOAL),
            'S', findItemStack("Copper Cable"));

        addRecipe(
            findItemStack("50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.COAL),
            'S', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.CHARCOAL),
            'S', findItemStack("Low Voltage Cable"));

        addRecipe(
            findItemStack("Small 50V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.GLOWSTONE_DUST),
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("50V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.GLOWSTONE_DUST),
            'S', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("200V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.GLOWSTONE_DUST),
            'S', findItemStack("Medium Voltage Cable"));

        addRecipe(findItemStack("50V Farming Lamp", 2),
            "GGG",
            "FFF",
            "GSG",
            'G', stack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("200V Farming Lamp", 2),
            "GGG",
            "FFF",
            "GSG",
            'G', stack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Medium Voltage Cable"));

        addRecipe(findItemStack("50V LED Bulb", 2),
            "GGG",
            "SSS",
            " C ",
            'G', stack(Blocks.GLASS_PANE),
            'S', findItemStack("Silicon Ingot"),
            'C', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("200V LED Bulb", 2),
            "GGG",
            "SSS",
            " C ",
            'G', stack(Blocks.GLASS_PANE),
            'S', findItemStack("Silicon Ingot"),
            'C', findItemStack("Medium Voltage Cable"));

    }

    public static void recipeProtection() {
        addRecipe(findItemStack("Overvoltage Protection", 4),
            "SCD",
            'S', findItemStack("Electrical Probe Chip"),
            'C', dictCheapChip,
            'D', stack(Items.REDSTONE));

        addRecipe(findItemStack("Overheating Protection", 4),
            "SCD",
            'S', findItemStack("Thermal Probe Chip"),
            'C', dictCheapChip,
            'D', stack(Items.REDSTONE));

    }

    public static void recipeCombustionChamber() {
        addRecipe(findItemStack("Combustion Chamber"),
            " L ",
            "L L",
            " L ",
            'L', stack(Blocks.STONE));
        addRecipe(findItemStack("Thermal Insulation", 4),
            "WSW",
            "SWS",
            "WSW",
            'S', stack(Blocks.STONE),
            'W', net.minecraft.tags.ItemTags.WOOL);   // any colour (Baughn 2026-10-26; 1.12: white)
    }

    public static void recipeFerromagneticCore() {
        addRecipe(findItemStack("Cheap Ferromagnetic Core"),
            "LLL",
            "L  ",
            "LLL",
            'L', Items.IRON_INGOT);

        addRecipe(findItemStack("Average Ferromagnetic Core"),
            "PCP",
            'C', findItemStack("Cheap Ferromagnetic Core"),
            'P', "plateIron");

        addRecipe(findItemStack("Optimal Ferromagnetic Core"),
            "P",
            "C",
            "P",
            'C', findItemStack("Average Ferromagnetic Core"),
            'P', "plateIron");
    }

    public static void recipeIngot() {
        // Done
    }

    public static void recipeDust() {
        addShapelessRecipe(findItemStack("Alloy Dust", 2),
            "dustIron",
            "dustIron",
            "dustCoal",
            dictTungstenDust);

    }

    public static void recipeElectricalMotor() {
        addRecipe(findItemStack("Electrical Motor"),
            " C ",
            "III",
            "C C",
            'I', stack(Items.IRON_INGOT),
            'C', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("Advanced Electrical Motor"),
            "RCR",
            "MIM",
            "CRC",
            'M', findItemStack("Advanced Magnet"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE),
            'C', findItemStack("Medium Voltage Cable"));

        // TODO

    }

    public static void recipeSolarTracker() {
        addRecipe(findItemStack("Solar Tracker", 4),
            "VVV",
            "RQR",
            "III",
            'Q', stack(Items.QUARTZ),
            'V', stack(Blocks.GLASS_PANE),
            'R', stack(Items.REDSTONE),
            'G', stack(Items.GOLD_INGOT),
            'I', stack(Items.IRON_INGOT));

    }

    public static void recipeDynamo() {

    }

    public static void recipeWindRotor() {

    }

    public static void recipeMeter() {
        addRecipe(findItemStack("MultiMeter"),
            "RGR",
            "RER",
            "RCR",
            'G', stack(Blocks.GLASS_PANE),
            'C', findItemStack("Electrical Probe Chip"),
            'E', stack(Items.REDSTONE),
            'R', "itemRubber");

        addRecipe(findItemStack("Thermometer"),
            "RGR",
            "RER",
            "RCR",
            'G', stack(Blocks.GLASS_PANE),
            'C', findItemStack("Thermal Probe Chip"),
            'E', stack(Items.REDSTONE),
            'R', "itemRubber");

        addShapelessRecipe(findItemStack("AllMeter"),
            findItemStack("MultiMeter"),
            findItemStack("Thermometer"));

        addRecipe(findItemStack("Wireless Analyser"),
            " S ",
            "RGR",
            "RER",
            'G', stack(Blocks.GLASS_PANE),
            'S', findItemStack("Signal Antenna"),
            'E', stack(Items.REDSTONE),
            'R', "itemRubber");

    }

    public static void recipeElectricalDrill() {
        addRecipe(findItemStack("Cheap Electrical Drill"),
            "CMC",
            " T ",
            " P ",
            'T', findItemStack("Mining Pipe"),
            'C', dictCheapChip,
            'M', findItemStack("Electrical Motor"),
            'P', stack(Items.IRON_PICKAXE));

        addRecipe(findItemStack("Average Electrical Drill"),
            "RCR",
            " D ",
            " d ", 'R', Items.REDSTONE,
            'C', dictCheapChip,
            'D', findItemStack("Cheap Electrical Drill"),
            'd', stack(Items.DIAMOND));

        addRecipe(findItemStack("Fast Electrical Drill"),
            "MCM",
            " T ",
            " P ",
            'T', findItemStack("Mining Pipe"),
            'C', dictAdvancedChip,
            'M', findItemStack("Advanced Electrical Motor"),
            'P', stack(Items.DIAMOND_PICKAXE));

    }

    public static void recipeOreScanner() {
        addRecipe(findItemStack("Ore Scanner"),
            "IGI",
            "RCR",
            "IGI",
            'C', dictCheapChip,
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'G', stack(Items.GOLD_INGOT));

    }

    public static void recipeMiningPipe() {
        addRecipe(findItemStack("Mining Pipe", 4),
            "A",
            "A",
            "A",
            'A', "ingotAlloy");
    }

    public static void recipeTreeResinAndRubber() {
        addRecipe(findItemStack("Tree Resin Collector"),
            "W W",
            "WW ", 'W', "plankWood");

        addRecipe(findItemStack("Tree Resin Collector"),
            "W W",
            " WW", 'W', "plankWood");

    }

    public static void recipeRawCable() {
        // 26.x (Baughn): copper cable is 1 per ingot by hand; the thin Copper Coil too, wound on a stick
        addRecipe(findItemStack("Copper Cable", 3),
            "III",
            'I', "ingotCopper");

        addRecipe(findItemStack("Copper Coil", 4),
            " I ",
            "ISI",
            " I ",
            'I', "ingotCopper",
            'S', stack(Items.STICK));

        addRecipe(findItemStack("Iron Cable", 6),
            "III",
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Tungsten Cable", 6),
            "III",
            'I', dictTungstenIngot);

    }

    public static void recipeBatteryItem() {
        addRecipe(findItemStack("Portable Battery"),
            " I ",
            "IPI",
            "IPI",
            'P', "ingotLead",
            'I', stack(Items.IRON_INGOT));
        // (the Portable Battery Pack: BatteryPackRecipe, which keeps the batteries' charge; its JSON is in main resources)
    }

    public static void recipeElectricalTool() {
        addRecipe(findItemStack("Small Flashlight"),
            "GLG",
            "IBI",
            " I ",
            'L', findItemStack("50V Incandescent Light Bulb"),
            'B', findItemStack("Portable Battery"),
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Improved Flashlight"),
            "GLG",
            "IBI",
            " I ",
            'L', findItemStack("50V LED Bulb"),
            'B', findItemStack("Portable Battery Pack"),
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));

        // the headlamp (Baughn 2026-10-30): a small flashlight on a leather band
        addShapelessRecipe(stack(mods.eln.registry.ElnRegistry.HEADLAMP.get()),
            findItemStack("Small Flashlight"), stack(Items.LEATHER), stack(Items.LEATHER), stack(Items.STRING));

        addRecipe(findItemStack("Portable Electrical Mining Drill"),
            " T ",
            "IBI",
            " I ",
            'T', findItemStack("Average Electrical Drill"),
            'B', findItemStack("Portable Battery"),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Portable Electrical Axe"),
            " T ",
            "IMI",
            "IBI",
            'T', stack(Items.IRON_AXE),
            'B', findItemStack("Portable Battery"),
            'M', findItemStack("Electrical Motor"),
            'I', stack(Items.IRON_INGOT));

        if (xRayScannerCanBeCrafted) {
            addRecipe(findItemStack("X-Ray Scanner"),
                "PGP",
                "PCP",
                "PBP",
                'C', dictAdvancedChip,
                'B', findItemStack("Portable Battery"),
                'P', stack(Items.IRON_INGOT),
                'G', findItemStack("Ore Scanner"));
        }

    }

    public static void recipeECoal() {
        addRecipe(findItemStack("E-Coal Helmet"),
            "PPP",
            "PCP",
            'P', "plateCoal",
            'C', dictAdvancedChip);
        addRecipe(findItemStack("E-Coal Boots"),
            " C ",
            "P P",
            "P P",
            'P', "plateCoal",
            'C', dictAdvancedChip);

        addRecipe(findItemStack("E-Coal Chestplate"),
            "P P",
            "PCP",
            "PPP",
            'P', "plateCoal",
            'C', dictAdvancedChip);

        addRecipe(findItemStack("E-Coal Leggings"),
            "PPP",
            "PCP",
            "P P",
            'P', "plateCoal",
            'C', dictAdvancedChip);

    }

    public static void recipePortableCapacitor() {
        addRecipe(findItemStack("Portable Condensator"),
            "RcR",
            "wCw",
            "RcR",
            'C', stack(Items.REDSTONE),
            'R', "itemRubber",
            'w', findItemStack("Copper Cable"),
            'c', "plateCopper");

        addShapelessRecipe(findItemStack("Portable Condensator Pack"),
            findItemStack("Portable Condensator"),
            findItemStack("Portable Condensator"),
            findItemStack("Portable Condensator"));
    }

    public static void recipeMiscItem() {
        addRecipe(findItemStack("Cheap Chip"),
            " R ",
            "RSR",
            " R ",
            'S', "ingotSilicon",
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("Advanced Chip"),
            "LRL",
            "RCR",
            "LRL",
            'C', dictCheapChip,
            'L', "ingotSilicon",
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Machine Block"),
            "LLL",
            "LcL",
            "LLL",
            'L', stack(Items.IRON_INGOT),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Advanced Machine Block"),
            " C ",
            "CcC",
            " C ",
            'C', "plateAlloy",
            'L', "ingotAlloy",
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Electrical Probe Chip"),
            " R ",
            "RCR",
            " R ",
            'C', findItemStack("High Voltage Cable"),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Thermal Probe Chip"),
            " C ",
            "RIR",
            " C ",
            'G', stack(Items.GOLD_INGOT),
            'I', stack(Items.IRON_INGOT),
            'C', "ingotCopper",
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Signal Antenna"),
            "c",
            "c",
            'c', findItemStack("Iron Cable"));

        addRecipe(findItemStack("Machine Booster"),
            "m",
            "c",
            "m",
            'm', findItemStack("Electrical Motor"),
            'c', dictAdvancedChip);

        addRecipe(findItemStack("Wrench"),
            " c ",
            "cc ",
            "  c",
            'c', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Player Filter"),
            " g",
            "gc",
            " g",
            'g', stack(Blocks.GLASS_PANE),
            'c', dye(2));

        addRecipe(findItemStack("Monster Filter"),
            " g",
            "gc",
            " g",
            'g', stack(Blocks.GLASS_PANE),
            'c', dye(1));

        // (1.12 registered the Animal Filter without a recipe; AGE's: blue dye, as the others' colours)
        addRecipe(findItemStack("Animal Filter"),
            " g",
            "gc",
            " g",
            'g', stack(Blocks.GLASS_PANE),
            'c', dye(4));

        addRecipe(findItemStack("Casing", 8),
            "ppp",
            "p p",
            "ppp",
            'p', "plateIron");

    }













    public static void recipeFuelBurnerItem() {
        addRecipe(findItemStack("Small Fuel Burner"),
            "   ",
            " Cc",
            "   ",
            'C', findItemStack("Combustion Chamber"),
            'c', findItemStack("Copper Thermal Cable"));

        addRecipe(findItemStack("Medium Fuel Burner"),
            "   ",
            " Cc",
            " C ",
            'C', findItemStack("Combustion Chamber"),
            'c', findItemStack("Copper Thermal Cable"));

        addRecipe(findItemStack("Big Fuel Burner"),
            "   ",
            "CCc",
            "CC ",
            'C', findItemStack("Combustion Chamber"),
            'c', findItemStack("Copper Thermal Cable"));
    }

    public static void recipeFurnace() {
        RStack in;

        in = findItemStack("dustCopper");
        addSmelting(in.getItem(), 0,
            stack(Items.COPPER_INGOT)); // 26.x: EA's copper ingot is gone
        in = findItemStack("Lead Ore");
        addSmelting(in.getItem(), 0,
            findItemStack("ingotLead"));
        in = findItemStack("Deepslate Lead Ore");
        addSmelting(in.getItem(), 0,
            findItemStack("ingotLead"));
        in = findItemStack("dustLead");
        addSmelting(in.getItem(), 0,
            findItemStack("ingotLead"));
        in = findItemStack("Tungsten Ore");
        addSmelting(in.getItem(), 0,
            findItemStack("Tungsten Ingot"));
        in = findItemStack("Tungsten Dust");
        addSmelting(in.getItem(), 0,
            findItemStack("Tungsten Ingot"));
        // raw lead and tungsten (their ores' drops, as raw iron), smelted and blasted
        addSmelting(mods.eln.registry.ElnRegistry.RAW_LEAD.get(), 0, findItemStack("ingotLead"));
        addSmelting(mods.eln.registry.ElnRegistry.RAW_TUNGSTEN.get(), 0, findItemStack("Tungsten Ingot"));
        addBlasting(mods.eln.registry.ElnRegistry.RAW_LEAD.get(), findItemStack("ingotLead"), 0.7f);
        addBlasting(mods.eln.registry.ElnRegistry.RAW_TUNGSTEN.get(), findItemStack("Tungsten Ingot"), 0.7f);
        // charcoal nuggets (an electrical oven's burnt food): four to a charcoal, and back
        var nugget = mods.eln.registry.ElnRegistry.CHARCOAL_NUGGET.get();
        addShapelessRecipe(stack(net.minecraft.world.item.Items.CHARCOAL), stack(nugget), stack(nugget), stack(nugget), stack(nugget));
        addShapelessRecipe(stack(nugget, 4), stack(net.minecraft.world.item.Items.CHARCOAL));
        in = findItemStack("ingotAlloy");
        // addSmelting(in.getItem().itemID, 0,
        // findItemStack("Ferrite Ingot"));
        in = findItemStack("dustIron");
        addSmelting(in.getItem(), 0,
            stack(Items.IRON_INGOT));

        in = findItemStack("dustGold");
        addSmelting(in.getItem(), 0,
            stack(Items.GOLD_INGOT));

        in = findItemStack("Tree Resin");
        addSmelting(in.getItem(), 0,
            findItemStack("Rubber", 2));

        in = findItemStack("Alloy Dust");
        addSmelting(in.getItem(), 0,
            findItemStack("Alloy Ingot"));

        in = findItemStack("Silicon Dust");
        addSmelting(in.getItem(), 0,
            findItemStack("Silicon Ingot"));

        // in = findItemStack("Purified Cinnabar Dust");
        in = findItemStack("dustCinnabar");
        addSmelting(in.getItem(), 0,
            findItemStack("Mercury"));

    }

    public static void recipeElectricalSensor() {
        addRecipe(findItemStack("Voltage Probe", 1),
            "SC",
            'S', findItemStack("Electrical Probe Chip"),
            'C', findItemStack("Signal Cable"));

        addRecipe(findItemStack("Electrical Probe", 1),
            "SCS",
            'S', findItemStack("Electrical Probe Chip"),
            'C', findItemStack("Signal Cable"));

    }

    public static void recipeThermalSensor() {
        addRecipe(findItemStack("Thermal Probe", 1),
            "SCS",
            'S', findItemStack("Thermal Probe Chip"),
            'C', findItemStack("Signal Cable"));

        addRecipe(findItemStack("Temperature Probe", 1),
            "SC",
            'S', findItemStack("Thermal Probe Chip"),
            'C', findItemStack("Signal Cable"));

    }

    public static void recipeTransporter() {
        addRecipe(findItemStack("Experimental Transporter", 1),
            "RMR",
            "RMR",
            " R ",
            'M', findItemStack("Advanced Machine Block"),
            'C', findItemStack("High Voltage Cable"),
            'R', dictAdvancedChip);
    }


    public static void recipeTurret() {
        addRecipe(findItemStack("800V Defence Turret", 1),
            " R ",
            "CMC",
            " c ",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', stack(highVoltageCableDescriptor, 1),
            'R', stack(Blocks.REDSTONE_BLOCK));

    }

    public static void recipeMachine() {
        addRecipe(findItemStack("50V Macerator", 1),
            "IRI",
            "FMF",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'F', stack(Items.FLINT),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("200V Macerator", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', stack(Items.DIAMOND),
            'I', "ingotAlloy");

        // 26.x oil: tar sand separators (hot-water extraction: a heated, stirred tank)
        addRecipe(findItemStack("50V Tar Sand Separator", 1),
            "IBI",
            "HMH",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'H', findItemStack("50V Copper Heating Corp"),
            'B', stack(Items.BUCKET),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("200V Tar Sand Separator", 1),
            "IBI",
            "HMH",
            "IcI",
            'M', findItemStack("Advanced Machine Block"),
            'c', findItemStack("Advanced Electrical Motor"),
            'H', findItemStack("200V Copper Heating Corp"),
            'B', stack(Items.BUCKET),
            'I', "ingotAlloy");

        addRecipe(stack(mods.eln.oil.OilWorldgen.WELL_CASING_ITEM.get(), 4),
            "I I",
            "I I",
            "I I",
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("50V Oil Pump", 1),
            "IPI",
            "cMc",
            "IWI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'P', stack(Blocks.PISTON),
            'W', stack(mods.eln.oil.OilWorldgen.WELL_CASING_ITEM.get()),
            'I', stack(Items.IRON_INGOT));
        // the steam injection wellhead (todo #138): an alloy pipe in, a valve, well casing down
        addRecipe(findItemStack("Steam Injection Wellhead", 1),
            " p ",
            "IvI",
            "IWI",
            'p', stack(mods.eln.registry.ElnRegistry.PIPE_KIT_ALLOY.get()),
            'v', stack(mods.eln.registry.ElnRegistry.PIPE_VALVE.get()),
            'W', stack(mods.eln.oil.OilWorldgen.WELL_CASING_ITEM.get()),
            'I', "ingotAlloy");
        // the submersible pumps (Baughn 2026-10-25): a motor in a can, an intake screen, a pipe out of the top
        addRecipe(findItemStack("50V Submersible Pump", 1),
            " p ",
            "IcI",
            "IBI",
            'p', stack(mods.eln.registry.ElnRegistry.PIPE_KIT_IRON.get()),
            'c', findItemStack("Electrical Motor"),
            'B', stack(Items.IRON_BARS),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("200V Submersible Pump", 1),
            " p ",
            "IcI",
            "IBI",
            'p', stack(mods.eln.registry.ElnRegistry.PIPE_KIT_IRON.get()),
            'c', findItemStack("Advanced Electrical Motor"),
            'B', stack(Items.IRON_BARS),
            'I', "ingotAlloy");
        addRecipe(findItemStack("200V Oil Pump", 1),
            "IPI",
            "cMc",
            "IWI",
            'M', findItemStack("Advanced Machine Block"),
            'c', findItemStack("Advanced Electrical Motor"),
            'P', stack(Blocks.PISTON),
            'W', stack(mods.eln.oil.OilWorldgen.WELL_CASING_ITEM.get()),
            'I', "ingotAlloy");

        // electric heaters: heating corps around a thermal cable out of the back; the boiler: a kettle on a thermal face
        addRecipe(findItemStack("50V Electric Heater", 1),
            "IHI",
            "HCH",
            "ItI",
            'H', findItemStack("50V Copper Heating Corp"),
            'C', findItemStack("Machine Block"),
            't', findItemStack("Copper Thermal Cable"),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("200V Electric Heater", 1),
            "IHI",
            "HCH",
            "ItI",
            'H', findItemStack("200V Copper Heating Corp"),
            'C', findItemStack("Advanced Machine Block"),
            't', findItemStack("Copper Thermal Cable"),
            'I', "ingotAlloy");
        addRecipe(findItemStack("50V Steam Turbine", 1),
            " m ",
            "IMI",
            " E ",
            'M', findItemStack("Machine Block"),
            'E', findItemStack("Low Voltage Cable"),
            'I', stack(Items.IRON_INGOT),
            'm', findItemStack("Electrical Motor"));
        addRecipe(findItemStack("200V Steam Turbine", 1),
            "ImI",
            "IMI",
            "IEI",
            'I', "itemRubber",
            'M', findItemStack("Advanced Machine Block"),
            'E', findItemStack("Medium Voltage Cable"),
            'm', findItemStack("Advanced Electrical Motor"));
        // the bootstrap turbine (Baughn 2026-10-27): cheaper than the 50V Steam Turbine and no boiler to buy; a bucket for
        // its tank, copper plates for its condenser
        addRecipe(findItemStack("50V Compact Steam Turbine", 1),
            "CBC",
            "ImI",
            "IEI",
            'C', "plateCopper",
            'B', stack(Items.BUCKET),
            'I', stack(Items.IRON_INGOT),
            'm', findItemStack("Electrical Motor"),
            'E', findItemStack("Low Voltage Cable"));
        // the electrical anvil (Baughn 2026-10-28: late-midgame gear): an anvil fed by 800 V cable on an advanced machine
        // block, a steel-plate top and two legs
        addRecipe(findItemStack("Electrical Anvil", 1),
            "HAH",
            "pMp",
            "i i",
            'H', findItemStack("High Voltage Cable"),
            'A', stack(Items.ANVIL),
            'p', "plateIron",
            'M', findItemStack("Advanced Machine Block"),
            'i', "ingotIron");
        // the capacitor bank (Baughn 2026-10-28: lots of dielectrics): six cells, each six Dielectrics round a block of
        // redstone between copper foils; the bank is six cells over an iron base with its 800 V lead
        addRecipe(stack(mods.eln.registry.ElnRegistry.CAPACITOR_CELL.get()),
            "dpd",
            "dRd",
            "dpd",
            'd', findItemStack("Dielectric"),
            'p', "plateCopper",
            'R', stack(Items.REDSTONE_BLOCK));
        addRecipe(findItemStack("Capacitor Bank", 1),
            "CCC",
            "CCC",
            "pHp",
            'C', stack(mods.eln.registry.ElnRegistry.CAPACITOR_CELL.get()),
            'p', "plateIron",
            'H', findItemStack("High Voltage Cable"));
        // the electric bike (Baughn 2026-10-28): a chest for its trunk and a leather saddle, an iron frame round an electrical
        // motor, two rubber wheels
        addRecipe(stack(mods.eln.registry.ElnRegistry.ELECTRIC_BIKE.get()),
            "CL ",
            "ImI",
            "R R",
            'C', stack(Items.CHEST),
            'L', stack(Items.LEATHER),
            'I', stack(Items.IRON_INGOT),
            'm', findItemStack("Electrical Motor"),
            'R', "itemRubber");
        // the terrain bike (Baughn 2026-10-29: much dearer): an electric bike rebuilt round a 200 V motor in an advanced
        // machine block, alloy plates for its frame, more rubber for its tyres
        addRecipe(stack(mods.eln.registry.ElnRegistry.TERRAIN_BIKE.get()),
            "PmP",
            "RBR",
            " M ",
            'P', "plateAlloy",
            'm', findItemStack("Advanced Electrical Motor"),
            'R', "itemRubber",
            'B', stack(mods.eln.registry.ElnRegistry.ELECTRIC_BIKE.get()),
            'M', findItemStack("Advanced Machine Block"));
        // the turbines' condensers (Baughn 2026-10-26): a shell of copper tubes (the cooler) round a vessel, and a pump
        addRecipe(findItemStack("Small Steam Condenser", 1),
            "PCP",
            "CMC",
            "PmP",
            'P', "plateIron",
            'C', "plateCopper",
            'M', findItemStack("Machine Block"),
            'm', findItemStack("Electrical Motor"));
        addRecipe(findItemStack("Steam Condenser", 1),
            "PCP",
            "CMC",
            "PmP",
            'P', "plateAlloy",
            'C', "plateCopper",
            'M', findItemStack("Advanced Machine Block"),
            'm', findItemStack("Advanced Electrical Motor"));
        // the steam oven (Baughn 2026-10-23): an iron box with a glass door, a pipe in on top and a fan
        addRecipe(findItemStack("Steam Oven", 1),
            "IpI",
            "GMG",
            "IEI",
            'I', stack(Items.IRON_INGOT),
            'p', stack(mods.eln.registry.ElnRegistry.PIPE_KIT_IRON.get()),
            'G', stack(Items.GLASS_PANE),
            'M', findItemStack("Machine Block"),
            'E', findItemStack("Electrical Motor"));
        // the smelter (Baughn 2026-10-23): a furnace in a brick jacket on a thermal cable, with a hopper's funnel on top
        addRecipe(findItemStack("Smelter", 1),
            "BHB",
            "BFB",
            "BtB",
            'B', stack(Items.BRICKS),
            'H', stack(Items.HOPPER),
            'F', stack(Items.FURNACE),
            't', findItemStack("Copper Thermal Cable"));
        addRecipe(findItemStack("Fractionating Column Base", 1),
            "IBI",
            "ICI",
            "ItI",
            'B', stack(Items.BUCKET),
            'C', findItemStack("Machine Block"),
            't', findItemStack("Copper Thermal Cable"),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Fractionating Column Segment", 2),
            "IGI",
            "B B",
            "IGI",
            'B', stack(Items.BUCKET),
            'G', stack(Blocks.GLASS),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Flare Stack", 1),
            "IFI",
            "I I",
            "IBI",
            'F', stack(Items.FLINT_AND_STEEL),
            'B', stack(Items.BUCKET),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Boiler", 1),
            "IBI",
            "ICI",
            "ItI",
            'B', stack(Items.BUCKET),
            'C', stack(Items.CAULDRON),
            't', findItemStack("Copper Thermal Cable"),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("50V Compressor", 1),
            "IRI",
            "FMF",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'F', "plateIron",
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("200V Compressor", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', "plateAlloy",
            'I', "ingotAlloy");

        addRecipe(findItemStack("50V Plate Machine", 1),
            "IRI",
            "IMI",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("200V Plate Machine", 1),
            "DCD",
            "DMD",
            "DcD",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', "plateAlloy",
            'I', "ingotAlloy");

        addRecipe(findItemStack("50V Magnetizer", 1),
            "IRI",
            "cMc",
            "III",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("200V Magnetizer", 1),
            "ICI",
            "cMc",
            "III",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'I', "ingotAlloy");

    }

    public static void recipeElectricalGate() {
        addShapelessRecipe(findItemStack("Electrical Timer"),
            stack(Items.REPEATER),
            dictCheapChip);

        addRecipe(findItemStack("Signal Processor", 1),
            "IcI",
            "cCc",
            "IcI",
            'I', stack(Items.IRON_INGOT),
            'c', findItemStack("Signal Cable"),
            'C', dictCheapChip);
    }

    public static void recipeElectricalRedstone() {
        addRecipe(findItemStack("Redstone-to-Voltage Converter", 1),
            "TCS",
            'S', findItemStack("Signal Cable"),
            'C', dictCheapChip,
            'T', stack(Blocks.REDSTONE_TORCH));

        addRecipe(findItemStack("Voltage-to-Redstone Converter", 1),
            "CTR",
            'R', stack(Items.REDSTONE),
            'C', dictCheapChip,
            'T', stack(Blocks.REDSTONE_TORCH));

    }

    public static void recipeElectricalEnvironmentalSensor() {
        addShapelessRecipe(findItemStack("Electrical Daylight Sensor"),
            stack(Blocks.DAYLIGHT_DETECTOR),
            findItemStack("Redstone-to-Voltage Converter"));

        addShapelessRecipe(findItemStack("Electrical Light Sensor"),
            stack(Blocks.DAYLIGHT_DETECTOR),
            stack(Items.QUARTZ),
            findItemStack("Redstone-to-Voltage Converter"));

        addRecipe(findItemStack("Electrical Weather Sensor"),
            " r ",
            "rRr",
            " r ",
            'R', stack(Items.REDSTONE),
            'r', "itemRubber");

        addRecipe(findItemStack("Electrical Anemometer Sensor"),
            " I ",
            " R ",
            "I I",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Electrical Entity Sensor"),
            " G ",
            "GRG",
            " G ",
            'G', stack(Blocks.GLASS_PANE),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Electrical Fire Detector"),
            "cbr",
            "p p",
            "r r",
            'c', findItemStack("Signal Cable"),
            'b', dictCheapChip,
            'r', "itemRubber",
            'p', "plateCopper");

        addRecipe(findItemStack("Electrical Fire Buzzer"),
            "rar",
            "p p",
            "r r",
            'a', dictAdvancedChip,
            'r', "itemRubber",
            'p', "plateCopper");

        addShapelessRecipe(findItemStack("Scanner"),
            stack(Items.COMPARATOR),
            dictAdvancedChip);

    }

    public static void recipeElectricalVuMeter() {
        for (int idx = 0; idx < 4; idx++) {
            addRecipe(findItemStack("Analog vuMeter", 1),
                "WWW",
                "RIr",
                "WSW",
                'W', planks(idx),
                'R', stack(Items.REDSTONE),
                'I', stack(Items.IRON_INGOT),
                'r', dye(1),
                'S', findItemStack("Signal Cable"));
        }
        for (int idx = 0; idx < 4; idx++) {
            addRecipe(findItemStack("LED vuMeter", 1),
                " W ",
                "WTW",
                " S ",
                'W', planks(idx),
                'T', stack(Blocks.REDSTONE_TORCH),
                'S', findItemStack("Signal Cable"));
        }
    }

    public static void recipeElectricalBreaker() {

        addRecipe(findItemStack("Electrical Breaker", 1),
            "crC",
            'c', findItemStack("Overvoltage Protection"),
            'C', findItemStack("Overheating Protection"),
            'r', findItemStack("High Voltage Relay"));

    }

    public static void recipeFuses() {

        addRecipe(findItemStack("Electrical Fuse Holder", 1),
            "i",
            " ",
            "i",
            'i', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Lead Fuse for low voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("Lead Fuse for medium voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Medium Voltage Cable"));

        addRecipe(findItemStack("Lead Fuse for high voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("High Voltage Cable"));

        addRecipe(findItemStack("Lead Fuse for very high voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Very High Voltage Cable"));

    }

    public static void recipeElectricalGateSource() {
        addRecipe(findItemStack("Signal Trimmer", 1),
            "RsR",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            's', stack(Items.STICK),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Signal Switch", 3),
            " r ",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Signal Button", 3),
            " R ",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Wireless Switch", 3),
            " a ",
            "rCr",
            " r ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'C', dictCheapChip,
            'a', findItemStack("Signal Antenna"),
            'r', "itemRubber",
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Wireless Button", 3),
            " a ",
            "rCr",
            " R ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'C', dictCheapChip,
            'a', findItemStack("Signal Antenna"),
            'r', "itemRubber",
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        // Wireless Switch
        // Wireless Button
    }

    public static void recipeElectricalDataLogger() {
        addRecipe(findItemStack("Data Logger", 1),
            "RRR",
            "RGR",
            "RCR",
            'R', "itemRubber",
            'C', dictCheapChip,
            'G', stack(Blocks.GLASS_PANE));

        addRecipe(findItemStack("Modern Data Logger", 1),
            "RRR",
            "RGR",
            "RCR",
            'R', "itemRubber",
            'C', dictAdvancedChip,
            'G', stack(Blocks.GLASS_PANE));

        // three inputs: a data logger and two cheap chips (Baughn 2026-10-30, board #45)
        addShapelessRecipe(findItemStack("Three-Channel Data Logger", 1),
            findItemStack("Data Logger", 1), dictCheapChip, dictCheapChip);

        // its screen on a bracket: a three-channel logger, a glass pane and an iron ingot
        addShapelessRecipe(findItemStack("Tilted Terminal Logger", 1),
            findItemStack("Three-Channel Data Logger", 1), stack(Blocks.GLASS_PANE), stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Industrial Data Logger", 1),
            "RRR",
            "GGG",
            "RCR",
            'R', "itemRubber",
            'C', dictAdvancedChip,
            'G', stack(Blocks.GLASS_PANE));
    }

    public static void recipeSixNodeCache() {

    }

    public static void recipeElectricalAlarm() {
        addRecipe(findItemStack("Nuclear Alarm", 1),
            "ITI",
            "IMI",
            "IcI",
            'c', findItemStack("Signal Cable"),
            'T', stack(Blocks.REDSTONE_TORCH),
            'I', stack(Items.IRON_INGOT),
            'M', stack(Blocks.NOTE_BLOCK));
        addRecipe(findItemStack("Standard Alarm", 1),
            "MTM",
            "IcI",
            "III",
            'c', findItemStack("Signal Cable"),
            'T', stack(Blocks.REDSTONE_TORCH),
            'I', stack(Items.IRON_INGOT),
            'M', stack(Blocks.NOTE_BLOCK));

    }

    public static void recipeElectricalAntenna() {
        addRecipe(findItemStack("Low Power Transmitter Antenna", 1),
            "R i",
            "CI ",
            "R i",
            'C', dictCheapChip,
            'i', stack(Items.IRON_INGOT),
            'I', "plateIron",
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("Low Power Receiver Antenna", 1),
            "i  ",
            " IC",
            "i  ",
            'C', dictCheapChip,
            'I', "plateIron",
            'i', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("Medium Power Transmitter Antenna", 1),
            "c I",
            "CI ",
            "c I",
            'C', dictAdvancedChip,
            'c', dictCheapChip,
            'I', "plateIron",
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("Medium Power Receiver Antenna", 1),
            "I  ",
            " IC",
            "I  ",
            'C', dictAdvancedChip,
            'I', "plateIron",
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("High Power Transmitter Antenna", 1),
            "C I",
            "CI ",
            "C I",
            'C', dictAdvancedChip,
            'c', dictCheapChip,
            'I', "plateIron",
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("High Power Receiver Antenna", 1),
            "I D",
            " IC",
            "I D",
            'C', dictAdvancedChip,
            'I', "plateIron",
            'R', stack(Items.REDSTONE),
            'D', stack(Items.DIAMOND));

    }

    public static void recipeBatteryCharger() {
        addRecipe(findItemStack("Weak 50V Battery Charger", 1),
            "RIR",
            "III",
            "RcR",
            'c', findItemStack("Low Voltage Cable"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("50V Battery Charger", 1),
            "RIR",
            "ICI",
            "RcR",
            'C', dictCheapChip,
            'c', findItemStack("Low Voltage Cable"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("200V Battery Charger", 1),
            "RIR",
            "ICI",
            "RcR",
            'C', dictAdvancedChip,
            'c', findItemStack("Medium Voltage Cable"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

    }

    public static void recipeEggIncubator() {
        addRecipe(findItemStack("50V Egg Incubator", 1),
            "IGG",
            "E G",
            "CII",
            'C', dictCheapChip,
            'E', findItemStack("Small 50V Tungsten Heating Corp"),
            'I', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE));

    }

    public static void recipeEnergyConverter() {
        if (ElnToOtherEnergyConverterEnable) {
            addRecipe(findItemStack("50V Electrical Age to other energy exporter"),
                "III",
                "cCR",
                "III",
                'C', dictCheapChip,
                'c', findItemStack("Low Voltage Cable"),
                'I', stack(Items.IRON_INGOT),
                'R', "ingotCopper");

            addRecipe(findItemStack("200V Electrical Age to other energy exporter"),
                "III",
                "cCR",
                "III",
                'C', dictCheapChip,
                'c', findItemStack("Medium Voltage Cable"),
                'I', stack(Items.IRON_INGOT),
                'R', dictTungstenIngot);

            addRecipe(findItemStack("800V Electrical Age to other energy exporter"),
                "III",
                "cCR",
                "III",
                'C', dictAdvancedChip,
                'c', findItemStack("High Voltage Cable"),
                'I', stack(Items.IRON_INGOT),
                'R', stack(Items.GOLD_INGOT));

        }
    }

    // recipeComputerProbe(): OpenComputers/ComputerCraft dropped (rule 8)


    // recipeArmor(), recipeTool(): copper armour and tools dropped, vanilla since 1.21.9 (Baughn, 2026-10-11)
    public static void recipeArmor() {
    }

    public static void recipeTool() {
    }
}
