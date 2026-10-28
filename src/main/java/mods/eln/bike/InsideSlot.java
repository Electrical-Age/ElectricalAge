package mods.eln.bike;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Server: one slot of the open container item's insides in the trunk menu (BikeTrunkMenu). Every access finds the item's
 * handler again through the trunk slot, so nothing is cached that could outlive the item: taken out (or swapped) while
 * the panel is open, the slot reads empty and refuses everything. Changes go through the handler's insert and extract
 * in a transaction, so its own rules hold (a bundle's weight; and vanilla's: no shulker box inside another item); what it won't take goes
 * back to the player. Vanilla's in-place edits (read getItem, change the copy, setChanged) are written back only while
 * the trunk still holds the same stack object it was read from: another player's change in between makes them void.
 */
public class InsideSlot extends Slot {
    final BikeTrunkMenu menu;
    final int index;
    /** The copy getItem last handed out, and the trunk's stack it was read from (for setChanged). */
    @Nullable ItemStack lastReturned;
    @Nullable ItemStack lastSource;

    InsideSlot(BikeTrunkMenu menu, int index, int x, int y) {
        super(new SimpleContainer(0), index, x, y);
        this.menu = menu;
        this.index = index;
    }

    @Nullable ResourceHandler<ItemResource> handler() {
        var h = menu.handler();
        return h != null && index < h.size() ? h : null;
    }

    ItemStack source() {
        return menu.open >= 1 && menu.open < menu.trunk.getContainerSize() ? menu.trunk.getItem(menu.open) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack getItem() {
        var h = handler();
        ItemStack s = h == null ? ItemStack.EMPTY : h.getResource(index).toStack(h.getAmountAsInt(index));
        lastReturned = s;
        lastSource = source();
        return s;
    }

    @Override
    public boolean hasItem() {
        var h = handler();
        return h != null && !h.getResource(index).isEmpty() && h.getAmountAsInt(index) > 0;
    }

    @Override
    public boolean isActive() {
        return handler() != null;
    }

    /**
     * Whether s may go into a container item at all: not a shulker box (vanilla's rule, which the item handlers don't
     * check themselves).
     */
    static boolean fits(ItemStack s) {
        return s.canFitInsideContainerItems();
    }

    /** What the slot would hold of s at most if s replaced what's in it now (simulated), 0 if it takes none. */
    int capacityFor(ItemStack s) {
        var h = handler();
        if (h == null || s.isEmpty() || !fits(s)) return 0;
        ItemResource cur = h.getResource(index), want = ItemResource.of(s);
        try (var tx = Transaction.openRoot()) {
            int kept = 0;
            if (!cur.isEmpty()) {
                if (cur.equals(want)) kept = h.getAmountAsInt(index);
                else h.extract(index, cur, h.getAmountAsInt(index), tx);
            }
            int more = h.insert(index, want, s.getMaxStackSize() * 4, tx);
            if (more == 0 && kept == 0) more = h.insert(want, s.getMaxStackSize(), tx);   // (a bundle's free slot is its last)
            return Math.min(s.getMaxStackSize(), kept + more);
        }
    }

    @Override
    public boolean mayPlace(ItemStack s) {
        return capacityFor(s) > 0;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public int getMaxStackSize(ItemStack s) {
        return Math.max(1, capacityFor(s));
    }

    @Override
    public boolean mayPickup(Player p) {
        var h = handler();
        if (h == null) return false;
        ItemResource cur = h.getResource(index);
        if (cur.isEmpty()) return false;
        try (var tx = Transaction.openRoot()) {
            return h.extract(index, cur, 1, tx) == 1;
        }
    }

    /** Take up to amount out of the item; what came out. */
    @Override
    public ItemStack remove(int amount) {
        lastReturned = null;
        var h = handler();
        if (h == null || amount <= 0) return ItemStack.EMPTY;
        ItemResource cur = h.getResource(index);
        if (cur.isEmpty()) return ItemStack.EMPTY;
        int got;
        try (var tx = Transaction.openRoot()) {
            got = h.extract(index, cur, amount, tx);
            tx.commit();
        }
        return cur.toStack(got);
    }

    @Override
    public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player p) {
        if (!mayPickup(p)) return Optional.empty();
        ItemStack got = remove(Math.min(amount, maxAmount));
        return got.isEmpty() ? Optional.empty() : Optional.of(got);
    }

    /** Put s into the item (this slot first, then wherever the handler takes it); what it didn't take. */
    ItemStack insert(ItemStack s) {
        lastReturned = null;
        var h = handler();
        if (h == null || s.isEmpty() || !fits(s)) return s;
        ItemResource r = ItemResource.of(s);
        int in;
        try (var tx = Transaction.openRoot()) {
            in = h.insert(index, r, s.getCount(), tx);
            if (in < s.getCount()) in += h.insert(r, s.getCount() - in, tx);
            tx.commit();
        }
        return s.copyWithCount(s.getCount() - in);
    }

    @Override
    public ItemStack safeInsert(ItemStack s, int amount) {
        if (s.isEmpty() || !mayPlace(s)) return s;
        int n = Math.min(amount, s.getCount());
        ItemStack left = insert(s.copyWithCount(n));
        s.shrink(n - left.getCount());
        return s;
    }

    /**
     * Make the slot hold want (vanilla's swaps and drags): the difference from what it holds, through the handler.
     * What the item won't take goes back to the player.
     */
    @Override
    public void set(ItemStack want) {
        lastReturned = null;
        var h = handler();
        if (h == null || !want.isEmpty() && !fits(want)) {
            menu.giveBack(want.copy());
            return;
        }
        ItemResource cur = h.getResource(index), r = ItemResource.of(want);
        int have = cur.isEmpty() ? 0 : h.getAmountAsInt(index);
        ItemStack left = ItemStack.EMPTY;
        try (var tx = Transaction.openRoot()) {
            if (have > 0 && (want.isEmpty() || !cur.equals(r))) {
                h.extract(index, cur, have, tx);
                have = 0;
            }
            if (!want.isEmpty()) {
                int need = want.getCount() - have;
                if (need > 0) {
                    int in = h.insert(index, r, need, tx);
                    if (in < need) in += h.insert(r, need - in, tx);
                    left = want.copyWithCount(need - in);
                } else if (need < 0) {
                    h.extract(index, cur, -need, tx);
                }
            }
            tx.commit();
        }
        menu.giveBack(left);
    }

    /** Write back an in-place edit of the last getItem copy, only if the trunk still holds the stack it came from. */
    @Override
    public void setChanged() {
        ItemStack copy = lastReturned;
        if (copy == null || lastSource == null || source() != lastSource) return;
        var h = handler();
        if (h == null) return;
        ItemStack now = h.getResource(index).toStack(h.getAmountAsInt(index));
        if (!ItemStack.matches(copy, now)) set(copy);
    }

    @Override
    public boolean isSameInventory(Slot other) {
        return other instanceof InsideSlot;
    }
}
