package mods.eln;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientKeyHandler;
import mods.eln.client.SoundLoader;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.entity.ReplicatorPopProcess;
import mods.eln.generic.GenericCreativeTab;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment;
import mods.eln.generic.SharedItem;
import mods.eln.generic.genericArmorItem;
import mods.eln.generic.genericArmorItem.ArmourType;
import mods.eln.ghost.GhostBlock;
import mods.eln.ghost.GhostGroup;
import mods.eln.ghost.GhostManager;
import mods.eln.item.BrushDescriptor;
import mods.eln.item.CombustionChamber;
import mods.eln.item.CopperCableDescriptor;
import mods.eln.item.DielectricItem;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.item.HeatingCorpElement;
import mods.eln.item.ItemAxeEln;
import mods.eln.item.ItemPickaxeEln;
import mods.eln.item.LampDescriptor;
import mods.eln.item.MachineBoosterDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.item.OreScanner;
import mods.eln.item.OverHeatingProtectionDescriptor;
import mods.eln.item.OverVoltageProtectionDescriptor;
import mods.eln.item.SolarTrackerDescriptor;
import mods.eln.item.TreeResin;
import mods.eln.item.electricalinterface.ItemEnergyInventoryProcess;
import mods.eln.item.electricalitem.BatteryItem;
import mods.eln.item.electricalitem.ElectricalArmor;
import mods.eln.item.electricalitem.ElectricalAxe;
import mods.eln.item.electricalitem.ElectricalLampItem;
import mods.eln.item.electricalitem.ElectricalPickaxe;
import mods.eln.item.electricalitem.PortableOreScannerItem;
import mods.eln.item.electricalitem.PortableOreScannerItem.RenderStorage.OreScannerConfigElement;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorAnalogDescriptor;
import mods.eln.item.regulator.RegulatorOnOffDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.FunctionTableYProtect;
import mods.eln.misc.I18N;
import mods.eln.misc.IFunction;
import mods.eln.misc.LiveDataManager;
import mods.eln.misc.Obj3DFolder;
import mods.eln.misc.Recipe;
import mods.eln.misc.RecipesList;
import mods.eln.misc.TileEntityDestructor;
import mods.eln.misc.Utils;
import mods.eln.misc.Version;
import mods.eln.misc.WindProcess;
import mods.eln.misc.series.SerieEE;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import mods.eln.node.NodeServer;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeBlock;
import mods.eln.node.six.SixNodeCacheStd;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.six.SixNodeItem;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeBlock;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.node.transparent.TransparentNodeItem;
import mods.eln.ore.OreBlock;
import mods.eln.ore.OreDescriptor;
import mods.eln.ore.OreItem;
import mods.eln.server.ConsoleListener;
import mods.eln.server.OreRegenerate;
import mods.eln.server.PlayerManager;
import mods.eln.server.SaveConfig;
import mods.eln.server.ServerEventListener;
import mods.eln.signalinductor.SignalInductorDescriptor;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.ThermalLoadInitializerByPowerDrop;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.simplenode.computerprobe.ComputerProbeBlock;
import mods.eln.simplenode.computerprobe.ComputerProbeEntity;
import mods.eln.simplenode.computerprobe.ComputerProbeNode;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor.ElnDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor.Ic2Descriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor.OcDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherEntity;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;
import mods.eln.simplenode.test.TestBlock;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor;
import mods.eln.sixnode.batterycharger.BatteryChargerDescriptor;
import mods.eln.sixnode.diode.DiodeDescriptor;
import mods.eln.sixnode.electricalalarm.ElectricalAlarmDescriptor;
import mods.eln.sixnode.electricalbreaker.ElectricalBreakerDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor;
import mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerDescriptor;
import mods.eln.sixnode.electricalentitysensor.ElectricalEntitySensorDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceRenderObj;
import mods.eln.sixnode.electricallightsensor.ElectricalLightSensorDescriptor;
import mods.eln.sixnode.electricalmath.ElectricalMathDescriptor;
import mods.eln.sixnode.electricalredstoneinput.ElectricalRedstoneInputDescriptor;
import mods.eln.sixnode.electricalredstoneoutput.ElectricalRedstoneOutputDescriptor;
import mods.eln.sixnode.electricalrelay.ElectricalRelayDescriptor;
import mods.eln.sixnode.electricalsource.ElectricalSourceDescriptor;
import mods.eln.sixnode.electricalswitch.ElectricalSwitchDescriptor;
import mods.eln.sixnode.electricaltimeout.ElectricalTimeoutDescriptor;
import mods.eln.sixnode.electricalvumeter.ElectricalVuMeterDescriptor;
import mods.eln.sixnode.electricalwatch.ElectricalWatchDescriptor;
import mods.eln.sixnode.electricalweathersensor.ElectricalWeatherSensorDescriptor;
import mods.eln.sixnode.electricalwindsensor.ElectricalWindSensorDescriptor;
import mods.eln.sixnode.electricasensor.ElectricalSensorDescriptor;
import mods.eln.sixnode.energymeter.EnergyMeterDescriptor;
import mods.eln.sixnode.groundcable.GroundCableDescriptor;
import mods.eln.sixnode.hub.HubDescriptor;
import mods.eln.sixnode.lampsocket.LampSocketDescriptor;
import mods.eln.sixnode.lampsocket.LampSocketStandardObjRender;
import mods.eln.sixnode.lampsocket.LampSocketSuspendedObjRender;
import mods.eln.sixnode.lampsocket.LampSocketType;
import mods.eln.sixnode.lampsocket.LightBlock;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import mods.eln.sixnode.lampsupply.LampSupplyDescriptor;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import mods.eln.sixnode.modbusrtu.ModbusRtuDescriptor;
import mods.eln.sixnode.modbusrtu.ModbusServer;
import mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixDescriptor;
import mods.eln.sixnode.powerinductorsix.PowerInductorSixDescriptor;
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
import mods.eln.solver.ConstSymbole;
import mods.eln.solver.ISymbole;
import mods.eln.sound.SoundCommand;
import mods.eln.transparentnode.autominer.AutoMinerDescriptor;
import mods.eln.transparentnode.battery.BatteryDescriptor;
import mods.eln.transparentnode.computercraftio.ComputerCraftIoDescriptor;
import mods.eln.transparentnode.computercraftio.PeripheralHandler;
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
import mods.eln.transparentnode.solarpannel.SolarPannelDescriptor;
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
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = Eln.MODID, name = Eln.NAME, version = Version.REVISION)
// @Mod(modid = "Eln", name = "Electrical Age", version = "BETA-1.2.0b")
// @NetworkMod(clientSideRequired = true, serverSideRequired = true, channels = { "miaouMod" }, packetHandler = PacketHandler.class)
public class Eln {
	// Mod information (override from 'mcmod.info' file)
	protected final static String MODID = "Eln";
	protected final static String NAME = "Electrical Age";
	private final static String URL = "http://electrical-age.net";
	private final static String UPDATE_URL = "https://github.com/Dolu1990/ElectricalAge/releases";
	private final static String LOGO_FILE = "logo.png";
	private final static String[] AUTHORS = { "Dolu1990", "lambdaShade", "cm0x4D", "TheBroBeans", "DrummingFish" };

	public static String channelName = "miaouMod";

	public static final String[] objNames = new String[] {
			"/model/PowerElectricPrimitives/PowerElectricPrimitives.obj",
			"/model/SimpleLamp/SimpleLamp.obj",
			"/model/condo200/condo200.obj",
			"/model/WallClock/WallClock.obj",
			"/model/TutoPlate/TutoPlate.obj",
			"/model/relay800/relay800.obj",
			"/model/hub/hub.obj",
			"/model/electricalweathersensor/electricalweathersensor.obj",
			"/model/SmallWaterWheel/SmallWaterWheel.obj",
			"/model/ProximitySensor/ProximitySensor.obj",
			"/model/FlatLamp/FlatLamp.obj",
			"/model/AutoMiner/AutoMiner.obj",
			"/model/Anemometer/Anemometer.obj",
			"/model/XRayScanner/XRayScanner.obj",
			"/model/RobustLampSuspended/RobustLampSuspended.obj",
			"/model/activeThermalDissipatorA/activethermaldissipatora.obj",
			"/model/200vactiveThermalDissipatorA/200vactivethermaldissipatora.obj",
			"/model/AlarmMedium/alarmmedium.obj",
			"/model/BatteryBig/BatteryBig.obj",
			"/model/batterychargera/batterychargera.obj",
			"/model/batterychargerb/batterychargerb.obj",
			"/model/ClassicLampSocket/ClassicLampSocket.obj",
			"/model/compressora/compressora.obj",
			"/model/compressorb/compressorb.obj",
			"/model/DataloggerCRTFloor/DataloggerCRTFloor.obj",
			"/model/daylightsensor/daylightsensor.obj",
			"/model/eggIncubator/eggincubator.obj",
			"/model/ElectricalSensor/electricalsensor.obj",
			"/model/electricaltimer/electricaltimer.obj",
			"/model/ElectricFurnace/ElectricFurnace.obj",
			"/model/eleToRed/eletored.obj",
			"/model/groundCable/groundcable.obj",
			"/model/HighCapacityBattery/HighCapacityBattery.obj",
			"/model/HighCurrentBattery/HighCurrentBattery.obj",
			"/model/HighVoltageBattery/HighVoltageBattery.obj",
			"/model/HighVoltageSwitch/HighVoltageSwitch.obj",
			"/model/lampsupply/lampsupply.obj",
			"/model/Led/Led.obj",
			"/model/LedSwitch/ledswitch.obj",
			"/model/lightsensor/lightsensor.obj",
			"/model/LongLifeBattery/LongLifeBattery.obj",
			"/model/LowCostBattery/LowCostBattery.obj",
			"/model/LowCostBattery2/LowCostBattery2.obj",
			"/model/LowPowerReceiverAntenna/lowpowerreceiverantenna.obj",
			"/model/LowPowerTransmitterAntenna/lowpowertransmitterantenna.obj",
			"/model/LowVoltageSwitch/LowVoltageSwitch.obj",
			"/model/macerator50V/macerator50V.obj",
			"/model/maceratora/maceratora.obj",
			"/model/maceratorb/maceratorb.obj",
			"/model/magnetizera/magnetizera.obj",
			"/model/magnetizerb/magnetizerb.obj",
			"/model/MediumVoltageSwitch/MediumVoltageSwitch.obj",
			"/model/passiveThermalDissipatorA/passivethermaldissipatora.obj",
			"/model/plateMachineA/platemachinea.obj",
			"/model/plateMachineB/platemachineb.obj",
			"/model/PLC/PLC.obj",
			"/model/redToEle/redtoele.obj",
			"/model/RelayBig/RelayBig.obj",
			"/model/RobustLamp/RobustLamp.obj",
			"/model/RTU/RTU.obj",
			"/model/signalsourcepot/signalsourcepot.obj",
			"/model/smallsolarpannel/smallsolarpannel.obj",
			"/model/smallsolarpannelrot/smallsolarpannelrot.obj",
			"/model/SolarPanel2x2/SolarPanel2x2.obj",
			"/model/stoneCache/stonecache.obj",
			"/model/StoneFurnace/stonefurnace.obj",
			"/model/StreetLight/StreetLight.obj",
			"/model/TemperatureSensor/temperaturesensor.obj",
			"/model/ThermalSensor/thermalsensor.obj",
			"/model/transformator/corea/feromagneticcorea.obj",
			"/model/transformator/transformator.obj",
			"/model/Transporter/Transporter.obj",
			"/model/treeresincolector/treeresincolector.obj",
			"/model/turbine50V/turbine50V.obj",
			"/model/turbineB/turbineb.obj",
			"/model/turbineBBlue/turbinebblue.obj",
			"/model/VoltageSensor/voltagesensor.obj",
			"/model/voltagesource/voltagesource.obj",
			"/model/Vumeter/Vumeter.obj",
			"/model/WindTurbineMini/WindTurbineMini.obj",
			"/model/wirelesssignalrepeater/wirelesssignalrepeater.obj",
			"/model/wirelesssignalrx/wirelesssignalrx.obj",
			"/model/wirelesssignaltx/wirelesssignaltx.obj",
			"/model/Turret/Turret.obj"
			// "/model/BatteryBigHV/BatteryBigHV.obj"

	};

	public static final double networkSerializeValueFactor = 100.0;

	// public static final byte packetNodeSerialized24bitPosition = 11;
	public static final byte packetNodeSerialized48bitPosition = 12;
	public static final byte packetNodeRefreshRequest = 13;
	public static final byte packetPlayerKey = 14;
	public static final byte packetNodeSingleSerialized = 15;
	public static final byte packetPublishForNode = 16;
	public static final byte packetOpenLocalGui = 17;
	public static final byte packetForClientNode = 18;
	public static final byte packetPlaySound = 19;
	public static final byte packetDestroyUuid = 20;

	public static PacketHandler packetHandler;
	static NodeServer nodeServer;
	public static LiveDataManager clientLiveDataManager;
	public static ClientKeyHandler clientKeyHandler;
	public static SaveConfig saveConfig;
	public static GhostManager ghostManager;
	private static NodeManager nodeManager;
	public static PlayerManager playerManager;
	public static ModbusServer modbusServer;

	public static Simulator simulator = null;
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
	@Instance("Eln")
	public static Eln instance;

	// Says where the client and server 'proxy' code is loaded.
	@SidedProxy(clientSide = "mods.eln.client.ClientProxy", serverSide = "mods.eln.CommonProxy")
	public static CommonProxy proxy;

	public double electricalFrequancy, thermalFrequancy;
	public int electricalInterSystemOverSampling;

	public ElectricalCableDescriptor highVoltageCableDescriptor;
	public ElectricalCableDescriptor signalCableDescriptor;
	public ElectricalCableDescriptor lowVoltageCableDescriptor;
	public ElectricalCableDescriptor batteryCableDescriptor;
	public ElectricalCableDescriptor meduimVoltageCableDescriptor;

	public OreRegenerate oreRegenerate;

	public static Obj3DFolder obj = new Obj3DFolder();

	public static boolean dicThungsten;
	public static boolean genCooper, genPlumb, genTungsten, genCinnabar;
	public static String dicTungstenOre, dicTungstenDust, dicTungstenIngot;
	public static ArrayList<OreScannerConfigElement> oreScannerConfig = new ArrayList<OreScannerConfigElement>();
	public static boolean modbusEnable = false;

	float xRayScannerRange;
	boolean addOtherModOreToXRay;

	private boolean replicatorPop;

	public boolean forceOreRegen;
	public static boolean debugEnable = false, versionCheckEnable = true;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {

		ModContainer container = FMLCommonHandler.instance().findContainerFor(this);
		// LanguageRegistry.instance().loadLanguagesFor(container, Side.CLIENT);

		// Update ModInfo by code
		ModMetadata meta = event.getModMetadata();
		meta.modId = MODID;
		meta.version = Version.getVersionName();
		meta.name = I18N.getString("mod.name");
		meta.description = I18N.getString("mod.desc");
		meta.url = URL;
		meta.updateUrl = UPDATE_URL;
		meta.logoFile = "/assets/eln/" + LOGO_FILE;
		meta.authorList = Arrays.asList(AUTHORS);
		meta.autogenerated = false; // Force to update from code

		Utils.println(Version.print());

		ArrayList<ISymbole> symboleList = new ArrayList<ISymbole>();
		symboleList.add(new ConstSymbole("A", 0.1));
		symboleList.add(new ConstSymbole("B", 0.2));
		symboleList.add(new ConstSymbole("C", 0.3));
		double value = 0.0;

		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (side == Side.CLIENT)
			MinecraftForge.EVENT_BUS.register(new SoundLoader());

		Configuration config = new Configuration(
				event.getSuggestedConfigurationFile());
		config.load();

		modbusEnable = config.get("modbus", "enable", false).getBoolean(false);
		debugEnable = config.get("debug", "enable", false).getBoolean(false);
		versionCheckEnable = config.get("general", "versionCheckEnable", true).getBoolean(true);

		ComputerProbeEnable = config.get("compatibility", "ComputerProbeEnable", true).getBoolean(true);
		ElnToOtherEnergyConverterEnable = config.get("compatibility", "ElnToOtherEnergyConverterEnable", true).getBoolean(true);

		replicatorPop = config.get("entity", "replicatorPop", true).getBoolean(true);
		replicatorRegistrationId = config.get("entity", "replicatorId", -1).getInt(-1);

		forceOreRegen = config.get("mapGenerate", "forceOreRegen", false).getBoolean(false);
		genCooper = config.get("mapGenerate", "cooper", true).getBoolean(true);
		genPlumb = config.get("mapGenerate", "plumb", true).getBoolean(true);
		genPlumb = config.get("mapGenerate", "lead", genPlumb).getBoolean(genPlumb);
		genTungsten = config.get("mapGenerate", "tungsten", true).getBoolean(true);
		genCinnabar = config.get("mapGenerate", "cinnabar", true).getBoolean(true);
		genCinnabar = false;

		dicThungsten = config.get("dictionary", "tungsten", false).getBoolean(false);
		if (dicThungsten) {
			dicTungstenOre = "oreTungsten";
			dicTungstenDust = "dustTungsten";
			dicTungstenIngot = "ingotTungsten";
		} else {
			dicTungstenOre = "oreElnTungsten";
			dicTungstenDust = "dustElnTungsten";
			dicTungstenIngot = "ingotElnTungsten";
		}

		incondecentLampLife = config.get("lamp", "incondescentLifeInHours", 8).getDouble(8) * 3600;
		economicLampLife = config.get("lamp", "economicLifeInHours", 32).getDouble(32) * 3600;

		addOtherModOreToXRay = config.get("xrayscannerconfig", "addOtherModOreToXRay", true).getBoolean(true);
		xRayScannerRange = (float) config.get("xrayscannerconfig", "rangeInBloc", 5.0).getDouble(5.0);
		xRayScannerRange = Math.max(Math.min(xRayScannerRange, 10), 4);

		electricalFrequancy = config.get("simulation", "electricalFrequancy", 20).getDouble(20);
		electricalInterSystemOverSampling = config.get("simulation", "electricalInterSystemOverSampling", 50).getInt(50);
		thermalFrequancy = config.get("simulation", "thermalFrequancy", 400).getDouble(400);

		config.save();

	}

	public static FMLEventChannel eventChannel;
	//boolean computerCraftReady = false;
	boolean ComputerProbeEnable;
	boolean ElnToOtherEnergyConverterEnable;

	// FMLCommonHandler.instance().bus().register(this);


	@EventHandler
	public void modsLoaded(FMLPostInitializationEvent event) {
		Other.check();
		if (Other.ccLoaded) {
			PeripheralHandler.register();
		}

	}

	@EventHandler
	public void load(FMLInitializationEvent event) {
		Object o;

		//computerCraftReady = Utils.isClassLoaded("dan200.computercraft.ComputerCraft");


		eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);

		simulator = new Simulator(0.05, 1 / electricalFrequancy, electricalInterSystemOverSampling, 1 / thermalFrequancy);

		playerManager = new PlayerManager();
		tileEntityDestructor = new TileEntityDestructor();

		oreRegenerate = new OreRegenerate();
		nodeServer = new NodeServer();
		clientLiveDataManager = new LiveDataManager();

		packetHandler = new PacketHandler();
		// ForgeDummyContainer
		instance = this;

		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

		Item itemCreativeTab = new Item()
				.setUnlocalizedName("eln:elncreativetab")
				.setTextureName("eln:elncreativetab");
		GameRegistry.registerItem(itemCreativeTab, "eln.itemCreativeTab");
		creativeTab = new GenericCreativeTab("Eln", itemCreativeTab);

		oreBlock = (OreBlock) new OreBlock().setCreativeTab(creativeTab).setBlockName("OreEln");

		sharedItem = (SharedItem) new SharedItem()
				.setCreativeTab(creativeTab).setMaxStackSize(64)
				.setUnlocalizedName("sharedItem");

		sharedItemStackOne = (SharedItem) new SharedItem()
				.setCreativeTab(creativeTab).setMaxStackSize(1)
				.setUnlocalizedName("sharedItemStackOne");

		transparentNodeBlock = (TransparentNodeBlock) new TransparentNodeBlock(
				Material.iron,
				TransparentNodeEntity.class)
				.setCreativeTab(creativeTab)
				.setBlockTextureName("iron_block");
		sixNodeBlock = (SixNodeBlock) new SixNodeBlock(
				Material.plants, SixNodeEntity.class)
				.setCreativeTab(creativeTab)
				.setBlockTextureName("iron_block");

