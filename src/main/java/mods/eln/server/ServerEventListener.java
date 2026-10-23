package mods.eln.server;

import mods.eln.Eln;
import mods.eln.ElnContent;
import mods.eln.misc.Coordonate;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.LinkedList;

/** 1.12 ServerEventListener: the lightning bolts of the last tick (wireless transmitters) and the content tick hook. */
public class ServerEventListener {

    private LinkedList<LightningBolt> lightningListNext = new LinkedList<>();
    private LinkedList<LightningBolt> lightningList = new LinkedList<>();

    public ServerEventListener() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> tick());
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, e -> {
            if (!e.getLevel().isClientSide() && e.getEntity() instanceof LightningBolt l) lightningListNext.add(l);
        });
    }

    void tick() {
        lightningList = lightningListNext;
        lightningListNext = new LinkedList<>();

        for (ElnContent c : Eln.CONTENT) c.serverTick(); // TreeCapitation.INSTANCE.process(0.05)
    }

    public void clear() {
        lightningList.clear();
        lightningListNext.clear();
    }

    public double getLightningClosestTo(Coordonate c) {
        double best = 10000000;
        for (LightningBolt l : lightningList) {
            if (c.world() != l.level()) continue;
            double d = Math.sqrt(l.distanceToSqr(c.x, c.y, c.z));
            if (d < best) best = d;
        }
        return best;
    }
}
