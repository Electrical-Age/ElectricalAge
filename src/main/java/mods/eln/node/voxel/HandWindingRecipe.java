package mods.eln.node.voxel;

import com.mojang.serialization.MapCodec;
import mods.eln.misc.OreDictTags;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Windings wound by hand (Baughn 2026-11-08: a DC-DC converter before the wire-drawing machine):
 * <ul>
 * <li>Copper Cables, each with an insulator (rubber or leather), make a winding of TURNS_PER_CABLE turns a cable, of
 * 1.5 mm² wire insulated for 230 V (Baughn: 1.5 mm² only, up to 230 V);</li>
 * <li>windings of the same wire combine into one, up to MAX_TURNS turns.</li>
 * </ul>
 * Thicker wire, better insulation and longer windings take the wire-drawing machine.
 */
public final class HandWindingRecipe extends CustomRecipe {
    public static final HandWindingRecipe INSTANCE = new HandWindingRecipe();
    public static final MapCodec<HandWindingRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, HandWindingRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<HandWindingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /** Turns a Copper Cable makes (a winding's turn is WindingItem.BLOCKS_PER_TURN of a block of wire). */
    public static final int TURNS_PER_CABLE = (int) Math.round(1 / WindingItem.BLOCKS_PER_TURN);
    /** The most turns a winding may have by hand. */
    public static final int MAX_TURNS = 64;
    /** The hand winding's wire: 1.5 mm² (CableSpec.MM2 index), insulated for 230 V (power tier 2), black. */
    public static final int MM2 = 3, TIER = 2;

    public static CableSpec spec() {
        return new CableSpec(MM2, CableSpec.preset(TIER).colour());
    }

    static boolean copperCable(ItemStack s) {
        ItemStack c = mods.eln.Eln.findItemStack("Copper Cable", 1);
        return !c.isEmpty() && ItemStack.isSameItemSameComponents(c, s.copyWithCount(1));
    }

    static boolean insulator(ItemStack s) {
        return s.is(Items.LEATHER) || s.is(OreDictTags.tag("itemRubber"));
    }

    /** The winding the grid makes, or EMPTY. */
    public static ItemStack result(CraftingInput in) {
        int cables = 0, insulators = 0, windings = 0, turns = 0;
        ItemStack first = ItemStack.EMPTY;
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            if (copperCable(s)) cables++;
            else if (insulator(s)) insulators++;
            else if (WindingItem.is(s)) {
                if (first.isEmpty()) first = s;
                else if (WindingItem.tier(s) != WindingItem.tier(first) || !WindingItem.spec(s).equals(WindingItem.spec(first))) return ItemStack.EMPTY;
                windings++;
                turns += WindingItem.turns(s);
            } else return ItemStack.EMPTY;
        }
        if (windings == 0) {
            // copper and insulation, one for one
            if (cables == 0 || cables != insulators || cables * TURNS_PER_CABLE > MAX_TURNS) return ItemStack.EMPTY;
            return WindingItem.make(TIER, spec(), cables * TURNS_PER_CABLE);
        }
        // windings of one wire put together (more wire to them, too: copper and insulation one for one)
        if (cables != insulators || windings + cables < 2) return ItemStack.EMPTY;
        if (cables > 0 && (WindingItem.tier(first) != TIER || !WindingItem.spec(first).equals(spec()))) return ItemStack.EMPTY;
        turns += cables * TURNS_PER_CABLE;
        if (turns > MAX_TURNS) return ItemStack.EMPTY;
        return WindingItem.make(WindingItem.tier(first), WindingItem.spec(first), turns);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !result(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return result(input);
    }

    @Override
    public RecipeSerializer<HandWindingRecipe> getSerializer() {
        return SERIALIZER;
    }

    /** In the recipe book and JEI as a Copper Cable and an insulator (unlocked by holding a Copper Cable). */
    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public PlacementInfo placementInfo() {
        ItemStack c = mods.eln.Eln.findItemStack("Copper Cable", 1);
        if (c.isEmpty()) return PlacementInfo.NOT_PLACEABLE;
        return PlacementInfo.create(List.of(net.minecraft.world.item.crafting.Ingredient.of(c.getItem()),
            net.minecraft.world.item.crafting.Ingredient.of(Items.LEATHER)));
    }

    @Override
    public List<RecipeDisplay> display() {
        ItemStack c = mods.eln.Eln.findItemStack("Copper Cable", 1);
        if (c.isEmpty()) return List.of();
        ItemStack out = WindingItem.make(TIER, spec(), TURNS_PER_CABLE);
        var table = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE.builtInRegistryHolder());
        return List.of(
            new ShapelessCraftingRecipeDisplay(List.of(slot(c), slot(new ItemStack(Items.LEATHER))), slot(out), table),
            new ShapelessCraftingRecipeDisplay(List.of(slot(c), slot(new ItemStack(mods.eln.Eln.itemsByName.get("Rubber")))), slot(out), table));
    }

    static SlotDisplay slot(ItemStack s) {
        return new SlotDisplay.ItemStackSlotDisplay(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(s));
    }
}
