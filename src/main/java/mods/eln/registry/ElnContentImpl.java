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
 * This class runs the ported core slice (ElnDeviceRegistry) and then, one line each, the device batches
 * (mods.eln.registry.batch.WpXContent; their unported registrations are in registry/pending, excluded).
 * Batch agents edit only their own batch files, never this class. Sub-UIDs must not change (rule 6).
 * Recipes (ElnRecipes) are all pending: TODO(1.12 WP15).
 */
public class ElnContentImpl implements ElnContent {
    /** Device batches (parallel porting work packages), one line each; called after the core slice in every phase. */
    private static final ElnContent[] BATCHES = {
        new mods.eln.registry.batch.Wp9aContent(),
        new mods.eln.registry.batch.Wp9bContent(),
        new mods.eln.registry.batch.Wp9cContent(),
        new mods.eln.registry.batch.Wp10aContent(),
        new mods.eln.registry.batch.Wp10bContent(),
        new mods.eln.registry.batch.Wp11Content(),
        new mods.eln.registry.batch.Wp12Content(),
    };

    @Override
    public void preInit() {
        lightBlock = new LightBlock();
        GameRegistryCompat.registerBlock(lightBlock, null, "Eln.lightBlock"); // eln:light_block
        GameRegistry.registerTileEntity(LightBlockEntity.class, new ResourceLocation(MODID, "light_block_entity"));

        // registerComputer(): OpenComputers/ComputerCraft probe dropped (rule 8)

        //SIX NODE REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerGround(2);
        registerElectricalSource(3);
        registerElectricalCable(32);
        registerLampSocket(64);
        registerLampSupply(65);

        //TRANSPARENT NODE REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerBattery(16);

        //ITEM REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        // registerThermalIsolator(2);
        registerLampItem(4);
        //
        registerMeter(14);
        for (ElnContent b : BATCHES) b.preInit();
    }

    @Override
    public void modsLoaded() {
        // PENDING(1.12 WP15): recipeMaceratorModOres();
        for (ElnContent b : BATCHES) b.modsLoaded();
    }

    @Override
    public void init() {
        // PENDING(1.12 WP15): all recipes (ElnRecipes: recipeEnergyConverter() ... recipeGridDevices(oreNames)),
        // in the 1.7.10 order of ElnContentImpl before the WP8 slice (git show ddb6a583:src/main/java/mods/eln/registry/ElnContentImpl.java).
        for (ElnContent b : BATCHES) b.init();
    }

    @Override
    public void serverAboutToStart() {
        LightBlockEntity.observers.clear();
        LampSupplyElement.channelMap.clear();
        for (ElnContent b : BATCHES) b.serverAboutToStart();
    }

    @Override
    public void serverStarting() {
        for (ElnContent b : BATCHES) b.serverStarting();
    }

    @Override
    public void serverStopped() {
        LightBlockEntity.observers.clear();
        LampSupplyElement.channelMap.clear();
        for (ElnContent b : BATCHES) b.serverStopped();
    }

    @Override
    public void serverTick() {
        for (ElnContent b : BATCHES) b.serverTick();
    }

    @Override
    public void clientInit() {
        for (ElnContent b : BATCHES) b.clientInit();
    }

    @Override
    public void clientConnected() {
        for (ElnContent b : BATCHES) b.clientConnected();
    }

    @Override
    public void openWiki() {
        for (ElnContent b : BATCHES) b.openWiki();
    }
}
