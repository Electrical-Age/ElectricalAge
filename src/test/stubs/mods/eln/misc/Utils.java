package mods.eln.misc;

/**
 * TEST STUB for the simulator test harness (tools/simtest.sh). Replaces the real mods.eln.misc.Utils
 * (1,400 lines, Minecraft-heavy) with only the members the simulator closure calls:
 * println/print (logging; silent unless -Dsimtest.verbose=true) and rand (deterministic here).
 * Not on the mod's classpath.
 */
public class Utils {
    static final boolean VERBOSE = Boolean.getBoolean("simtest.verbose");

    public static void println(Object str) {
        if (VERBOSE) System.out.println(str);
    }

    public static void print(String format, Object... data) {
        if (VERBOSE) System.out.print(String.format(format, data));
    }

    /** Real one is uniform random in [min, max); tests want determinism, so return the midpoint. */
    public static double rand(double min, double max) {
        return (min + max) / 2;
    }
}
