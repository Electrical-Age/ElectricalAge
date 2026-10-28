package mods.eln.bike;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * The bike's trunk (Baughn 2026-10-28): the battery slot, three slots for anything, over the player's inventory; and,
 * for a container item in one of the three (anything with NeoForge's item capability: shulker boxes, bundles), a panel
 * of its insides beside the trunk, opened by that slot's tab (menu buttons 1-3). The panel's slots edit the item
 * itself through its handler, found again from the trunk slot on every access (never cached), so it goes inert the
 * moment the item leaves the slot, and two players at one trunk see one item. Menu data: the battery's charge (per
 * mille), the open tab (0 none), and the open item's slot count.
 */
public class BikeTrunkMenu extends AbstractContainerMenu {
    public static final int BATTERY_X = 26, ITEMS_X = 80, SLOT_Y = 20, INV_Y = 51;
    /** The panel: at most this many of an item's slots (a shulker box's 27), 9 to a row, right of the trunk. */
    public static final int PANEL = 27, PANEL_X = 188, PANEL_Y = 36;
    /** Slot index ranges: the trunk, the player's inventory, the panel. */
    public static final int INV_START = ElectricBike.TRUNK_SLOTS, INV_END = INV_START + 36, PANEL_START = INV_END, PANEL_END = PANEL_START + PANEL;
    final Container trunk;
    final ContainerData data;
    /** Server: the bike; client: null. */
    final ElectricBike bike;
    final Player player;
    /** Server: the open tab, 1-3 (the trunk slot), 0 none. */
    int open;

    /** Client side (MenuType factory). */
    public BikeTrunkMenu(int id, Inventory inv) {
        this(id, inv, null, new SimpleContainer(ElectricBike.TRUNK_SLOTS), new SimpleContainerData(4));
    }

    public BikeTrunkMenu(int id, Inventory inv, ElectricBike bike) {
        this(id, inv, bike, bike.trunk, null);
    }

