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

    /** config gameplay.explosion (1.12 meaning: the remaining real explosions; see Failure). */
    public static boolean explosionEnable = true;

    /** Registered blocks the node code compares against; set by the registry (P1 step 2+). */
    public static net.minecraft.world.level.block.Block sixNodeBlock, ghostBlock;
    public static mods.eln.ghost.GhostManager ghostManager;
    public static mods.eln.node.NodeManager nodeManager;

    public Eln(IEventBus modBus) {
        simulator = new Simulator(0.05, 1 / electricalFrequency, electricalInterSystemOverSampling, 1 / thermalFrequency);
        NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, e -> simulator.init());
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, e -> simulator.stop());
        // 1.12 ran the simulator in ServerTickEvent phase START
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class, e -> simulator.tick());
        mods.eln.network.ElnNet.register(modBus);
        mods.eln.gametest.ElnGameTests.register(modBus);
        LOG.info("Electrical Age (26.x port, P0) loaded");
    }
}
