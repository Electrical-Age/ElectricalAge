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
    public static final String MODID = "eln";
    public static final Logger LOG = LoggerFactory.getLogger("eln");

    // [simulation] defaults of the 1.12 config (electricalFrequency 20, electricalInterSystemOverSampling 50,
    // thermalFrequency 400); the 26.x config comes with P1.
    public static double electricalFrequency = 20, thermalFrequency = 400;
    public static int electricalInterSystemOverSampling = 50;

    public static Simulator simulator;

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

    /** Device descriptors by 1.12 id (1.12: Eln.sixNodeItem / transparentNodeItem). */
    public static final mods.eln.node.DescriptorRegistry<mods.eln.node.six.SixNodeDescriptor> sixNodeItem =
        new mods.eln.node.DescriptorRegistry<>(mods.eln.node.six.SixNodeDescriptor.class);
    public static final mods.eln.node.DescriptorRegistry<mods.eln.node.transparent.TransparentNodeDescriptor> transparentNodeItem =
        new mods.eln.node.DescriptorRegistry<>(mods.eln.node.transparent.TransparentNodeDescriptor.class);

    public Eln(IEventBus modBus) {
        simulator = new Simulator(0.05, 1 / electricalFrequency, electricalInterSystemOverSampling, 1 / thermalFrequency);
        nodeManager = new mods.eln.node.NodeManager("caca");
        ghostManager = new mods.eln.ghost.GhostManager("caca2");
        mods.eln.server.ElnWorldSaves.register();
        mods.eln.node.NodeManager.registerUuid("s", mods.eln.node.six.SixNode.class);
        mods.eln.node.NodeManager.registerUuid("t", mods.eln.node.transparent.TransparentNode.class);
        NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, e -> simulator.init());
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, e -> {
            simulator.stop();
            nodeManager.clear();
            ghostManager.clear();
        });
        // 1.12 ran the simulator in ServerTickEvent phase START
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class, e -> simulator.tick());
        mods.eln.network.ElnNet.register(modBus);
        mods.eln.gametest.ElnGameTests.register(modBus);
        LOG.info("Electrical Age (26.x port, P0) loaded");
    }
}
