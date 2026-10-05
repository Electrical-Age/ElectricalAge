package mods.eln.registry;

import mods.eln.compat.GameRegistryCompat;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientKeyHandler;
import mods.eln.client.SoundLoader;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.entity.ReplicatorPopProcess;
import mods.eln.generic.*;
import mods.eln.generic.genericArmorItem.ArmourType;
import mods.eln.ghost.GhostBlock;
import mods.eln.ghost.GhostGroup;
import mods.eln.ghost.GhostManager;
import mods.eln.ghost.GhostManagerNbt;
import mods.eln.gridnode.electricalpole.ElectricalPoleDescriptor;
import mods.eln.i18n.I18N;
import mods.eln.item.*;
import mods.eln.item.electricalinterface.ItemEnergyInventoryProcess;
import mods.eln.item.electricalitem.*;
import mods.eln.item.electricalitem.PortableOreScannerItem.RenderStorage.OreScannerConfigElement;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorAnalogDescriptor;
import mods.eln.item.regulator.RegulatorOnOffDescriptor;
import mods.eln.mechanical.*;
import mods.eln.misc.*;
import mods.eln.misc.series.SerieEE;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import mods.eln.node.NodeManagerNbt;
import mods.eln.node.NodeServer;
import mods.eln.node.simple.SimpleNodeItem;
import mods.eln.node.six.*;
import mods.eln.node.transparent.*;
import mods.eln.ore.OreBlock;
import mods.eln.ore.OreDescriptor;
import mods.eln.ore.OreItem;
import mods.eln.server.*;
import mods.eln.signalinductor.SignalInductorDescriptor;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.ThermalLoadInitializerByPowerDrop;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor.ElnDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor.Ic2Descriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor.OcDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherEntity;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;
import mods.eln.simplenode.test.TestBlock;
import mods.eln.sixnode.*;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor;
import mods.eln.sixnode.batterycharger.BatteryChargerDescriptor;
import mods.eln.sixnode.diode.DiodeDescriptor;
import mods.eln.sixnode.electricalalarm.ElectricalAlarmDescriptor;
import mods.eln.sixnode.electricalbreaker.ElectricalBreakerDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor;
import mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerDescriptor;
import mods.eln.sixnode.electricalentitysensor.ElectricalEntitySensorDescriptor;
import mods.eln.sixnode.electricalfiredetector.ElectricalFireDetectorDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceRenderObj;
import mods.eln.sixnode.electricallightsensor.ElectricalLightSensorDescriptor;
import mods.eln.sixnode.electricalmath.ElectricalMathDescriptor;
import mods.eln.sixnode.electricalredstoneinput.ElectricalRedstoneInputDescriptor;
import mods.eln.sixnode.electricalredstoneoutput.ElectricalRedstoneOutputDescriptor;
import mods.eln.sixnode.electricalrelay.ElectricalRelayDescriptor;
import mods.eln.sixnode.electricalsensor.ElectricalSensorDescriptor;
import mods.eln.sixnode.electricalsource.ElectricalSourceDescriptor;
import mods.eln.sixnode.electricalswitch.ElectricalSwitchDescriptor;
import mods.eln.sixnode.electricaltimeout.ElectricalTimeoutDescriptor;
import mods.eln.sixnode.electricalvumeter.ElectricalVuMeterDescriptor;
import mods.eln.sixnode.electricalwatch.ElectricalWatchDescriptor;
import mods.eln.sixnode.electricalweathersensor.ElectricalWeatherSensorDescriptor;
import mods.eln.sixnode.electricalwindsensor.ElectricalWindSensorDescriptor;
import mods.eln.sixnode.energymeter.EnergyMeterDescriptor;
import mods.eln.sixnode.groundcable.GroundCableDescriptor;
import mods.eln.sixnode.hub.HubDescriptor;
import mods.eln.sixnode.lampsocket.*;
import mods.eln.sixnode.lampsupply.LampSupplyDescriptor;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import mods.eln.sixnode.logicgate.*;
import mods.eln.sixnode.modbusrtu.ModbusRtuDescriptor;
import mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixDescriptor;
import mods.eln.sixnode.powerinductorsix.PowerInductorSixDescriptor;
import mods.eln.sixnode.powersocket.PowerSocketDescriptor;
import mods.eln.sixnode.powersocket.PowerSocketElement;
import mods.eln.sixnode.resistor.ResistorDescriptor;
import mods.eln.sixnode.thermalcable.ThermalCableDescriptor;
import mods.eln.sixnode.thermalsensor.ThermalSensorDescriptor;
import mods.eln.sixnode.tutorialsign.TutorialSignDescriptor;
import mods.eln.sixnode.tutorialsign.TutorialSignElement;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.WirelessSignalAnalyserItemDescriptor;
import mods.eln.sixnode.wirelesssignal.repeater.WirelessSignalRepeaterDescriptor;
import mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxDescriptor;
import mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceDescriptor;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxDescriptor;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;
import mods.eln.sound.SoundCommand;
import mods.eln.transparentnode.FuelGeneratorDescriptor;
import mods.eln.transparentnode.FuelHeatFurnaceDescriptor;
import mods.eln.transparentnode.LargeRheostatDescriptor;
import mods.eln.transparentnode.autominer.AutoMinerDescriptor;
import mods.eln.transparentnode.battery.BatteryDescriptor;
import mods.eln.transparentnode.eggincubator.EggIncubatorDescriptor;
import mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxDescriptor;
import mods.eln.transparentnode.electricalantennatx.ElectricalAntennaTxDescriptor;
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor;
import mods.eln.transparentnode.electricalmachine.CompressorDescriptor;
import mods.eln.transparentnode.electricalmachine.MaceratorDescriptor;
import mods.eln.transparentnode.electricalmachine.MagnetizerDescriptor;
import mods.eln.transparentnode.electricalmachine.PlateMachineDescriptor;
import mods.eln.transparentnode.heatfurnace.HeatFurnaceDescriptor;
import mods.eln.transparentnode.powercapacitor.PowerCapacitorDescriptor;
import mods.eln.transparentnode.powerinductor.PowerInductorDescriptor;
import mods.eln.transparentnode.solarpanel.SolarPanelDescriptor;
import mods.eln.transparentnode.teleporter.TeleporterDescriptor;
import mods.eln.transparentnode.teleporter.TeleporterElement;
import mods.eln.transparentnode.thermaldissipatoractive.ThermalDissipatorActiveDescriptor;
import mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor;
import mods.eln.transparentnode.transformer.TransformerDescriptor;
import mods.eln.transparentnode.turbine.TurbineDescriptor;
import mods.eln.transparentnode.turret.TurretDescriptor;
import mods.eln.transparentnode.waterturbine.WaterTurbineDescriptor;
import mods.eln.transparentnode.windturbine.WindTurbineDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LogWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import java.util.*;
import static mods.eln.i18n.I18N.*;
import mods.eln.Eln;
import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;
import static mods.eln.registry.ElnRecipes.*;

/**
 * Device/item registration moved verbatim out of Eln.java (1.12 port, core agent). Unqualified Eln fields/helpers resolve through
 * `import static mods.eln.Eln.*`. TODO(1.12 WP9/WP10): re-enable per device batch; split further per area.
 */
@SuppressWarnings({"SameParameterValue", "PointlessArithmeticExpression", "unused"})
public final class ElnDeviceRegistry {
    private ElnDeviceRegistry() {
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

    public static void registerEnergyConverter() {
        if (ElnToOtherEnergyConverterEnable) {
            String entityName = "eln.EnergyConverterElnToOtherEntity";

            TileEntity.addMapping(EnergyConverterElnToOtherEntity.class, entityName);
            NodeManager.registerUuid(EnergyConverterElnToOtherNode.getNodeUuidStatic(), EnergyConverterElnToOtherNode.class);

            {
                String blockName = TR_NAME(Type.TILE, "eln.EnergyConverterElnToOtherLVUBlock");
                ElnDescriptor elnDesc = new ElnDescriptor(LVU, LVP());
                Ic2Descriptor ic2Desc = new Ic2Descriptor(32, 1);
                OcDescriptor ocDesc = new OcDescriptor(ic2Desc.outMax * Other.getElnToOcConversionRatio() / Other.getElnToIc2ConversionRatio());
                EnergyConverterElnToOtherDescriptor desc =
                    new EnergyConverterElnToOtherDescriptor("EnergyConverterElnToOtherLVU", elnDesc, ic2Desc, ocDesc);
                elnToOtherBlockLvu = new EnergyConverterElnToOtherBlock(desc);
                elnToOtherBlockLvu.setCreativeTab(creativeTab).setBlockName(blockName);
                GameRegistryCompat.registerBlock(elnToOtherBlockLvu, SimpleNodeItem.class, blockName);
            }
            {
                String blockName = TR_NAME(Type.TILE, "eln.EnergyConverterElnToOtherMVUBlock");
                ElnDescriptor elnDesc = new ElnDescriptor(MVU, MVP());
                Ic2Descriptor ic2Desc = new Ic2Descriptor(128, 2);
                OcDescriptor ocDesc = new OcDescriptor(ic2Desc.outMax * Other.getElnToOcConversionRatio() / Other.getElnToIc2ConversionRatio());
                EnergyConverterElnToOtherDescriptor desc =
                    new EnergyConverterElnToOtherDescriptor("EnergyConverterElnToOtherMVU", elnDesc, ic2Desc, ocDesc);
                elnToOtherBlockMvu = new EnergyConverterElnToOtherBlock(desc);
                elnToOtherBlockMvu.setCreativeTab(creativeTab).setBlockName(blockName);
                GameRegistryCompat.registerBlock(elnToOtherBlockMvu, SimpleNodeItem.class, blockName);
            }
            {
                String blockName = TR_NAME(Type.TILE, "eln.EnergyConverterElnToOtherHVUBlock");
                ElnDescriptor elnDesc = new ElnDescriptor(HVU, HVP());
                Ic2Descriptor ic2Desc = new Ic2Descriptor(512, 3);
                OcDescriptor ocDesc = new OcDescriptor(ic2Desc.outMax * Other.getElnToOcConversionRatio() / Other.getElnToIc2ConversionRatio());
                EnergyConverterElnToOtherDescriptor desc =
                    new EnergyConverterElnToOtherDescriptor("EnergyConverterElnToOtherHVU", elnDesc, ic2Desc, ocDesc);
                elnToOtherBlockHvu = new EnergyConverterElnToOtherBlock(desc);
                elnToOtherBlockHvu.setCreativeTab(creativeTab).setBlockName(blockName);
                GameRegistryCompat.registerBlock(elnToOtherBlockHvu, SimpleNodeItem.class, blockName);
            }
        }
    }

    // registerComputer(): OpenComputers/ComputerCraft dropped (rule 8)


    public static void registerTestBlock() {
		/*
		 * testBlock = new TestBlock(); testBlock.setCreativeTab(creativeTab).setBlockName("TestBlock"); GameRegistryCompat.registerBlock(testBlock, "Eln.TestBlock"); TileEntity.addMapping(TestEntity.class, "Eln.TestEntity"); //LanguageRegistry.addName(testBlock,"Test Block"); NodeManager.instance.registerUuid(TestNode.getInfoStatic().getUuid(), TestNode.class);
		 *
		 * GameRegistryCompat.registerCustomItemStack("Test Block", new ItemStack(testBlock));
		 */
    }

    public static void registerElectricalCable(int id) {
        int subId, completId;
        String name;

        CableRenderDescriptor render;
        ElectricalCableDescriptor desc;
        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Signal Cable");

            stdCableRenderSignal = new CableRenderDescriptor("eln",
                "sprites/cable.png", 0.95f, 0.95f);

            desc = new ElectricalCableDescriptor(name, stdCableRenderSignal,
                "For signal transmission.", true);

            signalCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(SVU, SVP, 0.02 / 50
                    * gateOutputCurrent / SVII,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                SVU * 1.3, SVP * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                0.5,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, 1// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
            // GameRegistryCompat.registerCustomItemStack(name, desc.newItemStack(1));

        }

        {
            subId = 4;

            name = TR_NAME(Type.NONE, "Low Voltage Cable");

            stdCableRender50V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 1.95f, 0.95f);

            desc = new ElectricalCableDescriptor(name, stdCableRender50V,
                "For low voltage with high current.", false);

            lowVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(LVU, LVP(), 0.2 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                LVU * 1.3, LVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                20,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);

            desc = new ElectricalCableDescriptor(name, stdCableRender50V,
                "For low voltage with high current.", false);

            desc.setPhysicalConstantLikeNormalCable(
                LVU, LVP() / 4, 0.2 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                LVU * 1.3, LVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                20,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );
            batteryCableDescriptor = desc;

        }

        {
            subId = 8;

            name = TR_NAME(Type.NONE, "Medium Voltage Cable");

            stdCableRender200V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 2.95f, 0.95f);

            desc = new ElectricalCableDescriptor(name, stdCableRender200V,
                "miaou", false);

            meduimVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(MVU, MVP(), 0.10 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                MVU * 1.3, MVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                30,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);

        }
        {
            subId = 12;

            // highVoltageCableId = subId;
            name = TR_NAME(Type.NONE, "High Voltage Cable");

            stdCableRender800V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 3.95f, 1.95f);

