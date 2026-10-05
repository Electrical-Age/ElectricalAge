package mods.eln;

import net.minecraft.util.ResourceLocation;

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
import static mods.eln.registry.ElnDeviceRegistry.*;
import static mods.eln.registry.ElnRecipes.*;

@SuppressWarnings({"SameParameterValue", "PointlessArithmeticExpression"})
@Mod(modid = Eln.MODID, name = Eln.NAME, version = "@VERSION@")
public class Eln {
    // Mod information (override from 'mcmod.info' file)
    public final static String MODID = "eln"; // 1.12: lowercase (registry names eln:snake_case)
    public final static String NAME = "Electrical Age";
    public final static String MODDESC = "Electricity in your base !";
    public final static String URL = "https://electrical-age.net";
    public final static String UPDATE_URL = "https://github.com/Electrical-Age/ElectricalAge/releases";
    public final static String SRC_URL = "https://github.com/Electrical-Age";
    public final static String[] AUTHORS = {"Dolu1990", "lambdaShade", "cm0x4D", "metc", "Baughn"};

    public static final String channelName = "miaouMod";
    public static final double solarPanelBasePower = 65.0;
    public static ArrayList<IConfigSharing> configShared = new ArrayList<IConfigSharing>();

    // public static final double networkSerializeValueFactor = 100.0;
    // public static final byte packetNodeSerialized24bitPosition = 11;
    // public static final byte packetNodeSerialized48bitPosition = 12;
    // public static final byte packetNodeRefreshRequest = 13;

    public static final byte packetPlayerKey = 14;
    public static final byte packetNodeSingleSerialized = 15;
    public static final byte packetPublishForNode = 16;
    public static final byte packetOpenLocalGui = 17;
    public static final byte packetForClientNode = 18;
    public static final byte packetPlaySound = 19;
    public static final byte packetDestroyUuid = 20;
    public static final byte packetClientToServerConnection = 21;
    public static final byte packetServerToClientInfo = 22;

    public static PacketHandler packetHandler;
    static NodeServer nodeServer;
    public static LiveDataManager clientLiveDataManager;
    public static ClientKeyHandler clientKeyHandler;
    public static SaveConfig saveConfig;
    public static GhostManager ghostManager;
    public static GhostManagerNbt ghostManagerNbt;
    public static NodeManager nodeManager;
    public static PlayerManager playerManager;
    public static NodeManagerNbt nodeManagerNbt;
    public static Simulator simulator = null;
    public static DelayedTaskManager delayedTask;
    public static ItemEnergyInventoryProcess itemEnergyInventoryProcess;
    public static CreativeTabs creativeTab;

    public static Item swordCopper, hoeCopper, shovelCopper, pickaxeCopper, axeCopper;

    public static ItemArmor helmetCopper, plateCopper, legsCopper, bootsCopper;
    public static ItemArmor helmetECoal, plateECoal, legsECoal, bootsECoal;

    public static SharedItem sharedItem;
    public static SharedItem sharedItemStackOne;
    public static ItemStack wrenchItemStack;
    public static SixNodeBlock sixNodeBlock;
    public static TransparentNodeBlock transparentNodeBlock;
    public static OreBlock oreBlock;
    public static GhostBlock ghostBlock;
    public static LightBlock lightBlock;

    public static SixNodeItem sixNodeItem;
    public static TransparentNodeItem transparentNodeItem;
    public static OreItem oreItem;

    // The instance of your mod that Forge uses.
    @Instance(MODID)
    public static Eln instance;

    // Says where the client and server 'proxy' code is loaded.
    @SidedProxy(clientSide = "mods.eln.client.ClientProxy", serverSide = "mods.eln.CommonProxy")
    public static CommonProxy proxy;

    public static double electricalFrequency, thermalFrequency;
    public static int electricalInterSystemOverSampling;

    public static CopperCableDescriptor copperCableDescriptor;

    public static ElectricalCableDescriptor veryHighVoltageCableDescriptor;
    public static ElectricalCableDescriptor highVoltageCableDescriptor;
    public static ElectricalCableDescriptor signalCableDescriptor;
    public static ElectricalCableDescriptor lowVoltageCableDescriptor;
    public static ElectricalCableDescriptor batteryCableDescriptor;
    public static ElectricalCableDescriptor meduimVoltageCableDescriptor;

