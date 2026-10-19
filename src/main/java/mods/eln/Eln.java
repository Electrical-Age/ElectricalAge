package mods.eln;

import mods.eln.sim.Simulator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Electrical Age for Minecraft 26.x (port in progress, plan: notes/port26/PLAN.md). P0: the simulator runs on the
 * server tick; no devices yet. Keep the static state here trivial: the sim unit tests touch Eln.simulator without
 * booting Minecraft (registries live in their own classes).
 */
@Mod(Eln.MODID)
public class Eln {
    /** Global wind state (1.12: Eln.wind); saved with the overworld. */
    /** Wireless signal transmitter range (1.12 config wireless.txRange, default 32). */
    public static int wirelessTxRange = 32;

    /** 1.12 config balancing.heatTurbinePowerFactor (default 1). */
    public static double heatTurbinePowerFactor = 1;
    public static double windTurbinePowerFactor = 1;
    public static double waterTurbinePowerFactor = 1;
    public static double fuelGeneratorPowerFactor = 1;
    public static double fuelHeatFurnacePowerFactor = 1;
    /** 1.12 config fuelGeneratorTankCapacity [s of nominal power]. */
    public static double fuelGeneratorTankCapacity = 20 * 60;
    /** 1.12 config balancing.fuelHeatValueFactor: J per mB of fuel = factor * real heating value per litre. */
    public static double fuelHeatValueFactor = 0.0000675;

    /** 1.12 config compatibility.ElnToOtherEnergyConverterEnable (true) and the FE ratio balancing.ElnToThermalExpansionConversionRatio (4/3 FE per J). */
    public static boolean ElnToOtherEnergyConverterEnable = true;
    public static double ElnToTeConversionRatio = 1.0 / 3.0 * 4;

    public static boolean modbusEnable = false; // 1.12 port: Modbus TCP server dropped; RTU devices stay off

    public static mods.eln.server.ServerEventListener serverEventListener;

    /** Ingots and dusts (1.12 Eln statics; registered by Wp12a). */
    public static mods.eln.generic.GenericItemUsingDamageDescriptorWithComment tinIngot, silverIngot, plumbIngot, tungstenIngot;
    public static mods.eln.generic.GenericItemUsingDamageDescriptorWithComment dustTin, dustCopper, dustSilver;

    /** Lamp lifetimes [s] (1.12 config lamp.*LifeInHours: 16, 64, 6, 512 h). */
    public static double incandescentLampLife = 16.0 * 3600, economicLampLife = 64.0 * 3600, carbonLampLife = 6.0 * 3600,
        ledLampLife = 512.0 * 3600;
    public static boolean ledLampInfiniteLife = false;
    /** 1.12 config entity.killMonstersAroundLamps (true) / killMonstersAroundLampsRange (9). */
    public static boolean killMonstersAroundLamps = true;
    public static int killMonstersAroundLampsRange = 9;

    /** 1.12 config mapGenerate.* defaults (production too): only tungsten; 1.12 forced cinnabar off in code. */
    public static boolean genLead = true, genTungsten = true, genCinnabar = false;

    /**
     * 1.12 config dictionary.tungsten (false: EA-private oreElnTungsten ...) and dictionary.chips (true: circuitBasic /
     * circuitAdvanced, shared with other mods): the ore names, mapped to tags by OreDictTags.
     */
    public static String dictTungstenOre = "oreElnTungsten", dictTungstenDust = "dustElnTungsten", dictTungstenIngot = "ingotElnTungsten";
    public static String dictCheapChip = "circuitBasic", dictAdvancedChip = "circuitAdvanced";

    /** 1.12 config balancing.autominerRange (10): maximum horizontal mining distance. */
    public static int autominerRange = 10;

    /** 1.12 config balancing.platesPerIngot (1). */
    public static int plateConversionRatio = 1;

    /** 1.12 config entity.replicatorPop (true) and replicatorPopWhenThunderPerSecond (1/120). */
    public static boolean replicatorPop = true;
    public static double replicatorPopPerSecondPerPlayer = 1.0 / 120;