		ghostBlock = (GhostBlock) new GhostBlock().setBlockTextureName("iron_block");
		lightBlock = (LightBlock) new LightBlock();
		// obj.loadFolder("eln", "/model");
		for (String path : objNames) {
			obj.loadObj("eln", path);
		}

		GameRegistry.registerItem(sharedItem, "Eln.sharedItem");
		GameRegistry.registerItem(sharedItemStackOne, "Eln.sharedItemStackOne");
		GameRegistry.registerBlock(ghostBlock, "Eln.ghostBlock");
		GameRegistry.registerBlock(lightBlock, "Eln.lightBlock");
		GameRegistry.registerBlock(sixNodeBlock, SixNodeItem.class, "Eln.SixNode");
		GameRegistry.registerBlock(transparentNodeBlock, TransparentNodeItem.class, "Eln.TransparentNode");
		GameRegistry.registerBlock(oreBlock, OreItem.class, "Eln.Ore");
		TileEntity.addMapping(TransparentNodeEntity.class, "TransparentNodeEntity");
		// TileEntity.addMapping(TransparentNodeEntityWithSiededInv.class, "TransparentNodeEntityWSI");
		TileEntity.addMapping(SixNodeEntity.class, "SixNodeEntity");
		TileEntity.addMapping(LightBlockEntity.class, "LightBlockEntity");

		NodeManager.registerUuid(sixNodeBlock.getNodeUuid(), SixNode.class);
		NodeManager.registerUuid(transparentNodeBlock.getNodeUuid(), TransparentNode.class);

		o = Item.getItemFromBlock(sixNodeBlock);
		sixNodeItem = (SixNodeItem) Item.getItemFromBlock(sixNodeBlock);
		transparentNodeItem = (TransparentNodeItem) Item.getItemFromBlock(transparentNodeBlock);

		oreItem = (OreItem) Item.getItemFromBlock(oreBlock);
		/*
		 * 
		 * int id = 0,subId = 0,completId; String name;
		 */

		SixNode.sixNodeCacheList.add(new SixNodeCacheStd());


		registerTestBlock();
		registerEnergyConverter();
		registerComputer();

		registerArmor();
		registerTool();
		registerOre();

		registerGround(2);
		registerElectricalSource(3);
		registerElectricalCable(32);
		registerThermalCable(48);
		registerLampSocket(64);
		registerLampSupply(65);
		registerBatteryCharger(66);
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
		registerElectricalEnvironementalSensor(104);
		registerElectricalRedstone(108);
		registerElectricalGate(109);
		registerTreeResinCollector(116);
		registerSixNodeMisc(117);
		//

		registerPowerComponent(1);
		registerTransformer(2);
		registerHeatFurnace(3);
		registerTurbine(4);
		registerElectricalAntenna(7);
		registerBattery(16);
		registerElectricalFurnace(32);
		registerMacerator(33);
		registerCompressor(35);
		registermagnetiser(36);
		registerPlateMachine(37);
		registerEggIncubator(41);
		registerAutoMiner(42);
		registerSolarPannel(48);
		registerWindTurbine(49);
		registerThermalDissipatorPassiveAndActive(64);
		registerTransparentNodeMisc(65);
		//registerTurret(66);

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
		//
		registerReplicator();
		//

		recipeEnergyConverter();
		recipeComputerProbe();

		recipeArmor();
		recipeTool();

		recipeGround();
		recipeElectricalSource();
		recipeElectricalCable();
		recipeThermalCable();
		recipeLampSocket();
		recipeLampSupply();
		recipePassiveComponent();
		recipeSwitch();
		recipeWirelessSignal();
		recipeElectricalRelay();
		recipeElectricalDataLogger();
		recipeElectricalGateSource();
		recipeElectricalBreaker();
		recipeElectricalVuMeter();
		recipeElectricalEnvironnementalSensor();
		recipeElectricalRedstone();
		recipeElectricalGate();
		recipeElectricalAlarm();
		recipeSixNodeCache();
		recipeElectricalSensor();
		recipeThermalSensor();
		recipeSixNodeMisc();

		recipeMachine();
		recipeTransformer();
		recipeHeatFurnace();
		recipeTurbine();
		recipeBattery();
		recipeElectricalFurnace();
		recipeAutoMiner();
		recipeSolarPannel();

		recipeThermalDissipatorPassiveAndActive();
		recipeElectricalAntenna();
		recipeEggIncubatore();
		recipeBatteryCharger();
		recipeTransporter();
		recipeWindTurbine();

		recipeGeneral();
		recipeHeatingCorp();
		recipeThermalIsolator();
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
		recipePortableCondensator();

		recipeFurnace();
		recipeMacerator();
		recipeCompressor();
		recipePlateMachine();
		recipemagnetiser();

		recipeECoal();

		proxy.registerRenderers();

		LanguageRegistry.instance().addStringLocalization("itemGroup.Eln", "Electrical Age");

		try {
			// elnHttpServer = new ElnHttpServer();
		} catch (Exception e) {

			e.printStackTrace();
		}

		/*
		 * if(Utils.){ Utils.itemRenderer= new RenderItem() }
		 */

		checkRecipe();

