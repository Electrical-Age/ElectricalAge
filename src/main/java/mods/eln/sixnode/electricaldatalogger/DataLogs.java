package mods.eln.sixnode.electricaldatalogger;

import mods.eln.gl.GL11;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A logger's graph as shown (board #45): up to {@link #POINTS} points per channel at the sampling period, each the
 * mean, minimum and maximum of the signal (0..1) over its period, in a ring; and each channel's scale and unit. The
 * client keeps one per logger, filled from the server's {@link LogStore}; a print keeps a compact copy.
 */
public class DataLogs {
    public static final int POINTS = 256;
    public static final byte voltageType = 0, currentType = 1, powerType = 2, celsiusType = 3, percentType = 4, energyType = 5;

    public float samplingPeriod = 0.5f;
    /** Logger.LINES, STACKED or LANES. */
    public byte mode;
    public LogChannel[] channels;
    /** [channel][slot] -> mean, min, max; NaN where nothing was recorded. head is the newest point's slot. */
    final float[][] mean, min, max;
    int head = -1, size;

    public DataLogs(int channels) {
        this.channels = new LogChannel[channels];
        for (int c = 0; c < channels; c++) this.channels[c] = new LogChannel();
        mean = new float[channels][POINTS];
        min = new float[channels][POINTS];
        max = new float[channels][POINTS];
    }

    /** The same graph settings for n channels (the points start empty). */
    public DataLogs resized(int n) {
        DataLogs d = new DataLogs(n);
        d.samplingPeriod = samplingPeriod;
        d.mode = mode;
        for (int c = 0; c < Math.min(n, channels.length); c++) d.channels[c] = channels[c];
        return d;
    }

    public int channelCount() {
        return channels.length;
    }

    public void reset() {
        head = -1;
        size = 0;
    }

    public int size() {
        return size;
    }

    /** A new newest point: [channel] -> {mean, min, max} or null. */
    public void add(float[][] point) {
        head = (head + 1) % POINTS;
        size = Math.min(POINTS, size + 1);
        for (int c = 0; c < channels.length; c++) {
            float[] p = c < point.length ? point[c] : null;
            mean[c][head] = p == null ? Float.NaN : p[0];
            min[c][head] = p == null ? Float.NaN : p[1];
            max[c][head] = p == null ? Float.NaN : p[2];
        }
    }

    /** Replaces the points with a view, newest first ([point][channel] -> {mean, min, max}). */
    public void set(float[][][] view) {
        reset();
        for (int j = Math.min(view.length, POINTS) - 1; j >= 0; j--) add(view[j]);
    }

    int slot(int age) {
        return Math.floorMod(head - age, POINTS);
    }

    /** Channel c's mean, age points back (0 = newest): a signal fraction, NaN if none. */
    public float mean(int c, int age) {
        return mean[c][slot(age)];
    }

    public float min(int c, int age) {
        return min[c][slot(age)];
    }

    public float max(int c, int age) {
        return max[c][slot(age)];
    }

    // ---- network: halves, NaN for a missing point

    public static void writePoint(DataOutputStream o, float[][] point, int channels) throws IOException {
        for (int c = 0; c < channels; c++)
            for (int k = 0; k < 3; k++) o.writeShort(Float.floatToFloat16(c < point.length && point[c] != null ? point[c][k] : Float.NaN));
    }

    public static float[][] readPoint(DataInputStream i, int channels) throws IOException {
        float[][] p = new float[channels][];
        for (int c = 0; c < channels; c++) {
            float[] v = {Float.float16ToFloat(i.readShort()), Float.float16ToFloat(i.readShort()), Float.float16ToFloat(i.readShort())};
            p[c] = Float.isNaN(v[0]) ? null : v;
        }
        return p;
    }

    /** All points, oldest first. */
    public void writeAll(DataOutputStream o) throws IOException {
        o.writeShort(size);
        for (int age = size - 1; age >= 0; age--) {
            float[][] p = new float[channels.length][];
            for (int c = 0; c < channels.length; c++) p[c] = new float[]{mean(c, age), min(c, age), max(c, age)};
            writePoint(o, p, channels.length);
        }
    }

