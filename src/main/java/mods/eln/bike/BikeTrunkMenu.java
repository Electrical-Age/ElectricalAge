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

/**
 * The bike's trunk (Baughn 2026-10-28): the battery slot, three slots for anything, over the player's inventory. The
 * battery's charge (per mille) goes to the client as menu data, for the screen.
 */
public class BikeTrunkMenu extends AbstractContainerMenu {
    public static final int BATTERY_X = 26, ITEMS_X = 80, SLOT_Y = 20, INV_Y = 51;
    final Container trunk;
    final ContainerData data;
    /** Server: the bike; client: null. */
    final ElectricBike bike;

    /** Client side (MenuType factory). */
    public BikeTrunkMenu(int id, Inventory inv) {
        this(id, inv, null, new SimpleContainer(ElectricBike.TRUNK_SLOTS), new SimpleContainerData(1));
    }

    BikeTrunkMenu(int id, Inventory inv, ElectricBike bike) {
        this(id, inv, bike, bike.trunk, new ContainerData() {
            public int get(int i) {
                return Math.round(bike.charge() * 1000);
            }

            public void set(int i, int v) {
            }

            public int getCount() {
                return 1;
            }
        });
    }

    BikeTrunkMenu(int id, Inventory inv, ElectricBike bike, Container trunk, ContainerData data) {
        super(mods.eln.registry.ElnRegistry.BIKE_TRUNK_MENU.get(), id);
        this.bike = bike;
        this.trunk = trunk;
        this.data = data;
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
        addDataSlots(data);
    }

    /** The battery's charge, 0..1, as the server last sent it. */
    public float charge() {
        return data.get(0) / 1000f;
    }

    @Override
    public boolean stillValid(Player p) {
        return bike == null || bike.isAlive() && p.distanceToSqr(bike) < 64;
    }

    /** Shift-click: a battery to the battery slot, anything else to the trunk; out of the trunk into the inventory. */
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem(), was = s.copy();
        int n = ElectricBike.TRUNK_SLOTS;
        if (index < n) {
            if (!moveItemStackTo(s, n, slots.size(), true)) return ItemStack.EMPTY;
        } else if (ElectricBike.accepts(s) && !slots.get(0).hasItem()) {
            if (!moveItemStackTo(s, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(s, 1, n, false)) {
            return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return was;
    }
}