            desc = new ElectricalCableDescriptor(name, stdCableRender800V,
                "miaou2", false);

            highVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(HVU, HVP(), 0.025 * 5 / 4 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                HVU * 1.3, HVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                40,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);

        }


        {
            subId = 16;

            // highVoltageCableId = subId;
            name = TR_NAME(Type.NONE, "Very High Voltage Cable");

            stdCableRender3200V = new CableRenderDescriptor("eln",
                "sprites/cableVHV.png", 3.95f, 1.95f);

            desc = new ElectricalCableDescriptor(name, stdCableRender3200V,
                "miaou2", false);

            veryHighVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(VVU, VVP(), 0.025 * 5 / 4 / 20 / 8,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                VVU * 1.3, VVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                40,// electricalOverVoltageStartPowerLost,
                cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                cableHeatingTime, cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);

        }
    }

    public static void registerThermalCable(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;

            name = "Removed from mod Copper Thermal Cable";

            ThermalCableDescriptor desc = new ThermalCableDescriptor(name,
                1000 - 20, -200, // thermalWarmLimit, thermalCoolLimit,
                500, 2000, // thermalStdT, thermalStdPower,
                2, 400, 0.1,// thermalStdDrop, thermalStdLost, thermalTao,
                new CableRenderDescriptor("eln",
                    "sprites/tex_thermalcablebase.png", 4, 4),
                "Miaou !");// description

            desc.addToData(false);
            desc.setDefaultIcon("empty-texture");
            sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);

        }

        {
            subId = 1;

            name = TR_NAME(Type.NONE, "Copper Thermal Cable");

            ThermalCableDescriptor desc = new ThermalCableDescriptor(name,
                1000 - 20, -200, // thermalWarmLimit, thermalCoolLimit,
                500, 2000, // thermalStdT, thermalStdPower,
                2, 10, 0.1,// thermalStdDrop, thermalStdLost, thermalTao,
                new CableRenderDescriptor("eln",
                    "sprites/tex_thermalcablebase.png", 4, 4),
                "Miaou !");// description

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerBattery(int id) {
        int subId, completId;
        String name;
        double heatTIme = 30;
        double[] voltageFunctionTable = {0.000, 0.9, 1.0, 1.025, 1.04, 1.05,
            2.0};
        FunctionTable voltageFunction = new FunctionTable(voltageFunctionTable,
            6.0 / 5);
        double[] condoVoltageFunctionTable = {0.000, 0.89, 0.90, 0.905, 0.91, 1.1,
            1.5};
        FunctionTable condoVoltageFunction = new FunctionTable(condoVoltageFunctionTable,
            6.0 / 5);

        Utils.printFunction(voltageFunction, -0.2, 1.2, 0.1);

        double stdDischargeTime = 4 * 60;
        double stdU = LVU;
        double stdP = LVP() / 4;
        double stdEfficiency = 1.0 - 2.0 / 50.0;
        double condoEfficiency = 1.0 - 2.0 / 50.0;

        batteryVoltageFunctionTable = voltageFunction;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Cost Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction, stdU,
                stdP * 1.2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "Cheap battery" // name, description)
            );
            desc.setRenderSpec("lowcost");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Capacity Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction,
                stdU / 4, stdP / 2 * 1.2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP / 2, stdDischargeTime * 8 * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("capacity");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(Type.NONE, "Voltage Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", meduimVoltageCableDescriptor, 0.5, true, true, voltageFunction, stdU * 4,
                stdP * 1.2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("highvoltage");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 3;
            name = TR_NAME(Type.NONE, "Current Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction, stdU,
                stdP * 1.2 * 4, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP * 4, stdDischargeTime / 6 * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("current");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;
            name = TR_NAME(Type.NONE, "Life Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction, stdU,
                stdP * 1.2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife * 8, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("life");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 5;
            name = TR_NAME(Type.NONE, "Single-use Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", batteryCableDescriptor, 1.0, false, false, voltageFunction, stdU,
                stdP * 1.2 * 2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP * 2, stdDischargeTime * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife * 8, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("coal");
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 32;
            name = TR_NAME(Type.NONE, "50V Condensator");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "condo200", batteryCableDescriptor, 0.0, true, false,
                condoVoltageFunction,
                stdU, stdP * 1.2 * 8, 0.005, // electricalU,//
                // electricalPMax,electricalDischargeRate
                stdP * 8, 4, condoEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "Obselete, must be deleted" // name, description)
            );
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 2.0);
            desc.setDefaultIcon("empty-texture");
            transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }

        {
            subId = 36;
            name = TR_NAME(I18N.Type.NONE, "200V Condensator");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "condo200", highVoltageCableDescriptor, 0.0, true, false,
                condoVoltageFunction,
                MVU, MVP() * 1.5, 0.005, // electricalU,//
                // electricalPMax,electricalDischargeRate
                MVP(), 4, condoEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 2.0);
            desc.setDefaultIcon("empty-texture");
            transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
    }

    public static void registerGround(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Ground Cable");

            GroundCableDescriptor desc = new GroundCableDescriptor(name, obj.getObj("groundcable"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(Type.NONE, "Hub");

            HubDescriptor desc = new HubDescriptor(name, obj.getObj("hub"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalSource(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Electrical Source");

            ElectricalSourceDescriptor desc = new ElectricalSourceDescriptor(
                name, obj.getObj("voltagesource"), false);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Signal Source");

            ElectricalSourceDescriptor desc = new ElectricalSourceDescriptor(
                name, obj.getObj("signalsource"), true);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerLampSocket(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Lamp Socket A");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("ClassicLampSocket"), false),
                LampSocketType.Douille, // LampSocketType
                false,
                4, 0, 0, 0);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(Type.NONE, "Lamp Socket B Projector");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("ClassicLampSocket"), false),
                LampSocketType.Douille, // LampSocketType
                false,
                10, -90, 90, 0);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 4;

            name = TR_NAME(Type.NONE, "Robust Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("RobustLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            desc.setInitialOrientation(-90.f);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 5;

            name = TR_NAME(Type.NONE, "Flat Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("FlatLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 6;

            name = TR_NAME(Type.NONE, "Simple Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("SimpleLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 7;

            name = TR_NAME(Type.NONE, "Fluorescent Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("FluorescentLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                4, 0, 0, 0);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);


            desc.cableLeft = false;
            desc.cableRight = false;
        }
        {
            subId = 8;

            name = TR_NAME(Type.NONE, "Street Light");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("StreetLight"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                0, 0, 0, 0);
            desc.setPlaceDirection(Direction.YN);
            GhostGroup g = new GhostGroup();
            g.addElement(1, 0, 0);
            g.addElement(2, 0, 0);
            desc.setGhostGroup(g);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.cameraOpt = false;
        }
        {
            subId = 9;

            name = TR_NAME(Type.NONE, "Sconce Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("SconceLamp"), true),
                LampSocketType.Douille, // LampSocketType
                true,
                3, 0, 0, 0);
            desc.setPlaceDirection(new Direction[]{Direction.XP, Direction.XN, Direction.ZP, Direction.ZN});
            desc.setInitialOrientation(-90.f);
            desc.setUserRotationLibertyDegrees(true);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 12;

            name = TR_NAME(Type.NONE, "Suspended Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name,
                new LampSocketSuspendedObjRender(obj.getObj("RobustLampSuspended"), true, 3),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            desc.setPlaceDirection(Direction.YP);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.cameraOpt = false;
        }
        {
            subId = 13;

            name = TR_NAME(Type.NONE, "Long Suspended Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name,
                new LampSocketSuspendedObjRender(obj.getObj("RobustLampSuspended"), true, 7),
                LampSocketType.Douille, // LampSocketType
                false,
                4, 0, 0, 0);
            desc.setPlaceDirection(Direction.YP);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.cameraOpt = false;
        }

        // TODO: Modern street light.

        sixNodeItem.addDescriptor(15 + (id << 6),
            new EmergencyLampDescriptor(TR_NAME(Type.NONE, "50V Emergency Lamp"),
                lowVoltageCableDescriptor, 10 * 60 * 10, 10, 5, 6, obj.getObj("EmergencyExitLighting")));

        sixNodeItem.addDescriptor(16 + (id << 6),
            new EmergencyLampDescriptor(TR_NAME(Type.NONE, "200V Emergency Lamp"),
                meduimVoltageCableDescriptor, 10 * 60 * 20, 25, 10, 8, obj.getObj("EmergencyExitLighting")));
    }

    public static void registerLampSupply(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Lamp Supply");

            LampSupplyDescriptor desc = new LampSupplyDescriptor(
                name, obj.getObj("DistributionBoard"),
                32
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerPowerSocket(int id) {
        int subId;
        String name;
        PowerSocketDescriptor desc;
        {
            subId = 1;
            name = TR_NAME(Type.NONE, "50V Power Socket");
            desc = new PowerSocketDescriptor(
                subId, name, obj.getObj("PowerSocket"),
                10 //Range for plugged devices (without obstacles)
            );
            desc.setPlaceDirection(new Direction[]{Direction.XP, Direction.XN, Direction.ZP, Direction.ZN});
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(Type.NONE, "200V Power Socket");
            desc = new PowerSocketDescriptor(
                subId, name, obj.getObj("PowerSocket"),
                10 //Range for plugged devices (without obstacles)
            );
            desc.setPlaceDirection(new Direction[]{Direction.XP, Direction.XN, Direction.ZP, Direction.ZN});
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerPassiveComponent(int id) {
        int subId, completId;
        String name;
        IFunction function;
        FunctionTableYProtect baseFunction = new FunctionTableYProtect(
            new double[]{0.0, 0.01, 0.03, 0.1, 0.2, 0.4, 0.8, 1.2}, 1.0,
            0, 5);

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "10A Diode");

            function = new FunctionTableYProtect(new double[]{0.0, 0.1, 0.3,
                1.0, 2.0, 4.0, 8.0, 12.0}, 1.0, 0, 100);

            DiodeDescriptor desc = new DiodeDescriptor(
                name,// int iconId, String name,
                function,
                10, // double Imax,
                1, 10,
                sixNodeThermalLoadInitializer.copy(),
                lowVoltageCableDescriptor,
                obj.getObj("PowerElectricPrimitives"));

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 1;

            name = TR_NAME(Type.NONE, "25A Diode");

            function = new FunctionTableYProtect(new double[]{0.0, 0.25,
                0.75, 2.5, 5.0, 10.0, 20.0, 30.0}, 1.0, 0, 100);

            DiodeDescriptor desc = new DiodeDescriptor(
                name,// int iconId, String name,
                function,
                25, // double Imax,
                1, 25,
                sixNodeThermalLoadInitializer.copy(),
                lowVoltageCableDescriptor,
                obj.getObj("PowerElectricPrimitives"));

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 8;

            name = TR_NAME(Type.NONE, "Signal Diode");

            function = baseFunction.duplicate(1.0, 0.1);

            DiodeDescriptor desc = new DiodeDescriptor(name,// int iconId,
                // String name,
                function, 0.1, // double Imax,
                1, 0.1,
                sixNodeThermalLoadInitializer.copy(), signalCableDescriptor,
                obj.getObj("PowerElectricPrimitives"));

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 16;

            name = TR_NAME(Type.NONE, "Signal 20H inductor");

            SignalInductorDescriptor desc = new SignalInductorDescriptor(
                name, 20, lowVoltageCableDescriptor
            );

            desc.setDefaultIcon("empty-texture");
            sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }

        {
            subId = 32;

            name = TR_NAME(Type.NONE, "Power Capacitor");

            PowerCapacitorSixDescriptor desc = new PowerCapacitorSixDescriptor(
                name, obj.getObj("PowerElectricPrimitives"), SerieEE.newE6(-1), 60 * 2000
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 34;

            name = TR_NAME(Type.NONE, "Power Inductor");

            PowerInductorSixDescriptor desc = new PowerInductorSixDescriptor(
                name, obj.getObj("PowerElectricPrimitives"), SerieEE.newE6(-1)
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 36;

            name = TR_NAME(Type.NONE, "Power Resistor");

            ResistorDescriptor desc = new ResistorDescriptor(
                name, obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), 0, false
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 37;
            name = TR_NAME(Type.NONE, "Rheostat");

            ResistorDescriptor desc = new ResistorDescriptor(
                name, obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), 0, true
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 38;

            name = TR_NAME(Type.NONE, "Thermistor");

            ResistorDescriptor desc = new ResistorDescriptor(
                name, obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), -0.01, false
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 39;

            name = TR_NAME(Type.NONE, "Large Rheostat");

            ThermalDissipatorPassiveDescriptor dissipator = new ThermalDissipatorPassiveDescriptor(
                name,
                obj.getObj("LargeRheostat"),
                1000, -100,// double warmLimit,double coolLimit,
                4000, 800,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop
            );
            LargeRheostatDescriptor desc = new LargeRheostatDescriptor(
                name, dissipator, veryHighVoltageCableDescriptor, SerieEE.newE12(0)
            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerPowerComponent(int id) {
        int subId, completId;
        String name;

        {
            subId = 16;

            name = TR_NAME(Type.NONE, "Power inductor");

            PowerInductorDescriptor desc = new PowerInductorDescriptor(
                name, null, SerieEE.newE12(-1)
            );

            transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }

        {
            subId = 20;

            name = TR_NAME(Type.NONE, "Power capacitor");

            PowerCapacitorDescriptor desc = new PowerCapacitorDescriptor(
                name, null, SerieEE.newE6(-2), 300
            );

            transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
    }

    public static void registerSwitch(int id) {
        int subId, completId;
        String name;
        IFunction function;
        ElectricalSwitchDescriptor desc;


        {
            subId = 4;

            name = TR_NAME(Type.NONE, "Very High Voltage Switch");

            desc = new ElectricalSwitchDescriptor(name, stdCableRender3200V,
                obj.getObj("HighVoltageSwitch"), VVU, VVP(), veryHighVoltageCableDescriptor.electricalRs * 2,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                VVU * 1.5, VVP() * 1.2,// maximalVoltage, maximalPower
                cableThermalLoadInitializer.copy(), false);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "High Voltage Switch");

            desc = new ElectricalSwitchDescriptor(name, stdCableRender800V,
                obj.getObj("HighVoltageSwitch"), HVU, HVP(), highVoltageCableDescriptor.electricalRs * 2,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                HVU * 1.5, HVP() * 1.2,// maximalVoltage, maximalPower
                cableThermalLoadInitializer.copy(), false);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(Type.NONE, "Low Voltage Switch");

            desc = new ElectricalSwitchDescriptor(name, stdCableRender50V,
                obj.getObj("LowVoltageSwitch"), LVU, LVP(), lowVoltageCableDescriptor.electricalRs * 2,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                LVU * 1.5, LVP() * 1.2,// maximalVoltage, maximalPower
                cableThermalLoadInitializer.copy(), false);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;

            name = TR_NAME(Type.NONE, "Medium Voltage Switch");

            desc = new ElectricalSwitchDescriptor(name, stdCableRender200V,
                obj.getObj("LowVoltageSwitch"), MVU, MVP(), meduimVoltageCableDescriptor.electricalRs * 2,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                MVU * 1.5, MVP() * 1.2,// maximalVoltage, maximalPower
                cableThermalLoadInitializer.copy(), false);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;

            name = TR_NAME(Type.NONE, "Signal Switch");

            desc = new ElectricalSwitchDescriptor(name, stdCableRenderSignal,
                obj.getObj("LowVoltageSwitch"), SVU, SVP, 0.02,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                SVU * 1.5, SVP * 1.2,// maximalVoltage, maximalPower
                cableThermalLoadInitializer.copy(), true);

            sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
        // 4 taken
        {
            subId = 8;

            name = TR_NAME(Type.NONE, "Signal Switch with LED");

            desc = new ElectricalSwitchDescriptor(name, stdCableRenderSignal,
                obj.getObj("ledswitch"), SVU, SVP, 0.02,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                SVU * 1.5, SVP * 1.2,// maximalVoltage, maximalPower
                cableThermalLoadInitializer.copy(), true);

            sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }

    }

    public static void registerSixNodeMisc(int id) {

        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Modbus RTU");

            ModbusRtuDescriptor desc = new ModbusRtuDescriptor(
                name,
                obj.getObj("RTU")

            );

            if (modbusEnable) {
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            } else {
                sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
            }
        }

        {
            subId = 4;
            name = TR_NAME(Type.NONE, "Analog Watch");

            ElectricalWatchDescriptor desc = new ElectricalWatchDescriptor(
                name,
                obj.getObj("WallClock"),
                20000.0 / (3600 * 40)

            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 5;
            name = TR_NAME(Type.NONE, "Digital Watch");

            ElectricalWatchDescriptor desc = new ElectricalWatchDescriptor(
                name,
                obj.getObj("DigitalWallClock"),
                20000.0 / (3600 * 15)

            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 8;
            name = TR_NAME(Type.NONE, "Tutorial Sign");

            TutorialSignDescriptor desc = new TutorialSignDescriptor(
                name, obj.getObj("TutoPlate"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalManager(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Electrical Breaker");

            ElectricalBreakerDescriptor desc = new ElectricalBreakerDescriptor(name, obj.getObj("ElectricalBreaker"));

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;

            name = TR_NAME(Type.NONE, "Energy Meter");

            EnergyMeterDescriptor desc = new EnergyMeterDescriptor(name, obj.getObj("EnergyMeter"), 8, 0);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 5;

            name = TR_NAME(Type.NONE, "Advanced Energy Meter");

            EnergyMeterDescriptor desc = new EnergyMeterDescriptor(name, obj.getObj("AdvancedEnergyMeter"), 7, 8);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 6;

            name = TR_NAME(Type.NONE, "Electrical Fuse Holder");

            ElectricalFuseHolderDescriptor desc = new ElectricalFuseHolderDescriptor(name, obj.getObj("ElectricalFuse"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 7;

            name = TR_NAME(Type.NONE, "Lead Fuse for low voltage cables");

            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, lowVoltageCableDescriptor, obj.getObj("ElectricalFuse"));
            sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 8;

            name = TR_NAME(Type.NONE, "Lead Fuse for medium voltage cables");

            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, meduimVoltageCableDescriptor, obj.getObj("ElectricalFuse"));
            sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 9;

            name = TR_NAME(Type.NONE, "Lead Fuse for high voltage cables");

            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, highVoltageCableDescriptor, obj.getObj("ElectricalFuse"));
            sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 10;

            name = TR_NAME(Type.NONE, "Lead Fuse for very high voltage cables");

            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, veryHighVoltageCableDescriptor, obj.getObj("ElectricalFuse"));
            sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 11;

            name = TR_NAME(Type.NONE, "Blown Lead Fuse");

            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, null, obj.getObj("ElectricalFuse"));
            ElectricalFuseDescriptor.Companion.setBlownFuse(desc);
            sharedItem.addWithoutRegistry(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalSensor(int id) {
        int subId, completId;
        String name;
        ElectricalSensorDescriptor desc;

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Electrical Probe");

            desc = new ElectricalSensorDescriptor(name, "electricalsensor",
                false);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(Type.NONE, "Voltage Probe");

            desc = new ElectricalSensorDescriptor(name, "voltagesensor", true);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerThermalSensor(int id) {
        int subId, completId;
        String name;
        ThermalSensorDescriptor desc;

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Thermal Probe");

            desc = new ThermalSensorDescriptor(name,
                obj.getObj("thermalsensor"), false);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(Type.NONE, "Temperature Probe");

            desc = new ThermalSensorDescriptor(name,
                obj.getObj("temperaturesensor"), true);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerElectricalVuMeter(int id) {
        int subId, completId;
        String name;
        ElectricalVuMeterDescriptor desc;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Analog vuMeter");
            desc = new ElectricalVuMeterDescriptor(name, "Vumeter", false);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(Type.NONE, "LED vuMeter");
            desc = new ElectricalVuMeterDescriptor(name, "Led", true);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalAlarm(int id) {
        int subId, completId;
        String name;
        ElectricalAlarmDescriptor desc;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Nuclear Alarm");
            desc = new ElectricalAlarmDescriptor(name,
                obj.getObj("alarmmedium"), 7, "eln:alarma", 11, 1f);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Standard Alarm");
            desc = new ElectricalAlarmDescriptor(name,
                obj.getObj("alarmmedium"), 7, "eln:smallalarm_critical",
                1.2, 2f);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalEnvironmentalSensor(int id) {
        int subId, completId;
        String name;
        {
            ElectricalLightSensorDescriptor desc;
            {
                subId = 0;
                name = TR_NAME(Type.NONE, "Electrical Daylight Sensor");
                desc = new ElectricalLightSensorDescriptor(name, obj.getObj("daylightsensor"), true);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
            {
                subId = 1;
                name = TR_NAME(Type.NONE, "Electrical Light Sensor");
                desc = new ElectricalLightSensorDescriptor(name, obj.getObj("lightsensor"), false);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalWeatherSensorDescriptor desc;
            {
                subId = 4;
                name = TR_NAME(Type.NONE, "Electrical Weather Sensor");
                desc = new ElectricalWeatherSensorDescriptor(name, obj.getObj("electricalweathersensor"));
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalWindSensorDescriptor desc;
            {
                subId = 8;
                name = TR_NAME(Type.NONE, "Electrical Anemometer Sensor");
                desc = new ElectricalWindSensorDescriptor(name, obj.getObj("Anemometer"), 25);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalEntitySensorDescriptor desc;
            {
                subId = 12;
                name = TR_NAME(Type.NONE, "Electrical Entity Sensor");
                desc = new ElectricalEntitySensorDescriptor(name, obj.getObj("ProximitySensor"), 10);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalFireDetectorDescriptor desc;
            {
                subId = 13;
                name = TR_NAME(Type.NONE, "Electrical Fire Detector");
                desc = new ElectricalFireDetectorDescriptor(name, obj.getObj("FireDetector"), 15, false);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalFireDetectorDescriptor desc;
            {
                subId = 14;
                name = TR_NAME(Type.NONE, "Electrical Fire Buzzer");
                desc = new ElectricalFireDetectorDescriptor(name, obj.getObj("FireDetector"), 15, true);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ScannerDescriptor desc;
            {
                subId = 15;
                name = TR_NAME(Type.NONE, "Scanner");
                desc = new ScannerDescriptor(name, obj.getObj("scanner"));
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
    }

    public static void registerElectricalRedstone(int id) {
        int subId, completId;
        String name;
        {
            ElectricalRedstoneInputDescriptor desc;
            subId = 0;
            name = TR_NAME(Type.NONE, "Redstone-to-Voltage Converter");
            desc = new ElectricalRedstoneInputDescriptor(name, obj.getObj("redtoele"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            ElectricalRedstoneOutputDescriptor desc;
            subId = 1;
            name = TR_NAME(Type.NONE, "Voltage-to-Redstone Converter");
            desc = new ElectricalRedstoneOutputDescriptor(name,
                obj.getObj("eletored"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalGate(int id) {
        int subId, completId;
        String name;
        {
            ElectricalTimeoutDescriptor desc;
            subId = 0;

            name = TR_NAME(Type.NONE, "Electrical Timer");

            desc = new ElectricalTimeoutDescriptor(name,
                obj.getObj("electricaltimer"));
            desc.setTickSound("eln:timer", 0.01f);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            ElectricalMathDescriptor desc;
            subId = 4;

            name = TR_NAME(Type.NONE, "Signal Processor");

            desc = new ElectricalMathDescriptor(name,
                obj.getObj("PLC"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerWirelessSignal(int id) {
        int subId, completId;
        String name;

        {
            WirelessSignalRxDescriptor desc;
            subId = 0;

            name = TR_NAME(Type.NONE, "Wireless Signal Receiver");

            desc = new WirelessSignalRxDescriptor(
                name,
                obj.getObj("wirelesssignalrx")

            );
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            WirelessSignalTxDescriptor desc;
            subId = 8;

            name = TR_NAME(Type.NONE, "Wireless Signal Transmitter");

            desc = new WirelessSignalTxDescriptor(
                name,
                obj.getObj("wirelesssignaltx"),
                wirelessTxRange
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            WirelessSignalRepeaterDescriptor desc;
            subId = 16;

            name = TR_NAME(Type.NONE, "Wireless Signal Repeater");

            desc = new WirelessSignalRepeaterDescriptor(
                name,
                obj.getObj("wirelesssignalrepeater"),
                wirelessTxRange
            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerElectricalDataLogger(int id) {
        int subId, completId;
        String name;
        {
            ElectricalDataLoggerDescriptor desc;
            subId = 0;

            name = TR_NAME(Type.NONE, "Data Logger");

            desc = new ElectricalDataLoggerDescriptor(name, true,
                "DataloggerCRTFloor", 1f, 0.5f, 0f, "\u00a76");
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            ElectricalDataLoggerDescriptor desc;
            subId = 1;

            name = TR_NAME(Type.NONE, "Modern Data Logger");

            desc = new ElectricalDataLoggerDescriptor(name, true,
                "FlatScreenMonitor", 0.0f, 1f, 0.0f, "\u00A7a");
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            ElectricalDataLoggerDescriptor desc;
            subId = 2;

            name = TR_NAME(Type.NONE, "Industrial Data Logger");

            desc = new ElectricalDataLoggerDescriptor(name, false,
                "IndustrialPanel", 0.25f, 0.5f, 1f, "\u00A7f");
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalRelay(int id) {
        int subId, completId;
        String name;
        ElectricalRelayDescriptor desc;

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Low Voltage Relay");

            desc = new ElectricalRelayDescriptor(
                name, obj.getObj("RelayBig"),
                lowVoltageCableDescriptor);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(Type.NONE, "Medium Voltage Relay");

            desc = new ElectricalRelayDescriptor(
                name, obj.getObj("RelayBig"),
                meduimVoltageCableDescriptor);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;

            name = TR_NAME(Type.NONE, "High Voltage Relay");

            desc = new ElectricalRelayDescriptor(
                name, obj.getObj("relay800"),
                highVoltageCableDescriptor);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;

            name = TR_NAME(Type.NONE, "Very High Voltage Relay");

            desc = new ElectricalRelayDescriptor(
                name, obj.getObj("relay800"),
                veryHighVoltageCableDescriptor);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 4;

            name = TR_NAME(Type.NONE, "Signal Relay");

            desc = new ElectricalRelayDescriptor(
                name, obj.getObj("RelaySmall"),
                signalCableDescriptor);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalGateSource(int id) {
        int subId, completId;
        String name;

        ElectricalGateSourceRenderObj signalsourcepot = new ElectricalGateSourceRenderObj(obj.getObj("signalsourcepot"));
        ElectricalGateSourceRenderObj ledswitch = new ElectricalGateSourceRenderObj(obj.getObj("ledswitch"));

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Signal Trimmer");

            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, signalsourcepot, false,
                "trimmer");

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(Type.NONE, "Signal Switch");

            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, ledswitch, true,
                Eln.noSymbols ? "signalswitch" : "switch");

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;

            name = TR_NAME(Type.NONE, "Signal Button");

            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, ledswitch, true, "button");
            desc.setWithAutoReset();
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 12;

            name = TR_NAME(Type.NONE, "Wireless Button");

            WirelessSignalSourceDescriptor desc = new WirelessSignalSourceDescriptor(name, ledswitch, wirelessTxRange, true);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 16;

            name = TR_NAME(Type.NONE, "Wireless Switch");

            WirelessSignalSourceDescriptor desc = new WirelessSignalSourceDescriptor(name, ledswitch, wirelessTxRange, false);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerLogicalGates(int id) {
        Obj3D model = obj.getObj("LogicGates");
        sixNodeItem.addDescriptor(0 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "NOT Chip"), model, "NOT", Not.class));

        sixNodeItem.addDescriptor(1 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "AND Chip"), model, "AND", And.class));
        sixNodeItem.addDescriptor(2 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "NAND Chip"), model, "NAND", Nand.class));

        sixNodeItem.addDescriptor(3 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "OR Chip"), model, "OR", Or.class));
        sixNodeItem.addDescriptor(4 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "NOR Chip"), model, "NOR", Nor.class));

        sixNodeItem.addDescriptor(5 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "XOR Chip"), model, "XOR", Xor.class));
        sixNodeItem.addDescriptor(6 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "XNOR Chip"), model, "XNOR", XNor.class));

        sixNodeItem.addDescriptor(7 + (id << 6),
            new PalDescriptor(TR_NAME(Type.NONE, "PAL Chip"), model));

        sixNodeItem.addDescriptor(8 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "Schmitt Trigger Chip"), model, "SCHMITT",
                SchmittTrigger.class));

        sixNodeItem.addDescriptor(9 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "D Flip Flop Chip"), model, "DFF", DFlipFlop.class));

        sixNodeItem.addDescriptor(10 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "Oscillator Chip"), model, "OSC", Oscillator.class));

        sixNodeItem.addDescriptor(11 + (id << 6),
            new LogicGateDescriptor(TR_NAME(Type.NONE, "JK Flip Flop Chip"), model, "JKFF", JKFlipFlop.class));
    }

    public static void registerAnalogChips(int id) {
        id <<= 6;

        Obj3D model = obj.getObj("AnalogChips");
        sixNodeItem.addDescriptor(id + 0,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "OpAmp"), model, "OP", OpAmp.class));

        sixNodeItem.addDescriptor(id + 1, new AnalogChipDescriptor(TR_NAME(Type.NONE, "PID Regulator"), model, "PID",
            PIDRegulator.class, PIDRegulatorElement.class, PIDRegulatorRender.class));

        sixNodeItem.addDescriptor(id + 2,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Voltage controlled sawtooth oscillator"), model, "VCO-SAW",
                VoltageControlledSawtoothOscillator.class));

        sixNodeItem.addDescriptor(id + 3,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Voltage controlled sine oscillator"), model, "VCO-SIN",
                VoltageControlledSineOscillator.class));

        sixNodeItem.addDescriptor(id + 4,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Amplifier"), model, "AMP",
                Amplifier.class, AmplifierElement.class, AmplifierRender.class));

        sixNodeItem.addDescriptor(id + 5,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Voltage controlled amplifier"), model, "VCA",
                VoltageControlledAmplifier.class));

        sixNodeItem.addDescriptor(id + 6,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Configurable summing unit"), model, "SUM",
                SummingUnit.class, SummingUnitElement.class, SummingUnitRender.class));

        sixNodeItem.addDescriptor(id + 7,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Sample and hold"), model, "SAH",
                SampleAndHold.class));

        sixNodeItem.addDescriptor(id + 8,
            new AnalogChipDescriptor(TR_NAME(Type.NONE, "Lowpass filter"), model, "LPF",
                Filter.class, FilterElement.class, FilterRender.class));
    }

    public static void registerTransformer(int id) {
        int subId;
        String name;

        {
            subId = 0;
            name = TR_NAME(Type.NONE, "DC-DC Converter");

            TransformerDescriptor desc = new TransformerDescriptor(name, obj.getObj("transformator"),
                obj.getObj("feromagneticcorea"), obj.getObj("transformatorCase"), 0.5f);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerHeatFurnace(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Stone Heat Furnace");

            HeatFurnaceDescriptor desc = new HeatFurnaceDescriptor(name,
                "stonefurnace", 1000,
                Utils.getCoalEnergyReference() * 2 / 3,// double
                // nominalPower,
                // double
                // nominalCombustibleEnergy,
                2, 500,// int combustionChamberMax,double
                // combustionChamberPower,
                new ThermalLoadInitializerByPowerDrop(780, -100, 10, 2) // thermal
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Fuel Heat Furnace");

            FuelHeatFurnaceDescriptor desc = new FuelHeatFurnaceDescriptor(name,
                obj.getObj("FuelHeater"), new ThermalLoadInitializerByPowerDrop(780, -100, 10, 2));
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerTurbine(int id) {
        int subId, completId;
        String name;

        FunctionTable TtoU = new FunctionTable(new double[]{0, 0.1, 0.85,
            1.0, 1.1, 1.15, 1.18, 1.19, 1.25}, 8.0 / 5.0);
        FunctionTable PoutToPin = new FunctionTable(new double[]{0.0, 0.2,
            0.4, 0.6, 0.8, 1.0, 1.3, 1.8, 2.7}, 8.0 / 5.0);

        {
            subId = 1;
            name = TR_NAME(Type.NONE, "50V Turbine");
            double RsFactor = 0.1;
            double nominalU = LVU;
            double nominalP = 300 * heatTurbinePowerFactor;
            double nominalDeltaT = 250;
            TurbineDescriptor desc = new TurbineDescriptor(name, "turbineb", lowVoltageCableDescriptor.render,
                TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                nominalU, nominalP, nominalP / 40, lowVoltageCableDescriptor.electricalRs * RsFactor, 25.0,
                nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_50v");
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 8;
            name = TR_NAME(Type.NONE, "200V Turbine");
            double RsFactor = 0.10;
            double nominalU = MVU;
            double nominalP = 500 * heatTurbinePowerFactor;
            double nominalDeltaT = 350;
            TurbineDescriptor desc = new TurbineDescriptor(name, "turbinebblue", meduimVoltageCableDescriptor.render,
                TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                nominalU, nominalP, nominalP / 40, meduimVoltageCableDescriptor.electricalRs * RsFactor, 50.0,
                nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_200v");
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

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

    public static void registerElectricalFurnace(int id) {
        int subId, completId;
        String name;
        furnaceList.add(new ItemStack(Blocks.FURNACE));
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Electrical Furnace");
            double[] PfTTable = new double[]{0, 20, 40, 80, 160, 240, 360,
                540, 756, 1058.4, 1481.76};

            double[] thermalPlostfTTable = new double[PfTTable.length];
            for (int idx = 0; idx < thermalPlostfTTable.length; idx++) {
                thermalPlostfTTable[idx] = PfTTable[idx]
                    * Math.pow((idx + 1.0) / thermalPlostfTTable.length, 2)
                    * 2;
            }

            FunctionTableYProtect PfT = new FunctionTableYProtect(PfTTable,
                800.0, 0, 100000.0);

            FunctionTableYProtect thermalPlostfT = new FunctionTableYProtect(
                thermalPlostfTTable, 800.0, 0.001, 10000000.0);

            ElectricalFurnaceDescriptor desc = new ElectricalFurnaceDescriptor(
                name, PfT, thermalPlostfT,// thermalPlostfT;
                40// thermalC;
            );
            electricalFurnace = desc;
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            furnaceList.add(desc.newItemStack());

            // Utils.smeltRecipeList.addMachine(desc.newItemStack());
        }
        // Utils.smeltRecipeList.addMachine(new ItemStack(Blocks.FURNACE));
    }

    public static void registerMacerator(int id) {
        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "50V Macerator");

            MaceratorDescriptor desc = new MaceratorDescriptor(name,
                "maceratora", LVU, 200,// double nominalU,double nominalP,
                LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                maceratorRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:macerator");
        }

        {
            subId = 4;
            name = TR_NAME(Type.NONE, "200V Macerator");

            MaceratorDescriptor desc = new MaceratorDescriptor(name,
                "maceratorb", MVU, 2000,// double nominalU,double nominalP,
                MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                maceratorRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:macerator");
        }
    }

    public static void registerPlateMachine(int id) {

        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "50V Plate Machine");

            PlateMachineDescriptor desc = new PlateMachineDescriptor(
                name,// String name,
                obj.getObj("platemachinea"),
                LVU, 200,// double nominalU,double nominalP,
                LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                plateMachineRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:plate_machine");

        }

        {
            subId = 4;
            name = TR_NAME(Type.NONE, "200V Plate Machine");

            PlateMachineDescriptor desc = new PlateMachineDescriptor(
                name,// String name,
                obj.getObj("platemachineb"),
                MVU, 2000,// double nominalU,double nominalP,
                MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                plateMachineRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:plate_machine");

        }
    }

    public static void registerEggIncubator(int id) {

        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "50V Egg Incubator");

            EggIncubatorDescriptor desc = new EggIncubatorDescriptor(
                name, obj.getObj("eggincubator"),
                lowVoltageCableDescriptor,
                LVU, 50);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerCompressor(int id) {

        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "50V Compressor");

            CompressorDescriptor desc = new CompressorDescriptor(
                name,// String name,
                obj.getObj("compressora"),
                LVU, 200,// double nominalU,double nominalP,
                LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                compressorRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);

            desc.setRunningSound("eln:compressor_run");
            desc.setEndSound(new SoundCommand("eln:compressor_end"));
        }

        {
            subId = 4;
            name = TR_NAME(Type.NONE, "200V Compressor");

            CompressorDescriptor desc = new CompressorDescriptor(
                name,// String name,
                obj.getObj("compressorb"),
                MVU, 2000,// double nominalU,double nominalP,
                MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                compressorRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:compressor_run");
            desc.setEndSound(new SoundCommand("eln:compressor_end"));
        }
    }

    public static void registerMagnetizer(int id) {

        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "50V Magnetizer");

            MagnetizerDescriptor desc = new MagnetizerDescriptor(
                name,// String name,
                obj.getObj("magnetizera"),
                LVU, 200,// double nominalU,double nominalP,
                LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                magnetiserRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);

            desc.setRunningSound("eln:Motor");
        }

        {
            subId = 4;
            name = TR_NAME(Type.NONE, "200V Magnetizer");

            MagnetizerDescriptor desc = new MagnetizerDescriptor(
                name,// String name,
                obj.getObj("magnetizerb"),
                MVU, 2000,// double nominalU,double nominalP,
                MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                magnetiserRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);

            desc.setRunningSound("eln:Motor");
        }
    }

    public static void registerSolarPanel(int id) {
        int subId, completId;
        GhostGroup ghostGroup;
        String name;

        FunctionTable diodeIfUBase;
        diodeIfUBase = new FunctionTableYProtect(new double[]{0.0, 0.002,
            0.005, 0.01, 0.015, 0.02, 0.025, 0.03, 0.035, 0.04, 0.045,
            0.05, 0.06, 0.07, 0.08, 0.09, 0.10, 0.11, 0.12, 0.13, 1.0},
            1.0, 0, 1.0);

        FunctionTable solarIfSBase;
        solarIfSBase = new FunctionTable(new double[]{0.0, 0.1, 0.4, 0.6,
            0.8, 1.0}, 1);

        double LVSolarU = 59;

        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Small Solar Panel");

            ghostGroup = new GhostGroup();

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,// String
                // name,
                obj.getObj("smallsolarpannel"), null,
                ghostGroup, 0, 1, 0,// GhostGroup ghostGroup, int
                // solarOffsetX,int solarOffsetY,int
                // solarOffsetZ,
                // FunctionTable solarIfSBase,
                null, LVSolarU / 4, 65.0 * solarPanelPowerFactor,// double electricalUmax,double
                // electricalPmax,
                0.01,// ,double electricalDropFactor
                Math.PI / 2, Math.PI / 2 // alphaMin alphaMax
            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(Type.NONE, "Small Rotating Solar Panel");

            ghostGroup = new GhostGroup();

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,// String
                // name,
                obj.getObj("smallsolarpannelrot"), lowVoltageCableDescriptor.render,
                ghostGroup, 0, 1, 0,// GhostGroup ghostGroup, int
                // solarOffsetX,int solarOffsetY,int
                // solarOffsetZ,
                // FunctionTable solarIfSBase,
                null, LVSolarU / 4, solarPanelBasePower * solarPanelPowerFactor,// double electricalUmax,double
                // electricalPmax,
                0.01,// ,double electricalDropFactor
                Math.PI / 4, Math.PI / 4 * 3 // alphaMin alphaMax
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;
            name = TR_NAME(Type.NONE, "2x3 Solar Panel");

            Coordonate groundCoordinate = new Coordonate(1, 0, 0, 0);

            ghostGroup = new GhostGroup();
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1);
            ghostGroup.removeElement(0, 0, 0);

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,
                obj.getObj("bigSolarPanel"), meduimVoltageCableDescriptor.render,
                ghostGroup, 1, 1, 0,
                groundCoordinate,
                LVSolarU * 2, solarPanelBasePower * solarPanelPowerFactor * 8,
                0.01,
                Math.PI / 2, Math.PI / 2
            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;
            name = TR_NAME(Type.NONE, "2x3 Rotating Solar Panel");

            Coordonate groundCoordinate = new Coordonate(1, 0, 0, 0);

            ghostGroup = new GhostGroup();
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1);
            ghostGroup.removeElement(0, 0, 0);

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,
                obj.getObj("bigSolarPanelrot"), meduimVoltageCableDescriptor.render,
                ghostGroup, 1, 1, 1,
                groundCoordinate,
                LVSolarU * 2, solarPanelBasePower * solarPanelPowerFactor * 8,
                0.01,
                Math.PI / 8 * 3, Math.PI / 8 * 5
            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
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

    public static void registerLampItem(int id) {
        int subId, completId;
        double[] lightPower = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            15, 20, 25, 30, 40};
        double[] lightLevel = new double[16];
        double economicPowerFactor = 0.5;
        double standardGrowRate = 0.0;
        for (int idx = 0; idx < 16; idx++) {
            lightLevel[idx] = (idx + 0.49) / 15.0;
        }
        LampDescriptor element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(Type.NONE, "Small 50V Incandescent Light Bulb"),
                "incandescentironlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, LVU, lightPower[12], // nominalU,
                // nominalP
                lightLevel[12], incandescentLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            sharedItem.addElement(completId, element);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(Type.NONE, "50V Incandescent Light Bulb"),
                "incandescentironlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, LVU, lightPower[14], // nominalU,
                // nominalP
                lightLevel[14], incandescentLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            sharedItem.addElement(completId, element);
        }
        {
            subId = 2;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(Type.NONE, "200V Incandescent Light Bulb"),
                "incandescentironlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, MVU, lightPower[14], // nominalU,
                // nominalP
                lightLevel[14], incandescentLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            sharedItem.addElement(completId, element);
        }

        {
            subId = 4;
            completId = subId + (id << 6);
            element = new LampDescriptor(
                TR_NAME(Type.NONE, "Small 50V Carbon Incandescent Light Bulb"),
                "incandescentcarbonlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, LVU, lightPower[11], // nominalU,
                // nominalP
                lightLevel[11], carbonLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            sharedItem.addElement(completId, element);
        }
        {
            subId = 5;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(Type.NONE, "50V Carbon Incandescent Light Bulb"),
                "incandescentcarbonlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, LVU, lightPower[13], // nominalU,
                // nominalP
                lightLevel[13], carbonLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            sharedItem.addElement(completId, element);
        }

        {
            subId = 16;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(Type.NONE, "Small 50V Economic Light Bulb"),
                "fluorescentlamp", LampDescriptor.Type.eco,
                LampSocketType.Douille, LVU, lightPower[12]
                * economicPowerFactor, // nominalU, nominalP
                lightLevel[12], economicLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            sharedItem.addElement(completId, element);
        }
        {
            subId = 17;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(Type.NONE, "50V Economic Light Bulb"),
                "fluorescentlamp", LampDescriptor.Type.eco,
                LampSocketType.Douille, LVU, lightPower[14]
                * economicPowerFactor, // nominalU, nominalP
                lightLevel[14], economicLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            sharedItem.addElement(completId, element);
        }
        {
            subId = 18;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(Type.NONE, "200V Economic Light Bulb"),
                "fluorescentlamp", LampDescriptor.Type.eco,
                LampSocketType.Douille, MVU, lightPower[14]
                * economicPowerFactor, // nominalU, nominalP
                lightLevel[14], economicLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            sharedItem.addElement(completId, element);
        }

        {
            subId = 32;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(Type.NONE, "50V Farming Lamp"),
                "farminglamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, LVU, 120, // nominalU, nominalP
                lightLevel[15], incandescentLampLife, 0.50 // nominalLight,
                // nominalLife
            );
            sharedItem.addElement(completId, element);
        }
        {
            subId = 36;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(Type.NONE, "200V Farming Lamp"),
                "farminglamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, MVU, 120, // nominalU, nominalP
                lightLevel[15], incandescentLampLife, 0.50 // nominalLight,
                // nominalLife
            );
            sharedItem.addElement(completId, element);
        }
        {
            subId = 37;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(Type.NONE, "50V LED Bulb"),
                "ledlamp", LampDescriptor.Type.LED,
                LampSocketType.Douille, LVU, lightPower[14] / 2, // nominalU, nominalP
                lightLevel[14], ledLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            sharedItem.addElement(completId, element);
        }
        {
            subId = 38;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(Type.NONE, "200V LED Bulb"),
                "ledlamp", LampDescriptor.Type.LED,
                LampSocketType.Douille, MVU, lightPower[14] / 2, // nominalU, nominalP
                lightLevel[14], ledLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
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

    public static void registerOre() {
        int id;
        String name;

        {
            id = 1;

            name = TR_NAME(Type.NONE, "Copper Ore");

            OreDescriptor desc = new OreDescriptor(name, id, // int itemIconId,
                // String
                // name,int
                // metadata,
                30 * (genCopper ? 1 : 0), 6, 10, 0, 80 // int spawnRate,int
                // spawnSizeMin,int
                // spawnSizeMax,int spawnHeightMin,int
                // spawnHeightMax
            );
            oreCopper = desc;
            oreItem.addDescriptor(id, desc);
            addToOre("oreCopper", desc.newItemStack());
        }

        {
            id = 4;

            name = TR_NAME(Type.NONE, "Lead Ore");

            OreDescriptor desc = new OreDescriptor(name, id, // int itemIconId,
                // String
                // name,int
                // metadata,
                8 * (genLead ? 1 : 0), 3, 9, 0, 24 // int spawnRate,int
                // spawnSizeMin,int
                // spawnSizeMax,int spawnHeightMin,int
                // spawnHeightMax
            );
            oreItem.addDescriptor(id, desc);
            addToOre("oreLead", desc.newItemStack());
        }
        {
            id = 5;

            name = TR_NAME(Type.NONE, "Tungsten Ore");

            OreDescriptor desc = new OreDescriptor(name, id, // int itemIconId,
                // String
                // name,int
                // metadata,
                6 * (genTungsten ? 1 : 0), 3, 9, 0, 32 // int spawnRate,int
                // spawnSizeMin,int
                // spawnSizeMax,int spawnHeightMin,int
                // spawnHeightMax
            );
            oreItem.addDescriptor(id, desc);
            addToOre(dictTungstenOre, desc.newItemStack());
        }
        {
            id = 6;

            name = TR_NAME(Type.NONE, "Cinnabar Ore");

            OreDescriptor desc = new OreDescriptor(name, id, // int itemIconId,
                // String
                // name,int
                // metadata,
                3 * (genCinnabar ? 1 : 0), 3, 9, 0, 32 // int spawnRate,int
                // spawnSizeMin,int
                // spawnSizeMax,int spawnHeightMin,int
                // spawnHeightMax
            );
            oreItem.addDescriptor(id, desc);
            addToOre("oreCinnabar", desc.newItemStack());
        }

    }

    public static void addToOre(String name, ItemStack ore) {
        OreDictionary.registerOre(name, ore);
        dictionnaryOreFromMod.put(name, ore);
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
            Data.addResource(element.newItemStack());
            addToOre("dustCopper", element.newItemStack());
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
            Data.addResource(element.newItemStack());
            addToOre("dustIron", element.newItemStack());
        }

        {
            id = 5;

            name = TR_NAME(Type.NONE, "Lead Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            addToOre("dustLead", element.newItemStack());
        }
        {
            id = 6;

            name = TR_NAME(Type.NONE, "Tungsten Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            addToOre(dictTungstenDust, element.newItemStack());
        }

        {
            id = 7;

            name = TR_NAME(Type.NONE, "Gold Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            addToOre("dustGold", element.newItemStack());
        }

        {
            id = 8;

            name = TR_NAME(Type.NONE, "Coal Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            addToOre("dustCoal", element.newItemStack());
        }
        {
            id = 9;

            name = TR_NAME(Type.NONE, "Alloy Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            addToOre("dustAlloy", element.newItemStack());
        }

        {
            id = 10;

            name = TR_NAME(Type.NONE, "Cinnabar Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            addToOre("dustCinnabar", element.newItemStack());
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
            Data.addResource(element.newItemStack());
            addToOre("ingotCopper", element.newItemStack());
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
            Data.addResource(element.newItemStack());
            addToOre("ingotLead", element.newItemStack());

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
            Data.addResource(element.newItemStack());
            addToOre(dictTungstenIngot, element.newItemStack());
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

            Data.addResource(element.newItemStack());
            addToOre("ingotFerrite", element.newItemStack());
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

            Data.addResource(element.newItemStack());
            addToOre("ingotAlloy", element.newItemStack());
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

            Data.addResource(element.newItemStack());
            addToOre("quicksilver", element.newItemStack());
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

            Data.addResource(element.newItemStack());

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
            Data.addResource(element.newItemStack());

        }

    }

    public static void registerArmor() {
        ItemStack stack;
        String name;

        {
            name = TR_NAME(Type.ITEM, "Copper Helmet");
            helmetCopper = (ItemArmor) (new genericArmorItem(ArmorMaterial.IRON, 2, ArmourType.Helmet, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setTranslationKey(name).setTextureName("eln:copper_helmet").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(helmetCopper, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(helmetCopper));
        }
        {
            name = TR_NAME(Type.ITEM, "Copper Chestplate");
            plateCopper = (ItemArmor) (new genericArmorItem(ArmorMaterial.IRON, 2, ArmourType.Chestplate, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setTranslationKey(name).setTextureName("eln:copper_chestplate").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(plateCopper, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(plateCopper));
        }
        {
            name = TR_NAME(Type.ITEM, "Copper Leggings");
            legsCopper = (ItemArmor) (new genericArmorItem(ArmorMaterial.IRON, 2, ArmourType.Leggings, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setTranslationKey(name).setTextureName("eln:copper_leggings").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(legsCopper, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(legsCopper));
        }
        {
            name = TR_NAME(Type.ITEM, "Copper Boots");
            bootsCopper = (ItemArmor) (new genericArmorItem(ArmorMaterial.IRON, 2, ArmourType.Boots, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setTranslationKey(name).setTextureName("eln:copper_boots").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(bootsCopper, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(bootsCopper));
        }

        int armorPoint;
        String t1, t2;
        t1 = "eln:textures/armor/ecoal_layer_1.png";
        t2 = "eln:textures/armor/ecoal_layer_2.png";
        double energyPerDamage = 500;
        int armor, armorMarge;
        ArmorMaterial eCoalMaterial = net.minecraftforge.common.util.EnumHelper.addArmorMaterial("ECoal", 10, new int[]{2, 6, 5, 2}, 9);
        {
            name = TR_NAME(Type.ITEM, "E-Coal Helmet");
            armor = 2;
            armorMarge = 1;
            helmetECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, ArmourType.Helmet, t1, t2,
                (armor + armorMarge) * energyPerDamage, 250.0,// double
                // energyStorage,double
                // chargePower
                armor / 20.0, armor * energyPerDamage,// double
                // ratioMax,double
                // ratioMaxEnergy,
                energyPerDamage// double energyPerDamage
            )).setTranslationKey(name).setTextureName("eln:ecoal_helmet").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(helmetECoal, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(helmetECoal));
        }
        {
            name = TR_NAME(Type.ITEM, "E-Coal Chestplate");
            armor = 6;
            armorMarge = 2;
            plateECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, ArmourType.Chestplate, t1, t2,
                (armor + armorMarge) * energyPerDamage, 250.0,// double
                // energyStorage,double
                // chargePower
                armor / 20.0, armor * energyPerDamage,// double
                // ratioMax,double
                // ratioMaxEnergy,
                energyPerDamage// double energyPerDamage
            )).setTranslationKey(name).setTextureName("eln:ecoal_chestplate").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(plateECoal, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(plateECoal));
        }
        {
            name = TR_NAME(Type.ITEM, "E-Coal Leggings");
            armor = 5;
            armorMarge = 2;
            legsECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, ArmourType.Leggings, t1, t2,
                (armor + armorMarge) * energyPerDamage, 250.0,// double
                // energyStorage,double
                // chargePower
                armor / 20.0, armor * energyPerDamage,// double
                // ratioMax,double
                // ratioMaxEnergy,
                energyPerDamage// double energyPerDamage
            )).setTranslationKey(name).setTextureName("eln:ecoal_leggings").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(legsECoal, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(legsECoal));
        }
        {
            name = TR_NAME(Type.ITEM, "E-Coal Boots");
            armor = 2;
            armorMarge = 1;
            bootsECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, ArmourType.Boots, t1, t2,
                (armor + armorMarge) * energyPerDamage, 250.0,// double
                // energyStorage,double
                // chargePower
                armor / 20.0, armor * energyPerDamage,// double
                // ratioMax,double
                // ratioMaxEnergy,
                energyPerDamage// double energyPerDamage
            )).setTranslationKey(name).setTextureName("eln:ecoal_boots").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(bootsECoal, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(bootsECoal));
        }
    }

    public static void registerTool() {
        ItemStack stack;
        String name;
        {
            name = TR_NAME(Type.ITEM, "Copper Sword");
            swordCopper = (new ItemSword(ToolMaterial.IRON)).setTranslationKey(name).setTextureName("eln:copper_sword").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(swordCopper, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(swordCopper));
        }
        {
            name = TR_NAME(Type.ITEM, "Copper Hoe");
            hoeCopper = (new ItemHoe(ToolMaterial.IRON)).setTranslationKey(name).setTextureName("eln:copper_hoe").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(hoeCopper, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(hoeCopper));
        }
        {
            name = TR_NAME(Type.ITEM, "Copper Shovel");
            shovelCopper = (new ItemSpade(ToolMaterial.IRON)).setTranslationKey(name).setTextureName("eln:copper_shovel").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(shovelCopper, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(shovelCopper));
        }
        {
            name = TR_NAME(Type.ITEM, "Copper Pickaxe");
            pickaxeCopper = new ItemPickaxeEln(ToolMaterial.IRON).setTranslationKey(name).setTextureName("eln:copper_pickaxe").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(pickaxeCopper, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(pickaxeCopper));
        }
        {
            name = TR_NAME(Type.ITEM, "Copper Axe");
            axeCopper = new ItemAxeEln(ToolMaterial.IRON).setTranslationKey(name).setTextureName("eln:copper_axe").setCreativeTab(creativeTab);
            GameRegistryCompat.registerItem(axeCopper, "Eln." + name);
            GameRegistryCompat.registerCustomItemStack(name, new ItemStack(axeCopper));
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

    public static void registerWindTurbine(int id) {
        int subId, completId;
        String name;

        FunctionTable PfW = new FunctionTable(
            new double[]{0.0, 0.1, 0.3, 0.5, 0.8, 1.0, 1.1, 1.15, 1.2},
            8.0 / 5.0);
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Wind Turbine");

            WindTurbineDescriptor desc = new WindTurbineDescriptor(
                name, obj.getObj("WindTurbineMini"), // name,Obj3D obj,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable,
                PfW,// PfW
                160 * windTurbinePowerFactor, 10,// double nominalPower,double nominalWind,
                LVU * 1.18, 22,// double maxVoltage, double maxWind,
                3,// int offY,
                7, 2, 2,// int rayX,int rayY,int rayZ,
                2, 0.07,// int blockMalusMinCount,double blockMalus
                "eln:WINDTURBINE_BIG_SF", 1f // Use the wind turbine sound and play at normal volume (1 => 100%)
            );

            GhostGroup g = new GhostGroup();
            g.addElement(0, 1, 0);
            g.addElement(0, 2, -1);
            g.addElement(0, 2, 1);
            g.addElement(0, 3, -1);
            g.addElement(0, 3, 1);
            g.addRectangle(0, 0, 1, 3, 0, 0);
            desc.setGhostGroup(g);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 16;
            name = TR_NAME(Type.NONE, "Water Turbine");

            Coordonate waterCoord = new Coordonate(1, -1, 0, 0);

            WaterTurbineDescriptor desc = new WaterTurbineDescriptor(
                name, obj.getObj("SmallWaterWheel"), // name,Obj3D obj,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor
                30 * waterTurbinePowerFactor,
                LVU * 1.18,
                waterCoord,
                "eln:water_turbine", 1f
            );

            GhostGroup g = new GhostGroup();

            g.addRectangle(1, 1, 0, 1, -1, 1);
            desc.setGhostGroup(g);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerFuelGenerator(int id) {
        int subId;
        {
            subId = 1;
            FuelGeneratorDescriptor descriptor =
                new FuelGeneratorDescriptor(TR_NAME(Type.NONE, "50V Fuel Generator"), obj.getObj("FuelGenerator50V"),
                    lowVoltageCableDescriptor, fuelGeneratorPowerFactor * 300, LVU * 1.05, fuelGeneratorTankCapacity);
            transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 2;
            FuelGeneratorDescriptor descriptor =
                new FuelGeneratorDescriptor(TR_NAME(Type.NONE, "200V Fuel Generator"), obj.getObj("FuelGenerator200V"),
                    meduimVoltageCableDescriptor, fuelGeneratorPowerFactor * 1500, MVU * 1.05,
                    fuelGeneratorTankCapacity);
            transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
    }

    public static void registerThermalDissipatorPassiveAndActive(int id) {
        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Small Passive Thermal Dissipator");

            ThermalDissipatorPassiveDescriptor desc = new ThermalDissipatorPassiveDescriptor(
                name,
                obj.getObj("passivethermaldissipatora"),
                200, -100,// double warmLimit,double coolLimit,
                250, 30,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop

            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 32;
            name = TR_NAME(Type.NONE, "Small Active Thermal Dissipator");

            ThermalDissipatorActiveDescriptor desc = new ThermalDissipatorActiveDescriptor(
                name,
                obj.getObj("activethermaldissipatora"),
                LVU, 50,// double nominalElectricalU,double
                // electricalNominalP,
                800,// double nominalElectricalCoolingPower,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cableDescriptor,
                130, -100,// double warmLimit,double coolLimit,
                200, 30,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop

            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 34;
            name = TR_NAME(Type.NONE, "200V Active Thermal Dissipator");

            ThermalDissipatorActiveDescriptor desc = new ThermalDissipatorActiveDescriptor(
                name,
                obj.getObj("200vactivethermaldissipatora"),
                MVU, 60,// double nominalElectricalU,double
                // electricalNominalP,
                1200,// double nominalElectricalCoolingPower,
                meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cableDescriptor,
                130, -100,// double warmLimit,double coolLimit,
                200, 30,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop

            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerTransparentNodeMisc(int id) {
        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Experimental Transporter");

            Coordonate[] powerLoad = new Coordonate[2];
            powerLoad[0] = new Coordonate(-1, 0, 1, 0);
            powerLoad[1] = new Coordonate(-1, 0, -1, 0);

            GhostGroup doorOpen = new GhostGroup();
            doorOpen.addRectangle(-4, -3, 2, 2, 0, 0);

            GhostGroup doorClose = new GhostGroup();
            doorClose.addRectangle(-2, -2, 0, 1, 0, 0);

            TeleporterDescriptor desc = new TeleporterDescriptor(
                name, obj.getObj("Transporter"),
                highVoltageCableDescriptor,
                new Coordonate(-1, 0, 0, 0), new Coordonate(-1, 1, 0, 0),
                2,// int areaH
                powerLoad,
                doorOpen, doorClose

            );
            desc.setChargeSound("eln:transporter", 0.5f);
            GhostGroup g = new GhostGroup();
            g.addRectangle(-2, 0, 0, 1, -1, -1);
            g.addRectangle(-2, 0, 0, 1, 1, 1);
            g.addRectangle(-4, -1, 2, 2, 0, 0);
            g.addElement(0, 1, 0);
            //g.addElement(0, 2, 0);
            g.addElement(-1, 0, 0, ghostBlock, ghostBlock.tFloor);
		/*	g.addElement(1, 0, 0,ghostBlock,ghostBlock.tLadder);
			g.addElement(1, 1, 0,ghostBlock,ghostBlock.tLadder);
			g.addElement(1, 2, 0,ghostBlock,ghostBlock.tLadder);*/
            g.addRectangle(-3, -3, 0, 1, -1, -1);
            g.addRectangle(-3, -3, 0, 1, 1, 1);
            // g.addElement(-4, 0, -1);
            // g.addElement(-4, 0, 1);

            desc.setGhostGroup(g);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

		/*if (Other.ccLoaded && ComputerProbeEnable) {
			subId = 4;
			name = "ComputerCraft Probe";

			ComputerCraftIoDescriptor desc = new ComputerCraftIoDescriptor(
					name,
					obj.getObj("passivethermaldissipatora")

					);

			transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
		}*/

    }

    public static void registerTurret(int id) {
        {
            int subId = 0;
            String name = TR_NAME(Type.NONE, "800V Defence Turret");

            TurretDescriptor desc = new TurretDescriptor(name, "Turret");

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalAntenna(int id) {
        int subId, completId;
        String name;
        {

            subId = 0;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(Type.NONE, "Low Power Transmitter Antenna");
            double P = 250;
            desc = new ElectricalAntennaTxDescriptor(name,
                obj.getObj("lowpowertransmitterantenna"), 200,// int
                // rangeMax,
                0.9, 0.7,// double electricalPowerRatioEffStart,double
                // electricalPowerRatioEffEnd,
                LVU, P,// double electricalNominalVoltage,double
                // electricalNominalPower,
                LVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                lowVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {

            subId = 1;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(Type.NONE, "Low Power Receiver Antenna");
            double P = 250;
            desc = new ElectricalAntennaRxDescriptor(name,
                obj.getObj("lowpowerreceiverantenna"), LVU, P,// double
                // electricalNominalVoltage,double
                // electricalNominalPower,
                LVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                lowVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {

            subId = 2;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(Type.NONE, "Medium Power Transmitter Antenna");
            double P = 1000;
            desc = new ElectricalAntennaTxDescriptor(name,
                obj.getObj("lowpowertransmitterantenna"), 250,// int
                // rangeMax,
                0.9, 0.75,// double electricalPowerRatioEffStart,double
                // electricalPowerRatioEffEnd,
                MVU, P,// double electricalNominalVoltage,double
                // electricalNominalPower,
                MVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                meduimVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {

            subId = 3;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(Type.NONE, "Medium Power Receiver Antenna");
            double P = 1000;
            desc = new ElectricalAntennaRxDescriptor(name,
                obj.getObj("lowpowerreceiverantenna"), MVU, P,// double
                // electricalNominalVoltage,double
                // electricalNominalPower,
                MVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                meduimVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {

            subId = 4;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(Type.NONE, "High Power Transmitter Antenna");
            double P = 2000;
            desc = new ElectricalAntennaTxDescriptor(name,
                obj.getObj("lowpowertransmitterantenna"), 300,// int
                // rangeMax,
                0.95, 0.8,// double electricalPowerRatioEffStart,double
                // electricalPowerRatioEffEnd,
                HVU, P,// double electricalNominalVoltage,double
                // electricalNominalPower,
                HVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                highVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {

            subId = 5;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(Type.NONE, "High Power Receiver Antenna");
            double P = 2000;
            desc = new ElectricalAntennaRxDescriptor(name,
                obj.getObj("lowpowerreceiverantenna"), HVU, P,// double
                // electricalNominalVoltage,double
                // electricalNominalPower,
                HVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                highVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerMeter(int id) {
        int subId, completId;

        GenericItemUsingDamageDescriptor element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new GenericItemUsingDamageDescriptor(TR_NAME(Type.NONE, "MultiMeter"));
            sharedItem.addElement(completId, element);
            multiMeterElement = element;
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            element = new GenericItemUsingDamageDescriptor(TR_NAME(Type.NONE, "Thermometer"));
            sharedItem.addElement(completId, element);
            thermometerElement = element;
        }
        {
            subId = 2;
            completId = subId + (id << 6);
            element = new GenericItemUsingDamageDescriptor(TR_NAME(Type.NONE, "AllMeter"));
            sharedItem.addElement(completId, element);
            allMeterElement = element;
        }
        {
            subId = 8;
            completId = subId + (id << 6);
            element = new WirelessSignalAnalyserItemDescriptor(TR_NAME(Type.NONE, "Wireless Analyser"));
            sharedItem.addElement(completId, element);

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
            addToOre("materialResin", descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 1;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Rubber");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            sharedItem.addElement(completId, descriptor);
            addToOre("itemRubber", descriptor.newItemStack());
        }
    }

    public static void registerTreeResinCollector(int id) {
        int subId, completId;
        String name;

        TreeResinCollectorDescriptor descriptor;
        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Tree Resin Collector");

            descriptor = new TreeResinCollectorDescriptor(name, obj.getObj("treeresincolector"));
            sixNodeItem.addDescriptor(completId, descriptor);
        }
    }

    public static void registerBatteryCharger(int id) {
        int subId, completId;
        String name;

        BatteryChargerDescriptor descriptor;
        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Weak 50V Battery Charger");

            descriptor = new BatteryChargerDescriptor(
                name, obj.getObj("batterychargera"),
                lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable,
                LVU, 200// double nominalVoltage,double nominalPower
            );
            sixNodeItem.addDescriptor(completId, descriptor);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "50V Battery Charger");

            descriptor = new BatteryChargerDescriptor(
                name, obj.getObj("batterychargera"),
                lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable,
                LVU, 400// double nominalVoltage,double nominalPower
            );
            sixNodeItem.addDescriptor(completId, descriptor);
        }
        {
            subId = 4;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "200V Battery Charger");

            descriptor = new BatteryChargerDescriptor(
                name, obj.getObj("batterychargera"),
                meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable,
                MVU, 1000// double nominalVoltage,double nominalPower
            );
            sixNodeItem.addDescriptor(completId, descriptor);
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

    public static void registerAutoMiner(int id) {
        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Auto Miner");

            Coordonate[] powerLoad = new Coordonate[2];
            powerLoad[0] = new Coordonate(-2, -1, 1, 0);
            powerLoad[1] = new Coordonate(-2, -1, -1, 0);

            Coordonate lightCoord = new Coordonate(-3, 0, 0, 0);

            Coordonate miningCoord = new Coordonate(-1, 0, 1, 0);

            AutoMinerDescriptor desc = new AutoMinerDescriptor(name,
                obj.getObj("AutoMiner"),
                powerLoad, lightCoord, miningCoord,
                2, 1, 0,
                highVoltageCableDescriptor,
                1, 50// double pipeRemoveTime,double pipeRemoveEnergy
            );

            GhostGroup ghostGroup = new GhostGroup();

            ghostGroup.addRectangle(-2, -1, -1, 0, -1, 1);
            ghostGroup.addRectangle(1, 1, -1, 0, 1, 1);
            ghostGroup.addRectangle(1, 1, -1, 0, -1, -1);
            ghostGroup.addElement(1, 0, 0);
            ghostGroup.addElement(0, 0, 1);
            ghostGroup.addElement(0, 1, 0);
            ghostGroup.addElement(0, 0, -1);
            ghostGroup.removeElement(-1, -1, 0);

            desc.setGhostGroup(ghostGroup);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
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
            Data.addResource(copperCableDescriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 1;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Iron Cable");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 2;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "Tungsten Cable");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
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

        ItemStack emptyStack = findItemStack("White Brush");
        whiteDesc.setLife(emptyStack, 0);

        for (int idx = 0; idx < 16; idx++) {

            addShapelessRecipe(emptyStack.copy(),
                new ItemStack(Blocks.WOOL, 1, idx),
                new ItemStack(Items.IRON_INGOT));
        }

        for (int idx = 0; idx < 16; idx++) {
            name = subNames[idx];
            addShapelessRecipe(findItemStack(name, 1),
                new ItemStack(Items.DYE, 1, idx),
                emptyStack.copy());
        }

    }

    public static void registerElectricalTool(int id) {
        int subId, completId;
        ItemStack stack;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Small Flashlight");

            ElectricalLampItem desc = new ElectricalLampItem(
                name,
                10, 6, 20, 12, 8, 50,// int light,int range,
                6000, 100// , energyStorage, discharge, charge
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Improved Flashlight");

            ElectricalLampItem desc = new ElectricalLampItem(
                name,
                15, 8, 20, 15, 12, 50,// int light,int range,
                24000, 400// , energyStorage, discharge, charge
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        {
            subId = 8;
            name = TR_NAME(Type.NONE, "Portable Electrical Mining Drill");

            ElectricalPickaxe desc = new ElectricalPickaxe(
                name,
                8, 3,// float strengthOn,float strengthOff,
                120000, 200, 2400// double energyStorage,double
                // energyPerBlock,double chargePower
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        {
            subId = 12;
            name = TR_NAME(Type.NONE, "Portable Electrical Axe");

            ElectricalAxe desc = new ElectricalAxe(
                name,
                8, 3,// float strengthOn,float strengthOff,
                40000, 200, 800// double energyStorage,double
                // energyPerBlock,double chargePower
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

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
    }

    public static void registerFuelBurnerItem(int id) {
        sharedItemStackOne.addElement(0 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(Type.NONE, "Small Fuel Burner"), 5000 * fuelHeatFurnacePowerFactor, 2, 1.6f));
        sharedItemStackOne.addElement(1 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(Type.NONE, "Medium Fuel Burner"), 10000 * fuelHeatFurnacePowerFactor, 1, 1.4f));
        sharedItemStackOne.addElement(2 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(Type.NONE, "Big Fuel Burner"), 25000 * fuelHeatFurnacePowerFactor, 0, 1f));
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
            Data.addResource(desc.newItemStack());
            OreDictionary.registerOre(dictCheapChip, desc.newItemStack());
        }
        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Advanced Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            OreDictionary.registerOre(dictAdvancedChip, desc.newItemStack());
        }
        {
            subId = 2;
            name = TR_NAME(Type.NONE, "Machine Block");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            addToOre("casingMachine", desc.newItemStack());
        }
        {
            subId = 3;
            name = TR_NAME(Type.NONE, "Electrical Probe Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
        }
        {
            subId = 4;
            name = TR_NAME(Type.NONE, "Thermal Probe Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
        }

        {
            subId = 6;
            name = TR_NAME(Type.NONE, "Copper Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            addToOre("plateCopper", desc.newItemStack());
        }
        {
            subId = 7;
            name = TR_NAME(Type.NONE, "Iron Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            addToOre("plateIron", desc.newItemStack());
        }
        {
            subId = 8;
            name = TR_NAME(Type.NONE, "Gold Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            addToOre("plateGold", desc.newItemStack());
        }
        {
            subId = 9;
            name = TR_NAME(Type.NONE, "Lead Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            addToOre("plateLead", desc.newItemStack());
        }
        {
            subId = 10;
            name = TR_NAME(Type.NONE, "Silicon Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            addToOre("plateSilicon", desc.newItemStack());
        }

        {
            subId = 11;
            name = TR_NAME(Type.NONE, "Alloy Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            addToOre("plateAlloy", desc.newItemStack());
        }
        {
            subId = 12;
            name = TR_NAME(Type.NONE, "Coal Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            addToOre("plateCoal", desc.newItemStack());
        }

        {
            subId = 16;
            name = TR_NAME(Type.NONE, "Silicon Dust");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            addToOre("dustSilicon", desc.newItemStack());
        }
        {
            subId = 17;
            name = TR_NAME(Type.NONE, "Silicon Ingot");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            addToOre("ingotSilicon", desc.newItemStack());
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
            Data.addResource(desc.newItemStack());
            addToOre("casingMachineAdvanced", desc.newItemStack());
        }
        {
            subId = 28;
            name = TR_NAME(Type.NONE, "Basic Magnet");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
        }
        {
            subId = 29;
            name = TR_NAME(Type.NONE, "Advanced Magnet");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
        }
        {
            subId = 32;
            name = TR_NAME(Type.NONE, "Data Logger Print");
            DataLogsPrintDescriptor desc = new DataLogsPrintDescriptor(name);
            dataLogsPrintDescriptor = desc;
            desc.setDefaultIcon("empty-texture");
            sharedItem.addWithoutRegistry(subId + (id << 6), desc);
        }

        {
            subId = 33;
            name = TR_NAME(Type.NONE, "Signal Antenna");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
        }

        {
            subId = 40;
            name = TR_NAME(Type.NONE, "Player Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, EntityPlayer.class, 0f, 1f, 0f);
            sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 41;
            name = TR_NAME(Type.NONE, "Monster Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, IMob.class, 1f, 0f, 0f);
            sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 42;
            name = TR_NAME(Type.NONE, "Animal Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, EntityAnimal.class, .3f, .3f, 1f);
            sharedItem.addElement(subId + (id << 6), desc);
        }

        {
            subId = 48;
            name = TR_NAME(Type.NONE, "Wrench");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, TR("Electrical age wrench,\nCan be used to turn\nsmall wall blocks").split("\n"));
            sharedItem.addElement(subId + (id << 6), desc);

            wrenchItemStack = desc.newItemStack();
        }

        {
            subId = 52;
            name = TR_NAME(Type.NONE, "Dielectric");
            DielectricItem desc = new DielectricItem(name, LVU);
            sharedItem.addElement(subId + (id << 6), desc);
        }

        sharedItem.addElement(53 + (id << 6), new CaseItemDescriptor(TR_NAME(Type.NONE, "Casing")));
    }

    public static void registerReplicator() {
        int redColor = (255 << 16);
        int orangeColor = (255 << 16) + (200 << 8);

        if (replicatorRegistrationId == -1)
            replicatorRegistrationId = EntityRegistry.findGlobalUniqueEntityId();
        Utils.println("Replicator registred at" + replicatorRegistrationId);
        // Register mob
        EntityRegistry.registerGlobalEntityID(ReplicatorEntity.class, TR_NAME(Type.ENTITY, "EAReplicator"), replicatorRegistrationId, redColor, orangeColor);

        ReplicatorEntity.dropList.add(findItemStack("Iron Dust", 1));
        ReplicatorEntity.dropList.add(findItemStack("Copper Dust", 1));
        ReplicatorEntity.dropList.add(findItemStack("Gold Dust", 1));
        ReplicatorEntity.dropList.add(new ItemStack(Items.REDSTONE));
        ReplicatorEntity.dropList.add(new ItemStack(Items.GLOWSTONE_DUST));
        // Add mob spawn
        // EntityRegistry.addSpawn(ReplicatorEntity.class, 1, 1, 2, EnumCreatureType.monster, BiomeGenBase.PLAINS);

    }

    // Registers WIP items.
    public static void registerWipItems() {
    }

    public static void regenOreScannerFactors() {
        OreColorMapping.INSTANCE.updateColorMapping();

        oreScannerConfig.clear();

        if (addOtherModOreToXRay) {
            for (String name : OreDictionary.getOreNames()) {
                if (name == null)
                    continue;
                // Utils.println(name + " " +
                // OreDictionary.getOreID(name));
                if (name.startsWith("ore")) {
                    for (ItemStack stack : OreDictionary.getOres(name)) {
                        int id = Utils.getItemId(stack) + 4096 * stack.getItem().getMetadata(stack.getMetadata());
                        // Utils.println(OreDictionary.getOreID(name));
                        boolean find = false;
                        for (OreScannerConfigElement c : oreScannerConfig) {
                            if (c.getBlockKey() == id) {
                                find = true;
                                break;
                            }
                        }

                        if (!find) {
                            Utils.println(id + " added to xRay (other mod)");
                            oreScannerConfig.add(new OreScannerConfigElement(id, 0.15f));
                        }
                    }
                }
            }
        }

        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.COAL_ORE), 5 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.IRON_ORE), 15 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.GOLD_ORE), 40 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.LAPIS_ORE), 40 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.REDSTONE_ORE), 40 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.DIAMOND_ORE), 100 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.EMERALD_ORE), 40 / 100f));

        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(oreBlock) + (1 << 12), 10 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(oreBlock) + (4 << 12), 20 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(oreBlock) + (5 << 12), 20 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(oreBlock) + (6 << 12), 20 / 100f));
    }
}
