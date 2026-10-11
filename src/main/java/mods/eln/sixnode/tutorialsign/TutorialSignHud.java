package mods.eln.sixnode.tutorialsign;

import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;

/**
 * R5, client: the nearest tutorial sign within one block shows its text at the top left of the screen, half size, and
 * lights up (1.12 TutorialSignOverlay). A GUI layer, registered by ElnClient.
 */
public final class TutorialSignHud {
    private TutorialSignHud() {
    }

    private static TutorialSignRender lit;

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (lit != null) {
            lit.lightInterpol.setTarget(0);
            lit = null;
        }
        BlockPos p = mc.player.blockPosition();
        TutorialSignRender best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
            if (!(mc.level.getBlockEntity(q) instanceof SixNodeEntity e)) continue;
            for (SixNodeElementRender r : e.elementRenderList) {
                if (r instanceof TutorialSignRender t) {
                    double d = mc.player.position().distanceToSqr(q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5);
                    if (d < bestDistance) {
                        bestDistance = d;
                        best = t;
                    }
                    break;
                }
            }
        }
        if (best == null || best.texts == null) return;
        lit = best;
        best.lightInterpol.setTarget(1f);
        g.pose().pushMatrix();
        g.pose().scale(0.5f, 0.5f);
        int y = 10;
        for (String line : best.texts) {
            g.text(mc.font, line, 10, y, 0xFFFFFFFF, true);
            y += 10;
        }
        g.pose().popMatrix();
    }
}
