package mods.eln.api;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Utilities
 * @author Gregory Maddra
 * 2016-11-18
 */
public class Utilities {

    public static void makeModifiable(Field field){
        field.setAccessible(true);
        int modifiers = field.getModifiers();
        try {
            Field modifiersField = field.getClass().getDeclaredField("modifiers");
            modifiers = modifiers & ~Modifier.FINAL;
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, modifiers);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    public static boolean areSame(ItemStack stack, ItemStack output) {
        try {
            if (stack.getItem() == output.getItem() && stack.getItemDamage() == output.getItemDamage()) return true;
            int[] stackIds = OreDictionary.getOreIDs(stack);
            int[] outputIds = OreDictionary.getOreIDs(output);
            // System.out.println(Arrays.toString(stackIds) + "   " + Arrays.toString(outputIds));
            for (int i : outputIds) {
                for (int j : stackIds) {
                    if (i == j) return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
