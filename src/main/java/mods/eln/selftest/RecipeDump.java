package mods.eln.selftest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mods.eln.Eln;
import mods.eln.misc.Recipe;
import mods.eln.misc.RecipesList;
import mods.eln.registry.ElnRecipes;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * `/eln selftest recipes`: every EA recipe as JSON (crafting, smelting, the four machine lists), with ingredients as the
 * stacks they match. Port step T4 (notes/port26/PLAN.md): golden data to diff the 26.x recipes against, and the seed
 * for its data-driven recipe files. Written to &lt;server dir&gt;/eln-recipes.json.
 */
public final class RecipeDump {
    private RecipeDump() {
    }

    public static void command(ICommandSender sender) {
        File out = new File("eln-recipes.json");
        try (Writer w = new FileWriter(out)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(dump(), w);
            sender.sendMessage(new TextComponentString("eln recipes written to " + out.getAbsolutePath()));
        } catch (IOException e) {
            sender.sendMessage(new TextComponentString("eln recipes: " + e));
        }
    }

    static JsonObject dump() {
        JsonObject root = new JsonObject();
        JsonArray crafting = new JsonArray();
        for (IRecipe r : ForgeRegistries.RECIPES.getValuesCollection()) {
            if (r.getRegistryName() == null || !Eln.MODID.equals(r.getRegistryName().getNamespace())) continue;
            JsonObject o = new JsonObject();
            o.addProperty("id", r.getRegistryName().toString());
            o.addProperty("class", r.getClass().getSimpleName());
            o.add("output", stack(r.getRecipeOutput()));
            if (r instanceof IShapedRecipe) {
                o.addProperty("width", ((IShapedRecipe) r).getRecipeWidth());
                o.addProperty("height", ((IShapedRecipe) r).getRecipeHeight());
            }
            JsonArray ing = new JsonArray();
            for (Ingredient i : r.getIngredients()) ing.add(ingredient(i));
            o.add("ingredients", ing);
            crafting.add(o);
        }
        root.add("crafting", crafting);

        JsonArray smelting = new JsonArray();
        for (ItemStack[] s : ElnRecipes.smeltingCalls) {
            JsonObject o = new JsonObject();
            o.add("input", stack(s[0]));
            o.add("output", stack(s[1]));
            smelting.add(o);
        }
        root.add("smelting", smelting);

        root.add("macerator", list(Eln.maceratorRecipes));
        root.add("compressor", list(Eln.compressorRecipes));
        root.add("plateMachine", list(Eln.plateMachineRecipes));
        root.add("magnetizer", list(Eln.magnetiserRecipes));
        return root;
    }

    static JsonArray list(RecipesList l) {
        JsonArray a = new JsonArray();
        for (Recipe r : l.getRecipes()) {
            JsonObject o = new JsonObject();
            o.add("input", stack(r.input));
            JsonArray out = new JsonArray();
            for (ItemStack s : r.output) out.add(stack(s));
            o.add("output", out);
            o.addProperty("energy", r.energy);
            a.add(o);
        }
        return a;
    }

    static JsonArray ingredient(Ingredient i) {
        JsonArray a = new JsonArray();
        for (ItemStack s : i.getMatchingStacks()) a.add(stack(s));
        return a;
    }

    static JsonObject stack(ItemStack s) {
        JsonObject o = new JsonObject();
        if (s == null || s.isEmpty()) {
            o.addProperty("item", "minecraft:air");
            return o;
        }
        o.addProperty("item", String.valueOf(s.getItem().getRegistryName()));
        o.addProperty("damage", s.getItemDamage());
        if (s.getCount() != 1) o.addProperty("count", s.getCount());
        String name;
        try {
            name = s.getDisplayName();
        } catch (RuntimeException e) {
            name = "?";
        }
        o.addProperty("name", name);
        return o;
    }
}
