package mods.eln.oil;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

/**
 * A recipe ingredient (eln:empty_tank) that takes these tank items only when they carry no contents (TankContents): a
 * filled tank crafted into an Alloy Tank or a machine lost what it held. Recipes name tanks through it (ElnRecipesBase).
 */
public record EmptyTankIngredient(HolderSet<Item> itemSet) implements ICustomIngredient {
    public static final MapCodec<EmptyTankIngredient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        RegistryCodecs.holderSet(Registries.ITEM).fieldOf("items").forGetter(EmptyTankIngredient::itemSet)
    ).apply(i, EmptyTankIngredient::new));

    @Override
    public boolean test(ItemStack stack) {
        return itemSet.contains(stack.typeHolder()) && !stack.has(mods.eln.registry.ElnRegistry.TANK_CONTENTS.get());
    }

    @Override
    public Stream<Holder<Item>> items() {
        return itemSet.stream();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return mods.eln.registry.ElnRegistry.EMPTY_TANK_INGREDIENT.get();
    }

    /** The ingredient: item, empty. */
    public static net.minecraft.world.item.crafting.Ingredient of(Item item) {
        return new EmptyTankIngredient(HolderSet.direct(item.builtInRegistryHolder())).toVanilla();
    }
}