    /** 1.12 config xrayscannerconfig.rangeInBloc (default 5, clamped 4..10); 26.x default 7.5 (Baughn's playtests). */
    public static float xRayScannerRange = 7.5f;
    /** 1.12 config xrayscannerconfig.canBeCrafted (true). */
    public static boolean xRayScannerCanBeCrafted = true;

    /** Items shown as "furnace" machines for smelting recipes (vanilla furnace, EA electrical furnaces). */
    public static java.util.ArrayList<net.minecraft.world.item.ItemStack> furnaceList = new java.util.ArrayList<>();

    /** Machine recipes (1.12 Eln; filled by ElnRecipes, P5). */
    public static mods.eln.misc.RecipesList maceratorRecipes = new mods.eln.misc.RecipesList();
    /** 26.x oil: tar sand -> sand (+ crude into the separator's tank). */
    public static mods.eln.misc.RecipesList separatorRecipes = new mods.eln.misc.RecipesList();
    public static mods.eln.misc.RecipesList compressorRecipes = new mods.eln.misc.RecipesList();
    public static mods.eln.misc.RecipesList plateMachineRecipes = new mods.eln.misc.RecipesList();
    public static mods.eln.misc.RecipesList magnetiserRecipes = new mods.eln.misc.RecipesList();

    public static mods.eln.misc.WindProcess wind = new mods.eln.misc.WindProcess();

    public static final String MODID = "eln";
    public static final Logger LOG = LoggerFactory.getLogger("eln");

    // [simulation] defaults of the 1.12 config (electricalFrequency 20, electricalInterSystemOverSampling 50,
    // thermalFrequency 400); the 26.x config comes with P1.
    public static double electricalFrequency = 20, thermalFrequency = 400;
    public static int electricalInterSystemOverSampling = 50;

    public static Simulator simulator;

    /** Device batches ported from 1.12 (registry/batch), in the 1.12 order. */
    public static final java.util.List<ElnContent> CONTENT = java.util.List.of(
        new mods.eln.registry.batch.Wp9aContent(),
        new mods.eln.registry.batch.Wp9bContent(),
        new mods.eln.registry.batch.Wp9cContent(),
        new mods.eln.registry.batch.Wp10aContent(),
        new mods.eln.registry.batch.Wp10bContent(),
        new mods.eln.registry.batch.Wp11Content(),
        new mods.eln.registry.batch.Wp12aContent(),
        new mods.eln.registry.batch.Wp12bContent(),
        new mods.eln.registry.batch.Wp12cContent(),
        new mods.eln.registry.batch.Wp12dContent(),
        new mods.eln.registry.batch.Wp12eContent());

    // EA packet ids (first byte of an ElnNet.Data payload), as in 1.12
    public static final byte packetPlayerKey = 14;
    public static final byte packetNodeSingleSerialized = 15;
    public static final byte packetPublishForNode = 16;
    public static final byte packetOpenLocalGui = 17;
    public static final byte packetForClientNode = 18;
    public static final byte packetPlaySound = 19;
    public static final byte packetDestroyUuid = 20;
    public static final byte packetClientToServerConnection = 21;
    public static final byte packetServerToClientInfo = 22;
    /** 26.x: a cable kit's preview outlines (node.voxel.KitPreview). */
    public static final byte packetKitPreview = 23;
    /** 26.x, client -> server: the held chisel's brush from the radial menu (size byte, shape byte; ChiselItem.applyBrush). */
    public static final byte packetChiselBrush = 24;

    // Voltage / power levels, as in 1.12
    public static final double gateOutputCurrent = 0.100;
    public static final double SVU = 50, SVII = gateOutputCurrent / 50, SVUinv = 1.0 / SVU;
    public static final double LVU = 50, MVU = 200, HVU = 800, VVU = 3200;
    public static final double SVP = gateOutputCurrent * SVU;
    /** config balancing.cablePowerFactor (1.12 default 1.0; config arrives with the ModConfigSpec) */
    public static double cablePowerFactor = 1.0;

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
    public static final mods.eln.sim.ThermalLoadInitializer cableThermalLoadInitializer = new mods.eln.sim.ThermalLoadInitializer(
        cableWarmLimit, -100, cableHeatingTime, cableThermalConductionTao);
    public static final mods.eln.sim.ThermalLoadInitializer sixNodeThermalLoadInitializer = new mods.eln.sim.ThermalLoadInitializer(
        cableWarmLimit, -100, cableHeatingTime, 1000);

