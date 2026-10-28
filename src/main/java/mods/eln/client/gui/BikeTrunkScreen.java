package mods.eln.client.gui;

import mods.eln.bike.BikeTrunkMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The bike's trunk (BikeTrunkMenu): EA's panel, the battery slot with its charge beside it, three slots, the inventory. */
public class BikeTrunkScreen extends AbstractContainerScreen<BikeTrunkMenu> {
    public BikeTrunkScreen(BikeTrunkMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 133);
        inventoryLabelY = BikeTrunkMenu.INV_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, ElnScreen.PANEL, leftPos, topPos, imageWidth, imageHeight);
        for (var s : menu.slots) g.blitSprite(RenderPipelines.GUI_TEXTURED, ElnScreen.SLOT, leftPos + s.x - 1, topPos + s.y - 1, 18, 18);
        // the charge: a bar under the battery slot
        int x = leftPos + BikeTrunkMenu.BATTERY_X - 1, y = topPos + BikeTrunkMenu.SLOT_Y + 18;
        g.fill(x, y, x + 18, y + 3, 0xFF202020);
        g.fill(x + 1, y + 1, x + 1 + Math.round(16 * menu.charge()), y + 2, 0xFF40C040);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(font, title, titleLabelX, titleLabelY, ElnScreen.LABEL, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ElnScreen.LABEL, false);
        g.text(font, Component.literal(Math.round(menu.charge() * 100) + "%"), BikeTrunkMenu.BATTERY_X + 20, BikeTrunkMenu.SLOT_Y + 5, ElnScreen.LABEL, false);
    }
}
