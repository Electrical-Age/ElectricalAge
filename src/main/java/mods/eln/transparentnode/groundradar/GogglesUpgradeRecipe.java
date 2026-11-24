package mods.eln.transparentnode.groundradar;

import com.mojang.serialization.MapCodec;
import mods.eln.Eln;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A Ground Radar and a pair of Radar Goggles make a Ground Radar w/Goggles that keeps what the radar held: its batteries
 * ride in its item's data, which a plain shapeless recipe would drop (as the Portable Battery Pack's recipe does it).
 */
public final class GogglesUpgradeRecipe extends CustomRecipe {
    public static final GogglesUpgradeRecipe INSTANCE = new GogglesUpgradeRecipe();
    public static final MapCodec<GogglesUpgradeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, GogglesUpgradeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<GogglesUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    static ItemStack radar() {
        return Eln.findItemStack("Ground Radar");
    }

    static ItemStack goggles() {
        return Eln.findItemStack("Radar Goggles");
    }

    static ItemStack upgraded() {
        return Eln.findItemStack("Ground Radar w/Goggles");
    }

    static boolean same(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem()
            && mods.eln.misc.Utils.getItemObject(a) == mods.eln.misc.Utils.getItemObject(b);
    }

    /** The radar in a grid holding exactly one Ground Radar and one pair of Radar Goggles; else EMPTY. */
    static ItemStack radarIn(CraftingInput in) {
        ItemStack r = radar(), g = goggles(), found = ItemStack.EMPTY;
        int radars = 0, goggles = 0;
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            if (same(s, r)) {
                radars++;
                found = s;
            } else if (same(s, g)) goggles++;
            else return ItemStack.EMPTY;
        }
        return radars == 1 && goggles == 1 ? found : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !radarIn(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack r = radarIn(input), u = upgraded();
        if (r.isEmpty() || u.isEmpty()) return ItemStack.EMPTY;
        mods.eln.generic.ItemNbt.set(u, mods.eln.generic.ItemNbt.get(r));
        return u;
    }

    @Override
    public RecipeSerializer<GogglesUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }

    /** In the recipe book and JEI as the radar and the goggles it takes. */
    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public PlacementInfo placementInfo() {
        ItemStack r = radar(), g = goggles();
        if (r.isEmpty() || g.isEmpty()) return PlacementInfo.NOT_PLACEABLE;
        return PlacementInfo.create(List.of(Ingredient.of(r.getItem()), Ingredient.of(g.getItem())));
    }

    @Override
    public List<RecipeDisplay> display() {
        ItemStack r = radar(), g = goggles(), u = upgraded();
        if (r.isEmpty() || g.isEmpty() || u.isEmpty()) return List.of();
        return List.of(new ShapelessCraftingRecipeDisplay(List.of(slot(r), slot(g)), slot(u),
            new SlotDisplay.ItemSlotDisplay(net.minecraft.world.item.Items.CRAFTING_TABLE.builtInRegistryHolder())));
    }

    static SlotDisplay slot(ItemStack s) {
        return new SlotDisplay.ItemStackSlotDisplay(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(s));
    }
}
