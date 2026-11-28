package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
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

    /** A kit like this one (a cable kit: of its spec) holding len voxels. */
    default ItemStack make(ItemStack like, int len) {
        return make(len);
    }

    /** One kind of kit: the same item, and a cable kit the same spec (so they merge and top each other up). */
    static boolean same(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && java.util.Objects.equals(CableKitItem.specOf(a), CableKitItem.specOf(b));
    }

    String LEN_KEY = "kitLen";

    public static int lengthForTests(ItemStack s) {
        return length(s);
    }

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

    /** A kit's name with its length: "Iron Pipe Kit (7.5 blocks)" (cable, pipe and track kits alike). */
    static Component nameWithLength(String descriptionId, ItemStack s) {
        int v = length(s);
        String blocks = v % 16 == 0 ? (v / 16) + (v == 16 ? " block" : " blocks")
            : v % 8 == 0 ? String.format("%.1f blocks", v / 16.0) : String.format("%.2f blocks", v / 16.0);
        return Component.translatable(descriptionId).append(" (" + blocks + ")");
    }

    /** The bar: how full the kit is. */
    static int barWidth(ItemStack s) {
        return Math.round(13f * length(s) / MAX);
    }

    static int barColor(ItemStack s) {
        return net.minecraft.util.Mth.hsvToRgb(Math.max(0f, (float) length(s) / MAX) / 3f, 1f, 1f);
    }

    /**
     * Pour a kit into another of its kind, as much as fits (both changed in place; a kit poured out is left with
     * count 0). The voxels moved. Every place kits meet merges them through this (Baughn 2026-11-28: kits merge in all
     * circumstances; to split one for another player, throw the crafted kit).
     */
    static int pour(ItemStack from, ItemStack into) {
        if (from == into || from.isEmpty() || into.isEmpty() || from.getCount() != 1 || into.getCount() != 1
            || !(from.getItem() instanceof KitItem) || !same(from, into)) return 0;
        int n = Math.min(length(from), MAX - length(into));
        if (n <= 0) return 0;
        setLength(into, length(into) + n);
        setLength(from, length(from) - n);
        if (length(from) <= 0) from.setCount(0);
        return n;
    }

    /**
     * Merge the kits of a container that owns its stacks (a player's inventory, the Toolbag's slots): each kit pours
     * into the kits of its kind before it, the one in slot `first` (the hand's; -1: none) before all. True if any moved.
     */
    public static boolean tidy(net.minecraft.world.Container c, int size, int first) {
        int[] order = new int[size];
        int n = 0;
        if (first >= 0 && first < size) order[n++] = first;
        for (int i = 0; i < size; i++) if (i != first) order[n++] = i;
        boolean moved = false;
        for (int a = 0; a < n; a++) {
            ItemStack into = c.getItem(order[a]);
            if (!(into.getItem() instanceof KitItem) || into.getCount() != 1) continue;
            for (int b = a + 1; b < n && length(into) < MAX; b++) {
                ItemStack from = c.getItem(order[b]);
                if (pour(from, into) <= 0) continue;
                moved = true;
                if (from.isEmpty()) c.setItem(order[b], ItemStack.EMPTY);
            }
        }
        if (moved) c.setChanged();
        return moved;
    }

    /** A player's kits merge once a second (server): however they reached the inventory. The held kit takes the others. */
    static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        var p = e.getEntity();
        if (p.level().isClientSide() || (p.tickCount + p.getId()) % 20 != 0) return;
        var inv = p.getInventory();
        tidy(inv, inv.getContainerSize(), inv.getSelectedItem().getItem() instanceof KitItem ? inv.getSelectedSlot() : -1);
    }

    /**
     * Shift-click (AbstractContainerMenu.moveItemStackTo, KitMoveMixin): a kit that fits whole into the kits of its
     * kind among the target slots goes into them (true: all of it went). All or nothing: a caller that is told "not
     * moved" doesn't finish its take, so a part poured out of a crafting result would be a free one.
     */
    static boolean pourIntoSlots(ItemStack kit, java.util.List<net.minecraft.world.inventory.Slot> slots, int start, int end, boolean reverse) {
        if (!(kit.getItem() instanceof KitItem) || kit.getCount() != 1 || start < 0 || end > slots.size()) return false;
        int room = 0;
        for (int i = start; i < end; i++) {
            ItemStack o = slots.get(i).getItem();
            if (o != kit && o.getCount() == 1 && same(o, kit) && slots.get(i).mayPlace(kit)) room += MAX - length(o);
        }
        if (room < length(kit)) return false;
        for (int k = 0; k < end - start && !kit.isEmpty(); k++) {
            var slot = slots.get(reverse ? end - 1 - k : start + k);
            ItemStack o = slot.getItem();
            if (o == kit || o.getCount() != 1 || !same(o, kit) || !slot.mayPlace(kit)) continue;
            ItemStack grown = o.copy();
            if (pour(kit, grown) > 0) slot.set(grown);
        }
        return kit.isEmpty();
    }

    /** {@link #pourIntoSlots} for a menu's slots (KitMoveMixin). */
    public static boolean pourIntoSlotsOf(net.minecraft.world.inventory.AbstractContainerMenu menu, ItemStack kit, int start, int end, boolean reverse) {
        return pourIntoSlots(kit, menu.slots, start, end, reverse);
    }

    /**
     * A container closing (server): the kits in its own slots (a backpack's, a chest's; not the player's inventory,
     * which {@link #tick} keeps) merge. Through the slots, and only where both take a kit placed by hand (not a result
     * slot) and show what was set: a slot that doesn't keep it is left alone and the other put back.
     */
    static void onClose(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close e) {
        var p = e.getEntity();
        if (p.level().isClientSide()) return;
        var slots = e.getContainer().slots;
        for (int a = 0; a < slots.size(); a++) {
            var sa = slots.get(a);
            if (sa.container == p.getInventory() || !(sa.getItem().getItem() instanceof KitItem)) continue;
            for (int b = a + 1; b < slots.size(); b++) {
                var sb = slots.get(b);
                ItemStack into = sa.getItem(), from = sb.getItem();
                if (sb.container == p.getInventory() || from == into || !(from.getItem() instanceof KitItem) || !same(from, into)
                    || !sa.mayPlace(into) || !sb.mayPlace(from) || !sa.mayPickup(p) || !sb.mayPickup(p)) continue;
                ItemStack was = into.copy(), grown = into.copy(), rest = from.copy();
                if (pour(rest, grown) <= 0) continue;
                sa.set(grown);
                if (sa.getItem().getCount() != 1 || length(sa.getItem()) != length(grown)) {
                    sa.set(was);
                    break;
                }
                sb.set(rest.isEmpty() ? ItemStack.EMPTY : rest);
                if (rest.isEmpty() ? !sb.getItem().isEmpty() : length(sb.getItem()) != length(rest)) {
                    sa.set(was);
                    break;
                }
            }
        }
    }

    /**
     * Picking up a kit tops up the player's other kits of the same item first (a kit is a one-item stack holding up to
     * MAX voxels). What's left is picked up as usual.
     */
    static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre e) {
        var ent = e.getItemEntity();
        ItemStack s = ent.getItem();
        if (!(s.getItem() instanceof KitItem) || e.getPlayer().level().isClientSide() || ent.hasPickUpDelay()) return;
        int before = length(s);
        ItemStack rest = s.copy();
        var inv = e.getPlayer().getInventory();
        for (int i = 0; i < inv.getContainerSize() && !rest.isEmpty(); i++) pour(rest, inv.getItem(i));
        if (rest.isEmpty()) {
            e.getPlayer().take(ent, 1);
            ent.discard();
            e.setCanPickup(net.minecraft.util.TriState.FALSE);
        } else if (length(rest) != before) setLength(s, length(rest));
    }
}