    public static OreRegenerate oreRegenerate;

    public static final Obj3DFolder obj = new Obj3DFolder();

    public static boolean oredictTungsten, oredictChips;
    public static boolean genCopper, genLead, genTungsten, genCinnabar;
    public static String dictTungstenOre, dictTungstenDust, dictTungstenIngot;
    public static String dictCheapChip, dictAdvancedChip;
    public static final ArrayList<OreScannerConfigElement> oreScannerConfig = new ArrayList<OreScannerConfigElement>();
    public static boolean modbusEnable = false; // 1.12 port: Modbus TCP server dropped (rule 8); RTU devices stay off

    public static float xRayScannerRange;
    public static boolean addOtherModOreToXRay;

    public static boolean replicatorPop;

    public static boolean xRayScannerCanBeCrafted = true;
    public static boolean forceOreRegen;
    public static boolean explosionEnable;

    public static boolean debugEnabled = false;  // Read from configuration file. Default is `false`.

    public static double heatTurbinePowerFactor = 1;
    public static double solarPanelPowerFactor = 1;
    public static double windTurbinePowerFactor = 1;
    public static double waterTurbinePowerFactor = 1;
    public static double fuelGeneratorPowerFactor = 1;
    public static double fuelHeatFurnacePowerFactor = 1;
    public static int autominerRange = 10;

    public static boolean killMonstersAroundLamps;
    public static int killMonstersAroundLampsRange;

    public static double stdBatteryHalfLife = 2 * Utils.minecraftDay;
    public static double batteryCapacityFactor = 1.;

    public static boolean wailaEasyMode = false;

    public static double fuelHeatValueFactor = 0.0000675;
    public static int plateConversionRatio;

    public static boolean noSymbols = false;
    public static boolean noVoltageBackground = false;

    public static double maxSoundDistance = 16;
    public static double cablePowerFactor;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        // 1.12 port: the "electrical-age" SimpleNetworkWrapper only carried Waila and achievement packets (dropped).
        MinecraftForge.EVENT_BUS.register(GameRegistryCompat.EVENTS); // RegistryEvent.Register<Block/Item>

        ModContainer container = FMLCommonHandler.instance().findContainerFor(this);
        // LanguageRegistry.instance().loadLanguagesFor(container, Side.CLIENT);

        // Update ModInfo by code
        ModMetadata meta = event.getModMetadata();
        meta.modId = MODID;
        meta.version = Version.getVersionName();
        meta.name = NAME;
        meta.description = tr("mod.meta.desc");
        meta.url = URL;
        meta.updateUrl = UPDATE_URL;
        meta.authorList = Arrays.asList(AUTHORS);
        meta.autogenerated = false; // Force to update from code

        Utils.println(Version.print());

        Side side = FMLCommonHandler.instance().getEffectiveSide();
        if (side == Side.CLIENT)
            MinecraftForge.EVENT_BUS.register(new SoundLoader());

        Configuration config = new Configuration(
            event.getSuggestedConfigurationFile());
        config.load();


        //Hacks for correct long date typing failures in config file
        //WARNING/BUG: "renameProperty" changes the type to String! However read functions don't seem to care attention to it, so it's OK... for the moment.
        if (config.hasKey("lamp", "incondescentLifeInHours"))
            config.renameProperty("lamp", "incondescentLifeInHours", "incandescentLifeInHours");
        if (config.hasKey("mapgenerate", "plumb"))
            config.renameProperty("mapgenerate", "plumb", "lead");
        if (config.hasKey("mapgenerate", "cooper"))
            config.renameProperty("mapgenerate", "cooper", "copper");
        if (config.hasKey("simulation", "electricalFrequancy"))
            config.renameProperty("simulation", "electricalFrequancy", "electricalFrequency");
        if (config.hasKey("simulation", "thermalFrequancy"))
            config.renameProperty("simulation", "thermalFrequancy", "thermalFrequency");


        debugEnabled = config.get("debug", "enable", false).getBoolean(false);

        explosionEnable = config.get("gameplay", "explosion", true).getBoolean(true);

        //explosionEnable = false;
        // 1.12 port: version check and analytics dropped (rule 8)

