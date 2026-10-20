package mods.eln.node.voxel;

import com.mojang.serialization.MapCodec;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Cable kits and the old cable items ("coils": still crafting ingredients and device components; Baughn 2026-10-18):
 * coils of one tier, with or without a kit of that tier, make a kit 16 voxels longer per coil; a lone cable kit gives
 * one coil and keeps the rest (16 voxels less) in the grid.
 */
public final class CableKitRecipes {
    private CableKitRecipes() {
    }

    /** The cable tier of a coil (an old six-node cable item), or -1. */
    public static int coilTier(ItemStack s) {
        if (s.isEmpty()) return -1;
        var d = GenericItemBlockUsingDamageDescriptor.getDescriptor(s);
        if (d instanceof ElectricalCableDescriptor c) return CablePart.tierOf(c.name);
        if (d instanceof mods.eln.sixnode.thermalcable.ThermalCableDescriptor t) return CablePart.tierOf(t.name);
        return -1;
    }

    /** A coil of a tier. */
    public static ItemStack coil(int tier) {
        var d = CablePart.TIERS[tier].sixDescriptor();
        return d == null ? ItemStack.EMPTY : d.newItemStack();
    }

    public static final class FromCoils extends CustomRecipe {
        public static final FromCoils INSTANCE = new FromCoils();
        public static final MapCodec<FromCoils> MAP_CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<RegistryFriendlyByteBuf, FromCoils> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        public static final RecipeSerializer<FromCoils> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        /**
         * {tier, length, cables used}: coils of one tier and at most one kit of it; the kit takes whole stacks of cable,
         * as many as fit (Baughn: a grid of cable stacks gave one cable's worth per slot, and shift-click a heap of small
         * kits). At least one cable from every slot (the crafting grid takes one from each), or null.
         */
        static int[] total(CraftingInput in) {
            List<ItemStack> items = new java.util.ArrayList<>();
            for (int i = 0; i < in.size(); i++) items.add(in.getItem(i));
            int[] use = use(items);
            if (use == null) return null;
            int tier = use[use.length - 3], len = use[use.length - 2], cables = use[use.length - 1];
            return new int[]{tier, len, cables};
        }

        /**
         * How many cables each slot gives (the slots in order, first come first served), then tier, length and the
         * cables used; or null. Shared by the recipe and the crafted event (CableKitRecipes.crafted), which takes them.
         */
        public static int[] useForTests(List<ItemStack> items) {
            return use(items);
        }

        static int[] use(List<ItemStack> items) {
            int tier = -1, kitLen = 0, kits = 0, slots = 0;
            for (ItemStack s : items) {
                if (s.isEmpty()) continue;
                int t;
                if (s.getItem() instanceof CableKitItem k) {
                    if (++kits > 1 || s.getCount() != 1) return null;
                    t = k.tier;
                    kitLen += KitItem.length(s);
                } else {
                    t = coilTier(s);
                    if (t < 0) return null;
                    slots++;
                }
                if (tier >= 0 && t != tier) return null;
                tier = t;
            }
            int room = (KitItem.MAX - kitLen) / 16;
            if (slots < 1 || room < slots) return null;
            int[] out = new int[items.size() + 3];
            int used = 0;
            // one from each slot first (the grid takes that anyway), then the rest in order while it fits
            for (int i = 0; i < items.size(); i++)
                if (!items.get(i).isEmpty() && !(items.get(i).getItem() instanceof CableKitItem)) {
                    out[i] = 1;
                    used++;
                }
            for (int i = 0; i < items.size() && used < room; i++) {
                ItemStack s = items.get(i);
                if (s.isEmpty() || s.getItem() instanceof CableKitItem) continue;
                int more = Math.min(s.getCount() - 1, room - used);
                out[i] += more;
                used += more;
            }
            out[items.size()] = tier;
            out[items.size() + 1] = kitLen + used * 16;
            out[items.size() + 2] = used;
            return out;
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return total(input) != null;
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            int[] t = total(input);
            return t == null ? ItemStack.EMPTY : CableKitItem.kit(t[0], t[1]);
        }

        @Override
        public RecipeSerializer<FromCoils> getSerializer() {
            return SERIALIZER;
        }
    }

    public static final class ToCoil extends CustomRecipe {
        public static final ToCoil INSTANCE = new ToCoil();
        public static final MapCodec<ToCoil> MAP_CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<RegistryFriendlyByteBuf, ToCoil> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        public static final RecipeSerializer<ToCoil> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        static ItemStack lone(CraftingInput in) {
            ItemStack found = null;
            for (int i = 0; i < in.size(); i++) {
                ItemStack s = in.getItem(i);
                if (s.isEmpty()) continue;
                if (found != null || !(s.getItem() instanceof CableKitItem) || s.getCount() != 1) return null;
                found = s;
            }
            return found != null && KitItem.length(found) >= 16 ? found : null;
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return lone(input) != null;
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            ItemStack s = lone(input);
            return s == null ? ItemStack.EMPTY : coil(((CableKitItem) s.getItem()).tier);
        }

        /** The rest of the kit stays in the grid. */
        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
            for (int i = 0; i < input.size(); i++) {
                ItemStack s = input.getItem(i);
                if (s.getItem() instanceof CableKitItem k) out.set(i, CableKitItem.kit(k.tier, KitItem.length(s) - 16));
            }
            return out;
        }

        @Override
        public RecipeSerializer<ToCoil> getSerializer() {
            return SERIALIZER;
        }
    }

    /**
     * A kit crafted from cables takes the cables it counted: the grid takes one from each slot after this, so the rest
     * goes here (both sides, as the grid's own taking).
     */
    public static void crafted(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e) {
        if (!(e.getCrafting().getItem() instanceof CableKitItem)) return;
        var grid = e.getInventory();
        List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < grid.getContainerSize(); i++) items.add(grid.getItem(i));
        int[] use = FromCoils.use(items);
        if (use == null) return;
        if (KitItem.length(e.getCrafting()) != use[items.size() + 1]) return;   // (not this recipe's kit)
        for (int i = 0; i < items.size(); i++)
            if (use[i] > 1) grid.getItem(i).shrink(use[i] - 1);
    }
}
