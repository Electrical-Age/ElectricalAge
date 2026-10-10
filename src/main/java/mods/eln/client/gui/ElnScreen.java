package mods.eln.client.gui;

import mods.eln.gui.ElnNodeMenu;
import mods.eln.gui.SlotSpec;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The base of every EA screen (P4b), in the vanilla 26.x style: a container panel, the device's slots as vanilla slot
 * frames (big ones for 1.12's big skin), labels in the vanilla grey, the player inventory when the menu has one.
 * Subclasses add widgets in {@link #init()} and refresh them from the device's render in {@link #update()} (every
 * client tick). Without a subclass this is the generic screen: slots only.
 */
public class ElnScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<ElnNodeMenu> {
    public static final Identifier PANEL = Identifier.fromNamespaceAndPath("eln", "panel");
    public static final Identifier INSET = Identifier.fromNamespaceAndPath("eln", "inset");
    public static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
    public static final int LABEL = 0xFF404040;

    public ElnScreen(ElnNodeMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, 176, menu.spec.playerInventory() ? menu.spec.inventoryY() + 82 : 40);
    }

    public ElnScreen(ElnNodeMenu menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
        this.inventoryLabelY = menu.spec.inventoryY() - 11;
    }

    /** Refresh labels and values from the device (client tick). */
    protected void update() {
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        update();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) {
            if (s.x < -100 || s.y < -100) continue; // 1.12 hid some slots off-screen (thermal isolator)
            boolean big = s instanceof mods.eln.gui.ElnSlot es && SlotSpec.BIG.equals(es.spec.skin());
            if (big) g.blitSprite(RenderPipelines.GUI_TEXTURED, INSET, leftPos + s.x - 5, topPos + s.y - 5, 26, 26);
            else g.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, leftPos + s.x - 1, topPos + s.y - 1, 18, 18);
        }
        extractDevice(g, mouseX, mouseY, a);
    }

    /** Device graphics under the widgets (progress arrows, gauges' backgrounds). Screen coordinates. */
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(font, title, titleLabelX, titleLabelY, LABEL, false);
        if (menu.spec.playerInventory()) g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
    }

    /** An empty device slot says what goes in it (1.12's slot comments). */
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (hoveredSlot instanceof mods.eln.gui.ElnSlot es && !es.hasItem() && menu.getCarried().isEmpty()
            && es.spec.comment() != null && es.spec.comment().length > 0) {
            java.util.List<Component> lines = new java.util.ArrayList<>();
            for (String c : es.spec.comment()) lines.add(Component.literal(c));
            g.setTooltipForNextFrame(font, lines, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    /** Typing in a text box must not close the screen (the inventory key) or move hotbar items. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (getFocused() instanceof EditBox box && box.isFocused() && event.key() != com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE)
            return box.keyPressed(event) || box.canConsumeInput();
        return super.keyPressed(event);
    }
}
