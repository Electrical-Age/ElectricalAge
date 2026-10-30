package mods.eln.node.voxel;

import org.erisia.cad.Face;
import org.erisia.cad.Flat;
import org.erisia.cad.preview.Raster;
import org.erisia.cad.tex.Sheet;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Before/after pictures of the safety valve and the pipe sensor on each pipe tier: -PcadPreviews=DIR (board #44). */
public class VoxelPreviewTest {
    static final Map<String, int[]> SPRITES = new HashMap<>();

    static int[] sprite(String id) {
        return SPRITES.computeIfAbsent(id, k -> {
            try {
                int[] size = new int[2];
                int[] px = Sheet.readPng(Path.of(System.getProperty("eln.resources"), "assets/eln/textures/" + k.replace("eln:", "") + ".png"), size);
                int[] out = new int[size[0] * size[0] + 1];
                System.arraycopy(px, 0, out, 0, size[0] * size[0]);
                out[out.length - 1] = size[0];
                return out;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    static List<Face> faces(List<?> quads) {
        List<Face> out = new ArrayList<>();
        for (Object o : quads) {
            float[] xyz;
            float[] uv;
            int argb;
            String sprite;
            if (o instanceof VoxelGeometry.Quad q) {
                xyz = q.xyz(); uv = q.uv(); argb = q.argb(); sprite = q.sprite().toString();
            } else {
                var q = (LegacyVoxelGeometry.Quad) o;
                xyz = q.xyz(); uv = q.uv(); argb = q.argb(); sprite = q.sprite().toString();
            }
            double[] p = new double[12];
            for (int i = 0; i < 12; i++) p[i] = xyz[i] * 16;
            out.add(Flat.face(p, uv, argb, sprite, ""));
        }
        return out;
    }

    static final Raster.Sampler SAMPLE = (f, u, v) -> {
        int[] s = sprite((String) f.material());
        int n = s[s.length - 1];
        int x = Math.max(0, Math.min(n - 1, (int) (u * n))), y = Math.max(0, Math.min(n - 1, (int) (v * n)));
        return Raster.tint(s[y * n + x], f.argb());
    };

    static Raster pic(List<Face> faces, double[] fit) {
        Raster r = new Raster(320, 0xFF2B2F36);
        r.draw(faces, SAMPLE, 30, 25, fit);
        return r;
    }

    @Test
    void safetyValveAndSensor() throws Exception {
        String dir = System.getProperty("cad.previews");
        Assumptions.assumeTrue(dir != null, "-PcadPreviews=DIR");
        for (int tier = 0; tier < PipePart.TIER_NAME.length; tier++) {
            PipePart pipe = new PipePart(tier, new int[]{0, 8, 8, 16, 8, 8});
            List<VoxelPart> sv = List.of(pipe, new SafetyValvePart(8, 8, 8, 1, PipePart.GRID[tier], 10e5));
            PipeSensorPart sensor = new PipeSensorPart(8, 8, 8, 1, PipePart.GRID[tier], PipeSensorPart.PRESSURE, 1);
            sensor.shown = 0.3;
            List<VoxelPart> ps = List.of(pipe, sensor);
            double[] fit = {3, 4, 3, 13, 14, 13};
            List<Raster> row = List.of(pic(faces(LegacyVoxelGeometry.of(sv)), fit), pic(faces(VoxelGeometry.of(sv)), fit),
                pic(faces(LegacyVoxelGeometry.of(ps)), fit), pic(faces(VoxelGeometry.of(ps)), fit));
            Raster.writeRow(Path.of(dir, "fittings-" + PipePart.TIER_NAME[tier].replace(' ', '-') + ".png"), row);
        }
    }
}
