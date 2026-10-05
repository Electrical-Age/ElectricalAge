package net.minecraft.nbt;

import java.util.HashMap;
import java.util.Map;

/**
 * TEST STUB for the simulator test harness (tools/simtest.sh): a HashMap-backed stand-in for Minecraft's
 * NBTTagCompound with only the typed getters/setters the simulator uses. Like the real class, a missing
 * key reads as 0 / false. Floats are stored as float, so the float round-trip loss is preserved.
 * Not on the mod's classpath.
 */
public class NBTTagCompound {
    private final Map<String, Object> map = new HashMap<String, Object>();

    public void setDouble(String key, double v) { map.put(key, v); }

    public double getDouble(String key) {
        Object o = map.get(key);
        return o instanceof Number ? ((Number) o).doubleValue() : 0.0;
    }

    public void setFloat(String key, float v) { map.put(key, v); }

    public float getFloat(String key) {
        Object o = map.get(key);
        return o instanceof Number ? ((Number) o).floatValue() : 0.0f;
    }

    public void setBoolean(String key, boolean v) { map.put(key, v ? (byte) 1 : (byte) 0); }

    public boolean getBoolean(String key) {
        Object o = map.get(key);
        return o instanceof Number && ((Number) o).byteValue() != 0;
    }

    public boolean hasKey(String key) { return map.containsKey(key); }
}
