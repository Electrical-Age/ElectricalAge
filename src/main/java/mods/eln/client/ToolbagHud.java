package mods.eln.client;

import mods.eln.node.voxel.ToolbagItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * With the Toolbag in the main hand: what it lays and how to use it, terse, on a line near the bottom of the screen
 * (above the route note and the action bar; Baughn 2026-11-18).
 */
public final class ToolbagHud {
    private ToolbagHud() {
    }

    static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || !(mc.player.getMainHandItem().getItem() instanceof ToolbagItem)) return;
        var k = ToolbagItem.selected(mc.player.getMainHandItem());
        String text = (k == null ? "" : k.label() + " | ") + "R-click cable: lay its kind | elsewhere or sneak: choose | air: open | L-click: cut";
        int w = mc.font.width(text);
        g.text(mc.font, text, (g.guiWidth() - w) / 2, g.guiHeight() - 68 - 4 - 24, 0xC0D0D0D0, true);
    }
}
