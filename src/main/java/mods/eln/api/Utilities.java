package mods.eln.api;

import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Created by Gregory Maddra on 2016-11-18.
 */
public class Utilities {

    public static boolean areSame(ItemStack input, ItemStack output){
        try {
            Class<?> Utils = Class.forName("mods.eln.misc.Utils");
            return (Boolean) Utils.getDeclaredMethod("areSame", ItemStack.class, ItemStack.class).invoke(null, input, output);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void makeModifiable(Field field){
        field.setAccessible(true);
        int modifiers = field.getModifiers();
        try {
            Field modifiersField = field.getClass().getDeclaredField("modifiers");
            modifiers = modifiers & ~Modifier.FINAL;
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, modifiers);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }
}
