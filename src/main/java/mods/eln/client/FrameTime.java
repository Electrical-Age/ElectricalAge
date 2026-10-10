package mods.eln.client;

import mods.eln.misc.UtilsClient;
import mods.eln.node.NodeBlockEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.ArrayList;

/** Once per rendered frame: every client node block entity's clientRefresh(deltaT), deltaT capped at 0.1 s (1.12). */
public final class FrameTime {
    private FrameTime() {
    }

    static float deltaT = 0.02f;
    static long oldNanoTime;
    static boolean boot = true;

    public static float get() {
        return UtilsClient.isGameInPause() ? 0f : Math.min(0.1f, deltaT);
    }

    public static void onFrame(RenderFrameEvent.Pre event) {
        mods.eln.gl.GlFrame.beginRenderFrame();
        long now = System.nanoTime();
        if (boot) boot = false;
        else deltaT = (now - oldNanoTime) * 1e-9f;
        oldNanoTime = now;
        if (UtilsClient.isGameInPause() || Minecraft.getInstance().level == null) return;
        float dt = get();
        for (NodeBlockEntity e : new ArrayList<>(NodeBlockEntity.clientList)) {
            if (e.getLevel() != Minecraft.getInstance().level) {
                NodeBlockEntity.clientList.remove(e);
                continue;
            }
            e.clientRefresh(dt);
        }
    }
}
