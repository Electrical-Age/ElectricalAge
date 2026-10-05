package mods.eln.registry;

import mods.eln.ElnContent;
import mods.eln.compat.GameRegistryCompat;
import mods.eln.sixnode.lampsocket.LightBlock;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * The device parts of Eln's lifecycle, moved out of Eln.java (1.12 port, M1 split); see mods.eln.ElnContent.
 * Only PORTED devices are called here. The 1.7.10 calls of devices that are not ported yet are kept in place as
 * `// PENDING(1.12 WPn): call;` lines (grep PENDING): when a device lands, move its register method from
 * ElnDeviceRegistryPending to ElnDeviceRegistry and un-comment its line. Sub-UIDs must not change (rule 6).
 * Recipes (ElnRecipes) are all pending: TODO(1.12 WP15).
 */
public class ElnContentImpl implements ElnContent {
    @Override
    public void preInit() {
        // PENDING(1.12 WP12): ReplicatorPopProcess.popPerSecondPerPlayer = replicatorPopPerSecondPerPlayer;
        // PENDING(1.12 WP14): oreRegenerate = new OreRegenerate();
        // PENDING(1.12 WP14): oreBlock = (OreBlock) new OreBlock().setCreativeTab(creativeTab).setTranslationKey("OreEln");
        lightBlock = new LightBlock();
        GameRegistryCompat.registerBlock(lightBlock, null, "Eln.lightBlock"); // eln:light_block
        // PENDING(1.12 WP14): GameRegistryCompat.registerBlock(oreBlock, OreItem.class, "Eln.Ore");
        GameRegistry.registerTileEntity(LightBlockEntity.class, new ResourceLocation(MODID, "light_block_entity"));
        // PENDING(1.12 WP14): oreItem = (OreItem) GameRegistryCompat.getItemBlock(oreBlock);

        // PENDING(1.12 WP11): registerTestBlock();
        // PENDING(1.12 WP11): registerEnergyConverter();
        // registerComputer(): OpenComputers/ComputerCraft probe dropped (rule 8)

        // PENDING(1.12 WP12): registerArmor();
        // PENDING(1.12 WP12): registerTool();
        // PENDING(1.12 WP14): registerOre();

        //SIX NODE REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerGround(2);
        registerElectricalSource(3);
        registerElectricalCable(32);
        // PENDING(1.12 WP9): registerThermalCable(48);
        registerLampSocket(64);
        registerLampSupply(65);
        // PENDING(1.12 WP9): registerBatteryCharger(66);
        // PENDING(1.12 WP9): registerPowerSocket(67);

        // PENDING(1.12 WP9): registerWirelessSignal(92);
        // PENDING(1.12 WP9): registerElectricalDataLogger(93);
        // PENDING(1.12 WP9): registerElectricalRelay(94);
        // PENDING(1.12 WP9): registerElectricalGateSource(95);
        // PENDING(1.12 WP9): registerPassiveComponent(96);
        // PENDING(1.12 WP9): registerSwitch(97);
        // PENDING(1.12 WP9): registerElectricalManager(98);
        // PENDING(1.12 WP9): registerElectricalSensor(100);
        // PENDING(1.12 WP9): registerThermalSensor(101);
        // PENDING(1.12 WP9): registerElectricalVuMeter(102);
        // PENDING(1.12 WP9): registerElectricalAlarm(103);
        // PENDING(1.12 WP9): registerElectricalEnvironmentalSensor(104);
        // PENDING(1.12 WP9): registerElectricalRedstone(108);
        // PENDING(1.12 WP9): registerElectricalGate(109);
        // PENDING(1.12 WP9): registerTreeResinCollector(116);
        // PENDING(1.12 WP9): registerSixNodeMisc(117);
        // PENDING(1.12 WP9): registerLogicalGates(118);
        // PENDING(1.12 WP9): registerAnalogChips(124);

        //TRANSPARENT NODE REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        // PENDING(1.12 WP10): registerPowerComponent(1);
        // PENDING(1.12 WP10): registerTransformer(2);
        // PENDING(1.12 WP10): registerHeatFurnace(3);
        // PENDING(1.12 WP10): registerTurbine(4);
        // PENDING(1.12 WP10): registerElectricalAntenna(7);
        registerBattery(16);
        // PENDING(1.12 WP10): registerElectricalFurnace(32);
        // PENDING(1.12 WP10): registerMacerator(33);
        // PENDING(1.12 WP10): registerCompressor(35);
        // PENDING(1.12 WP10): registerMagnetizer(36);
        // PENDING(1.12 WP10): registerPlateMachine(37);
        // PENDING(1.12 WP10): registerEggIncubator(41);
        // PENDING(1.12 WP10): registerAutoMiner(42);
        // PENDING(1.12 WP10): registerSolarPanel(48);
        // PENDING(1.12 WP10): registerWindTurbine(49);
        // PENDING(1.12 WP10): registerThermalDissipatorPassiveAndActive(64);
        // PENDING(1.12 WP10): registerTransparentNodeMisc(65);
        // PENDING(1.12 WP10): registerTurret(66);
        // PENDING(1.12 WP10): registerFuelGenerator(67);
        // PENDING(1.12 WP11): registerGridDevices(123);

        //ITEM REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        // PENDING(1.12 WP12): registerHeatingCorp(1);
        // registerThermalIsolator(2);
        // PENDING(1.12 WP12): registerRegulatorItem(3);
        registerLampItem(4);
        // PENDING(1.12 WP12): registerProtection(5);
        // PENDING(1.12 WP12): registerCombustionChamber(6);
        // PENDING(1.12 WP12): registerFerromagneticCore(7);
        // PENDING(1.12 WP12): registerIngot(8);
        // PENDING(1.12 WP12): registerDust(9);
        // PENDING(1.12 WP12): registerElectricalMotor(10);
        // PENDING(1.12 WP12): registerSolarTracker(11);
        //
        registerMeter(14);
        // PENDING(1.12 WP12): registerElectricalDrill(15);
        // PENDING(1.12 WP12): registerOreScanner(16);
        // PENDING(1.12 WP12): registerMiningPipe(17);
        // PENDING(1.12 WP12): registerTreeResinAndRubber(64);
        // PENDING(1.12 WP12): registerRawCable(65);
        // PENDING(1.12 WP12): registerBrush(119);
        // PENDING(1.12 WP12): registerMiscItem(120);
        // PENDING(1.12 WP12): registerElectricalTool(121);
        // PENDING(1.12 WP12): registerPortableItem(122);
        // PENDING(1.12 WP12): registerFuelBurnerItem(124);

        // Register WIP items only on development runs!
        // PENDING(1.12 WP12): if (isDevelopmentRun()) registerWipItems();
    }

