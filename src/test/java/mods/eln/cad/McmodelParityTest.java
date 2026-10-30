package mods.eln.cad;

import org.erisia.cad.Box;
import org.erisia.cad.Frustum;
import org.erisia.cad.Model;
import org.erisia.cad.Ring;
import org.erisia.cad.Solid;
import org.erisia.cad.obj.ObjCompare;
import org.erisia.cad.obj.ObjReader;
import org.erisia.cad.obj.ObjWriter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The OBJ emitters against mcmodel.py (board #44, #145): the energy converters (tools/models/converter.py, the block
 * target) and the steam oven (steamoven.py, EA's Obj3D target) rebuilt with the library draw the same surface,
 * material by material, with the same texture coordinates, as the committed OBJs (triangulated differently). These
 * ports aren't shipped: the checker finds z-fights in the models (ObjSurveyTest), and fixing them changes their look,
 * which waits for Baughn; the oven's atlas also differs from the global palette (its steel is lighter).
 */
public class McmodelParityTest {
    record Tier(String paint, int bushings, double h, int sheds, double r) {
    }

    static final Map<String, Tier> TIERS = Map.of("lvu", new Tier("paint_green", 2, 1.6, 0, 0.75), "mvu", new Tier("paint_blue", 3, 2.4, 1, 0.8),
        "hvu", new Tier("paint_orange", 3, 3.0, 2, 0.85));
    static final double CX = 8.0, CY = 6.75, R = 4.75;

    static Solid box(Model m, double x0, double y0, double z0, double x1, double y1, double z1, String mat) {
        return m.add(new Box(x0, y0, z0, x1, y1, z1)).material(mat);
    }

    /** mcmodel.prism: n sides, a flat side up (phase half a side), caps as polygons. */
    static Frustum prism(Model m, int axis, double c1, double c2, double r, double a0, double a1, String mat, int n, double rEnd, boolean cap0, boolean cap1) {
        Frustum f = m.add(new Frustum(axis, c1, c2, r, rEnd, a0, a1, n, 0.5));
        f.material(mat);
        f.cap0 = cap0;
        f.cap1 = cap1;
        f.capPolygon = true;
        return f;
    }

    /** mcmodel.tube with inner=False. */
    static void ring(Model m, int axis, double c1, double c2, double rIn, double rOut, double a0, double a1, String mat, int n) {
        Ring r = m.add(new Ring(axis, c1, c2, rIn, rOut, a0, a1, n));
        r.phase = 0.5;
        r.inner = false;
        r.material(mat);
    }

    static Model condenser(String tier) {
        Tier t = TIERS.get(tier);
        Model m = new Model();
        box(m, 1.5, 0, 0.5, 4, 1.5, 15.5, "darksteel");
        box(m, 12, 0, 0.5, 14.5, 1.5, 15.5, "darksteel");
        box(m, 4, 0.25, 2, 12, 1.25, 3.5, "darksteel");
        box(m, 4, 0.25, 12.5, 12, 1.25, 14, "darksteel");
        box(m, 3.5, 1.5, 4, 5.5, 3.5, 6, "steel");
        box(m, 10.5, 1.5, 4, 12.5, 3.5, 6, "steel");
        box(m, 3.5, 1.5, 10, 5.5, 3.5, 12, "steel");
        box(m, 10.5, 1.5, 10, 12.5, 3.5, 12, "steel");
        prism(m, 2, CX, CY, R, 3, 13, "steel", 12, R, false, false);
        prism(m, 2, CX, CY, R, 2, 3, "steel", 12, R, false, false);
        prism(m, 2, CX, CY, R - 1.25, 1.25, 2, "steel", 12, R, true, false);
        prism(m, 2, CX, CY, R, 13, 14, "steel", 12, R, false, false);
        prism(m, 2, CX, CY, R, 14, 14.75, "steel", 12, R - 1.25, false, true);
        for (double z : new double[]{3.5, 5.0, 11.0, 12.5}) ring(m, 2, CX, CY, R - 0.1, R + 0.7, z, z + 0.5, "darksteel", 12);
        ring(m, 2, CX, CY, R - 0.1, R + 0.45, 6.5, 9.5, t.paint(), 12);
        prism(m, 2, CX, CY, 1.0, 0.75, 1.25, "darksteel", 8, 1.0, true, true);
        prism(m, 2, CX, CY, 1.0, 14.75, 15.0, "darksteel", 8, 1.0, true, true);
        prism(m, 2, CX, CY, 2.0, 15.0, 15.5, "copper", 8, 2.0, true, true);
        box(m, 4.5, 0, 0, 11.5, 4.0, 0.75, "steel");
        for (double x : new double[]{5.75, 8.0, 10.25}) box(m, x - 0.5, 1.75, -0.01, x + 0.5, 2.75, 0.01, "copper");
        box(m, 4.5, 3.75, 0, 11.5, 4.25, 0.75, t.paint());
        box(m, 5.0, 0.0, 15.25, 11.0, 2.5, 16.0, "darksteel");
        box(m, 5.5, 2.5, 15.5, 10.5, 10.0, 16.0, "darksteel");
        ((Box) box(m, 6.25, 3.25, 15.99, 9.75, 9.25, 16.01, "export")).only("s");
        for (double[] b : new double[][]{{5.9, 2.9}, {10.1, 2.9}, {5.9, 9.6}, {10.1, 9.6}})
            ((Box) box(m, b[0] - 0.25, b[1] - 0.25, 15.99, b[0] + 0.25, b[1] + 0.25, 16.02, "brass")).only("s");
        double top = CY + R;
        box(m, 5.0, top - 0.75, 4.5, 11.0, top + 1.25, 11.5, "steel");
        box(m, 4.75, top + 1.25, 4.25, 11.25, top + 1.5, 11.75, "darksteel");
        double by = top + 1.5;
        int n = t.bushings();
        for (int i = 0; i < n; i++) {
            double x = n == 1 ? 8.0 : 8.0 + (i - (n - 1) / 2.0) * 2.25, z = 8.0;
            prism(m, 1, x, z, t.r(), by, by + t.h(), "porcelain", 8, t.r(), false, false);
            prism(m, 1, x, z, 0.45, by + t.h(), by + t.h() + 0.5, "copper", 6, 0.45, false, true);
            for (int k = 0; k < t.sheds(); k++) {
                double sy = by + (k + 1) * t.h() / (t.sheds() + 1) - 0.2;
                prism(m, 1, x, z, t.r() + 0.55, sy, sy + 0.35, "porcelain", 8, t.r() + 0.55, true, true);
            }
            prism(m, 1, x, z, t.r() + 0.2, by, by + 0.3, t.paint(), 8, t.r() + 0.2, true, true);
        }
        box(m, 11.0, top - 0.25, 6.0, 11.1, top + 0.75, 10.0, "brass");
        return m;
    }

    @Test
    void theEmitterDrawsWhatMcmodelDrew() throws Exception {
        Path tmp = Files.createTempDirectory("cad-converter");
        Path committed = Path.of(System.getProperty("eln.resources", "src/main/resources"), "assets/eln/models/block");
        for (String tier : List.of("lvu", "mvu", "hvu")) {
            String name = "energy_converter_" + tier;
            ObjWriter.block(condenser(tier), tmp, name);
            var mine = ObjReader.read(tmp.resolve(name + ".obj"), false).faces();
            var theirs = ObjReader.read(committed.resolve(name + ".obj"), false).faces();
            List<String> diff = ObjCompare.differences(mine, theirs, f -> String.valueOf(f.material()).replaceAll("^.*/", ""), 8);
            assertTrue(diff.isEmpty(), tier + ":\n" + String.join("\n", diff));
        }
    }

    /** tools/models/steamoven.py's oven, for the Obj3D emitter (ObjWriter.eln). */
    static Model oven() {
        Model m = new Model();
        for (double[] p : new double[][]{{1.5, 1.5}, {13, 1.5}, {1.5, 13}, {13, 13}}) box(m, p[0], 0, p[1], p[0] + 1.5, 1.5, p[1] + 1.5, "darksteel");
        box(m, 1, 1.5, 1, 15, 13, 15, "steel");
        ((Box) box(m, 0.8, 1.5, 0.8, 15.2, 2.0, 15.2, "darksteel")).only("nsewd");
        ((Box) box(m, 0.8, 12.5, 0.8, 15.2, 13.0, 15.2, "darksteel")).only("nsew");
        box(m, 0.5, 3, 2.5, 1.0, 12, 13.5, "darksteel");
        ((Box) box(m, 0.4, 4, 3.5, 0.5, 11, 12.5, "glass")).only("w");
        box(m, 0.0, 6.5, 12.6, 0.5, 8.5, 13.2, "brass");
        for (double y : new double[]{4.5, 10.0}) box(m, 0.2, y, 2.3, 0.8, y + 1, 2.9, "darksteel");
        prism(m, 1, 8, 8, 2, 13, 15, "castiron", 12, 2, false, false);
        prism(m, 1, 8, 8, 2.75, 15, 16, "steel", 12, 2.75, true, true);
        prism(m, 1, 8, 8, 2.35, 13, 13.6, "paint_green", 12, 2.35, false, true);
        prism(m, 0, 11, 8, 0.9, 15, 15.9, "castiron", 8, 0.9, false, false);
        prism(m, 0, 11, 8, 1.2, 15.6, 16, "darksteel", 8, 1.2, false, false);
        prism(m, 0, 11, 8, 0.85, 15.3, 15.35, "soot", 8, 0.85, false, true);
        for (double[] z : new double[][]{{0.1, 0.85}, {15.15, 15.9}}) {
            box(m, 4.5, 0, z[0], 11.5, 4.0, z[1], "steel");
            box(m, 4.5, 4.0, z[0], 11.5, 4.5, z[1], "paint_green");
            for (double x : new double[]{5.75, 8.0, 10.25}) box(m, x - 0.5, 1.75, z[0] - 0.1, x + 0.5, 2.75, z[1] + 0.1, "copper");
        }
        return m;
    }

    @Test
    void theObj3dEmitterDrawsWhatMcmodelDrew() throws Exception {
        Path tmp = Files.createTempDirectory("cad-oven");
        var atlas = new org.erisia.cad.tex.Atlas(4, "steel", "darksteel", "castiron", "copper", "glass", "brass", "soot", "paint_green");
        ObjWriter.eln(List.of(new ObjWriter.EPart("main", oven().solids)), atlas, tmp, "steamoven", "tex.png");
        Path committed = Path.of(System.getProperty("eln.resources", "src/main/resources"), "assets/eln/model/steamoven/steamoven.obj");
        var mine = ObjReader.read(tmp.resolve("steamoven.obj"), true).faces();
        var theirs = ObjReader.read(committed, true).faces();
        List<String> diff = ObjCompare.differences(mine, theirs, f -> String.valueOf(f.material()), 8);
        assertTrue(diff.isEmpty(), String.join("\n", diff));
    }
}
