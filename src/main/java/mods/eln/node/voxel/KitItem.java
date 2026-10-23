package mods.eln.node.voxel;

import net.minecraft.world.item.ItemStack;

/**
 * A kit: a length of pipe or cable kept as the item's damage (Baughn: a kit holds 1024 voxels, 64 blocks, of any tier).
 * length = MAX - damage. Pipe kits and cable kits merge, split and top each other up alike (KitRecipes, onPickup).
 */
public interface KitItem {
    int MAX = 1024;

    /** A kit of this item holding len voxels (empty if none). */
    ItemStack make(int len);

    static int length(ItemStack s) {
        return s.getItem() instanceof KitItem ? MAX - s.getDamageValue() : 0;
    }

    static void setLength(ItemStack s, int len) {
        s.setDamageValue(MAX - Math.max(0, Math.min(MAX, len)));
    }

    /**
     * Picking up a kit tops up the player's other kits of the same item first (a kit is a one-item stack holding up to
     * MAX voxels). What's left is picked up as usual.
     */
    static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre e) {
        var ent = e.getItemEntity();
        ItemStack s = ent.getItem();
        if (!(s.getItem() instanceof KitItem) || e.getPlayer().level().isClientSide() || ent.hasPickUpDelay()) return;
        int len = length(s);
        var inv = e.getPlayer().getInventory();
        for (int i = 0; i < inv.getContainerSize() && len > 0; i++) {
            ItemStack o = inv.getItem(i);
            if (o == s || o.getItem() != s.getItem()) continue;
            int room = MAX - length(o);
            if (room <= 0) continue;
            int take = Math.min(room, len);
            setLength(o, length(o) + take);
            len -= take;
        }
        if (len == length(s)) return;
        if (len <= 0) {
            e.getPlayer().take(ent, 1);
            ent.discard();
            e.setCanPickup(net.minecraft.util.TriState.FALSE);
        } else setLength(s, len);
    }
}