        heatTurbinePowerFactor = config.get("balancing", "heatTurbinePowerFactor", 1).getDouble(1);
        solarPanelPowerFactor = config.get("balancing", "solarPanelPowerFactor", 1).getDouble(1);
        windTurbinePowerFactor = config.get("balancing", "windTurbinePowerFactor", 1).getDouble(1);
        waterTurbinePowerFactor = config.get("balancing", "waterTurbinePowerFactor", 1).getDouble(1);
        fuelGeneratorPowerFactor = config.get("balancing", "fuelGeneratorPowerFactor", 1).getDouble(1);
        fuelHeatFurnacePowerFactor = config.get("balancing", "fuelHeatFurnacePowerFactor", 1.0).getDouble();
        autominerRange = config.get("balancing", "autominerRange", 10, "Maximum horizontal distance from autominer that will be mined").getInt(10);

        Other.ElnToIc2ConversionRatio = config.get("balancing", "ElnToIndustrialCraftConversionRatio", 1.0 / 3.0).getDouble(1.0 / 3.0);
        Other.ElnToOcConversionRatio = config.get("balancing", "ElnToOpenComputerConversionRatio", 1.0 / 3.0 / 2.5).getDouble(1.0 / 3.0 / 2.5);
        Other.ElnToTeConversionRatio = config.get("balancing", "ElnToThermalExpansionConversionRatio", 1.0 / 3.0 * 4).getDouble(1.0 / 3.0 * 4);
        //	Other.ElnToBuildcraftConversionRatio = config.get("balancing", "ElnToBuildcraftConversionRatio", 1.0 / 3.0 / 5 * 2).getDouble(1.0 / 3.0 / 5 * 2);
        plateConversionRatio = config.get("balancing", "platesPerIngot", 1).getInt(1);

        stdBatteryHalfLife = config.get("battery", "batteryHalfLife", 2, "How many days it takes for a battery to decay half way").getDouble(2) * Utils.minecraftDay;
        batteryCapacityFactor = config.get("balancing", "batteryCapacityFactor", 1.).getDouble(1.);

        ComputerProbeEnable = config.get("compatibility", "ComputerProbeEnable", true).getBoolean(true);
        ElnToOtherEnergyConverterEnable = config.get("compatibility", "ElnToOtherEnergyConverterEnable", true).getBoolean(true);

        replicatorPop = config.get("entity", "replicatorPop", true).getBoolean(true);
        ReplicatorPopProcess.popPerSecondPerPlayer = config.get("entity", "replicatorPopWhenThunderPerSecond", 1.0 / 120).getDouble(1.0 / 120);
        replicatorRegistrationId = config.get("entity", "replicatorId", -1).getInt(-1);
        killMonstersAroundLamps = config.get("entity", "killMonstersAroundLamps", true).getBoolean(true);
        killMonstersAroundLampsRange = config.get("entity", "killMonstersAroundLampsRange", 9).getInt(9);

        forceOreRegen = config.get("mapGenerate", "forceOreRegen", false).getBoolean(false);
        genCopper = config.get("mapGenerate", "copper", true).getBoolean(true);
        genLead = config.get("mapGenerate", "lead", true).getBoolean(true);
        genTungsten = config.get("mapGenerate", "tungsten", true).getBoolean(true);
        genCinnabar = config.get("mapGenerate", "cinnabar", true).getBoolean(true);
        genCinnabar = false;

        oredictTungsten = config.get("dictionary", "tungsten", false).getBoolean(false);
        if (oredictTungsten) {
            dictTungstenOre = "oreTungsten";
            dictTungstenDust = "dustTungsten";
            dictTungstenIngot = "ingotTungsten";
        } else {
            dictTungstenOre = "oreElnTungsten";
            dictTungstenDust = "dustElnTungsten";
            dictTungstenIngot = "ingotElnTungsten";
        }
        oredictChips = config.get("dictionary", "chips", true).getBoolean(true);
        if (oredictChips) {
            dictCheapChip = "circuitBasic";
            dictAdvancedChip = "circuitAdvanced";
        } else {
            dictCheapChip = "circuitElnBasic";
            dictAdvancedChip = "circuitElnAdvanced";
        }

        incandescentLampLife = config.get("lamp", "incandescentLifeInHours", 16.0).getDouble(16.0) * 3600;
        economicLampLife = config.get("lamp", "economicLifeInHours", 64.0).getDouble(64.0) * 3600;
        carbonLampLife = config.get("lamp", "carbonLifeInHours", 6.0).getDouble(6.0) * 3600;
        ledLampLife = config.get("lamp", "ledLifeInHours", 512.0).getDouble(512.0) * 3600;
        ledLampInfiniteLife = config.get("lamp", "infiniteLedLife", false).getBoolean();

