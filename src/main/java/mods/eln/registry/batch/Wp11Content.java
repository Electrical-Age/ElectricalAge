package mods.eln.registry.batch;

import mods.eln.ElnContent;
import mods.eln.ghost.GhostGroup;
import mods.eln.gridnode.electricalpole.ElectricalPoleDescriptor;
import mods.eln.mechanical.FlywheelDescriptor;
import mods.eln.mechanical.GasTurbineDescriptor;
import mods.eln.mechanical.GeneratorDescriptor;
import mods.eln.mechanical.JointHubDescriptor;
import mods.eln.mechanical.SteamTurbineDescriptor;
import mods.eln.mechanical.StraightJointDescriptor;
import mods.eln.mechanical.TachometerDescriptor;
import mods.eln.misc.Obj3D;

import static mods.eln.Eln.*;
import static mods.eln.i18n.I18N.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp11 (1.12 port): mechanical (shafts, steam/gas turbines, generator, flywheel, joints, tachometer), grid node (poles, downlink, grid transformer), simple-node energy converters (FE/RF/IC2 EU), TOP provider.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * Sources: m1-exclude-wp11.txt (delete lines to build them). Not yet ported registrations:
 * registry/pending/Wp11Pending.java (excluded). To port a device: move its register method (and the fields it
 * sets) from Wp11Pending into this class, un-comment its PENDING line below, point device code at
 * Wp11Content.&lt;field&gt;. Keep the ids (sub-UIDs) unchanged. Selftest cases: mods.eln.selftest.cases.Wp11Cases.
 */
public class Wp11Content implements ElnContent {
    @Override
    public void preInit() {
        registerTestBlock();
        // PENDING(1.12 wp11): registerEnergyConverter();
        registerTurbine(4); // mechanical sub-UIDs (steam/gas turbine, generator, joints, flywheel, tachometer)
        registerGridDevices(123);
        // PENDING(1.12 wp11): TODO(1.12 WP11): TheOneProbe provider (replaces Waila; Element getWaila() data)
    }

    /**
     * A model the SERVER needs too: grid nodes compute their cable attachment points from the OBJ part bounding
     * boxes in networkSerialize (server side). Core loads OBJ models on the client only (Eln.obj stays empty on a
     * dedicated server), so on the server the grid models are parsed here (geometry only; nothing is drawn).
     */
    static Obj3D serverSideObj(String name, String path) {
        Obj3D o = obj.getObj(name);
        if (o != null) return o;
        o = new Obj3D();
        return o.loadFile(path) ? o : null;
    }

    public static void registerTestBlock() {
        // 1.7.10: commented out (TestBlock/TestEntity/TestNode never registered); kept as it was.
		/*
		 * testBlock = new TestBlock(); testBlock.setCreativeTab(creativeTab).setBlockName("TestBlock"); GameRegistryCompat.registerBlock(testBlock, "Eln.TestBlock"); TileEntity.addMapping(TestEntity.class, "Eln.TestEntity"); //LanguageRegistry.addName(testBlock,"Test Block"); NodeManager.instance.registerUuid(TestNode.getInfoStatic().getUuid(), TestNode.class);
		 *
		 * GameRegistryCompat.registerCustomItemStack("Test Block", new ItemStack(testBlock));
		 */
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
        Obj3D utilityPole = serverSideObj("UtilityPole", "powerpole/utilitypole.obj");
        {
            subId = 4;
            ElectricalPoleDescriptor descriptor =
                new ElectricalPoleDescriptor("Utility Pole", utilityPole, "textures/wire.png", highVoltageCableDescriptor, false);
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
                new ElectricalPoleDescriptor("Utility Pole w/DC-DC Converter", utilityPole, "textures/wire.png", highVoltageCableDescriptor, true);
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
