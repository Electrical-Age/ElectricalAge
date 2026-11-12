package mods.eln.node.voxel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Fittings drawn where runs are joined (Baughn: visible = connected; routing phase 2). */
public class FittingsTest {
    static long named(List<VoxelPart> parts, String name) {
        return VoxelGeometry.model(parts, null).solids.stream().filter(s -> name.equals(s.name)).count();
    }

    @Test
    void aCableBranchGetsASleeve() {
        var trunk = new CablePart(1, 0, new int[]{8, 0, 0, 8, 0, 16});
        var branch = new CablePart(1, 0, new int[]{8, 0, 8, 16, 0, 8});
        assertEquals(1, named(List.of(trunk, branch), "cable splice"));
    }

    @Test
    void aCornerOrAnotherKindGetsNone() {
        var a = new CablePart(1, 0, new int[]{8, 0, 0, 8, 0, 8});
        var b = new CablePart(1, 0, new int[]{8, 0, 8, 16, 0, 8});
        assertEquals(0, named(List.of(a, b), "cable splice"), "two pieces of one route meeting at a corner");
        var signal = new CablePart(0, 0, new int[]{8, 0, 8, 16, 0, 8});
        var trunk = new CablePart(1, 0, new int[]{8, 0, 0, 8, 0, 16});
        assertEquals(0, named(List.of(trunk, signal), "cable splice"), "a signal cable ending on a power cable isn't joined to it");
    }

    @Test
    void oldPipeBranchesShowAJunction() {
        var a = new PipePart(0, new int[]{0, 8, 8, 8, 8, 8});
        var b = new PipePart(0, new int[]{8, 8, 8, 16, 8, 8});
        var c = new PipePart(0, new int[]{8, 8, 8, 8, 16, 8});
        assertEquals(1, named(List.of(a, b, c), "pipe junction (old)"));
        assertEquals(0, named(List.of(a, b, c, new JunctionPart(8, 8, 8, 0)), "pipe junction (old)"), "a junction part draws itself");
        assertEquals(1, named(List.of(a, b, c, new JunctionPart(8, 8, 8, 0)), "pipe junction"));
        assertEquals(0, named(List.of(a, b), "pipe junction (old)"), "two ends: a straight run, no junction");
    }
}
