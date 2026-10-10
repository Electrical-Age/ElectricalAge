package mods.eln.gui;

import java.util.List;

/**
 * A device GUI's container part (P4): its slots (empty for a GUI without a container: 1.12 opened those as a plain
 * screen) and, for 1.12 INodeContainers, how often the node publishes to the player while it is open (-1: never).
 * Made from the descriptor, so the server and the client build the same menu.
 */
public record MenuSpec(List<SlotSpec> slots, boolean playerInventory, int refreshRateDivider, int inventoryX, int inventoryY,
                       int width, int height) {
    /** Vanilla's 176x166 container: the player inventory at (8, 84), the hotbar 58 below it. */
    public static final int DEFAULT_INVENTORY_Y = 84;

    /** A GUI without a container (1.12 packetOpenLocalGui): no slots, no player inventory. */
    public static final MenuSpec SCREEN_ONLY = new MenuSpec(List.of(), false, -1, 8, 0, 176, 40);

    public static MenuSpec of(SlotSpec... slots) {
        return new MenuSpec(List.of(slots), true, -1, 8, DEFAULT_INVENTORY_Y, 176, 166);
    }

    /** A 1.12 INodeContainer: the node publishes every (1 + divider) server ticks while open. */
    public MenuSpec publishing(int divider) {
        return new MenuSpec(slots, playerInventory, divider, inventoryX, inventoryY, width, height);
    }

    /** The player inventory's top row at this y (a taller or shorter device area than vanilla's). */
    public MenuSpec inventoryAt(int y) {
        return new MenuSpec(slots, playerInventory, refreshRateDivider, inventoryX, y, width, y + 82);
    }

    /** 1.12 GuiHelperContainer(screen, width, height, xInv, yInv): the panel and where the player inventory sits. */
    public MenuSpec panel(int width, int height, int invX, int invY) {
        return new MenuSpec(slots, playerInventory, refreshRateDivider, invX, invY, width, height);
    }

    /** Inventory size the slots need (the client's stand-in inventory). */
    public int inventorySize() {
        int n = 0;
        for (SlotSpec s : slots) n = Math.max(n, s.index() + 1);
        return n;
    }
}
