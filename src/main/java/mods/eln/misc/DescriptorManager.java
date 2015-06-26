package mods.eln.misc;

import java.util.HashMap;

public class DescriptorManager {

    static HashMap<Object, Object> map = new HashMap<Object, Object>();

    public static void put(Object key, Object value) {
        map.put(key, value);
    }

    public static <T> T get(Object key) {
        if (key == null) return null;
        return (T) map.get(key);
    }
}
