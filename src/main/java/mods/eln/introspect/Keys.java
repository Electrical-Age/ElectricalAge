package mods.eln.introspect;

/**
 * Shared key names, so a device, the machine API and a test spell a quantity the same way. Keys say what a value is,
 * never its unit (the unit travels with the number). A key a test uses is a contract: rename it only with a note in the
 * commit, as with NBT keys.
 */
public final class Keys {
    private Keys() {
    }

    // id
    public static final String KIND = "kind", NODE = "node", POS = "pos", FACE = "face", SLOT = "slot", ELEMENT = "element";
    /** True on a report no client has a half of (a port node, a voxel part): nothing to compare. */
    public static final String SERVER_ONLY = "server_only";

    // spec
    public static final String NOMINAL_VOLTAGE = "nominal_voltage", RATED_POWER = "rated_power", IDLE_POWER = "idle_power",
        MIN_VOLTAGE_RATIO = "min_voltage_ratio", MAX_SPEED = "max_speed", ACCELERATION = "acceleration", EFFICIENCY = "efficiency", TRAVEL_MIN = "travel_min", TRAVEL_MAX = "travel_max";

    // state
    public static final String STATUS = "status", VOLTAGE = "voltage", VOLTAGE_RATIO = "voltage_ratio", CURRENT = "current",
        POWER = "power", ENERGY = "energy", BUFFER_ENERGY = "buffer_energy", LIFETIME_ENERGY = "lifetime_energy",
        TEMPERATURE = "temperature", ENABLED = "enabled", TARGET = "target", SPEED = "speed", LOAD = "load";

    // sync (both halves)
    public static final String POSITION = "position", VELOCITY = "velocity", SEGMENT_START = "segment_start", ELAPSED = "elapsed";

    // electrical / thermal entries
    public static final String U = "u", I = "i", T = "t", P = "p";

    /** True for a valid key: a lower-case letter, then lower-case letters, digits and underscores. */
    public static boolean valid(String key) {
        if (key == null || key.isEmpty() || key.length() > 64) return false;
        char c0 = key.charAt(0);
        if (c0 < 'a' || c0 > 'z') return false;
        for (int i = 1; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_')) return false;
        }
        return true;
    }
}
