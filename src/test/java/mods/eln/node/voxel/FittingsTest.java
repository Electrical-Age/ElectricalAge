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

    @Test
    void aRoutedBranchGetsASleeve() {
        // as the kit lays them: a trunk along x on a floor, a branch routed from a point of it out to +z
        var solid = CableRouterTest.floor(0, 5, 0, 3);
        var sp = CableRouterTest.world(solid);
        var t = CableRouter.route(sp, RouteKind.cable("cable", 2, 1), new CableRouter.End(2, 0, 6, 0, -1), List.of(), new CableRouter.End(60, 0, 6, 0, -1), 10_000);
        assertNull(t.error);
        var b = CableRouter.route(sp, RouteKind.cable("cable", 2, 1), new CableRouter.End(24, 0, 6, 0, -1), List.of(), new CableRouter.End(24, 0, 40, 0, -1), 10_000);
        assertNull(b.error);
        java.util.List<VoxelPart> inBlock = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (var r : List.of(t, b))
            for (CableRouter.Piece p : CableRouter.pieces(r.steps, 2, 1))
                if (p.bx() == 1 && p.by() == 0 && p.bz() == 0 && p.face() >= 0) {
                    inBlock.add(new CablePart(1, p.face(), p.points()));
                    sb.append(java.util.Arrays.toString(p.points())).append(' ');
                }
        assertEquals(1, named(inBlock, "cable splice"), "pieces in the block: " + sb);
    }

    @Test
    void threeEndsAtAPointGetASleeve() {
        var a = new CablePart(1, 0, new int[]{8, 0, 0, 8, 0, 8});
        var b = new CablePart(1, 0, new int[]{8, 0, 8, 16, 0, 8});
        var c = new CablePart(1, 0, new int[]{8, 0, 8, 8, 0, 16});
        assertEquals(1, named(List.of(a, b, c), "cable splice"));
    }
}