    /** config balancing.wailaEasyMode: more detailed probe info on some machines */
    public static boolean wailaEasyMode = false;

    /** EA's OBJ models (assets/eln/model), loaded at mod construction like 1.12 preInit: descriptors take parts. */
    public static mods.eln.misc.Obj3DFolder obj = new mods.eln.misc.Obj3DFolder();

    public static mods.eln.cable.CableRenderDescriptor stdCableRenderSignal, stdCableRender50V, stdCableRender200V,
        stdCableRender800V, stdCableRender3200V;

    /** config balancing.batteryCapacityFactor (1.12 default 1.0) */
    public static double batteryCapacityFactor = 1.0;
    /** battery half life in s (1.12: 2 * Utils.minecraftDay) */
    public static final double stdBatteryHalfLife = 2 * mods.eln.misc.Utils.minecraftDay;
    public static mods.eln.misc.FunctionTable batteryVoltageFunctionTable;

    /** config balancing.solarPanelPowerFactor (1.12 default 1.0) */
    public static double solarPanelPowerFactor = 1.0;
    public static final double solarPanelBasePower = 65.0;

    /** config gameplay.explosion (1.12 meaning: the remaining real explosions; see Failure). */
    public static boolean explosionEnable = true;

    /** Registered blocks the node code compares against; set by the registry (P1 step 2+). */
    public static net.minecraft.world.level.block.Block sixNodeBlock, ghostBlock;
    public static mods.eln.ghost.GhostManager ghostManager;
    public static mods.eln.node.NodeManager nodeManager;

    /** EA items by their 1.12 display name (1.12 findItemStack looked up the GameRegistry by name). Filled at registration. */
    public static final java.util.Map<String, net.minecraft.world.item.Item> itemsByName = new java.util.HashMap<>();

