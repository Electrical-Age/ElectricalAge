package mods.eln.gui;

import java.util.List;

/**
 * A device GUI's container part (P4): its slots (empty for a GUI without a container: 1.12 opened those as a plain
 * screen) and, for 1.12 INodeContainers, how often the node publishes to the player while it is open (-1: never).
 * Made from the descriptor, so the server and the client build the same menu.
 */
public record MenuSpec(List<SlotSpec> slots, boolean playerInventory, int refreshRateDivider, int inventoryY) {
    /** Vanilla's 176x166 container: the player inventory at (8, 84), the hotbar 58 below it. */
    public static final int DEFAULT_INVENTORY_Y = 84;

    /** A GUI without a container (1.12 packetOpenLocalGui): no slots, no player inventory. */
    public static final MenuSpec SCREEN_ONLY = new MenuSpec(List.of(), false, -1, 0);

    public static MenuSpec of(SlotSpec... slots) {
        return new MenuSpec(List.of(slots), true, -1, DEFAULT_INVENTORY_Y);
    }

    /** A 1.12 INodeContainer: the node publishes every (1 + divider) server ticks while open. */
    public MenuSpec publishing(int divider) {
        return new MenuSpec(slots, playerInventory, divider, inventoryY);
    }

    /** The player inventory's top row at this y (a taller or shorter device area than vanilla's). */
    public MenuSpec inventoryAt(int y) {
        return new MenuSpec(slots, playerInventory, refreshRateDivider, y);
    }

    /** Inventory size the slots need (the client's stand-in inventory). */
    public int inventorySize() {
        int n = 0;
        for (SlotSpec s : slots) n = Math.max(n, s.index() + 1);
        return n;
    }
}
