package mods.eln.node.voxel;

import org.erisia.cad.Checker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VoxelGeometry on the checked model library (board #44): every pipe tier x fitting x mode x orientation x route shape
 * and every cable passes the checker, and draws exactly what it drew before (LegacyVoxelGeometry) except the safety
 * valve and the pipe sensor, which were fixed.
 */
public class VoxelGeometryCadTest {
    /**
     * Z-fights the checker finds in geometry drawn as before, left as they were (they need Baughn's say on the look):
     * a pair is known when both faces' solids are named so. The cable boxes' tops overlap where a cable turns or
     * branches, each with its own strip of texture; an adapter's flange lies in the block face over the stub's end; two
     * bends of a large pipe 6 voxels apart have fitting rings that overlap side by side.
     */
    static final String[][] KNOWN = {{"cable box", "cable box"}, {"adapter flange", "pipe"}, {"pipe fitting", "pipe fitting"}};

    static boolean known(Checker.Issue is) {
        if (!is.rule().equals("z-fight")) return false;
        String[] lines = is.what().split("\n");
        for (String[] k : KNOWN)
            if (lines.length == 2 && (lines[0].contains(" " + k[0] + " ") && lines[1].contains(" " + k[1] + " ")
                || lines[0].contains(" " + k[1] + " ") && lines[1].contains(" " + k[0] + " "))) return true;
        return false;
    }

    @Test
    void everyCasePassesTheChecker() {
        List<String> bad = new ArrayList<>();
        Map<String, Integer> knownCount = new HashMap<>();
        int n = 0;
        for (VoxelCases.Case c : VoxelCases.all()) {
            n++;
            for (Checker.Issue is : Checker.check(VoxelGeometry.model(c.parts(), null))) {
                if (known(is)) knownCount.merge(c.name().split(" (on|bend|cross|tee|straight|knuckle|[0-9])")[0], 1, Integer::sum);
                else bad.add(c.name() + ": " + is);
            }
        }
        System.out.println(n + " blocks checked; known z-fights left as drawn: " + knownCount);
        assertTrue(bad.isEmpty(), bad.size() + " issues, e.g.\n" + String.join("\n", bad.subList(0, Math.min(12, bad.size()))));
    }

    /** A quad as text, its corners rotated to start at the smallest (the same quad wound the same way compares equal). */
    static String key(String sprite, int argb, float[] xyz, float[] uv, int face) {
        String[] corner = new String[4];
        for (int i = 0; i < 4; i++)
            corner[i] = String.format("(%.4f %.4f %.4f|%.4f %.4f)", xyz[3 * i], xyz[3 * i + 1], xyz[3 * i + 2], uv[2 * i], uv[2 * i + 1]).replace("-0.0000", "0.0000");
        int start = 0;
        for (int i = 1; i < 4; i++) if (corner[i].compareTo(corner[start]) < 0) start = i;
        StringBuilder b = new StringBuilder(sprite + " " + Integer.toHexString(argb) + " " + face);
        for (int i = 0; i < 4; i++) b.append(' ').append(corner[(start + i) % 4]);
        return b.toString();
    }

    @Test
    void drawsAsBeforeExceptTheFixedFittings() {
        int compared = 0;
        for (VoxelCases.Case c : VoxelCases.all()) {
            if (c.parts().stream().anyMatch(p -> p instanceof SafetyValvePart || p instanceof PipeSensorPart)) continue;
            List<String> was = new ArrayList<>(), now = new ArrayList<>();
            for (var q : LegacyVoxelGeometry.of(c.parts())) was.add(key(q.sprite().toString(), q.argb(), q.xyz(), q.uv(), q.face()));
            for (var q : VoxelGeometry.of(c.parts())) now.add(key(q.sprite().toString(), q.argb(), q.xyz(), q.uv(), q.face()));
            was.sort(null);
            now.sort(null);
            assertEquals(was, now, c.name());
            compared++;
        }
        // lugs too
        CablePart cable = new CablePart(0, 0, new int[]{0, 0, 8, 16, 0, 8});
        float[] lugs = {0, 0, 3, 3};
        List<String> was = new ArrayList<>(), now = new ArrayList<>();
        for (var q : LegacyVoxelGeometry.of(List.of(cable), lugs)) was.add(key(q.sprite().toString(), q.argb(), q.xyz(), q.uv(), q.face()));
        for (var q : VoxelGeometry.of(List.of(cable), lugs)) now.add(key(q.sprite().toString(), q.argb(), q.xyz(), q.uv(), q.face()));
        was.sort(null);
        now.sort(null);
        assertTrue(was.size() > 18, "the lugs drew");
        assertEquals(was, now, "lugs");
        System.out.println(compared + " blocks draw as before");
    }

    @Test
    void fittingsAreSizedToTheDrawnPipe() {
        // the safety valve's cap: at most twice the drawn pipe's radius (it was 4 px across on the 1 px iron pipe)
        for (int tier = 0; tier < PipePart.TIER_NAME.length; tier++) {
            double rp = PipePart.DRAWN[tier] / 2;
            PipePart pipe = new PipePart(tier, new int[]{0, 8, 8, 16, 8, 8});
            var m = VoxelGeometry.model(List.of(pipe, new SafetyValvePart(8, 8, 8, 1, PipePart.GRID[tier], 10e5)), null);
            double[] cap = m.bounds(s -> s.name.equals("safety valve cap"));
            assertTrue((cap[3] - cap[0]) / 2 <= 2 * rp + 1e-6, PipePart.TIER_NAME[tier] + ": cap radius " + (cap[3] - cap[0]) / 2);
            double[] body = m.bounds(s -> s.name.equals("safety valve body"));
            assertEquals(8, body[1], 1e-9, "the body stands on the pipe (from its axis)");
        }
    }
}
