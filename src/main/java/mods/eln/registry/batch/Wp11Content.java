package mods.eln.registry.batch;

import mods.eln.ElnContent;
import mods.eln.compat.GameRegistryCompat;
import mods.eln.compat.top.TopIntegration;
import mods.eln.node.NodeManager;
import mods.eln.node.simple.SimpleNodeItem;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor.ElnDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor.Ic2Descriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherEntity;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import mods.eln.ghost.GhostGroup;
import mods.eln.gridnode.electricalpole.ElectricalPoleDescriptor;
import mods.eln.mechanical.FlywheelDescriptor;
import mods.eln.mechanical.GasTurbineDescriptor;
import mods.eln.mechanical.GeneratorDescriptor;
import mods.eln.mechanical.JointHubDescriptor;
import mods.eln.mechanical.SteamTurbineDescriptor;
import mods.eln.mechanical.StraightJointDescriptor;
import mods.eln.mechanical.TachometerDescriptor;

import static mods.eln.Eln.*;
import static mods.eln.i18n.I18N.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp11 (1.12 port): mechanical (shafts, steam/gas turbines, generator, flywheel, joints, tachometer), grid node (poles, downlink, grid transformer), simple-node energy converters (FE/RF/IC2 EU), TOP provider.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * All 1.7.10 registrations of this batch are ported (Wp11Pending removed). Keep the ids (sub-UIDs) unchanged.
 * Selftest cases: mods.eln.selftest.cases.Wp11Cases.
 */
public class Wp11Content implements ElnContent {
    @Override
    public void preInit() {
        registerTestBlock();
        registerEnergyConverter();
        registerTurbine(4); // mechanical sub-UIDs (steam/gas turbine, generator, joints, flywheel, tachometer)
        registerGridDevices(123);
        // TheOneProbe info (replaces 1.7.10 Waila): node getWaila() lines, ghost blocks resolved to the real device
        if (Loader.isModLoaded(TopIntegration.MODID_TOP)) TopIntegration.register();
    }

    public static void registerTestBlock() {
        // 1.7.10: commented out (TestBlock/TestEntity/TestNode never registered); kept as it was.
		/*
		 * testBlock = new TestBlock(); testBlock.setCreativeTab(creativeTab).setBlockName("TestBlock"); GameRegistryCompat.registerBlock(testBlock, "Eln.TestBlock"); TileEntity.addMapping(TestEntity.class, "Eln.TestEntity"); //LanguageRegistry.addName(testBlock,"Test Block"); NodeManager.instance.registerUuid(TestNode.getInfoStatic().getUuid(), TestNode.class);
		 *
		 * GameRegistryCompat.registerCustomItemStack("Test Block", new ItemStack(testBlock));
		 */
    }

    public static EnergyConverterElnToOtherBlock elnToOtherBlockHvu;
    public static EnergyConverterElnToOtherBlock elnToOtherBlockMvu;
    public static EnergyConverterElnToOtherBlock elnToOtherBlockLvu;

    /** EA -> FE/RF/IC2 exporters (1.12: OpenComputers part dropped). */
    public static void registerEnergyConverter() {
        if (ElnToOtherEnergyConverterEnable) {
            GameRegistry.registerTileEntity(EnergyConverterElnToOtherEntity.class, new ResourceLocation(MODID, "energy_converter_eln_to_other_entity"));
            NodeManager.registerUuid(EnergyConverterElnToOtherNode.getNodeUuidStatic(), EnergyConverterElnToOtherNode.class);

            elnToOtherBlockLvu = energyConverter("eln.EnergyConverterElnToOtherLVUBlock", "EnergyConverterElnToOtherLVU",
                new ElnDescriptor(LVU, LVP()), new Ic2Descriptor(32, 1));
            elnToOtherBlockMvu = energyConverter("eln.EnergyConverterElnToOtherMVUBlock", "EnergyConverterElnToOtherMVU",
                new ElnDescriptor(MVU, MVP()), new Ic2Descriptor(128, 2));
            elnToOtherBlockHvu = energyConverter("eln.EnergyConverterElnToOtherHVUBlock", "EnergyConverterElnToOtherHVU",
                new ElnDescriptor(HVU, HVP()), new Ic2Descriptor(512, 3));
        }
    }