        fuelGeneratorTankCapacity = config.get("fuelGenerator",
            "tankCapacityInSecondsAtNominalPower", 20 * 60).getDouble(20 * 60);

        addOtherModOreToXRay = config.get("xrayscannerconfig", "addOtherModOreToXRay", true).getBoolean(true);
        xRayScannerRange = (float) config.get("xrayscannerconfig", "rangeInBloc", 5.0).getDouble(5.0);
        xRayScannerRange = Math.max(Math.min(xRayScannerRange, 10), 4);
        xRayScannerCanBeCrafted = config.get("xrayscannerconfig", "canBeCrafted", true).getBoolean(true);

        electricalFrequency = config.get("simulation", "electricalFrequency", 20).getDouble(20);
        electricalInterSystemOverSampling = config.get("simulation", "electricalInterSystemOverSampling", 50).getInt(50);
        thermalFrequency = config.get("simulation", "thermalFrequency", 400).getDouble(400);

        wirelessTxRange = config.get("wireless", "txRange", 32).getInt();

        wailaEasyMode = config.get("balancing", "wailaEasyMode", false, "Display more detailed WAILA info on some machines").getBoolean(false);
        cablePowerFactor = config.get("balancing", "cablePowerFactor", 1.0, "Multiplication factor for cable power capacity. We recommend 2.0 to 4.0 for larger modpacks, but 1.0 for Eln standalone, or if you like a challenge.", 0.5, 4.0).getDouble(1.0);

        fuelHeatValueFactor = config.get("balancing", "fuelHeatValueFactor", 0.0000675,
            "Factor to apply when converting real word heat values to Minecraft heat values (1mB = 1l).").getDouble();

        Eln.noSymbols = config.get("general", "noSymbols", false).getBoolean();
        Eln.noVoltageBackground = config.get("general", "noVoltageBackground", false).getBoolean();

        Eln.maxSoundDistance = config.get("debug", "maxSoundDistance", 16.0).getDouble();

        config.save();

        eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);

        simulator = new Simulator(0.05, 1 / electricalFrequency, electricalInterSystemOverSampling, 1 / thermalFrequency);
        nodeManager = new NodeManager("caca");
        ghostManager = new GhostManager("caca2");
        delayedTask = new DelayedTaskManager();

        playerManager = new PlayerManager();
        //tileEntityDestructor = new TileEntityDestructor();

        oreRegenerate = new OreRegenerate();
        nodeServer = new NodeServer();
        clientLiveDataManager = new LiveDataManager();

        packetHandler = new PacketHandler();
        // ForgeDummyContainer
        instance = this;

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        Item itemCreativeTab = new Item()
            .setTranslationKey("eln:elncreativetab"); // TODO(1.12 WP6 icon): was setTextureName("eln:elncreativetab")
        GameRegistryCompat.registerItem(itemCreativeTab, "eln.itemCreativeTab");
        creativeTab = new GenericCreativeTab("Eln", itemCreativeTab);

        oreBlock = (OreBlock) new OreBlock().setCreativeTab(creativeTab).setTranslationKey("OreEln");

        sharedItem = (SharedItem) new SharedItem()
            .setCreativeTab(creativeTab).setMaxStackSize(64)
            .setTranslationKey("sharedItem");

        sharedItemStackOne = (SharedItem) new SharedItem()
            .setCreativeTab(creativeTab).setMaxStackSize(1)
            .setTranslationKey("sharedItemStackOne");

        transparentNodeBlock = (TransparentNodeBlock) new TransparentNodeBlock(
            Material.IRON,
            TransparentNodeEntity.class)
            .setCreativeTab(creativeTab); // TODO(1.12 WP5): was setBlockTextureName("iron_block") (break particles)
        sixNodeBlock = (SixNodeBlock) new SixNodeBlock(
            Material.PLANTS, SixNodeEntity.class)
            .setCreativeTab(creativeTab); // TODO(1.12 WP5): was setBlockTextureName("iron_block")

        ghostBlock = new GhostBlock(); // TODO(1.12 WP5): was setBlockTextureName("iron_block")
        lightBlock = (LightBlock) new LightBlock();

        obj.loadAllElnModels();

        // 1.12: registered on RegistryEvent.Register (GameRegistryCompat), names eln:snake_case (rule 6)
        GameRegistryCompat.registerItem(sharedItem, "Eln.sharedItem");
        GameRegistryCompat.registerItem(sharedItemStackOne, "Eln.sharedItemStackOne");
        GameRegistryCompat.registerBlock(ghostBlock, null, "Eln.ghostBlock"); // 1.7.10 also made an (unused) ItemBlock
        GameRegistryCompat.registerBlock(lightBlock, null, "Eln.lightBlock");
        GameRegistryCompat.registerBlock(sixNodeBlock, SixNodeItem.class, "Eln.SixNode");
        GameRegistryCompat.registerBlock(transparentNodeBlock, TransparentNodeItem.class, "Eln.TransparentNode");
        GameRegistryCompat.registerBlock(oreBlock, OreItem.class, "Eln.Ore");
        GameRegistry.registerTileEntity(TransparentNodeEntity.class, new ResourceLocation(MODID, "transparent_node_entity"));
        GameRegistry.registerTileEntity(TransparentNodeEntityWithFluid.class, new ResourceLocation(MODID, "transparent_node_entity_wf"));
        // TileEntity.addMapping(TransparentNodeEntityWithSiededInv.class, "TransparentNodeEntityWSI");
        GameRegistry.registerTileEntity(SixNodeEntity.class, new ResourceLocation(MODID, "six_node_entity"));
        GameRegistry.registerTileEntity(LightBlockEntity.class, new ResourceLocation(MODID, "light_block_entity"));

        NodeManager.registerUuid(sixNodeBlock.getNodeUuid(), SixNode.class);
        NodeManager.registerUuid(transparentNodeBlock.getNodeUuid(), TransparentNode.class);

        sixNodeItem = (SixNodeItem) GameRegistryCompat.getItemBlock(sixNodeBlock);
        transparentNodeItem = (TransparentNodeItem) GameRegistryCompat.getItemBlock(transparentNodeBlock);

        oreItem = (OreItem) GameRegistryCompat.getItemBlock(oreBlock);
        /*
         *
		 * int id = 0,subId = 0,completId; String name;
		 */

        SixNode.sixNodeCacheList.add(new SixNodeCacheStd());

        registerTestBlock();
        registerEnergyConverter();
        // registerComputer(): OpenComputers/ComputerCraft probe dropped (rule 8)

        registerArmor();
        registerTool();
        registerOre();

        //SIX NODE REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerGround(2);
        registerElectricalSource(3);
        registerElectricalCable(32);
        registerThermalCable(48);
        registerLampSocket(64);
        registerLampSupply(65);
        registerBatteryCharger(66);
        registerPowerSocket(67);

        registerWirelessSignal(92);
        registerElectricalDataLogger(93);
        registerElectricalRelay(94);
        registerElectricalGateSource(95);
        registerPassiveComponent(96);
        registerSwitch(97);
        registerElectricalManager(98);
        registerElectricalSensor(100);
        registerThermalSensor(101);
        registerElectricalVuMeter(102);
        registerElectricalAlarm(103);
        registerElectricalEnvironmentalSensor(104);
        registerElectricalRedstone(108);
        registerElectricalGate(109);
        registerTreeResinCollector(116);
        registerSixNodeMisc(117);
        registerLogicalGates(118);
        registerAnalogChips(124);

        //TRANSPARENT NODE REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerPowerComponent(1);
        registerTransformer(2);
        registerHeatFurnace(3);
        registerTurbine(4);
        registerElectricalAntenna(7);
        registerBattery(16);
        registerElectricalFurnace(32);
        registerMacerator(33);
        registerCompressor(35);
        registerMagnetizer(36);
        registerPlateMachine(37);
        registerEggIncubator(41);
        registerAutoMiner(42);
        registerSolarPanel(48);
        registerWindTurbine(49);
        registerThermalDissipatorPassiveAndActive(64);
        registerTransparentNodeMisc(65);
        registerTurret(66);
        registerFuelGenerator(67);
        registerGridDevices(123);


        //ITEM REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerHeatingCorp(1);
        // registerThermalIsolator(2);
        registerRegulatorItem(3);
        registerLampItem(4);
        registerProtection(5);
        registerCombustionChamber(6);
        registerFerromagneticCore(7);
        registerIngot(8);
        registerDust(9);
        registerElectricalMotor(10);
        registerSolarTracker(11);
        //
        registerMeter(14);
        registerElectricalDrill(15);
        registerOreScanner(16);
        registerMiningPipe(17);
        registerTreeResinAndRubber(64);
        registerRawCable(65);
        registerBrush(119);
        registerMiscItem(120);
        registerElectricalTool(121);
        registerPortableItem(122);
        registerFuelBurnerItem(124);

        // Register WIP items only on development runs!
        if (isDevelopmentRun()) {
            registerWipItems();
        }
    }

    public static FMLEventChannel eventChannel;
    //boolean computerCraftReady = false;
    public static boolean ComputerProbeEnable;
    public static boolean ElnToOtherEnergyConverterEnable;

    // FMLCommonHandler.instance().bus().register(this);


    @EventHandler
    public void modsLoaded(FMLPostInitializationEvent event) {
        Other.check();
        recipeMaceratorModOres();
    }

    @EventHandler
    public void load(FMLInitializationEvent event) {
        HashSet<String> oreNames = new HashSet<String>();
        {
            final String[] names = OreDictionary.getOreNames();
            Collections.addAll(oreNames, names);
        }

        //
        registerReplicator();
        //

        recipeEnergyConverter();

        recipeArmor();
        recipeTool();

        recipeGround();
        recipeElectricalSource();
        recipeElectricalCable();
        recipeThermalCable();
        recipeLampSocket();
        recipeLampSupply();
        recipePowerSocket();
        recipePassiveComponent();
        recipeSwitch();
        recipeWirelessSignal();
        recipeElectricalRelay();
        recipeElectricalDataLogger();
        recipeElectricalGateSource();
        recipeElectricalBreaker();
        recipeFuses();
        recipeElectricalVuMeter();
        recipeElectricalEnvironmentalSensor();
        recipeElectricalRedstone();
        recipeElectricalGate();
        recipeElectricalAlarm();
        recipeSixNodeCache();
        recipeElectricalSensor();
        recipeThermalSensor();
        recipeSixNodeMisc();


        recipeTurret();
        recipeMachine();
        recipeChips();
        recipeTransformer();
        recipeHeatFurnace();
        recipeTurbine();
        recipeBattery();
        recipeElectricalFurnace();
        recipeAutoMiner();
        recipeSolarPanel();

        recipeThermalDissipatorPassiveAndActive();
        recipeElectricalAntenna();
        recipeEggIncubator();
        recipeBatteryCharger();
        recipeTransporter();
        recipeWindTurbine();
        recipeFuelGenerator();

        recipeGeneral();
        recipeHeatingCorp();
        recipeRegulatorItem();
        recipeLampItem();
        recipeProtection();
        recipeCombustionChamber();
        recipeFerromagneticCore();
        recipeIngot();
        recipeDust();
        recipeElectricalMotor();
        recipeSolarTracker();
        recipeDynamo();
        recipeWindRotor();
        recipeMeter();
        recipeElectricalDrill();
        recipeOreScanner();
        recipeMiningPipe();
        recipeTreeResinAndRubber();
        recipeRawCable();
        recipeMiscItem();
        recipeBatteryItem();
        recipeElectricalTool();
        recipePortableCapacitor();

        recipeFurnace();
        recipeMacerator();
        recipeCompressor();
        recipePlateMachine();
        recipeMagnetizer();
        recipeFuelBurnerItem();

        recipeECoal();

        recipeGridDevices(oreNames);

        proxy.registerRenderers();

        TR("itemGroup.Eln");

        checkRecipe();

        // 1.12 port: achievements, their event handlers and the Waila registration dropped (rule 8)

        Utils.println("Electrical age init done");
    }

    public static EnergyConverterElnToOtherBlock elnToOtherBlockLvu;
    public static EnergyConverterElnToOtherBlock elnToOtherBlockMvu;
    public static EnergyConverterElnToOtherBlock elnToOtherBlockHvu;



    public static TestBlock testBlock;

    private void checkRecipe() {
        Utils.println("No recipe for ");
        for (SixNodeDescriptor d : sixNodeItem.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                Utils.println("  " + d.name);
            }
        }
        for (TransparentNodeDescriptor d : transparentNodeItem.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                Utils.println("  " + d.name);
            }
        }
        for (GenericItemUsingDamageDescriptor d : sharedItem.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                Utils.println("  " + d.name);
            }
        }
        for (GenericItemUsingDamageDescriptor d : sharedItemStackOne.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                Utils.println("  " + d.name);
            }
        }
    }

    private boolean recipeExists(ItemStack stack) {
        if (Utils.isEmpty(stack))
            return false;
        for (Object o : CraftingManager.REGISTRY) {
            if (o instanceof IRecipe) {
                IRecipe r = (IRecipe) o;
                if (Utils.isEmpty(r.getRecipeOutput()))
                    continue;
                if (Utils.areSame(stack, r.getRecipeOutput()))
                    return true;
            }
        }
        return false;
    }

    // ElnHttpServer elnHttpServer;

    public static ServerEventListener serverEventListener;

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {

        serverEventListener = new ServerEventListener();

    }

	/*
	 * @EventHandler public void clientStart(Client event) {
	 *
	 *
	 * }
	 */

    @EventHandler
	/* Remember to use the right event! */
    public void onServerStopped(FMLServerStoppedEvent ev) {
        TutorialSignElement.resetBalise();


        LightBlockEntity.observers.clear();
        NodeBlockEntity.clientList.clear();
        TeleporterElement.teleporterList.clear();
        IWirelessSignalSpot.spots.clear();
        playerManager.clear();


        clientLiveDataManager.stop();
        nodeManager.clear();
        ghostManager.clear();
        saveConfig = null;
        oreRegenerate.clear();


        delayedTask.clear();
        DelayedBlockRemove.clear();

        serverEventListener.clear();


        nodeServer.stop();

        simulator.stop();

        //tileEntityDestructor.clear();
        LampSupplyElement.channelMap.clear();
        PowerSocketElement.channelMap.clear();
        WirelessSignalTxElement.channelMap.clear();

    }

    //public TileEntityDestructor tileEntityDestructor;

    public static WindProcess wind;

    @EventHandler
    public void onServerStart(FMLServerAboutToStartEvent ev) {
        TeleporterElement.teleporterList.clear();
        //tileEntityDestructor.clear();
        LightBlockEntity.observers.clear();
        WirelessSignalTxElement.channelMap.clear();
        LampSupplyElement.channelMap.clear();
        PowerSocketElement.channelMap.clear();
        playerManager.clear();
        clientLiveDataManager.start();
        simulator.init();
        simulator.addSlowProcess(wind = new WindProcess());

        if (replicatorPop)
            simulator.addSlowProcess(new ReplicatorPopProcess());
        simulator.addSlowProcess(itemEnergyInventoryProcess = new ItemEnergyInventoryProcess());
    }

    @EventHandler
	/* Remember to use the right event! */
    public void onServerStarting(FMLServerStartingEvent ev) {

        {
            MinecraftServer server = FMLCommonHandler.instance()
                .getMinecraftServerInstance();
            WorldServer worldServer = server.worlds[0];


            ghostManagerNbt = (GhostManagerNbt) worldServer.getMapStorage().getOrLoadData(
                GhostManagerNbt.class, "GhostManager");
            if (ghostManagerNbt == null) {
                ghostManagerNbt = new GhostManagerNbt("GhostManager");
                worldServer.getMapStorage().setData("GhostManager", ghostManagerNbt);
            }

            saveConfig = (SaveConfig) worldServer.getMapStorage().getOrLoadData(
                SaveConfig.class, "SaveConfig");
            if (saveConfig == null) {
                saveConfig = new SaveConfig("SaveConfig");
                worldServer.getMapStorage().setData("SaveConfig", saveConfig);
            }
            // saveConfig.init();

            nodeManagerNbt = (NodeManagerNbt) worldServer.getMapStorage().getOrLoadData(
                NodeManagerNbt.class, "NodeManager");
            if (nodeManagerNbt == null) {
                nodeManagerNbt = new NodeManagerNbt("NodeManager");
                worldServer.getMapStorage().setData("NodeManager", nodeManagerNbt);
            }

            nodeServer.init();
        }

        {
            MinecraftServer s = FMLCommonHandler.instance().getMinecraftServerInstance();
            ICommandManager command = s.getCommandManager();
            ServerCommandManager manager = (ServerCommandManager) command;
            manager.registerCommand(new ConsoleListener());
        }

        regenOreScannerFactors();
    }


    public static CableRenderDescriptor stdCableRenderSignal;
    public static CableRenderDescriptor stdCableRender50V;
    public static CableRenderDescriptor stdCableRender200V;
    public static CableRenderDescriptor stdCableRender800V;
    public static CableRenderDescriptor stdCableRender3200V;

    public static final double gateOutputCurrent = 0.100;
    public static final double SVU = 50, SVII = gateOutputCurrent / 50,
        SVUinv = 1.0 / SVU;
    public static final double LVU = 50;
    public static final double MVU = 200;
    public static final double HVU = 800;
    public static final double VVU = 3200;

    public static final double SVP = gateOutputCurrent * SVU;

    public static double LVP() {
        return 1000 * cablePowerFactor;
    }
    public static double MVP() {
        return 2000 * cablePowerFactor;
    }
    public static double HVP() {
        return 5000 * cablePowerFactor;
    }
    public static double VVP() {
        return 15000 * cablePowerFactor;
    }

    public static final double cableHeatingTime = 30;
    public static final double cableWarmLimit = 130;
    public static final double cableThermalConductionTao = 0.5;
    public static final ThermalLoadInitializer cableThermalLoadInitializer = new ThermalLoadInitializer(
        cableWarmLimit, -100, cableHeatingTime, cableThermalConductionTao);
    public static final ThermalLoadInitializer sixNodeThermalLoadInitializer = new ThermalLoadInitializer(
        cableWarmLimit, -100, cableHeatingTime, 1000);

    public static int wirelessTxRange = 32;

    public static FunctionTable batteryVoltageFunctionTable;

    public static ArrayList<ItemStack> furnaceList = new ArrayList<ItemStack>();

    public static ElectricalFurnaceDescriptor electricalFurnace;
    public static RecipesList maceratorRecipes = new RecipesList();

    public static RecipesList compressorRecipes = new RecipesList();
    public static RecipesList plateMachineRecipes = new RecipesList();

    public static RecipesList magnetiserRecipes = new RecipesList();

    public static double incandescentLampLife;
    public static double economicLampLife;
    public static double carbonLampLife;
    public static double ledLampLife;
    public static boolean ledLampInfiniteLife = false;

    public static OreDescriptor oreTin, oreCopper, oreSilver;

    public static GenericItemUsingDamageDescriptorWithComment dustTin,
        dustCopper, dustSilver;

    public static final HashMap<String, ItemStack> dictionnaryOreFromMod = new HashMap<String, ItemStack>();

    public static GenericItemUsingDamageDescriptorWithComment tinIngot, copperIngot,
        silverIngot, plumbIngot, tungstenIngot;

    public static double fuelGeneratorTankCapacity = 20 * 60;

    static public GenericItemUsingDamageDescriptor multiMeterElement,
        thermometerElement, allMeterElement;

    public static TreeResin treeResin;

    public static MiningPipeDescriptor miningPipeDescriptor;

    public static DataLogsPrintDescriptor dataLogsPrintDescriptor;

    public static int replicatorRegistrationId = -1;

    public static double getSmallRs() {
        return instance.lowVoltageCableDescriptor.electricalRs;
    }

    public static void applySmallRs(NbtElectricalLoad aLoad) {
        instance.lowVoltageCableDescriptor.applyTo(aLoad);
    }

    public static void applySmallRs(Resistor r) {
        instance.lowVoltageCableDescriptor.applyTo(r);
    }

    static ItemStack findItemStack(String name, int stackSize) {
        ItemStack stack = GameRegistryCompat.findItemStack("Eln", name, stackSize);
        if (Utils.isEmpty(stack)) {
            stack = dictionnaryOreFromMod.get(name);
            stack = Utils.newItemStack(Item.getIdFromItem(stack.getItem()), stackSize, stack.getMetadata());
        }
        return stack;
    }

    public static ItemStack findItemStack(String name) {
        return findItemStack(name, 1);
    }

    public static String firstExistingOre(String... oreNames) {
        for (String oreName : oreNames) {
            if (OreDictionary.doesOreNameExist(oreName)) {
                return oreName;
            }
        }

        return "";
    }

    public static boolean isDevelopmentRun() {
        return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }
}
