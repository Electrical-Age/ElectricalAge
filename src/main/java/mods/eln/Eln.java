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

    public Eln(IEventBus modBus) {
        simulator = new Simulator(0.05, 1 / electricalFrequency, electricalInterSystemOverSampling, 1 / thermalFrequency);
        NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, e -> simulator.init());
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, e -> simulator.stop());
        // 1.12 ran the simulator in ServerTickEvent phase START
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class, e -> simulator.tick());
        mods.eln.gametest.ElnGameTests.register(modBus);
        LOG.info("Electrical Age (26.x port, P0) loaded");
    }
}
