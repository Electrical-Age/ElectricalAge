package mods.eln.client.entity;

import org.erisia.cad.Checker;
import org.erisia.cad.Face;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The checker over the bike as drawn before the port (LegacyBikeModel), parked and ridden: what it finds besides the
 * known bugs (board #44). Reports, never fails; -PcadReport=PREFIX writes the list.
 */
public class BikeSurveyTest {
    @Test
    void legacyFindings() throws Exception {
        StringBuilder b = new StringBuilder();
        for (boolean terrain : new boolean[]{false, true})
            for (boolean ridden : new boolean[]{false, true}) {
                var root = LegacyBikeModel.createLayer(terrain).bakeRoot();
                var stand = root.getChild("kickstand");
                stand.zRot = ridden ? 0.15f : 0.35f;
                stand.xRot = ridden ? 1.45f : 0.05f;
                List<Face> faces = BikeFaces.of(root, LegacyBikeModel.GROUPS);
                assertTrue(faces.size() > 100);
                var issues = Checker.faces(faces);
                b.append(terrain ? "terrain bike" : "bike").append(ridden ? ", ridden" : ", parked").append(": ").append(faces.size()).append(" faces, ")
                    .append(issues.size()).append(" issues\n");
                for (var is : issues) b.append("  ").append(is.toString().replace("\n", "\n  ")).append("\n");
            }
        System.out.println(b);
        String out = System.getProperty("cad.report");
        if (out != null) java.nio.file.Files.writeString(java.nio.file.Path.of(out + "-bike.txt"), b.toString());
    }
}
