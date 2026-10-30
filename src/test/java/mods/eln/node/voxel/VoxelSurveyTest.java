package mods.eln.node.voxel;

import org.erisia.cad.Checker;
import org.erisia.cad.Face;
import org.erisia.cad.Flat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The checker over the voxel parts as drawn before the port (LegacyVoxelGeometry): what it finds besides the known bugs
 * (board #44). Reports, never fails; -PcadReport=FILE writes the list.
 */
public class VoxelSurveyTest {
    static List<Face> legacyFaces(List<VoxelPart> parts) {
        List<Face> out = new ArrayList<>();
        int i = 0;
        for (var q : LegacyVoxelGeometry.of(parts)) {
            double[] xyz = new double[12];
            for (int k = 0; k < 12; k++) xyz[k] = q.xyz()[k] * 16;
            out.add(Flat.face(xyz, q.uv(), q.argb(), q.sprite(), "quad " + i++));
        }
        return out;
    }

    @Test
    void legacyFindings() throws Exception {
        Map<String, List<String>> byKind = new TreeMap<>();
        int cases = 0;
        for (VoxelCases.Case c : VoxelCases.all()) {
            cases++;
            for (Checker.Issue is : Checker.faces(legacyFaces(c.parts()))) {
                if (System.getProperty("cad.detail") != null && c.name().contains(System.getProperty("cad.detail"))) System.out.println("DETAIL " + c.name() + ": " + is);
                byKind.computeIfAbsent(is.rule() + " | " + c.name().replaceAll(" (at|on|outlet) [0-9a-z]+", "").replaceAll(" [0-9]$", ""), k -> new ArrayList<>())
                    .add(c.name() + ": " + is.what().replace("\n", " "));
            }
        }
        StringBuilder b = new StringBuilder("Legacy voxel geometry, " + cases + " blocks checked\n");
        for (var e : byKind.entrySet()) {
            long blocks = e.getValue().stream().map(x -> x.substring(0, x.indexOf(':'))).distinct().count();
            b.append(blocks).append(" blocks (").append(e.getValue().size()).append(" face pairs): ").append(e.getKey()).append("\n    e.g. ").append(e.getValue().get(0)).append("\n");
        }
        System.out.println(b);
        String out = System.getProperty("cad.report");
        if (out != null) java.nio.file.Files.writeString(java.nio.file.Path.of(out + "-voxel.txt"), b.toString());
    }
}
