package mods.eln.sixnode.electricaldatalogger;

import net.minecraft.nbt.CompoundTag;

/**
 * A logger's history (board #45): what its channels read, kept so that the sampling period can change without losing
 * anything. Values are signal fractions (0..1 of the signal voltage), kept as half-floats; the scale and unit a channel
 * is shown in are applied when drawn, so changing them changes nothing here.
 * <p>
 * Recording runs at a fixed base, one value a tick (0.05 s), whatever the period. Tier 0 keeps the last
 * {@link #BASE_SIZE} ticks; tier k (1..{@link #TIERS}-1) keeps the last {@link #TIER_SIZE} buckets of 4^k ticks, each
 * with its minimum, maximum and mean, so a spike survives decimation. Each tier fills from the one below as the tick
 * count crosses a bucket boundary; bucket i of tier k always covers ticks [i * 4^k, (i + 1) * 4^k). All are true rings.
 * <p>
 * A view at a period of p ticks has points covering [E - (j + 1) p, E - j p), E being the tick of the last emitted
 * point. Each point is read from the finest tier that still holds that time: tier 0 owns the newest stretch, each
 * coarser tier the stretch before (aligned to its bucket size, so nothing counts twice). Means are weighted by
 * overlap; a coarse bucket lends its minimum and maximum to a point only if at least half of it, or of the point, lies
 * inside, so an old spike shows in one point, not smeared over its neighbours. Changing the period only changes the
 * view: 0.5 s, then 5 s, then 0.5 s again shows the last 51 s tick by tick as before.
 */
public final class LogStore {
    public static final double TICK = 0.05;
    public static final int RATIO = 4, TIERS = 6, BASE_SIZE = 1024, TIER_SIZE = 512;
    /** The oldest tick any tier can hold: what a migration replays at most. */
    public static final long SPAN = (long) TIER_SIZE << (2 * (TIERS - 1));

    final int channels;
    /** [channel][tier] -> mean, min, max halves (tier 0: mean only; min and max are the same array). */
    final short[][][] mean, min, max;
    long ticks;
    /** Ticks per view point, and the tick of the last point emitted. */
    int period = 10;
    long emitted;
    /** The tick being recorded: seconds so far, and value * seconds per channel (not saved: under a tick). */
    double pending;
    final double[] acc;

    public LogStore(int channels) {
        this.channels = channels;
        mean = new short[channels][TIERS][];
        min = new short[channels][TIERS][];
        max = new short[channels][TIERS][];
        acc = new double[channels];
        for (int c = 0; c < channels; c++)
            for (int k = 0; k < TIERS; k++) {
                int n = capacity(k);
                mean[c][k] = new short[n];
                min[c][k] = k == 0 ? mean[c][k] : new short[n];
                max[c][k] = k == 0 ? mean[c][k] : new short[n];
            }
    }

    static int capacity(int tier) {
        return tier == 0 ? BASE_SIZE : TIER_SIZE;
    }

    static long bucket(int tier) {
        return 1L << (2 * tier);
    }

    static short half(float v) {
        return Float.floatToFloat16(v);
    }

    static float real(short h) {
        return Float.float16ToFloat(h);
    }

    public int channels() {
        return channels;
    }

    public long ticks() {
        return ticks;
    }

    public int period() {
        return period;
    }

    public float periodSeconds() {
        return (float) (period * TICK);
    }

    /** A new period in seconds (at least a tick): the view starts afresh from now; nothing is lost. */
    public void setPeriod(double seconds) {
        period = periodTicks(seconds);
        emitted = ticks;
    }

