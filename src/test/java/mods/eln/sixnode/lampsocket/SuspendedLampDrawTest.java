package mods.eln.sixnode.lampsocket;

import mods.eln.gl.GlFrame;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The suspended lamp draws its head once. 1.12 drew a lit head twice in the same place, shaded and then at full light,
 * and the second draw covered the first. Here the two draws are different render types, which the game draws in an
 * order that differs from launch to launch, so the lit head showed shaded on some launches.
 */
class SuspendedLampDrawTest {
    static Map<GlFrame.Key, GlFrame.Batch> draw(LampSocketSuspendedObjRender render, int light) {
        GlFrame f = GlFrame.open(new Matrix4f(), null);
        try {
            render.draw(LRDU.Up, 0, (byte) light, 3, -2, 10);
        } finally {
            GlFrame.close();
        }
        return f.batches();
    }

    static int vertices(Map<GlFrame.Key, GlFrame.Batch> batches, net.minecraft.resources.Identifier texture, boolean fullBright) {
        int n = 0;
        for (var e : batches.entrySet()) {
            GlFrame.Key k = e.getKey();
            if (texture.equals(k.texture()) && k.fullBright() == fullBright) {
                assertEquals(!fullBright, k.lit(), "full light means unshaded");
                assertEquals(GlFrame.Blend.NONE, k.blend());
                n += e.getValue().vertices();
            }
        }
        return n;
    }

    @Test
    void theHeadIsDrawnOnceAtFullLightWhenLitAndShadedWhenOff() {
        Obj3D obj = new Obj3D();
        assertTrue(obj.loadFile("RobustLampSuspended/RobustLampSuspended.obj"));
        LampSocketSuspendedObjRender render = new LampSocketSuspendedObjRender(obj, true, 3);
        GlFrame f = GlFrame.open(new Matrix4f(), null);
        obj.getPart("socket").drawNoBind();
        GlFrame.close();
        int head = 0;
        for (GlFrame.Batch b : f.batches().values()) head += b.vertices();
        assertTrue(head > 0);

        var lit = draw(render, 15);
        assertEquals(head, vertices(lit, render.tOn, true), "the lit head, at full light");
        int rest = vertices(lit, render.tOn, false);
        assertTrue(rest > 0, "base and chain stay shaded");
        assertEquals(head + rest, lit.values().stream().mapToInt(GlFrame.Batch::vertices).sum(), "nothing else is drawn from 10 blocks away");

        var off = draw(render, 0);
        assertEquals(head, vertices(off, render.tOff, false), "the unlit head, shaded");
        assertEquals(rest, vertices(off, render.tOn, false));
        assertEquals(head + rest, off.values().stream().mapToInt(GlFrame.Batch::vertices).sum());
    }
}
