package mods.eln;






import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.bouncycastle.asn1.esf.CompleteRevocationRefs;


import mods.eln.TreeResinCollector.TreeResinCollectorBlock;
import mods.eln.TreeResinCollector.TreeResinCollectorDescriptor;
import mods.eln.TreeResinCollector.TreeResinCollectorTileEntity;
import mods.eln.autominer.AutoMinerDescriptor;
import mods.eln.battery.BatteryDescriptor;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientKeyHandler;
import mods.eln.client.ClientProxy;
import mods.eln.client.FrameTime;
import mods.eln.client.SoundLoader;
import mods.eln.diode.DiodeDescriptor;
import mods.eln.electricalalarm.ElectricalAlarmDescriptor;
import mods.eln.electricalantennarx.ElectricalAntennaRxDescriptor;
import mods.eln.electricalantennatx.ElectricalAntennaTxDescriptor;
import mods.eln.electricalbreaker.ElectricalBreakerDescriptor;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.electricaldatalogger.DataLogsPrintDescriptor;
import mods.eln.electricaldatalogger.ElectricalDataLoggerDescriptor;
import mods.eln.electricalfurnace.ElectricalFurnaceDescriptor;
import mods.eln.electricalfurnace.ElectricalFurnaceElement;
import mods.eln.electricalfurnace.ElectricalFurnaceRender;
import mods.eln.electricalgatesource.ElectricalGateSourceDescriptor;
import mods.eln.electricallightsensor.ElectricalLightSensorDescriptor;
import mods.eln.electricalmachine.ElectricalMachineDescriptor;
import mods.eln.electricalmachine.MaceratorDescriptor;
import mods.eln.electricalredstoneinput.ElectricalRedstoneInputDescriptor;
import mods.eln.electricalredstoneoutput.ElectricalRedstoneOutputDescriptor;
import mods.eln.electricalrelay.ElectricalRelayDescriptor;
import mods.eln.electricalrelay.ElectricalRelayElement;
import mods.eln.electricalsource.ElectricalSourceElement;
import mods.eln.electricalsource.ElectricalSourceRender;
import mods.eln.electricalswitch.ElectricalSwitchDescriptor;
import mods.eln.electricaltimout.ElectricalTimeoutDescriptor;
import mods.eln.electricalvumeter.ElectricalVuMeterDescriptor;
import mods.eln.electricasensor.ElectricalSensorDescriptor;
import mods.eln.elnhttpserver.ElnHttpServer;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment;
import mods.eln.generic.SharedItem;
import mods.eln.ghost.GhostBlock;
import mods.eln.ghost.GhostEntity;
import mods.eln.ghost.GhostGroup;
import mods.eln.ghost.GhostManager;
import mods.eln.groundcable.GroundCableElement;
import mods.eln.groundcable.GroundCableRender;
import mods.eln.heatfurnace.HeatFurnaceDescriptor;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.heatfurnace.HeatFurnaceRender;
import mods.eln.intelligenttransformer.IntelligentTransformerElement;
import mods.eln.intelligenttransformer.IntelligentTransformerRender;
import mods.eln.item.BrushDescriptor;
import mods.eln.item.CombustionChamber;
import mods.eln.item.DynamoDescriptor;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.item.ElectricalMotorDescriptor;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.item.HeatingCorpElement;
import mods.eln.item.LampDescriptor;
import mods.eln.item.MaceratorSorterDescriptor;
import mods.eln.item.MachineBoosterDescriptor;
import mods.eln.item.MeterItemArmor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.item.OreScanner;
import mods.eln.item.OverHeatingProtectionDescriptor;
import mods.eln.item.OverVoltageProtectionDescriptor;
import mods.eln.item.SixNodeCacheItem;
import mods.eln.item.SolarTrackerDescriptor;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.ToolsSetItem;
import mods.eln.item.TreeResin;
import mods.eln.item.WindRotorDescriptor;
import mods.eln.item.LampDescriptor.Type;
import mods.eln.item.WindRotorDescriptor.WindRotorAxeType;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorAnalogDescriptor;
import mods.eln.item.regulator.RegulatorOnOffDescriptor;
import mods.eln.lampsocket.LampSocketDescriptor;
import mods.eln.lampsocket.LampSocketType;
import mods.eln.lampsocket.LightBlock;
import mods.eln.lampsocket.LightBlockEntity;

import mods.eln.misc.FunctionTable;
import mods.eln.misc.FunctionTableYProtect;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3DFolder;
import mods.eln.misc.Recipe;
import mods.eln.misc.RecipesList;
import mods.eln.misc.Utils;
import mods.eln.mppt.MpptDescriptor;
import mods.eln.node.NodeBlock;
import mods.eln.node.NodeBlockItemWithSubTypes;
import mods.eln.node.NodeManager;
import mods.eln.node.NodeServer;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeBlock;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.node.SixNodeEntity;
import mods.eln.node.SixNodeItem;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeBlock;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeEntity;
import mods.eln.node.TransparentNodeItem;
import mods.eln.ore.OreBlock;
import mods.eln.ore.OreDescriptor;
import mods.eln.ore.OreItem;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.RegulatorType;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.ThermalLoadInitializerByPowerDrop;
import mods.eln.sim.ThermalRegulator;
import mods.eln.solarpannel.SolarPannelDescriptor;
import mods.eln.thermalcable.ThermalCableDescriptor;
import mods.eln.thermaldissipatoractive.ThermalDissipatorActiveDescriptor;
import mods.eln.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor;
import mods.eln.thermalsensor.ThermalSensorDescriptor;
import mods.eln.transformer.TransformerElement;
import mods.eln.transformer.TransformerRender;
import mods.eln.turbine.TurbineCoreDescriptor;
import mods.eln.turbine.TurbineDescriptor;
import mods.eln.turbine.TurbineElement;
import mods.eln.turbine.TurbineRender;
import mods.eln.windturbine.WindTurbineDescriptor;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.ModLoader;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeDummyContainer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;




@Mod(modid="Eln", name="Eln", version="0.0.1")
@NetworkMod(clientSideRequired=true, serverSideRequired=false, 
				channels={"miaouMod"}, packetHandler = PacketHandler.class)
public class Eln {
	/*
	Eln()
	{
		instance = this;
	}*/
	public static String channelName = "miaouMod";
	
	public static final double networkSerializeValueFactor = 100.0;

	
	public static final byte packetNodeSerialized24bitPosition = 11;
	public static final byte packetNodeSerialized48bitPosition = 12;
	public static final byte packetNodeRefreshRequest = 13;
	public static final byte packetPlayerKey = 14;
	public static final byte packetNodeSingleSerialized = 15;
	public static final byte packetPublishForNode = 16;
	public static final byte packetOpenLocalGui = 17;
	public static final byte packetForClientNode = 18;
	


	

	
	static PacketHandler packetHandler;
	static NodeServer nodeServer;
	public static ClientKeyHandler clientKeyHandler;
	public static GhostManager ghostManager;
	private static NodeManager nodeManager;
	public static PlayerManager playerManager;
	
	public static Simulator simulator = null;

	 public static CreativeTabs creativeTab;
	
	
	public final static int blocBaseId = 220;
	public final static int itemBaseId = 7260;

	public static  Item voltMeterHelmet;
	public static  Item thermoMeterHelmet;
	public static  Item currentMeterHelmet;
	public static Item toolsSetItem;
	public static Item brushItem;
//	public static LampItem lampItem;

/*	public static GenericItemUsingDamage<HeatingCorpElement> heatingCorpItem;
	public static GenericItemUsingDamage<ThermalIsolatorElement> thermalIsolatorItem;
	public static GenericItemUsingDamage<RegulatorElement> regulatorItem;*/

	public static SharedItem sharedItem;

	
	public static  SixNodeBlock sixNodeBlock;
	public static  TransparentNodeBlock transparentNodeBlock;
	public static  OreBlock oreBlock;
	public static  GhostBlock ghostBlock;
	public static  LightBlock lightBlock;
	
	public static SixNodeItem sixNodeItem; 
	public static TransparentNodeItem transparentNodeItem;
	public static OreItem oreItem;
//	public static TreeResinCollectorBlock treeResinCollectorBlock;
	
	// The instance of your mod that Forge uses.
    @Instance("Eln")
    public static Eln instance;
    
    // Says where the client and server 'proxy' code is loaded.
    @SidedProxy(clientSide="mods.eln.client.ClientProxy", serverSide="mods.eln.CommonProxy")
    public static CommonProxy proxy;
    
    public int electricalOverSampling,thermalOverSampling = 1,commonOverSampling = 20;

	
    public ElectricalCableDescriptor highVoltageCableDescriptor;
    public ElectricalCableDescriptor signalCableDescriptor;
    public ElectricalCableDescriptor lowVoltageCableDescriptor;
    public ElectricalCableDescriptor meduimVoltageCableDescriptor;
    
    int creativeTabId;
    int brushItemId;
    
    int oreId;
    int sharedItemId;
    int transparentNodeBlockId;
    int SixNodeBlockId;
    int ghostBlockId;
    public static int lightBlockId;
    