		Utils.println("Electrical age init done");
	}

	EnergyConverterElnToOtherBlock elnToOtherBlockLvu;
	EnergyConverterElnToOtherBlock elnToOtherBlockMvu;
	EnergyConverterElnToOtherBlock elnToOtherBlockHvu;

	private void registerEnergyConverter() {
		if (ElnToOtherEnergyConverterEnable) {
			String baseName = "EnergyConverterElnToOther";
			String entityName = "eln.EnergyConverterElnToOtherEntity";

			TileEntity.addMapping(EnergyConverterElnToOtherEntity.class, entityName);
			NodeManager.instance.registerUuid(EnergyConverterElnToOtherNode.getNodeUuidStatic(), EnergyConverterElnToOtherNode.class);

			{
				String blockName = "eln." + baseName + "LVUBlock";
				String name = "ELN to Other 50V converter";
				ElnDescriptor elnDesc = new ElnDescriptor(LVU, LVP);
				Ic2Descriptor ic2Desc = new Ic2Descriptor(32, 1);
				OcDescriptor ocDesc = new OcDescriptor(ic2Desc.outMax*Other.getElnToOcConversionRatio()/Other.getElnToIc2ConversionRatio());
				EnergyConverterElnToOtherDescriptor desc = new EnergyConverterElnToOtherDescriptor(baseName + "LVU", elnDesc,ic2Desc,ocDesc);
				elnToOtherBlockLvu = new EnergyConverterElnToOtherBlock(desc);
				elnToOtherBlockLvu.setCreativeTab(creativeTab).setBlockName(blockName);
				GameRegistry.registerBlock(elnToOtherBlockLvu, blockName);
				LanguageRegistry.addName(elnToOtherBlockLvu, name);
			}
			{
				String blockName = "eln." + baseName + "MVUBlock";
				String name = "ELN to Other 200V converter";
				ElnDescriptor elnDesc = new ElnDescriptor(MVU, MVP);
				Ic2Descriptor ic2Desc = new Ic2Descriptor( 128, 2);
				OcDescriptor ocDesc = new OcDescriptor(ic2Desc.outMax*Other.getElnToOcConversionRatio()/Other.getElnToIc2ConversionRatio());
				EnergyConverterElnToOtherDescriptor desc = new EnergyConverterElnToOtherDescriptor(baseName + "MVU",elnDesc,ic2Desc,ocDesc);
				elnToOtherBlockLvu = new EnergyConverterElnToOtherBlock(desc);
				elnToOtherBlockLvu.setCreativeTab(creativeTab).setBlockName(blockName);
				GameRegistry.registerBlock(elnToOtherBlockLvu, blockName);
				LanguageRegistry.addName(elnToOtherBlockLvu, name);
			}
			{
				String blockName = "eln." + baseName + "HVUBlock";
				String name = "ELN to Other 800V converter";
				ElnDescriptor elnDesc = new ElnDescriptor( HVU, HVP);
				Ic2Descriptor ic2Desc = new Ic2Descriptor(512, 3);
				OcDescriptor ocDesc = new OcDescriptor(ic2Desc.outMax*Other.getElnToOcConversionRatio()/Other.getElnToIc2ConversionRatio());
				EnergyConverterElnToOtherDescriptor desc = new EnergyConverterElnToOtherDescriptor(baseName + "HVU", elnDesc,ic2Desc,ocDesc);
				elnToOtherBlockLvu = new EnergyConverterElnToOtherBlock(desc);
				elnToOtherBlockLvu.setCreativeTab(creativeTab).setBlockName(blockName);
				GameRegistry.registerBlock(elnToOtherBlockLvu, blockName);
				LanguageRegistry.addName(elnToOtherBlockLvu, name);
			}
		}
	}

	
	ComputerProbeBlock computerProbeBlock;
	
	private void registerComputer(){
		if(ComputerProbeEnable){
			String baseName = "ElnProbe";
			String entityName = "eln.ElnProbe";
	
			TileEntity.addMapping(ComputerProbeEntity.class, entityName);
			NodeManager.instance.registerUuid(ComputerProbeNode.getNodeUuidStatic(), ComputerProbeNode.class);
	
			
			String blockName = "eln." + baseName;
			String name = "Eln Computer Probe";
			computerProbeBlock = new ComputerProbeBlock();
			computerProbeBlock.setCreativeTab(creativeTab).setBlockName(blockName);
			GameRegistry.registerBlock(computerProbeBlock, blockName);
			LanguageRegistry.addName(computerProbeBlock, name);
		}
		
	}
	
	TestBlock testBlock;

	private void registerTestBlock() {
		/*
		 * testBlock = new TestBlock(); testBlock.setCreativeTab(creativeTab).setBlockName("TestBlock"); GameRegistry.registerBlock(testBlock, "Eln.TestBlock"); TileEntity.addMapping(TestEntity.class, "Eln.TestEntity"); LanguageRegistry.addName(testBlock,"Test Block"); NodeManager.instance.registerUuid(TestNode.getInfoStatic().getUuid(), TestNode.class);
		 * 
		 * GameRegistry.registerCustomItemStack("Test Block", new ItemStack(testBlock));
		 */
	}

	void checkRecipe() {
		Utils.println("No recipe for ");
		for (SixNodeDescriptor d : sixNodeItem.subItemList.values()) {
			ItemStack stack = d.newItemStack();
			if (aRecipeExist(stack) == false) {
				Utils.println("  " + d.name);
			}
		}
		for (TransparentNodeDescriptor d : transparentNodeItem.subItemList.values()) {
			ItemStack stack = d.newItemStack();
			if (aRecipeExist(stack) == false) {
				Utils.println("  " + d.name);
			}
		}
		for (GenericItemUsingDamageDescriptor d : sharedItem.subItemList.values()) {
			ItemStack stack = d.newItemStack();
			if (aRecipeExist(stack) == false) {
				Utils.println("  " + d.name);
			}
		}
		for (GenericItemUsingDamageDescriptor d : sharedItemStackOne.subItemList.values()) {
			ItemStack stack = d.newItemStack();
			if (aRecipeExist(stack) == false) {
				Utils.println("  " + d.name);
			}
		}
	}

	boolean aRecipeExist(ItemStack stack) {
		if (stack == null)
			return false;
		List list = CraftingManager.getInstance().getRecipeList();
		for (Object o : list) {
			if (o instanceof IRecipe) {
				IRecipe r = (IRecipe) o;
				if (r.getRecipeOutput() == null)
					continue;
				if (Utils.areSame(stack, r.getRecipeOutput()))
					return true;
			}
		}
		return false;
	}

	// ElnHttpServer elnHttpServer;

	public ServerEventListener serverEventListener;

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
	public void onServerStopping(FMLServerStoppingEvent ev) {
		TutorialSignElement.resetBalise();
		modbusServer.destroy();
		LightBlockEntity.observers.clear();
		NodeBlockEntity.clientList.clear();
		TeleporterElement.teleporterList.clear();
		IWirelessSignalSpot.spots.clear();
		playerManager.clear();
		MinecraftServer server = FMLCommonHandler.instance()
				.getMinecraftServerInstance();
		WorldServer worldServer = server.worldServers[0];
		simulator.init();
		nodeServer.init();
		clientLiveDataManager.stop();
		nodeManager = null;
		ghostManager = null;
		saveConfig = null;
		modbusServer = null;
		oreRegenerate.clear();
	}

	public TileEntityDestructor tileEntityDestructor;

	public static WindProcess wind;

	boolean firstStart = true;

	@EventHandler
	/* Remember to use the right event! */
	public void onServerStarting(FMLServerStartingEvent ev) {
		{
			if (firstStart) {

				firstStart = false;
			}

			modbusServer = new ModbusServer();
			TeleporterElement.teleporterList.clear();
			tileEntityDestructor.clear();
			LightBlockEntity.observers.clear();
			WirelessSignalTxElement.channelMap.clear();
			LampSupplyElement.channelMap.clear();
			playerManager.clear();
			clientLiveDataManager.start();
			MinecraftServer server = FMLCommonHandler.instance()
					.getMinecraftServerInstance();
			WorldServer worldServer = server.worldServers[0];
			simulator.init();
			simulator.addSlowProcess(wind = new WindProcess());
			if (replicatorPop)
				simulator.addSlowProcess(new ReplicatorPopProcess());
			simulator.addSlowProcess(itemEnergyInventoryProcess = new ItemEnergyInventoryProcess());

			ghostManager = (GhostManager) worldServer.mapStorage.loadData(
					GhostManager.class, "GhostManager");
			if (ghostManager == null) {
				ghostManager = new GhostManager("GhostManager");
				worldServer.mapStorage.setData("GhostManager", ghostManager);
			}
			ghostManager.init();

			saveConfig = (SaveConfig) worldServer.mapStorage.loadData(
					SaveConfig.class, "SaveConfig");
			if (saveConfig == null) {
				saveConfig = new SaveConfig("SaveConfig");
				worldServer.mapStorage.setData("SaveConfig", saveConfig);
			}
			// saveConfig.init();

			nodeManager = (NodeManager) worldServer.mapStorage.loadData(
					NodeManager.class, "NodeManager");
			if (nodeManager == null) {
				nodeManager = new NodeManager("NodeManager");
				worldServer.mapStorage.setData("NodeManager", nodeManager);
			}

			nodeServer.init();
		}

		{
			MinecraftServer s = MinecraftServer.getServer();
			ICommandManager command = s.getCommandManager();
			ServerCommandManager manager = (ServerCommandManager) command;
			manager.registerCommand(new ConsoleListener());
		}

		regenOreScannerFactors();
	}

	@EventHandler
	public void ServerStopping(FMLServerStoppingEvent ev) {

		serverEventListener.clear();
		playerManager.clear();

		nodeServer.stop();

		simulator.stop();

		tileEntityDestructor.clear();
		LightBlockEntity.observers.clear();
		LampSupplyElement.channelMap.clear();
		WirelessSignalTxElement.channelMap.clear();

	}

	public CableRenderDescriptor stdCableRenderSignal;
	public CableRenderDescriptor stdCableRender50V;
	public CableRenderDescriptor stdCableRender200V;
	public CableRenderDescriptor stdCableRender800V;

	public static double gateOutputCurrent = 0.100;
	public static final double SVU = 50, SVII = gateOutputCurrent / 50,
			SVUinv = 1.0 / SVU;
	public static final double LVU = 50;
	public static final double MVU = 200;
	public static final double HVU = 800;

	public static final double SVP = gateOutputCurrent * SVU;
	public static final double LVP = 1000;
	public static final double MVP = 2000;
	public static final double HVP = 4000;

	public static double electricalCableDeltaTMax = 20;

	public static final double cableHeatingTime = 30;
	public static final double cableWarmLimit = 130;
	public static final double cableThermalConductionTao = 0.5;
	public static final ThermalLoadInitializer cableThermalLoadInitializer = new ThermalLoadInitializer(
			cableWarmLimit, -100, cableHeatingTime, cableThermalConductionTao);
	public static final ThermalLoadInitializer sixNodeThermalLoadInitializer = new ThermalLoadInitializer(
			cableWarmLimit, -100, cableHeatingTime, 1000);

	public static final int wirelessTxRange = 32;

	void registerElectricalCable(int id) {
		int subId, completId;
		String name;

		CableRenderDescriptor render;
		ElectricalCableDescriptor desc;
		{
			subId = 0;

			name = "Signal Cable";

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
			// GameRegistry.registerCustomItemStack(name, desc.newItemStack(1));

		}

		{
			subId = 4;

			name = "Low Voltage Cable";

			stdCableRender50V = new CableRenderDescriptor("eln",
					"sprites/cable.png", 1.95f, 0.95f);

			desc = new ElectricalCableDescriptor(name, stdCableRender50V,
					"For low voltage with high current.", false);

			lowVoltageCableDescriptor = desc;

			desc.setPhysicalConstantLikeNormalCable(LVU, LVP, 0.2 / 20,// electricalNominalVoltage,
																		// electricalNominalPower,
																		// electricalNominalPowerDrop,
					LVU * 1.3, LVP * 1.2,// electricalMaximalVoltage,
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
					LVU, LVP / 4, 0.2 / 20,// electricalNominalVoltage,
					// electricalNominalPower,
					// electricalNominalPowerDrop,
					LVU * 1.3, LVP * 1.2,// electricalMaximalVoltage,
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

			name = "Medium Voltage Cable";

			stdCableRender200V = new CableRenderDescriptor("eln",
					"sprites/cable.png", 2.95f, 0.95f);

			desc = new ElectricalCableDescriptor(name, stdCableRender200V,
					"miaou", false);

			meduimVoltageCableDescriptor = desc;

			desc.setPhysicalConstantLikeNormalCable(MVU, MVP, 0.10 / 20,// electricalNominalVoltage,
																		// electricalNominalPower,
																		// electricalNominalPowerDrop,
					MVU * 1.3, MVP * 1.2,// electricalMaximalVoltage,
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
			name = "High Voltage Cable";

			stdCableRender800V = new CableRenderDescriptor("eln",
					"sprites/cable.png", 3.95f, 1.95f);

			desc = new ElectricalCableDescriptor(name, stdCableRender800V,
					"miaou2", false);

			highVoltageCableDescriptor = desc;

			desc.setPhysicalConstantLikeNormalCable(HVU, HVP, 0.05 / 20,// electricalNominalVoltage,
																		// electricalNominalPower,
																		// electricalNominalPowerDrop,
					HVU * 1.3, HVP * 1.2,// electricalMaximalVoltage,
											// electricalMaximalPower,
					40,// electricalOverVoltageStartPowerLost,
					cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
					cableHeatingTime, cableThermalConductionTao// thermalNominalHeatTime,
			// thermalConductivityTao
			);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);

		}
	}

	void registerThermalCable(int id) {
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
			sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);

		}

		// subId 0 taken !

		{
			subId = 1;

			name = "Copper Thermal Cable";

			ThermalCableDescriptor desc = new ThermalCableDescriptor(name,
					1000 - 20, -200, // thermalWarmLimit, thermalCoolLimit,
					500, 2000, // thermalStdT, thermalStdPower,
					2, 10, 0.1,// thermalStdDrop, thermalStdLost, thermalTao,
					new CableRenderDescriptor("eln",
							"sprites/tex_thermalcablebase.png", 4, 4),
					"Miaou !");// description

			sixNodeItem.addDescriptor(subId + (id << 6), desc);

			// sixNodeItem.doubleEntry(subId + (id << 6), subId + (0 << 6));
		}
	}

	public FunctionTable batteryVoltageFunctionTable;

	void registerBattery(int id) {
		int subId, completId;
		String name;
		double heatTIme = 30;
		double[] voltageFunctionTable = { 0.000, 0.9, 1.0, 1.025, 1.04, 1.05,
				2.0 };
		FunctionTable voltageFunction = new FunctionTable(voltageFunctionTable,
				6.0 / 5);
		double[] condoVoltageFunctionTable = { 0.000, 0.89, 0.90, 0.905, 0.91, 1.1,
				1.5 };
		FunctionTable condoVoltageFunction = new FunctionTable(condoVoltageFunctionTable,
				6.0 / 5);
		double stdDischargeTime = 4 * 60;
		double stdU = LVU;
		double stdP = LVP / 4;
		double stdHalfLife = Utils.minecraftDay * 2;
		double stdEfficiency = 1.0 - 2.0 / 50.0;
		double condoEfficiency = 1.0 - 2.0 / 50.0;

		batteryVoltageFunctionTable = voltageFunction;
		{
			subId = 0;
			name = "Cost Oriented Battery";

			BatteryDescriptor desc = new BatteryDescriptor(name,
					"BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction, stdU,
					stdP * 1.2, 0.000, // electricalU,
										// electricalPMax,electricalDischargeRate
					stdP, stdDischargeTime, stdEfficiency, stdHalfLife, // electricalStdP,
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
			name = "Capacity Oriented Battery";

			BatteryDescriptor desc = new BatteryDescriptor(name,
					"BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction,
					stdU / 4, stdP / 2 * 1.2, 0.000, // electricalU,
														// electricalPMax,electricalDischargeRate
					stdP / 2, stdDischargeTime * 8, stdEfficiency, stdHalfLife, // electricalStdP,
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
			name = "Voltage Oriented Battery";

			BatteryDescriptor desc = new BatteryDescriptor(name,
					"BatteryBig", meduimVoltageCableDescriptor, 0.5, true, true, voltageFunction, stdU * 4,
					stdP * 1.2, 0.000, // electricalU,
										// electricalPMax,electricalDischargeRate
					stdP, stdDischargeTime, stdEfficiency, stdHalfLife, // electricalStdP,
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
			name = "Current Oriented Battery";

			BatteryDescriptor desc = new BatteryDescriptor(name,
					"BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction, stdU,
					stdP * 1.2 * 4, 0.000, // electricalU,
											// electricalPMax,electricalDischargeRate
					stdP * 4, stdDischargeTime / 6, stdEfficiency, stdHalfLife, // electricalStdP,
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
			name = "Life Oriented Battery";

			BatteryDescriptor desc = new BatteryDescriptor(name,
					"BatteryBig", batteryCableDescriptor, 0.5, true, true, voltageFunction, stdU,
					stdP * 1.2, 0.000, // electricalU,
										// electricalPMax,electricalDischargeRate
					stdP, stdDischargeTime, stdEfficiency, stdHalfLife * 8, // electricalStdP,
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
			name = "Single-use Battery";

			BatteryDescriptor desc = new BatteryDescriptor(name,
					"BatteryBig", batteryCableDescriptor, 1.0, false, false, voltageFunction, stdU,
					stdP * 1.2 * 2, 0.000, // electricalU,
											// electricalPMax,electricalDischargeRate
					stdP * 2, stdDischargeTime, stdEfficiency, stdHalfLife * 8, // electricalStdP,
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
			name = "50V Condensator";

			BatteryDescriptor desc = new BatteryDescriptor(name,
					"condo200", batteryCableDescriptor, 0.0, true, false,
					condoVoltageFunction,
					stdU, stdP * 1.2 * 8, 0.005, // electricalU,//
													// electricalPMax,electricalDischargeRate
					stdP * 8, 4, condoEfficiency, stdHalfLife, // electricalStdP,
					// electricalStdDischargeTime,
					// electricalStdEfficiency,
					// electricalStdHalfLife,
					heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
					// thermalCoolLimit,
					"Obselete, must be deleted" // name, description)
			);
			desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 2.0);
			transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
		}

		{
			subId = 36;
			name = "200V Condensator";

			BatteryDescriptor desc = new BatteryDescriptor(name,
					"condo200", highVoltageCableDescriptor, 0.0, true, false,
					condoVoltageFunction,
					MVU, MVP * 1.5, 0.005, // electricalU,//
											// electricalPMax,electricalDischargeRate
					MVP, 4, condoEfficiency, stdHalfLife, // electricalStdP,
					// electricalStdDischargeTime,
					// electricalStdEfficiency,
					// electricalStdHalfLife,
					heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
					// thermalCoolLimit,
					"the battery" // name, description)
			);
			desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 2.0);
			transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
		}
	}

	void registerGround(int id) {
		int subId, completId;
		String name;

		{
			subId = 0;
			name = "Ground Cable";

			GroundCableDescriptor desc = new GroundCableDescriptor(name, obj.getObj("groundcable"));
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 8;
			name = "Hub";

			HubDescriptor desc = new HubDescriptor(name, obj.getObj("hub"));
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
	}

	void registerElectricalSource(int id) {
		int subId, completId;
		String name;

		{
			subId = 0;
			name = "Electrical Source";

			ElectricalSourceDescriptor desc = new ElectricalSourceDescriptor(
					name, obj.getObj("voltagesource"));
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerLampSocket(int id) {
		int subId, completId;
		String name;

		{
			subId = 0;

			name = "Lamp Socket A";

			LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("ClassicLampSocket"), false),
					LampSocketType.Douille, // LampSocketType
											// socketType
					4, 0, 0, 0);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 1;

			name = "Lamp Socket B Projector";

			LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("ClassicLampSocket"), false),
					LampSocketType.Douille, // LampSocketType
											// socketType
					10, -90, 90, 0);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

		{
			subId = 4;

			name = "Robust Lamp Socket";

			LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("RobustLamp"), true),
					LampSocketType.Douille, // LampSocketType
											// socketType
					0, 0, 0, 0);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 5;

			name = "Flat Lamp Socket";

			LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("FlatLamp"), true),
					LampSocketType.Douille, // LampSocketType
											// socketType
					0, 0, 0, 0);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}		
		{
			subId = 6;

			name = "Simple Lamp Socket";

			LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("SimpleLamp"), true),
					LampSocketType.Douille, // LampSocketType
											// socketType
					0, 0, 0, 0);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

		{
			subId = 8;

			name = "Street Light";

			LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(obj.getObj("StreetLight"), true),
					LampSocketType.Douille, // LampSocketType
											// socketType
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
			subId = 12;

			name = "Suspended Lamp Socket";

			LampSocketDescriptor desc = new LampSocketDescriptor(name,
					new LampSocketSuspendedObjRender(obj.getObj("RobustLampSuspended"), true, 3),
					LampSocketType.Douille, // LampSocketType
											// socketType
					3, 0, 0, 0);
			desc.setPlaceDirection(Direction.YP);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
			desc.cameraOpt = false;
			desc.useIcon(true);
		}

		{
			subId = 13;

			name = "Long Suspended Lamp Socket";

			LampSocketDescriptor desc = new LampSocketDescriptor(name,
					new LampSocketSuspendedObjRender(obj.getObj("RobustLampSuspended"), true, 7),
					LampSocketType.Douille, // LampSocketType
											// socketType
					4, 0, 0, 0);
			desc.setPlaceDirection(Direction.YP);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
			desc.cameraOpt = false;
			desc.useIcon(true);
		}
	}

	void registerLampSupply(int id) {
		int subId, completId;
		String name;

		{
			subId = 0;

			name = "Lamp Supply";

			LampSupplyDescriptor desc = new LampSupplyDescriptor(
					name, obj.getObj("lampsupply"),
					32
					);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerPassiveComponent(int id) {
		int subId, completId;
		String name;
		IFunction function;
		FunctionTableYProtect baseFunction = new FunctionTableYProtect(
				new double[] { 0.0, 0.01, 0.03, 0.1, 0.2, 0.4, 0.8, 1.2 }, 1.0,
				0, 5);

		{
			subId = 0;

			name = "10A Diode";

			function = new FunctionTableYProtect(new double[] { 0.0, 0.1, 0.3,
					1.0, 2.0, 4.0, 8.0, 12.0 }, 1.0, 0, 100);

			DiodeDescriptor desc = new DiodeDescriptor(
					name,// int iconId, String name,
					function,
					10, // double Imax,
					1, 10,
					sixNodeThermalLoadInitializer.copy(),
					lowVoltageCableDescriptor

					);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

		{
			subId = 1;

			name = "25A Diode";

			function = new FunctionTableYProtect(new double[] { 0.0, 0.25,
					0.75, 2.5, 5.0, 10.0, 20.0, 30.0 }, 1.0, 0, 100);

			DiodeDescriptor desc = new DiodeDescriptor(
					name,// int iconId, String name,
					function,
					25, // double Imax,
					1, 25,
					sixNodeThermalLoadInitializer.copy(),
					lowVoltageCableDescriptor);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

		{
			subId = 8;

			name = "Signal Diode";

			function = baseFunction.duplicate(1.0, 0.1);

			DiodeDescriptor desc = new DiodeDescriptor(name,// int iconId,
															// String name,
					function, 0.1, // double Imax,
					1, 0.1,
					sixNodeThermalLoadInitializer.copy(), signalCableDescriptor

					);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

		{
			subId = 16;

			name = "Signal 20H inductor";

			SignalInductorDescriptor desc = new SignalInductorDescriptor(
					name, 20, lowVoltageCableDescriptor
					);

			sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
		}

		{
			subId = 32;

			name = "Power Capacitor";

			PowerCapacitorSixDescriptor desc = new PowerCapacitorSixDescriptor(
					name, obj.getObj("PowerElectricPrimitives"), SerieEE.newE6(-2), 60*2000
					);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

		{
			subId = 34;

			name = "Power Inductor";

			PowerInductorSixDescriptor desc = new PowerInductorSixDescriptor(
					name, obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-1)
					);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerPowerComponent(int id) {
		int subId, completId;
		String name;

		{
			subId = 16;

			name = "Power inductor";

			PowerInductorDescriptor desc = new PowerInductorDescriptor(
					name, null, SerieEE.newE12(-1)
					);

			transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
		}

		{
			subId = 20;

			name = "Power capacitor";

			PowerCapacitorDescriptor desc = new PowerCapacitorDescriptor(
					name, null, SerieEE.newE6(-2), 300
					);

			transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
		}

	}

	void registerSwitch(int id) {
		int subId, completId;
		String name;
		IFunction function;
		ElectricalSwitchDescriptor desc;

		{
			subId = 0;

			name = "High Voltage Switch";

			desc = new ElectricalSwitchDescriptor(name, stdCableRender800V,
					obj.getObj("HighVoltageSwitch"), HVU, HVP, 0.02,// nominalVoltage,
					// nominalPower,
					// nominalDropFactor,
					HVU * 1.5, HVP * 1.2,// maximalVoltage, maximalPower
					cableThermalLoadInitializer.copy(), false);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 1;

			name = "Low Voltage Switch";

			desc = new ElectricalSwitchDescriptor(name, stdCableRender50V,
					obj.getObj("LowVoltageSwitch"), LVU, LVP, 0.02,// nominalVoltage,
					// nominalPower,
					// nominalDropFactor,
					LVU * 1.5, LVP * 1.2,// maximalVoltage, maximalPower
					cableThermalLoadInitializer.copy(), false);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 2;

			name = "Medium Voltage Switch";

			desc = new ElectricalSwitchDescriptor(name, stdCableRender200V,
					obj.getObj("LowVoltageSwitch"), MVU, MVP, 0.02,// nominalVoltage,
					// nominalPower,
					// nominalDropFactor,
					MVU * 1.5, MVP * 1.2,// maximalVoltage, maximalPower
					cableThermalLoadInitializer.copy(), false);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 3;

			name = "Signal Switch";

			desc = new ElectricalSwitchDescriptor(name, stdCableRenderSignal,
					obj.getObj("LowVoltageSwitch"), SVU, SVP, 0.02,// nominalVoltage,
					// nominalPower,
					// nominalDropFactor,
					SVU * 1.5, SVP * 1.2,// maximalVoltage, maximalPower
					cableThermalLoadInitializer.copy(), true);

			sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
		}

		{
			subId = 8;

			name = "Signal Switch with LED";

			desc = new ElectricalSwitchDescriptor(name, stdCableRenderSignal,
					obj.getObj("ledswitch"), SVU, SVP, 0.02,// nominalVoltage,
					// nominalPower,
					// nominalDropFactor,
					SVU * 1.5, SVP * 1.2,// maximalVoltage, maximalPower
					cableThermalLoadInitializer.copy(), true);

			sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
		}

	}

	private void registerSixNodeMisc(int id) {

		int subId, completId;
		String name;
		{
			subId = 0;
			name = "Modbus RTU";

			ModbusRtuDescriptor desc = new ModbusRtuDescriptor(
					name,
					obj.getObj("RTU")

					);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

		{
			subId = 4;
			name = "Watch";

			ElectricalWatchDescriptor desc = new ElectricalWatchDescriptor(
					name,
					obj.getObj("WallClock"),
					20000.0 / (3600 * 40)

					);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

		{
			subId = 8;
			name = "Tutorial sign";

			TutorialSignDescriptor desc = new TutorialSignDescriptor(
					name, obj.getObj("TutoPlate"));
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
	}

	void registerElectricalManager(int id) {
		int subId, completId;
		String name;

		{
			subId = 0;

			name = "Electrical Breaker";

			ElectricalBreakerDescriptor desc = new ElectricalBreakerDescriptor(name, obj.getObj("HighVoltageSwitch"));

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 4;

			name = "Energy Meter";

			EnergyMeterDescriptor desc = new EnergyMeterDescriptor(name, obj.getObj("HighVoltageSwitch"));

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerElectricalSensor(int id) {
		int subId, completId;
		String name;
		ElectricalSensorDescriptor desc;

		{
			subId = 0;

			name = "Electrical Probe";

			desc = new ElectricalSensorDescriptor(name, "electricalsensor",
					false);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 1;

			name = "Voltage Probe";

			desc = new ElectricalSensorDescriptor(name, "voltagesensor", true);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerThermalSensor(int id) {
		int subId, completId;
		String name;
		ThermalSensorDescriptor desc;

		{
			subId = 0;

			name = "Thermal Probe";

			desc = new ThermalSensorDescriptor(name,
					obj.getObj("thermalsensor"), false);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 1;

			name = "Temperature Probe";

			desc = new ThermalSensorDescriptor(name,
					obj.getObj("temperaturesensor"), true);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerElectricalVuMeter(int id) {
		int subId, completId;
		String name;
		ElectricalVuMeterDescriptor desc;
		{
			subId = 0;
			name = "Analog vuMeter";
			desc = new ElectricalVuMeterDescriptor(name, "Vumeter", false);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 8;
			name = "LED vuMeter";
			desc = new ElectricalVuMeterDescriptor(name, "Led", true);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
	}

	void registerElectricalAlarm(int id) {
		int subId, completId;
		String name;
		ElectricalAlarmDescriptor desc;
		{
			subId = 0;
			name = "Nuclear Alarm";
			desc = new ElectricalAlarmDescriptor(name,
					obj.getObj("alarmmedium"), 7, "eln:alarma", 11, 1f);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 1;
			name = "Standard Alarm";
			desc = new ElectricalAlarmDescriptor(name,
					obj.getObj("alarmmedium"), 7, "eln:smallalarm_critical",
					1.2, 2f);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
	}

	void registerElectricalEnvironementalSensor(int id) {
		int subId, completId;
		String name;
		{
			ElectricalLightSensorDescriptor desc;
			{
				subId = 0;
				name = "Electrical Daylight Sensor";
				desc = new ElectricalLightSensorDescriptor(name, obj.getObj("daylightsensor"), true);
				sixNodeItem.addDescriptor(subId + (id << 6), desc);
			}
			{
				subId = 1;
				name = "Electrical Light Sensor";
				desc = new ElectricalLightSensorDescriptor(name, obj.getObj("lightsensor"), false);
				sixNodeItem.addDescriptor(subId + (id << 6), desc);
			}
		}
		{
			ElectricalWeatherSensorDescriptor desc;
			{
				subId = 4;
				name = "Electrical Weather Sensor";
				desc = new ElectricalWeatherSensorDescriptor(name, obj.getObj("electricalweathersensor"));
				sixNodeItem.addDescriptor(subId + (id << 6), desc);
			}
		}
		{
			ElectricalWindSensorDescriptor desc;
			{
				subId = 8;
				name = "Electrical Anemometer Sensor";
				desc = new ElectricalWindSensorDescriptor(name, obj.getObj("Anemometer"), 25);
				sixNodeItem.addDescriptor(subId + (id << 6), desc);
			}
		}
		{
			ElectricalEntitySensorDescriptor desc;
			{
				subId = 12;
				name = "Electrical Entity Sensor";
				desc = new ElectricalEntitySensorDescriptor(name, obj.getObj("ProximitySensor"), 10);
				sixNodeItem.addDescriptor(subId + (id << 6), desc);
			}
		}
	}

	void registerElectricalRedstone(int id) {
		int subId, completId;
		String name;
		{
			ElectricalRedstoneInputDescriptor desc;
			subId = 0;
			name = "Redstone-to-Voltage Converter";
			desc = new ElectricalRedstoneInputDescriptor(name, obj.getObj("redtoele"));
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

		{
			ElectricalRedstoneOutputDescriptor desc;
			subId = 1;
			name = "Voltage-to-Redstone Converter";
			desc = new ElectricalRedstoneOutputDescriptor(name,
					obj.getObj("eletored"));
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
	}

	void registerElectricalGate(int id) {
		int subId, completId;
		String name;
		{
			ElectricalTimeoutDescriptor desc;
			subId = 0;

			name = "Electrical Timer";

			desc = new ElectricalTimeoutDescriptor(name,
					obj.getObj("electricaltimer"));
			desc.setTickSound("eln:timer", 0.01f);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			ElectricalMathDescriptor desc;
			subId = 4;

			name = "Signal Processor";

			desc = new ElectricalMathDescriptor(name,
					obj.getObj("PLC"));
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerWirelessSignal(int id) {
		int subId, completId;
		String name;

		{
			WirelessSignalRxDescriptor desc;
			subId = 0;

			name = "Wireless Signal Receiver";

			desc = new WirelessSignalRxDescriptor(
					name,
					obj.getObj("wirelesssignalrx")

					);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

		{
			WirelessSignalTxDescriptor desc;
			subId = 8;

			name = "Wireless Signal Transmitter";

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

			name = "Wireless Signal Repeater";

			desc = new WirelessSignalRepeaterDescriptor(
					name,
					obj.getObj("wirelesssignalrepeater"),
					wirelessTxRange
					);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerElectricalDataLogger(int id) {
		int subId, completId;
		String name;
		{
			ElectricalDataLoggerDescriptor desc;
			subId = 0;

			name = "Data Logger";

			desc = new ElectricalDataLoggerDescriptor(name, true,
					"DataloggerCRTFloor", 0f, 1f, 0f);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
	}

	void registerElectricalRelay(int id) {
		int subId, completId;
		String name;
		ElectricalRelayDescriptor desc;

		{
			subId = 0;

			name = "Low Voltage Relay";

			desc = new ElectricalRelayDescriptor(
					name, obj.getObj("RelayBig"),
					lowVoltageCableDescriptor);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 1;

			name = "Medium Voltage Relay";

			desc = new ElectricalRelayDescriptor(
					name, obj.getObj("RelayBig"),
					meduimVoltageCableDescriptor);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 2;

			name = "High Voltage Relay";

			desc = new ElectricalRelayDescriptor(
					name, obj.getObj("relay800"),
					highVoltageCableDescriptor);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerElectricalGateSource(int id) {
		int subId, completId;
		String name;

		ElectricalGateSourceRenderObj signalsourcepot = new ElectricalGateSourceRenderObj(obj.getObj("signalsourcepot"));
		ElectricalGateSourceRenderObj ledswitch = new ElectricalGateSourceRenderObj(obj.getObj("ledswitch"));

		{
			subId = 0;

			name = "Signal Trimmer";

			ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, signalsourcepot, false);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 1;

			name = "Signal Switch";

			ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, ledswitch, true);

			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 8;

			name = "Signal Button";

			ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, ledswitch, true);
			desc.setWithAutoReset();
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 12;

			name = "Wireless Button";

			WirelessSignalSourceDescriptor desc = new WirelessSignalSourceDescriptor(name, ledswitch, wirelessTxRange, true);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 16;

			name = "Wireless Switch";

			WirelessSignalSourceDescriptor desc = new WirelessSignalSourceDescriptor(name, ledswitch, wirelessTxRange, false);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerTransformer(int id) {
		int subId, completId;
		String name;

		{
			subId = 0;
			name = "Transformer";

			TransformerDescriptor desc = new TransformerDescriptor(name, obj.getObj("transformator"), obj.getObj("feromagneticcorea"), 0.5f);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerHeatFurnace(int id) {
		int subId, completId;
		String name;
		{
			subId = 0;
			name = "Stone Heat Furnace";

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

	}

	void registerTurbine(int id) {
		int subId, completId;
		String name;

		FunctionTable TtoU = new FunctionTable(new double[] { 0, 0.1, 0.85,
				1.0, 1.1, 1.15, 1.18, 1.19, 1.25 }, 8.0 / 5.0);
		FunctionTable PoutToPin = new FunctionTable(new double[] { 0.0, 0.2,
				0.4, 0.6, 0.8, 1.0, 1.3, 1.8, 2.7 }, 8.0 / 5.0);

		{
			subId = 1;
			name = "50V Turbine";
			double RsFactor = 0.1;
			double nominalU = LVU;
			double nominalP = 300;
			double nominalDeltaT = 250;
			TurbineDescriptor desc = new TurbineDescriptor(
					name,
					"turbineb",
					"Miaouuuu turbine",// int iconId, String name,String
										// description,
					lowVoltageCableDescriptor.render,
					TtoU.duplicate(nominalDeltaT, nominalU),
					PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
					nominalU,
					nominalP,
					nominalP / 40,// double nominalDeltaT, double
									// nominalU,nominalP,double nominalPowerLost
					nominalU * 1.3,
					lowVoltageCableDescriptor.electricalRs * RsFactor,
					lowVoltageCableDescriptor.electricalRp,
					lowVoltageCableDescriptor.electricalC / RsFactor,// ElectricalCableDescriptor
																		// electricalCable,
					25.0, nominalDeltaT / 40, // double thermalC,double
												// DeltaTForInput
					nominalP / (nominalU / 25),
					new SoundCommand("eln:heat_turbine_50v", 2).mulVolume(1.5).mulVolume(2));
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
		}

		{
			subId = 8;
			name = "200V Turbine";
			double RsFactor = 0.10;
			double nominalU = MVU;
			double nominalP = 500;
			double nominalDeltaT = 350;
			TurbineDescriptor desc = new TurbineDescriptor(
					name,
					"turbinebblue",
					"Miaouuuu turbine",// int iconId, String name,String
										// description,
					meduimVoltageCableDescriptor.render,
					TtoU.duplicate(nominalDeltaT, nominalU),
					PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
					nominalU,
					nominalP,
					nominalP / 40,// double nominalDeltaT, double
									// nominalU,nominalP,double nominalPowerLost
					nominalU * 1.3,
					meduimVoltageCableDescriptor.electricalRs * RsFactor,
					meduimVoltageCableDescriptor.electricalRp,
					meduimVoltageCableDescriptor.electricalC / RsFactor,// ElectricalCableDescriptor
																		// electricalCable,
					50.0, nominalDeltaT / 40, // double thermalC,double
												// DeltaTForInput
					nominalP / (nominalU / 25),
					new SoundCommand("eln:heat_turbine_200v", 2).mulVolume(2));
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	/*
	 * void registerIntelligentTransformer(int id) { int subId, completId; String name; { subId = 0; name = "Intelligent Transformer";
	 * 
	 * TransparentNodeDescriptor desc = new TransparentNodeDescriptor( name, IntelligentTransformerElement.class, IntelligentTransformerRender.class); transparentNodeItem.addDescriptor(subId + (id << 6), desc); }
	 * 
	 * }
	 */
	public ArrayList<ItemStack> furnaceList = new ArrayList<ItemStack>();

	void registerElectricalFurnace(int id) {
		int subId, completId;
		String name;
		furnaceList.add(new ItemStack(Blocks.furnace));
		{
			subId = 0;
			name = "Electrical Furnace";
			double[] PfTTable = new double[] { 0, 20, 40, 80, 160, 240, 360,
					540, 756, 1058.4, 1481.76 };

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
		// Utils.smeltRecipeList.addMachine(new ItemStack(Blocks.furnace));
	}

	public ElectricalFurnaceDescriptor electricalFurnace;
	public RecipesList maceratorRecipes = new RecipesList();

	void registerMacerator(int id) {
		int subId, completId;
		String name;
		{
			subId = 0;
			name = "50V Macerator";

			MaceratorDescriptor desc = new MaceratorDescriptor(name,
					"maceratora", LVU, 200,// double nominalU,double nominalP,
					LVU * 1.25,// double maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
					maceratorRecipes);

			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
			desc.setRuningSound(new SoundCommand("eln:macerator", 1.5).mulVolume(0.3));
		}

		{
			subId = 4;
			name = "200V Macerator";

			MaceratorDescriptor desc = new MaceratorDescriptor(name,
					"maceratorb", MVU, 400,// double nominalU,double nominalP,
					MVU * 1.25,// double maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					meduimVoltageCableDescriptor,// ElectricalCableDescriptor
													// cable
					maceratorRecipes);

			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
			desc.setRuningSound(new SoundCommand("eln:macerator", 1.5).mulVolume(0.3));
		}
	}

	public RecipesList compressorRecipes = new RecipesList();
	public RecipesList plateMachineRecipes = new RecipesList();

	void registerPlateMachine(int id) {

		int subId, completId;
		String name;
		{
			subId = 0;
			name = "50V Plate Machine";

			PlateMachineDescriptor desc = new PlateMachineDescriptor(
					name,// String name,
					obj.getObj("platemachinea"),
					LVU, 200,// double nominalU,double nominalP,
					LVU * 1.25,// double maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
					plateMachineRecipes);

			transparentNodeItem.addDescriptor(subId + (id << 6), desc);

			desc.setEndSound(new SoundCommand("eln:plate_machine"));
		}

		{
			subId = 4;
			name = "200V Plate Machine";

			PlateMachineDescriptor desc = new PlateMachineDescriptor(
					name,// String name,
					obj.getObj("platemachineb"),
					MVU, 400,// double nominalU,double nominalP,
					MVU * 1.25,// double maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					meduimVoltageCableDescriptor,// ElectricalCableDescriptor
													// cable
					plateMachineRecipes);

			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
			desc.setEndSound(new SoundCommand("eln:plate_machine"));
		}
	}

	void registerEggIncubator(int id) {

		int subId, completId;
		String name;
		{
			subId = 0;
			name = "50V Egg Incubator";

			EggIncubatorDescriptor desc = new EggIncubatorDescriptor(
					name, obj.getObj("eggincubator"),
					lowVoltageCableDescriptor,
					LVU, 50);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
		}

	}

	void registerCompressor(int id) {

		int subId, completId;
		String name;
		{
			subId = 0;
			name = "50V Compressor";

			CompressorDescriptor desc = new CompressorDescriptor(
					name,// String name,
					obj.getObj("compressora"),
					LVU, 200,// double nominalU,double nominalP,
					LVU * 1.25,// double maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
					compressorRecipes);

			transparentNodeItem.addDescriptor(subId + (id << 6), desc);

			desc.setRuningSound(new SoundCommand("eln:compressor", 1.6).mulVolume(0.3));
		}

		{
			subId = 4;
			name = "200V Compressor";

			CompressorDescriptor desc = new CompressorDescriptor(
					name,// String name,
					obj.getObj("compressorb"),
					MVU, 400,// double nominalU,double nominalP,
					MVU * 1.25,// double maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					meduimVoltageCableDescriptor,// ElectricalCableDescriptor
													// cable
					compressorRecipes);

			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
			desc.setRuningSound(new SoundCommand("eln:compressor", 1.6).mulVolume(0.3));
		}
	}

	public RecipesList magnetiserRecipes = new RecipesList();

	void registermagnetiser(int id) {

		int subId, completId;
		String name;
		{
			subId = 0;
			name = "50V Magnetizer";

			MagnetizerDescriptor desc = new MagnetizerDescriptor(
					name,// String name,
					obj.getObj("magnetizera"),
					LVU, 200,// double nominalU,double nominalP,
					LVU * 1.25,// double maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
					magnetiserRecipes);

			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
			
			desc.setRuningSound(new SoundCommand("eln:Motor", 1.6).mulVolume(0.3));
		}

		{
			subId = 4;
			name = "200V Magnetizer";

			MagnetizerDescriptor desc = new MagnetizerDescriptor(
					name,// String name,
					obj.getObj("magnetizerb"),
					MVU, 400,// double nominalU,double nominalP,
					MVU * 1.25,// double maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					meduimVoltageCableDescriptor,// ElectricalCableDescriptor
													// cable
					magnetiserRecipes);

			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
			
			desc.setRuningSound(new SoundCommand("eln:Motor", 1.6).mulVolume(0.3));
		}
	}

	void registerSolarPannel(int id) {
		int subId, completId;
		GhostGroup ghostGroup;
		String name;

		FunctionTable diodeIfUBase;
		diodeIfUBase = new FunctionTableYProtect(new double[] { 0.0, 0.002,
				0.005, 0.01, 0.015, 0.02, 0.025, 0.03, 0.035, 0.04, 0.045,
				0.05, 0.06, 0.07, 0.08, 0.09, 0.10, 0.11, 0.12, 0.13, 1.0 },
				1.0, 0, 1.0);

		FunctionTable solarIfSBase;
		solarIfSBase = new FunctionTable(new double[] { 0.0, 0.1, 0.4, 0.6,
				0.8, 1.0 }, 1);

		double LVSolarU = 59;

		{
			subId = 1;
			name = "Small Solar Panel";

			ghostGroup = new GhostGroup();

			SolarPannelDescriptor desc = new SolarPannelDescriptor(name,// String
																		// name,
					obj.getObj("smallsolarpannel"), null,
					ghostGroup, 0, 1, 0,// GhostGroup ghostGroup, int
										// solarOffsetX,int solarOffsetY,int
										// solarOffsetZ,
					// FunctionTable solarIfSBase,
					LVSolarU / 4, 65.0,// double electricalUmax,double
										// electricalPmax,
					0.01,// ,double electricalDropFactor
					Math.PI / 2, Math.PI / 2 // alphaMin alphaMax
			);

			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
		}
		{
			subId = 2;
			name = "Small Rotating Solar Panel";

			ghostGroup = new GhostGroup();

			SolarPannelDescriptor desc = new SolarPannelDescriptor(name,// String
																		// name,
					obj.getObj("smallsolarpannelrot"), lowVoltageCableDescriptor.render,
					ghostGroup, 0, 1, 0,// GhostGroup ghostGroup, int
										// solarOffsetX,int solarOffsetY,int
										// solarOffsetZ,
					// FunctionTable solarIfSBase,
					LVSolarU / 4, 65.0,// double electricalUmax,double
										// electricalPmax,
					0.01,// ,double electricalDropFactor
					Math.PI / 4, Math.PI / 4 * 3 // alphaMin alphaMax
			);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
		}
	}

	void registerHeatingCorp(int id) {
		int subId, completId;
		String name;

		HeatingCorpElement element;
		{
			subId = 0;
			completId = subId + (id << 6);
			element = new HeatingCorpElement("Small 50V Copper Heating Corp",// iconId,
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
			element = new HeatingCorpElement("50V Copper Heating Corp",// iconId,
																		// name,
					LVU, 250,// electricalNominalU, electricalNominalP,
					320,// electricalMaximalP)
					lowVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 2;
			completId = subId + (id << 6);
			element = new HeatingCorpElement("Small 200V Copper Heating Corp",// iconId,
																				// name,
					MVU, 400,// electricalNominalU, electricalNominalP,
					500,// electricalMaximalP)
					meduimVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 3;
			completId = subId + (id << 6);
			element = new HeatingCorpElement("200V Copper Heating Corp",// iconId,
																		// name,
					MVU, 600,// electricalNominalU, electricalNominalP,
					750,// electricalMaximalP)
					highVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 4;
			completId = subId + (id << 6);
			element = new HeatingCorpElement("Small 50V Iron Heating Corp",// iconId,
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
			element = new HeatingCorpElement("50V Iron Heating Corp",// iconId,
																		// name,
					LVU, 375,// electricalNominalU, electricalNominalP,
					480,// electricalMaximalP)
					lowVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 6;
			completId = subId + (id << 6);
			element = new HeatingCorpElement("Small 200V Iron Heating Corp",// iconId,
																			// name,
					MVU, 600,// electricalNominalU, electricalNominalP,
					750,// electricalMaximalP)
					meduimVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 7;
			completId = subId + (id << 6);
			element = new HeatingCorpElement("200V Iron Heating Corp",// iconId,
																		// name,
					MVU, 900,// electricalNominalU, electricalNominalP,
					1050,// electricalMaximalP)
					highVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 8;
			completId = subId + (id << 6);
			element = new HeatingCorpElement("Small 50V Tungsten Heating Corp",// iconId,
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
			element = new HeatingCorpElement("50V Tungsten Heating Corp",// iconId,
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
					"Small 200V Tungsten Heating Corp",// iconId, name,
					MVU, 800,// electricalNominalU, electricalNominalP,
					1000,// electricalMaximalP)
					meduimVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 11;
			completId = subId + (id << 6);
			element = new HeatingCorpElement("200V Tungsten Heating Corp",// iconId,
																			// name,
					MVU, 1200,// electricalNominalU, electricalNominalP,
					1500,// electricalMaximalP)
					highVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}

	}

	void registerThermalIsolator(int id) {
		int subId, completId;
		String name;

	}

	void registerRegulatorItem(int id) {
		int subId, completId;
		String name;
		IRegulatorDescriptor element;
		{
			subId = 0;
			completId = subId + (id << 6);
			element = new RegulatorOnOffDescriptor("On/OFF Regulator 1%",
					"onoffregulator", 0.01);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 1;
			completId = subId + (id << 6);
			element = new RegulatorOnOffDescriptor("On/OFF Regulator 10%",
					"onoffregulator", 0.1);
			sharedItem.addElement(completId, element);
		}

		{
			subId = 8;
			completId = subId + (id << 6);
			element = new RegulatorAnalogDescriptor("Analogic Regulator",
					"Analogicregulator");
			sharedItem.addElement(completId, element);
		}

	}

	double incondecentLampLife;
	double economicLampLife;

	void registerLampItem(int id) {
		int subId, completId;
		String name;
		double[] lightPower = new double[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				15, 20, 25, 30, 40 };
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
			element = new LampDescriptor("Small 50V Incandescent Light Bulb",
					"incandescentlampiron", LampDescriptor.Type.Incandescent,
					LampSocketType.Douille, LVU, lightPower[12], // nominalU,
																	// nominalP
					lightLevel[12], incondecentLampLife, standardGrowRate // nominalLight,
																			// nominalLife
			);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 1;
			completId = subId + (id << 6);
			element = new LampDescriptor("50V Incandescent Light Bulb",
					"incandescentlampiron", LampDescriptor.Type.Incandescent,
					LampSocketType.Douille, LVU, lightPower[14], // nominalU,
																	// nominalP
					lightLevel[14], incondecentLampLife, standardGrowRate // nominalLight,
																			// nominalLife
			);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 2;
			completId = subId + (id << 6);
			element = new LampDescriptor("200V Incandescent Light Bulb",
					"incandescentlampiron", LampDescriptor.Type.Incandescent,
					LampSocketType.Douille, MVU, lightPower[14], // nominalU,
																	// nominalP
					lightLevel[14], incondecentLampLife, standardGrowRate // nominalLight,
																			// nominalLife
			);
			sharedItem.addElement(completId, element);
		}

		{
			subId = 4;
			completId = subId + (id << 6);
			element = new LampDescriptor(
					"Small 50V Carbon Incandescent Light Bulb",
					"incandescentlampcarbon", LampDescriptor.Type.Incandescent,
					LampSocketType.Douille, LVU, lightPower[11], // nominalU,
																	// nominalP
					lightLevel[11], incondecentLampLife / 3, standardGrowRate // nominalLight,
			// nominalLife
			);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 5;
			completId = subId + (id << 6);
			element = new LampDescriptor("50V Carbon Incandescent Light Bulb",
					"incandescentlampcarbon", LampDescriptor.Type.Incandescent,
					LampSocketType.Douille, LVU, lightPower[13], // nominalU,
																	// nominalP
					lightLevel[13], incondecentLampLife / 3, standardGrowRate // nominalLight,
			// nominalLife
			);
			sharedItem.addElement(completId, element);
		}

		{
			subId = 16;
			completId = subId + (id << 6);
			element = new LampDescriptor("Small 50V Economic Light Bulb",
					"economiclamp", LampDescriptor.Type.eco,
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
			element = new LampDescriptor("50V Economic Light Bulb",
					"economiclamp", LampDescriptor.Type.eco,
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
			element = new LampDescriptor("200V Economic Light Bulb",
					"economiclamp", LampDescriptor.Type.eco,
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
			element = new LampDescriptor("50V Farming Lamp",
					"incandescentlampiron", LampDescriptor.Type.Incandescent,
					LampSocketType.Douille, LVU, 120, // nominalU, nominalP
					lightLevel[15], incondecentLampLife, 0.50 // nominalLight,
																// nominalLife
			);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 36;
			completId = subId + (id << 6);
			element = new LampDescriptor("200V Farming Lamp",
					"incandescentlampiron", LampDescriptor.Type.Incandescent,
					LampSocketType.Douille, MVU, 120, // nominalU, nominalP
					lightLevel[15], incondecentLampLife, 0.50 // nominalLight,
																// nominalLife
			);
			sharedItem.addElement(completId, element);
		}

	}

	void registerProtection(int id) {
		int subId, completId;
		String name;

		{
			OverHeatingProtectionDescriptor element;
			subId = 0;
			completId = subId + (id << 6);
			element = new OverHeatingProtectionDescriptor(
					"Overheating Protection");
			sharedItem.addElement(completId, element);
		}
		{
			OverVoltageProtectionDescriptor element;
			subId = 1;
			completId = subId + (id << 6);
			element = new OverVoltageProtectionDescriptor(
					"Overvoltage Protection");
			sharedItem.addElement(completId, element);
		}

	}

	void registerCombustionChamber(int id) {
		int subId, completId;
		String name;
		{
			CombustionChamber element;
			subId = 0;
			completId = subId + (id << 6);
			element = new CombustionChamber("Combustion Chamber");
			sharedItem.addElement(completId, element);
		}

	}

	void registerFerromagneticCore(int id) {
		int subId, completId;
		String name;

		FerromagneticCoreDescriptor element;
		{
			subId = 0;
			completId = subId + (id << 6);
			element = new FerromagneticCoreDescriptor(
					"Cheap Ferromagnetic Core", obj.getObj("feromagneticcorea"),// iconId,
																				// name,
					10);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 1;
			completId = subId + (id << 6);
			element = new FerromagneticCoreDescriptor(
					"Average Ferromagnetic Core", obj.getObj("feromagneticcorea"),// iconId,
																					// name,
					4);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 2;
			completId = subId + (id << 6);
			element = new FerromagneticCoreDescriptor(
					"Optimal Ferromagnetic Core", obj.getObj("feromagneticcorea"),// iconId,
																					// name,
					1);
			sharedItem.addElement(completId, element);
		}
	}

	public static OreDescriptor oreTin, oreCopper, oreSilver;

	void registerOre() {
		int id;
		String name;

		{
			id = 1;

			name = "Copper Ore";

			OreDescriptor desc = new OreDescriptor(name, id, // int itemIconId,
																// String
																// name,int
																// metadata,
					30 * (genCooper ? 1 : 0), 6, 10, 0, 80 // int spawnRate,int
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

			name = "Lead Ore";

			OreDescriptor desc = new OreDescriptor(name, id, // int itemIconId,
																// String
																// name,int
																// metadata,
					8 * (genPlumb ? 1 : 0), 3, 9, 0, 24 // int spawnRate,int
														// spawnSizeMin,int
			// spawnSizeMax,int spawnHeightMin,int
			// spawnHeightMax
			);
			oreItem.addDescriptor(id, desc);
			addToOre("oreLead", desc.newItemStack());
		}
		{
			id = 5;

			name = "Tungsten Ore";

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
			addToOre(dicTungstenOre, desc.newItemStack());
		}
		{
			id = 6;

			name = "Cinnabar Ore";

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

	public static GenericItemUsingDamageDescriptorWithComment dustTin,
			dustCopper, dustSilver;

	HashMap<String, ItemStack> dictionnaryOreFromMod = new HashMap<String, ItemStack>();

	void addToOre(String name, ItemStack ore) {
		OreDictionary.registerOre(name, ore);
		dictionnaryOreFromMod.put(name, ore);
	}

	void registerDust(int id) {
		int subId, completId;
		String name;
		GenericItemUsingDamageDescriptorWithComment element;

		{
			subId = 1;
			completId = subId + (id << 6);

			name = "Copper Dust";
			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			dustCopper = element;
			sharedItem.addElement(completId, element);
			Data.addResource(element.newItemStack());
			addToOre("dustCopper", element.newItemStack());
		}
		{
			subId = 2;
			completId = subId + (id << 6);

			name = "Iron Dust";
			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			dustCopper = element;
			sharedItem.addElement(completId, element);
			Data.addResource(element.newItemStack());
			addToOre("dustIron", element.newItemStack());
		}

		{
			id = 5;

			name = "Lead Dust";

			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			sharedItem.addElement(id, element);
			Data.addResource(element.newItemStack());
			addToOre("dustLead", element.newItemStack());
		}
		{
			id = 6;

			name = "Tungsten Dust";

			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			sharedItem.addElement(id, element);
			Data.addResource(element.newItemStack());
			addToOre(dicTungstenDust, element.newItemStack());
		}

		{
			id = 7;

			name = "Gold Dust";

			element = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(id, element);
			Data.addResource(element.newItemStack());
			addToOre("dustGold", element.newItemStack());
		}

		{
			id = 8;

			name = "Coal Dust";

			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			sharedItem.addElement(id, element);
			Data.addResource(element.newItemStack());
			addToOre("dustCoal", element.newItemStack());
		}
		{
			id = 9;

			name = "Alloy Dust";

			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			sharedItem.addElement(id, element);
			Data.addResource(element.newItemStack());
			addToOre("dustAlloy", element.newItemStack());
		}

		{
			id = 10;

			name = "Cinnabar Dust";

			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			sharedItem.addElement(id, element);
			Data.addResource(element.newItemStack());
			addToOre("dustCinnabar", element.newItemStack());
		}

	}

	GenericItemUsingDamageDescriptorWithComment tinIngot, copperIngot,
			silverIngot, plumbIngot, tungstenIngot;

	void registerIngot(int id) {
		int subId, completId;
		String name;

		GenericItemUsingDamageDescriptorWithComment element;

		{
			subId = 1;
			completId = subId + (id << 6);

			name = "Copper Ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			sharedItem.addElement(completId, element);
			// GameRegistry.registerCustomItemStack(name,
			// element.newItemStack(1));
			copperIngot = element;
			Data.addResource(element.newItemStack());
			addToOre("ingotCopper", element.newItemStack());
		}

		{
			subId = 4;
			completId = subId + (id << 6);

			name = "Lead Ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			sharedItem.addElement(completId, element);
			// GameRegistry.registerCustomItemStack(name,
			// element.newItemStack(1));
			plumbIngot = element;
			Data.addResource(element.newItemStack());
			addToOre("ingotLead", element.newItemStack());

		}

		{
			subId = 5;
			completId = subId + (id << 6);

			name = "Tungsten Ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			sharedItem.addElement(completId, element);
			// GameRegistry.registerCustomItemStack(name,
			// element.newItemStack(1));
			tungstenIngot = element;
			Data.addResource(element.newItemStack());
			addToOre(dicTungstenIngot, element.newItemStack());
		}

		{
			subId = 6;
			completId = subId + (id << 6);

			name = "Ferrite Ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] { "useless", "Realy useless" });
			sharedItem.addElement(completId, element);
			// GameRegistry.registerCustomItemStack(name,
			// element.newItemStack(1));

			Data.addResource(element.newItemStack());
			addToOre("ingotFerrite", element.newItemStack());
		}

		{
			subId = 7;
			completId = subId + (id << 6);

			name = "Alloy Ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			sharedItem.addElement(completId, element);
			// GameRegistry.registerCustomItemStack(name,
			// element.newItemStack(1));

			Data.addResource(element.newItemStack());
			addToOre("ingotAlloy", element.newItemStack());
		}

		{
			subId = 8;
			completId = subId + (id << 6);

			name = "Mercury";
			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] { "useless", "miaou" });
			sharedItem.addElement(completId, element);
			// GameRegistry.registerCustomItemStack(name,
			// element.newItemStack(1));

			Data.addResource(element.newItemStack());
			addToOre("quicksilver", element.newItemStack());
		}
	}

	void registerElectricalMotor(int id) {

		int subId, completId;
		String name;
		GenericItemUsingDamageDescriptorWithComment element;

		{
			subId = 0;
			completId = subId + (id << 6);

			name = "Electrical Motor";
			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			sharedItem.addElement(completId, element);
			// GameRegistry.registerCustomItemStack(name,
			// element.newItemStack(1));

			Data.addResource(element.newItemStack());

		}
		{
			subId = 1;
			completId = subId + (id << 6);

			name = "Advanced Electrical Motor";
			element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
																			// name,
					new String[] {});
			sharedItem.addElement(completId, element);
			// GameRegistry.registerCustomItemStack(name,
			// element.newItemStack(1));
			Data.addResource(element.newItemStack());

		}

	}

	private void registerArmor() {
		ItemStack stack;
		String name;

		{
			name = "Copper Helmet";
			helmetCopper = (ItemArmor) (new genericArmorItem(ArmorMaterial.IRON, 2, ArmourType.Helmet, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_helmet").setCreativeTab(creativeTab);
			GameRegistry.registerItem(helmetCopper, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(helmetCopper));
			LanguageRegistry.addName(helmetCopper, name);
		}
		{
			name = "Copper Chestplate";
			plateCopper = (ItemArmor) (new genericArmorItem(ArmorMaterial.IRON, 2, ArmourType.Chestplate, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_chestplate").setCreativeTab(creativeTab);
			GameRegistry.registerItem(plateCopper, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(plateCopper));
			LanguageRegistry.addName(plateCopper, name);
		}
		{
			name = "Copper Leggings";
			legsCopper = (ItemArmor) (new genericArmorItem(ArmorMaterial.IRON, 2, ArmourType.Leggings, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_leggings").setCreativeTab(creativeTab);
			GameRegistry.registerItem(legsCopper, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(legsCopper));
			LanguageRegistry.addName(legsCopper, name);
		}
		{
			name = "Copper Boots";
			bootsCopper = (ItemArmor) (new genericArmorItem(ArmorMaterial.IRON, 2, ArmourType.Boots, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_boots").setCreativeTab(creativeTab);
			GameRegistry.registerItem(bootsCopper, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(bootsCopper));
			LanguageRegistry.addName(bootsCopper, name);
		}

		int armorPoint;
		String t1, t2;
		t1 = "eln:textures/armor/ecoal_layer_1.png";
		t2 = "eln:textures/armor/ecoal_layer_2.png";
		double energyPerDamage = 500;
		int armor, armorMarge;
		ArmorMaterial eCoalMaterial = net.minecraftforge.common.util.EnumHelper.addArmorMaterial("ECoal", 10, new int[] { 2, 6, 5, 2 }, 9);
		{
			name = "E-Coal Helmet";
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
			)).setUnlocalizedName(name).setTextureName("eln:ecoal_helmet").setCreativeTab(creativeTab);
			GameRegistry.registerItem(helmetECoal, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(helmetECoal));
			LanguageRegistry.addName(helmetECoal, name);
		}
		{
			name = "E-Coal Chestplate";
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
			)).setUnlocalizedName(name).setTextureName("eln:ecoal_chestplate").setCreativeTab(creativeTab);
			GameRegistry.registerItem(plateECoal, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(plateECoal));
			LanguageRegistry.addName(plateECoal, name);
		}
		{
			name = "E-Coal Leggings";
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
			)).setUnlocalizedName(name).setTextureName("eln:ecoal_leggings").setCreativeTab(creativeTab);
			GameRegistry.registerItem(legsECoal, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(legsECoal));
			LanguageRegistry.addName(legsECoal, name);
		}
		{
			name = "E-Coal Boots";
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
			)).setUnlocalizedName(name).setTextureName("eln:ecoal_boots").setCreativeTab(creativeTab);
			GameRegistry.registerItem(bootsECoal, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(bootsECoal));
			LanguageRegistry.addName(bootsECoal, name);
		}
	}

	private void registerTool() {
		ItemStack stack;
		String name;
		{
			name = "Copper Sword";
			swordCopper = (new ItemSword(ToolMaterial.IRON)).setUnlocalizedName(name).setTextureName("eln:copper_sword").setCreativeTab(creativeTab);
			GameRegistry.registerItem(swordCopper, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(swordCopper));
			LanguageRegistry.addName(swordCopper, name);
		}
		{
			name = "Copper Hoe";
			hoeCopper = (new ItemHoe(ToolMaterial.IRON)).setUnlocalizedName(name).setTextureName("eln:copper_hoe").setCreativeTab(creativeTab);
			GameRegistry.registerItem(hoeCopper, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(hoeCopper));
			LanguageRegistry.addName(hoeCopper, name);
		}
		{
			name = "Copper Shovel";
			shovelCopper = (new ItemSpade(ToolMaterial.IRON)).setUnlocalizedName(name).setTextureName("eln:copper_shovel").setCreativeTab(creativeTab);
			GameRegistry.registerItem(shovelCopper, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(shovelCopper));
			LanguageRegistry.addName(shovelCopper, name);
		}
		{
			name = "Copper Pickaxe";
			pickaxeCopper = new ItemPickaxeEln(ToolMaterial.IRON).setUnlocalizedName(name).setTextureName("eln:copper_pickaxe").setCreativeTab(creativeTab);
			GameRegistry.registerItem(pickaxeCopper, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(pickaxeCopper));
			LanguageRegistry.addName(pickaxeCopper, name);
		}
		{
			name = "Copper Axe";
			axeCopper = new ItemAxeEln(ToolMaterial.IRON).setUnlocalizedName(name).setTextureName("eln:copper_axe").setCreativeTab(creativeTab);
			GameRegistry.registerItem(axeCopper, "Eln." + name);
			GameRegistry.registerCustomItemStack(name, new ItemStack(axeCopper));
			LanguageRegistry.addName(axeCopper, name);
		}

	}

	// public static int swordCopperId,hoeCopperId,shovelCopperId,pickaxeCopperId,axeCopperId;
	// public static Item swordCopper,hoeCopper,shovelCopper,pickaxeCopper,axeCopper;

	void registerSolarTracker(int id) {
		int subId, completId;
		String name;

		SolarTrackerDescriptor element;
		{
			subId = 0;
			completId = subId + (id << 6);
			element = new SolarTrackerDescriptor("Solar Tracker"// iconId, name,

			);
			sharedItem.addElement(completId, element);
		}

	}

	void registerWindTurbine(int id) {
		int subId, completId;
		String name;

		FunctionTable PfW = new FunctionTable(
				new double[] { 0.0, 0.1, 0.3, 0.5, 0.8, 1.0, 1.1, 1.15, 1.2 },
				8.0 / 5.0);
		{
			subId = 0;
			name = "Wind Turbine";

			WindTurbineDescriptor desc = new WindTurbineDescriptor(
					name, obj.getObj("WindTurbineMini"), // name,Obj3D obj,
					lowVoltageCableDescriptor,// ElectricalCableDescriptor
												// cable,
					PfW,// PfW
					160, 10,// double nominalPower,double nominalWind,
					LVU * 1.18, 22,// double maxVoltage, double maxWind,
					3,// int offY,
					7, 2, 2,// int rayX,int rayY,int rayZ,
					2, 0.07,// int blockMalusMinCount,double blockMalus
					"eln:WINDTURBINE_BIG_SF", 2f // Use the wind turbine sound and play at normal volume (1 => 100%)
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
			name = "Water Turbine";

			Coordonate waterCoord = new Coordonate(1, -1, 0, 0);

			WaterTurbineDescriptor desc = new WaterTurbineDescriptor(
					name, obj.getObj("SmallWaterWheel"), // name,Obj3D obj,
					lowVoltageCableDescriptor,// ElectricalCableDescriptor
					30,
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

	void registerThermalDissipatorPassiveAndActive(int id) {
		int subId, completId;
		String name;
		{
			subId = 0;
			name = "Small Passive Thermal Dissipator";

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
			name = "Small Active Thermal Dissipator";

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
			name = "200V Active Thermal Dissipator";

			ThermalDissipatorActiveDescriptor desc = new ThermalDissipatorActiveDescriptor(
					name,
					obj.getObj("200vactivethermaldissipatora"),
					MVU, 60,// double nominalElectricalU,double
							// electricalNominalP,
					1200,// double nominalElectricalCoolingPower,
					lowVoltageCableDescriptor,// ElectricalCableDescriptor
												// cableDescriptor,
					130, -100,// double warmLimit,double coolLimit,
					200, 30,// double nominalP,double nominalT,
					10, 1// double nominalTao,double nominalConnectionDrop

			);

			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
		}
	}

	void registerTransparentNodeMisc(int id) {
		int subId, completId;
		String name;
		{
			subId = 0;
			name = "Experimental Transporter";

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

	void registerTurret(int id) {
		{
			int subId = 0;
			String name = "200V Defence turret";

			TurretDescriptor desc = new TurretDescriptor(name, "Turret", "Defence turret WIP");

			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
		}
	}
	
	/*
	 * void registerMppt(int id) { int subId, completId; String name; MpptDescriptor desc;
	 * 
	 * FunctionTable PoutfPin = new FunctionTable(new double[] { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.08, 1.15, 1.21, 1.26, 1.29 }, 1.5);
	 * 
	 * { subId = 0; name = "Basic Maximum Power Point Tracker";
	 * 
	 * desc = new MpptDescriptor(name, -1, LVU * 1.3,// double // inUmin,double // inUmax, 10, LVU * 1.19,// double outUmin,double outUmax,
	 * 
	 * 500,// double designedPout, PoutfPin,// FunctionTable PoutfPin, 0.01,// electricalLoadDropFactor
	 * 
	 * 6.0, 0.1,// double inResistorLowHighTime,double // inResistorNormalTime, 0.05,// double inResistorStepFactor, 1.0, 50.0// double inResistorMin,double inResistorMax
	 * 
	 * );
	 * 
	 * transparentNodeItem.addDescriptor(subId + (id << 6), desc); } }
	 */
	void registerElectricalAntenna(int id) {
		int subId, completId;
		String name;
		{

			subId = 0;
			ElectricalAntennaTxDescriptor desc;
			name = "Low Power Transmitter Antenna";
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
			name = "Low Power Receiver Antenna";
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
			name = "Medium Power Transmitter Antenna";
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
			name = "Medium Power Receiver Antenna";
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
			name = "High Power Transmitter Antenna";
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
			name = "High Power Receiver Antenna";
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

	static public GenericItemUsingDamageDescriptor multiMeterElement,
			thermoMeterElement, allMeterElement;

	void registerMeter(int id) {
		int subId, completId;

		GenericItemUsingDamageDescriptor element;
		{
			subId = 0;
			completId = subId + (id << 6);
			element = new GenericItemUsingDamageDescriptor("MultiMeter");
			sharedItem.addElement(completId, element);
			multiMeterElement = element;
		}
		{
			subId = 1;
			completId = subId + (id << 6);
			element = new GenericItemUsingDamageDescriptor("ThermoMeter");
			sharedItem.addElement(completId, element);
			thermoMeterElement = element;
		}
		{
			subId = 2;
			completId = subId + (id << 6);
			element = new GenericItemUsingDamageDescriptor("AllMeter");
			sharedItem.addElement(completId, element);
			allMeterElement = element;
		}
		{
			subId = 8;
			completId = subId + (id << 6);
			element = new WirelessSignalAnalyserItemDescriptor("Wireless Analyser");
			sharedItem.addElement(completId, element);

		}

	}

	public static TreeResin treeResin;

	void registerTreeResinAndRubber(int id) {
		int subId, completId;
		String name;

		{
			TreeResin descriptor;
			subId = 0;
			completId = subId + (id << 6);
			name = "Tree Resin";

			descriptor = new TreeResin(name);

			sharedItem.addElement(completId, descriptor);
			treeResin = descriptor;
			addToOre("materialResin", descriptor.newItemStack());
		}
		{
			GenericItemUsingDamageDescriptor descriptor;
			subId = 1;
			completId = subId + (id << 6);
			name = "Rubber";

			descriptor = new GenericItemUsingDamageDescriptor(name);
			sharedItem.addElement(completId, descriptor);
			addToOre("itemRubber", descriptor.newItemStack());
		}
	}

	void registerTreeResinCollector(int id) {
		int subId, completId;
		String name;

		TreeResinCollectorDescriptor descriptor;
		{
			subId = 0;
			completId = subId + (id << 6);
			name = "Tree Resin Collector";

			descriptor = new TreeResinCollectorDescriptor(name, obj.getObj("treeresincolector"));
			sixNodeItem.addDescriptor(completId, descriptor);
		}
	}

	void registerBatteryCharger(int id) {
		int subId, completId;
		String name;

		BatteryChargerDescriptor descriptor;
		{
			subId = 0;
			completId = subId + (id << 6);
			name = "Weak 50V Battery Charger";

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
			name = "50V Battery Charger";

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
			name = "200V Battery Charger";

			descriptor = new BatteryChargerDescriptor(
					name, obj.getObj("batterychargera"),
					meduimVoltageCableDescriptor,// ElectricalCableDescriptor
													// cable,
					MVU, 1000// double nominalVoltage,double nominalPower
			);
			sixNodeItem.addDescriptor(completId, descriptor);
		}
	}

	void registerElectricalDrill(int id) {
		int subId, completId;
		String name;

		ElectricalDrillDescriptor descriptor;
		{
			subId = 0;
			completId = subId + (id << 6);
			name = "Cheap Electrical Drill";

			descriptor = new ElectricalDrillDescriptor(name,// iconId, name,
					8, 4000 // double operationTime,double operationEnergy
			);
			sharedItem.addElement(completId, descriptor);
		}
		{
			subId = 1;
			completId = subId + (id << 6);
			name = "Average Electrical Drill";

			descriptor = new ElectricalDrillDescriptor(name,// iconId, name,
					5, 5000 // double operationTime,double operationEnergy
			);
			sharedItem.addElement(completId, descriptor);
		}
		{
			subId = 2;
			completId = subId + (id << 6);
			name = "Fast Electrical Drill";

			descriptor = new ElectricalDrillDescriptor(name,// iconId, name,
					3, 6000 // double operationTime,double operationEnergy
			);
			sharedItem.addElement(completId, descriptor);
		}

	}

	void registerOreScanner(int id) {
		int subId, completId;
		String name;

		OreScanner descriptor;
		{
			subId = 0;
			completId = subId + (id << 6);
			name = "Ore Scanner";

			descriptor = new OreScanner(name

					);
			sharedItem.addElement(completId, descriptor);
		}

	}

	public static MiningPipeDescriptor miningPipeDescriptor;

	void registerMiningPipe(int id) {
		int subId, completId;
		String name;

		MiningPipeDescriptor descriptor;
		{
			subId = 0;
			completId = subId + (id << 6);
			name = "Mining Pipe";

			descriptor = new MiningPipeDescriptor(name// iconId, name
			);
			sharedItem.addElement(completId, descriptor);

			miningPipeDescriptor = descriptor;
		}

	}

	void registerAutoMiner(int id) {
		int subId, completId;
		String name;
		{
			subId = 0;
			name = "Auto Miner";

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

	void registerRawCable(int id) {
		int subId, completId;
		String name;

		{
			CopperCableDescriptor descriptor;
			subId = 0;
			completId = subId + (id << 6);
			name = "Copper Cable";

			descriptor = new CopperCableDescriptor(name);
			sharedItem.addElement(completId, descriptor);
			Data.addResource(descriptor.newItemStack());
		}
		{
			GenericItemUsingDamageDescriptor descriptor;
			subId = 1;
			completId = subId + (id << 6);
			name = "Iron Cable";

			descriptor = new GenericItemUsingDamageDescriptor(name);
			sharedItem.addElement(completId, descriptor);
			Data.addResource(descriptor.newItemStack());
		}
		{
			GenericItemUsingDamageDescriptor descriptor;
			subId = 2;
			completId = subId + (id << 6);
			name = "Tungsten Cable";

			descriptor = new GenericItemUsingDamageDescriptor(name);
			sharedItem.addElement(completId, descriptor);
			Data.addResource(descriptor.newItemStack());
		}
	}

	void registerBrush(int id) {

		int subId, completId;
		BrushDescriptor whiteDesc = null;
		String name = "";
		String[] subNames = { "Black", "Red", "Green", "Brown", "Blue",
				"Purple", "Cyan", "Silver", "Gray", "Pink", "Lime", "Yellow",
				"Light Blue", "Magenta", "Orange", "White" };
		for (int idx = 0; idx < 16; idx++) {
			subId = idx;
			name = subNames[idx] + " Brush";
			BrushDescriptor desc = new BrushDescriptor(name);
			sharedItem.addElement(subId + (id << 6), desc);
			whiteDesc = desc;

		}
		ItemStack emptyStack = findItemStack("White Brush");
		whiteDesc.setLife(emptyStack, 0);

		for (int idx = 0; idx < 16; idx++) {

			addShapelessRecipe(emptyStack.copy(),
					new ItemStack(Blocks.wool, 1, idx),
					new ItemStack(Items.iron_ingot));
		}

		for (int idx = 0; idx < 16; idx++) {
			name = subNames[idx] + " Brush";
			addShapelessRecipe(findItemStack(name, 1),
					new ItemStack(Items.dye, 1, idx),
					emptyStack.copy());
		}

	}

	void registerElectricalTool(int id) {
		int subId, completId;
		ItemStack stack;
		String name;
		{
			subId = 0;
			name = "Small Flashlight";

			ElectricalLampItem desc = new ElectricalLampItem(
					name,
					10, 8, 30, 15, 5, 50,// int light,int range,
					6000, 100// , energyStorage,discharg, charge
			);
			sharedItemStackOne.addElement(subId + (id << 6), desc);
		}

		{
			subId = 8;
			name = "Portable Electrical Mining Drill";

			ElectricalPickaxe desc = new ElectricalPickaxe(
					name,
					8, 3,// float strengthOn,float strengthOff,
					40000, 200, 800// double energyStorage,double
									// energyPerBlock,double chargePower
			);
			sharedItemStackOne.addElement(subId + (id << 6), desc);
		}

		{
			subId = 12;
			name = "Portable Electrical Axe";

			ElectricalAxe desc = new ElectricalAxe(
					name,
					8, 3,// float strengthOn,float strengthOff,
					40000, 200, 800// double energyStorage,double
									// energyPerBlock,double chargePower
			);
			sharedItemStackOne.addElement(subId + (id << 6), desc);
		}

	}

	void registerPortableItem(int id) {
		int subId, completId;
		String name;
		{
			subId = 0;
			name = "Portable Battery";

			BatteryItem desc = new BatteryItem(
					name,
					20000, 500, 100,// double energyStorage,double
									// chargePower,double dischargePower,
					2// int priority
			);
			sharedItemStackOne.addElement(subId + (id << 6), desc);
		}

		{
			subId = 1;
			name = "Portable Battery Pack";

			BatteryItem desc = new BatteryItem(
					name,
					60000, 1500, 300,// double energyStorage,double
										// chargePower,double dischargePower,
					2// int priority
			);
			sharedItemStackOne.addElement(subId + (id << 6), desc);
		}

		{
			subId = 16;
			name = "Portable Condensator";

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
			name = "Portable Condensator Pack";

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
			name = "X-Ray Scanner";

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

	void registerMiscItem(int id) {
		int subId, completId;
		String name;
		{
			subId = 0;
			name = "Cheap Chip";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
		}
		{
			subId = 1;
			name = "Advanced Chip";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
		}
		{
			subId = 2;
			name = "Machine Block";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
		}
		{
			subId = 3;
			name = "Electrical Probe Chip";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
		}
		{
			subId = 4;
			name = "Thermal Probe Chip";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
		}

		{
			subId = 6;
			name = "Copper Plate";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
			addToOre("plateCopper", desc.newItemStack());
		}
		{
			subId = 7;
			name = "Iron Plate";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
			addToOre("plateIron", desc.newItemStack());
		}
		{
			subId = 8;
			name = "Gold Plate";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
			addToOre("plateGold", desc.newItemStack());
		}
		{
			subId = 9;
			name = "Lead Plate";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
			addToOre("plateLead", desc.newItemStack());
		}
		{
			subId = 10;
			name = "Silicon Plate";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
			addToOre("plateSilicon", desc.newItemStack());
		}

		{
			subId = 11;
			name = "Alloy Plate";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
			addToOre("plateAlloy", desc.newItemStack());
		}
		{
			subId = 12;
			name = "Coal Plate";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
			addToOre("plateCoal", desc.newItemStack());
		}

		{
			subId = 16;
			name = "Silicon Dust";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
			addToOre("dustSilicon", desc.newItemStack());
		}
		{
			subId = 17;
			name = "Silicon Ingot";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
			addToOre("ingotSilicon", desc.newItemStack());
		}

		{
			subId = 22;
			name = "Machine Booster";
			MachineBoosterDescriptor desc = new MachineBoosterDescriptor(name);
			sharedItem.addElement(subId + (id << 6), desc);
		}
		{
			subId = 23;
			name = "Advanced Machine Block";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
		}
		{
			subId = 28;
			name = "Basic Magnet";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
		}
		{
			subId = 29;
			name = "Advanced Magnet";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
		}
		{
			subId = 32;
			name = "Data Logger Print";
			DataLogsPrintDescriptor desc = new DataLogsPrintDescriptor(name);
			dataLogsPrintDescriptor = desc;
			sharedItem.addWithoutRegistry(subId + (id << 6), desc);
		}

		{
			subId = 33;
			name = "Signal Antenna";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] {});
			sharedItem.addElement(subId + (id << 6), desc);
			Data.addResource(desc.newItemStack());
		}

		{
			subId = 40;
			name = "Player Filter";
			EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, EntityPlayerMP.class, 0f, 1f, 0f);
			sharedItem.addElement(subId + (id << 6), desc);
		}
		{
			subId = 41;
			name = "Monster Filter";
			EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, EntityMob.class, 1f, 0f, 0f);
			sharedItem.addElement(subId + (id << 6), desc);
		}

		{
			subId = 48;
			name = "Wrench";
			GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
					name, new String[] { "Electrical age wrench", "Can be used to turn", "some small wall blocks" });
			sharedItem.addElement(subId + (id << 6), desc);

			wrenchItemStack = desc.newItemStack();
		}

		{
			subId = 52;
			name = "Dielectric";
			DielectricItem desc = new DielectricItem(name, LVU);
			sharedItem.addElement(subId + (id << 6), desc);
		}
	}

	public DataLogsPrintDescriptor dataLogsPrintDescriptor;

	void recipeGround() {
		addRecipe(findItemStack("Ground Cable"),
				" C ",
				" C ",
				"CCC",
				Character.valueOf('C'), findItemStack("Copper Cable"));
	}

	void recipeElectricalSource() {
		// Trololol
	}

	void recipeElectricalCable() {
		addRecipe(signalCableDescriptor.newItemStack(1),
				"R",
				"C",
				Character.valueOf('C'), findItemStack("Iron Cable"),
				Character.valueOf('R'), "itemRubber");

		addRecipe(lowVoltageCableDescriptor.newItemStack(1),
				"R",
				"C",
				Character.valueOf('C'), findItemStack("Copper Cable"),
				Character.valueOf('R'), "itemRubber");

		addRecipe(meduimVoltageCableDescriptor.newItemStack(1),
				"R",
				"C",
				Character.valueOf('C'), lowVoltageCableDescriptor.newItemStack(1),
				Character.valueOf('R'), "itemRubber");

		addRecipe(highVoltageCableDescriptor.newItemStack(1),
				"R",
				"C",
				Character.valueOf('C'), meduimVoltageCableDescriptor.newItemStack(1),
				Character.valueOf('R'), "itemRubber");

		addRecipe(signalCableDescriptor.newItemStack(6),
				"RRR",
				"CCC",
				"RRR",
				Character.valueOf('C'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), "itemRubber");

		addRecipe(lowVoltageCableDescriptor.newItemStack(6),
				"RRR",
				"CCC",
				"RRR",
				Character.valueOf('C'), "ingotCopper",
				Character.valueOf('R'), "itemRubber");

	}

	void recipeThermalCable() {

		addRecipe(findItemStack("Copper Thermal Cable", 6),
				"SSS",
				"CCC",
				"SSS",
				Character.valueOf('S'), new ItemStack(Blocks.cobblestone),
				Character.valueOf('C'), "ingotCopper");

		addRecipe(findItemStack("Copper Thermal Cable", 1),
				"S",
				"C",
				Character.valueOf('S'), new ItemStack(Blocks.cobblestone),
				Character.valueOf('C'), findItemStack("Copper Cable"));

		// for(int idx = 0;idx<16;idx++)

	}

	void recipeLampSocket() {
		addRecipe(findItemStack("Lamp Socket A", 3),
				"G ",
				"IG",
				"G ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Lamp Socket B Projector", 3),
				" I",
				"IG",
				" I",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Street Light", 1),
				"G",
				"I",
				"I",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Robust Lamp Socket", 3),
				"GIG",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));
		addRecipe(findItemStack("Flat Lamp Socket", 3),
				"IGI",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));
		addRecipe(findItemStack("Simple Lamp Socket", 3),
				" I ",
				"GGG",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		

		addRecipe(findItemStack("Suspended Lamp Socket", 2),
				"I",
				"G",
				Character.valueOf('G'), findItemStack("Robust Lamp Socket"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Long Suspended Lamp Socket", 2),
				"I",
				"I",
				"G",
				Character.valueOf('G'), findItemStack("Robust Lamp Socket"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

	}

	void recipeLampSupply() {
		addRecipe(findItemStack("Lamp Supply", 1),
				" I ",
				"ICI",
				" I ",
				Character.valueOf('C'), "ingotCopper",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

	}

	void recipePassiveComponent() {

		addRecipe(findItemStack("Signal Diode", 4),
				" RB",
				"IIR",
				" RB",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('I'), findItemStack("Iron Cable"),
				Character.valueOf('B'), "itemRubber");

		addRecipe(findItemStack("10A Diode", 3),
				" RB",
				"IIR",
				" RB",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('B'), "itemRubber");

		addRecipe(findItemStack("25A Diode"),
				"D",
				"D",
				"D",
				Character.valueOf('D'), findItemStack("10A Diode"));

		
		addRecipe(findItemStack("Power Capacitor"),
				"cPc",
				"III",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('c'), findItemStack("Iron Cable"),
				Character.valueOf('P'), "plateIron");
		
		addRecipe(findItemStack("Power Inductor"),
				" P ",
				"cIc",
				"IPI",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('c'), findItemStack("Copper Cable"),
				Character.valueOf('P'), "plateIron");
		
		//name = "Power Capacitor"
		//name = "Power Inductor"
		
	}

	void recipeSwitch() {
		/*
		 * addRecipe(findItemStack("Signal Switch"), "  I", " I ", "CAC", Character.valueOf('R'), new ItemStack(Items.redstone), Character.valueOf('A'), "itemRubber", Character.valueOf('I'), findItemStack("Copper Cable"), Character.valueOf('C'), findItemStack("Signal Cable"));
		 * 
		 * addRecipe(findItemStack("Signal Switch with LED"), " RI", " I ", "CAC", Character.valueOf('R'), new ItemStack(Items.redstone), Character.valueOf('A'), "itemRubber", Character.valueOf('I'), findItemStack("Copper Cable"), Character.valueOf('C'), findItemStack("Signal Cable"));
		 */

		addRecipe(findItemStack("Low Voltage Switch"),
				"  I",
				" I ",
				"CAC",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('A'), "itemRubber",
				Character.valueOf('I'), findItemStack("Copper Cable"),
				Character.valueOf('C'), findItemStack("Low Voltage Cable"));

		addRecipe(findItemStack("Medium Voltage Switch"),
				"  I",
				"AIA",
				"CAC",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('A'), "itemRubber",
				Character.valueOf('I'), findItemStack("Copper Cable"),
				Character.valueOf('C'), findItemStack("Medium Voltage Cable"));

		addRecipe(findItemStack("High Voltage Switch"),
				"AAI",
				"AIA",
				"CAC",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('A'), "itemRubber",
				Character.valueOf('I'), findItemStack("Copper Cable"),
				Character.valueOf('C'), findItemStack("High Voltage Cable"));
	}

	void recipeElectricalRelay() {

		addRecipe(findItemStack("Low Voltage Relay"),
				"GGG",
				"OIO",
				"CRC",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('O'), new ItemStack(Items.iron_ingot),
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('A'), "itemRubber",
				Character.valueOf('I'), findItemStack("Copper Cable"),
				Character.valueOf('C'), findItemStack("Low Voltage Cable"));

		addRecipe(findItemStack("Medium Voltage Relay"),
				"GGG",
				"OIO",
				"CRC",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('O'), new ItemStack(Items.iron_ingot),
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('A'), "itemRubber",
				Character.valueOf('I'), findItemStack("Copper Cable"),
				Character.valueOf('C'), findItemStack("Medium Voltage Cable"));

		addRecipe(findItemStack("High Voltage Relay"),
				"GGG",
				"OIO",
				"CRC",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('O'), new ItemStack(Items.iron_ingot),
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('A'), "itemRubber",
				Character.valueOf('I'), findItemStack("Copper Cable"),
				Character.valueOf('C'), findItemStack("High Voltage Cable"));
	}

	void recipeWirelessSignal() {

		addRecipe(findItemStack("Wireless Signal Transmitter"),
				" S ",
				" R ",
				"ICI",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('S'), findItemStack("Signal Antenna"));

		addRecipe(findItemStack("Wireless Signal Repeater"),
				"S S",
				"R R",
				"ICI",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('S'), findItemStack("Signal Antenna"));

		addRecipe(findItemStack("Wireless Signal Receiver"),
				" S ",
				"ICI",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('S'), findItemStack("Signal Antenna"));

	}

	void recipeTransformer() {
		//for (int idx = 0; idx < 4; idx++) {
			addRecipe(findItemStack("Transformer"),
					"C C",
					"III",
					Character.valueOf('C'), findItemStack("Copper Cable"),
					Character.valueOf('I'), new ItemStack(Items.iron_ingot));
		//}
	}

	void recipeHeatFurnace() {
		addRecipe(findItemStack("Stone Heat Furnace"),
				"BBB",
				"BIB",
				"BiB",
				Character.valueOf('B'), new ItemStack(Blocks.stone),
				Character.valueOf('i'), findItemStack("Copper Thermal Cable"),
				Character.valueOf('I'), findItemStack("Combustion Chamber"));

	}

	void recipeTurbine() {

		addRecipe(findItemStack("50V Turbine"),
				" m ",
				"HMH",
				" E ",
				Character.valueOf('M'), findItemStack("Machine Block"),
				Character.valueOf('E'), findItemStack("Low Voltage Cable"),
				Character.valueOf('H'), findItemStack("Copper Thermal Cable"),
				Character.valueOf('m'), findItemStack("Electrical Motor")

		);
		addRecipe(findItemStack("200V Turbine"),
				"ImI",
				"HMH",
				"IEI",
				Character.valueOf('I'), "itemRubber",
				Character.valueOf('M'), findItemStack("Advanced Machine Block"),
				Character.valueOf('E'), findItemStack("Medium Voltage Cable"),
				Character.valueOf('H'), findItemStack("Copper Thermal Cable"),
				Character.valueOf('m'), findItemStack("Advanced Electrical Motor"));

	}

	void recipeBattery() {

		addRecipe(findItemStack("Cost Oriented Battery"),
				"C C",
				"PPP",
				"PPP",
				Character.valueOf('C'), findItemStack("Low Voltage Cable"),
				Character.valueOf('P'), "ingotLead",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Capacity Oriented Battery"),
				"PPP",
				"PBP",
				"PPP",
				Character.valueOf('B'), findItemStack("Cost Oriented Battery"),
				Character.valueOf('P'), "ingotLead");

		addRecipe(findItemStack("Voltage Oriented Battery"),
				"PPP",
				"PBP",
				"PPP",
				Character.valueOf('B'), findItemStack("Cost Oriented Battery"),
				Character.valueOf('P'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Current Oriented Battery"),
				"PPP",
				"PBP",
				"PPP",
				Character.valueOf('B'), findItemStack("Cost Oriented Battery"),
				Character.valueOf('P'), "ingotCopper");

		addRecipe(findItemStack("Life Oriented Battery"),
				"P P",
				" B ",
				"P P",
				Character.valueOf('B'), findItemStack("Cost Oriented Battery"),
				Character.valueOf('P'), new ItemStack(Items.gold_ingot));

		addRecipe(findItemStack("Single-use Battery"),
				"Cpp",
				"CII",
				"Cpp",
				Character.valueOf('C'), findItemStack("Low Voltage Cable"),
				Character.valueOf('p'), new ItemStack(Items.coal, 1, 0),
				Character.valueOf('I'), "ingotCopper");

		addRecipe(findItemStack("Single-use Battery"),
				"Cpp",
				"CII",
				"Cpp",
				Character.valueOf('C'), findItemStack("Low Voltage Cable"),
				Character.valueOf('p'), new ItemStack(Items.coal, 1, 1),
				Character.valueOf('I'), "ingotCopper");

		/*
		 * addRecipe(findItemStack("200V Condensator"), "C C", "ppp", "III", Character.valueOf('C'), findItemStack("Medium Voltage Cable"), Character.valueOf('p'), "plateCoal", Character.valueOf('I'), new ItemStack(Items.iron_ingot));
		 */

	}

	void recipeElectricalFurnace() {

		addRecipe(findItemStack("Electrical Furnace"),
				"III",
				"IFI",
				"ICI",
				Character.valueOf('C'), findItemStack("Low Voltage Cable"),
				Character.valueOf('F'), new ItemStack(Blocks.furnace),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));
	}

	private void recipeSixNodeMisc() {
		addRecipe(findItemStack("Watch"),
				"crc",
				"III",
				Character.valueOf('c'), findItemStack("Iron Cable"),
				Character.valueOf('r'), new ItemStack(Items.redstone),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Hub"),
				"I I",
				" c ",
				"I I",
				Character.valueOf('c'), findItemStack("Copper Cable"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));
	}

	void recipeAutoMiner() {
		addRecipe(findItemStack("Auto Miner"),
				"MCM",
				"BOB",
				" P ",
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('O'), findItemStack("Ore Scanner"),
				Character.valueOf('B'), findItemStack("Advanced Machine Block"),
				Character.valueOf('M'), findItemStack("Electrical Motor"),
				Character.valueOf('P'), findItemStack("Mining Pipe"));
	}

	void recipeWindTurbine() {
		addRecipe(findItemStack("Wind Turbine"),
				" I ",
				"IMI",
				" B ",
				Character.valueOf('B'), findItemStack("Machine Block"),
				Character.valueOf('I'), "plateIron",
				Character.valueOf('M'), findItemStack("Electrical Motor"));

		addRecipe(findItemStack("Water Turbine"),
				"  I",
				"BMI",
				"  I",
				Character.valueOf('I'), "plateIron",
				Character.valueOf('B'), findItemStack("Machine Block"),
				Character.valueOf('M'), findItemStack("Electrical Motor"));

	}

	void recipeSolarPannel() {
		addRecipe(findItemStack("Small Solar Panel"),
				"III",
				"CSC",
				"III",
				Character.valueOf('S'), "plateSilicon",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('C'), findItemStack("Low Voltage Cable"));

		addRecipe(findItemStack("Small Rotating Solar Panel"),
				"ISI",
				"I I",
				Character.valueOf('S'), findItemStack("Small Solar Panel"),
				Character.valueOf('M'), findItemStack("Electrical Motor"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

	}

	void recipeThermalDissipatorPassiveAndActive() {
		addRecipe(
				findItemStack("Small Passive Thermal Dissipator"),
				"I I",
				"III",
				"CIC",
				Character.valueOf('I'), "ingotCopper",
				Character.valueOf('C'), findItemStack("Copper Thermal Cable"));

	/*	addRecipe(
				findItemStack("Small Active Thermal Dissipator"),
				"RMR",
				"I I",
				"III",
				Character.valueOf('I'), "ingotCopper",
				Character.valueOf('M'), findItemStack("Electrical Motor"),
				Character.valueOf('R'), "itemRubber",
				Character.valueOf('C'), findItemStack("Copper Thermal Cable"));*/

		addRecipe(
				findItemStack("Small Active Thermal Dissipator"),
				"RMR",
				" D ",
				Character.valueOf('D'), findItemStack("Small Passive Thermal Dissipator"),
				Character.valueOf('M'), findItemStack("Electrical Motor"),
				Character.valueOf('R'), "itemRubber");

	/*	addRecipe(
				findItemStack("200V Active Thermal Dissipator"),
				"RMR",
				"I I",
				"III",
				Character.valueOf('I'), "ingotCopper",
				Character.valueOf('M'), findItemStack("Advanced Electrical Motor"),
				Character.valueOf('R'), "itemRubber",
				Character.valueOf('C'), findItemStack("Copper Thermal Cable"));*/

		addRecipe(
				findItemStack("200V Active Thermal Dissipator"),
				"RMR",
				" D ",
				Character.valueOf('D'), findItemStack("Small Passive Thermal Dissipator"),
				Character.valueOf('M'), findItemStack("Advanced Electrical Motor"),
				Character.valueOf('R'), "itemRubber");

	}

	void recipeGeneral() {

		Utils.addSmelting(treeResin.parentItem,
				treeResin.parentItemDamage, findItemStack("Rubber", 1), 0f);

	}

	void recipeHeatingCorp() {
		addRecipe(findItemStack("Small 50V Copper Heating Corp"),
				"C C",
				"CCC",
				"C C",
				Character.valueOf('C'), findItemStack("Copper Cable"));

		addRecipe(findItemStack("50V Copper Heating Corp"),
				"C C",
				"CCC",
				"C C",
				Character.valueOf('C'), "ingotCopper");

		addRecipe(findItemStack("Small 200V Copper Heating Corp"),
				"CC",
				Character.valueOf('C'), findItemStack("50V Copper Heating Corp"));

		addRecipe(findItemStack("200V Copper Heating Corp"),
				"CC",
				Character.valueOf('C'), findItemStack("Small 200V Copper Heating Corp"));

		addRecipe(findItemStack("Small 50V Iron Heating Corp"),
				"C C",
				"CCC",
				"C C", Character.valueOf('C'), findItemStack("Iron Cable"));

		addRecipe(findItemStack("50V Iron Heating Corp"),
				"C C",
				"CCC",
				"C C",
				Character.valueOf('C'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Small 200V Iron Heating Corp"),
				"CC",
				Character.valueOf('C'), findItemStack("50V Iron Heating Corp"));

		addRecipe(findItemStack("200V Iron Heating Corp"),
				"CC",
				Character.valueOf('C'), findItemStack("Small 200V Iron Heating Corp"));

		addRecipe(findItemStack("Small 50V Tungsten Heating Corp"),
				"C C",
				"CCC",
				"C C",
				Character.valueOf('C'), findItemStack("Tungsten Cable"));

		addRecipe(findItemStack("50V Tungsten Heating Corp"),
				"C C",
				"CCC",
				"C C",
				Character.valueOf('C'), findItemStack("Tungsten Ingot"));

		addRecipe(findItemStack("Small 200V Tungsten Heating Corp"),
				"CC",
				Character.valueOf('C'), findItemStack("50V Tungsten Heating Corp"));
		addRecipe(findItemStack("200V Tungsten Heating Corp"),
				"CC",
				Character.valueOf('C'), findItemStack("Small 200V Tungsten Heating Corp"));
	}

	void recipeThermalIsolator() {

	}

	void recipeRegulatorItem() {

		addRecipe(findItemStack("On/OFF Regulator 10%", 1),
				"R R",
				" R ",
				" I ",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("On/OFF Regulator 1%", 1),
				"RRR",
				" I ",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Analogic Regulator", 1),
				"R R",
				" C ",
				" I ",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('C'), findItemStack("Cheap Chip"));
	}

	void recipeLampItem() {

		// Tungsten
		addRecipe(
				findItemStack("Small 50V Incandescent Light Bulb", 4),
				" G ",
				"GFG",
				" S ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), dicTungstenIngot,
				Character.valueOf('S'), findItemStack("Copper Cable"));

		addRecipe(findItemStack("50V Incandescent Light Bulb", 4),
				" G ",
				"GFG",
				" S ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), dicTungstenIngot,
				Character.valueOf('S'), findItemStack("Low Voltage Cable"));

		addRecipe(findItemStack("200V Incandescent Light Bulb", 4),
				" G ",
				"GFG",
				" S ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), dicTungstenIngot,
				Character.valueOf('S'), findItemStack("Medium Voltage Cable"));

		// CARBON
		addRecipe(findItemStack("Small 50V Carbon Incandescent Light Bulb", 4),
				" G ",
				"GFG",
				" S ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), new ItemStack(Items.coal),
				Character.valueOf('S'), findItemStack("Copper Cable"));

		addRecipe(findItemStack("Small 50V Carbon Incandescent Light Bulb", 4),
				" G ",
				"GFG",
				" S ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), new ItemStack(Items.coal, 1, 1),
				Character.valueOf('S'), findItemStack("Copper Cable"));

		addRecipe(
				findItemStack("50V Carbon Incandescent Light Bulb", 4),
				" G ",
				"GFG",
				" S ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), new ItemStack(Items.coal),
				Character.valueOf('S'), findItemStack("Low Voltage Cable"));

		addRecipe(findItemStack("50V Carbon Incandescent Light Bulb", 4),
				" G ",
				"GFG",
				" S ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), new ItemStack(Items.coal, 1, 1),
				Character.valueOf('S'), findItemStack("Low Voltage Cable"));

		addRecipe(
				findItemStack("Small 50V Economic Light Bulb", 4),
				" G ",
				"GFG",
				" S ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), new ItemStack(Items.glowstone_dust),
				Character.valueOf('S'), findItemStack("Copper Cable"));

		addRecipe(findItemStack("50V Economic Light Bulb", 4),
				" G ",
				"GFG",
				" S ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), new ItemStack(Items.glowstone_dust),
				Character.valueOf('S'), findItemStack("Low Voltage Cable"));

		addRecipe(findItemStack("200V Economic Light Bulb", 4),
				" G ",
				"GFG",
				" S ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), new ItemStack(Items.glowstone_dust),
				Character.valueOf('S'), findItemStack("Medium Voltage Cable"));

		addRecipe(findItemStack("50V Farming Lamp", 2),
				"GGG",
				"FFF",
				"GSG",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), dicTungstenIngot,
				Character.valueOf('S'), findItemStack("Low Voltage Cable"));

		addRecipe(findItemStack("200V Farming Lamp", 2),
				"GGG",
				"FFF",
				"GSG",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('F'), dicTungstenIngot,
				Character.valueOf('S'), findItemStack("Medium Voltage Cable"));

	}

	void recipeProtection() {

		addRecipe(findItemStack("Overvoltage Protection", 4),
				"SCD",
				Character.valueOf('S'), findItemStack("Electrical Probe Chip"),
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('D'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("Overheating Protection", 4),
				"SCD",
				Character.valueOf('S'), findItemStack("Thermal Probe Chip"),
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('D'), new ItemStack(Items.redstone));

	}

	void recipeCombustionChamber() {
		addRecipe(findItemStack("Combustion Chamber"),
				" L ",
				"L L",
				" L ",
				Character.valueOf('L'), new ItemStack(Blocks.stone));
	}

	void recipeFerromagneticCore() {
		addRecipe(findItemStack("Cheap Ferromagnetic Core"),
				"LLL",
				"L  ",
				"LLL",
				Character.valueOf('L'), Items.iron_ingot);

		addRecipe(findItemStack("Average Ferromagnetic Core"),
				"PCP",
				Character.valueOf('C'), findItemStack("Cheap Ferromagnetic Core"),
				Character.valueOf('P'), "plateIron");

		addRecipe(findItemStack("Optimal Ferromagnetic Core"),
				"P",
				"C",
				"P",
				Character.valueOf('C'), findItemStack("Average Ferromagnetic Core"),
				Character.valueOf('P'), "plateIron");
	}

	void recipeIngot() {
		// Done
	}

	void recipeDust() {
		addShapelessRecipe(findItemStack("Alloy Dust", 3),
				"dustIron",
				"dustIron",
				"dustCoal",
				dicTungstenDust);

	}

	void addShapelessRecipe(ItemStack output, Object... params) {
		GameRegistry.addRecipe(new ShapelessOreRecipe(output, params));
	}

	void recipeElectricalMotor() {

		addRecipe(findItemStack("Electrical Motor"),
				" C ",
				"III",
				"C C",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('C'), findItemStack("Low Voltage Cable"));

		addRecipe(findItemStack("Advanced Electrical Motor"),
				"RCR",
				"MIM",
				"CRC",
				Character.valueOf('M'), findItemStack("Basic Magnet"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('C'), findItemStack("Medium Voltage Cable"));

		// TODO

	}

	void recipeSolarTracker() {
		addRecipe(findItemStack("Solar Tracker", 4),
				"VVV",
				"RQR",
				"III",
				Character.valueOf('Q'), new ItemStack(Items.quartz),
				Character.valueOf('V'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('G'), new ItemStack(Items.gold_ingot),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

	}

	void recipeDynamo() {

	}

	void recipeWindRotor() {

	}

	void recipeMeter() {
		addRecipe(findItemStack("MultiMeter"),
				"RGR",
				"RER",
				"RCR",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('C'), findItemStack("Electrical Probe Chip"),
				Character.valueOf('E'), new ItemStack(Items.redstone),
				Character.valueOf('R'), "itemRubber");

		addRecipe(findItemStack("ThermoMeter"),
				"RGR",
				"RER",
				"RCR",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('C'), findItemStack("Thermal Probe Chip"),
				Character.valueOf('E'), new ItemStack(Items.redstone),
				Character.valueOf('R'), "itemRubber");

		addShapelessRecipe(findItemStack("AllMeter"),
				findItemStack("MultiMeter"),
				findItemStack("ThermoMeter"));

		addRecipe(findItemStack("Wireless Analyser"),
				" S ",
				"RGR",
				"RER",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('S'), findItemStack("Signal Antenna"),
				Character.valueOf('E'), new ItemStack(Items.redstone),
				Character.valueOf('R'), "itemRubber");

	}

	void recipeElectricalDrill() {
		addRecipe(findItemStack("Cheap Electrical Drill"),
				"CMC",
				" T ",
				" P ",
				Character.valueOf('T'), findItemStack("Mining Pipe"),
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('M'), findItemStack("Electrical Motor"),
				Character.valueOf('P'), new ItemStack(Items.iron_pickaxe));

		addRecipe(findItemStack("Average Electrical Drill"),
				"RCR",
				" D ",
				" d ", Character.valueOf('R'), Items.redstone,
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('D'), findItemStack("Cheap Electrical Drill"),
				Character.valueOf('d'), new ItemStack(Items.diamond));

		addRecipe(findItemStack("Fast Electrical Drill"),
				"MCM",
				" T ",
				" P ",
				Character.valueOf('T'), findItemStack("Mining Pipe"),
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('M'), findItemStack("Advanced Electrical Motor"),
				Character.valueOf('P'), new ItemStack(Items.diamond_pickaxe));

	}

	void recipeOreScanner() {

		addRecipe(findItemStack("Ore Scanner"),
				"IGI",
				"RCR",
				"IGI",
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('G'), new ItemStack(Items.gold_ingot));

	}

	void recipeMiningPipe() {
		addRecipe(findItemStack("Mining Pipe", 4),
				"A",
				"A",
				"A",
				Character.valueOf('A'), "ingotAlloy");
	}

	void recipeTreeResinAndRubber() {
		for (int idx = 0; idx < 4; idx++) {
			addRecipe(findItemStack("Tree Resin Collector"),
					"W W",
					" WW",
					Character.valueOf('W'), new ItemStack(Blocks.planks, 1, idx));
		}
		for (int idx = 0; idx < 4; idx++) {
			addRecipe(findItemStack("Tree Resin Collector"),
					"W W",
					"WW ", Character.valueOf('W'), new ItemStack(Blocks.planks, 1, idx));
		}

	}

	void recipeRawCable() {
		addRecipe(findItemStack("Copper Cable", 6),
				"III",
				Character.valueOf('I'), "ingotCopper");

		addRecipe(findItemStack("Iron Cable", 6),
				"III",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Tungsten Cable", 6),
				"III",
				Character.valueOf('I'), dicTungstenIngot);

	}

	void recipeBatteryItem() {
		addRecipe(findItemStack("Portable Battery"),
				"I",
				"P",
				"P",
				Character.valueOf('P'), "ingotLead",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));
		addShapelessRecipe(
				findItemStack("Portable Battery Pack"),
				findItemStack("Portable Battery"), findItemStack("Portable Battery"), findItemStack("Portable Battery"));
	}

	void recipeElectricalTool() {

		addRecipe(findItemStack("Small Flashlight"),
				"GLG",
				"IBI",
				" I ",
				Character.valueOf('L'), findItemStack("50V Incandescent Light Bulb"),
				Character.valueOf('B'), findItemStack("Portable Battery"),
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Portable Electrical Mining Drill"),
				" T ",
				"IBI",
				" I ",
				Character.valueOf('T'), findItemStack("Average Electrical Drill"),
				Character.valueOf('B'), findItemStack("Portable Battery"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Portable Electrical Axe"),
				" T ",
				"IMI",
				"IBI",
				Character.valueOf('T'), new ItemStack(Items.iron_axe),
				Character.valueOf('B'), findItemStack("Portable Battery"),
				Character.valueOf('M'), findItemStack("Electrical Motor"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("X-Ray Scanner"),
				"PGP",
				"PCP",
				"PBP",
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('B'), findItemStack("Portable Battery"),
				Character.valueOf('P'), new ItemStack(Items.iron_ingot),
				Character.valueOf('G'), findItemStack("Ore Scanner"));

	}

	void recipeECoal()
	{
		addRecipe(findItemStack("E-Coal Helmet"),
				"PPP",
				"PCP",
				Character.valueOf('P'), "plateCoal",
				Character.valueOf('C'), findItemStack("Advanced Chip"));
		addRecipe(findItemStack("E-Coal Boots"),
				" C ",
				"P P",
				"P P",
				Character.valueOf('P'), "plateCoal",
				Character.valueOf('C'), findItemStack("Advanced Chip"));

		addRecipe(findItemStack("E-Coal Chestplate"),
				"P P",
				"PCP",
				"PPP",
				Character.valueOf('P'), "plateCoal",
				Character.valueOf('C'), findItemStack("Advanced Chip"));

		addRecipe(findItemStack("E-Coal Leggings"),
				"PPP",
				"PCP",
				"P P",
				Character.valueOf('P'), "plateCoal",
				Character.valueOf('C'), findItemStack("Advanced Chip"));

	}

	void recipePortableCondensator()
	{
		addRecipe(findItemStack("Portable Condensator"),
				"RcR",
				"wCw",
				"RcR",
				Character.valueOf('C'), new ItemStack(Items.redstone),
				Character.valueOf('R'), "itemRubber",
				Character.valueOf('w'), findItemStack("Copper Cable"),
				Character.valueOf('c'), "plateCopper");

		addShapelessRecipe(findItemStack("Portable Condensator Pack"),
				findItemStack("Portable Condensator"),
				findItemStack("Portable Condensator"),
				findItemStack("Portable Condensator"));
	}

	void recipeMiscItem() {
		addRecipe(findItemStack("Cheap Chip"),
				" R ",
				"RSR",
				" R ",
				Character.valueOf('S'), "ingotSilicon",
				Character.valueOf('R'), new ItemStack(Items.redstone));
		addRecipe(findItemStack("Advanced Chip"),
				"LRL",
				"RCR",
				"LRL",
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('L'), "ingotSilicon",
				Character.valueOf('R'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("Machine Block"),
				"LLL",
				"LcL",
				"LLL",
				Character.valueOf('L'), new ItemStack(Items.iron_ingot),
				Character.valueOf('c'), findItemStack("Copper Cable"));

		addRecipe(findItemStack("Advanced Machine Block"),
				" C ",
				"CcC",
				" C ",
				Character.valueOf('C'), "plateAlloy",
				Character.valueOf('L'), "ingotAlloy",
				Character.valueOf('c'), findItemStack("Copper Cable"));

		addRecipe(findItemStack("Electrical Probe Chip"),
				" R ",
				"RCR",
				" R ",
				Character.valueOf('C'), findItemStack("High Voltage Cable"),
				Character.valueOf('R'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("Thermal Probe Chip"),
				" C ",
				"RIR",
				" C ",
				Character.valueOf('G'), new ItemStack(Items.gold_ingot),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('C'), "ingotCopper",
				Character.valueOf('R'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("Signal Antenna"),
				"c",
				"c",
				Character.valueOf('c'), findItemStack("Iron Cable"));

		addRecipe(findItemStack("Machine Booster"),
				"m",
				"c",
				"m",
				Character.valueOf('m'), findItemStack("Electrical Motor"),
				Character.valueOf('c'), findItemStack("Advanced Chip"));

		addRecipe(findItemStack("Wrench"),
				" c ",
				"cc ",
				"  c",
				Character.valueOf('c'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Player Filter"),
				" g",
				"gc",
				" g",
				Character.valueOf('g'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('c'), new ItemStack(Items.dye, 1, 2));

		addRecipe(findItemStack("Monster Filter"),
				" g",
				"gc",
				" g",
				Character.valueOf('g'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('c'), new ItemStack(Items.dye, 1, 1));

	}

	void recipeMacerator() {
		float f = 4000;
		maceratorRecipes.addRecipe(new Recipe(findItemStack("Copper Ore"),
				new ItemStack[] { findItemStack("Copper Dust", 2) }, 1.0 * f));
		maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.iron_ore),
				new ItemStack[] { findItemStack("Iron Dust", 2) }, 1.5 * f));
		maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.gold_ore),
				new ItemStack[] { findItemStack("Gold Dust", 2) }, 3.0 * f));
		maceratorRecipes.addRecipe(new Recipe(findItemStack("Lead Ore"),
				new ItemStack[] { findItemStack("Lead Dust", 2) }, 2.0 * f));
		maceratorRecipes.addRecipe(new Recipe(findItemStack("Tungsten Ore"),
				new ItemStack[] { findItemStack("Tungsten Dust", 2) }, 2.0 * f));
		maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.coal, 1, 0),
				new ItemStack[] { findItemStack("Coal Dust", 2) }, 1.0 * f));
		maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.coal, 1, 1),
				new ItemStack[] { findItemStack("Coal Dust", 2) }, 1.0 * f));
		maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.sand, 1),
				new ItemStack[] { findItemStack("Silicon Dust", 1) }, 3.0 * f));
		maceratorRecipes.addRecipe(new Recipe(findItemStack("Cinnabar Ore"),
				new ItemStack[] { findItemStack("Cinnabar Dust", 2) }, 2.0 * f));

		maceratorRecipes.addRecipe(new Recipe(findItemStack("Copper Ingot"),
				new ItemStack[] { findItemStack("Copper Dust", 1) }, 0.5 * f));
		maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.iron_ingot),
				new ItemStack[] { findItemStack("Iron Dust", 1) }, 0.5 * f));
		maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.gold_ingot),
				new ItemStack[] { findItemStack("Gold Dust", 1) }, 0.5 * f));
		maceratorRecipes.addRecipe(new Recipe(findItemStack("Lead Ingot"),
				new ItemStack[] { findItemStack("Lead Dust", 1) }, 0.5 * f));
		maceratorRecipes.addRecipe(new Recipe(findItemStack("Tungsten Ingot"),
				new ItemStack[] { findItemStack("Tungsten Dust", 1) }, 0.5 * f));

		maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.cobblestone),
				new ItemStack[] { new ItemStack(Blocks.gravel) }, 1.0 * f));
		maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.gravel),
				new ItemStack[] { new ItemStack(Items.flint) }, 1.0 * f));

		maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.dirt),
				new ItemStack[] { new ItemStack(Blocks.sand) }, 1.0 * f));
	}

	void recipePlateMachine() {
		float f = 10000;
		plateMachineRecipes.addRecipe(new Recipe(
				findItemStack("Copper Ingot", 4),
				findItemStack("Copper Plate"), 1.0 * f));

		plateMachineRecipes.addRecipe(new Recipe(findItemStack("Lead Ingot", 4),
				findItemStack("Lead Plate"), 1.0 * f));

		plateMachineRecipes.addRecipe(new Recipe(
				findItemStack("Silicon Ingot", 4),
				findItemStack("Silicon Plate"), 1.0 * f));

		plateMachineRecipes.addRecipe(new Recipe(findItemStack("Alloy Ingot", 4),
				findItemStack("Alloy Plate"), 1.0 * f));

		plateMachineRecipes.addRecipe(new Recipe(new ItemStack(Items.iron_ingot, 4,
				0), findItemStack("Iron Plate"), 1.0 * f));

		plateMachineRecipes.addRecipe(new Recipe(new ItemStack(Items.gold_ingot, 4,
				0), findItemStack("Gold Plate"), 1.0 * f));

	}

	void recipeCompressor() {
		compressorRecipes.addRecipe(new Recipe(findItemStack("Coal Plate", 4),
				new ItemStack[] { new ItemStack(Items.diamond) }, 80000.0));
		// extractorRecipes.addRecipe(new
		// Recipe("dustCinnabar",new
		// ItemStack[]{findItemStack("Purified Cinnabar Dust",1)}, 1000.0));

		compressorRecipes.addRecipe(new Recipe(findItemStack("Coal Dust", 4),
				findItemStack("Coal Plate"), 4000.0));

		compressorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.sand),
				findItemStack("Dielectric"), 2000.0));

		compressorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.log),
				findItemStack("Tree Resin"), 3000.0));

	}

	void recipemagnetiser() {
		magnetiserRecipes.addRecipe(new Recipe(new ItemStack(Items.iron_ingot),
				new ItemStack[] { findItemStack("Basic Magnet") }, 5000.0));
		magnetiserRecipes.addRecipe(new Recipe(findItemStack("Alloy Ingot", 1),
				new ItemStack[] { findItemStack("Advanced Magnet") }, 15000.0));
	}

	void recipeFurnace() {

		ItemStack in;

		in = findItemStack("Copper Ore");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				findItemStack("Copper Ingot"));
		in = findItemStack("dustCopper");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				findItemStack("Copper Ingot"));
		in = findItemStack("Lead Ore");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				findItemStack("ingotLead"));
		in = findItemStack("dustLead");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				findItemStack("ingotLead"));
		in = findItemStack("Tungsten Ore");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				findItemStack("Tungsten Ingot"));
		in = findItemStack("Tungsten Dust");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				findItemStack("Tungsten Ingot"));
		in = findItemStack("ingotAlloy");
		// Utils.addSmelting(in.getItem().itemID, in.getItemDamage(),
		// findItemStack("Ferrite Ingot"));
		in = findItemStack("dustIron");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				new ItemStack(Items.iron_ingot));

		in = findItemStack("dustGold");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				new ItemStack(Items.gold_ingot));

		in = findItemStack("Tree Resin");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				findItemStack("Rubber", 2));

		in = findItemStack("Alloy Dust");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				findItemStack("Alloy Ingot"));

		in = findItemStack("Silicon Dust");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				findItemStack("Silicon Ingot"));

		// in = findItemStack("Purified Cinnabar Dust");
		in = findItemStack("dustCinnabar");
		Utils.addSmelting(in.getItem(), in.getItemDamage(),
				findItemStack("Mercury"));

	}

	void recipeElectricalSensor() {
		addRecipe(findItemStack("Voltage Probe", 1),
				"SC",
				Character.valueOf('S'), findItemStack("Electrical Probe Chip"),
				Character.valueOf('C'), findItemStack("Signal Cable"));

		addRecipe(findItemStack("Electrical Probe", 1),
				"SCS",
				Character.valueOf('S'), findItemStack("Electrical Probe Chip"),
				Character.valueOf('C'), findItemStack("Signal Cable"));

	}

	void recipeThermalSensor() {

		addRecipe(findItemStack("Thermal Probe", 1),
				"SCS",
				Character.valueOf('S'), findItemStack("Thermal Probe Chip"),
				Character.valueOf('C'), findItemStack("Signal Cable"));

		addRecipe(findItemStack("Temperature Probe", 1),
				"SC",
				Character.valueOf('S'), findItemStack("Thermal Probe Chip"),
				Character.valueOf('C'), findItemStack("Signal Cable"));

	}

	void recipeTransporter() {
		addRecipe(findItemStack("Experimental Transporter", 1),
				"RMR",
				"RMR",
				" R ",
				Character.valueOf('M'), findItemStack("Advanced Machine Block"),
				Character.valueOf('C'), findItemStack("High Voltage Cable"),
				Character.valueOf('R'), findItemStack("Advanced Chip"));
	}

	void recipeMachine() {
		addRecipe(findItemStack("50V Macerator", 1),
				"IRI",
				"FMF",
				"IcI",
				Character.valueOf('M'), findItemStack("Machine Block"),
				Character.valueOf('c'), findItemStack("Electrical Motor"),
				Character.valueOf('F'), new ItemStack(Items.flint),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));
		addRecipe(findItemStack("200V Macerator", 1),
				"ICI",
				"DMD",
				"IcI",
				Character.valueOf('M'), findItemStack("Advanced Machine Block"),
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('c'), findItemStack("Advanced Electrical Motor"),
				Character.valueOf('D'), new ItemStack(Items.diamond),
				Character.valueOf('I'), "ingotAlloy");

		addRecipe(findItemStack("50V Compressor", 1),
				"IRI",
				"FMF",
				"IcI",
				Character.valueOf('M'), findItemStack("Machine Block"),
				Character.valueOf('c'), findItemStack("Electrical Motor"),
				Character.valueOf('F'), "plateIron",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));
		addRecipe(findItemStack("200V Compressor", 1),
				"ICI",
				"DMD",
				"IcI",
				Character.valueOf('M'), findItemStack("Advanced Machine Block"),
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('c'), findItemStack("Advanced Electrical Motor"),
				Character.valueOf('D'), "plateAlloy",
				Character.valueOf('I'), "ingotAlloy");

		addRecipe(findItemStack("50V Plate Machine", 1),
				"IRI",
				"IMI",
				"IcI",
				Character.valueOf('M'), findItemStack("Machine Block"),
				Character.valueOf('c'), findItemStack("Electrical Motor"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("200V Plate Machine", 1),
				"DCD",
				"DMD",
				"DcD",
				Character.valueOf('M'), findItemStack("Advanced Machine Block"),
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('c'), findItemStack("Advanced Electrical Motor"),
				Character.valueOf('D'), "plateAlloy",
				Character.valueOf('I'), "ingotAlloy");

		addRecipe(findItemStack("50V Magnetizer", 1),
				"IRI",
				"cMc",
				"III",
				Character.valueOf('M'), findItemStack("Machine Block"),
				Character.valueOf('c'), findItemStack("Electrical Motor"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("200V Magnetizer", 1),
				"ICI",
				"cMc",
				"III",
				Character.valueOf('M'), findItemStack("Advanced Machine Block"),
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('c'), findItemStack("Advanced Electrical Motor"),
				Character.valueOf('I'), "ingotAlloy");

	}

	private void recipeElectricalGate() {

		addShapelessRecipe(findItemStack("Electrical Timer"),
				new ItemStack(Items.repeater),
				findItemStack("Cheap Chip"));

		addRecipe(findItemStack("Signal Processor", 1),
				" c ",
				"cCc",
				" c ",
				Character.valueOf('c'), findItemStack("Signal Cable"),
				Character.valueOf('C'), findItemStack("Cheap Chip"));
	}

	private void recipeElectricalRedstone() {

		addRecipe(findItemStack("Redstone-to-Voltage Converter", 1),
				"TCS",
				Character.valueOf('S'), findItemStack("Signal Cable"),
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('T'), new ItemStack(Blocks.redstone_torch));

		addRecipe(findItemStack("Voltage-to-Redstone Converter", 1),
				"CTR",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('T'), new ItemStack(Blocks.redstone_torch));

	}

	private void recipeElectricalEnvironnementalSensor() {
		addShapelessRecipe(findItemStack("Electrical Daylight Sensor"),
				new ItemStack(Blocks.daylight_detector),
				findItemStack("Redstone-to-Voltage Converter"));

		addShapelessRecipe(findItemStack("Electrical Light Sensor"),
				new ItemStack(Blocks.daylight_detector),
				new ItemStack(Items.quartz),
				findItemStack("Redstone-to-Voltage Converter"));

		addRecipe(findItemStack("Electrical Weather Sensor"),
				" r ",
				"rRr",
				" r ",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('r'), "itemRubber");

		addRecipe(findItemStack("Electrical Anemometer Sensor"),
				" I ",
				" R ",
				"I I",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot));

		addRecipe(findItemStack("Electrical Entity Sensor"),
				" G ",
				"GRG",
				" G ",
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane),
				Character.valueOf('R'), new ItemStack(Items.redstone));
	}

	private void recipeElectricalVuMeter() {

		for (int idx = 0; idx < 4; idx++) {
			addRecipe(findItemStack("Analog vuMeter", 1),
					"WWW",
					"RIr",
					"WSW",
					Character.valueOf('W'), new ItemStack(Blocks.planks, 1, idx),
					Character.valueOf('R'), new ItemStack(Items.redstone),
					Character.valueOf('I'), new ItemStack(Items.iron_ingot),
					Character.valueOf('r'), new ItemStack(Items.dye, 1, 1),
					Character.valueOf('S'), findItemStack("Signal Cable"));
		}
		for (int idx = 0; idx < 4; idx++) {
			addRecipe(findItemStack("LED vuMeter", 1),
					" W ",
					"WTW",
					" S ",
					Character.valueOf('W'), new ItemStack(Blocks.planks, 1, idx),
					Character.valueOf('T'), new ItemStack(Blocks.redstone_torch),
					Character.valueOf('S'), findItemStack("Signal Cable"));
		}
	}

	private void recipeElectricalBreaker() {

		addRecipe(findItemStack("Electrical Breaker", 1),
				"crC",
				Character.valueOf('c'), findItemStack("Overvoltage Protection"),
				Character.valueOf('C'), findItemStack("Overheating Protection"),
				Character.valueOf('r'), findItemStack("High Voltage Relay"));

	}

	private void recipeElectricalGateSource() {

		addRecipe(findItemStack("Signal Trimmer", 1),
				"RsR",
				"rRr",
				" c ",
				Character.valueOf('M'), findItemStack("Machine Block"),
				Character.valueOf('c'), findItemStack("Signal Cable"),
				Character.valueOf('r'), "itemRubber",
				Character.valueOf('s'), new ItemStack(Items.stick),
				Character.valueOf('R'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("Signal Switch", 3),
				" r ",
				"rRr",
				" c ",
				Character.valueOf('M'), findItemStack("Machine Block"),
				Character.valueOf('c'), findItemStack("Signal Cable"),
				Character.valueOf('r'), "itemRubber",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("Signal Button", 3),
				" R ",
				"rRr",
				" c ",
				Character.valueOf('M'), findItemStack("Machine Block"),
				Character.valueOf('c'), findItemStack("Signal Cable"),
				Character.valueOf('r'), "itemRubber",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("Wireless Switch", 3),
				" a ",
				"rCr",
				" r ",
				Character.valueOf('M'), findItemStack("Machine Block"),
				Character.valueOf('c'), findItemStack("Signal Cable"),
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('a'), findItemStack("Signal Antenna"),
				Character.valueOf('r'), "itemRubber",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("Wireless Button", 3),
				" a ",
				"rCr",
				" R ",
				Character.valueOf('M'), findItemStack("Machine Block"),
				Character.valueOf('c'), findItemStack("Signal Cable"),
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('a'), findItemStack("Signal Antenna"),
				Character.valueOf('r'), "itemRubber",
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));

		// Wireless Switch
		// Wireless Button
	}

	private void recipeElectricalDataLogger() {
		addRecipe(findItemStack("Data Logger", 1),
				"RRR",
				"RGR",
				"RCR",
				Character.valueOf('R'), "itemRubber",
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane));
	}

	private void recipeSixNodeCache()
	{

	}

	private void recipeElectricalAlarm() {
		addRecipe(findItemStack("Nuclear Alarm", 1),
				"ITI",
				"IMI",
				"IcI",
				Character.valueOf('c'), findItemStack("Signal Cable"),
				Character.valueOf('T'), new ItemStack(Blocks.redstone_torch),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('M'), new ItemStack(Blocks.noteblock));
		addRecipe(findItemStack("Standard Alarm", 1),
				"MTM",
				"IcI",
				"III",
				Character.valueOf('c'), findItemStack("Signal Cable"),
				Character.valueOf('T'), new ItemStack(Blocks.redstone_torch),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('M'), new ItemStack(Blocks.noteblock));

	}

	private void recipeElectricalAntenna() {
		addRecipe(findItemStack("Low Power Transmitter Antenna", 1),
				"R i",
				"CI ",
				"R i",
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('i'), new ItemStack(Items.iron_ingot),
				Character.valueOf('I'), "plateIron",
				Character.valueOf('R'), new ItemStack(Items.redstone));
		addRecipe(findItemStack("Low Power Receiver Antenna", 1),
				"i  ",
				" IC",
				"i  ",
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('I'), "plateIron",
				Character.valueOf('i'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));
		addRecipe(findItemStack("Medium Power Transmitter Antenna", 1),
				"c I",
				"CI ",
				"c I",
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('c'), findItemStack("Cheap Chip"),
				Character.valueOf('I'), "plateIron",
				Character.valueOf('R'), new ItemStack(Items.redstone));
		addRecipe(findItemStack("Medium Power Receiver Antenna", 1),
				"I  ",
				" IC",
				"I  ",
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('I'), "plateIron",
				Character.valueOf('R'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("High Power Transmitter Antenna", 1),
				"C I",
				"CI ",
				"C I",
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('c'), findItemStack("Cheap Chip"),
				Character.valueOf('I'), "plateIron",
				Character.valueOf('R'), new ItemStack(Items.redstone));
		addRecipe(findItemStack("High Power Receiver Antenna", 1),
				"I D",
				" IC",
				"I D",
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('I'), "plateIron",
				Character.valueOf('R'), new ItemStack(Items.redstone),
				Character.valueOf('R'), new ItemStack(Items.diamond));

	}

	private void recipeBatteryCharger() {
		addRecipe(findItemStack("Weak 50V Battery Charger", 1),
				"RIR",
				"III",
				"RcR",
				Character.valueOf('c'), findItemStack("Low Voltage Cable"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));
		addRecipe(findItemStack("50V Battery Charger", 1),
				"RIR",
				"ICI",
				"RcR",
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('c'), findItemStack("Low Voltage Cable"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));

		addRecipe(findItemStack("200V Battery Charger", 1),
				"RIR",
				"ICI",
				"RcR",
				Character.valueOf('C'), findItemStack("Advanced Chip"),
				Character.valueOf('c'), findItemStack("Medium Voltage Cable"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('R'), new ItemStack(Items.redstone));

	}

	private void recipeEggIncubatore() {
		addRecipe(findItemStack("50V Egg Incubator", 1),
				"IGG",
				"E G",
				"CII",
				Character.valueOf('C'), findItemStack("Cheap Chip"),
				Character.valueOf('E'), findItemStack("Small 50V Tungsten Heating Corp"),
				Character.valueOf('I'), new ItemStack(Items.iron_ingot),
				Character.valueOf('G'), new ItemStack(Blocks.glass_pane));

	}

	void recipeEnergyConverter() {
		if (ElnToOtherEnergyConverterEnable) {
			addRecipe(new ItemStack(elnToOtherBlockLvu),
					"III",
					"cCR",
					"III",
					Character.valueOf('C'), findItemStack("Cheap Chip"),
					Character.valueOf('c'), findItemStack("Low Voltage Cable"),
					Character.valueOf('I'), new ItemStack(Items.iron_ingot),
					Character.valueOf('R'), "ingotCopper");

			addRecipe(new ItemStack(elnToOtherBlockMvu),
					"III",
					"cCR",
					"III",
					Character.valueOf('C'), findItemStack("Cheap Chip"),
					Character.valueOf('c'), findItemStack("Medium Voltage Cable"),
					Character.valueOf('I'), new ItemStack(Items.iron_ingot),
					Character.valueOf('R'), new ItemStack(Items.iron_ingot));

			addRecipe(new ItemStack(elnToOtherBlockHvu),
					"III",
					"cCR",
					"III",
					Character.valueOf('C'), findItemStack("Advanced Chip"),
					Character.valueOf('c'), findItemStack("High Voltage Cable"),
					Character.valueOf('I'), new ItemStack(Items.iron_ingot),
					Character.valueOf('R'), new ItemStack(Items.gold_ingot));

		}
	}
	
	void recipeComputerProbe(){
		if(ComputerProbeEnable){
			addRecipe(new ItemStack(computerProbeBlock),
					"cIw",
					"ICI",
					"WIc",
					Character.valueOf('C'), findItemStack("Advanced Chip"),
					Character.valueOf('c'), findItemStack("Signal Cable"),
					Character.valueOf('I'), new ItemStack(Items.iron_ingot),
					Character.valueOf('w'), findItemStack("Wireless Signal Receiver"),
					Character.valueOf('W'), findItemStack("Wireless Signal Transmitter"));			
		}
	}

	void recipeArmor()
	{
		addRecipe(new ItemStack(helmetCopper),
				"CCC",
				"C C",
				Character.valueOf('C'), "ingotCopper");

		addRecipe(new ItemStack(plateCopper),
				"C C",
				"CCC",
				"CCC",
				Character.valueOf('C'), "ingotCopper");

		addRecipe(new ItemStack(legsCopper),
				"CCC",
				"C C",
				"C C",
				Character.valueOf('C'), "ingotCopper");

		addRecipe(new ItemStack(bootsCopper),
				"C C",
				"C C",
				Character.valueOf('C'), "ingotCopper");
	}

	private void addRecipe(ItemStack output, Object... params) {
		GameRegistry.addRecipe(new ShapedOreRecipe(output, params));
	}

	void recipeTool()
	{
		addRecipe(new ItemStack(shovelCopper),
				"i",
				"s",
				"s",
				Character.valueOf('i'), "ingotCopper",
				Character.valueOf('s'), new ItemStack(Items.stick));
		addRecipe(new ItemStack(axeCopper),
				"ii",
				"is",
				" s",
				Character.valueOf('i'), "ingotCopper",
				Character.valueOf('s'), new ItemStack(Items.stick));
		addRecipe(new ItemStack(hoeCopper),
				"ii",
				" s",
				" s",
				Character.valueOf('i'), "ingotCopper",
				Character.valueOf('s'), new ItemStack(Items.stick));
		addRecipe(new ItemStack(pickaxeCopper),
				"iii",
				" s ",
				" s ",
				Character.valueOf('i'), "ingotCopper",
				Character.valueOf('s'), new ItemStack(Items.stick));
		addRecipe(new ItemStack(swordCopper),
				"i",
				"i",
				"s",
				Character.valueOf('i'), "ingotCopper",
				Character.valueOf('s'), new ItemStack(Items.stick));

	}

	int replicatorRegistrationId = -1;

	void registerReplicator() {
		int redColor = (255 << 16);
		int orangeColor = (255 << 16) + (200 << 8);

		if (replicatorRegistrationId == -1)
			replicatorRegistrationId = EntityRegistry.findGlobalUniqueEntityId();
		Utils.println("Replicator registred at" + replicatorRegistrationId);
		// Register mob
		EntityRegistry.registerGlobalEntityID(ReplicatorEntity.class, "EAReplicator", replicatorRegistrationId, redColor, orangeColor);

		// Localize mob name
		LanguageRegistry.instance().addStringLocalization("entity.EAReplicator.name", "en_US", "Replicator");

		ReplicatorEntity.dropList.add(findItemStack("Iron Dust", 1));
		ReplicatorEntity.dropList.add(findItemStack("Copper Dust", 1));
		ReplicatorEntity.dropList.add(findItemStack("Gold Dust", 1));
		ReplicatorEntity.dropList.add(new ItemStack(Items.redstone));
		ReplicatorEntity.dropList.add(new ItemStack(Items.glowstone_dust));
		// Add mob spawn
		// EntityRegistry.addSpawn(ReplicatorEntity.class, 1, 1, 2, EnumCreatureType.monster, BiomeGenBase.plains);

	}

	private void regenOreScannerFactors() {
		PortableOreScannerItem.RenderStorage.blockKeyFactor = null;

		oreScannerConfig.clear();

		if (addOtherModOreToXRay) {
			for (String name : OreDictionary.getOreNames()) {
				// Utils.println(name + " " +
				// OreDictionary.getOreID(name));
				if (name.startsWith("ore")) {
					for (ItemStack stack : OreDictionary.getOres(name)) {
						int id = Utils.getItemId(stack) + 4096 * stack.getItem().getMetadata(stack.getItemDamage());
						// Utils.println(OreDictionary.getOreID(name));
						boolean find = false;
						for (OreScannerConfigElement c : oreScannerConfig) {
							if (c.blockKey == id) {
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

		oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.coal_ore), 5 / 100f));
		oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.iron_ore), 15 / 100f));
		oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.gold_ore), 40 / 100f));
		oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.lapis_ore), 40 / 100f));
		oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.redstone_ore), 40 / 100f));
		oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.diamond_ore), 100 / 100f));
		oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.emerald_ore), 40 / 100f));

		oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(this.oreBlock) + (1 << 12), 10 / 100f));
		oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(this.oreBlock) + (4 << 12), 20 / 100f));
		oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(this.oreBlock) + (5 << 12), 20 / 100f));
		oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(this.oreBlock) + (6 << 12), 20 / 100f));
	}

	public static double getSmallRs() {
		return instance.lowVoltageCableDescriptor.electricalRs;
	}

	public static void applySmallRs(NbtElectricalLoad aLoad) {
		instance.lowVoltageCableDescriptor.applyTo(aLoad);
	}
	public static void applySmallRs(Resistor r) {
		instance.lowVoltageCableDescriptor.applyTo(r);	
	}
	public ItemStack findItemStack(String name, int stackSize) {
		ItemStack stack = GameRegistry.findItemStack("Eln", name, stackSize);
		if (stack == null) {
			stack = dictionnaryOreFromMod.get(name);
			stack = Utils.newItemStack(Item.getIdFromItem(stack.getItem()), stackSize, stack.getItemDamage());
		}
		return stack;
	}

	public ItemStack findItemStack(String name) {
		return findItemStack(name, 1);
	}




}