package mods.eln.api;

import java.lang.reflect.Field;

/**
 * Created by Gregory Maddra on 2016-11-16.
 */
public class Misc {

    public static Object getRecipeList(String list){
        try {
            Class<?> Eln = Class.forName("mods.eln.Eln");
            Field Instance = Eln.getDeclaredField("instance");
            Object instanceObject = Instance.get(null);
            Object recipeList = Eln.getDeclaredField(list).get(instanceObject);
            return recipeList;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
