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
                int[] out = new int[size[0] * size[1] + 2];
                System.arraycopy(px, 0, out, 0, size[0] * size[1]);
                out[out.length - 2] = size[0];
                out[out.length - 1] = size[1];
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
        int w = s[s.length - 2], h = s[s.length - 1];
        int x = Math.max(0, Math.min(w - 1, (int) (u * w))), y = Math.max(0, Math.min(h - 1, (int) (v * h)));
        return Raster.tint(s[y * w + x], f.argb());
    };

    static Raster pic(List<Face> faces, double[] fit) {
        return pic(faces, fit, 30, 25, false);
    }

    /** ... marked: faces that z-fight drawn magenta-striped (the checker's pairs). */
    static Raster pic(List<Face> faces, double[] fit, double yaw, double pitch, boolean mark) {
        Raster r = new Raster(320, 0xFF2B2F36);
        java.util.Set<Face> fighting = new java.util.HashSet<>();
        if (mark) for (Face[] p : org.erisia.cad.Checker.fights(faces)) java.util.Collections.addAll(fighting, p);
        r.draw(faces, (f, u, v) -> fighting.contains(f) && ((int) (u * 64 + v * 64) & 1) == 0 ? 0xFFFF00FF : SAMPLE.at(f, u, v), yaw, pitch, fit);
        return r;
    }

    @Test
    void junctionsAndAdapters() throws Exception {
        String dir = System.getProperty("cad.previews");
        Assumptions.assumeTrue(dir != null, "-PcadPreviews=DIR");
        // cables: a bend and a cross on the floor (Medium Voltage), from above; magenta: faces that z-fought (before only)
        int tier = 1;
        List<VoxelPart> bend = new ArrayList<>(CablePart.arms(tier, 0, 2, 4)), cross = new ArrayList<>(CablePart.arms(tier, 0, 2, 3, 4, 5));
        double[] floor = {0, 0, 0, 16, 4, 16};
        Raster.writeRow(Path.of(dir, "cable-junctions.png"), List.of(pic(faces(LegacyVoxelGeometry.of(bend)), floor, 20, 60, true),
            pic(faces(VoxelGeometry.of(bend)), floor, 20, 60, false), pic(faces(LegacyVoxelGeometry.of(cross)), floor, 20, 60, true),
            pic(faces(VoxelGeometry.of(cross)), floor, 20, 60, false)));
        // an adapter on the block's top, the iron and the large pipe's stubs under it, seen from above
        List<Raster> row = new ArrayList<>();
        for (int t : new int[]{0, 2}) {
            List<VoxelPart> ad = List.of(new AdapterPart(1), new PipePart(t, AdapterPart.stub(1)));
            double[] fit = {2, 6, 2, 14, 16, 14};
            row.add(pic(faces(LegacyVoxelGeometry.of(ad)), fit, 30, 55, true));
            row.add(pic(faces(VoxelGeometry.of(ad)), fit, 30, 55, false));
        }
        Raster.writeRow(Path.of(dir, "adapter.png"), row);
        // the large pipe's S: two bends 6 voxels apart
        List<VoxelPart> s = List.of(new PipePart(2, new int[]{0, 8, 8, 6, 8, 8, 6, 12, 8, 16, 12, 8}));
        double[] fit = {0, 2, 2, 16, 16, 14};
        Raster.writeRow(Path.of(dir, "large-pipe-s.png"), List.of(pic(faces(LegacyVoxelGeometry.of(s)), fit, 0, 10, true),
            pic(faces(VoxelGeometry.of(s)), fit, 0, 10, false)));
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
