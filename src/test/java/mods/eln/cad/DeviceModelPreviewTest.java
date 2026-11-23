package mods.eln.cad;

import org.erisia.cad.obj.ObjReader;
import org.erisia.cad.preview.Raster;
import org.erisia.cad.tex.Sheet;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Pictures of generated device models (build/generated/cad, src/cadmodels), textured, from four sides and above:
 * -PcadPreviews=DIR writes DIR/model-NAME.png for each model in MODELS, or in -PcadPreviewModels=a,b (all: every
 * generated model).
 */
public class DeviceModelPreviewTest {
    static final String[] MODELS = {"airblower200v", "largeboiler", "boiler", "fuelheater", "computer", "fieldlogger", "compactturbine50v"};

    /** The names to draw: cad.preview.models (comma-separated, or "all": every model directory under root), else MODELS. */
    static List<String> models(Path root) throws java.io.IOException {
        String want = System.getProperty("cad.preview.models");
        if (want == null || want.isBlank()) return List.of(MODELS);
        if (!want.trim().equals("all")) return java.util.Arrays.stream(want.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        try (var dirs = Files.list(root)) {
            return dirs.filter(Files::isDirectory).map(d -> d.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void devices() throws Exception {
        String dir = System.getProperty("cad.previews");
        Assumptions.assumeTrue(dir != null, "-PcadPreviews=DIR");
        // (the project's root from eln.resources = ROOT/src/main/resources: the tests' working directory is elsewhere)
        Path root = Path.of(System.getProperty("eln.resources", "src/main/resources")).toAbsolutePath().getParent().getParent().getParent()
            .resolve("build/generated/cad/assets/eln/model");
        for (String name : models(root)) {
            Path obj = root.resolve(name).resolve(name + ".obj"), png = root.resolve(name).resolve("tex.png");
            if (!Files.exists(obj)) continue;
            var faces = ObjReader.read(obj, true).faces();
            int[] size = new int[2];
            int[] tex = Files.exists(png) ? Sheet.readPng(png, size) : null;
            double[] fit = {16, 16, 16, 0, 0, 0};
            for (var f : faces)
                for (int i = 0; i < f.n; i++)
                    for (int k = 0; k < 3; k++) {
                        fit[k] = Math.min(fit[k], f.xyz[3 * i + k]);
                        fit[k + 3] = Math.max(fit[k + 3], f.xyz[3 * i + k]);
                    }
            // (a margin round it: Raster fits 0.62 of the box's largest side to half the picture)
            for (int k = 0; k < 3; k++) {
                double c = (fit[k] + fit[k + 3]) / 2, h = (fit[k + 3] - fit[k]) / 2 * 1.35;
                fit[k] = c - h;
                fit[k + 3] = c + h;
            }
            Raster.Sampler s = (f, u, v) -> {
                if (tex == null || f.uv == null) return 0xFFB0B0B0;
                int x = Math.max(0, Math.min(size[0] - 1, (int) Math.floor(u * size[0]))), y = Math.max(0, Math.min(size[1] - 1, (int) Math.floor((1 - v) * size[1])));
                return tex[y * size[0] + x];
            };
            List<Raster> pics = new ArrayList<>();
            for (double[] view : new double[][]{{-135, -25}, {-45, -25}, {45, -25}, {135, -25}, {-90, -70}}) {
                Raster r = new Raster(320, 0xFF2B2F36);
                r.draw(faces, s, view[0], view[1], fit);
                pics.add(r);
            }
            Raster.writeRow(Path.of(dir, "model-" + name + ".png"), pics);
        }
    }
}
