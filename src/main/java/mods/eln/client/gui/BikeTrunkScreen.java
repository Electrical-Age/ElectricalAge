package mods.eln.client.gui;

import mods.eln.bike.BikeTrunkMenu;
import mods.eln.bike.ElectricBike;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The bike's trunk (BikeTrunkMenu): EA's panel, the battery slot with its charge beside it, three slots, the inventory.
 * Under each of the three that holds a container item (a shulker box, a bundle) a tab; clicked, that item's insides
 * open in a panel right of the trunk (clicked again, it closes).
 */
public class BikeTrunkScreen extends AbstractContainerScreen<BikeTrunkMenu> {
    static final int TAB_W = 16, TAB_H = 6, TAB_Y = BikeTrunkMenu.SLOT_Y + 18, PANEL_LEFT = BikeTrunkMenu.PANEL_X - 8,
        PANEL_TOP = BikeTrunkMenu.PANEL_Y - 14, PANEL_W = 9 * 18 + 16;

    public BikeTrunkScreen(BikeTrunkMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 133);
        inventoryLabelY = BikeTrunkMenu.INV_Y - 11;
    }

    /** Room for the panel: the trunk sits left of centre so trunk and panel together are centred. */
    @Override
    protected void init() {
        super.init();
        leftPos = Math.max(0, (width - (PANEL_LEFT + PANEL_W)) / 2);
    }

    int tabX(int k) {
        return leftPos + BikeTrunkMenu.ITEMS_X + (k - 1) * 18;
    }

    boolean hasTab(int k) {
        return BikeTrunkMenu.opens(menu.slots.get(k).getItem());
    }

    int panelRows() {
        return Math.max(1, (menu.panelSize() + 8) / 9);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, ElnScreen.PANEL, leftPos, topPos, imageWidth, imageHeight);
        boolean open = menu.openTab() != 0 && menu.panelSize() > 0;
        if (open)
            g.blitSprite(RenderPipelines.GUI_TEXTURED, ElnScreen.PANEL, leftPos + PANEL_LEFT, topPos + PANEL_TOP, PANEL_W, 14 + panelRows() * 18 + 8);
        for (var s : menu.slots)
            if (s.isActive()) g.blitSprite(RenderPipelines.GUI_TEXTURED, ElnScreen.SLOT, leftPos + s.x - 1, topPos + s.y - 1, 18, 18);
        // the charge: a bar under the battery slot
        int x = leftPos + BikeTrunkMenu.BATTERY_X - 1, y = topPos + BikeTrunkMenu.SLOT_Y + 18;
        g.fill(x, y, x + 18, y + 3, 0xFF202020);
        g.fill(x + 1, y + 1, x + 1 + Math.round(16 * menu.charge()), y + 2, 0xFF40C040);
        // the tabs
        for (int k = 1; k < ElectricBike.TRUNK_SLOTS; k++) {
            if (!hasTab(k)) continue;
            boolean sel = menu.openTab() == k;
            int tx = tabX(k), ty = topPos + TAB_Y;
            boolean hover = mouseX >= tx && mouseX < tx + TAB_W && mouseY >= ty && mouseY < ty + TAB_H;
            g.fill(tx, ty, tx + TAB_W, ty + TAB_H, sel ? 0xFFE0A040 : hover ? 0xFFB0B8C0 : 0xFF808890);
            // a small arrow pointing to the panel
            int ax = tx + TAB_W / 2 - 2, ay = ty + 1;
            g.fill(ax, ay, ax + 1, ay + 4, 0xFF202428);
            g.fill(ax + 1, ay + 1, ax + 2, ay + 3, 0xFF202428);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(font, title, titleLabelX, titleLabelY, ElnScreen.LABEL, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ElnScreen.LABEL, false);
        g.text(font, Component.literal(Math.round(menu.charge() * 100) + "%"), BikeTrunkMenu.BATTERY_X + 20, BikeTrunkMenu.SLOT_Y + 5, ElnScreen.LABEL, false);
        int k = menu.openTab();
        if (k != 0 && menu.panelSize() > 0)
            g.text(font, menu.slots.get(k).getItem().getHoverName(), PANEL_LEFT + 8, PANEL_TOP + 4, ElnScreen.LABEL, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (e.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)
            for (int k = 1; k < ElectricBike.TRUNK_SLOTS; k++) {
                int tx = tabX(k), ty = topPos + TAB_Y;
                if (hasTab(k) && e.x() >= tx && e.x() < tx + TAB_W && e.y() >= ty && e.y() < ty + TAB_H) {
                    if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, k);
                    return true;
                }
            }
        return super.mouseClicked(e, doubleClick);
    }

    /** A click on the open panel isn't a click outside (which would drop what you hold). */
    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        if (menu.openTab() != 0 && menu.panelSize() > 0 && mx >= xo + PANEL_LEFT && mx < xo + PANEL_LEFT + PANEL_W
            && my >= yo + PANEL_TOP && my < yo + PANEL_TOP + 14 + panelRows() * 18 + 8) return false;
        return super.hasClickedOutside(mx, my, xo, yo);
    }
}
