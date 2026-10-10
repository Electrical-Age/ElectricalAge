package mods.eln.misc;

import mods.eln.Eln;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The 1.12 Utils, ported a piece at a time (P0: what the simulator needs). With -Deln.deterministic=true (the unit
 * tests), rand returns the midpoint, like the 1.12 test stub did.
 */
public class Utils {
    static final boolean DETERMINISTIC = Boolean.getBoolean("eln.deterministic");

    public static void println(Object str) {
        Eln.LOG.debug(String.valueOf(str));
    }

    public static void print(String format, Object... data) {
        Eln.LOG.debug(String.format(format, data));
    }

    public static void print(Object str) {
        Eln.LOG.debug(String.valueOf(str));
    }

    /** Uniform in [min, max). */
    public static double rand(double min, double max) {
        if (DETERMINISTIC) return (min + max) / 2;
        return min + ThreadLocalRandom.current().nextDouble() * (max - min);
    }
}