    public static int periodTicks(double seconds) {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, Math.round(seconds / TICK)));
    }

    public void clear() {
        ticks = 0;
        emitted = 0;
        pending = 0;
        java.util.Arrays.fill(acc, 0);
    }

    /**
     * Records the channels' values held for dt seconds. Returns true when a view point became due (call {@link #emit}).
     */
    public boolean record(float[] values, double dt) {
        boolean due = false;
        while (dt > 1e-12) {
            double step = Math.min(dt, TICK - pending);
            for (int c = 0; c < channels; c++) acc[c] += values[c] * step;
            pending += step;
            dt -= step;
            if (pending >= TICK - 1e-9) {
                float[] v = new float[channels];
                for (int c = 0; c < channels; c++) v[c] = (float) (acc[c] / pending);
                pending = 0;
                java.util.Arrays.fill(acc, 0);
                due |= tick(v);
            }
        }
        return due;
    }

    /** One tick's values; fills the coarser tiers at their boundaries. True when a view point is due. */
    public boolean tick(float[] values) {
        int i0 = (int) (ticks % BASE_SIZE);
        for (int c = 0; c < channels; c++) mean[c][0][i0] = half(values[c]);
        ticks++;
        for (int k = 1; k < TIERS && ticks % bucket(k) == 0; k++) {
            long below = ticks / bucket(k - 1); // buckets complete in tier k - 1
            int at = (int) ((ticks / bucket(k) - 1) % TIER_SIZE), n0 = capacity(k - 1);
            for (int c = 0; c < channels; c++) {
                float lo = Float.POSITIVE_INFINITY, hi = Float.NEGATIVE_INFINITY, sum = 0;
                for (long i = below - RATIO; i < below; i++) {
                    int j = (int) (i % n0);
                    lo = Math.min(lo, real(min[c][k - 1][j]));
                    hi = Math.max(hi, real(max[c][k - 1][j]));
                    sum += real(mean[c][k - 1][j]);
                }
                mean[c][k][at] = half(sum / RATIO);
                min[c][k][at] = half(lo);
                max[c][k][at] = half(hi);
            }
        }
        return ticks - emitted >= period;
    }

    /** Marks the newest view point as emitted and returns it: [channel] -> {mean, min, max}. */
    public float[][] emit() {
        emitted = ticks - (ticks - emitted) % period;
        float[][] p = new float[channels][];
        for (int c = 0; c < channels; c++) p[c] = aggregate(c, emitted - period, emitted);
        return p;
    }

    /** Channel c over ticks [a, b): {mean, min, max}, or null where nothing was recorded. */
    public float[] aggregate(int c, long a, long b) {
        a = Math.max(a, 0);
        b = Math.min(b, ticks);
        if (b <= a) return null;
        double sum = 0, weight = 0;
        float lo = Float.POSITIVE_INFINITY, hi = Float.NEGATIVE_INFINITY;
        long top = ticks;
        for (int k = 0; k < TIERS && top > a; k++) {
            long s = bucket(k), complete = ticks / s;
            long oldest = Math.max(0, complete - capacity(k)) * s;
            long from = k == TIERS - 1 || oldest == 0 ? oldest : ceil(oldest, s * RATIO);
            long x0 = Math.max(a, from), x1 = Math.min(b, Math.min(top, complete * s));
            for (long i = Math.floorDiv(x0, s); i * s < x1; i++) {
                long overlap = Math.min(x1, (i + 1) * s) - Math.max(x0, i * s);
                if (overlap <= 0) continue;
                int j = (int) (i % capacity(k));
                sum += real(mean[c][k][j]) * overlap;
                weight += overlap;
                if (overlap * 2 >= Math.min(s, b - a)) {
                    lo = Math.min(lo, real(min[c][k][j]));
                    hi = Math.max(hi, real(max[c][k][j]));
                }
            }
            top = from;
        }
        if (weight == 0) return null;
        float m = (float) (sum / weight);
        return new float[]{m, Math.min(lo, m), Math.max(hi, m)};
    }

    static long ceil(long x, long step) {
        return Math.floorDiv(x + step - 1, step) * step;
    }

    /** The view: up to n points, newest first, ending at the last emitted point; [point][channel] -> {mean, min, max}. */
    public float[][][] view(int n) {
        int count = (int) Math.min(n, (emitted + period - 1) / period);
        float[][][] v = new float[count][channels][];
        for (int j = 0; j < count; j++)
            for (int c = 0; c < channels; c++) v[j][c] = aggregate(c, emitted - (long) (j + 1) * period, emitted - (long) j * period);
        return v;
    }

    /**
     * Replays a history kept as values per sample at a period (oldest first), as if recorded: what older saves
     * kept. Only the last {@link #SPAN} ticks can be held, so older samples are skipped.
     */
    public void replay(float[][] samples, int periodTicks) {
        long total = (long) samples.length * periodTicks, skip = Math.max(0, total - SPAN);
        for (int s = 0; s < samples.length; s++)
            for (int t = 0; t < periodTicks; t++)
                if ((long) s * periodTicks + t >= skip) tick(samples[s]);
        emitted = ticks;
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putLong("ticks", ticks);
        nbt.putInt("period", period);
        nbt.putLong("emitted", emitted);
        for (int c = 0; c < channels; c++)
            for (int k = 0; k < TIERS; k++) {
                nbt.putByteArray("c" + c + "t" + k + "mean", pack(mean[c][k], k));
                if (k > 0) {
                    nbt.putByteArray("c" + c + "t" + k + "min", pack(min[c][k], k));
                    nbt.putByteArray("c" + c + "t" + k + "max", pack(max[c][k], k));
                }
            }
    }

    public void readFromNBT(CompoundTag nbt) {
        clear();
        ticks = Math.max(0, nbt.getLongOr("ticks", 0L));
        period = Math.max(1, nbt.getIntOr("period", 10));
        emitted = Math.min(ticks, Math.max(0, nbt.getLongOr("emitted", ticks)));
        for (int c = 0; c < channels; c++)
            for (int k = 0; k < TIERS; k++) {
                unpack(nbt.getByteArray("c" + c + "t" + k + "mean").orElse(new byte[0]), mean[c][k], k);
                if (k > 0) {
                    unpack(nbt.getByteArray("c" + c + "t" + k + "min").orElse(new byte[0]), min[c][k], k);
                    unpack(nbt.getByteArray("c" + c + "t" + k + "max").orElse(new byte[0]), max[c][k], k);
                }
            }
    }

    /** The tier's held buckets, oldest first, two bytes each. */
    byte[] pack(short[] ring, int k) {
        long complete = ticks / bucket(k), n = Math.min(complete, ring.length);
        byte[] out = new byte[(int) n * 2];
        for (int i = 0; i < n; i++) {
            short h = ring[(int) ((complete - n + i) % ring.length)];
            out[2 * i] = (byte) (h >> 8);
            out[2 * i + 1] = (byte) h;
        }
        return out;
    }

    void unpack(byte[] in, short[] ring, int k) {
        long complete = ticks / bucket(k);
        int n = (int) Math.min(Math.min(complete, ring.length), in.length / 2);
        for (int i = 0; i < n; i++)
            ring[(int) ((complete - n + i) % ring.length)] = (short) ((in[2 * i] & 0xFF) << 8 | in[2 * i + 1] & 0xFF);
    }
}