  //  int TreeResinCollectorId;
    
    
    public static Obj3DFolder obj = new Obj3DFolder();
    
    
    @PreInit
    public void preInit(FMLPreInitializationEvent event) {
    	
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (side == Side.CLIENT)
			MinecraftForge.EVENT_BUS.register(new SoundLoader());

    	
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();

        creativeTabId = config.getItem("itemCreativeTab", itemBaseId  -1).getInt();
        brushItemId = config.getItem("brushItem", itemBaseId  -1).getInt();
        sharedItemId = config.getItem("sharedItem", itemBaseId + 18).getInt();
        
        oreId = config.getTerrainBlock("ELN", "OreBlock", blocBaseId + 7, "choubakaka").getInt();
        transparentNodeBlockId = config.getTerrainBlock("ELN", "transparentNodeBlock", blocBaseId + 6, "choubakaka").getInt();
        SixNodeBlockId = config.getTerrainBlock("ELN", "sixNodeBlock", blocBaseId + 1, "choubakaka").getInt();
    	
        ghostBlockId = config.getTerrainBlock("ELN", "GhostBlock", blocBaseId + 8, "choubakaka").getInt();
       
        lightBlockId = config.getTerrainBlock("ELN", "LightBlock", blocBaseId + 2, "choubakaka").getInt();
        
       // TreeResinCollectorId  = config.getTerrainBlock("ELN", "TreeResinCollectorId", blocBaseId + 9, "choubakaka").getInt();
        /*
    	sixNodeBlock = (SixNodeBlock) new SixNodeBlock(SixNodeBlockId, Material.ground,SixNodeEntity.class).setCreativeTab(creativeTab);
      	transparentNodeBlock = (TransparentNodeBlock) new TransparentNodeBlock(transparentNodeBlockId, Material.ground,TransparentNodeEntity.class).setCreativeTab(creativeTab);
      	oreBlock = (OreBlock) new OreBlock(oreId).setCreativeTab(creativeTab);
      	ghostBlock = (GhostBlock) new GhostBlock(ghostBlockId);


    	multimeterItem = new GenericItem(config.getItem("multimeterItem", itemBaseId + 0).getInt())
    	.setCreativeTab(creativeTab).setMaxStackSize(1).setUnlocalizedName("Multimeter");
    	allMeterItem = new GenericItem(config.getItem("allMeterItem", itemBaseId + 5).getInt())
    	.setCreativeTab(creativeTab).setMaxStackSize(1).setUnlocalizedName("AllMeterItem");

    	voltMeterHelmet = (new MeterItemArmor(config.getItem("voltMeterHelmet", itemBaseId + 6).getInt(), EnumArmorMaterial.CLOTH, 2, 0))
    			.setUnlocalizedName("Voltmeter Helmet").setCreativeTab(creativeTab);
    	thermoMeterHelmet = (new MeterItemArmor(config.getItem("thermoMeterHelmet", itemBaseId + 7).getInt(), EnumArmorMaterial.CLOTH, 2, 0))
    			.setUnlocalizedName("Thermometer Helmet").setCreativeTab(creativeTab);
    	currentMeterHelmet = (new MeterItemArmor(config.getItem("currentMeterHelmet", itemBaseId + 8).getInt(), EnumArmorMaterial.CLOTH, 2, 0))
    			.setUnlocalizedName("Current Helmet").setCreativeTab(creativeTab);
    	
    	
    	
    	

    	
    	
    	
    	
    	toolsSetItem = new ToolsSetItem(config.getItem("toolsSetItem", itemBaseId + 10).getInt())
    	.setCreativeTab(creativeTab).setMaxStackSize(1).setUnlocalizedName("ToolsSetItem");

    	*/


    	
    	electricalOverSampling = config.get("Simulator", "ElectricalHz", 8000).getInt() / commonOverSampling / 20;
    	if(electricalOverSampling<1)electricalOverSampling = 1;
    	
        config.save();
    }
    public FrameTime frameTime;
    @Init
    public void load(FMLInitializationEvent event) {
    	
      	simulator = new Simulator(20,commonOverSampling,electricalOverSampling,thermalOverSampling);
      	nodeServer = new NodeServer();
      	frameTime = new FrameTime();
      	packetHandler = new PacketHandler();
      	//ForgeDummyContainer
      	instance = this;
      	NetworkRegistry.instance().registerGuiHandler(this, new GuiHandler());
      	
       	Item itemCreativeTab = new Item(creativeTabId)
    	.setMaxStackSize(1).setUnlocalizedName("eln:ElnCreativeTab");
    	creativeTab = new GenericCreativeTab("Eln", itemCreativeTab);
    	
      	oreBlock = (OreBlock) new OreBlock(oreId).setCreativeTab(creativeTab);
    	
    	

       	sharedItem = (SharedItem) new SharedItem(sharedItemId)
    	.setCreativeTab(creativeTab).setMaxStackSize(64).setUnlocalizedName("sharedItem");
      	
       	transparentNodeBlock = (TransparentNodeBlock) new TransparentNodeBlock(transparentNodeBlockId, Material.ground,TransparentNodeEntity.class).setCreativeTab(creativeTab);
       	sixNodeBlock = (SixNodeBlock) new SixNodeBlock(SixNodeBlockId, Material.ground,SixNodeEntity.class).setCreativeTab(creativeTab);
       	
       	ghostBlock = (GhostBlock) new GhostBlock(ghostBlockId);
       	lightBlock = (LightBlock) new LightBlock(lightBlockId);
       	
       	obj.loadFolder("/mods/eln/model");
      // 	Obj3DFolder miaou = new Obj3DFolder();
     //  	miaou.loadFolder("/mods/eln/model");
       	
       	
       
		//GameRegistry.addRecipe(new ItemStack(treeResinCollectorBlock,1), "X", Character.valueOf('X'), Block.dirt);
      	/*
      	LanguageRegistry.instance().addStringLocalization("itemGroup.ElnCreativeTab", "en_US", "Eln tab");
    	LanguageRegistry.addName(multimeterItem, "Multimeter");
    	LanguageRegistry.addName(allMeterItem, "AllMeterItem");
    	LanguageRegistry.addName(toolsSetItem, "Tools set");
    	LanguageRegistry.addName(voltMeterHelmet,"VoltMeter Helmet");
    	LanguageRegistry.addName(thermoMeterHelmet,"ThermoMeter Helmet");
    	LanguageRegistry.addName(currentMeterHelmet,"Current Helmet");
        	
*/
    	GameRegistry.registerBlock(sixNodeBlock, SixNodeItem.class);
		TileEntity.addMapping(SixNodeEntity.class,"SixNodeEntity");   

    	GameRegistry.registerBlock(transparentNodeBlock, TransparentNodeItem.class);
		TileEntity.addMapping(TransparentNodeEntity.class,"TransparentNodeEntity");   

    	GameRegistry.registerBlock(oreBlock,OreItem.class);

		TileEntity.addMapping(GhostEntity.class,"ghostTileEntity");   
		TileEntity.addMapping(LightBlockEntity.class,"LightBlockEntity");   

		
       	NodeManager.registerBlock(sixNodeBlock,SixNode.class);
       	NodeManager.registerBlock(transparentNodeBlock,TransparentNode.class);

       	sixNodeItem = (SixNodeItem) Item.itemsList[sixNodeBlock.blockID];
       	transparentNodeItem = (TransparentNodeItem) Item.itemsList[transparentNodeBlock.blockID];
       	
       	oreItem = (OreItem) Item.itemsList[oreBlock.blockID];
       	/*
       	
    	int id = 0,subId = 0,completId;
    	String name;
    	
*/
	
    	registerOre();

 	
      	
    	
    	registerGround(2);
    	registerElectricalSource(3);
    	registerElectricalCable(32);
    	registerThermalCable(48);
    	registerLampSocket(64);
       	registerElectricalDataLogger(93);                     
       	registerElectricalRelay(94);                     
       	registerElectricalGateSource(95);                     
       	registerDiode(96);
       	registerSwitch(97);                     
       	registerElectricalBreaker(98);                     
       	registerElectricalSensor(100);                     
       	registerThermalSensor(101);                     
       	registerElectricalVuMeter(102);                     
       	registerElectricalAlarm(103);                     
       	registerElectricalLightSensor(104); 
       	registerElectricalRedstone(108);
       	registerElectricalGate(109);
    	registerTreeResinCollector(116);
       	
    	registerTransformer(2);
    	registerHeatFurnace(3);
    	registerTurbine(4);
    	registerIntelligentTransformer(5);
    	registerMppt(6);
    	registerElectricalAntenna(7);
    	registerBattery(16);
    	registerElectricalFurnace(32);
    	registerMacerator(33);
    	registerExtractor(34);
    	registerCompressor(35);
    	registermagnetiser(36);
    	registerAutoMiner(42);
    	registerSolarPannel(48);
    	registerWindTurbine(49);
    	registerThermalDissipatorPassiveAndActive(64);
    	    	
    	
    	
    	
    	registerHeatingCorp(1);
    	registerThermalIsolator(2);
    	registerRegulatorItem(3);
    	registerLampItem(4);
    	registerProtection(5);
    	registerCombustionChamber(6);
    	registerFerromagneticCore(7);   	
      	registerIngot(8);
      	registerDust(9);
      	registerElectricalMotor(10);
      	registerSolarTracker(11);
      	registerDynamo(12);
      	registerWindRotor(13);
      	registerMeter(14);
      	registerElectricalDrill(15);
      	registerOreScanner(16);
      	registerMiningPipe(17);
      	registerSixNodeCache(18);
      	registerTreeResinAndRubber(64);
      	registerRawCable(65);
      	registerBrush(119);
    	registerMiscItem(120);
      	
      	
    	recipeGround();
    	recipeElectricalSource();
    	recipeElectricalCable();
    	recipeThermalCable();
    	recipeLampSocket();
       	recipeDiode();
       	recipeSwitch();
       	recipeMachine();
    	recipeTransformer();
    	recipeHeatFurnace();
    	recipeTurbine();
    	recipeBattery();
    	recipeElectricalFurnace();
        recipeAutoMiner();
    	recipeSolarPannel();
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
      
      	
      	
      	recipeFurnace();
    	recipeMacerator();
       	recipeExtractor();
       	recipeCompressor();
       	recipemagnetiser();

		
		proxy.registerRenderers();
		
		
		
		
		try {
			elnHttpServer = new ElnHttpServer();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    }
    ElnHttpServer elnHttpServer;
    @PostInit
    public void postInit(FMLPostInitializationEvent event) {
            // Stub Method
    	
    }
    

    @cpw.mods.fml.common.Mod.ServerStopping                /* Remember to use the right event! */
    public void onServerStopping(FMLServerStoppingEvent ev) {
    	LightBlockEntity.observers.clear();
    	playerManager = null;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
    	WorldServer worldServer = server.worldServers[0];
        simulator.init();
        nodeServer.init();
    	nodeManager = null;
    	ghostManager = null;
    }  
    @ServerStarting                 /* Remember to use the right event! */
    public void onServerStarting(FMLServerStartingEvent ev) {
    	LightBlockEntity.observers.clear();
    	playerManager = new PlayerManager();
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
    	WorldServer worldServer = server.worldServers[0];
        simulator.init();

        ghostManager = (GhostManager) worldServer.mapStorage.loadData(GhostManager.class, "GhostManager");
    	if(ghostManager == null)
		{
    		ghostManager = new GhostManager("GhostManager");
			worldServer.mapStorage.setData("GhostManager", ghostManager);
		}        
    	ghostManager.init();
        
    	nodeManager = (NodeManager) worldServer.mapStorage.loadData(NodeManager.class, "NodeManager");
    	if(nodeManager == null)
		{
			nodeManager = new NodeManager("NodeManager");
			worldServer.mapStorage.setData("NodeManager", nodeManager);
		}
    	
    	
    	nodeServer.init(); 
    	

    }     
    
    @cpw.mods.fml.common.Mod.ServerStopping
    public void ServerStopping(FMLServerStoppingEvent ev)
    {
    	playerManager = null;

        nodeServer.stop();

    	simulator.stop();


    }

    public CableRenderDescriptor stdCableRenderSignal;  
    public CableRenderDescriptor stdCableRender50V;
    public CableRenderDescriptor stdCableRender200V;
    public CableRenderDescriptor stdCableRender800V;
    
    
    
    public static double gateOutputCurrent = 0.100;
    public static final double SVU = 50   ,SVII = gateOutputCurrent/10 , SVUinv = 1.0/SVU;
    public static final double LVU = 50;
    public static final double MVU = 200;
    public static final double HVU = 800;
    
    public static final double SVP = gateOutputCurrent*SVU;
    public static final double LVP = 1000;
    public static final double MVP = 2000;
    public static final double HVP = 4000;
    
    public static final double cableWarmLimit = 130;
    public static final double cableThermalConductionTao  = 0.5;
    public static final ThermalLoadInitializer cableThermalLoadInitializer = new ThermalLoadInitializer(cableWarmLimit, -100, 30, cableThermalConductionTao);
    public static final ThermalLoadInitializer sixNodeThermalLoadInitializer = new ThermalLoadInitializer(cableWarmLimit, -100, 30, 1000);
    void registerElectricalCable(int id)
    {
    	int subId,completId;
    	String name;

    	CableRenderDescriptor render;
    	ElectricalCableDescriptor desc;
    	{
    		subId = 0;

	    	name = "Signal cable";

	    	stdCableRenderSignal = new CableRenderDescriptor(ClientProxy.CABLE_PNG,1,0.95f);
	    	
			desc = new ElectricalCableDescriptor(
									name,
									stdCableRenderSignal,
									"For signal transmition",
									true);
			
			signalCableDescriptor = desc;
			
			desc.setPhysicalConstantLikeNormalCable(
					SVU,SVP,0.02/20*gateOutputCurrent/SVII,// electricalNominalVoltage, electricalNominalPower, electricalNominalPowerDrop,
					SVU*1.3,SVP*1.2,// electricalMaximalVoltage, electricalMaximalPower,
					0.5,// electricalOverVoltageStartPowerLost,
					cableWarmLimit,-100,// thermalWarmLimit, thermalCoolLimit,
					10,1// thermalNominalHeatTime, thermalConductivityTao			
					); 
			

			
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
			//GameRegistry.registerCustomItemStack(name, desc.newItemStack(1));

		}
    	
    	{
    		subId = 4;
    		
	    	name = "Low voltage cable";
		
	    	stdCableRender50V = new CableRenderDescriptor(ClientProxy.CABLE_PNG,2,1.5f);
	    	
			desc = new ElectricalCableDescriptor(
					name,
					stdCableRender50V,
					"For low voltage with high current",
					false);

			lowVoltageCableDescriptor = desc;
			
			desc.setPhysicalConstantLikeNormalCable(
					LVU,LVP,0.2/20,// electricalNominalVoltage, electricalNominalPower, electricalNominalPowerDrop,
					LVU*1.3,LVP*1.2,// electricalMaximalVoltage, electricalMaximalPower,
					20,// electricalOverVoltageStartPowerLost,
					cableWarmLimit,-100,// thermalWarmLimit, thermalCoolLimit,
					10,cableThermalConductionTao// thermalNominalHeatTime, thermalConductivityTao			
					); 
			
			
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
			

		}
    	
    	{
    		subId = 8;

	    	name = "Medium voltage cable";

	    	stdCableRender200V = new CableRenderDescriptor(ClientProxy.CABLE_PNG,3,1.95f);

			desc = new ElectricalCableDescriptor(
					name,
					stdCableRender200V,
					"miaou",
					false);


			meduimVoltageCableDescriptor = desc;
			
			desc.setPhysicalConstantLikeNormalCable(
					MVU,MVP,0.15/20,// electricalNominalVoltage, electricalNominalPower, electricalNominalPowerDrop,
					MVU*1.3,MVP*1.2,// electricalMaximalVoltage, electricalMaximalPower,
					30,// electricalOverVoltageStartPowerLost,
					cableWarmLimit,-100,// thermalWarmLimit, thermalCoolLimit,
					10,cableThermalConductionTao// thermalNominalHeatTime, thermalConductivityTao			
					); 
			
			
			sixNodeItem.addDescriptor(subId + (id << 6), desc);

		}
    	{
    		subId = 12;
    		
    	//	highVoltageCableId = subId;
	    	name = "High voltage cable";
	    	
	    	stdCableRender800V = new CableRenderDescriptor(ClientProxy.CABLE_PNG,4,2.95f);

			desc = new ElectricalCableDescriptor(
					name,
					stdCableRender800V,
					"miaou2",
					false);


			highVoltageCableDescriptor = desc;
			
			desc.setPhysicalConstantLikeNormalCable(
					HVU,HVP,0.1/20,// electricalNominalVoltage, electricalNominalPower, electricalNominalPowerDrop,
					HVU*1.3,HVP*1.2,// electricalMaximalVoltage, electricalMaximalPower,
					40,// electricalOverVoltageStartPowerLost,
					cableWarmLimit,-100,// thermalWarmLimit, thermalCoolLimit,
					10,cableThermalConductionTao// thermalNominalHeatTime, thermalConductivityTao			
					); 
			
			
			sixNodeItem.addDescriptor(subId + (id << 6), desc);

		}    	
    }
    
    
    void registerThermalCable(int id)
	{
    	int subId,completId;
    	String name;
			
    	{
    		subId = 0;
    		
	    	name = "Cooper thermal cable";

	    	ThermalCableDescriptor desc = new ThermalCableDescriptor(
	    					name,
	    					1000,-200, //thermalWarmLimit, thermalCoolLimit,
	    					500,2000, //thermalStdT, thermalStdPower, 
	    					4,400,0.1,//thermalStdDrop, thermalStdLost, thermalTao,
	    					new CableRenderDescriptor(ClientProxy.THERMALCABLE_PNG,4,4),
	    					"Miaou !");//description
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
    	{
    		subId = 1;
    		
	    	name = "Isolated cooper thermal cable";

	    	ThermalCableDescriptor desc = new ThermalCableDescriptor(
	    					name,
	    					1000,-200, //thermalWarmLimit, thermalCoolLimit,
	    					500,2000, //thermalStdT, thermalStdPower, 
	    					4,10,0.1,//thermalStdDrop, thermalStdLost, thermalTao,
	    					new CableRenderDescriptor(ClientProxy.THERMALCABLE_PNG,4,4),
	    					"Miaou !");//description
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
	}
    void registerBattery(int id)
	{
    	int subId,completId;
    	String name;
	
    	double[]  voltageFunctionTable = {0.000,0.9,1.0,1.025,1.04,1.05,2.0};
    	FunctionTable voltageFunction = new FunctionTable(voltageFunctionTable,6.0/5);
    	double stdDischargeTime = 4*60;
    	double stdU = LVU;
    	double stdP = LVP/4;
    	double stdHalfLife = Utils.minecraftDay * 2;

    	{
    		subId = 0;
	    	name = "Cost oriented battery";

    		BatteryDescriptor desc = new BatteryDescriptor(
    				name,"LowCostBattery",
    				voltageFunction,
    				stdU,stdP*1.2,0.001,	   // electricalU, electricalPMax,electricalDischargeRate
    				stdP,stdDischargeTime,0.99,stdHalfLife,  //   electricalStdP, electricalStdDischargeTime, electricalStdEfficiency, electricalStdHalfLife,
					50,60,-100,  // thermalHeatTime, thermalWarmLimit, thermalCoolLimit,
					"Cheap battery"  // name, description)
				);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
    	{
    		subId = 1;
	    	name = "Capacity oriented battery";

    		BatteryDescriptor desc = new BatteryDescriptor(
    				name,"HighCapacityBattery",
    				voltageFunction,
    				stdU/4,stdP/2*1.2,0.001,	   // electricalU, electricalPMax,electricalDischargeRate
    				stdP/2,stdDischargeTime*8,0.99,stdHalfLife,  //   electricalStdP, electricalStdDischargeTime, electricalStdEfficiency, electricalStdHalfLife,
					50,60,-100,  // thermalHeatTime, thermalWarmLimit, thermalCoolLimit,
					"the battery"  // name, description)
				);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
    	{
    		subId = 2;
	    	name = "Voltage oriented battery";

    		BatteryDescriptor desc = new BatteryDescriptor(
    				name,"HighVoltageBattery",
    				voltageFunction,
    				stdU*4,stdP*1.2,0.001,	   // electricalU, electricalPMax,electricalDischargeRate
    				stdP,stdDischargeTime,0.99,stdHalfLife,  //   electricalStdP, electricalStdDischargeTime, electricalStdEfficiency, electricalStdHalfLife,
					50,60,-100,  // thermalHeatTime, thermalWarmLimit, thermalCoolLimit,
					"the battery"  // name, description)
				);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}

    	{
    		subId = 3;
	    	name = "Current oriented battery";

    		BatteryDescriptor desc = new BatteryDescriptor(
    				name,"HighCurrentBattery",
    				voltageFunction,
    				stdU,stdP*1.2*4,0.001,	   // electricalU, electricalPMax,electricalDischargeRate
    				stdP*4,stdDischargeTime/6,0.99,stdHalfLife,  //   electricalStdP, electricalStdDischargeTime, electricalStdEfficiency, electricalStdHalfLife,
					50,60,-100,  // thermalHeatTime, thermalWarmLimit, thermalCoolLimit,
					"the battery"  // name, description)
				);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
    	{
    		subId = 4;
	    	name = "Life oriented battery";

    		BatteryDescriptor desc = new BatteryDescriptor(
    				name,"LongLifeBattery",
    				voltageFunction,
    				stdU,stdP*1.2,0.001,	   // electricalU, electricalPMax,electricalDischargeRate
    				stdP,stdDischargeTime,0.99,stdHalfLife*8,  //   electricalStdP, electricalStdDischargeTime, electricalStdEfficiency, electricalStdHalfLife,
					50,60,-100,  // thermalHeatTime, thermalWarmLimit, thermalCoolLimit,
					"the battery"  // name, description)
				);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
	}
    
    
    
    
    
    void registerGround(int id)
    {
    	int subId,completId;
    	String name;

    	{
    		subId = 0;
	    	name = "Ground cable";

			SixNodeDescriptor desc = new SixNodeDescriptor(
					name,
					GroundCableElement.class, GroundCableRender.class);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
	}
    void registerElectricalSource(int id)
    {
    	int subId,completId;
    	String name;

    	{
    		subId = 0;
	    	name = "ElectricalSource";

			SixNodeDescriptor desc = new SixNodeDescriptor(
					name,
					ElectricalSourceElement.class, ElectricalSourceRender.class);
			sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}

	}
    	

    	
    void registerLampSocket(int id)
    {
    	int subId,completId;
    	String name;

    	{
    		subId = 0;
    		
	    	name = "Lamp socket A";

	    	LampSocketDescriptor desc = 	new LampSocketDescriptor(
	    			name,"ClassicLampSocket",
					LampSocketType.Douille, //LampSocketType socketType
					0,
					0,0,0
					);
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}   
    	{
    		subId = 1;
    		
	    	name = "Lamp socket B projector";

	    	LampSocketDescriptor desc = 	new LampSocketDescriptor(
	    			name,"ClassicLampSocket",
					LampSocketType.Douille, //LampSocketType socketType
					10,
					-90,90,0
					);
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}   
	}
	
    void registerDiode(int id)
    {
    	int subId,completId;
    	String name;
    	IFunction function;
    	FunctionTableYProtect baseFunction = new FunctionTableYProtect(new double[]{	0.0, 0.01, 0.03, 0.1,
				0.2, 0.4, 0.8, 1.2}, 1.0, 0, 5);
    	
    	
    	{
    		subId = 0;
    		
	    	name = "10A diode";
	    	
	    	function = new FunctionTableYProtect(new double[]{	0.0, 0.1, 0.3, 1.0,
	    													2.0, 4.0, 8.0, 12.0}, 1.0, 0, 100);
	    	
	    	DiodeDescriptor desc = 	new DiodeDescriptor(
	    			name,//int iconId, String name,
	    			function,
	    			10, //double Imax,
	    			sixNodeThermalLoadInitializer.copy(),
	    			lowVoltageCableDescriptor
	    			
					);
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  
    	
    	 
    	{
    		subId = 1;
    		
	    	name = "25A diode";
	    	
	    	function = new FunctionTableYProtect(new double[]{	0.0, 0.25, 0.75, 2.5,
	    													5.0, 10.0, 20.0, 30.0}, 1.0, 0, 100);
	    	
	    	DiodeDescriptor desc = 	new DiodeDescriptor(
	    			name,//int iconId, String name,
	    			function,
	    			25, //double Imax,
	    			sixNodeThermalLoadInitializer.copy(),
	    			lowVoltageCableDescriptor
					);
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  
    	
    	{
    		subId = 8;
    		
	    	name = "Signal diode";
	    	
	    	function = baseFunction.duplicate(1.0, 0.1);
	    	
	    	DiodeDescriptor desc = 	new DiodeDescriptor(
	    			name,//int iconId, String name,
	    			function,
	    			0.1, //double Imax,
	    			sixNodeThermalLoadInitializer.copy(),
	    			signalCableDescriptor
	    			
					);
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  
	}	    	
	
    void registerSwitch(int id)
    {
    	int subId,completId;
    	String name;
    	IFunction function;
    	ElectricalSwitchDescriptor desc;
    	
    	{
    		subId = 0;
    		
	    	name = "High voltage switch";
	    	
	    	desc = new ElectricalSwitchDescriptor(
	    			name,stdCableRender800V,"HighVoltageSwitch",
	    			HVU,HVP,0.02,//nominalVoltage, nominalPower, nominalDropFactor,
	    			HVU*1.5,HVP*1.2,//maximalVoltage, maximalPower
	    			cableThermalLoadInitializer.copy(),
	    			false
	    			);
	    	
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  
    	{
    		subId = 1;
    		
	    	name = "Low voltage switch";
	    	
	    	desc = new ElectricalSwitchDescriptor(
	    			name,stdCableRender50V,"LowVoltageSwitch",
	    			LVU,LVP,0.02,//nominalVoltage, nominalPower, nominalDropFactor,
	    			LVU*1.5,LVP*1.2,//maximalVoltage, maximalPower
	    			cableThermalLoadInitializer.copy(),
	    			false
	    			);
	    	
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}      	
    	{
    		subId = 2;
    		
	    	name = "Medium voltage switch";
	    	
	    	desc = new ElectricalSwitchDescriptor(
	    			name,stdCableRender200V,"LowVoltageSwitch",
	    			MVU,MVP,0.02,//nominalVoltage, nominalPower, nominalDropFactor,
	    			MVU*1.5,MVP*1.2,//maximalVoltage, maximalPower
	    			cableThermalLoadInitializer.copy(),
	    			false
	    			);
	    	
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}      	
    	{
    		subId = 3;
    		
	    	name = "Signal voltage switch";
	    	
	    	desc = new ElectricalSwitchDescriptor(
	    			name,stdCableRenderSignal,"LowVoltageSwitch",
	    			SVU,SVP,0.02,//nominalVoltage, nominalPower, nominalDropFactor,
	    			SVU*1.5,SVP*1.2,//maximalVoltage, maximalPower
	    			cableThermalLoadInitializer.copy(),
	    			true
	    			);
	    	
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}      	

	}	    	
	
    	
    
	
    void registerElectricalBreaker(int id)
    {
    	int subId,completId;
    	String name;
    	ElectricalBreakerDescriptor desc;
    	
    	{
    		subId = 0;
    		
	    	name = "Electrical breaker";
	    	
	    	desc = new ElectricalBreakerDescriptor(
	    			name
	    			);
	    	
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  

	}	    	
	
    	
   	
	
    void registerElectricalSensor(int id)
    {
    	int subId,completId;
    	String name;
    	ElectricalSensorDescriptor desc;
    	
    	{
    		subId = 0;
    		
	    	name = "Electrical sensor sixNode";
	    	
	    	desc = new ElectricalSensorDescriptor(
	    			name,false
	    			);
	    	
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  
    	{
    		subId = 1;
    		
	    	name = "Voltage sensor sixNode";
	    	
	    	desc = new ElectricalSensorDescriptor(
	    			name,true
	    			);
	    	
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  

	}	
    void registerThermalSensor(int id)
    {
    	int subId,completId;
    	String name;
    	ThermalSensorDescriptor desc;
    	
    	{
    		subId = 0;
    		
	    	name = "Thermal sensor sixNode";
	    	
	    	desc = new ThermalSensorDescriptor(
	    			name,
	    			false
	    			);
	    	
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  
    	{
    		subId = 1;
    		
	    	name = "Temperature sensor sixNode";
	    	
	    	desc = new ThermalSensorDescriptor(
	    			name,
	    			true
	    			);
	    	
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  

	}	
    void registerElectricalVuMeter(int id)
    {
    	int subId,completId;
    	String name;
    	ElectricalVuMeterDescriptor desc;	
    	{
    		subId = 0;    		
	    	name = "Electrical vuMeter";	    	
	    	desc = new ElectricalVuMeterDescriptor(
	    			name,
	    			"Vumeter"
	    			);  		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  
	}	
    
    void registerElectricalAlarm(int id)
    {
    	int subId,completId;
    	String name;
    	ElectricalAlarmDescriptor desc;	
    	{
    		subId = 0;    		
	    	name = "Electrical alarm A";	    	
	    	desc = new ElectricalAlarmDescriptor(
	    			name,
	    			"Vumeter",
	    			"eln.sound.alarma",11,1f
	    			);  		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	} 
    	{
    		subId = 1;    		
	    	name = "Electrical alarm B";	    	
	    	desc = new ElectricalAlarmDescriptor(
	    			name,
	    			"Vumeter",
	    			"eln.sound.smallalarm_critical",1.2,2f
	    			);  		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	} 
	}	
    void registerElectricalLightSensor(int id)
    {
    	int subId,completId;
    	String name;
    	ElectricalLightSensorDescriptor desc;	
    	{
    		subId = 0;    		
	    	name = "Electrical daylight sensor";	    	
	    	desc = new ElectricalLightSensorDescriptor(
	    			name,
	    			"notimplemented"
	    			);  		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  
	}	
        
    void registerElectricalRedstone(int id)
    {
    	int subId,completId;
    	String name;
    	{
        	ElectricalRedstoneInputDescriptor desc;	
    		subId = 0;    		
	    	name = "Electrical redstone input";	    	
	    	desc = new ElectricalRedstoneInputDescriptor(
	    			name,
	    			"notimplemented"
	    			);  		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  

    	{
        	ElectricalRedstoneOutputDescriptor desc;	
    		subId = 1;    		
	    	name = "Electrical redstone output";	    	
	    	desc = new ElectricalRedstoneOutputDescriptor(
	    			name,
	    			"notimplemented"
	    			);  		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  
	}	
    
    void registerElectricalGate(int id)
    {
    	int subId,completId;
    	String name;
    	{
    		ElectricalTimeoutDescriptor desc;
        	subId = 0;
    		
	    	name = "electrical timeout";
	    	
	    	desc = new ElectricalTimeoutDescriptor(
	    			name
	    			);
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  
	}
    void registerElectricalDataLogger(int id)
    {
    	int subId,completId;
    	String name;
    	{
    		ElectricalDataLoggerDescriptor desc;
        	subId = 0;
    		
	    	name = "electrical data logger";
	    	
	    	desc = new ElectricalDataLoggerDescriptor(
	    			name,
	    			true,
	    			"DataloggerCRTFloor",
	    			0f,1f,0f
	    			);
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  
	}
        
    
        
    void registerElectricalRelay(int id)
    {
    	int subId,completId;
    	String name;
    	ElectricalRelayDescriptor desc;
    	
    	{
    		subId = 0;
    		
	    	name = "Low voltage relay";
	    	
	    	desc = new ElectricalRelayDescriptor(
	    			name,
	    			lowVoltageCableDescriptor
	    			);
	    	
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  

	}
        
    void registerElectricalGateSource(int id)
    {
    	int subId,completId;
    	String name;
    	ElectricalGateSourceDescriptor desc;
    	
    	{
    		subId = 0;
    		
	    	name = "Electrical gate source";
	    	
	    	desc = new ElectricalGateSourceDescriptor(
	    			name
	    			);
	    	
    		
	    	sixNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  

	}
    

    void registerTransformer(int id)
    {
    	int subId,completId;
    	String name;

	
    	{
    		subId = 0;
	    	name = "Transformer";

			TransparentNodeDescriptor desc = new TransparentNodeDescriptor(
					name,
					TransformerElement.class, TransformerRender.class);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}

	}

    void registerHeatFurnace(int id)
    {
    	int subId,completId;
    	String name;
    	{
    		subId = 0;
	    	name = "Stone heat furnace";

			HeatFurnaceDescriptor desc = new HeatFurnaceDescriptor(
					name,"stonefurnace",
					1000,Utils.getCoalEnergyReference()*2/3,//double nominalPower, double nominalCombustibleEnergy,
					2,500,//int combustionChamberMax,double combustionChamberPower,
					new ThermalLoadInitializerByPowerDrop(580, -100, 10,10) //thermal
					);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
    	{
    		subId = 1;
	    	name = "Brick heat furnace";

			HeatFurnaceDescriptor desc = new HeatFurnaceDescriptor(
					name,"stonefurnace",
					1500,Utils.getCoalEnergyReference(),//double nominalPower, double nominalCombustibleEnergy,
					2,750,//int combustionChamberMax,double combustionChamberPower,
					new ThermalLoadInitializerByPowerDrop(780, -100, 10,10) //thermal
					);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}


	}
	
    void registerTurbine(int id)
    {
    	int subId,completId;
    	String name;

		FunctionTable TtoU = new FunctionTable(new double[]{0,0.1,0.75,0.90,1.05,1.10,1.15,1.20,1.25}, 8.0/5.0);
	//	FunctionTable TtoU = new FunctionTable(new double[]{0,0.2,0.4,0.6,0.8,1.00,1.2,1.40,1.6}, 8.0/5.0);
		FunctionTable TtoP = new FunctionTable(new double[]{0,0.1,0.25,0.6,0.8,1.0,1.15,1.30,1.40}, 8.0/5.0);
		FunctionTable PoutToPin = new FunctionTable(new double[]{0.0,0.2,0.4,0.6,0.8,1.0,1.3,1.8,2.7}, 8.0/5.0);
	    	
		
		{
    		subId = 0;
	    	name = "Small 50V turbine";
	    	double RsFactor = 0.25;
	    	double nominalU = LVU;
	    	double nominalP = 200;
	    	double nominalDeltaT = 200;
	    	TurbineDescriptor desc = new TurbineDescriptor(
	    			name,"turbine50V","Miaouuuu turbine",//int iconId, String name,String description,
	    			TtoU.duplicate(nominalDeltaT, nominalU),
	    			TtoP.duplicate(nominalDeltaT, nominalP),
	    			PoutToPin.duplicate(nominalP, nominalP),
	    			nominalDeltaT,nominalU,nominalP,nominalP/40,//double nominalDeltaT, double nominalU,nominalP,double nominalPowerLost
					lowVoltageCableDescriptor.electricalRs*RsFactor,lowVoltageCableDescriptor.electricalRp,lowVoltageCableDescriptor.electricalC/RsFactor,//ElectricalCableDescriptor electricalCable,
					5.0,nominalDeltaT/40, //double thermalC,double DeltaTForInput
					nominalP/2
					);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
		{
    		subId = 1;
	    	name = "50V turbine";
	    	double RsFactor = 0.25;
	    	double nominalU = LVU;
	    	double nominalP = 300;
	    	double nominalDeltaT = 250;
	    	TurbineDescriptor desc = new TurbineDescriptor(
	    			name,"turbine50V","Miaouuuu turbine",//int iconId, String name,String description,
	    			TtoU.duplicate(nominalDeltaT, nominalU),
	    			TtoP.duplicate(nominalDeltaT, nominalP),
	    			PoutToPin.duplicate(nominalP, nominalP),
	    			nominalDeltaT,nominalU,nominalP,nominalP/40,//double nominalDeltaT, double nominalU,nominalP,double nominalPowerLost
					lowVoltageCableDescriptor.electricalRs*RsFactor,lowVoltageCableDescriptor.electricalRp,lowVoltageCableDescriptor.electricalC/RsFactor,//ElectricalCableDescriptor electricalCable,
					5.0,nominalDeltaT/40, //double thermalC,double DeltaTForInput
					nominalP/2
					);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
		
		{
    		subId = 8;
	    	name = "200V turbine";
	    	double RsFactor = 0.25;
	    	double nominalU = LVU;
	    	double nominalP = 500;
	    	double nominalDeltaT = 350;
	    	TurbineDescriptor desc = new TurbineDescriptor(
	    			name,"turbine50V","Miaouuuu turbine",//int iconId, String name,String description,
	    			TtoU.duplicate(nominalDeltaT, nominalU),
	    			TtoP.duplicate(nominalDeltaT, nominalP),
	    			PoutToPin.duplicate(nominalP, nominalP),
	    			nominalDeltaT,nominalU,nominalP,nominalP/40,//double nominalDeltaT, double nominalU,nominalP,double nominalPowerLost
					meduimVoltageCableDescriptor.electricalRs*RsFactor,meduimVoltageCableDescriptor.electricalRp,meduimVoltageCableDescriptor.electricalC/RsFactor,//ElectricalCableDescriptor electricalCable,
					5.0,nominalDeltaT/40, //double thermalC,double DeltaTForInput
					nominalP/2
					);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}

/*
		{
    		subId = 1;
	    	name = "50V turbine";
	    	double RsFactor = 0.25;
	    	TurbineDescriptor desc = new TurbineDescriptor(
	    			name,"Miaouuuu turbine",//int iconId, String name,String description,
	    			baseTtoU,//FunctionTable TtoU,
	    			PinToPout,
					300.0,LVU,300,50,//double nominalDeltaT, double nominalU,nominalP,double nominalPowerLost
					lowVoltageCableDescriptor.electricalRs*RsFactor,lowVoltageCableDescriptor.electricalRp,lowVoltageCableDescriptor.electricalC/RsFactor,//ElectricalCableDescriptor electricalCable,
					5.0,15 //double thermalC,double DeltaTForInput
					);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}

		{
    		subId = 2;
	    	name = "Upgraded 50V turbine";
	    	double RsFactor = 0.25;
	    	TurbineDescriptor desc = new TurbineDescriptor(
	    			name,"Miaouuuu turbine",//int iconId, String name,String description,
	    			baseTtoU,//FunctionTable TtoU,
	    			PinToPout,
					400.0,LVU,600,50,//double nominalDeltaT, double nominalU,nominalP,double nominalPowerLost
					lowVoltageCableDescriptor.electricalRs*RsFactor,lowVoltageCableDescriptor.electricalRp,lowVoltageCableDescriptor.electricalC/RsFactor,//ElectricalCableDescriptor electricalCable,
					5.0,20 //double thermalC,double DeltaTForInput
					);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}


*/
	}
	
	
	
    void registerIntelligentTransformer(int id)
    {
    	int subId,completId;
    	String name;
    	{
    		subId = 0;
	    	name = "IntelligentTransformer";

			TransparentNodeDescriptor desc = new TransparentNodeDescriptor(name,
					IntelligentTransformerElement.class, IntelligentTransformerRender.class);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}

	}



    void registerElectricalFurnace(int id)
    {
    	int subId,completId;
    	String name;
    	{
    		subId = 0;
	    	name = "Electrical furnace";
	    	double []PfTTable = new  double[]{10,
					 20,
					 40,
					 80,
					 160,
					 240,
					 360,
					 540,
					 756,
					 1058.4,
					 1481.76};
	    	
	    	double[] thermalPlostfTTable = new double[PfTTable.length];
	    	for(int idx = 0;idx < thermalPlostfTTable.length;idx++)
	    	{
	    		thermalPlostfTTable[idx] = PfTTable[idx] * Math.pow((idx + 1.0) / thermalPlostfTTable.length, 2) * 4;
	    	}
	    	
	    	FunctionTableYProtect PfT = new FunctionTableYProtect(
	    			PfTTable,800.0,0,100000.0);
	    	
	    	FunctionTableYProtect thermalPlostfT = new FunctionTableYProtect(
	    			thermalPlostfTTable,800.0,0.001,10000000.0);
	    	
			ElectricalFurnaceDescriptor desc = new ElectricalFurnaceDescriptor(
					name,
					PfT,
					thermalPlostfT,//thermalPlostfT; 
					10//thermalC;
					);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
	}		
    public RecipesList maceratorRecipes = new RecipesList();
    void registerMacerator(int id)
    {
    	int subId,completId;
    	String name;
    	{
    		subId = 0;
	    	name = "50V macerator";

	    	MaceratorDescriptor desc = new MaceratorDescriptor(
					name,"macerator50V",
					LVU,200,//double  nominalU,double nominalP,
					LVU*1.25,//double  maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					lowVoltageCableDescriptor,//ElectricalCableDescriptor cable
					maceratorRecipes
					);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}

    	{
    		subId = 4;
	    	name = "200V macerator";

	    	MaceratorDescriptor desc = new MaceratorDescriptor(
					name,"macerator50V",
					MVU,400,//double  nominalU,double nominalP,
					MVU*1.25,//double  maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					meduimVoltageCableDescriptor,//ElectricalCableDescriptor cable
					maceratorRecipes
					);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
	}		
    public RecipesList extractorRecipes = new RecipesList();
    void registerExtractor(int id)
    {

  	
    	int subId,completId;
    	String name;
    	{
    		subId = 0;
	    	name = "50V extractor";

			ElectricalMachineDescriptor desc = new ElectricalMachineDescriptor(
					name,//String name,
					LVU,200,//double  nominalU,double nominalP,
					LVU*1.25,//double  maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					lowVoltageCableDescriptor,//ElectricalCableDescriptor cable
					extractorRecipes
					);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}

    	{
    		subId = 4;
	    	name = "200V extractor";

			ElectricalMachineDescriptor desc = new ElectricalMachineDescriptor(
					name,//String name,
					MVU,400,//double  nominalU,double nominalP,
					MVU*1.25,//double  maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					meduimVoltageCableDescriptor,//ElectricalCableDescriptor cable
					extractorRecipes
					);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
	}		
    public RecipesList compressorRecipes = new RecipesList();
    void registerCompressor(int id)
    {

  	
    	int subId,completId;
    	String name;
    	{
    		subId = 0;
	    	name = "50V compressor";

			ElectricalMachineDescriptor desc = new ElectricalMachineDescriptor(
					name,//String name,
					LVU,200,//double  nominalU,double nominalP,
					LVU*1.25,//double  maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					lowVoltageCableDescriptor,//ElectricalCableDescriptor cable
					compressorRecipes
					);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}

    	{
    		subId = 4;
	    	name = "200V compressor";

			ElectricalMachineDescriptor desc = new ElectricalMachineDescriptor(
					name,//String name,
					MVU,400,//double  nominalU,double nominalP,
					MVU*1.25,//double  maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					meduimVoltageCableDescriptor,//ElectricalCableDescriptor cable
					compressorRecipes
					);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
	}		
    public RecipesList magnetiserRecipes = new RecipesList();
    void registermagnetiser(int id)
    {

  	
    	int subId,completId;
    	String name;
    	{
    		subId = 0;
	    	name = "50V magnetizer";

			ElectricalMachineDescriptor desc = new ElectricalMachineDescriptor(
					name,//String name,
					LVU,200,//double  nominalU,double nominalP,
					LVU*1.25,//double  maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					lowVoltageCableDescriptor,//ElectricalCableDescriptor cable
					magnetiserRecipes
					);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}

    	{
    		subId = 4;
	    	name = "200V magnetizer";

			ElectricalMachineDescriptor desc = new ElectricalMachineDescriptor(
					name,//String name,
					MVU,400,//double  nominalU,double nominalP,
					MVU*1.25,//double  maximalU,
					new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
					meduimVoltageCableDescriptor,//ElectricalCableDescriptor cable
					magnetiserRecipes
					);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}
	}		      
    void registerSolarPannel(int id)
    {
    	int subId,completId;
    	GhostGroup ghostGroup;
    	String name;
    	
    	
       	FunctionTable diodeIfUBase;
    	diodeIfUBase = new FunctionTableYProtect(new double[]{	0.0, 0.002, 0.005,0.01, 0.015, 0.02,
																	 0.025, 0.03, 0.035,0.04, 0.045,
																	 0.05, 0.06, 0.07, 0.08, 0.09,
    																 0.10, 0.11, 0.12, 0.13, 1.0}, 1.0, 0, 1.0);
      /*
       	{
       		double[] table = new double[81];
       		for(int idx = 0;idx < table.length;idx++)
       		{
       			table[idx] = 0.1*idx/table.length;
       		}
       		table[table.length-1] = 1.0;
       		
       		diodeIfUBase = new FunctionTableYProtect(table, 1.0, 0, 1.0);
       	}
       	*/
       	FunctionTable solarIfSBase;
       	solarIfSBase = new FunctionTable(new double[]{0.0, 0.1, 0.4, 0.6, 0.8, 1.0}, 1);
    	
    	double LVSolarU = 59;
       	{
    		subId = 0;
	    	name = "Test solar pannel";
	    	
	    	ghostGroup = new GhostGroup();
	    	ghostGroup.addElement(0, 1, 0);
	    	ghostGroup.addElement(0, 2, 0);
	    	
			SolarPannelDescriptor desc = new SolarPannelDescriptor(
					name,//iconID,Name
					ghostGroup,0,2,0, //ghost,int solarOffsetX,int solarOffsetY,int solarOffsetZ,
					diodeIfUBase,//FunctionTable diodeIfUBase,
					solarIfSBase,//solarIfS
					LVSolarU,500 / LVU,//double electricalUmax,double electricalImax,
					0.05,//,double electricalDropFactor	
					Math.PI/4,Math.PI/4*3 //alphaMin  alphaMax
					);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}    	
       	{
    		subId = 1;
	    	name = "Small solar pannel";
	    	
	    	ghostGroup = new GhostGroup();
	    	
		/*	SolarPannelDescriptor desc = new SolarPannelDescriptor(
					name,//iconID,Name
					ghostGroup,0,1,0, //ghost,int solarOffsetX,int solarOffsetY,int solarOffsetZ,
					diodeIfUBase,//FunctionTable diodeIfUBase,
					solarIfSBase,//solarIfS
					LVSolarU/4,100 / LVU * 4,//double electricalUmax,double electricalImax,
					0.05,//,double electricalDropFactor	
					Math.PI/2,Math.PI/2 //alphaMin  alphaMax
					);
			*/
	    	
	    	SolarPannelDescriptor desc = new SolarPannelDescriptor(
	    			name,//String name,
	    			ghostGroup,0,1,0,//GhostGroup ghostGroup, int solarOffsetX,int solarOffsetY,int solarOffsetZ,
	    			//FunctionTable solarIfSBase,
	    			LVSolarU/4,100.0,//double electricalUmax,double electricalPmax,
	    			0.05,//,double electricalDropFactor	
					Math.PI/2,Math.PI/2 //alphaMin  alphaMax
	    			);
	    			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}   
       	{
    		subId = 2;
	    	name = "Small rotating solar pannel";
	    	
	    	ghostGroup = new GhostGroup();
	    	/*
			SolarPannelDescriptor desc = new SolarPannelDescriptor(
					name,//iconID,Name
					ghostGroup,0,1,0, //ghost,int solarOffsetX,int solarOffsetY,int solarOffsetZ,
					diodeIfUBase,//FunctionTable diodeIfUBase,
					solarIfSBase,//solarIfS
					LVSolarU/4,100 / LVU * 4,//double electricalUmax,double electricalImax,
					0.05,//,double electricalDropFactor	
					Math.PI/4,Math.PI/4*3 //alphaMin  alphaMax
					);
			*/
	    	
	    	SolarPannelDescriptor desc = new SolarPannelDescriptor(
	    			name,//String name,
	    			ghostGroup,0,1,0,//GhostGroup ghostGroup, int solarOffsetX,int solarOffsetY,int solarOffsetZ,
	    			//FunctionTable solarIfSBase,
	    			LVSolarU/4,100.0,//double electricalUmax,double electricalPmax,
	    			0.05,//,double electricalDropFactor	
	    			Math.PI/4,Math.PI/4*3 //alphaMin  alphaMax
	    			);
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}   
    }
	
  	
    void registerHeatingCorp(int id)
    {
    	int subId,completId;
    	String name;

		HeatingCorpElement element;
		{
			subId = 0; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "Small 50V cooper heating corp",//iconId, name,
					LVU,150,//electricalNominalU, electricalNominalP,
					190,//electricalMaximalP)
					lowVoltageCableDescriptor//ElectricalCableDescriptor
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 1; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "50V cooper heating corp",//iconId, name,
					LVU,250,//electricalNominalU, electricalNominalP,
					320,//electricalMaximalP)
					lowVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}    		
		{
			subId = 2; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "Small 200V cooper heating corp",//iconId, name,
					MVU,400,//electricalNominalU, electricalNominalP,
					500,//electricalMaximalP)
					meduimVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}    		
		{
			subId = 3; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "200V cooper heating corp",//iconId, name,
					 MVU,600,//electricalNominalU, electricalNominalP,
					750,//electricalMaximalP)
					highVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}  
		{
			subId = 4; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "Small 50V iron heating corp",//iconId, name,
					 LVU,180,//electricalNominalU, electricalNominalP,
					225,//electricalMaximalP)
					lowVoltageCableDescriptor//ElectricalCableDescriptor
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 5; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "50V iron heating corp",//iconId, name,
					 LVU,375,//electricalNominalU, electricalNominalP,
					480,//electricalMaximalP)
					lowVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}    		
		{
			subId = 6; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "Small 200V iron heating corp",//iconId, name,
					 MVU,600,//electricalNominalU, electricalNominalP,
					750,//electricalMaximalP)
					meduimVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}    		
		{
			subId = 7; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "200V iron heating corp",//iconId, name,
					 MVU,900,//electricalNominalU, electricalNominalP,
					1050,//electricalMaximalP)
					highVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 8; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "Small 50V tungsten heating corp",//iconId, name,
					 LVU,240,//electricalNominalU, electricalNominalP,
					300,//electricalMaximalP)
					lowVoltageCableDescriptor//ElectricalCableDescriptor
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 9; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "50V tungsten heating corp",//iconId, name,
					 LVU,500,//electricalNominalU, electricalNominalP,
					640,//electricalMaximalP)
					lowVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}    		
		{
			subId = 10; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "Small 200V tungsten heating corp",//iconId, name,
					MVU,800,//electricalNominalU, electricalNominalP,
					1000,//electricalMaximalP)
					meduimVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}    		
		{
			subId = 11; completId = subId + (id << 6);
			element = new HeatingCorpElement(
					 "200V tungsten heating corp",//iconId, name,
					 MVU,1200,//electricalNominalU, electricalNominalP,
					1500,//electricalMaximalP)
					highVoltageCableDescriptor);
			sharedItem.addElement(completId, element);
		}
		
	}
	
    void registerThermalIsolator(int id)
    {
    	int subId,completId;
    	String name;

		ThermalIsolatorElement element;
		{
			subId = 0; completId = subId + (id << 6);
			element = new ThermalIsolatorElement(
					 "Sand thermal isolator",//iconId, name,
					0.7,1000
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 1; completId = subId + (id << 6);
			element = new ThermalIsolatorElement(
					 "Stone thermal isolator",//iconId, name,
					0.8,1000
					);
			sharedItem.addElement(completId, element);
		}  
		{
			subId = 2; completId = subId + (id << 6);
			element = new ThermalIsolatorElement(
					 "Brick thermal isolator",//iconId, name,
					0.5,1000
					);
			sharedItem.addElement(completId, element);
		}  
		/*{
			subId = 3; completId = subId + (id << 6);
			element = new ThermalIsolatorElement(
					 "Wood thermal isolator",//iconId, name,
					0.05,100
					);
			sharedItem.addElement(completId, element);
		}    	*/	
		
	}
	
	
    void registerRegulatorItem(int id)
    {
    	int subId,completId;
    	String name;
		IRegulatorDescriptor element;
		{
			subId = 0; completId = subId + (id << 6);
			element = new RegulatorOnOffDescriptor(
					 "On/OFF regulator 1%","onoffregulator",
					0.01
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 1; completId = subId + (id << 6);
			element = new RegulatorOnOffDescriptor(
					 "On/OFF regulator 10%","onoffregulator",
					0.1
					);
			sharedItem.addElement(completId, element);
		}
		
		
		{
			subId = 8; completId = subId + (id << 6);
			element = new RegulatorAnalogDescriptor(
					 "Analogic regulator","Analogicregulator"
					);
			sharedItem.addElement(completId, element);
		}/*
		{
			subId = 9; completId = subId + (id << 6);
			element = new RegulatorAnalogDescriptor(
					 "Analogic PI regulator"
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 10; completId = subId + (id << 6);
			element = new RegulatorAnalogDescriptor(
					 "Analogic PID regulator"
					);
			sharedItem.addElement(completId, element);
		}	*/
		
	}
	

    

    void registerLampItem(int id)
    {
    	int subId,completId;
    	String name;
    	double incondecentLife = Utils.minecraftDay*5;
    	double[] lightPower = new double[]{	0,
    										0,0,0,0,0,
    										0,0,0,0,0,
    										15,20,30,40,60};
    	double[] lightLevel = new double[16];
    	double economicPowerFactor = 0.75;
    	double economicLife = incondecentLife*4;
    	
    	for(int idx = 0 ;idx < 15;idx++)
    	{
    		lightLevel[idx] = (idx + 0.49) / 15.0;
    	}
		LampDescriptor element;
		{
			subId = 0; completId = subId + (id << 6);
			element = new LampDescriptor(
					"Small 50V incandescent light bulb","incandescentlampiron",
					LampDescriptor.Type.Incandescent,LampSocketType.Douille, 
					LVU ,lightPower[12], 	//nominalU, nominalP
					lightLevel[12],incondecentLife //nominalLight, nominalLife
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 1; completId = subId + (id << 6);
			element = new LampDescriptor(
					"50V incandescent light bulb","incandescentlampiron",
					LampDescriptor.Type.Incandescent,LampSocketType.Douille, 
					LVU ,lightPower[14], 	//nominalU, nominalP
					lightLevel[14],incondecentLife //nominalLight, nominalLife
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 2; completId = subId + (id << 6);
			element = new LampDescriptor(
					"200V incandescent light bulb","incandescentlampiron",
					LampDescriptor.Type.Incandescent,LampSocketType.Douille, 
					MVU ,lightPower[14], 	//nominalU, nominalP
					lightLevel[14],incondecentLife //nominalLight, nominalLife
					);
			sharedItem.addElement(completId, element);
		} 			
		/*{
			subId = 3; completId = subId + (id << 6);
			element = new LampDescriptor(
					"400V incandescent light bulb",
					LampDescriptor.Type.Incandescent,LampSocketType.Tube, 
					400 ,lightPower[14], 	//nominalU, nominalP
					lightLevel[14],incondecentLife //nominalLight, nominalLife
					);
			sharedItem.addElement(completId, element);
		} */		
		{
			subId = 4; completId = subId + (id << 6);
			element = new LampDescriptor(
					"Small 50V carbon incandescent light bulb","incandescentlampcarbon",
					LampDescriptor.Type.Incandescent,LampSocketType.Douille, 
					LVU ,lightPower[11], 	//nominalU, nominalP
					lightLevel[11],incondecentLife/3 //nominalLight, nominalLife
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 5; completId = subId + (id << 6);
			element = new LampDescriptor(
					"50V carbon incandescent light bulb","incandescentlampcarbon",
					LampDescriptor.Type.Incandescent,LampSocketType.Douille, 
					LVU ,lightPower[13], 	//nominalU, nominalP
					lightLevel[13],incondecentLife/3 //nominalLight, nominalLife
					);
			sharedItem.addElement(completId, element);
		}
		
		{
			subId = 16; completId = subId + (id << 6);
			element = new LampDescriptor(
					"Small 50V economic light bulb","economiclamp",
					LampDescriptor.Type.eco,LampSocketType.Douille, 
					LVU ,lightPower[12] * economicPowerFactor, 	//nominalU, nominalP
					lightLevel[12],economicLife //nominalLight, nominalLife
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 17; completId = subId + (id << 6);
			element = new LampDescriptor(
					"50V economic light bulb","economiclamp",
					LampDescriptor.Type.eco,LampSocketType.Douille, 
					LVU ,lightPower[14] * economicPowerFactor, 	//nominalU, nominalP
					lightLevel[14],economicLife //nominalLight, nominalLife
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 18; completId = subId + (id << 6);
			element = new LampDescriptor(
					"200V economic light bulb","economiclamp",
					LampDescriptor.Type.eco,LampSocketType.Douille, 
					MVU ,lightPower[14] * economicPowerFactor, 	//nominalU, nominalP
					lightLevel[14],economicLife //nominalLight, nominalLife
					);
			sharedItem.addElement(completId, element);
		} 	
		
	}
		
    void registerProtection(int id)
    {
    	int subId,completId;
    	String name;
		
		{
			OverHeatingProtectionDescriptor element;
			subId = 0; completId = subId + (id << 6);
			element = new OverHeatingProtectionDescriptor(
					"OverHeating protection");
			sharedItem.addElement(completId, element);
		}
		{
			OverVoltageProtectionDescriptor element;
			subId = 1; completId = subId + (id << 6);
			element = new OverVoltageProtectionDescriptor(
					"OverVoltage protection");
			sharedItem.addElement(completId, element);
		}

	} 
	
    void registerCombustionChamber(int id)
    {
    	int subId,completId;
    	String name;	
		{
			CombustionChamber element;
			subId = 0; completId = subId + (id << 6);
			element = new CombustionChamber(
					"Combustion chamber");
			sharedItem.addElement(completId, element);
		}

	} 

    void registerFerromagneticCore(int id)
    {
    	int subId,completId;
    	String name;	

		FerromagneticCoreDescriptor element;
		{
			subId = 0; completId = subId + (id << 6);
			element = new FerromagneticCoreDescriptor(
					 "Cheap ferromagnetic core",//iconId, name,
					10
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 1; completId = subId + (id << 6);
			element = new FerromagneticCoreDescriptor(
					 "Average ferromagnetic core",//iconId, name,
					4
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 2; completId = subId + (id << 6);
			element = new FerromagneticCoreDescriptor(
					 "Optimal ferromagnetic core",//iconId, name,
					1
					);
			sharedItem.addElement(completId, element);
		}    		
	}
    public static OreDescriptor oreTin,oreCooper,oreSilver;
    
    void registerOre()
    {
    	int id;
    	String name;
    	{
	    	id = 0;
	    	name = "Tin ore";

			OreDescriptor desc = new OreDescriptor(
					name,id, //int itemIconId, String name,int metadata,
					200,3,9,0,80 //int spawnRate,int spawnSizeMin,int spawnSizeMax,int spawnHeightMin,int spawnHeightMax

					);
			oreTin = desc;
			oreItem.addDescriptor(id, desc);
    	
    	}
    	{
	    	id = 1;

	    	name = "Cooper ore";

			OreDescriptor desc = new OreDescriptor(
					name,id, //int itemIconId, String name,int metadata,
					200,3,9,0,80 //int spawnRate,int spawnSizeMin,int spawnSizeMax,int spawnHeightMin,int spawnHeightMax
					);
			oreCooper = desc;
			oreItem.addDescriptor(id, desc);
    	}
    	{
	    	id = 2;

	    	name = "Silver ore";

			OreDescriptor desc = new OreDescriptor(
					name,id, //int itemIconId, String name,int metadata,
					200,3,9,0,80 //int spawnRate,int spawnSizeMin,int spawnSizeMax,int spawnHeightMin,int spawnHeightMax
					);
			oreSilver = desc;
			oreItem.addDescriptor(id, desc);
    	}
    	{
	    	id = 3;

	    	name = "Aluminum ore";

			OreDescriptor desc = new OreDescriptor(
					name,id, //int itemIconId, String name,int metadata,
					200,3,9,0,80 //int spawnRate,int spawnSizeMin,int spawnSizeMax,int spawnHeightMin,int spawnHeightMax
					);
			oreItem.addDescriptor(id, desc);
    	}
    	{
	    	id = 4;

	    	name = "Plumb ore";

			OreDescriptor desc = new OreDescriptor(
					name,id, //int itemIconId, String name,int metadata,
					200,3,9,0,80 //int spawnRate,int spawnSizeMin,int spawnSizeMax,int spawnHeightMin,int spawnHeightMax
					);
			oreItem.addDescriptor(id, desc);
    	}
    	{
	    	id = 5;
	    	
	    	name = "Tungsten ore";

			OreDescriptor desc = new OreDescriptor(
					name,id, //int itemIconId, String name,int metadata,
					200,3,9,0,80 //int spawnRate,int spawnSizeMin,int spawnSizeMax,int spawnHeightMin,int spawnHeightMax
					);
			oreItem.addDescriptor(id, desc);
    	} 
    	{
	    	id = 6;
	    	
	    	name = "Cinnabar ore";

			OreDescriptor desc = new OreDescriptor(
					name,id, //int itemIconId, String name,int metadata,
					200,3,9,0,80 //int spawnRate,int spawnSizeMin,int spawnSizeMax,int spawnHeightMin,int spawnHeightMax
					);
			oreItem.addDescriptor(id, desc);
    	} 
    	
    }
    
    public static GenericItemUsingDamageDescriptorWithComment dustTin,dustCooper,dustSilver;
    
    void registerDust(int id)
    {
    	int subId,completId;
    	String name;
    	GenericItemUsingDamageDescriptorWithComment element;
    	
    	{
			subId = 0; completId = subId + (id << 6);
			
			name = "Tin dust";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"dudu dust","miaou"}
					);
			dustTin = element;
			sharedItem.addElement(completId, element);
		}  	
    	{
			subId = 1; completId = subId + (id << 6);
			
			name = "Cooper dust";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"dudu dust","miaou"}
					);
			dustCooper = element;
			sharedItem.addElement(completId, element);
		}  	
    	{
			subId = 2; completId = subId + (id << 6);
			
			name = "Iron dust";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"dudu dust","miaou"}
					);
			dustCooper = element;
			sharedItem.addElement(completId, element);
		}  	

    	{
	    	id = 5;

	    	name = "Plumb dust";

			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"dudu dust","miaou"}
					);
			sharedItem.addElement(id, element);
    	}
    	{
	    	id = 6;
	    	
	    	name = "Tungsten dust";

			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"dudu dust","miaou"}
					);
			sharedItem.addElement(id, element);
    	} 
    	/*{
	    	id = 7;
	    	
	    	name = "Gold dust";

			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"dudu dust","miaou"}
					);
			sharedItem.addElement(id, element);
    	} */
    	
    	{
	    	id = 8;
	    	
	    	name = "Coal dust";

			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"dudu dust","miaou"}
					);
			sharedItem.addElement(id, element);
    	} 
    	{
	    	id = 9;
	    	
	    	name = "Steel dust";

			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"dudu dust","miaou"}
					);
			sharedItem.addElement(id, element);
    	} 
    	
    	{
	    	id = 10;
	    	
	    	name = "Cinnabar dust";

			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"dudu dust","miaou"}
					);
			sharedItem.addElement(id, element);
    	} 
    	{
	    	id = 11;
	    	
	    	name = "Purified cinnabar dust";

			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"dudu dust","miaou"}
					);
			sharedItem.addElement(id, element);
    	} 
    	        	    	
    	
 
    }
    
    
    
    GenericItemUsingDamageDescriptorWithComment tinIngot,cooperIngot,silverIngot,plumbIngot,tungstenIngot;
    void registerIngot(int id)
    {
    	int subId,completId;
    	String name;

  		GenericItemUsingDamageDescriptorWithComment element;
  		
		{
			subId = 0; completId = subId + (id << 6);
			
			name = "Tin ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(
					name,//iconId, name,
					new String[]{"useless","miaou"}
					);
			sharedItem.addElement(completId, element);
			//GameRegistry.registerCustomItemStack(name, element.newItemStack(1));
			tinIngot = element;
		}
		{
			subId = 1; completId = subId + (id << 6);
			
			name = "Cooper ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(
					name,//iconId, name,
					new String[]{"useless^2","miaou"}
					);
			sharedItem.addElement(completId, element);
			//GameRegistry.registerCustomItemStack(name, element.newItemStack(1));
			cooperIngot = element;
		}
		{
			subId = 2; completId = subId + (id << 6);
			
			name = "Silver ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"useless^3","miaou"}
					);
			sharedItem.addElement(completId, element);
			//GameRegistry.registerCustomItemStack(name, element.newItemStack(1));
			silverIngot = element;
		}
		{
			subId = 3; completId = subId + (id << 6);
			
			name = "Aluminum ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"useless^4","miaou"}
					);
			sharedItem.addElement(completId, element);
			//GameRegistry.registerCustomItemStack(name, element.newItemStack(1));
			
		}
		{
			subId = 4; completId = subId + (id << 6);
			
			name = "Plumb ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"useless","miaou"}
					);
			sharedItem.addElement(completId, element);
			//GameRegistry.registerCustomItemStack(name, element.newItemStack(1));
			plumbIngot = element;
		}

		{
			subId = 5; completId = subId + (id << 6);
			
			name = "Tungsten ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"useless","miaou"}
					);
			sharedItem.addElement(completId, element);
			//GameRegistry.registerCustomItemStack(name, element.newItemStack(1));
			tungstenIngot = element;
		}	
		
		{
			subId = 6; completId = subId + (id << 6);
			
			name = "Ferrite ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"useless","miaou"}
					);
			sharedItem.addElement(completId, element);
			//GameRegistry.registerCustomItemStack(name, element.newItemStack(1));
			
		}	
		
		
		{
			subId = 7; completId = subId + (id << 6);
			
			name = "Steel ingot";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"useless","miaou"}
					);
			sharedItem.addElement(completId, element);
			//GameRegistry.registerCustomItemStack(name, element.newItemStack(1));
			
		}	
		
		{
			subId = 8; completId = subId + (id << 6);
			
			name = "Mercury";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"useless","miaou"}
					);
			sharedItem.addElement(completId, element);
			//GameRegistry.registerCustomItemStack(name, element.newItemStack(1));
			
		}	
    }
    
    void registerElectricalMotor(int id)
    {
    	
    	int subId,completId;
    	String name;		
    	GenericItemUsingDamageDescriptorWithComment element;
    	
    	
		{
			subId = 0; completId = subId + (id << 6);
			
			name = "Electrical motor";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"useless","miaou"}
					);
			sharedItem.addElement(completId, element);
			//GameRegistry.registerCustomItemStack(name, element.newItemStack(1));
			
		}	
		{
			subId = 1; completId = subId + (id << 6);
			
			name = "Advanced electrical motor";
			element = new GenericItemUsingDamageDescriptorWithComment(
					 name,//iconId, name,
					new String[]{"useless","miaou"}
					);
			sharedItem.addElement(completId, element);
			//GameRegistry.registerCustomItemStack(name, element.newItemStack(1));
			
		}	
		
/*
		ElectricalMotorDescriptor element;
		{
			subId = 0; completId = subId + (id << 6);
			element = new ElectricalMotorDescriptor(
					 "Small 50V electrical motor",//iconId, name,
					LVU,240,//double  nominalU,double nominalP,
					LVU*1.2,//double  maximalU,
					1000000.0,200,20,//double thermalConductivityTao,double thermalWarmLimit,double thermalHeatTime,
					lowVoltageCableDescriptor
					
					);
			sharedItem.addElement(completId, element);
		}
		
		{
			subId = 1; completId = subId + (id << 6);
			element = new ElectricalMotorDescriptor(
					 "50V electrical motor",//iconId, name,
					 LVU,500,//double  nominalU,double nominalP,
					 LVU*1.2,//double  maximalU,
					 1000000.0,200,20,//double thermalConductivityTao,double thermalWarmLimit,double thermalHeatTime,
					lowVoltageCableDescriptor
					
					);
			sharedItem.addElement(completId, element);
		}
		
		{
			subId = 2; completId = subId + (id << 6);
			element = new ElectricalMotorDescriptor(
					 "200V electrical motor",//iconId, name,
					200,1000,//double  nominalU,double nominalP,
					LVU*1.2,//double  maximaU,
					1000000.0,200,20,//double thermalConductivityTao,double thermalWarmLimit,double thermalHeatTime,
					meduimVoltageCableDescriptor
					
					);
			sharedItem.addElement(completId, element);
		}*/
  		  		  		
	}
    
    void registerSolarTracker(int id)
    {
    	int subId,completId;
    	String name;	

		SolarTrackerDescriptor element;
		{
			subId = 0; completId = subId + (id << 6);
			element = new SolarTrackerDescriptor(
					 "Solar tracker"//iconId, name,

					);
			sharedItem.addElement(completId, element);
		}
  		
	}
    
    
    void registerWindTurbine(int id)
    {
    	int subId,completId;
    	String name;
    	{
    		subId = 0;
	    	name = "Wind turbine";

	    	WindTurbineDescriptor desc = new WindTurbineDescriptor(name);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  	
    }
    
    void registerThermalDissipatorPassiveAndActive(int id)
    {
    	int subId,completId;
    	String name;
    	{
    		subId = 0;
	    	name = "Small passive thermal dissipator";

	    	ThermalDissipatorPassiveDescriptor desc = new ThermalDissipatorPassiveDescriptor(
	    			name,
	    			200,-100,//double warmLimit,double coolLimit,
	    			250,30,//double nominalP,double nominalT,
	    			10,4//double nominalTao,double nominalConnectionDrop
	    			
	    			);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  	    
    	
    	
    	{
    		subId = 32;
	    	name = "Small active thermal dissipator";

	    	ThermalDissipatorActiveDescriptor desc = new ThermalDissipatorActiveDescriptor(
	    			name,
	    			LVU,50,//double nominalElectricalU,double electricalNominalP,
	    			800,//double nominalElectricalCoolingPower,
	    			lowVoltageCableDescriptor,//ElectricalCableDescriptor cableDescriptor,
	    			130,-100,//double warmLimit,double coolLimit,
	    			200,30,//double nominalP,double nominalT,
	    			10,4//double nominalTao,double nominalConnectionDrop
	    			
	    			);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  	    
    	
    		
    }
    
    void registerWindRotor(int id)
    {
    	int subId,completId;
    	String name;
    	
    	FunctionTable PfW = new FunctionTable(new double[]{0.0,0.1,0.3,0.6,1.0}, 1.0);
    	FunctionTable environnementalFunction = new FunctionTable(new double[]{	0.0,
    														0.0,0.0,0.0,0.0,0.0,
    														0.1,0.2,0.4,0.5,1.0}, 1.0);
    	GhostGroup ghostGroupe;
    	WindRotorDescriptor desc;
    	{
    		subId = 0;
	    	name = "Wind rotor";
	    	
	    	ghostGroupe = new GhostGroup();
	    	ghostGroupe.addRectangle(-1, -1, -1, 1, -1, 1);
	    	

	    	desc = new WindRotorDescriptor(
	    			name,//int iconId, String name, 
	    			ghostGroupe,
	    			-2,2,//int environnementalWidthStart,int environnementalWidthEnd,
	    			-2,2,//int environnementalHeightStart,int environnementalHeightEnd,
	    			-5,5,//int environnementalDepthStart,int environnementalDepthEnd,
	    			9+1+2+1+5,
	    			environnementalFunction,
	    			WindRotorAxeType.horizontal,//WindRotorAxeType axe,
	    			PfW,		
	    			10,500,
	    			30
	    			);
			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	
    }
    
       
    void registerDynamo(int id)
    {
    	int subId,completId;
    	String name;
    /*
     *     	FunctionTable PoutfPin = new FunctionTable(new double[]{1.0,
    			1.0,1.0,1.0,1.0,1.0,
    			1.0,1.0,1.0,1.0,1.0,
    			0.95,0.90,0.85,0.78,0.70
				}, 1.5);	
     */
    	FunctionTable PoutfPin = new FunctionTable(new double[]{0.0,
    			0.1,0.2,0.3,0.4,0.5,
    			0.6,0.7,0.8,0.9,1.0,
    			1.08,1.15,1.21,1.26,1.29
				}, 1.5);
    	FunctionTable UfP = new FunctionTable(new double[]{0.0,
				0.9,1.0,  1.10,1.15,1.18,
				1.18,1.18,1.18,1.18,1.18 														
				}, 1.0);

		DynamoDescriptor element;
		{
			subId = 0; completId = subId + (id << 6);
			element = new DynamoDescriptor(
					 "Small dynamo",//iconId, name,
					PoutfPin,
					UfP,
					LVU,250,0.01// electricalMaxU, electricalMaxP, electricalDropFactor
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 1; completId = subId + (id << 6);
			element = new DynamoDescriptor(
					 "Medium dynamo",//iconId, name,
					PoutfPin,
					UfP,
					LVU,500,0.01// electricalMaxU, electricalMaxP, electricalDropFactor
					);
			sharedItem.addElement(completId, element);
		}
		{
			subId = 2; completId = subId + (id << 6);
			element = new DynamoDescriptor(
					 "Big dynamo",//iconId, name,
					PoutfPin,
					UfP,
					LVU,750,0.01// electricalMaxU, electricalMaxP, electricalDropFactor
					);
			sharedItem.addElement(completId, element);
		}
  		   	
    }
    
    
    void registerMppt(int id)
    {
    	int subId,completId;
    	String name;    	
    	MpptDescriptor desc;
    	
    	FunctionTable PoutfPin = new FunctionTable(new double[]{0.0,
    			0.1,0.2,0.3,0.4,0.5,
    			0.6,0.7,0.8,0.9,1.0,
    			1.08,1.15,1.21,1.26,1.29
				}, 1.5);
    	
    	

    	{
    		subId = 0;
	    	name = "Basic maximum power point tracker";

	    	desc = new MpptDescriptor(
	    			name,
	    			-1,LVU*1.3,//double inUmin,double inUmax,
	    			10,LVU*1.19,//double outUmin,double outUmax,
	    			
	    			500,//double designedPout,	
	    			PoutfPin,//FunctionTable PoutfPin,
	    			0.01,//electricalLoadDropFactor
	    			
	    			6.0,0.1,//double inResistorLowHighTime,double inResistorNormalTime,
	    			0.05,//double inResistorStepFactor,
	    			1.0,50.0//double inResistorMin,double inResistorMax
	    			
	    			
	    			);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}      	
    }
    
    void registerElectricalAntenna(int id)
    {
    	int subId,completId;
    	String name;    	
    	{
        	
    		subId = 0;
    		ElectricalAntennaTxDescriptor desc;
	    	name = "Small power transmitter antenna";
	    	double P = 250;
	    	desc = new ElectricalAntennaTxDescriptor(
	    			name,
	    			200,//int rangeMax,
	    			0.9,0.7,//double electricalPowerRatioEffStart,double electricalPowerRatioEffEnd,
	    			LVU,P,//double electricalNominalVoltage,double electricalNominalPower,
	    			LVU * 1.3,P*1.3,// electricalMaximalVoltage,double electricalMaximalPower,
	    			lowVoltageCableDescriptor			
	    			);			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}      	
    	{
        	
    		subId = 1;
    		ElectricalAntennaRxDescriptor desc;
	    	name = "Small power receiver antenna";
	    	double P = 250;
	    	desc = new ElectricalAntennaRxDescriptor(
	    			name,
	    			LVU,P,//double electricalNominalVoltage,double electricalNominalPower,
	    			LVU * 1.3,P*1.3,// electricalMaximalVoltage,double electricalMaximalPower,
	    			lowVoltageCableDescriptor	   			
	    			);			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}      	
    }
     
    
   static public  GenericItemUsingDamageDescriptor multiMeterElement,thermoMeterElement,allMeterElement;
    void registerMeter(int id)
    {
    	int subId,completId;

    	GenericItemUsingDamageDescriptor element;
		{
			subId = 0; completId = subId + (id << 6);
			element = new GenericItemUsingDamageDescriptor("MultiMeter");
			sharedItem.addElement(completId, element);
			multiMeterElement = element;
		}
		{
			subId = 1; completId = subId + (id << 6);
			element = new GenericItemUsingDamageDescriptor("ThermoMeter");
			sharedItem.addElement(completId, element);
			thermoMeterElement = element;
		}
		{
			subId = 2; completId = subId + (id << 6);
			element = new GenericItemUsingDamageDescriptor("AllMeter");
			sharedItem.addElement(completId, element);
			allMeterElement = element;
		}
 		   	
    }    
    
   public static TreeResin treeResin;
    void registerTreeResinAndRubber(int id)
    {
    	int subId,completId;
    	String name;	

    
		{
			TreeResin descriptor;
			subId = 0; completId = subId + (id << 6);
			name = "Tree resin";
			
			descriptor = new TreeResin(name);
					
			sharedItem.addElement(completId, descriptor);
			treeResin = descriptor;
		}   
		{
			GenericItemUsingDamageDescriptor descriptor;
			subId = 1; completId = subId + (id << 6);
			name = "Rubber";
			
			descriptor = new GenericItemUsingDamageDescriptor(name);
			sharedItem.addElement(completId, descriptor);
		}   
    }

   	void registerTreeResinCollector(int id)
   	{
    	int subId,completId;
    	String name;	

    	TreeResinCollectorDescriptor descriptor;
		{
			subId = 0; completId = subId + (id << 6);
			name = "tree resin collector";
			
					descriptor = new TreeResinCollectorDescriptor(
					 name
					);
			sixNodeItem.addDescriptor(completId, descriptor);
		}
	}

	
    void registerElectricalDrill(int id)
    {
    	int subId,completId;
    	String name;	

		ElectricalDrillDescriptor descriptor;
		{
			subId = 0; completId = subId + (id << 6);
			name = "Cheap electrical drill";
			
					descriptor = new ElectricalDrillDescriptor(
					 name,//iconId, name,
					LVU,LVU*1.25,//double nominalVoltage,double maximalVoltage,
					10,1000 //double operationTime,double operationEnergy
					);
			sharedItem.addElement(completId, descriptor);
		}
		{
			subId = 1; completId = subId + (id << 6);
			name = "Average electrical drill";
			
					descriptor = new ElectricalDrillDescriptor(
					 name,//iconId, name,
					LVU,LVU*1.25,//double nominalVoltage,double maximalVoltage,
					5,1500 //double operationTime,double operationEnergy
					);
			sharedItem.addElement(completId, descriptor);
		}
		{
			subId = 2; completId = subId + (id << 6);
			name = "Fast electrical drill";
			
					descriptor = new ElectricalDrillDescriptor(
					 name,//iconId, name,
					 LVU,LVU*1.25,//double nominalVoltage,double maximalVoltage,
					3,2000 //double operationTime,double operationEnergy
					);
			sharedItem.addElement(completId, descriptor);
		}
  		
	}
    void registerOreScanner(int id)
    {
    	int subId,completId;
    	String name;	

    	OreScanner descriptor;
		{
			subId = 0; completId = subId + (id << 6);
			name = "Basic ore scanner";
			
					descriptor = new OreScanner(
					 name,//iconId, name,
					 LVU,LVU*1.25,//double nominalVoltage,double maximalVoltage,
					2,300//,int operationRadius,double operationTime,double operationEnergy

					);
			sharedItem.addElement(completId, descriptor);
		}
		{
			subId = 1; completId = subId + (id << 6);
			name = "Advanced ore scanner";
			
					descriptor = new OreScanner(
					 name,//iconId, name,
					 LVU,LVU*1.25,//double nominalVoltage,double maximalVoltage,
					4,800//,int operationRadius,double operationEnergy

					);
			sharedItem.addElement(completId, descriptor);
		}
  		
	}
    public static MiningPipeDescriptor miningPipeDescriptor;
    
    void registerMiningPipe(int id)
    {
    	int subId,completId;
    	String name;	

		MiningPipeDescriptor descriptor;
		{
			subId = 0; completId = subId + (id << 6);
			name = "Mining pipe";
			
					descriptor = new MiningPipeDescriptor(
					 name//iconId, name
					); 
			sharedItem.addElement(completId, descriptor);
			
			miningPipeDescriptor = descriptor;
		}
  		
	}
    
    void registerSixNodeCache(int id)
    {
    	int subId,completId;
    	String name;	

		SixNodeCacheItem descriptor;
		{
			subId = 0; completId = subId + (id << 6);
			name = "Stone cache";
			
					descriptor = new SixNodeCacheItem(
					 name,
					 this.obj.getObj("ElectricFurnace"),
					 1
					); 
			sharedItem.addElement(completId, descriptor);

		}
  		
	}    
    
    void registerAutoMiner(int id)
    {
    	int subId,completId;
    	String name;
    	{
    		subId = 0;
	    	name = "Auto miner";

	    	AutoMinerDescriptor desc = new AutoMinerDescriptor(
	    			name,
	    			LVU,LVU*1.4,//double nominalVoltage,double maximalVoltage,
	    			1500,0.01,//double nominalPower,double nominalDropFactor,
	    			1,50//double pipeRemoveTime,double pipeRemoveEnergy
	    			);
			
			transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    	}  	
    }
    
    
    void registerRawCable(int id)
    {
    	int subId,completId;
    	String name;	

 
    	{
			GenericItemUsingDamageDescriptor descriptor;
			subId = 0; completId = subId + (id << 6);
			name = "Cooper cable";
			
					descriptor = new GenericItemUsingDamageDescriptor(
					 name
					);
			sharedItem.addElement(completId, descriptor);
		}   
    	{
			GenericItemUsingDamageDescriptor descriptor;
			subId = 1; completId = subId + (id << 6);
			name = "Iron cable";
			
					descriptor = new GenericItemUsingDamageDescriptor(
					 name
					);
			sharedItem.addElement(completId, descriptor);
		}   
    	{
			GenericItemUsingDamageDescriptor descriptor;
			subId = 2; completId = subId + (id << 6);
			name = "Tungsten cable";
			
					descriptor = new GenericItemUsingDamageDescriptor(
					 name
					);
			sharedItem.addElement(completId, descriptor);
		}   
    } 
    void registerBrush(int id)
    {
		int subId,completId;
		String name;
		String[] subNames = {
			"black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink", "lime", "yellow", "lightBlue", "magenta", "orange", "white"
		};	
		for(int idx = 0;idx < 16;idx++)
		{
			subId = idx; name = subNames[idx] + " brush";
	    	BrushDescriptor desc = new BrushDescriptor(
	    			name
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
		}	
    }
    
    void registerMiscItem(int id)
    {
    	int subId,completId;
    	String name;
    	{
    		subId = 0; name = "Cheap chip";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    	
    	{
    		subId = 1; name = "Advanced chip";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	
    	{
    		subId = 2; name = "Machine block";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    
    	{
    		subId = 3; name = "Electrical sensor";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    	
    	{
    		subId = 4; name = "Thermal sensor";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    	
    	
    	{
    		subId = 5; name = "Tin plate";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    	
    	{
    		subId = 6; name = "Cooper plate";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    	  
    	{
    		subId = 7; name = "Iron plate";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    	
    	{
    		subId = 8; name = "Gold plate";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    	   	
    	{
    		subId = 9; name = "Plumb plate";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    	
    	{
    		subId = 10; name = "Silicon plate";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    	
  	  
      	{
      		subId = 11; name = "Steel plate";
    	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
    	    			name,
    	    			new String[]{}
    	    			);			
    	    	sharedItem.addElement(subId + (id << 6), desc);
      	}  	    	
      	{
      		subId = 12; name = "Coal plate";
    	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
    	    			name,
    	    			new String[]{}
    	    			);			
    	    	sharedItem.addElement(subId + (id << 6), desc);
      	}  	    	
      	  

	  /*
	{
		subId = 12; name = "Stone plate";
  	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
  			name,
  			new String[]{}
  			);			
  	sharedItem.addElement(subId + (id << 6), desc);
	}  	    	
	  
	*/
	
    	
    	
    	
    	{
    		subId = 16; name = "Silicon dust";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    	
    	{
    		subId = 17; name = "Silicon ingot";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	    	
    	/*{
    		subId = 20; name = "Macerator sorter module";
    		MaceratorSorterDescriptor desc = new MaceratorSorterDescriptor(
	    			name
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	 */     
 	
    	{
    		subId = 22; name = "Machine booster";
    		MachineBoosterDescriptor desc = new MachineBoosterDescriptor(
	    			name
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	     	
    	{
    		subId = 23; name = "Advanced machine block";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	       	
    	{
    		subId = 28; name = "Basic magnet";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	       	
    	{
    		subId = 29; name = "Advanced magnet";
	    	GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
	    			name,
	    			new String[]{}
	    			);			
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	       	
    	{
    		subId = 32; name = "Data logger print";
    		DataLogsPrintDescriptor desc = new DataLogsPrintDescriptor(
	    			name
	    			);			
    		dataLogsPrintDescriptor = desc;
	    	sharedItem.addElement(subId + (id << 6), desc);
    	}  	       	
    	
    	    	
    	
    	
    	
    }
    
    public DataLogsPrintDescriptor dataLogsPrintDescriptor;
 
	void recipeGround()
	{
		GameRegistry.addRecipe(	findItemStack("Ground cable"),
				" C ", 
				" C ", 
				"CCC",
				Character.valueOf('C'),findItemStack("Cooper cable"));		
	}
	
	void recipeElectricalSource()
	{
		//Trololol
	}
	
	void recipeElectricalCable()
	{
		GameRegistry.addRecipe(	signalCableDescriptor.newItemStack(1),"R", "C",
				Character.valueOf('C'), findItemStack("Iron cable"),
				Character.valueOf('R'), findItemStack("Rubber"));
		GameRegistry.addRecipe(	lowVoltageCableDescriptor.newItemStack(1),"R", "C",
				Character.valueOf('C'), findItemStack("Cooper cable"),
				Character.valueOf('R'), findItemStack("Rubber"));
		GameRegistry.addRecipe(	meduimVoltageCableDescriptor.newItemStack(1),"R", "C",
				Character.valueOf('C'), lowVoltageCableDescriptor.newItemStack(1),
				Character.valueOf('R'), findItemStack("Rubber"));
		GameRegistry.addRecipe(	highVoltageCableDescriptor.newItemStack(1),"R", "C",
				Character.valueOf('C'), meduimVoltageCableDescriptor.newItemStack(1),
				Character.valueOf('R'), findItemStack("Rubber"));		

		GameRegistry.addRecipe(	signalCableDescriptor.newItemStack(6),"RRR", "CCC","RRR",
				Character.valueOf('C'), new ItemStack(Item.ingotIron),
				Character.valueOf('R'), findItemStack("Rubber"));
		GameRegistry.addRecipe(	lowVoltageCableDescriptor.newItemStack(6),"RRR", "CCC","RRR",
				Character.valueOf('C'), findItemStack("Cooper ingot"),
				Character.valueOf('R'), findItemStack("Rubber"));
	

	}
	
	void recipeThermalCable()
	{
		
		GameRegistry.addRecipe(	findItemStack("Cooper thermal cable",6),
				"SSS",
				"CCC",
				"SSS",
				Character.valueOf('S'), new ItemStack(Block.cobblestone),
				Character.valueOf('C'), findItemStack("Cooper ingot"));		
		GameRegistry.addRecipe(	findItemStack("Cooper thermal cable",1),
				"S",
				"C",
				Character.valueOf('S'), new ItemStack(Block.cobblestone),
				Character.valueOf('C'), findItemStack("Cooper cable"));
		
		//for(int idx = 0;idx<16;idx++)
			GameRegistry.addRecipe(	findItemStack("Isolated cooper thermal cable",3),
				" W ",
				" S ",
				"CCC",
				Character.valueOf('W'), findItemStack("Rubber"),// new ItemStack(Block.cloth,1,idx),
				Character.valueOf('S'), new ItemStack(Block.sand),
				Character.valueOf('C'), findItemStack("Cooper thermal cable"));		
		
	}
	
	void recipeLampSocket()
	{
		GameRegistry.addRecipe(	findItemStack("Lamp socket A",3),
				"G ",
				"IG",
				"G ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('I'), new ItemStack(Item.ingotIron)
				);		
		GameRegistry.addRecipe(	findItemStack("Lamp socket B projector",3),
				" I",
				"IG",
				" I",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('I'), new ItemStack(Item.ingotIron)
				);		
				
	}
	 
	void recipeDiode()
	{ 

		GameRegistry.addRecipe(	findItemStack("Signal diode",4),
				" RB",
				"IIR",
				" RB",
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('I'), findItemStack("Iron cable"),
				Character.valueOf('B'), findItemStack("Rubber"));		

		
		GameRegistry.addRecipe(	findItemStack("10A diode",3),
				" RB",
				"IIR",
				" RB",
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('I'), new ItemStack(Item.ingotIron),
				Character.valueOf('B'), findItemStack("Rubber"));		

		GameRegistry.addRecipe(	findItemStack("25A diode"),"D", "D","D",
				Character.valueOf('D'), findItemStack("10A diode"));		
    
		
		
		
	}
	
	void recipeSwitch()     	
	{
		GameRegistry.addRecipe(	findItemStack("Signal voltage switch"),
				" AI",
				"AIA",
				"CRC",
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('A'), findItemStack("Rubber"),
				Character.valueOf('I'), findItemStack("Cooper cable"),
				Character.valueOf('C'), findItemStack("Signal cable"));	
		
		
		GameRegistry.addRecipe(	findItemStack("Low voltage switch"),
				" AI",
				"AIA",
				"CRC",
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('A'), findItemStack("Rubber"),
				Character.valueOf('I'), findItemStack("Cooper cable"),
				Character.valueOf('C'), findItemStack("Low voltage cable"));	
		
		GameRegistry.addRecipe(	findItemStack("Medium voltage switch"),
				" AI",
				"AIA",
				"CRC",
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('A'), findItemStack("Rubber"),
				Character.valueOf('I'), findItemStack("Cooper cable"),
				Character.valueOf('C'), findItemStack("Medium voltage cable"));	
		
		GameRegistry.addRecipe(	findItemStack("High voltage switch"),
				" AI",
				"AIA",
				"CRC",
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('A'), findItemStack("Rubber"),
				Character.valueOf('I'), findItemStack("Cooper cable"),
				Character.valueOf('C'), findItemStack("High voltage cable"));	
	}
	
	void recipeTransformer()
	{
		for(int idx = 0;idx< 4;idx++){
		GameRegistry.addRecipe(	findItemStack("Transformer"),
				"I I",
				"WWW",
				Character.valueOf('W'), new ItemStack(Block.planks,1,idx),
				Character.valueOf('I'), new ItemStack(Item.ingotIron));
		}
	}
	
	void recipeHeatFurnace()
	{
		GameRegistry.addRecipe(	findItemStack("Stone heat furnace"),
				"BIB",
				"BIB",
				"BBB",
				Character.valueOf('B'), new ItemStack(Block.stone),
				Character.valueOf('I'), findItemStack("Combustion chamber"));		
		
		GameRegistry.addRecipe(	findItemStack("Brick heat furnace"),
				"BIB",
				"BIB",
				"BBB",
				Character.valueOf('B'), new ItemStack(Block.brick),
				Character.valueOf('I'), findItemStack("Combustion chamber"));		
	}
	
	void recipeTurbine()
	{
		
		GameRegistry.addRecipe(	findItemStack("Small 50V turbine"),
				" m ",
				"HMH",
				" E ",
				
				Character.valueOf('m'), findItemStack("Electrical motor"),
				Character.valueOf('M'), findItemStack("Machine block"),
				Character.valueOf('E'), findItemStack("Cooper cable"),
				Character.valueOf('H'), findItemStack("Cooper thermal cable")
				);	

		GameRegistry.addRecipe(	findItemStack("50V turbine"),
				" m ",
				"HMH",
				" E ",
				Character.valueOf('M'), findItemStack("Machine block"),
				Character.valueOf('E'), findItemStack("Low voltage cable"),
				Character.valueOf('H'), findItemStack("Cooper thermal cable"),
				Character.valueOf('m'), findItemStack("Electrical motor")
	
				);	
		GameRegistry.addRecipe(	findItemStack("200V turbine"),
				"ImI",
				"HMH",
				"IEI",
				Character.valueOf('I'), findItemStack("Rubber"),
				Character.valueOf('M'), findItemStack("Advanced machine block"),
				Character.valueOf('E'), findItemStack("Medium voltage cable"),
				Character.valueOf('H'), findItemStack("Cooper thermal cable"),
				Character.valueOf('m'), findItemStack("Advanced electrical motor")
				);	
		

		
	}
	
	
	void recipeBattery()
	{
		
		
		GameRegistry.addRecipe(	findItemStack("Cost oriented battery"),
				"C C",
				"PPP",
				"III",
				Character.valueOf('C'), findItemStack("Low voltage cable"),
				Character.valueOf('P'), findItemStack("Plumb ingot"),
				Character.valueOf('I'), new ItemStack(Item.ingotIron));	
		
		GameRegistry.addRecipe(	findItemStack("Capacity oriented battery"),
				"PPP",
				"PBP",
				"PPP",
				Character.valueOf('B'), findItemStack("Cost oriented battery"),
				Character.valueOf('P'), findItemStack("Plumb ingot"));	
				
		GameRegistry.addRecipe(	findItemStack("Voltage oriented battery"),
				"PPP",
				"PBP",
				"PPP",
				Character.valueOf('B'), findItemStack("Cost oriented battery"),
				Character.valueOf('P'),  new ItemStack(Item.ingotIron));	
		
		GameRegistry.addRecipe(	findItemStack("Current oriented battery"),
				"PPP",
				"PBP",
				"PPP",
				Character.valueOf('B'), findItemStack("Cost oriented battery"),
				Character.valueOf('P'), findItemStack("Cooper ingot"));	
		
		GameRegistry.addRecipe(	findItemStack("Life oriented battery"),
				"PPP",
				"PBP",
				"PPP",
				Character.valueOf('B'), findItemStack("Cost oriented battery"),
				Character.valueOf('P'), new ItemStack(Item.ingotGold));	
				
	

	}
	
	void recipeElectricalFurnace()
	{

		GameRegistry.addRecipe(	findItemStack("Electrical furnace"),
				"III",
				"IFI",
				"ICI",
				Character.valueOf('C'), findItemStack("Low voltage cable"),
				Character.valueOf('F'), new ItemStack(Block.furnaceIdle),
				Character.valueOf('I'), new ItemStack(Item.ingotIron));		
	}

	
	void recipeAutoMiner()
	{
		GameRegistry.addRecipe(	findItemStack("Auto miner"),
				"CMC",
				" B ",
				" P ",
				Character.valueOf('C'), findItemStack("Cheap chip"),
				Character.valueOf('B'), findItemStack("Machine block"),
				Character.valueOf('M'), findItemStack("Electrical motor"),
				Character.valueOf('P'), findItemStack("Mining pipe"));		
	}
	
	void recipeSolarPannel()
	{
		GameRegistry.addRecipe(	findItemStack("Small solar pannel"),
				"III",
				"CSC",
				"III",
				Character.valueOf('S'), findItemStack("Silicon plate"),
				Character.valueOf('I'), new ItemStack(Item.ingotIron),
				Character.valueOf('C'), findItemStack("Low voltage cable"));		

		GameRegistry.addRecipe(	findItemStack("Small rotating solar pannel"),
				"ISI",
				"I I",
				Character.valueOf('S'), findItemStack("Small solar pannel"),
				Character.valueOf('I'), new ItemStack(Item.ingotIron));		

	}
	
	void recipeWindTurbine()
	{
		
	}
	

    void recipeGeneral()
    {
    	FurnaceRecipes.smelting().addSmelting(	treeResin.parentItem.itemID,treeResin.parentItemDamage,
				findItemStack("Rubber", 1), 0f);
    	
    	
    	


	}  	    	

	void recipeHeatingCorp()
	{
    	GameRegistry.addRecipe(	findItemStack("Small 50V cooper heating corp"),"CCC", "C C","C C",
				Character.valueOf('C'), findItemStack("Cooper cable"));
    	GameRegistry.addRecipe(	findItemStack("50V cooper heating corp"),"CCC", "C C","C C",
				Character.valueOf('C'), findItemStack("Cooper ingot"));
    	GameRegistry.addRecipe(	findItemStack("Small 200V cooper heating corp"),"CC",
				Character.valueOf('C'), findItemStack("50V cooper heating corp"));
    	GameRegistry.addRecipe(	findItemStack("200V cooper heating corp"),"CC",
				Character.valueOf('C'), findItemStack("Small 200V cooper heating corp")); 
    	
    	GameRegistry.addRecipe(	findItemStack("Small 50V iron heating corp"),"CCC", "C C","C C",
				Character.valueOf('C'), findItemStack("Iron cable"));
    	GameRegistry.addRecipe(	findItemStack("50V iron heating corp"),"CCC", "C C","C C",
				Character.valueOf('C'), new ItemStack(Item.ingotIron));
    	GameRegistry.addRecipe(	findItemStack("Small 200V iron heating corp"),"CC",
				Character.valueOf('C'), findItemStack("50V iron heating corp"));
    	GameRegistry.addRecipe(	findItemStack("200V iron heating corp"),"CC",
				Character.valueOf('C'), findItemStack("Small 200V iron heating corp")); 
    	
    	GameRegistry.addRecipe(	findItemStack("Small 50V tungsten heating corp"),"CCC", "C C","C C",
				Character.valueOf('C'), findItemStack("Tungsten cable"));
    	GameRegistry.addRecipe(	findItemStack("50V tungsten heating corp"),"CCC", "C C","C C",
				Character.valueOf('C'), findItemStack("Tungsten ingot"));
    	GameRegistry.addRecipe(	findItemStack("Small 200V tungsten heating corp"),"CC",
				Character.valueOf('C'), findItemStack("50V tungsten heating corp"));
    	GameRegistry.addRecipe(	findItemStack("200V tungsten heating corp"),"CC",
				Character.valueOf('C'), findItemStack("Small 200V tungsten heating corp")); 			
	}
	void recipeThermalIsolator()
	{
    	GameRegistry.addRecipe(	findItemStack("Sand thermal isolator"),"C C", " C ","C C",
				Character.valueOf('C'), new ItemStack(Block.sand));
    	GameRegistry.addRecipe(	findItemStack("Stone thermal isolator"),"C C", " C ","C C",
				Character.valueOf('C'), new ItemStack(Block.stone));
    	GameRegistry.addRecipe(	findItemStack("Brick thermal isolator"),"C C", " C ","C C",
				Character.valueOf('C'), new ItemStack(Item.brick));
    	/*GameRegistry.addRecipe(	findItemStack("Wood thermal isolator"),"C C", " C ","C C",
				Character.valueOf('C'), new ItemStack(Block.wood));
		*/
		
	}
	void recipeRegulatorItem()
	{


    	GameRegistry.addRecipe(	findItemStack("On/OFF regulator 10%",1),
    			"R R",
    			" R ",
    			" I ",
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('I'), new ItemStack(Item.ingotIron)
    			);	


    	GameRegistry.addRecipe(	findItemStack("On/OFF regulator 10%",1),
    			"RRR",
    			" I ",
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('I'), new ItemStack(Item.ingotIron)
    			);	
    	

    	GameRegistry.addRecipe(	findItemStack("Analogic regulator",1),
    			"R R",
    			" C ",
    			" I ",
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('I'), new ItemStack(Item.ingotIron),
				Character.valueOf('C'), findItemStack("Cheap chip")
    			);	
	}
	void recipeLampItem()
	{
		

		//Tungsten
    	GameRegistry.addRecipe(	findItemStack("Small 50V incandescent light bulb",4)," G ", "GFG"," S ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('F'), findItemStack("Tungsten ingot"),
				Character.valueOf('S'), findItemStack("Cooper cable")
    			);		
    	GameRegistry.addRecipe(	findItemStack("50V incandescent light bulb",4)," G ", "GFG"," S ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('F'), findItemStack("Tungsten ingot"),
				Character.valueOf('S'), findItemStack("Low voltage cable")
    			);
    	GameRegistry.addRecipe(	findItemStack("200V incandescent light bulb",4)," G ", "GFG"," S ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('F'), findItemStack("Tungsten ingot"),
				Character.valueOf('S'), findItemStack("Medium voltage cable")
    			);
    /*	GameRegistry.addRecipe(	findItemStack("400V incandescent light bulb",4)," G ", "GFG"," S ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('F'), findItemStack("Tungsten ingot"),
				Character.valueOf('S'), findItemStack("High voltage cable")
    			);*/
    	//CARBON
    	GameRegistry.addRecipe(	findItemStack("Small 50V carbon incandescent light bulb",4)," G ", "GFG"," S ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('F'), new ItemStack(Item.coal),
				Character.valueOf('S'), findItemStack("Cooper cable")
    			);	    	
    	GameRegistry.addRecipe(	findItemStack("Small 50V carbon incandescent light bulb",4)," G ", "GFG"," S ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('F'), new ItemStack(Item.coal,1,1),
				Character.valueOf('S'), findItemStack("Cooper cable")
    			);		
    	GameRegistry.addRecipe(	findItemStack("50V carbon incandescent light bulb",4)," G ", "GFG"," S ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('F'), new ItemStack(Item.coal),
				Character.valueOf('S'), findItemStack("Low voltage cable")
    			);				
    	GameRegistry.addRecipe(	findItemStack("50V carbon incandescent light bulb",4)," G ", "GFG"," S ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('F'), new ItemStack(Item.coal,1,1),
				Character.valueOf('S'), findItemStack("Low voltage cable")
    			);		

    	
    	GameRegistry.addRecipe(	findItemStack("Small 50V economic light bulb",4),
    			" G ",
    			"GFG",
    			" S ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('F'), findItemStack("Mercury"),
				Character.valueOf('S'), findItemStack("Cooper cable")
    			);		
    	GameRegistry.addRecipe(	findItemStack("50V economic light bulb",4)," G ", "GFG"," S ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('F'), findItemStack("Mercury"),
				Character.valueOf('S'), findItemStack("Low voltage cable")
    			);
    	GameRegistry.addRecipe(	findItemStack("200V economic light bulb",4)," G ", "GFG"," S ",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('F'), findItemStack("Mercury"),
				Character.valueOf('S'), findItemStack("Medium voltage cable")
    			);
	}
	



	
	
	void recipeProtection()
	{
		
 
    	GameRegistry.addRecipe(	findItemStack("OverVoltage protection",4),
    			"SCD", 
    			Character.valueOf('S'),findItemStack("Electrical sensor"),
    			Character.valueOf('C'),findItemStack("Cheap chip"),
    			Character.valueOf('D'),new ItemStack(Item.redstone)
    			);	
    	GameRegistry.addRecipe(	findItemStack("OverHeating protection",4),
    			"SCD", 
    			Character.valueOf('S'),findItemStack("Thermal sensor"),
    			Character.valueOf('C'),findItemStack("Cheap chip"),
    			Character.valueOf('D'),new ItemStack(Item.redstone)
    			);	
		
	}
	
	
	void recipeCombustionChamber()
	{
    	GameRegistry.addRecipe(	findItemStack("Combustion chamber"),
    			" L ",
    			"L L",
    			" L ",
				Character.valueOf('L'), new ItemStack(Block.stone));	
	}
	void recipeFerromagneticCore()   	
	{
    	GameRegistry.addRecipe(	findItemStack("Cheap ferromagnetic core"),"LLL", "L  ","LLL",
				Character.valueOf('L'), Item.ingotIron);
    	GameRegistry.addRecipe(	findItemStack("Average ferromagnetic core"),"LLL", "L  ","LLL",
				Character.valueOf('L'), findItemStack("Ferrite ingot"));
    	GameRegistry.addRecipe(	findItemStack("Optimal ferromagnetic core"),
    			"ll",
    			Character.valueOf('l'), findItemStack("Average ferromagnetic core")
    			);
	}
	

	void recipeIngot()
	{
		//Done
	}
	void recipeDust()
	{
    	GameRegistry.addShapelessRecipe(	findItemStack("Steel dust"),
				findItemStack("Iron dust"),
				findItemStack("Coal dust")
    			);		
    	

	}
	void recipeElectricalMotor()
	{
		
		
    	GameRegistry.addRecipe(	findItemStack("Electrical motor"),
    			" C ",
    			"III",//"PIP",
    			"C C",
				Character.valueOf('I'), new ItemStack(Item.ingotIron),
				Character.valueOf('C'), findItemStack("Low voltage cable")
    			);	
    	

    	GameRegistry.addRecipe(	findItemStack("Advanced electrical motor"),
    			"RCR",
    			"MIM",
    			"CRC",
				Character.valueOf('M'), findItemStack("Basic magnet"),
				Character.valueOf('I'), new ItemStack(Item.ingotIron),
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('C'), findItemStack("Medium voltage cable")
    			);	
    		
    	//TODO
    	/*GameRegistry.addRecipe(	findItemStack("200V electrical motor"),
    			"RFR",
    			"GMG",
    			"RFR",
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('G'), new ItemStack(Item.ingotGold),
				Character.valueOf('M'), findItemStack("50V electrical motor"),
				Character.valueOf('F'), findItemStack("Ferrite ingot")
    			);	*/
	}
	void recipeSolarTracker()
  	{
    	GameRegistry.addRecipe(	findItemStack("Solar tracker"),
    			"VVV",
    			"RQR",
    			"III",
				Character.valueOf('Q'), new ItemStack(Item.netherQuartz),
				Character.valueOf('V'), new ItemStack(Block.thinGlass), 
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('G'), new ItemStack(Item.ingotGold),
				Character.valueOf('I'), new ItemStack(Item.ingotIron)
    			);		
    			
	}
	void recipeDynamo()
  	{
		
	/*	
    	GameRegistry.addRecipe(	findItemStack("Small dynamo"),
    			" C ",
    			"PIP",
    			" C ",
				Character.valueOf('P'), findItemStack("Iron plate"),
				Character.valueOf('I'), findItemStack("Cooper ingot"),
				Character.valueOf('C'), findItemStack("Low voltage cable")
    			);	
    	

    	GameRegistry.addRecipe(	findItemStack("Medium dynamo"),
    			"CPC",
    			"PIP",
    			"CPC",
				Character.valueOf('P'), findItemStack("Iron plate"),
				Character.valueOf('I'), findItemStack("Cooper ingot"),
				Character.valueOf('C'), findItemStack("Low voltage cable")
    			);	
    	
*/
    	
    	//todo
    	/*GameRegistry.addRecipe(	findItemStack("Big dynamo"),
    			"RFR",
    			"GMG",
    			"RFR",
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('G'), new ItemStack(Item.ingotGold),
				Character.valueOf('M'), findItemStack("Big dynamo"),
				Character.valueOf('F'), findItemStack("Ferrite ingot")
    			);		*/	
	}
	void recipeWindRotor()
  	{
		
	}
	void recipeMeter()
  	{
    	GameRegistry.addRecipe(	findItemStack("MultiMeter"),
    			"RGR",
    			"RER",
    			"RCR",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('C'), findItemStack("Electrical sensor"),
				Character.valueOf('E'), new ItemStack(Item.redstone),
				Character.valueOf('R'), findItemStack("Rubber")
    			);		
    	
    	GameRegistry.addRecipe(	findItemStack("ThermoMeter"),"RGR","RER","RCR",
				Character.valueOf('G'), new ItemStack(Block.thinGlass),
				Character.valueOf('C'), findItemStack("Thermal sensor"),
				Character.valueOf('E'), new ItemStack(Item.redstone),
				Character.valueOf('R'), findItemStack("Rubber")
    			);		
    	
    	GameRegistry.addShapelessRecipe(
    			findItemStack("AllMeter"),
				findItemStack("MultiMeter"),
				findItemStack("ThermoMeter")
    			);		
    	
  	}

	void recipeElectricalDrill()
  	{
    	GameRegistry.addRecipe(	findItemStack("Cheap electrical drill"),
    			"CMC",
    			" T ",
       			" P ",
				Character.valueOf('T'), findItemStack("Mining pipe"),
				Character.valueOf('C'), findItemStack("Cheap chip"),
				Character.valueOf('M'), findItemStack("Electrical motor"),
				Character.valueOf('P'), new ItemStack(Item.pickaxeIron)
    			);
    	
    	GameRegistry.addRecipe(	findItemStack("Average electrical drill"),
       			"RCR",
       			" D ",
       			" d ",
				Character.valueOf('R'), Item.redstone,
				Character.valueOf('C'), findItemStack("Cheap chip"),
				Character.valueOf('D'), findItemStack("Cheap electrical drill"),
				Character.valueOf('d'), new ItemStack(Item.diamond)
    			);
    	
    	GameRegistry.addRecipe(	findItemStack("Fast electrical drill"),
    			"MCM",
    			" T ",
       			" P ",
				Character.valueOf('T'), findItemStack("Mining pipe"),
				Character.valueOf('C'), findItemStack("Advanced chip"),
				Character.valueOf('M'), findItemStack("Advanced electrical motor"),
				Character.valueOf('P'), new ItemStack(Item.pickaxeDiamond)
    			);
    	
    	
		

	}
	void recipeOreScanner()
  	{
		
    	GameRegistry.addRecipe(	findItemStack("Basic ore scanner"),
       			"IGI",
       			"RCR",
       			"IGI",
				Character.valueOf('C'), findItemStack("Cheap chip"),
				Character.valueOf('R'), new ItemStack(Item.redstone),
				Character.valueOf('I'), new ItemStack(Item.ingotIron),
				Character.valueOf('G'), new ItemStack(Item.ingotGold)
    			);
    	
    	GameRegistry.addRecipe(	findItemStack("Advanced ore scanner"),
       			"GCG",
       			"RSR",
       			"GRG",
				Character.valueOf('S'), findItemStack("Basic ore scanner"),
				Character.valueOf('C'), findItemStack("Advanced chip"),
				Character.valueOf('G'), new ItemStack(Item.lightStoneDust),
				Character.valueOf('R'), new ItemStack(Item.redstone)
    			);

    
	}
	
	
	
	void recipeMiningPipe()
  	{
    	GameRegistry.addRecipe(	findItemStack("Mining pipe"),"A","A","A",
				Character.valueOf('A'), findItemStack("Steel ingot")
    			);			
	}
	void recipeTreeResinAndRubber()
  	{
		for(int idx = 0;idx < 4;idx++)
		{
			GameRegistry.addRecipe(	findItemStack("tree resin collector"),
       			"W W",
       			" WW",
				Character.valueOf('W'), new ItemStack(Block.planks,1,idx)
    			);
		}
		for(int idx = 0;idx < 4;idx++)
		{
			GameRegistry.addRecipe(	findItemStack("tree resin collector"),
       			"W W",
       			"WW",
				Character.valueOf('W'), new ItemStack(Block.planks,1,idx)
    			);
		}

		
	}
	void recipeRawCable()
  	{
		GameRegistry.addRecipe(	findItemStack("Cooper cable", 6),"III",
				Character.valueOf('I'), findItemStack("Cooper ingot"));
		GameRegistry.addRecipe(	findItemStack("Iron cable", 6),"III",
				Character.valueOf('I'), new ItemStack(Item.ingotIron));
		GameRegistry.addRecipe(	findItemStack("Tungsten cable", 6),"III",
				Character.valueOf('I'), findItemStack("Cooper ingot"));
		
	}
	void recipeMiscItem()
	{
    	GameRegistry.addRecipe(	findItemStack("Cheap chip"),
       			" R ",
       			"RSR",
       			" R ",
				Character.valueOf('S'), findItemStack("Silicon ingot"),
				Character.valueOf('R'), new ItemStack(Item.redstone)
    			);
    	GameRegistry.addRecipe(	findItemStack("Advanced chip"),
       			"LRL",
       			"RCR",
       			"LRL",
				Character.valueOf('C'), findItemStack("Cheap chip"),
				Character.valueOf('L'), new ItemStack(Item.dyePowder,1,4),
				Character.valueOf('R'), new ItemStack(Item.redstone)
    			);
    	
       	GameRegistry.addRecipe(	findItemStack("Machine block"),"LLL", "L L","LLL",
    				Character.valueOf('L'), new ItemStack(Item.ingotIron));
       	
       	GameRegistry.addRecipe(	findItemStack("Advanced machine block"),"LLL", "LCL","LLL",
       				Character.valueOf('C'), findItemStack("Steel plate"),
    				Character.valueOf('L'), findItemStack("Steel ingot"));

    	GameRegistry.addRecipe(	findItemStack("Electrical sensor"),
    			" R ",
    			"RCR",
    			" R ",
				Character.valueOf('C'), findItemStack("High voltage cable"),
				Character.valueOf('R'), new ItemStack(Item.redstone));
    	
    	GameRegistry.addRecipe(	findItemStack("Thermal sensor"),
    			"RIR", 
    			"RGR",
				Character.valueOf('G'), new ItemStack(Item.ingotGold),
				Character.valueOf('I'),  new ItemStack(Item.ingotIron),
				Character.valueOf('R'), new ItemStack(Item.redstone));
    	GameRegistry.addRecipe(	findItemStack("Thermal sensor"),"RGR", "RIR",
				Character.valueOf('G'), new ItemStack(Item.ingotGold),
				Character.valueOf('I'),  new ItemStack(Item.ingotIron),
				Character.valueOf('R'), new ItemStack(Item.redstone));    	
    
	}
  
	
	void recipeMacerator()
	{	 
    	maceratorRecipes.addRecipe(new Recipe(findItemStack("Tin ore"),new ItemStack[]{findItemStack("Tin dust",2)}, 2000.0));
    	maceratorRecipes.addRecipe(new Recipe(findItemStack("Cooper ore"),new ItemStack[]{findItemStack("Cooper dust",2)}, 2000.0));
    	maceratorRecipes.addRecipe(new Recipe(new ItemStack(Block.oreIron),new ItemStack[]{findItemStack("Iron dust",2)}, 2000.0));
    	maceratorRecipes.addRecipe(new Recipe(new ItemStack(Block.oreGold),new ItemStack[]{new ItemStack(Item.goldNugget,18)}, 6000.0));
    	maceratorRecipes.addRecipe(new Recipe(findItemStack("Plumb ore"),new ItemStack[]{findItemStack("Plumb dust",2)}, 2000.0));
    	maceratorRecipes.addRecipe(new Recipe(findItemStack("Tungsten ore"),new ItemStack[]{findItemStack("Tungsten dust",2)}, 5000.0));
    	maceratorRecipes.addRecipe(new Recipe(new ItemStack(Item.coal,1,0),new ItemStack[]{findItemStack("Coal dust",2)}, 1000.0));
    	maceratorRecipes.addRecipe(new Recipe(new ItemStack(Item.coal,1,1),new ItemStack[]{findItemStack("Coal dust",2)}, 1000.0));
    	maceratorRecipes.addRecipe(new Recipe(new ItemStack(Block.sand,2),new ItemStack[]{findItemStack("Silicon dust",1)}, 1000.0));
    	maceratorRecipes.addRecipe(new Recipe(findItemStack("Cinnabar ore"),new ItemStack[]{findItemStack("Cinnabar dust",2)}, 2000.0));
    	 
	}
    
	void recipeExtractor()
	{	 
		extractorRecipes.addRecipe(new Recipe(findItemStack("Tree resin"),new ItemStack[]{findItemStack("Rubber",3)}, 1000.0));
		extractorRecipes.addRecipe(new Recipe(findItemStack("Cinnabar dust"),new ItemStack[]{findItemStack("Purified cinnabar dust",1)}, 1000.0));
	}
	void recipeCompressor()
	{	 
		compressorRecipes.addRecipe(new Recipe(findItemStack("Coal plate",4),new ItemStack[]{new ItemStack(Item.diamond)}, 80000.0));
	//	extractorRecipes.addRecipe(new Recipe(findItemStack("Cinnabar dust"),new ItemStack[]{findItemStack("Purified cinnabar dust",1)}, 1000.0));

		compressorRecipes.addRecipe(new Recipe(findItemStack("Coal dust",4),findItemStack("Coal plate"), 4000.0));
		compressorRecipes.addRecipe(new Recipe(findItemStack("Cooper ingot",4),findItemStack("Cooper plate"), 10000.0));
		compressorRecipes.addRecipe(new Recipe(findItemStack("Plumb ingot",4),findItemStack("Plumb plate"), 10000.0));
		compressorRecipes.addRecipe(new Recipe(findItemStack("Silicon ingot",4),findItemStack("Silicon plate"), 10000.0));
		compressorRecipes.addRecipe(new Recipe(findItemStack("Steel ingot",4),findItemStack("Steel plate"), 10000.0));
		compressorRecipes.addRecipe(new Recipe(new ItemStack(Item.ingotIron,4,0),findItemStack("Iron plate"), 10000.0));
		compressorRecipes.addRecipe(new Recipe(new ItemStack(Item.ingotGold,4,0),findItemStack("Gold plate"), 10000.0));  	

	}    	
	
	void recipemagnetiser()
	{	 
		magnetiserRecipes.addRecipe(new Recipe(new ItemStack(Item.ingotIron),new ItemStack[]{findItemStack("Basic magnet")}, 2000.0));
		magnetiserRecipes.addRecipe(new Recipe(findItemStack("Steel ingot",1),new ItemStack[]{findItemStack("Advanced magnet")}, 8000.0));
	}    
	
	
	void recipeFurnace()
	{    		
		ItemStack in;
		in = findItemStack("Tin ore"); FurnaceRecipes.smelting().addSmelting(in.itemID, in.getItemDamage(),
				findItemStack("Tin ingot"), 0);
		in = findItemStack("Tin dust"); FurnaceRecipes.smelting().addSmelting(in.itemID, in.getItemDamage(),
				findItemStack("Tin ingot"), 0);
		in = findItemStack("Cooper ore"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), findItemStack("Cooper ingot"), 0);
		in = findItemStack("Cooper dust"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), findItemStack("Cooper ingot"), 0);
		in = findItemStack("Plumb ore"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), findItemStack("Plumb ingot"), 0);
		in = findItemStack("Plumb dust"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), findItemStack("Plumb ingot"), 0);
		in = findItemStack("Tungsten ore"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), findItemStack("Tungsten ingot"), 0);
		in = findItemStack("Tungsten dust"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), findItemStack("Tungsten ingot"), 0);
		in = findItemStack("Steel ingot"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), findItemStack("Ferrite ingot"), 0);
		in = findItemStack("Iron dust"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), new ItemStack(Item.ingotIron), 0);
	/*	in = findItemStack("Gold dust"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), new ItemStack(Item.ingotGold), 0);
		*/
		in = findItemStack("Tree resin"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), findItemStack("Rubber"), 0);
		
		in = findItemStack("Steel dust"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), findItemStack("Steel ingot"), 0);
		
		in = findItemStack("Silicon dust"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), findItemStack("Silicon ingot"), 0);
		
		
		in = findItemStack("Purified cinnabar dust"); FurnaceRecipes.smelting().addSmelting(in.itemID,
				in.getItemDamage(), findItemStack("Mercury"), 0);
		
			
		

	
	}
	
	
	
	
	
	
	
    void recipeElectricalSensor()
    {
    	GameRegistry.addRecipe(	findItemStack("Voltage sensor sixNode",1),
    			"ICI", 
    			"ISI", 
    			Character.valueOf('S'),findItemStack("Electrical sensor"),
    			Character.valueOf('C'),findItemStack("Cheap chip"),
    			Character.valueOf('I'),new ItemStack(Item.ingotIron)
    			);	
		    	    	
    	GameRegistry.addRecipe(	findItemStack("Electrical sensor sixNode",1),
    			"RRR", 
    			"ICI", 
    			"ISI", 
    			Character.valueOf('S'),findItemStack("Electrical sensor"),
    			Character.valueOf('C'),findItemStack("Cheap chip"),
    			Character.valueOf('R'),new ItemStack(Item.redstone),
    			Character.valueOf('I'),new ItemStack(Item.ingotIron)
    			);	




	}	
    void recipeThermalSensor()
    {
    	GameRegistry.addRecipe(	findItemStack("Thermal sensor sixNode",1),
    			"RRR", 
    			"ICI", 
    			"ISI", 
    			Character.valueOf('S'),findItemStack("Thermal sensor"),
    			Character.valueOf('C'),findItemStack("Cheap chip"),
    			Character.valueOf('R'),new ItemStack(Item.redstone),
    			Character.valueOf('I'),new ItemStack(Item.ingotIron)
    			);	



	}	
    void recipeMachine()
    {
    	GameRegistry.addRecipe(	findItemStack("50V macerator",1),
    			"IRI", 
    			"FMF", 
    			"IcI",
    			Character.valueOf('M'),findItemStack("Machine block"),
    			Character.valueOf('c'),findItemStack("Low voltage cable"),
    			Character.valueOf('F'),new ItemStack(Item.flint),
    			Character.valueOf('I'),new ItemStack(Item.ingotIron),
    			Character.valueOf('R'),new ItemStack(Item.redstone)
    			);	
    	GameRegistry.addRecipe(	findItemStack("200V macerator",1),
    			"ICI", 
    			"DMD", 
    			"IcI",
    			Character.valueOf('M'),findItemStack("Advanced machine block"),
    			Character.valueOf('C'),findItemStack("Advanced chip"),
    			Character.valueOf('c'),findItemStack("Medium voltage cable"),
    			Character.valueOf('D'),new ItemStack(Item.diamond),
    			Character.valueOf('I'),findItemStack("Steel ingot")
    			);	

    	GameRegistry.addRecipe(	findItemStack("50V extractor",1),
    			"IRI", 
    			"FMF", 
    			"IcI",
    			Character.valueOf('M'),findItemStack("Machine block"),
    			Character.valueOf('c'),findItemStack("Low voltage cable"),
    			Character.valueOf('F'),new ItemStack(Item.dyePowder,1,4),
    			Character.valueOf('I'),new ItemStack(Item.ingotIron),
    			Character.valueOf('R'),new ItemStack(Item.redstone)
    			);	
    	GameRegistry.addRecipe(	findItemStack("200V extractor",1),
    			"ICI", 
    			"DMD", 
    			"IcI",
    			Character.valueOf('M'),findItemStack("Advanced machine block"),
    			Character.valueOf('C'),findItemStack("Advanced chip"),
    			Character.valueOf('c'),findItemStack("Medium voltage cable"),
    			Character.valueOf('D'),new ItemStack(Item.dyePowder,1,4),
    			Character.valueOf('I'),findItemStack("Steel ingot")
    			);

    	GameRegistry.addRecipe(	findItemStack("50V compressor",1),
    			"IRI", 
    			"FMF", 
    			"IcI",
    			Character.valueOf('M'),findItemStack("Machine block"),
    			Character.valueOf('c'),findItemStack("Low voltage cable"),
    			Character.valueOf('F'),findItemStack("Iron plate"),
    			Character.valueOf('I'),new ItemStack(Item.ingotIron),
    			Character.valueOf('R'),new ItemStack(Item.redstone)
    			);	
    	GameRegistry.addRecipe(	findItemStack("200V compressor",1),
    			"ICI", 
    			"DMD", 
    			"IcI",
    			Character.valueOf('M'),findItemStack("Advanced machine block"),
    			Character.valueOf('C'),findItemStack("Advanced chip"),
    			Character.valueOf('c'),findItemStack("Medium voltage cable"),
    			Character.valueOf('D'),findItemStack("Steel plate"),
    			Character.valueOf('I'),findItemStack("Steel ingot")
    			);	
    	
    	
    	GameRegistry.addRecipe(	findItemStack("50V magnetizer",1),
    			"IRI", 
    			"cMc", 
    			"IcI",
    			Character.valueOf('M'),findItemStack("Machine block"),
    			Character.valueOf('c'),findItemStack("Low voltage cable"),
    			Character.valueOf('I'),new ItemStack(Item.ingotIron),
    			Character.valueOf('R'),new ItemStack(Item.redstone)
    			);	
    	GameRegistry.addRecipe(	findItemStack("200V magnetizer",1),
    			"ICI", 
    			"cMc", 
    			"IcI",
    			Character.valueOf('M'),findItemStack("Advanced machine block"),
    			Character.valueOf('C'),findItemStack("Advanced chip"),
    			Character.valueOf('c'),findItemStack("Medium voltage cable"),
    			Character.valueOf('I'),findItemStack("Steel ingot")
    			);	
    	



	}	
    
    

	
	
	
    public ItemStack findItemStack(String name,int stackSize)
    {
    	return GameRegistry.findItemStack("Eln",name, stackSize);
    }
    public ItemStack findItemStack(String name)
    {
    	return findItemStack(name, 1);
    }
}