    public void readAll(DataInputStream i) throws IOException {
        reset();
        int n = i.readShort();
        for (int j = 0; j < n; j++) add(readPoint(i, channels.length));
    }

    // ---- prints: a compact copy (halves), and the 1.12 byte log read back

    public void writeToNBT(CompoundTag nbt) {
        nbt.putFloat("samplingPeriod", samplingPeriod);
        nbt.putByte("channels", (byte) channels.length);
        for (int c = 0; c < channels.length; c++) {
            channels[c].writeToNBT(nbt, "c" + c);
            byte[] b = new byte[size * 6];
            for (int age = size - 1, i = 0; age >= 0; age--, i += 6) {
                put(b, i, mean(c, age));
                put(b, i + 2, min(c, age));
                put(b, i + 4, max(c, age));
            }
            nbt.putByteArray("c" + c + "points", b);
        }
    }

    static void put(byte[] b, int i, float v) {
        short h = Float.floatToFloat16(v);
        b[i] = (byte) (h >> 8);
        b[i + 1] = (byte) h;
    }

    static float get(byte[] b, int i) {
        return Float.float16ToFloat((short) ((b[i] & 0xFF) << 8 | b[i + 1] & 0xFF));
    }

    /** A print's graph; a print made before 2026-10-30 holds 1.12's byte log (newest first) and one scale. */
    public static DataLogs fromNBT(CompoundTag nbt) {
        if (nbt.contains("log")) {
            DataLogs d = new DataLogs(1);
            d.samplingPeriod = nbt.getFloatOr("samplingPeriod", 0.5f);
            d.channels[0] = new LogChannel(nbt.getFloatOr("minValue", 0f), nbt.getFloatOr("maxValue", 0f), LogChannel.oldUnit(nbt.getByteOr("unitType", (byte) 0)));
            byte[] log = nbt.getByteArray("log").orElse(new byte[0]);
            for (int j = Math.min(log.length, POINTS) - 1; j >= 0; j--) {
                float v = oldSample(log[j]);
                d.add(new float[][]{{v, v, v}});
            }
            return d;
        }
        int n = Math.max(1, Math.min(8, nbt.getByteOr("channels", (byte) 1)));
        DataLogs d = new DataLogs(n);
        d.samplingPeriod = nbt.getFloatOr("samplingPeriod", 0.5f);
        byte[][] pts = new byte[n][];
        int count = POINTS;
        for (int c = 0; c < n; c++) {
            d.channels[c].readFromNBT(nbt, "c" + c);
            pts[c] = nbt.getByteArray("c" + c + "points").orElse(new byte[0]);
            count = Math.min(count, pts[c].length / 6);
        }
        for (int j = 0; j < count; j++) {
            float[][] p = new float[n][];
            for (int c = 0; c < n; c++) p[c] = new float[]{get(pts[c], j * 6), get(pts[c], j * 6 + 2), get(pts[c], j * 6 + 4)};
            d.add(p);
        }
        return d;
    }

    /** A 1.12 log byte as a signal fraction (it stored normalized * 255.5 - 128). */
    public static float oldSample(byte b) {
        return ((int) b + 128) / 255f;
    }

    // ---- drawing (in the world and on prints)

