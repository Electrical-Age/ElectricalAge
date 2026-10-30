package org.erisia.cad;

import org.erisia.cad.obj.ObjCompare;
import org.erisia.cad.obj.ObjReader;
import org.erisia.cad.obj.ObjWriter;
import org.erisia.cad.obj.PlanarUv;
import org.erisia.cad.tex.Atlas;
import org.erisia.cad.tex.Palette;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObjTest {
    static Model sample() {
        Model m = new Model();
        m.add(new Box(2, 0, 2, 14, 6, 14)).material("steel");
        Frustum f = m.add(new Frustum(1, 8, 8, 2, 2, 6, 10, 12, 0.5));
        f.capPolygon = true;
        f.material("copper");
        return m;
    }

    @Test
    void writtenAndReadBackTheSame() throws Exception {
        Path tmp = Files.createTempDirectory("cad-obj");
        ObjWriter.block(sample(), tmp, "s");
        var back = ObjReader.read(tmp.resolve("s.obj"), false);
        assertTrue(back.problems().isEmpty());
        List<Face> mine = sample().faces();
        for (Face f : mine) f.uv = null;
        assertEquals(List.of(), ObjCompare.differences(mine, back.faces(), f -> String.valueOf(f.material()).replaceAll("^.*/", ""), 5));
        Atlas a = new Atlas(2, "steel", "copper");
        ObjWriter.eln(List.of(new ObjWriter.EPart("main", sample().solids)), a, tmp, "n", "tex.png");
        var node = ObjReader.read(tmp.resolve("n.obj"), true);
        double[] b = Checker.bounds(node.faces());
        assertArrayEquals(new double[]{2, 0, 2, 14, 10, 14}, b, 1e-5);
        assertTrue(Files.readString(tmp.resolve("n.obj")).contains("o main\n"));
    }

    @Test
    void planarUvAsMcmodel() {
        // a +z face: u along x, v up, shifted into 0..1
        double[] uv = PlanarUv.of(new double[]{20, 2, 5, 24, 2, 5, 24, 6, 5}, 3, 0, 0, 1);
        assertArrayEquals(new double[]{0.25, 0.125, 0.5, 0.125, 0.5, 0.375}, uv, 1e-9);
    }

    @Test
    void thePalettePaintsEveryCell() {
        Atlas a = new Atlas(4, Palette.names().toArray(new String[0]));
        var s = a.paint(1);
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w && y < 16 * ((Palette.names().size() + 3) / 4); x++)
                if (x / 16 + 4 * (y / 16) < Palette.names().size()) assertTrue((s.get(x, y) >>> 24) == 255, "painted " + x + " " + y);
    }
}
