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
        Raster r = new Raster(480, 0xFF2B2F36);
        Raster.Sampler s = (f, u, v) -> {
            int x = Math.max(0, Math.min(63, (int) Math.floor(u * 64))), y = Math.max(0, Math.min(63, (int) Math.floor(v * 64)));
            String top = f.solid.name.split("/").length > 1 ? f.solid.name.split("/")[1].replaceAll("#.*", "") : "";
            Object g = groups.get(top);
            int tint = 0xFFFFFFFF;
            if (g != null && !g.toString().equals("LAMP")) {
                int i = List.of("FRAME", "FORK", "TRUNK", "SADDLE").indexOf(g.toString());
                if (i >= 0) tint = 0xFF000000 | mods.eln.bike.BikeColors.tint(type, i, mods.eln.bike.BikeColors.DEFAULT);
            }
            return Raster.tint(tex[y * 64 + x], tint);
        };
        r.draw(faces, s, yaw, pitch, new double[]{-8, 0, -16, 8, 22, 16});
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
        }
    }
}
