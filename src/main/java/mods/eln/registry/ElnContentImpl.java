package mods.eln.registry;

import mods.eln.Eln;
import mods.eln.ElnContent;
import mods.eln.compat.GameRegistryCompat;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.entity.ReplicatorPopProcess;
import mods.eln.entity.ReplicatorRender;
import mods.eln.item.electricalinterface.ItemEnergyInventoryProcess;
import mods.eln.item.electricalitem.TreeCapitation;
import mods.eln.misc.UtilsClient;
import mods.eln.ore.OreBlock;
import mods.eln.ore.OreItem;
import mods.eln.server.OreRegenerate;
import mods.eln.sixnode.lampsocket.LightBlock;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import mods.eln.sixnode.powersocket.PowerSocketElement;
import mods.eln.sixnode.tutorialsign.TutorialSignElement;
import mods.eln.sixnode.tutorialsign.TutorialSignOverlay;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;
import mods.eln.transparentnode.teleporter.TeleporterElement;
import mods.eln.wiki.Root;
import net.minecraft.client.model.ModelSilverfish;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Collections;
import java.util.HashSet;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;
import static mods.eln.registry.ElnRecipes.*;

/**
 * The device parts of Eln's lifecycle, moved verbatim out of Eln.java (1.12 port, M1 split). Built only together
 * with the device packages; see mods.eln.ElnContent. TODO(1.12 WP9/WP10/WP11/WP14): re-enable with the devices.
 */
public class ElnContentImpl implements ElnContent {
    @Override
    public void preInit() {
        ReplicatorPopProcess.popPerSecondPerPlayer = replicatorPopPerSecondPerPlayer;
        oreRegenerate = new OreRegenerate();
        oreBlock = (OreBlock) new OreBlock().setCreativeTab(creativeTab).setTranslationKey("OreEln");
        lightBlock = (LightBlock) new LightBlock();
        GameRegistryCompat.registerBlock(lightBlock, null, "ElnDeviceRegistry.lightBlock");
        GameRegistryCompat.registerBlock(oreBlock, OreItem.class, "Eln.Ore");
        GameRegistry.registerTileEntity(LightBlockEntity.class, new ResourceLocation(MODID, "light_block_entity"));
        oreItem = (OreItem) GameRegistryCompat.getItemBlock(oreBlock);

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

    @Override
    public void modsLoaded() {
        recipeMaceratorModOres();
    }

    @Override
    public void init() {
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

    }

    @Override
    public void serverAboutToStart() {
        TeleporterElement.teleporterList.clear();
        //tileEntityDestructor.clear();
        LightBlockEntity.observers.clear();
        WirelessSignalTxElement.channelMap.clear();
        LampSupplyElement.channelMap.clear();
        PowerSocketElement.channelMap.clear();

        if (replicatorPop)
            simulator.addSlowProcess(new ReplicatorPopProcess());
        simulator.addSlowProcess(itemEnergyInventoryProcess = new ItemEnergyInventoryProcess());
    }

    @Override
    public void serverStarting() {
        regenOreScannerFactors();
    }

    @Override
    public void serverStopped() {
        TutorialSignElement.resetBalise();
        LightBlockEntity.observers.clear();
        TeleporterElement.teleporterList.clear();
        IWirelessSignalSpot.spots.clear();
        oreRegenerate.clear();
        LampSupplyElement.channelMap.clear();
        PowerSocketElement.channelMap.clear();
        WirelessSignalTxElement.channelMap.clear();
    }

    @Override
    public void serverTick() {
        TreeCapitation.INSTANCE.process(0.05);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void clientInit() {
        RenderingRegistry.registerEntityRenderingHandler(ReplicatorEntity.class, new ReplicatorRender(new ModelSilverfish(), (float) 0.3));
        MinecraftForge.EVENT_BUS.register(new TutorialSignOverlay());
    }

    @Override
    public void clientConnected() {
        regenOreScannerFactors();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void openWiki() {
        UtilsClient.clientOpenGui(new Root(null));
    }
}