    /**
     * The graph in [0, margeX] x [0, margeY], y down, in the current colour: each channel's range over a period as a
     * thin band, its mean as a line; the first axis's top, middle and bottom on the right; the time span below.
     */
    void draw(float margeX, float margeY, String textHeader) {
        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (size >= 2) {
            float dx = margeX / (size - 1);
            for (int c = 0; c < channels.length; c++) {
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                for (int age = 0; age < size; age++) {
                    float lo = min(c, age), hi = max(c, age);
                    if (Float.isNaN(lo)) continue;
                    float x = margeX - dx * age;
                    GL11.glVertex2f(x, margeY - hi * margeY - 0.01f);
                    GL11.glVertex2f(x, margeY - lo * margeY + 0.01f);
                }
                GL11.glEnd();
                GL11.glBegin(GL11.GL_LINE_STRIP);
                for (int age = 0; age < size; age++) {
                    float m = mean(c, age);
                    if (!Float.isNaN(m)) GL11.glVertex2f(margeX - dx * age, margeY - m * margeY);
                }
                GL11.glEnd();
            }
        }
        float temp = 0.01f;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        GL11.glVertex2f(margeX + temp, 0f);
        GL11.glVertex2f(margeX - temp, 0f);
        GL11.glVertex2f(margeX + temp, margeY + temp);
        GL11.glVertex2f(margeX - temp, margeY - temp);
        GL11.glVertex2f(0f, margeY + temp);
        GL11.glVertex2f(0f, margeY - temp);
        GL11.glEnd();
        LogChannel ch = channels[0];
        if (ch.min < 0 && ch.max > 0 || ch.min > 0 && ch.max < 0) {
            temp = 0.005f;
            float zeroY = ch.max / (ch.max - ch.min) * margeY;
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            GL11.glVertex2f(margeX, zeroY + temp);
            GL11.glVertex2f(margeX, zeroY - temp);
            GL11.glVertex2f(0f, zeroY + temp);
            GL11.glVertex2f(0f, zeroY - temp);
            GL11.glEnd();
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glPushMatrix();
        float scale = 0.01f;
        GL11.glScalef(scale, scale, 1f);
        UtilsClient.drawString(textHeader + " " + getYstring(1f, ch.max, ch.min, ch.unit), (int) (margeX / scale), 0, 0);
        UtilsClient.drawString(textHeader + " " + getYstring(0.5f, ch.max, ch.min, ch.unit), (int) (margeX / scale), (int) ((margeY / 2 - 0.05f) / scale), 0);
        UtilsClient.drawString(textHeader + " " + getYstring(0.0f, ch.max, ch.min, ch.unit), (int) (margeX / scale), (int) ((margeY - 0.08f) / scale), 0);
        UtilsClient.drawString(textHeader + Utils.plotTime(size * samplingPeriod), 0, (int) ((margeY + 0.03) / scale), 0);
        UtilsClient.drawString(textHeader + Utils.plotTime(0), (int) ((margeX - 0.05) / scale), (int) ((margeY + 0.03) / scale), 0);
        GL11.glPopMatrix();
    }

    public static void draw(CompoundTag nbt, float margeX, float margeY, String textHeader) {
        if (nbt == null) return;
        fromNBT(nbt).draw(margeX, margeY, textHeader);
    }

    /** The value factor (0..1) of the way from min to max, in the unit. */
    public static String getYstring(float factor, float maxValue, float minValue, String unit) {
        return format(factor * (maxValue - minValue) + minValue, unit);
    }

    /** Units that take SI prefixes (k, m, ...); others are written as plain numbers. */
    static final java.util.Set<String> SI = java.util.Set.of("V", "A", "W", "J", "Wh", "Hz", "N", "Pa", "F", "H", "s", "\u03A9", "ohm", "g");

    /** A value in a unit: three significant digits, an SI prefix for SI units. */
    public static String format(double v, String unit) {
        if (SI.contains(unit)) return Utils.plotValue(v, unit);
        double a = Math.abs(v);
        String n = a == 0 ? "0" : a >= 1e5 || a < 1e-3 ? String.format(java.util.Locale.ROOT, "%.2e", v)
            : new java.math.BigDecimal(v).round(new java.math.MathContext(3)).stripTrailingZeros().toPlainString();
        return unit.isEmpty() ? n : unit.equals("%") || unit.startsWith("\u00B0") ? n + unit : n + " " + unit;
    }

    public List<LogChannel> channelList() {
        return Arrays.asList(channels);
    }
}