    @Override
    public void modsLoaded() {
        // PENDING(1.12 WP15): recipeMaceratorModOres();
    }

    @Override
    public void init() {
        // PENDING(1.12 WP14): registerReplicator();
        // PENDING(1.12 WP15): all recipes (ElnRecipes: recipeEnergyConverter() ... recipeGridDevices(oreNames)),
        // in the 1.7.10 order of ElnContentImpl before the WP8 slice (git show ddb6a583:src/main/java/mods/eln/registry/ElnContentImpl.java).
    }

    @Override
    public void serverAboutToStart() {
        // PENDING(1.12 WP10): TeleporterElement.teleporterList.clear();
        LightBlockEntity.observers.clear();
        // PENDING(1.12 WP9): WirelessSignalTxElement.channelMap.clear();
        LampSupplyElement.channelMap.clear();
        // PENDING(1.12 WP9): PowerSocketElement.channelMap.clear();

        // PENDING(1.12 WP14): if (replicatorPop) simulator.addSlowProcess(new ReplicatorPopProcess());
        // PENDING(1.12 WP12): simulator.addSlowProcess(itemEnergyInventoryProcess = new ItemEnergyInventoryProcess());
    }

    @Override
    public void serverStarting() {
        // PENDING(1.12 WP12): regenOreScannerFactors();
    }

    @Override
    public void serverStopped() {
        // PENDING(1.12 WP9): TutorialSignElement.resetBalise();
        LightBlockEntity.observers.clear();
        // PENDING(1.12 WP10): TeleporterElement.teleporterList.clear();
        // PENDING(1.12 WP9): IWirelessSignalSpot.spots.clear();
        // PENDING(1.12 WP14): oreRegenerate.clear();
        LampSupplyElement.channelMap.clear();
        // PENDING(1.12 WP9): PowerSocketElement.channelMap.clear();
        // PENDING(1.12 WP9): WirelessSignalTxElement.channelMap.clear();
    }

    @Override
    public void serverTick() {
        // PENDING(1.12 WP12): TreeCapitation.INSTANCE.process(0.05);
    }

    @Override
    public void clientInit() {
        // PENDING(1.12 WP14): RenderingRegistry.registerEntityRenderingHandler(ReplicatorEntity.class, new ReplicatorRender(new ModelSilverfish(), (float) 0.3));
        // PENDING(1.12 WP9): MinecraftForge.EVENT_BUS.register(new TutorialSignOverlay());
    }

    @Override
    public void clientConnected() {
        // PENDING(1.12 WP12): regenOreScannerFactors();
    }

    @Override
    public void openWiki() {
        // PENDING(1.12 WP14): UtilsClient.clientOpenGui(new Root(null));
    }
}