    BikeTrunkMenu(int id, Inventory inv, ElectricBike bike, Container trunk, @Nullable ContainerData clientData) {
        super(mods.eln.registry.ElnRegistry.BIKE_TRUNK_MENU.get(), id);
        this.bike = bike;
        this.trunk = trunk;
        this.player = inv.player;
        this.data = clientData != null ? clientData : new ContainerData() {
            public int get(int i) {
                return switch (i) {
                    case 0 -> Math.round(bike.charge() * 1000);
                    case 1 -> handler() == null ? 0 : open;
                    case 2 -> {
                        var h = handler();
                        yield h == null ? 0 : Math.min(PANEL, h.size());
                    }
                    default -> {   // which trunk slots get a tab: the server's handlers, as the contents may live server-side
                        int mask = 0;
                        for (int k = 1; k < ElectricBike.TRUNK_SLOTS; k++) if (handler(k) != null) mask |= 1 << k;
                        yield mask;
                    }
                };
            }

            public void set(int i, int v) {
            }

            public int getCount() {
                return 4;
            }
        };
        addSlot(new Slot(trunk, ElectricBike.BATTERY_SLOT, BATTERY_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return ElectricBike.accepts(s);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int k = 1; k < ElectricBike.TRUNK_SLOTS; k++) addSlot(new Slot(trunk, k, ITEMS_X + (k - 1) * 18, SLOT_Y));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, 9 + r * 9 + c, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58));
        // the panel: on the server the item's own slots; on the client a plain mirror the server fills
        SimpleContainer mirror = new SimpleContainer(PANEL);
        for (int i = 0; i < PANEL; i++) {
            int x = PANEL_X + (i % 9) * 18, y = PANEL_Y + (i / 9) * 18;
            final int idx = i;
            addSlot(bike != null ? new InsideSlot(this, i, x, y) : new Slot(mirror, i, x, y) {
                @Override
                public boolean isActive() {
                    return data.get(1) != 0 && idx < data.get(2);
                }
            });
        }
        addDataSlots(data);
    }

    /** The battery's charge, 0..1, as the server last sent it. */
    public float charge() {
        return data.get(0) / 1000f;
    }

    /** The open tab (trunk slot 1-3, 0 none) and how many of the panel's slots it uses, as the server last sent them. */
    public int openTab() {
        return data.get(1);
    }

    public int panelSize() {
        return data.get(2);
    }

    /** Whether trunk slot k gets a tab: the server found a handler with slots for its item (client: from menu data). */
    public boolean hasTab(int k) {
        return (data.get(3) & 1 << k) != 0;
    }

    /** Whether an item shows its insides here (server): it has NeoForge's item capability with at least one slot. */
    public static boolean opens(ItemStack s) {
        if (s.getCount() != 1) return false;
        var h = ItemAccess.forStack(s.copy()).getCapability(Capabilities.Item.ITEM);
        return h != null && h.size() > 0;
    }

    /** Server: the open item's handler, through the trunk slot (its edits replace the slot's stack); null if none. */
    @Nullable ResourceHandler<ItemResource> handler() {
        return handler(open);
    }

    /** Server: trunk slot k's item's handler, if it has slots; null if none (a bag with no storage yet has none). */
    @Nullable ResourceHandler<ItemResource> handler(int k) {
        if (k < 1 || k >= ElectricBike.TRUNK_SLOTS || trunk.getItem(k).getCount() != 1) return null;   // (one item, not a stack)
        var h = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(trunk), k).getCapability(Capabilities.Item.ITEM);
        return h != null && h.size() > 0 ? h : null;
    }

    /** Server, each tick: bags whose storage is made on first opening (Sophisticated Backpacks) get it now, so they open. */
    @Override
    public void broadcastChanges() {
        if (bike != null) for (int k = 1; k < ElectricBike.TRUNK_SLOTS; k++) BagStorage.prepare(trunk, k);
        super.broadcastChanges();
    }

    /** A tab clicked (1-3): open that slot's item, or close it if it's open. */
    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id < 1 || id >= ElectricBike.TRUNK_SLOTS) return false;
        open = open == id ? 0 : id;
        if (bike != null && handler() == null) open = 0;
        return true;
    }

    /** What the panel couldn't take after all (a race, a handler that changed its mind): back to the player, never lost. */
    void giveBack(ItemStack s) {
        if (!s.isEmpty()) player.getInventory().placeItemBackInInventory(s, net.minecraft.util.Prediction.SERVER_ONLY);
    }

    @Override
    public boolean stillValid(Player p) {
        return bike == null || bike.isAlive() && p.distanceToSqr(bike) < 64;
    }

    /**
     * Shift-click: out of the trunk or the panel into the inventory; from the inventory into the open panel if it takes
     * it, else a battery to the battery slot and anything else to the trunk.
     */
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        if (slot instanceof InsideSlot in) {
            // take it out of the item first, then place it; whatever doesn't fit goes back in (or to the player)
            ItemStack t = in.remove(in.getItem().getCount());
            moveItemStackTo(t, INV_START, INV_END, true);
            if (!t.isEmpty()) giveBack(in.insert(t));
            return ItemStack.EMPTY;
        }
        if (index >= PANEL_START) return ItemStack.EMPTY;   // (the client's mirror: the server decides)
        ItemStack s = slot.getItem(), was = s.copy();
        int n = ElectricBike.TRUNK_SLOTS;
        if (index < n) {
            if (!moveItemStackTo(s, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else {
            var h = bike != null ? handler() : null;
            if (h != null && InsideSlot.fits(s)) {
                int moved;
                try (var tx = Transaction.openRoot()) {
                    moved = h.insert(ItemResource.of(s), s.getCount(), tx);
                    tx.commit();
                }
                if (moved > 0) {
                    s.shrink(moved);
                    slot.setChanged();
                    return ItemStack.EMPTY;
                }
            }
            if (ElectricBike.accepts(s) && !slots.get(0).hasItem()) {
                if (!moveItemStackTo(s, 0, 1, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(s, 1, n, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return was;
    }
}
