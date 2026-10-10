package mods.eln.registry.batch;

import mods.eln.ElnContent;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment;
import mods.eln.i18n.I18N.Type;
import mods.eln.item.*;
import mods.eln.item.electricalitem.*;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorAnalogDescriptor;
import mods.eln.item.regulator.RegulatorOnOffDescriptor;
import mods.eln.misc.Utils;
import mods.eln.wiki.Data;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import mods.eln.Eln;
import java.util.ArrayList;
import static mods.eln.Eln.*;
import static mods.eln.i18n.I18N.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp12a (26.x port, split from 1.12 Wp12Content): the shared items (sub-items of 1.12 Eln.sharedItem /
 * sharedItemStackOne, one 26.x item per descriptor): heating corps, regulators, protections, combustion chambers,
 * ferromagnetic cores, ingots, dusts, motors, solar tracker, drills, ore scanner, mining pipe, resin and rubber, raw
 * cables, brushes, misc items, portable batteries and scanners, fuel burners. Ids (sub-UIDs) unchanged.
 * Not here (later Wp12 steps): armour, tools, electrical tools, ores and worldgen, replicator, wiki. Ore dictionary
 * entries become item tags in P5 (kept as comments); the wiki Data lists come with P4.
 */
@SuppressWarnings({"SameParameterValue", "PointlessArithmeticExpression", "unused"})
public class Wp12aContent implements ElnContent {
    public static CopperCableDescriptor copperCableDescriptor;

    @Override
    public void preInit() {
        registerHeatingCorp(1);
        registerRegulatorItem(3);
        registerProtection(5);
        registerCombustionChamber(6);
        registerFerromagneticCore(7);
        registerIngot(8);
        registerDust(9);
        registerElectricalMotor(10);
        registerSolarTracker(11);
        registerElectricalDrill(15);
        registerOreScanner(16);
        registerMiningPipe(17);
        registerTreeResinAndRubber(64);
        registerRawCable(65);
        registerBrush(119);
        registerMiscItem(120);
        registerPortableItem(122);
        registerFuelBurnerItem(124);
    }

