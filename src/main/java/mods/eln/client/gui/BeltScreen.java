package mods.eln.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import mods.eln.item.belt.BeltMenu;

/** The battery belt's pockets (BeltMenu): EA's panel, four slots, the inventory. */
public class BeltScreen extends AbstractContainerScreen<BeltMenu> {
    public BeltScreen(BeltMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 133);
        inventoryLabelY = BeltMenu.INV_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, ElnScreen.PANEL, leftPos, topPos, imageWidth, imageHeight);
        for (var s : menu.slots) g.blitSprite(RenderPipelines.GUI_TEXTURED, ElnScreen.SLOT, leftPos + s.x - 1, topPos + s.y - 1, 18, 18);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(font, title, titleLabelX, titleLabelY, ElnScreen.LABEL, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ElnScreen.LABEL, false);
    }
}
