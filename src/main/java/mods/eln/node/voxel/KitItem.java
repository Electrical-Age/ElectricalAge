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

    /** Other mods' slots over real storage that isn't a Container (by class name: no dependency). The kits in them merge
     *  when their menu closes; their filter and ghost slots, the same classes' siblings, are not listed. */
    java.util.Set<String> STORAGE_SLOTS = java.util.Set.of("net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot");

    /** The slot's container holds this very stack: plain storage, where a set stays (a chest, an inventory; not a
     *  result, ghost or handler slot, which answer with copies or with nothing behind them). */
    static boolean owned(net.minecraft.world.inventory.Slot slot) {
        ItemStack s = slot.getItem();
        int k = slot.getContainerSlot();
        return !s.isEmpty() && k >= 0 && k < slot.container.getContainerSize() && slot.container.getItem(k) == s && slot.getItem() == s;
    }

    static boolean storage(net.minecraft.world.inventory.Slot slot) {
        for (Class<?> c = slot.getClass(); c != null; c = c.getSuperclass()) if (STORAGE_SLOTS.contains(c.getName())) return true;
        return false;
    }

    /**
     * Shift-click (AbstractContainerMenu.moveItemStackTo, KitMoveMixin): a kit that fits whole into the kits of its
     * kind among the target slots goes into them (true: all of it went). All or nothing: a caller that is told "not
     * moved" doesn't finish its take, so a part poured out of a crafting result would be a free one. Only into slots
     * whose container owns the stack ({@link #owned}).
     */
    static boolean pourIntoSlots(ItemStack kit, java.util.List<net.minecraft.world.inventory.Slot> slots, int start, int end, boolean reverse) {
        if (!(kit.getItem() instanceof KitItem) || kit.getCount() != 1 || start < 0 || end > slots.size()) return false;
        int room = 0;
        for (int i = start; i < end; i++) {
            var slot = slots.get(i);
            ItemStack o = slot.getItem();
            if (o != kit && o.getCount() == 1 && same(o, kit) && owned(slot) && slot.mayPlace(kit)) room += MAX - length(o);
        }
        if (room < length(kit)) return false;
        for (int k = 0; k < end - start && !kit.isEmpty(); k++) {
            var slot = slots.get(reverse ? end - 1 - k : start + k);
            ItemStack o = slot.getItem();
            if (o == kit || o.getCount() != 1 || !same(o, kit) || !owned(slot) || !slot.mayPlace(kit)) continue;
            if (pour(kit, o) > 0) slot.setChanged();
        }
        return kit.isEmpty();
    }

    /** {@link #pourIntoSlots} for a menu's slots (KitMoveMixin). */
    public static boolean pourIntoSlotsOf(net.minecraft.world.inventory.AbstractContainerMenu menu, ItemStack kit, int start, int end, boolean reverse) {
        return pourIntoSlots(kit, menu.slots, start, end, reverse);
    }

    /**
     * A container closing (server): the kits in its own slots (a chest's, a backpack's; not the player's inventory,
     * which {@link #tick} keeps) merge. Only between slots of real storage: the container owns the stack
     * ({@link #owned}), or the slot's class is a known storage slot ({@link #STORAGE_SLOTS}), where each set is read
     * back and undone if it didn't hold.
     */
    static void onClose(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close e) {
        var p = e.getEntity();
        if (p.level().isClientSide() || p.isSpectator()) return;
        closing(p, e.getContainer().slots);
    }

    /** {@link #onClose}'s work (tests). */
    public static void closing(net.minecraft.world.entity.player.Player p, java.util.List<net.minecraft.world.inventory.Slot> slots) {
        for (int a = 0; a < slots.size(); a++) {
            var sa = slots.get(a);
            if (sa.container == p.getInventory() || !(sa.getItem().getItem() instanceof KitItem)) continue;
            for (int b = a + 1; b < slots.size(); b++) {
                var sb = slots.get(b);
                ItemStack into = sa.getItem(), from = sb.getItem();
                if (sb.container == p.getInventory() || from == into || into.getCount() != 1 || !(from.getItem() instanceof KitItem) || !same(from, into)
                    || !sa.mayPlace(into) || !sb.mayPlace(from) || !sa.mayPickup(p) || !sb.mayPickup(p)) continue;
                if (owned(sa) && owned(sb)) {
                    if (pour(from, into) <= 0) continue;
                    if (from.isEmpty()) sb.set(ItemStack.EMPTY);
                    sa.setChanged();
                    sb.setChanged();
                    continue;
                }
                if (!storage(sa) || !storage(sb)) continue;
                ItemStack was = into.copy(), grown = into.copy(), rest = from.copy();
                if (pour(rest, grown) <= 0) continue;
                sa.set(grown);
                if (sa.getItem().getCount() != 1 || length(sa.getItem()) != length(grown)) {
                    sa.set(was);
                    return;
                }
                sb.set(rest.isEmpty() ? ItemStack.EMPTY : rest);
                if (rest.isEmpty() ? !sb.getItem().isEmpty() : length(sb.getItem()) != length(rest)) {
                    sa.set(was);
                    return;
                }
            }
        }
    }

    /** A kit clicked onto a kit of its kind (either way round) pours into it (in a chest: the gesture one tries first). */
    static boolean clickedKit(ItemStack carried, ItemStack inSlot, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action,
                              net.minecraft.world.entity.player.Player player) {
        if (action != net.minecraft.world.inventory.ClickAction.PRIMARY || !slot.allowModification(player) || !owned(slot)) return false;
        if (pour(carried, inSlot) <= 0) return false;
        slot.setChanged();
        player.playSound(net.minecraft.sounds.SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
        return true;
    }

    /**
     * Picking up a kit tops up the player's other kits of the same item first (a kit is a one-item stack holding up to
     * MAX voxels). What's left is picked up as usual.
     */
    static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre e) {
        var ent = e.getItemEntity();
        ItemStack s = ent.getItem();
        if (!(s.getItem() instanceof KitItem) || e.getPlayer().level().isClientSide() || ent.hasPickUpDelay()) return;
        if (ent.getTarget() != null && !ent.getTarget().equals(e.getPlayer().getUUID())) return;   // (meant for someone else)
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