    public static void registerHeatingCorp(int id) {
        int subId, completId;
        String name;

        HeatingCorpElement element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(Type.NONE, "Small 50V Copper Heating Corp"),// iconId,
                // name,
                LVU, 150,// electricalNominalU, electricalNominalP,
                190,// electricalMaximalP)
                lowVoltageCableDescriptor// ElectricalCableDescriptor
            );
            sharedItem.addElement(completId, element);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(Type.NONE, "50V Copper Heating Corp"),// iconId,
                // name,
                LVU, 250,// electricalNominalU, electricalNominalP,
                320,// electricalMaximalP)
                lowVoltageCableDescriptor);
            sharedItem.addElement(completId, element);
        }
        {
            subId = 2;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(Type.NONE, "Small 200V Copper Heating Corp"),// iconId,
                // name,
                MVU, 400,// electricalNominalU, electricalNominalP,
                500,// electricalMaximalP)
                meduimVoltageCableDescriptor);
            sharedItem.addElement(completId, element);
        }
        {
            subId = 3;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(Type.NONE, "200V Copper Heating Corp"),// iconId,
                // name,
                MVU, 600,// electricalNominalU, electricalNominalP,
                750,// electricalMaximalP)
                highVoltageCableDescriptor);
            sharedItem.addElement(completId, element);
        }
        {
            subId = 4;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(Type.NONE, "Small 50V Iron Heating Corp"),// iconId,
                // name,
                LVU, 180,// electricalNominalU, electricalNominalP,
                225,// electricalMaximalP)
                lowVoltageCableDescriptor// ElectricalCableDescriptor
            );
            sharedItem.addElement(completId, element);
        }
        {
            subId = 5;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(Type.NONE, "50V Iron Heating Corp"),// iconId,
                // name,
                LVU, 375,// electricalNominalU, electricalNominalP,
                480,// electricalMaximalP)
                lowVoltageCableDescriptor);
            sharedItem.addElement(completId, element);
        }
        {
            subId = 6;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(Type.NONE, "Small 200V Iron Heating Corp"),// iconId,
                // name,
                MVU, 600,// electricalNominalU, electricalNominalP,
                750,// electricalMaximalP)
                meduimVoltageCableDescriptor);
            sharedItem.addElement(completId, element);
        }
        {
            subId = 7;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(Type.NONE, "200V Iron Heating Corp"),// iconId,
                // name,
                MVU, 900,// electricalNominalU, electricalNominalP,
                1050,// electricalMaximalP)
                highVoltageCableDescriptor);
            sharedItem.addElement(completId, element);
        }
        {
            subId = 8;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(Type.NONE, "Small 50V Tungsten Heating Corp"),// iconId,
                // name,
                LVU, 240,// electricalNominalU, electricalNominalP,
                300,// electricalMaximalP)
                lowVoltageCableDescriptor// ElectricalCableDescriptor
            );
            sharedItem.addElement(completId, element);
        }
        {
            subId = 9;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(Type.NONE, "50V Tungsten Heating Corp"),// iconId,
                // name,
                LVU, 500,// electricalNominalU, electricalNominalP,
                640,// electricalMaximalP)
                lowVoltageCableDescriptor);
            sharedItem.addElement(completId, element);
        }
        {
            subId = 10;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(
                TR_NAME(Type.NONE, "Small 200V Tungsten Heating Corp"),// iconId, name,
                MVU, 800,// electricalNominalU, electricalNominalP,
                1000,// electricalMaximalP)
                meduimVoltageCableDescriptor);
            sharedItem.addElement(completId, element);
        }
        {
            subId = 11;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(Type.NONE, "200V Tungsten Heating Corp"),// iconId,
                // name,
                MVU, 1200,// electricalNominalU, electricalNominalP,
                1500,// electricalMaximalP)
                highVoltageCableDescriptor);
            sharedItem.addElement(completId, element);
        }

    }

    public static void registerRegulatorItem(int id) {
        int subId, completId;
        String name;
        IRegulatorDescriptor element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new RegulatorOnOffDescriptor(TR_NAME(Type.NONE, "On/OFF Regulator 1 Percent"),
                "onoffregulator", 0.01);
            sharedItem.addElement(completId, element);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            element = new RegulatorOnOffDescriptor(TR_NAME(Type.NONE, "On/OFF Regulator 10 Percent"),
                "onoffregulator", 0.1);
            sharedItem.addElement(completId, element);
        }

        {
            subId = 8;
            completId = subId + (id << 6);
            element = new RegulatorAnalogDescriptor(TR_NAME(Type.NONE, "Analogic Regulator"),
                "Analogicregulator");
            sharedItem.addElement(completId, element);
        }

    }

    public static void registerProtection(int id) {
        int subId, completId;
        String name;

        {
            OverHeatingProtectionDescriptor element;
            subId = 0;
            completId = subId + (id << 6);
            element = new OverHeatingProtectionDescriptor(
                TR_NAME(Type.NONE, "Overheating Protection"));
            sharedItem.addElement(completId, element);
        }
        {
            OverVoltageProtectionDescriptor element;
            subId = 1;
            completId = subId + (id << 6);
            element = new OverVoltageProtectionDescriptor(
                TR_NAME(Type.NONE, "Overvoltage Protection"));
            sharedItem.addElement(completId, element);
        }

    }

    public static void registerCombustionChamber(int id) {
        int subId, completId;
        {
            CombustionChamber element;
            subId = 0;
            completId = subId + (id << 6);
            element = new CombustionChamber(TR_NAME(Type.NONE, "Combustion Chamber"));
            sharedItem.addElement(completId, element);
        }
        {
            ThermalIsolatorElement element;
            subId = 1;
            completId = subId + (id << 6);
            element = new ThermalIsolatorElement(
                TR_NAME(Type.NONE, "Thermal Insulation"),
                0.5,
                500
            );
            sharedItem.addElement(completId, element);
        }
    }

    public static void registerFerromagneticCore(int id) {
        int subId, completId;

        FerromagneticCoreDescriptor element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new FerromagneticCoreDescriptor(
                TR_NAME(Type.NONE, "Cheap Ferromagnetic Core"), obj.getObj("feromagneticcorea"),// iconId,
                // name,
                10);
            sharedItem.addElement(completId, element);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            element = new FerromagneticCoreDescriptor(
                TR_NAME(Type.NONE, "Average Ferromagnetic Core"), obj.getObj("feromagneticcorea"),// iconId,
                // name,
                4);
            sharedItem.addElement(completId, element);
        }
        {
            subId = 2;
            completId = subId + (id << 6);
            element = new FerromagneticCoreDescriptor(
                TR_NAME(Type.NONE, "Optimal Ferromagnetic Core"), obj.getObj("feromagneticcorea"),// iconId,
                // name,
                1);
            sharedItem.addElement(completId, element);
        }
    }

    public static void registerIngot(int id) {
        int subId, completId;
        String name;

        GenericItemUsingDamageDescriptorWithComment element;

        {
            subId = 1;
            completId = subId + (id << 6);

            name = TR_NAME(Type.NONE, "Copper Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(completId, element);
            // GameRegistryCompat.registerCustomItemStack(name,
            // element.newItemStack(1));
            copperIngot = element;
            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("ingotCopper", element.newItemStack());
        }

        {
            subId = 4;
            completId = subId + (id << 6);

            name = TR_NAME(Type.NONE, "Lead Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(completId, element);
            // GameRegistryCompat.registerCustomItemStack(name,
            // element.newItemStack(1));
            plumbIngot = element;
            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("ingotLead", element.newItemStack());

        }

        {
            subId = 5;
            completId = subId + (id << 6);

            name = TR_NAME(Type.NONE, "Tungsten Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(completId, element);
            // GameRegistryCompat.registerCustomItemStack(name,
            // element.newItemStack(1));
            tungstenIngot = element;
            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre(dictTungstenIngot, element.newItemStack());
        }

        {
            subId = 6;
            completId = subId + (id << 6);

            name = TR_NAME(Type.NONE, "Ferrite Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{"useless", "Really useless"});
            sharedItem.addElement(completId, element);
            // GameRegistryCompat.registerCustomItemStack(name,
            // element.newItemStack(1));

            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("ingotFerrite", element.newItemStack());
        }

        {
            subId = 7;
            completId = subId + (id << 6);

            name = TR_NAME(Type.NONE, "Alloy Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(completId, element);
            // GameRegistryCompat.registerCustomItemStack(name,
            // element.newItemStack(1));

            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("ingotAlloy", element.newItemStack());
        }

        {
            subId = 8;
            completId = subId + (id << 6);

            name = TR_NAME(Type.NONE, "Mercury");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{"useless", "miaou"});
            sharedItem.addElement(completId, element);
            // GameRegistryCompat.registerCustomItemStack(name,
            // element.newItemStack(1));

            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("quicksilver", element.newItemStack());
        }
    }

    public static void registerDust(int id) {
        int subId, completId;
        String name;
        GenericItemUsingDamageDescriptorWithComment element;

        {
            subId = 1;
            completId = subId + (id << 6);

            name = TR_NAME(Type.NONE, "Copper Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            dustCopper = element;
            sharedItem.addElement(completId, element);
            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("dustCopper", element.newItemStack());
        }
        {
            subId = 2;
            completId = subId + (id << 6);

            name = TR_NAME(Type.NONE, "Iron Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            dustCopper = element;
            sharedItem.addElement(completId, element);
            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("dustIron", element.newItemStack());
        }

        {
            id = 5;

            name = TR_NAME(Type.NONE, "Lead Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(id, element);
            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("dustLead", element.newItemStack());
        }
        {
            id = 6;

            name = TR_NAME(Type.NONE, "Tungsten Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(id, element);
            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre(dictTungstenDust, element.newItemStack());
        }

        {
            id = 7;

            name = TR_NAME(Type.NONE, "Gold Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(id, element);
            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("dustGold", element.newItemStack());
        }

        {
            id = 8;

            name = TR_NAME(Type.NONE, "Coal Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(id, element);
            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("dustCoal", element.newItemStack());
        }
        {
            id = 9;

            name = TR_NAME(Type.NONE, "Alloy Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(id, element);
            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("dustAlloy", element.newItemStack());
        }

        {
            id = 10;

            name = TR_NAME(Type.NONE, "Cinnabar Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(id, element);
            // wiki (P4): Data.addResource(element.newItemStack());
            // P5 (item tags): addToOre("dustCinnabar", element.newItemStack());
        }

    }

    public static void registerElectricalMotor(int id) {

        int subId, completId;
        String name;
        GenericItemUsingDamageDescriptorWithComment element;

        {
            subId = 0;
            completId = subId + (id << 6);

            name = TR_NAME(Type.NONE, "Electrical Motor");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(completId, element);
            // GameRegistryCompat.registerCustomItemStack(name,
            // element.newItemStack(1));

            // wiki (P4): Data.addResource(element.newItemStack());

        }
        {
            subId = 1;
            completId = subId + (id << 6);

            name = TR_NAME(Type.NONE, "Advanced Electrical Motor");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(completId, element);
            // GameRegistryCompat.registerCustomItemStack(name,
            // element.newItemStack(1));
            // wiki (P4): Data.addResource(element.newItemStack());

        }

    }

    public static void registerSolarTracker(int id) {
        int subId, completId;
        String name;

        SolarTrackerDescriptor element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new SolarTrackerDescriptor(TR_NAME(Type.NONE, "Solar Tracker") // iconId, name,

            );
            sharedItem.addElement(completId, element);
        }

    }

    public static void registerElectricalDrill(int id) {
        int subId, completId;
        String name;

        ElectricalDrillDescriptor descriptor;
        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Cheap Electrical Drill");

            descriptor = new ElectricalDrillDescriptor(name,// iconId, name,
                8, 4000 // double operationTime,double operationEnergy
            );
            sharedItem.addElement(completId, descriptor);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Average Electrical Drill");

            descriptor = new ElectricalDrillDescriptor(name,// iconId, name,
                5, 5000 // double operationTime,double operationEnergy
            );
            sharedItem.addElement(completId, descriptor);
        }
        {
            subId = 2;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Fast Electrical Drill");

            descriptor = new ElectricalDrillDescriptor(name,// iconId, name,
                3, 6000 // double operationTime,double operationEnergy
            );
            sharedItem.addElement(completId, descriptor);
        }

    }

    public static void registerOreScanner(int id) {
        int subId, completId;
        String name;

        OreScanner descriptor;
        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Ore Scanner");

            descriptor = new OreScanner(name

            );
            sharedItem.addElement(completId, descriptor);
        }

    }

    public static void registerMiningPipe(int id) {
        int subId, completId;
        String name;

        MiningPipeDescriptor descriptor;
        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Mining Pipe");

            descriptor = new MiningPipeDescriptor(name// iconId, name
            );
            sharedItem.addElement(completId, descriptor);

            miningPipeDescriptor = descriptor;
        }

    }

    public static void registerTreeResinAndRubber(int id) {
        int subId, completId;
        String name;

        {
            TreeResin descriptor;
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Tree Resin");

            descriptor = new TreeResin(name);

            sharedItem.addElement(completId, descriptor);
            treeResin = descriptor;
            // P5 (item tags): addToOre("materialResin", descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 1;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Rubber");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            sharedItem.addElement(completId, descriptor);
            // P5 (item tags): addToOre("itemRubber", descriptor.newItemStack());
        }
    }

    public static void registerRawCable(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Copper Cable");

            copperCableDescriptor = new CopperCableDescriptor(name);
            sharedItem.addElement(completId, copperCableDescriptor);
            // wiki (P4): Data.addResource(copperCableDescriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 1;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Iron Cable");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            sharedItem.addElement(completId, descriptor);
            // wiki (P4): Data.addResource(descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 2;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Tungsten Cable");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            sharedItem.addElement(completId, descriptor);
            // wiki (P4): Data.addResource(descriptor.newItemStack());
        }
    }

    public static void registerBrush(int id) {

        int subId, completId;
        BrushDescriptor whiteDesc = null;
        String name;
        String[] subNames = {
            TR_NAME(Type.NONE, "Black Brush"),
            TR_NAME(Type.NONE, "Red Brush"),
            TR_NAME(Type.NONE, "Green Brush"),
            TR_NAME(Type.NONE, "Brown Brush"),
            TR_NAME(Type.NONE, "Blue Brush"),
            TR_NAME(Type.NONE, "Purple Brush"),
            TR_NAME(Type.NONE, "Cyan Brush"),
            TR_NAME(Type.NONE, "Silver Brush"),
            TR_NAME(Type.NONE, "Gray Brush"),
            TR_NAME(Type.NONE, "Pink Brush"),
            TR_NAME(Type.NONE, "Lime Brush"),
            TR_NAME(Type.NONE, "Yellow Brush"),
            TR_NAME(Type.NONE, "Light Blue Brush"),
            TR_NAME(Type.NONE, "Magenta Brush"),
            TR_NAME(Type.NONE, "Orange Brush"),
            TR_NAME(Type.NONE, "White Brush")};
        for (int idx = 0; idx < 16; idx++) {
            subId = idx;
            name = subNames[idx];
            BrushDescriptor desc = new BrushDescriptor(name);
            sharedItem.addElement(subId + (id << 6), desc);
            whiteDesc = desc;
        }

        // 1.7.10 setLife(White Brush, 0) + the 32 wool/dye brush recipes: registry.ElnRecipes.recipeBrush (WP15).
    }

    public static void registerMiscItem(int id) {
        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Cheap Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): OreDictionary.registerOre(dictCheapChip, desc.newItemStack());
        }
        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Advanced Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): OreDictionary.registerOre(dictAdvancedChip, desc.newItemStack());
        }
        {
            subId = 2;
            name = TR_NAME(Type.NONE, "Machine Block");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): addToOre("casingMachine", desc.newItemStack());
        }
        {
            subId = 3;
            name = TR_NAME(Type.NONE, "Electrical Probe Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
        }
        {
            subId = 4;
            name = TR_NAME(Type.NONE, "Thermal Probe Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
        }
        {
            subId = 6;
            name = TR_NAME(Type.NONE, "Copper Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): addToOre("plateCopper", desc.newItemStack());
        }
        {
            subId = 7;
            name = TR_NAME(Type.NONE, "Iron Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): addToOre("plateIron", desc.newItemStack());
        }
        {
            subId = 8;
            name = TR_NAME(Type.NONE, "Gold Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): addToOre("plateGold", desc.newItemStack());
        }
        {
            subId = 9;
            name = TR_NAME(Type.NONE, "Lead Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): addToOre("plateLead", desc.newItemStack());
        }
        {
            subId = 10;
            name = TR_NAME(Type.NONE, "Silicon Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): addToOre("plateSilicon", desc.newItemStack());
        }
        {
            subId = 11;
            name = TR_NAME(Type.NONE, "Alloy Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): addToOre("plateAlloy", desc.newItemStack());
        }
        {
            subId = 12;
            name = TR_NAME(Type.NONE, "Coal Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): addToOre("plateCoal", desc.newItemStack());
        }
        {
            subId = 16;
            name = TR_NAME(Type.NONE, "Silicon Dust");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): addToOre("dustSilicon", desc.newItemStack());
        }
        {
            subId = 17;
            name = TR_NAME(Type.NONE, "Silicon Ingot");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): addToOre("ingotSilicon", desc.newItemStack());
        }
        {
            subId = 22;
            name = TR_NAME(Type.NONE, "Machine Booster");
            MachineBoosterDescriptor desc = new MachineBoosterDescriptor(name);
            sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 23;
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                TR_NAME(Type.NONE, "Advanced Machine Block"), new String[]{}); // TODO: Description.
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
            // P5 (item tags): addToOre("casingMachineAdvanced", desc.newItemStack());
        }
        {
            subId = 28;
            name = TR_NAME(Type.NONE, "Basic Magnet");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
        }
        {
            subId = 29;
            name = TR_NAME(Type.NONE, "Advanced Magnet");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
        }
        {
            subId = 33;
            name = TR_NAME(Type.NONE, "Signal Antenna");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            // wiki (P4): Data.addResource(desc.newItemStack());
        }
        {
            subId = 40;
            name = TR_NAME(Type.NONE, "Player Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, Player.class, 0f, 1f, 0f);
            sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 41;
            name = TR_NAME(Type.NONE, "Monster Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, net.minecraft.world.entity.monster.Enemy.class, 1f, 0f, 0f);
            sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 42;
            name = TR_NAME(Type.NONE, "Animal Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, net.minecraft.world.entity.animal.Animal.class, .3f, .3f, 1f);
            sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 48;
            name = TR_NAME(Type.NONE, "Wrench");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, TR("Electrical age wrench,\nCan be used to turn\nsmall wall blocks").split("\n"));
            sharedItem.addElement(subId + (id << 6), desc);

            // 1.12 Eln.wrenchItemStack + Utils.isWrench: this item, or any item named "...wrench..."
            final GenericItemUsingDamageDescriptorWithComment wrench = desc;
            Utils.wrenchCheck = p -> {
                ItemStack held = p.getMainHandItem();
                return !held.isEmpty() && (Utils.getItemObject(held) == wrench
                    || held.getHoverName().getString().toLowerCase().contains("wrench"));
            };
        }
        {
            subId = 52;
            name = TR_NAME(Type.NONE, "Dielectric");
            DielectricItem desc = new DielectricItem(name, LVU);
            sharedItem.addElement(subId + (id << 6), desc);
        }









        sharedItem.addElement(53 + (id << 6), new CaseItemDescriptor(TR_NAME(Type.NONE, "Casing")));
    
    }

    public static void registerPortableItem(int id) {
        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Portable Battery");

            BatteryItem desc = new BatteryItem(
                name,
                40000, 500, 100,// double energyStorage,double
                // chargePower,double dischargePower,
                2// int priority
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Portable Battery Pack");

            BatteryItem desc = new BatteryItem(
                name,
                120000, 1500, 300,// double energyStorage,double
                // chargePower,double dischargePower,
                2// int priority
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        {
            subId = 16;
            name = TR_NAME(Type.NONE, "Portable Condensator");

            BatteryItem desc = new BatteryItem(
                name,
                5000, 2000, 500,// double energyStorage,double
                // chargePower,double dischargePower,
                1// int priority
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
        {
            subId = 17;
            name = TR_NAME(Type.NONE, "Portable Condensator Pack");

            BatteryItem desc = new BatteryItem(
                name,
                15000, 6000, 1500,// double energyStorage,double
                // chargePower,double dischargePower,
                1// int priority
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        /* 26.x port: the X-Ray Scanner (mostly client: ore render storage) comes with P3
        {
            subId = 32;
            name = TR_NAME(Type.NONE, "X-Ray Scanner");

            PortableOreScannerItem desc = new PortableOreScannerItem(
                name, obj.getObj("XRayScanner"),
                10000, 400, 300,// double energyStorage,double
                // chargePower,double dischargePower,
                xRayScannerRange, (float) (Math.PI / 2),// float
                // viewRange,float
                // viewYAlpha,
                32, 20// int resWidth,int resHeight
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
        */
    }

    public static void registerFuelBurnerItem(int id) {
        sharedItemStackOne.addElement(0 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(Type.NONE, "Small Fuel Burner"), 5000 * fuelHeatFurnacePowerFactor, 2, 1.6f));
        sharedItemStackOne.addElement(1 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(Type.NONE, "Medium Fuel Burner"), 10000 * fuelHeatFurnacePowerFactor, 1, 1.4f));
        sharedItemStackOne.addElement(2 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(Type.NONE, "Big Fuel Burner"), 25000 * fuelHeatFurnacePowerFactor, 0, 1f));
    }
}
