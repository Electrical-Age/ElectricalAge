package mods.eln.robotics.client;

import mods.eln.robotics.Robotics;
import mods.eln.robotics.servo.SegmentPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Robotics' client setup (called from ElnClient): the car's renderer, its segment handler, the carry. */
public final class RoboticsClient {
    private RoboticsClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
            e -> e.registerEntityRenderer(Robotics.ELEVATOR_CAR.get(), ElevatorCarRenderer::new));
        SegmentPayload.clientHandler = CarCarry::onSegment;
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> CarCarry.tick());
    }
}
