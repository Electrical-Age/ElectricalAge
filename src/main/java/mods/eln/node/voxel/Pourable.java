package mods.eln.node.voxel;

import net.minecraft.world.item.ItemStack;

/**
 * An item whose one-item stack holds an amount of something that pours into another of its kind: cable, pipe and
 * track kits (KitItem: voxels) and overhead cable (blocks). Wherever two of a kind meet they merge (Baughn 2026-11-28:
 * kits in all circumstances, overhead cable as kits), through KitItem.pour and what is built on it.
 */
public interface Pourable {
    /** What the stack holds, in its own unit. */
    int held(ItemStack s);

    /** The most one stack holds. */
    int capacity();

    /** Set what the stack holds (0..capacity; the caller empties a stack left with none). */
    void setHeld(ItemStack s, int n);

    /** Two stacks of this item that may merge (a cable kit: the same spec; overhead cable: the same cross-section). */
    boolean sameKind(ItemStack a, ItemStack b);

    static int amount(ItemStack s) {
        return s.getItem() instanceof Pourable p ? p.held(s) : 0;
    }

    static int room(ItemStack s) {
        return s.getItem() instanceof Pourable p ? p.capacity() - p.held(s) : 0;
    }

    static boolean same(ItemStack a, ItemStack b) {
        return a.getItem() instanceof Pourable p && a.getItem() == b.getItem() && p.sameKind(a, b);
    }
}
