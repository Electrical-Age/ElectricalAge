package mods.eln.network;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Bounds for what a client sends in a GUI packet (a device's networkUnserialize). ServerPacketHandler checks who sent
 * a packet and from where; the values in it are the device's to check, because a modified client sends what no
 * screen would: a NaN, a slider value past its ends, an index past a list, a name of 60 000 characters. Stored as they
 * come, they are saved and used every tick (an FE exporter's NaN power factor threw in each simulator tick and again
 * after a restart). Handlers read through these:
 * <ul>
 * <li>a number that is not finite changes nothing (the current value stays);</li>
 * <li>a number from a slider is clamped to the slider's ends; a number typed in a text box is taken if finite;</li>
 * <li>an index outside its list changes nothing;</li>
 * <li>a string is cut to what a screen's text box holds ({@link #MAX_STRING}).</li>
 * </ul>
 * A value read back from NBT goes through {@link #load} where an old handler may have saved a bad one.
 */
public final class ClientValues {
    private ClientValues() {
    }

    /** The longest text a screen's text box sends (EaField's limit). */
    public static final int MAX_STRING = 64;

    /** v clamped to min..max; if v is not finite, current (the packet changes nothing). */
    public static double clamp(double v, double min, double max, double current) {
        if (!Double.isFinite(v)) return current;
        return v < min ? min : Math.min(v, max);
    }

    /** v clamped to min..max; if v is not finite, current (the packet changes nothing). */
    public static float clamp(float v, float min, float max, float current) {
        if (!Float.isFinite(v)) return current;
        return v < min ? min : Math.min(v, max);
    }

    /** v if it is finite (a number typed in a text box), else current. */
    public static double finite(double v, double current) {
        return Double.isFinite(v) ? v : current;
    }

    /** v if it is finite (a number typed in a text box), else current. */
    public static float finite(float v, float current) {
        return Float.isFinite(v) ? v : current;
    }

    /** Whether i indexes a list or an array of this size. */
    public static boolean index(int i, int size) {
        return i >= 0 && i < size;
    }

    /** The packet's next string, cut to {@link #MAX_STRING} characters. */
    public static String readString(DataInputStream stream) throws IOException {
        return cut(stream.readUTF(), MAX_STRING);
    }

    /** s cut to at most max characters (not inside a surrogate pair). */
    public static String cut(String s, int max) {
        if (s.length() <= max) return s;
        int end = Character.isHighSurrogate(s.charAt(max - 1)) ? max - 1 : max;
        return s.substring(0, end);
    }

    /** A saved value: clamped to min..max, or fallback if it is not finite (a save an older handler let a bad value into). */
    public static double load(double v, double min, double max, double fallback) {
        return clamp(v, min, max, fallback);
    }
}