    private static EnergyConverterElnToOtherBlock energyConverter(String name, String key, ElnDescriptor elnDesc, Ic2Descriptor ic2Desc) {
        String blockName = TR_NAME(Type.TILE, name);
        EnergyConverterElnToOtherDescriptor desc = new EnergyConverterElnToOtherDescriptor(key, elnDesc, ic2Desc);
        EnergyConverterElnToOtherBlock block = new EnergyConverterElnToOtherBlock(desc);
        block.setCreativeTab(creativeTab).setTranslationKey(blockName);
        GameRegistryCompat.registerBlock(block, SimpleNodeItem.class, blockName);
        return block;
    }

    /** Mechanical sub-UIDs of id 4 (the electrical turbines of id 4 are wp10b's registerTurbineElectrical). */
    public static void registerTurbine(int id) {
        int subId;

        {
            subId = 9;
            SteamTurbineDescriptor desc = new SteamTurbineDescriptor(
                TR_NAME(Type.NONE, "Steam Turbine"),
                obj.getObj("Turbine")
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 10;
            float nominalRads = 800, nominalU = 3200;
            float nominalP = 4000;
            GeneratorDescriptor desc = new GeneratorDescriptor(
                TR_NAME(Type.NONE, "Generator"),
                obj.getObj("Generator"),
                highVoltageCableDescriptor,
                nominalRads, nominalU,
                nominalP / (nominalU / 25),
                nominalP,
                sixNodeThermalLoadInitializer.copy()
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 11;
            GasTurbineDescriptor desc = new GasTurbineDescriptor(
                TR_NAME(Type.NONE, "Gas Turbine"),
                obj.getObj("GasTurbine")
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 12;
            StraightJointDescriptor desc = new StraightJointDescriptor(
                TR_NAME(Type.NONE, "Joint"),
                obj.getObj("StraightJoint"));
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 13;
            JointHubDescriptor desc = new JointHubDescriptor(
                TR_NAME(Type.NONE, "Joint hub"),
                obj.getObj("JointHub"));
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 14;
            FlywheelDescriptor desc = new FlywheelDescriptor(
                TR_NAME(Type.NONE, "Flywheel"),
                obj.getObj("Flywheel"));
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 15;
            TachometerDescriptor desc = new TachometerDescriptor(
                TR_NAME(Type.NONE, "Tachometer"),
                obj.getObj("Tachometer"));
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerGridDevices(int id) {
        int subId;
        {
            subId = 2;
//			DownlinkDescriptor descriptor =
//					new DownlinkDescriptor("Downlink", obj.getObj("DownLink"), "textures/wire.png", highVoltageCableDescriptor);
//			transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 3;
//			GridTransformerDescriptor descriptor =
//					new GridTransformerDescriptor("Grid Transformer", obj.getObj("Transformer"), "textures/wire.png", highVoltageCableDescriptor);
//			GhostGroup g = new GhostGroup();
//			g.addElement(1, 0, 0);
//			g.addElement(0, 0, -1);
//			g.addElement(1, 0, -1);
//			g.addElement(1, 1, 0);
//			g.addElement(0, 1, 0);
//			g.addElement(1, 1, -1);
//			g.addElement(0, 1, -1);
//			descriptor.setGhostGroup(g);
//			transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 4;
            ElectricalPoleDescriptor descriptor =
                new ElectricalPoleDescriptor("Utility Pole", obj.getObj("UtilityPole"), "textures/wire.png", highVoltageCableDescriptor, false);
            GhostGroup g = new GhostGroup();
            g.addElement(0, 1, 0);
            g.addElement(0, 2, 0);
            g.addElement(0, 3, 0);
            //g.addRectangle(-1, 1, 3, 4, -1, 1);
            descriptor.setGhostGroup(g);
            transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 5;
            ElectricalPoleDescriptor descriptor =
                new ElectricalPoleDescriptor("Utility Pole w/DC-DC Converter", obj.getObj("UtilityPole"), "textures/wire.png", highVoltageCableDescriptor, true);
            GhostGroup g = new GhostGroup();
            g.addElement(0, 1, 0);
            g.addElement(0, 2, 0);
            g.addElement(0, 3, 0);
            //g.addRectangle(-1, 1, 3, 4, -1, 1);
            descriptor.setGhostGroup(g);
            transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
    }
}
