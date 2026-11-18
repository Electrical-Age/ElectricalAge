package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Toolbag's inside (ToolbagItem): fifteen slots for cable and pipe kits (three rows of five) and one for shears,
 * over the player's inventory. As the battery belt's (BeltMenu): written back into the bag whenever it changes, the
 * bag's hotbar slot locked while it is open.
 */
public class ToolbagMenu extends AbstractContainerMenu {
    public static final int KIT_X = 62, KIT_Y = 18, SHEARS_X = 26, SHEARS_Y = 36, INV_Y = 86;
    final SimpleContainer bagSlots;
    /** Server: the bag and its hotbar slot; client: null, -2. */
    final ItemStack bag;
    final int bagSlot;

    /** Client side (MenuType factory). */
    public ToolbagMenu(int id, Inventory inv) {
        this(id, inv, null, -2);
    }

    ToolbagMenu(int id, Inventory inv, ItemStack bag, int bagSlot) {
        super(mods.eln.registry.ElnRegistry.TOOLBAG_MENU.get(), id);
        this.bag = bag;
        this.bagSlot = bagSlot;
        bagSlots = new SimpleContainer(ToolbagItem.SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                save();
            }
        };
        if (bag != null) {
            var c = ToolbagItem.contents(bag);
            for (int k = 0; k < ToolbagItem.SLOTS; k++) bagSlots.setItem(k, c.get(k));
        }
        for (int k = 0; k < ToolbagItem.KITS; k++) addSlot(bagSlot(k, KIT_X + k % 5 * 18, KIT_Y + k / 5 * 18));
        addSlot(bagSlot(ToolbagItem.SHEARS_SLOT, SHEARS_X, SHEARS_Y));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, 9 + r * 9 + c, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) {
            final int idx = c;
            addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58) {
                @Override
                public boolean mayPickup(Player p) {
                    return idx != bagSlot;
                }
            });
        }
    }

    Slot bagSlot(int k, int x, int y) {
        return new Slot(bagSlots, k, x, y) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return ToolbagItem.accepts(k, s);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
    }

    void save() {
        if (bag == null) return;
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int k = 0; k < ToolbagItem.SLOTS; k++) items.add(bagSlots.getItem(k).copy());
        ToolbagItem.setContents(bag, items);
    }

    public static void open(ServerPlayer p) {
        ItemStack bag = p.getMainHandItem();
        int slot = p.getInventory().getSelectedSlot();
        p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new ToolbagMenu(id, inv, bag, slot),
            Component.translatable("item.eln.toolbag")));
    }

    /** GameTests: a menu over this bag (as opened from hotbar slot `slot`). */
    public static ToolbagMenu forTests(Inventory inv, ItemStack bag, int slot) {
        return new ToolbagMenu(0, inv, bag, slot);
    }

    @Override
    public boolean stillValid(Player p) {
        if (bag == null) return true;
        return p.getInventory().getItem(bagSlot) == bag && !bag.isEmpty();
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        save();
    }

    /** Shift-click: a kit into a free kit slot, shears into theirs; out of the bag into the inventory. */
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem(), was = s.copy();
        if (index < ToolbagItem.SLOTS) {
            if (!moveItemStackTo(s, ToolbagItem.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (ToolbagItem.accepts(ToolbagItem.SHEARS_SLOT, s)) {
            if (!moveItemStackTo(s, ToolbagItem.KITS, ToolbagItem.SLOTS, false)) return ItemStack.EMPTY;
        } else {
            if (!ToolbagItem.accepts(0, s) || !moveItemStackTo(s, 0, ToolbagItem.KITS, false)) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return was;
    }
}
