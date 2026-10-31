package mods.eln.item.electricalitem;

import com.mojang.serialization.MapCodec;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
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
 * Three portable batteries make a portable battery pack that holds their charge (Baughn: a pack of three full batteries
 * came out empty; the plain shapeless recipe dropped it). The pack stores exactly three batteries' worth.
 */
public final class BatteryPackRecipe extends CustomRecipe {
    public static final BatteryPackRecipe INSTANCE = new BatteryPackRecipe();
    public static final MapCodec<BatteryPackRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, BatteryPackRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<BatteryPackRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    static final int BATTERIES = 3;

    static ItemStack battery() {
        return Eln.findItemStack("Portable Battery");
    }

    static ItemStack pack() {
        return Eln.findItemStack("Portable Battery Pack");
    }

    static BatteryItem descriptor(ItemStack s) {
        return s.isEmpty() ? null : GenericItemUsingDamageDescriptor.getDescriptor(s) instanceof BatteryItem b ? b : null;
    }

    /** The batteries' summed energy (J) when the grid holds exactly three portable batteries and nothing else; else -1. */
    public static double energy(CraftingInput in) {
        ItemStack b = battery();
        if (b.isEmpty()) return -1;
        int n = 0;
        double e = 0;
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() != b.getItem()) return -1;
            BatteryItem d = descriptor(s);
            if (d == null) return -1;
            n++;
            e += d.getEnergy(s);
        }
        return n == BATTERIES ? e : -1;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return energy(input) >= 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        double e = energy(input);
        ItemStack p = pack();
        if (e < 0 || p.isEmpty()) return ItemStack.EMPTY;
        BatteryItem d = descriptor(p);
        if (d != null) d.setEnergy(p, Math.min(e, d.getEnergyMax(p)));
        return p;
    }

    @Override
    public RecipeSerializer<BatteryPackRecipe> getSerializer() {
        return SERIALIZER;
    }

    /** In the recipe book and JEI as the three batteries it takes (unlocked by holding one: the recipe advancement). */
    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public PlacementInfo placementInfo() {
        ItemStack b = battery();
        if (b.isEmpty()) return PlacementInfo.NOT_PLACEABLE;
        Ingredient ing = Ingredient.of(b.getItem());
        return PlacementInfo.create(List.of(ing, ing, ing));
    }

    @Override
    public List<RecipeDisplay> display() {
        ItemStack b = battery(), p = pack();
        if (b.isEmpty() || p.isEmpty()) return List.of();
        SlotDisplay s = slot(b);
        return List.of(new ShapelessCraftingRecipeDisplay(List.of(s, s, s), slot(p),
            new SlotDisplay.ItemSlotDisplay(net.minecraft.world.item.Items.CRAFTING_TABLE.builtInRegistryHolder())));
    }

    static SlotDisplay slot(ItemStack s) {
        return new SlotDisplay.ItemStackSlotDisplay(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(s));
    }
}
