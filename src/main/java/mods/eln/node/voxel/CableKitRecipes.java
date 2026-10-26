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
 * one coil and keeps the rest (16 voxels less) in the grid (KitRecipes.Unroll, as pipe kits).
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
            return KitRecipes.use(items, s -> s.getItem() instanceof CableKitItem k ? k.tier : -1, CableKitRecipes::coilTier, t -> 1);
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            int[] t = total(input);
            return t != null && (tier < 0 || t[0] == tier) && !PipeKitRecipes.otherRecipe(input, level);
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

    public static final class ToCoil extends KitRecipes.Unroll {
        /** Any tier (tests); the recipes are one per tier, as FromCoils. */
        public static final ToCoil INSTANCE = new ToCoil(-1);
        public static final MapCodec<ToCoil> MAP_CODEC = tierCodec(ToCoil::new, r -> r.tier);
        public static final StreamCodec<RegistryFriendlyByteBuf, ToCoil> STREAM_CODEC = tierStream(ToCoil::new, r -> r.tier);
        public static final RecipeSerializer<ToCoil> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        public ToCoil(int tier) {
            super(tier);
        }

        @Override
        protected int kitTier(ItemStack s) {
            return s.getItem() instanceof CableKitItem k ? k.tier : -1;
        }

        @Override
        protected ItemStack unit(int tier) {
            return coil(tier);
        }

        @Override
        protected ItemStack kit(int tier, int len) {
            return CableKitItem.kit(tier, len);
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
        KitRecipes.crafted(e, FromCoils.class, FromCoils::use);
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
            for (int n : new int[]{9}) {   // (the one-cable recipe JEI lists itself, from display(): AdventurAgent saw it twice)
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
