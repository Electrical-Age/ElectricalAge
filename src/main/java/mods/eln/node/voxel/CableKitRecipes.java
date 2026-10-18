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
        return d instanceof ElectricalCableDescriptor c ? CablePart.tierOf(c.name) : -1;
    }

    /** A coil of a tier. */
    public static ItemStack coil(int tier) {
        var d = CablePart.TIERS[tier].descriptor();
        return d == null ? ItemStack.EMPTY : d.newItemStack();
    }

    public static final class FromCoils extends CustomRecipe {
        public static final FromCoils INSTANCE = new FromCoils();
        public static final MapCodec<FromCoils> MAP_CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<RegistryFriendlyByteBuf, FromCoils> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        public static final RecipeSerializer<FromCoils> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        /** {tier, length}: 1+ coils of one tier and at most one kit of it, fitting in a kit; or null. */
        static int[] total(CraftingInput in) {
            int tier = -1, len = 0, coils = 0, kits = 0;
            for (int i = 0; i < in.size(); i++) {
                ItemStack s = in.getItem(i);
                if (s.isEmpty()) continue;
                int t;
                if (s.getItem() instanceof CableKitItem k) {
                    if (++kits > 1 || s.getCount() != 1) return null;
                    t = k.tier;
                    len += KitItem.length(s);
                } else {
                    t = coilTier(s);
                    if (t < 0) return null;
                    coils++;
                    len += 16;
                }
                if (tier >= 0 && t != tier) return null;
                tier = t;
            }
            return coils >= 1 && len <= KitItem.MAX ? new int[]{tier, len} : null;
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
}