    /** An EA item by its 1.12 name (e.g. "Cheap Chip"); EMPTY (logged) if unknown, like the 1.12 port. */
    public static net.minecraft.world.item.ItemStack findItemStack(String name, int stackSize) {
        net.minecraft.world.item.Item it = itemsByName.get(name);
        if (it == null) {
            LOG.warn("Electrical Age: findItemStack: no item named '{}'", name);
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        return new net.minecraft.world.item.ItemStack(it, stackSize);
    }

    public static net.minecraft.world.item.ItemStack findItemStack(String name) {
        return findItemStack(name, 1);
    }

    /** Plain item descriptors by 1.12 id (1.12: Eln.sharedItem / sharedItemStackOne, items with damage sub-items). */
    public static final mods.eln.node.DescriptorRegistry<mods.eln.generic.GenericItemUsingDamageDescriptor> sharedItem =
        new mods.eln.node.DescriptorRegistry<>(mods.eln.generic.GenericItemUsingDamageDescriptor.class);
    public static final mods.eln.node.DescriptorRegistry<mods.eln.generic.GenericItemUsingDamageDescriptor> sharedItemStackOne =
        new mods.eln.node.DescriptorRegistry<>(mods.eln.generic.GenericItemUsingDamageDescriptor.class);

    /** Device descriptors by 1.12 id (1.12: Eln.sixNodeItem / transparentNodeItem). */
    public static final mods.eln.node.DescriptorRegistry<mods.eln.node.six.SixNodeDescriptor> sixNodeItem =
        new mods.eln.node.DescriptorRegistry<>(mods.eln.node.six.SixNodeDescriptor.class);
    public static final mods.eln.node.DescriptorRegistry<mods.eln.node.transparent.TransparentNodeDescriptor> transparentNodeItem =
        new mods.eln.node.DescriptorRegistry<>(mods.eln.node.transparent.TransparentNodeDescriptor.class);

    public Eln(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SYNCED, ElnConfig.SPEC);
        modBus.addListener(net.neoforged.fml.event.config.ModConfigEvent.Loading.class, e -> {
            if (e.getConfig().getSpec() == ElnConfig.SPEC) ElnConfig.refresh();
        });
        modBus.addListener(net.neoforged.fml.event.config.ModConfigEvent.Reloading.class, e -> {
            if (e.getConfig().getSpec() == ElnConfig.SPEC) ElnConfig.refresh();
        });
        simulator = new Simulator(0.05, 1 / electricalFrequency, electricalInterSystemOverSampling, 1 / thermalFrequency);
        nodeManager = new mods.eln.node.NodeManager("caca");
        ghostManager = new mods.eln.ghost.GhostManager("caca2");
        mods.eln.server.ElnWorldSaves.register();
        mods.eln.node.NodeManager.registerUuid("s", mods.eln.node.six.SixNode.class);
        mods.eln.node.NodeManager.registerUuid("t", mods.eln.node.transparent.TransparentNode.class);
        mods.eln.node.NodeManager.registerUuid(mods.eln.node.voxel.VoxelNode.UUID, mods.eln.node.voxel.VoxelNode.class);   // (pipes; never unregister)
        // FE exporters saved while they were simple nodes load as transparent nodes
        mods.eln.node.NodeManager.registerTagUpgrade(mods.eln.transparentnode.energyconverter.ExporterSaveUpgrade.OLD_TYPE,
            mods.eln.transparentnode.energyconverter.ExporterSaveUpgrade::upgrade);
        NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, e -> {
            // item components are bound only once the server's registries are: ItemStacks can be made from here on
            mods.eln.registry.ElnRegistry.afterRegistration();
            mods.eln.registry.ElnMachineRecipes.registerAll();
            simulator.init();
            simulator.addSlowProcess(wind = new mods.eln.misc.WindProcess());
            simulator.addSlowProcess(mods.eln.fluid.FluidWorld.reset());   // (before the devices' own: they see this tick's state)
            for (ElnContent c : CONTENT) c.serverAboutToStart();
        });
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, e -> {
            simulator.stop();
            mods.eln.fluid.FluidWorld.reset();
            mods.eln.node.voxel.PipePlacer.forgetAll();
            for (ElnContent c : CONTENT) c.serverStopped();
            serverEventListener.clear();
            nodeManager.clear();
            ghostManager.clear();
        });
        // 1.12 ran the simulator in ServerTickEvent phase START
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class, e -> simulator.tick());
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre.class, mods.eln.node.voxel.KitItem::onPickup);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.class, mods.eln.node.voxel.ChiselItem::onLeftClick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post.class, mods.eln.node.voxel.KitPreview::tick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent.class,
            e -> mods.eln.node.voxel.KitPreview.forget(e.getEntity().getUUID()));
        // pipe routes being laid: forgotten on logout and death
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent.class,
            e -> mods.eln.node.voxel.PipePlacer.forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.living.LivingDeathEvent.class,
            e -> mods.eln.node.voxel.PipePlacer.forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> mods.eln.node.NodeOrphans.tick(e.getServer()));
        // X-ray scanner: ore blocks per material come from tags
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.TagsUpdatedEvent.class, e -> mods.eln.item.xray.XRayFilter.invalidate());
        serverEventListener = new mods.eln.server.ServerEventListener();
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent.class,
            mods.eln.item.electricalitem.ElectricalArmor::onIncomingDamage);
        obj.loadAllElnModels();
        mods.eln.registry.ElnDeviceRegistry.registerBatch1();
        for (ElnContent c : CONTENT) c.preInit();
        mods.eln.registry.ElnRegistry.register(modBus);
        mods.eln.oil.Oil.register(modBus);
        mods.eln.sound.ElnSounds.register(modBus);
        mods.eln.datagen.ElnDataGen.register(modBus);
        mods.eln.gametest.LabCommands.register(); // /elnlab: render-port scenes (op only)
        mods.eln.node.voxel.CableConverter.register(); // six-node cables to voxel cables as chunks load (config)
        mods.eln.network.ServerPacketHandler.register();
        mods.eln.node.NodeServer nodeServer = new mods.eln.node.NodeServer();
        // fluid devices (fuel generator, turbines...): the element's tank as the block's fluid capability (1.12:
        // TransparentNodeEntityWithFluid); server side only, elements live there
        // pipe adapters (voxel nodes): the fluid capability on the adapter's face, a view of the pipe run behind it
        modBus.addListener(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent.class, e ->
            e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                mods.eln.registry.ElnRegistry.VOXEL_NODE_ENTITY.get(), (be, side) -> {
                    if (side == null || be.getLevel() == null || be.getLevel().isClientSide()) return null;
                    if (!(be.getNode() instanceof mods.eln.node.voxel.VoxelNode n)) return null;
                    var a = n.adapterOn(side.get3DDataValue());
                    return a == null ? null : a.tanks;
                }));
        modBus.addListener(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent.class, e ->
            e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                mods.eln.registry.ElnRegistry.TRANSPARENT_NODE_ENTITY.get(), (be, side) -> {
                    if (be.getLevel() == null || be.getLevel().isClientSide()) return null;
                    mods.eln.node.NodeBase node = be.getNode();
                    if (!(node instanceof mods.eln.node.transparent.TransparentNode tn)) return null;
                    if (tn.getFluidHandler() instanceof mods.eln.fluid.ElementFluidHandler h) return h;
                    // 26.x oil: devices with more than one tank (the boiler's water in, steam out)
                    @SuppressWarnings("unchecked")
                    var multi = tn.getFluidHandler() instanceof net.neoforged.neoforge.transfer.ResourceHandler<?> r
                        ? (net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource>) r : null;
                    return multi;
                }));
        // machines' items to hoppers and pipes (never worked in any EA version): the element's own sided-inventory
        // rules; null on faces without slots, for the null side and while burnt (TransparentNodeElement.getItemHandler)
        modBus.addListener(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent.class, e ->
            e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
                mods.eln.registry.ElnRegistry.TRANSPARENT_NODE_ENTITY.get(), (be, side) -> {
                    if (side == null || be.getLevel() == null || be.getLevel().isClientSide()) return null;
                    return be.getNode() instanceof mods.eln.node.transparent.TransparentNode tn ? tn.getItemHandler(side) : null;
                }));
        modBus.addListener(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent.class, e ->
            e.put(mods.eln.registry.ElnRegistry.REPLICATOR.get(), mods.eln.entity.ReplicatorEntity.createAttributes().build()));
        // FE on EA's energy items (batteries, tools, lamps)
        modBus.addListener(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent.class, mods.eln.item.electricalitem.ItemEnergyCapability::register);
        // FE out of a device's face (the exporters' back face; 1.12: CapabilityEnergy.ENERGY on the entity): the
        // element's handler, null on the client, for the null side and while burnt (TransparentNode.getEnergyHandler)
        modBus.addListener(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent.class, e ->
            e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
                mods.eln.registry.ElnRegistry.TRANSPARENT_NODE_ENTITY.get(), (be, side) -> {
                    if (side == null || be.getLevel() == null || be.getLevel().isClientSide()) return null;
                    return be.getNode() instanceof mods.eln.node.transparent.TransparentNode tn ? tn.getEnergyHandler(side) : null;
                }));
        modBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class, e -> {
            sixNodeBlock = mods.eln.registry.ElnRegistry.SIX_NODE_BLOCK.get();
            ghostBlock = mods.eln.registry.ElnRegistry.GHOST_BLOCK.get();
            mods.eln.registry.ElnDeviceRegistry.lightBlock = mods.eln.registry.ElnRegistry.LIGHT_BLOCK.get();
        });
        mods.eln.network.ElnNet.register(modBus);
        mods.eln.gametest.ElnGameTests.register(modBus);
        LOG.info("Electrical Age (26.x port, P0) loaded");
    }
}
