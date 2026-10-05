package mods.eln.registry.batch;

import mods.eln.ElnContent;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp12 (1.12 port): items, tools, armour, electrical items, ore and worldgen, replicator entity, wiki.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * Sources: m1-exclude-wp12.txt (delete lines to build them). Not yet ported registrations:
 * registry/pending/Wp12Pending.java (excluded). To port a device: move its register method (and the fields it
 * sets) from Wp12Pending into this class, un-comment its PENDING line below, point device code at
 * Wp12Content.&lt;field&gt;. Keep the ids (sub-UIDs) unchanged. Selftest cases: mods.eln.selftest.cases.Wp12Cases.
 */
public class Wp12Content implements ElnContent {
    @Override
    public void preInit() {
        // PENDING(1.12 wp12): ReplicatorPopProcess.popPerSecondPerPlayer = replicatorPopPerSecondPerPlayer;
        // PENDING(1.12 wp12): oreRegenerate = new OreRegenerate();
        // PENDING(1.12 wp12): oreBlock = (OreBlock) new OreBlock().setCreativeTab(creativeTab).setTranslationKey("OreEln");
        // PENDING(1.12 wp12): GameRegistryCompat.registerBlock(oreBlock, OreItem.class, "Eln.Ore");
        // PENDING(1.12 wp12): oreItem = (OreItem) GameRegistryCompat.getItemBlock(oreBlock);
        // PENDING(1.12 wp12): registerArmor();
        // PENDING(1.12 wp12): registerTool();
        // PENDING(1.12 wp12): registerOre();
        // PENDING(1.12 wp12): registerHeatingCorp(1);
        // PENDING(1.12 wp12): registerRegulatorItem(3);
        // PENDING(1.12 wp12): registerProtection(5);
        // PENDING(1.12 wp12): registerCombustionChamber(6);
        // PENDING(1.12 wp12): registerFerromagneticCore(7);
        // PENDING(1.12 wp12): registerIngot(8);
        // PENDING(1.12 wp12): registerDust(9);
        // PENDING(1.12 wp12): registerElectricalMotor(10);
        // PENDING(1.12 wp12): registerSolarTracker(11);
        // PENDING(1.12 wp12): registerElectricalDrill(15);
        // PENDING(1.12 wp12): registerOreScanner(16);
        // PENDING(1.12 wp12): registerMiningPipe(17);
        // PENDING(1.12 wp12): registerTreeResinAndRubber(64);
        // PENDING(1.12 wp12): registerRawCable(65);
        // PENDING(1.12 wp12): registerBrush(119);
        // PENDING(1.12 wp12): registerMiscItem(120);
        // PENDING(1.12 wp12): registerElectricalTool(121);
        // PENDING(1.12 wp12): registerPortableItem(122);
        // PENDING(1.12 wp12): registerFuelBurnerItem(124);
        // PENDING(1.12 wp12): if (isDevelopmentRun()) registerWipItems();
    }

    @Override
    public void init() {
        // PENDING(1.12 wp12): registerReplicator();
    }

    @Override
    public void serverAboutToStart() {
        // PENDING(1.12 wp12): if (replicatorPop) simulator.addSlowProcess(new ReplicatorPopProcess());
        // PENDING(1.12 wp12): simulator.addSlowProcess(itemEnergyInventoryProcess = new ItemEnergyInventoryProcess());
    }

    @Override
    public void serverStarting() {
        // PENDING(1.12 wp12): regenOreScannerFactors();
    }

    @Override
    public void serverStopped() {
        // PENDING(1.12 wp12): oreRegenerate.clear();
    }

    @Override
    public void serverTick() {
        // PENDING(1.12 wp12): TreeCapitation.INSTANCE.process(0.05);
    }

    @Override
    public void clientInit() {
        // PENDING(1.12 wp12): RenderingRegistry.registerEntityRenderingHandler(ReplicatorEntity.class, new ReplicatorRender(new ModelSilverfish(), (float) 0.3));
    }

    @Override
    public void clientConnected() {
        // PENDING(1.12 wp12): regenOreScannerFactors();
    }

    @Override
    public void openWiki() {
        // PENDING(1.12 wp12): UtilsClient.clientOpenGui(new Root(null));
    }
}
