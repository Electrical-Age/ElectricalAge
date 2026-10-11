package mods.eln.client.gui;

import mods.eln.item.xray.XRayRayMarch;
import mods.eln.item.xray.XRayScannerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** The X-ray scanner's filters: 4 slots framed in their picture colours (red, green, blue, yellow). */
public class XRayScannerScreen extends AbstractContainerScreen<XRayScannerMenu> {
    public XRayScannerScreen(XRayScannerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = XRayScannerMenu.INV_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, ElnScreen.PANEL, leftPos, topPos, imageWidth, imageHeight);
        for (int i = 0; i < XRayScannerMenu.FILTERS; i++) {
            int x = leftPos + XRayScannerMenu.SLOT_X0 + i * XRayScannerMenu.SLOT_DX, y = topPos + XRayScannerMenu.SLOT_Y;
            g.fill(x - 3, y - 3, x + 19, y + 19, XRayRayMarch.ARGB[i]);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, ElnScreen.SLOT, x - 1, y - 1, 18, 18);
        }
        for (var s : menu.slots)
            if (s.index >= XRayScannerMenu.FILTERS)
                g.blitSprite(RenderPipelines.GUI_TEXTURED, ElnScreen.SLOT, leftPos + s.x - 1, topPos + s.y - 1, 18, 18);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(font, title, titleLabelX, titleLabelY, ElnScreen.LABEL, false);
        g.text(font, tr("Ores to show: an ore block or raw chunk"), 8, 18, ElnScreen.LABEL, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ElnScreen.LABEL, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (hoveredSlot != null && hoveredSlot.index < XRayScannerMenu.FILTERS && !hoveredSlot.hasItem() && menu.getCarried().isEmpty())
            g.setTooltipForNextFrame(font, Component.literal(tr("Shows this ore in the picture")), mouseX, mouseY);
    }
}
