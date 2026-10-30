package mods.eln.client.entity;

import mods.eln.bike.BikeTexture;
import mods.eln.bike.BikeType;
import org.erisia.cad.Face;
import org.erisia.cad.preview.Raster;
import org.erisia.cad.tex.Sheet;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Before/after pictures of both bikes (old layer and texture, new ones), side on and from the front quarter: -PcadPreviews=DIR. */
public class BikePreviewTest {
    static Raster pic(List<Face> faces, int[] tex, Map<String, ?> groups, BikeType type, double yaw, double pitch) {
        return pic(faces, tex, groups, type, yaw, pitch, new double[]{-8, 0, -16, 8, 22, 16}, false);
    }

    /** ... fitted to a box, and marked: faces that z-fight drawn magenta-striped (the checker's pairs). */
    static Raster pic(List<Face> faces, int[] tex, Map<String, ?> groups, BikeType type, double yaw, double pitch, double[] fit, boolean mark) {
        Raster r = new Raster(480, 0xFF2B2F36);
        java.util.Set<Face> fighting = new java.util.HashSet<>();
        if (mark) for (Face[] p : org.erisia.cad.Checker.fights(faces)) java.util.Collections.addAll(fighting, p);
        Raster.Sampler s = (f, u, v) -> {
            int x = Math.max(0, Math.min(63, (int) Math.floor(u * 64))), y = Math.max(0, Math.min(63, (int) Math.floor(v * 64)));
            String top = f.solid.name.split("/").length > 1 ? f.solid.name.split("/")[1].replaceAll("#.*", "") : "";
            Object g = groups.get(top);
            int tint = 0xFFFFFFFF;
            if (g != null && !g.toString().equals("LAMP")) {
                int i = List.of("FRAME", "FORK", "TRUNK", "SADDLE").indexOf(g.toString());
                if (i >= 0) tint = 0xFF000000 | mods.eln.bike.BikeColors.tint(type, i, mods.eln.bike.BikeColors.DEFAULT);
            }
            if (fighting.contains(f) && ((int) (u * 256 + v * 256) & 1) == 0) return 0xFFFF00FF;
            return Raster.tint(tex[y * 64 + x], tint);
        };
        r.draw(faces, s, yaw, pitch, fit);
        return r;
    }

    @Test
    void bikes() throws Exception {
        String dir = System.getProperty("cad.previews");
        Assumptions.assumeTrue(dir != null, "-PcadPreviews=DIR");
        int[] size = new int[2];
        int[] before = Sheet.readPng(Path.of(dir, "electric_bike-texture-before.png"), size), after = BikeTexture.paint().argb;
        for (boolean terrain : new boolean[]{false, true}) {
            BikeType type = terrain ? BikeType.TERRAIN : BikeType.ELECTRIC;
            var old = BikeFaces.of(LegacyBikeModel.createLayer(terrain).bakeRoot(), Map.of());
            var now = BikeFaces.of(BikeModel.createLayer(terrain).bakeRoot(), Map.of());
            Map<String, ?> og = LegacyBikeModel.GROUPS, ng = BikeModel.GROUPS;
            for (double[] view : new double[][]{{90, 5}, {-55, 25}}) {
                String name = (terrain ? "terrain-bike" : "bike") + (view[0] == 90 ? "-side" : "-quarter") + ".png";
                Raster.writeRow(Path.of(dir, name), List.of(pic(old, before, og, type, view[0], view[1]), pic(now, after, ng, type, view[0], view[1])));
            }
            // close-ups, magenta where faces z-fought before (after: VoxelGeometryCadTest/BikeModelCadTest find none): the rear hub (stays, rims) from the side, the seat cluster and the
            // saddle from below and behind
            String b = terrain ? "terrain-bike" : "bike";
            double[] hub = {-3, 0, 3, 3, 13, 17}, saddle = {-4, 10, -2, 4, 20, 8};
            double[] front = {-3, 7, -12, 3, 18, -3};
            Raster.writeRow(Path.of(dir, b + "-fork-crown.png"), List.of(pic(old, before, og, type, -50, 15, front, true), pic(now, after, ng, type, -50, 15, front, false)));
            double[] crank = {-6, 0, -4, 6, 10, 6};
            Raster.writeRow(Path.of(dir, b + "-cranks.png"), List.of(pic(old, before, og, type, 60, 10, crank, true), pic(now, after, ng, type, 60, 10, crank, false)));
            Raster.writeRow(Path.of(dir, b + "-rear-hub.png"), List.of(pic(old, before, og, type, 75, 10, hub, true), pic(now, after, ng, type, 75, 10, hub, false)));
            Raster.writeRow(Path.of(dir, b + "-saddle.png"), List.of(pic(old, before, og, type, 150, -35, saddle, true), pic(now, after, ng, type, 150, -35, saddle, false),
                pic(old, before, og, type, 60, 30, saddle, true), pic(now, after, ng, type, 60, 30, saddle, false)));
        }
    }
}
