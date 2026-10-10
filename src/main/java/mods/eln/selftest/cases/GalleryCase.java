package mods.eln.selftest.cases;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * `/eln selftest gallery`: every device, unpowered, in rows on a stone floor, kept for screenshots (port step T5,
 * notes/port26/PLAN.md). Six-node devices 3 apart in rows of 7 (rows 6 apart), then transparent ones 8 apart (rows 12
 * apart; they face -X). Writes &lt;server dir&gt;/eln-gallery.json: one camera per row (from -Z, looking +Z, 30 degrees
 * down) and one close-up per device, as {label, x, y, z, yaw, pitch} with y at the feet (eye = feet + 1.62).
 */
public final class GalleryCase implements SelfTestCase {
    static final int PER_ROW = 7;
    final List<SmokeCases.Device> devices = SmokeCases.allDevices();

    public String name() {
        return "gallery";
    }

    public int width() {
        return 1;
    }

    public void build(SelfTestContext ctx) {
        int i = 0, t = 0;
        int sixRows = (int) devices.stream().filter(d -> d.six).count() / PER_ROW + 1;
        for (SmokeCases.Device d : devices) {
            BlockPos p;
            if (d.six) {
                p = ctx.at(2 + (i % PER_ROW) * 3, 1, 2 + (i / PER_ROW) * 6);
                i++;
            } else {
                p = ctx.at(4 + (t % PER_ROW) * 8, 1, 2 + sixRows * 6 + 6 + (t / PER_ROW) * 12);
                t++;
            }
            // a 3x3 floor for looks under six-node devices; transparent ones get what place() puts (the water
            // turbine's ghosts reach down)
            if (d.six) for (BlockPos f : BlockPos.getAllInBox(p.add(-1, -1, -1), p.add(1, -1, 1))) ctx.floor(f);
            SmokeCases.place(ctx, d, p);
        }
    }

    static JsonObject cam(String label, double x, double eyeY, double z, float yaw, float pitch) {
        JsonObject o = new JsonObject();
        o.addProperty("label", label);
        o.addProperty("x", x);
        o.addProperty("y", eyeY - 1.62);
        o.addProperty("z", z);
        o.addProperty("yaw", yaw);
        o.addProperty("pitch", pitch);
        return o;
    }

    public void measure(SelfTestContext ctx) {
        JsonArray rows = new JsonArray(), closeups = new JsonArray();
        int placed = 0;
        java.util.Map<Integer, double[]> rowSpan = new java.util.TreeMap<>(); // row key -> minX, maxX, z, y
        for (SmokeCases.Device d : devices) {
            if (d.placeError != null || d.at == null) {
                ctx.check("gallery placed " + d.name, false, String.valueOf(d.placeError));
                continue;
            }
            placed++;
            double x = d.at.getX() + 0.5, y = d.at.getY(), z = d.at.getZ() + 0.5;
            String label = (d.six ? "six-" : "transparent-") + d.damage + " " + d.name;
            if (d.six) closeups.add(cam(label, x, y + 2.2, z - 1.8, 0f, 45f));
            else closeups.add(cam(label, x - 4.5, y + 2.5, z, -90f, 20f));
            int key = (d.six ? 0 : 100000) + d.at.getZ();
            double[] s = rowSpan.computeIfAbsent(key, k -> new double[]{1e9, -1e9, z, y, 0});
            s[0] = Math.min(s[0], x);
            s[1] = Math.max(s[1], x);
            s[4] = d.six ? 0 : 1;
        }
        int r = 0;
        for (double[] s : rowSpan.values()) {
            boolean big = s[4] > 0;
            double back = big ? 12 : 6, up = big ? 6 : 3.5;
            rows.add(cam("row " + (++r), (s[0] + s[1]) / 2, s[3] + up, s[2] - back, 0f, 30f));
        }
        JsonObject out = new JsonObject();
        out.add("rows", rows);
        out.add("closeups", closeups);
        try (Writer w = new FileWriter(new File("eln-gallery.json"))) {
            new GsonBuilder().setPrettyPrinting().create().toJson(out, w);
        } catch (IOException e) {
            ctx.check("gallery json written", false, e.toString());
        }
        ctx.check("gallery: devices placed", placed == devices.size(), placed + "/" + devices.size());
        ctx.line("INFO gallery: " + rows.size() + " row cameras, " + closeups.size() + " close-ups in eln-gallery.json");
    }
}
