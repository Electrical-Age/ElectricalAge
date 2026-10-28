package mods.eln.item.belt;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The battery belt's pockets (BatteryBeltItem): four slots that take only what the belt accepts, over the player's
 * inventory. The server writes the pockets back into the belt it was opened from whenever they change; that belt's
 * hotbar slot is locked while it is open (so it can't be moved or dropped from under the menu).
 */
public class BeltMenu extends AbstractContainerMenu {
    public static final int POCKET_X = 53, POCKET_Y = 20, INV_Y = 51;
    final SimpleContainer pockets;
    /** Server: the belt, and its slot in the player's inventory (-1 offhand); client: null. */
    final ItemStack belt;
    final int beltSlot;

    /** Client side (MenuType factory). */
    public BeltMenu(int id, Inventory inv) {
        this(id, inv, null, -2);
    }

    BeltMenu(int id, Inventory inv, ItemStack belt, int beltSlot) {
        super(mods.eln.registry.ElnRegistry.BELT_MENU.get(), id);
        this.belt = belt;
        this.beltSlot = beltSlot;
        pockets = new SimpleContainer(BatteryBeltItem.SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                save();
            }
        };
        if (belt != null) {
            var c = BatteryBeltItem.contents(belt);
            for (int k = 0; k < BatteryBeltItem.SLOTS; k++) pockets.setItem(k, c.get(k));
        }
        for (int k = 0; k < BatteryBeltItem.SLOTS; k++)
            addSlot(new Slot(pockets, k, POCKET_X + k * 18, POCKET_Y) {
                @Override
                public boolean mayPlace(ItemStack s) {
                    return BatteryBeltItem.accepts(s);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, 9 + r * 9 + c, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) {
            final int idx = c;
            addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58) {
                @Override
                public boolean mayPickup(Player p) {
                    return idx != beltSlot;
                }
            });
        }
    }

    void save() {
        if (belt == null) return;
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int k = 0; k < BatteryBeltItem.SLOTS; k++) items.add(pockets.getItem(k).copy());
        BatteryBeltItem.setContents(belt, items);
    }

    public static void open(ServerPlayer p, InteractionHand hand) {
        ItemStack belt = p.getItemInHand(hand);
        int slot = hand == InteractionHand.MAIN_HAND ? p.getInventory().getSelectedSlot() : -1;
        p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new BeltMenu(id, inv, belt, slot),
            Component.translatable("item.eln.battery_belt")));
    }

    @Override
    public boolean stillValid(Player p) {
        if (belt == null) return true;
        ItemStack held = beltSlot >= 0 ? p.getInventory().getItem(beltSlot) : p.getOffhandItem();
        return held == belt && !belt.isEmpty();
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        save();
    }

    /** Shift-click: a battery or capacitor into a free pocket; out of a pocket into the inventory. */
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem(), was = s.copy();
        if (index < BatteryBeltItem.SLOTS) {
            if (!moveItemStackTo(s, BatteryBeltItem.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!BatteryBeltItem.accepts(s) || !moveItemStackTo(s, 0, BatteryBeltItem.SLOTS, false)) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return was;
    }
}
