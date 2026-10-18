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

/**
 * Pipe kits in the crafting grid (Baughn, notes/pipes/DESIGN.md 6a): kits of one tier put together merge (their lengths
 * add, up to a full kit); a lone kit splits in two halves (one is the result, the other stays in the grid).
 */
public final class PipeKitRecipes {
    private PipeKitRecipes() {
    }

    public static final class Merge extends CustomRecipe {
        public static final Merge INSTANCE = new Merge();
        public static final MapCodec<Merge> MAP_CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<RegistryFriendlyByteBuf, Merge> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        public static final RecipeSerializer<Merge> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        /** One of 2+ kits of one item (each a single item) and their total length, or null. */
        static Object[] total(CraftingInput in) {
            ItemStack kit = null;
            int len = 0, n = 0;
            for (int i = 0; i < in.size(); i++) {
                ItemStack s = in.getItem(i);
                if (s.isEmpty()) continue;
                if (!(s.getItem() instanceof KitItem) || s.getCount() != 1 || (kit != null && s.getItem() != kit.getItem())) return null;
                kit = s;
                len += KitItem.length(s);
                n++;
            }
            return n >= 2 && len <= KitItem.MAX ? new Object[]{kit, len} : null;
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return total(input) != null;
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            Object[] t = total(input);
            return t == null ? ItemStack.EMPTY : ((KitItem) ((ItemStack) t[0]).getItem()).make((Integer) t[1]);
        }

        @Override
        public RecipeSerializer<Merge> getSerializer() {
            return SERIALIZER;
        }
    }

    public static final class Split extends CustomRecipe {
        public static final Split INSTANCE = new Split();
        public static final MapCodec<Split> MAP_CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<RegistryFriendlyByteBuf, Split> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        public static final RecipeSerializer<Split> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        /** The one kit (2+ voxels long) in the grid, or null. */
        static ItemStack lone(CraftingInput in) {
            ItemStack found = null;
            for (int i = 0; i < in.size(); i++) {
                ItemStack s = in.getItem(i);
                if (s.isEmpty()) continue;
                if (found != null || !(s.getItem() instanceof PipeKitItem) || s.getCount() != 1) return null;
                found = s;
            }
            return found != null && PipeKitItem.length(found) >= 2 ? found : null;
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return lone(input) != null;
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            ItemStack s = lone(input);
            return s == null ? ItemStack.EMPTY : PipeKitItem.kit(((PipeKitItem) s.getItem()).tier, PipeKitItem.length(s) / 2);
        }

        /** The other half stays in the grid. */
        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
            for (int i = 0; i < input.size(); i++) {
                ItemStack s = input.getItem(i);
                if (s.getItem() instanceof PipeKitItem k) {
                    int len = PipeKitItem.length(s);
                    out.set(i, PipeKitItem.kit(k.tier, len - len / 2));
                }
            }
            return out;
        }

        @Override
        public RecipeSerializer<Split> getSerializer() {
            return SERIALIZER;
        }
    }
}
