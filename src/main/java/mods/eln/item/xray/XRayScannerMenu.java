package mods.eln.item.xray;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The X-ray scanner's filter slots (opened by sneak + use): 4 slots, one ore block or raw chunk each, stored in the
 * scanner stack in the player's inventory (DataComponents.CONTAINER, written back on every change). The scanner's own
 * slot can't be moved while the menu is open.
 */
public class XRayScannerMenu extends AbstractContainerMenu {
    public static final int OFFHAND = 40;
    public static final int FILTERS = 4;
    /** Panel layout (the screen uses the same numbers). */
    public static final int SLOT_Y = 34, SLOT_X0 = 44, SLOT_DX = 24, INV_Y = 84;

    final Inventory playerInventory;
    final int scannerSlot;
    final SimpleContainer filters = new SimpleContainer(FILTERS) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            save(this);
        }
    };
    private boolean loading;

    public XRayScannerMenu(int id, Inventory inv, int scannerSlot) {
        super(mods.eln.registry.ElnRegistry.XRAY_MENU.get(), id);
        this.playerInventory = inv;
        this.scannerSlot = scannerSlot;
        loading = true;
        List<ItemStack> f = XRayFilter.filters(scanner());
        for (int i = 0; i < FILTERS; i++) filters.setItem(i, f.get(i).copy());
        loading = false;
        for (int i = 0; i < FILTERS; i++)
            addSlot(new Slot(filters, i, SLOT_X0 + i * SLOT_DX, SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return XRayFilter.accepts(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(playerSlot(c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(playerSlot(c, 8 + c * 18, INV_Y + 58));
    }

    public static XRayScannerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new XRayScannerMenu(id, inv, buf.readInt());
    }

    Slot playerSlot(int index, int x, int y) {
        return new Slot(playerInventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return getContainerSlot() != scannerSlot;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return getContainerSlot() != scannerSlot;
            }
        };
    }

    ItemStack scanner() {
        return playerInventory.getItem(scannerSlot);
    }

    void save(Container c) {
        if (loading) return;
        ItemStack s = scanner();
        if (!(s.getItem() instanceof mods.eln.generic.ElnItem i && i.descriptor instanceof XRayScannerDescriptor)) return;
        List<ItemStack> f = new ArrayList<>();
        for (int k = 0; k < FILTERS; k++) f.add(filters.getItem(k).copy());
        XRayFilter.setFilters(s, f);
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack s = scanner();
        return s.getItem() instanceof mods.eln.generic.ElnItem i && i.descriptor instanceof XRayScannerDescriptor;
    }

    /** Shift-click: an ore into the first free filter slot, a filter back into the inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index < FILTERS) {
            if (!moveItemStackTo(stack, FILTERS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!XRayFilter.accepts(stack)) return ItemStack.EMPTY;
            boolean moved = false;
            for (int i = 0; i < FILTERS && !moved; i++)
                if (!slots.get(i).hasItem()) {
                    slots.get(i).set(stack.split(1));
                    moved = true;
                }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }
}
