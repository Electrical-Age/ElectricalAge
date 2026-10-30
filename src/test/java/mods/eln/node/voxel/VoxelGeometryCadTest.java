package mods.eln.node.voxel;

import org.erisia.cad.Checker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VoxelGeometry on the checked model library (board #44): every pipe tier x fitting x mode x orientation x route shape
 * and every cable passes the checker, and draws exactly what it drew before (LegacyVoxelGeometry) except the parts
 * that were fixed ({@link #changed}).
 */
public class VoxelGeometryCadTest {
    @Test
    void everyCasePassesTheChecker() {
        List<String> bad = new ArrayList<>();
        int n = 0;
        for (VoxelCases.Case c : VoxelCases.all()) {
            n++;
            for (Checker.Issue is : Checker.check(VoxelGeometry.model(c.parts(), null))) bad.add(c.name() + ": " + is);
        }
        System.out.println(n + " blocks checked");
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

    /**
     * The blocks whose drawing was fixed: the safety valve and sensor (resized), adapters (no pipe flange or cap under
     * the adapter's), cable bends and branches (boxes trimmed where they overlapped), the large pipe's S (its second
     * fitting ring a layer out).
     */
    static boolean changed(VoxelCases.Case c) {
        return c.parts().stream().anyMatch(p -> p instanceof SafetyValvePart || p instanceof PipeSensorPart || p instanceof AdapterPart)
            || c.name().startsWith("cable") && c.name().matches(".* (bend|tee|cross) on .*") || c.name().startsWith("large alloy S ");
    }

    @Test
    void drawsAsBeforeExceptTheFixedParts() {
        int compared = 0;
        for (VoxelCases.Case c : VoxelCases.all()) {
            if (changed(c)) continue;
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
