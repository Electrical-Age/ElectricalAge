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
        /** Any tier (tests). The recipes themselves are one per tier (data/eln/recipe/cable_kit_from_coils_*.json). */
        public static final FromCoils INSTANCE = new FromCoils(-1);
        public static final MapCodec<FromCoils> MAP_CODEC = tierCodec(FromCoils::new, r -> r.tier);
        public static final StreamCodec<RegistryFriendlyByteBuf, FromCoils> STREAM_CODEC = tierStream(FromCoils::new, r -> r.tier);
        public static final RecipeSerializer<FromCoils> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        /**
         * One recipe per tier (AdventurAgent: with one recipe for all tiers the book offered every tier's kit from LV
         * cable alone, and clicking the MV kit placed LV cable); -1 matches any.
         */
        public final int tier;

        public FromCoils(int tier) {
            this.tier = tier;
        }

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
            int[] t = total(input);
            return t != null && (tier < 0 || t[0] == tier);
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            int[] t = total(input);
            return t == null || tier >= 0 && t[0] != tier ? ItemStack.EMPTY : CableKitItem.kit(t[0], t[1]);
        }

        @Override
        public RecipeSerializer<FromCoils> getSerializer() {
            return SERIALIZER;
        }

        /**
         * In the recipe book (Baughn: it wasn't in the recipe browser or JEI): learnable, unlocked by holding cable
         * (data/eln/advancement/recipes/misc/cable_kit_from_coils.json), shown as one cable to a one-cable kit per tier.
         * The grid takes whole stacks all the same. (The any-tier instance stays out of the book.)
         */
        @Override
        public boolean isSpecial() {
            return tier < 0;
        }

        @Override
        public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
            ItemStack c = tier < 0 ? ItemStack.EMPTY : coil(tier);
            return c.isEmpty() ? net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE
                : net.minecraft.world.item.crafting.PlacementInfo.create(net.minecraft.world.item.crafting.Ingredient.of(c.getItem()));
        }

        @Override
        public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
            ItemStack c = tier < 0 ? ItemStack.EMPTY : coil(tier);
            if (c.isEmpty()) return List.of();
            return List.of(new net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay(
                List.of(slot(c)), slot(CableKitItem.kit(tier, 16)), table()));
        }
    }

    public static final class ToCoil extends CustomRecipe {
        /** Any tier (tests); the recipes are one per tier, as FromCoils. */
        public static final ToCoil INSTANCE = new ToCoil(-1);
        public static final MapCodec<ToCoil> MAP_CODEC = tierCodec(ToCoil::new, r -> r.tier);
        public static final StreamCodec<RegistryFriendlyByteBuf, ToCoil> STREAM_CODEC = tierStream(ToCoil::new, r -> r.tier);
        public static final RecipeSerializer<ToCoil> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        public final int tier;

        public ToCoil(int tier) {
            this.tier = tier;
        }

        ItemStack lone(CraftingInput in) {
            ItemStack found = null;
            for (int i = 0; i < in.size(); i++) {
                ItemStack s = in.getItem(i);
                if (s.isEmpty()) continue;
                if (found != null || !(s.getItem() instanceof CableKitItem) || s.getCount() != 1) return null;
                found = s;
            }
            if (found != null && tier >= 0 && ((CableKitItem) found.getItem()).tier != tier) return null;
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

        /** In the recipe book too: a kit gives a cable back (and keeps the rest), per tier. */
        @Override
        public boolean isSpecial() {
            return tier < 0;
        }

        @Override
        public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
            if (tier < 0) return net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE;
            return net.minecraft.world.item.crafting.PlacementInfo.create(net.minecraft.world.item.crafting.Ingredient.of(CableKitItem.kit(tier, 32).getItem()));
        }

        @Override
        public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
            ItemStack c = tier < 0 ? ItemStack.EMPTY : coil(tier);
            if (c.isEmpty()) return List.of();
            return List.of(new net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay(
                List.of(slot(CableKitItem.kit(tier, 32))), slot(c), table()));
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

    /** Codecs for a recipe with a tier ({"tier": n}; absent: any tier). */
    static <R> MapCodec<R> tierCodec(java.util.function.IntFunction<R> make, java.util.function.ToIntFunction<R> tier) {
        return com.mojang.serialization.Codec.INT.optionalFieldOf("tier", -1).xmap(make::apply, tier::applyAsInt);
    }

    static <R> StreamCodec<RegistryFriendlyByteBuf, R> tierStream(java.util.function.IntFunction<R> make, java.util.function.ToIntFunction<R> tier) {
        return net.minecraft.network.codec.ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(make::apply, tier::applyAsInt);
    }

    static net.minecraft.world.item.crafting.display.SlotDisplay slot(ItemStack s) {
        return new net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(s));
    }

    static net.minecraft.world.item.crafting.display.SlotDisplay table() {
        return new net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay(net.minecraft.world.item.Items.CRAFTING_TABLE.builtInRegistryHolder());
    }

    /** Example recipes for JEI (compat/jei), which can't list these itself: per tier, 1 and 9 cables to a kit, a kit to a cable. */
    public static List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> examples() {
        List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> out = new java.util.ArrayList<>();
        var info = new net.minecraft.world.item.crafting.Recipe.CommonInfo(false);
        var book = new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(net.minecraft.world.item.crafting.CraftingBookCategory.MISC, "eln_cable_kit");
        for (int t = 0; t < CablePart.TIERS.length; t++) {
            ItemStack c = coil(t);
            if (c.isEmpty()) continue;
            var ing = net.minecraft.world.item.crafting.Ingredient.of(c.getItem());
            for (int n : new int[]{1, 9}) {
                List<net.minecraft.world.item.crafting.Ingredient> ings = new java.util.ArrayList<>();
                for (int i = 0; i < n; i++) ings.add(ing);
                out.add(new net.minecraft.world.item.crafting.RecipeHolder<>(key("cable_kit_from_coils_" + t + "_" + n),
                    new net.minecraft.world.item.crafting.ShapelessRecipe(info, book, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(CableKitItem.kit(t, 16 * n)), ings)));
            }
            var kit = CableKitItem.kit(t, 32);
            out.add(new net.minecraft.world.item.crafting.RecipeHolder<>(key("cable_kit_to_coil_" + t),
                new net.minecraft.world.item.crafting.ShapelessRecipe(info, book, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(c),
                    List.of(net.minecraft.world.item.crafting.Ingredient.of(kit.getItem())))));
        }
        return out;
    }

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key(String path) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
            net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "jei/" + path));
    }
}
