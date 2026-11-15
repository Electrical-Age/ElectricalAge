package mods.eln.gl;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The frames of one measured view of the render benchmark's client tier and what they add up to (the arithmetic
 * of mods.eln.client.RenderBenchClient, kept apart from the client so that it can be unit-tested).
 */
public final class BenchRun {
    /**
     * Where a measurement is taken. here = where the player stands and looks when its turn comes (no teleport);
     * spin = degrees a second the view turns about the vertical while measured (0 = still).
     */
    public record View(String name, boolean here, double x, double y, double z, float yaw, float pitch, float spin) {
    }

    /**
     * Views from "name=x,y,z,yaw,pitch[,spin];name=here[,spin];...". The name and its '=' may be left out (view1,
     * view2...). Nothing or blank = one view, here. Throws IllegalArgumentException on anything else.
     */
    public static List<View> parseViews(String spec) {
        List<View> out = new ArrayList<>();
        if (spec != null)
            for (String part : spec.split(";")) {
                part = part.trim();
                if (part.isEmpty()) continue;
                String name = "view" + (out.size() + 1);
                int eq = part.indexOf('=');
                if (eq >= 0) {
                    name = part.substring(0, eq).trim();
                    part = part.substring(eq + 1).trim();
                }
                String[] f = part.split(",");
                try {
                    if (f[0].trim().equals("here")) {
                        if (f.length > 2) throw new IllegalArgumentException("too many fields");
                        out.add(new View(name, true, 0, 0, 0, 0, 0, f.length == 2 ? Float.parseFloat(f[1].trim()) : 0));
                    } else {
                        if (f.length != 5 && f.length != 6) throw new IllegalArgumentException("want x,y,z,yaw,pitch[,spin]");
                        out.add(new View(name, false, Double.parseDouble(f[0].trim()), Double.parseDouble(f[1].trim()), Double.parseDouble(f[2].trim()),
                            Float.parseFloat(f[3].trim()), Float.parseFloat(f[4].trim()), f.length == 6 ? Float.parseFloat(f[5].trim()) : 0));
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("view '" + part + "': " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("view '" + part + "': " + e.getMessage());
                }
            }
        if (out.isEmpty()) out.add(new View("here", true, 0, 0, 0, 0, 0, 0));
        return out;
    }

    /** The command that puts the player at the view (without the slash). */
    public static String teleport(View v) {
        return String.format(Locale.ROOT, "tp @s %.3f %.3f %.3f %.2f %.2f", v.x(), v.y(), v.z(), v.yaw(), v.pitch());
    }

    private long[] frameNs = new long[4096], eaNs = new long[4096];
    private int n;
    private final long[] sums = new long[RenderProbe.N];
    private long totalNs;

    /** One rendered frame: its length, and the {@link RenderProbe} counters of it. */
    public void frame(long ns, long[] counters) {
        if (n == frameNs.length) {
            frameNs = Arrays.copyOf(frameNs, n * 2);
            eaNs = Arrays.copyOf(eaNs, n * 2);
        }
        long ea = 0;
        for (int i : RenderProbe.FRAME_NS) ea += counters[i];
        frameNs[n] = ns;
        eaNs[n] = ea;
        n++;
        totalNs += ns;
        for (int i = 0; i < RenderProbe.N; i++) sums[i] += counters[i];
    }

    public int frames() {
        return n;
    }

    private static JsonObject ms(FrameStats.Summary s) {
        JsonObject o = new JsonObject();
        o.addProperty("mean", round(s.mean() / 1e6));
        o.addProperty("p50", round(s.p50() / 1e6));
        o.addProperty("p95", round(s.p95() / 1e6));
        o.addProperty("p99", round(s.p99() / 1e6));
        o.addProperty("max", round(s.max() / 1e6));
        o.addProperty("min", round(s.min() / 1e6));
        return o;
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    /**
     * frames, seconds, fps (frames over seconds), frameMs and eaMs (mean, p50, p95, p99, max, min of a frame's
     * length and of EA's render-thread time in it), eaShare (EA's time over all time), perFrame (each counter's mean
     * a frame; times in ns) and perSecond (the chunk workers' meshing and the settling, which do not come by the frame).
     */
    public JsonObject json() {
        JsonObject o = new JsonObject();
        double seconds = totalNs / 1e9;
        o.addProperty("frames", n);
        o.addProperty("seconds", round(seconds));
        o.addProperty("fps", seconds == 0 ? 0 : round(n / seconds));
        o.add("frameMs", ms(FrameStats.summary(frameNs, n)));
        o.add("eaMs", ms(FrameStats.summary(eaNs, n)));
        long ea = 0;
        for (int i : RenderProbe.FRAME_NS) ea += sums[i];
        o.addProperty("eaShare", totalNs == 0 ? 0 : round((double) ea / totalNs));
        JsonObject per = new JsonObject(), sec = new JsonObject();
        for (int i = 0; i < RenderProbe.SETTLES; i++) per.addProperty(RenderProbe.NAMES[i], n == 0 ? 0 : round((double) sums[i] / n));
        for (int i = RenderProbe.SETTLES; i < RenderProbe.N; i++) sec.addProperty(RenderProbe.NAMES[i], seconds == 0 ? 0 : round(sums[i] / seconds));
        o.add("perFrame", per);
        o.add("perSecond", sec);
        return o;
    }
}
