package mods.eln.node.voxel;

import net.minecraft.world.item.ItemStack;

/**
 * A kit: a length of pipe or cable (Baughn: a kit holds 1024 voxels, 64 blocks, of any tier), kept in the item's custom
 * data ("kitLen"), not as damage: other mods repair damaged items (an Occultism blacksmith familiar filled kits for free,
 * a player's report via Baughn). Kits from before keep their damage until they next change: length = MAX - damage.
 * The bar shows how full it is. Pipe kits and cable kits merge, split and top each other up alike (KitRecipes, onPickup).
 */
public interface KitItem {
    int MAX = 1024;

    /** A kit of this item holding len voxels (empty if none). */
    ItemStack make(int len);

    String LEN_KEY = "kitLen";

    static int length(ItemStack s) {
        if (!(s.getItem() instanceof KitItem)) return 0;
        var data = s.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null) {
            var t = data.copyTag();
            if (t.contains(LEN_KEY)) return Math.max(0, Math.min(MAX, t.getIntOr(LEN_KEY, MAX)));
        }
        Integer damage = s.get(net.minecraft.core.component.DataComponents.DAMAGE);   // (a kit from before: its damage)
        return damage == null ? MAX : Math.max(0, MAX - damage);
    }

    static void setLength(ItemStack s, int len) {
        int v = Math.max(0, Math.min(MAX, len));
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, s, t -> t.putInt(LEN_KEY, v));
        s.remove(net.minecraft.core.component.DataComponents.DAMAGE);
    }

    /** The bar: how full the kit is. */
    static int barWidth(ItemStack s) {
        return Math.round(13f * length(s) / MAX);
    }

    static int barColor(ItemStack s) {
        return net.minecraft.util.Mth.hsvToRgb(Math.max(0f, (float) length(s) / MAX) / 3f, 1f, 1f);
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
