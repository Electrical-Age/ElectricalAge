package mods.eln.api;

import java.lang.reflect.Field;

/**
 * Created by Gregory Maddra on 2016-11-16.
 */
public class Misc {

    public static Object getRecipeList(String list){
        try {
            Class<?> Eln = getEln();
            Object instanceObject = getElnInstance(Eln);
            Object recipeList = Eln.getDeclaredField(list).get(instanceObject);
            return recipeList;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Double getElectricalFurnaceProcessEnergyNeededPerSmelt(){
        try {
            Class<?> ElectricalFurnaceProcess = Class.forName("mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess");
            return ElectricalFurnaceProcess.getDeclaredField("energyNeededPerSmelt").getDouble(null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0D;
    }

    public static Class<?> getEln(){
        try {
            return Class.forName("mods.eln.Eln");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Object getElnInstance(Class<?> Eln){
        try {
            return Eln.getDeclaredField("instance").get(null);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
