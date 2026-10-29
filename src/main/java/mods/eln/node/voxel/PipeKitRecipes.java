package mods.eln.node.voxel;

import com.mojang.serialization.MapCodec;
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
 * Pipe kits in the crafting grid (Baughn, notes/pipes/DESIGN.md 6a): kits of one tier put together merge (their lengths
 * add; over a full kit the rest stays in the grid); a lone kit gives a block's worth of plates back and keeps the rest
 * (Baughn: as cable kits, not halves).
 */
public final class PipeKitRecipes {
    private PipeKitRecipes() {
    }

    /** The plate a tier's pipe is made of, and how many make one block (16 voxels) of it: 2 iron plates (the old
     *  recipe), 1 alloy plate (Baughn: 3 m per craft of 3 plates instead of 1). */
    static final String[] PLATE = {"eln:iron_plate", "eln:alloy_plate", "eln:alloy_plate"};
    /** (The large alloy pipe: 8 alloy plates a block, made by its own shaped recipe; plates on their own make the alloy
     *  pipe, plateTier finding it first.) */
    static final int[] PER_BLOCK = {2, 1, 8};

    static net.minecraft.world.item.Item plate(int tier) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(PLATE[tier]));
    }

    /** The pipe tier a stack of plates makes, or -1: any plate in the tier's eln:high_quality_plates tag (Baughn
     *  2026-10-27: EA's own, and what a modpack adds; a lone kit gives back EA's). */
    public static int plateTier(ItemStack s) {
        if (s.isEmpty()) return -1;
        for (int t = 0; t < PLATE_TIERS; t++) if (s.is(mods.eln.misc.OreDictTags.highQualityPlates(METAL[t]))) return t;
        return -1;
    }

    static final String[] METAL = {"iron", "alloy", "alloy"};
    /** The tiers plates alone make (iron, alloy); the large alloy pipe has its own recipe. */
    static final int PLATE_TIERS = 2;

    /**
     * Pipe kits from plates (Baughn: pipe kits didn't stack up or craft many at a time; cable kits had been fixed):
     * plates of one tier, with or without a kit of that tier, make a kit a block longer per PER_BLOCK plates, from whole
     * stacks, as many as fit in one kit (at least one plate from every slot: the grid takes one from each; the rest by
     * the crafted event, {@link #crafted}). As CableKitRecipes.FromCoils.
     */
    public static final class FromPlates extends CustomRecipe {
        public static final FromPlates INSTANCE = new FromPlates(-1);
        public static final MapCodec<FromPlates> MAP_CODEC = CableKitRecipes.tierCodec(FromPlates::new, r -> r.tier);
        public static final StreamCodec<RegistryFriendlyByteBuf, FromPlates> STREAM_CODEC = CableKitRecipes.tierStream(FromPlates::new, r -> r.tier);
        public static final RecipeSerializer<FromPlates> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        public final int tier;

        public FromPlates(int tier) {
            this.tier = tier;
        }

        /** Plates taken from each slot (in order), then tier, length and plates used; or null. */
        public static int[] use(List<ItemStack> items) {
            return KitRecipes.use(items, s -> s.getItem() instanceof PipeKitItem k ? k.tier : -1, PipeKitRecipes::plateTier, t -> PER_BLOCK[t]);
        }

        static int[] total(CraftingInput in) {
            List<ItemStack> items = new java.util.ArrayList<>();
            for (int i = 0; i < in.size(); i++) items.add(in.getItem(i));
            int[] u = use(items);
            return u == null ? null : new int[]{u[u.length - 3], u[u.length - 2]};
        }

        /**
         * Plates of this tier (and at most one kit), however few: when there are too few for a block it matches with
         * no result. After a craft the grid asks the matching recipe for its remainders; with too few plates left to
         * match, vanilla gave the inputs back instead (AdventurAgent: alloy kits left a plate behind).
         */
        @Override
        public boolean matches(CraftingInput input, Level level) {
            int t = -1, kits = 0, plates = 0;
            for (int i = 0; i < input.size(); i++) {
                ItemStack s = input.getItem(i);
                if (s.isEmpty()) continue;
                int st;
                if (s.getItem() instanceof PipeKitItem k) {
                    if (++kits > 1 || s.getCount() != 1) return false;
                    st = k.tier;
                } else {
                    st = plateTier(s);
                    if (st < 0) return false;
                    plates++;
                }
                if (t >= 0 && st != t) return false;
                t = st;
            }
            return plates > 0 && (tier < 0 || t == tier) && !otherRecipe(input, level);
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            int[] t = total(input);
            return t == null || tier >= 0 && t[0] != tier ? ItemStack.EMPTY : PipeKitItem.kit(t[0], t[1]);
        }

        /** Nothing stays behind: the plates are used up (an EA plate's crafting remainder is the plate itself, so the
         *  default put one back each craft: AdventurAgent's alloy kits left a plate). */
        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            return NonNullList.withSize(input.size(), ItemStack.EMPTY);
        }

        @Override
        public RecipeSerializer<FromPlates> getSerializer() {
            return SERIALIZER;
        }

        /** In the recipe book: PER_BLOCK plates to a one-block kit, per tier (the grid takes whole stacks all the same). */
        @Override
        public boolean isSpecial() {
            return tier < 0;
        }

        @Override
        public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
            if (tier < 0) return net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE;
            List<net.minecraft.world.item.crafting.Ingredient> ings = new java.util.ArrayList<>();
            for (int i = 0; i < PER_BLOCK[tier]; i++) ings.add(net.minecraft.world.item.crafting.Ingredient.of(plate(tier)));
            return net.minecraft.world.item.crafting.PlacementInfo.create(ings);
        }

        @Override
        public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
            if (tier < 0) return List.of();
            List<net.minecraft.world.item.crafting.display.SlotDisplay> in = new java.util.ArrayList<>();
            for (int i = 0; i < PER_BLOCK[tier]; i++) in.add(CableKitRecipes.slot(new ItemStack(plate(tier))));
            return List.of(new net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay(in,
                CableKitRecipes.slot(PipeKitItem.kit(tier, 16)), CableKitRecipes.table()));
        }
    }

    /**
     * Does another crafting recipe take this grid (server; the client can't tell)? Kits made from whole stacks match
     * any grid of their items, so they step aside for those: EA's casing is 8 iron plates in a ring.
     */
    static boolean otherRecipe(CraftingInput input, Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return false;
        for (var h : sl.recipeAccess().recipeMap().byType(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            var r = h.value();
            if (r instanceof FromPlates || r instanceof CableKitRecipes.FromCoils || r instanceof Merge || r instanceof KitRecipes.Unroll) continue;
            if (r.matches(input, level)) return true;
        }
        return false;
    }

    /**
     * Was this craft one of cls's recipes? Asked of the grid on the server (AdventurAgent: a shift-click craft passes
     * the moved, often empty, stack as the crafted item, so checking that let the kit take one plate per slot: a free
     * kit's worth of plates). The client leaves it to the server's sync.
     */
    static boolean ours(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e, Class<?> cls) {
        if (!(e.getEntity().level() instanceof net.minecraft.server.level.ServerLevel sl) || !(e.getInventory() instanceof net.minecraft.world.inventory.CraftingContainer cc))
            return false;
        var in = cc.asCraftInput();
        return sl.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, in, sl).map(h -> cls.isInstance(h.value())).orElse(false);
    }

    /** A kit crafted from plates takes the plates it counted (the grid takes one from each slot after this). */
    public static void crafted(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e) {
        KitRecipes.crafted(e, FromPlates.class, FromPlates::use);
    }

    /** JEI examples: per tier, 9 slots of plates to a kit (kit to plates JEI lists itself, from display(): AdventurAgent saw it twice). */
    public static List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> examples() {
        List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> out = new java.util.ArrayList<>();
        var info = new net.minecraft.world.item.crafting.Recipe.CommonInfo(false);
        var book = new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(net.minecraft.world.item.crafting.CraftingBookCategory.MISC, "eln_pipe_kit");
        for (int t = 0; t < PLATE_TIERS; t++) {
            var ing = net.minecraft.world.item.crafting.Ingredient.of(plate(t));
            for (int n : new int[]{9}) {   // (the one-block recipe JEI lists itself, from display(): AdventurAgent saw it twice)
                List<net.minecraft.world.item.crafting.Ingredient> ings = new java.util.ArrayList<>();
                for (int i = 0; i < n; i++) ings.add(ing);
                out.add(new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "jei/pipe_kit_from_plates_" + t + "_" + n)),
                    new net.minecraft.world.item.crafting.ShapelessRecipe(info, book,
                        net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(PipeKitItem.kit(t, 16 * (n / PER_BLOCK[t]))), ings)));
            }
        }
        return out;
    }

    public static final class Merge extends CustomRecipe {
        public static final Merge INSTANCE = new Merge();
        public static final MapCodec<Merge> MAP_CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<RegistryFriendlyByteBuf, Merge> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        public static final RecipeSerializer<Merge> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        /**
         * One of 2+ kits of one item (each a single item, none full) and their total length, or null. Over a full kit
         * the result is a full kit and the rest stays in the grid (Baughn: kits top up the same way, pipe and cable;
         * 63 + 2 blocks gave nothing).
         */
        static Object[] total(CraftingInput in) {
            ItemStack kit = null;
            int len = 0, n = 0;
            for (int i = 0; i < in.size(); i++) {
                ItemStack s = in.getItem(i);
                if (s.isEmpty()) continue;
                if (!(s.getItem() instanceof KitItem) || s.getCount() != 1 || (kit != null && s.getItem() != kit.getItem())) return null;
                if (KitItem.length(s) >= KitItem.MAX) return null;
                kit = s;
                len += KitItem.length(s);
                n++;
            }
            return n >= 2 ? new Object[]{kit, len} : null;
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return total(input) != null;
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            Object[] t = total(input);
            return t == null ? ItemStack.EMPTY : ((KitItem) ((ItemStack) t[0]).getItem()).make(Math.min(KitItem.MAX, (Integer) t[1]));
        }

        /** What's over a full kit stays in the grid, as one kit in the last kit's slot. */
        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
            Object[] t = total(input);
            if (t == null || (Integer) t[1] <= KitItem.MAX) return out;
            for (int i = input.size() - 1; i >= 0; i--)
                if (!input.getItem(i).isEmpty()) {
                    out.set(i, ((KitItem) ((ItemStack) t[0]).getItem()).make((Integer) t[1] - KitItem.MAX));
                    break;
                }
            return out;
        }

        @Override
        public RecipeSerializer<Merge> getSerializer() {
            return SERIALIZER;
        }
    }

    /** A lone kit gives a block's worth of plates back and keeps the rest (KitRecipes.Unroll, as cable kits). */
    public static final class ToPlates extends KitRecipes.Unroll {
        public static final ToPlates INSTANCE = new ToPlates(-1);
        public static final MapCodec<ToPlates> MAP_CODEC = CableKitRecipes.tierCodec(ToPlates::new, r -> r.tier);
        public static final StreamCodec<RegistryFriendlyByteBuf, ToPlates> STREAM_CODEC = CableKitRecipes.tierStream(ToPlates::new, r -> r.tier);
        public static final RecipeSerializer<ToPlates> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        public ToPlates(int tier) {
            super(tier);
        }

        @Override
        protected int kitTier(ItemStack s) {
            return s.getItem() instanceof PipeKitItem k ? k.tier : -1;
        }

        @Override
        protected ItemStack unit(int tier) {
            return new ItemStack(plate(tier), PER_BLOCK[tier]);
        }

        @Override
        protected ItemStack kit(int tier, int len) {
            return PipeKitItem.kit(tier, len);
        }

        @Override
        public RecipeSerializer<ToPlates> getSerializer() {
            return SERIALIZER;
        }
    }
}
