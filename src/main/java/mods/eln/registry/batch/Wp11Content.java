package mods.eln.registry.batch;

import net.minecraft.world.level.block.entity.BlockEntity;

import mods.eln.ElnContent;
import mods.eln.transparentnode.energyconverter.EnergyConverterElnToOtherDescriptor;
import mods.eln.transparentnode.energyconverter.EnergyConverterElnToOtherDescriptor.ElnDescriptor;
import mods.eln.transparentnode.energyconverter.EnergyConverterElnToOtherDescriptor.Ic2Descriptor;
import mods.eln.transparentnode.energyconverter.ExporterSaveUpgrade;
import mods.eln.node.NodeManager;
import mods.eln.ghost.GhostGroup;
import mods.eln.gridnode.electricalpole.ElectricalPoleDescriptor;
import mods.eln.mechanical.FlywheelDescriptor;
import mods.eln.mechanical.GasTurbineDescriptor;
import mods.eln.mechanical.GeneratorDescriptor;
import mods.eln.mechanical.ShaftMotorDescriptor;
import mods.eln.mechanical.JointHubDescriptor;
import mods.eln.mechanical.SteamTurbineDescriptor;
import mods.eln.mechanical.StraightJointDescriptor;
import mods.eln.mechanical.TachometerDescriptor;

import static mods.eln.Eln.*;
import static mods.eln.i18n.I18N.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp11 (1.12 port): mechanical (shafts, steam/gas turbines, generator, flywheel, joints, tachometer), grid node (poles, downlink, grid transformer), energy exporters (EA to FE), TOP provider.
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
        // TheOneProbe: 26.x port, when a TOP build for 26.x exists (P6)
    }

    public static void registerTestBlock() {
        // 1.7.10: commented out (TestBlock/TestEntity/TestNode never registered); kept as it was.
		/*
		 * testBlock = new TestBlock(); testBlock.setCreativeTab(creativeTab).setBlockName("TestBlock"); GameRegistryCompat.registerBlock(testBlock, "Eln.TestBlock"); BlockEntity.addMapping(TestEntity.class, "Eln.TestEntity"); //LanguageRegistry.addName(testBlock,"Test Block"); NodeManager.instance.registerUuid(TestNode.getInfoStatic().getUuid(), TestNode.class);
		 *
		 * GameRegistryCompat.registerCustomItemStack("Test Block", new ItemStack(testBlock));
		 */
    }

    public static EnergyConverterElnToOtherDescriptor elnToOtherHvu, elnToOtherMvu, elnToOtherLvu;

    /**
     * EA -> FE exporters (1.12: FE/RF/IC2 simple nodes; 26.x: FE, transparent nodes since 2026-10, ids in
     * ExporterSaveUpgrade). Their 1.12 block names (eln-ids.json) and descriptor keys (old saves) are kept.
     */
    public static void registerEnergyConverter() {
        if (ElnToOtherEnergyConverterEnable) {
            elnToOtherLvu = energyConverter(ExporterSaveUpgrade.LVU_ID, "50V Electrical Age to other energy exporter", "EnergyConverterElnToOtherLVU",
                new ElnDescriptor(LVU, LVP()), new Ic2Descriptor(32, 1));
            elnToOtherMvu = energyConverter(ExporterSaveUpgrade.MVU_ID, "200V Electrical Age to other energy exporter", "EnergyConverterElnToOtherMVU",
                new ElnDescriptor(MVU, MVP()), new Ic2Descriptor(128, 2));
            elnToOtherHvu = energyConverter(ExporterSaveUpgrade.HVU_ID, "800V Electrical Age to other energy exporter", "EnergyConverterElnToOtherHVU",
                new ElnDescriptor(HVU, HVP()), new Ic2Descriptor(512, 3));
        }
    }

    private static EnergyConverterElnToOtherDescriptor energyConverter(int id, String name, String key, ElnDescriptor elnDesc, Ic2Descriptor ic2Desc) {
        EnergyConverterElnToOtherDescriptor desc = new EnergyConverterElnToOtherDescriptor(name, key, elnDesc, ic2Desc);
        transparentNodeItem.addDescriptor(id, desc);
        return desc;
    }


    /** Mechanical sub-UIDs of id 4 (the electrical turbines of id 4 are wp10b's registerTurbineElectrical). */
    public static void registerTurbine(int id) {
        int subId;

        {
            subId = 9;
            SteamTurbineDescriptor desc = new SteamTurbineDescriptor(
                TR_NAME(Type.NONE, "Steam Turbine"),
                obj.getObj("shaftsteamhp"),
                mods.eln.mechanical.SteamTurbineSpec.HP
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 10;
            float nominalRads = 800, nominalU = 3200;
            float nominalP = 4000;
            GeneratorDescriptor desc = new GeneratorDescriptor(
                TR_NAME(Type.NONE, "Generator"),
                obj.getObj("shaftgen4kw"),
                highVoltageCableDescriptor,
                nominalRads, nominalU,
                nominalP / (nominalU / 25),
                nominalP,
                sixNodeThermalLoadInitializer.copy()
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            // Baughn 2026-10-29: an 8 kW generator beside the 4 kW one (four 4 kW per 16 kW turbine is intended)
            subId = 16;
            float nominalRads = 800, nominalU = 3200;
            float nominalP = 8000;
            GeneratorDescriptor desc = new GeneratorDescriptor(
                TR_NAME(Type.NONE, "8kW Generator"),
                obj.getObj("shaftgen8kw"),
                highVoltageCableDescriptor,
                nominalRads, nominalU,
                nominalP / (nominalU / 25),
                nominalP,
                sixNodeThermalLoadInitializer.copy()
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            // Baughn 2026-10-29: a small electric motor to spin shafts up and drive a condenser's pumps
            subId = 17;
            ShaftMotorDescriptor desc = new ShaftMotorDescriptor(
                TR_NAME(Type.NONE, "200V Shaft Motor"),
                obj.getObj("shaftmotor"),
                meduimVoltageCableDescriptor,
                MVU, 2000);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            // Baughn 2026-10-29: the low-pressure stage, between an HP turbine's exhaust and a plant condenser
            subId = 18;
            SteamTurbineDescriptor desc = new SteamTurbineDescriptor(
                TR_NAME(Type.NONE, "LP Steam Turbine"),
                obj.getObj("shaftsteamlp"),
                mods.eln.mechanical.SteamTurbineSpec.LP
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 11;
            GasTurbineDescriptor desc = new GasTurbineDescriptor(
                TR_NAME(Type.NONE, "Gas Turbine"),
                obj.getObj("shaftgas")
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 12;
            StraightJointDescriptor desc = new StraightJointDescriptor(
                TR_NAME(Type.NONE, "Joint"),
                obj.getObj("shaftjoint"));
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 13;
            JointHubDescriptor desc = new JointHubDescriptor(
                TR_NAME(Type.NONE, "Joint hub"),
                obj.getObj("shafthub"));
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 14;
            FlywheelDescriptor desc = new FlywheelDescriptor(
                TR_NAME(Type.NONE, "Flywheel"),
                obj.getObj("shaftflywheel"));
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 15;
            TachometerDescriptor desc = new TachometerDescriptor(
                TR_NAME(Type.NONE, "Tachometer"),
                obj.getObj("shafttacho"));
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
