package mods.eln.node.voxel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The chisel brush geometry (ChiselBrush.box) for every face axis, size, shape and hit voxel; brush validation. */
public class ChiselBrushTest {

    @Test
    void everyBoxIsInsideTheBlockAndHoldsTheVoxelHit() {
        for (var shape : ChiselBrush.Shape.values())
            for (int size : ChiselBrush.SIZES)
                for (int axis = 0; axis < 3; axis++)
                    for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                        int[] b = ChiselBrush.box(shape, size, axis, x, y, z), v = {x, y, z};
                        for (int a = 0; a < 3; a++) {
                            assertTrue(b[a] >= 0 && b[a + 3] <= 16 && b[a] < b[a + 3], shape + " " + size + " axis " + axis);
                            assertTrue(b[a] <= v[a] && v[a] < b[a + 3], "holds the voxel hit");
                        }
                    }
    }

    @Test
    void shapesCoverTheirVolumes() {
        for (int size : ChiselBrush.SIZES)
            for (int axis = 0; axis < 3; axis++) {
                assertEquals(size * size * size, ChiselBrush.volume(ChiselBrush.box(ChiselBrush.Shape.CUBE, size, axis, 5, 9, 14)));
                assertEquals(size * size, ChiselBrush.volume(ChiselBrush.box(ChiselBrush.Shape.FLAT, size, axis, 5, 9, 14)));
                assertEquals(size * size * 16, ChiselBrush.volume(ChiselBrush.box(ChiselBrush.Shape.TUNNEL, size, axis, 5, 9, 14)));
            }
    }

    @Test
    void flatIsOneLayerAtTheHitDepthAndGridAlignedAcross() {
        // the west or east face (x axis), hit at voxel (15, 5, 9), size 4: the layer x = 15, y 4..8, z 8..12
        assertArrayEquals(new int[]{15, 4, 8, 16, 8, 12}, ChiselBrush.box(ChiselBrush.Shape.FLAT, 4, 0, 15, 5, 9));
        // the top face (y axis), hit at (3, 15, 12), size 8: y = 15, x 0..8, z 8..16
        assertArrayEquals(new int[]{0, 15, 8, 8, 16, 16}, ChiselBrush.box(ChiselBrush.Shape.FLAT, 8, 1, 3, 15, 12));
        // the north face (z axis), size 2, a layer one voxel in (a hole's floor): z = 1
        assertArrayEquals(new int[]{6, 10, 1, 8, 12, 2}, ChiselBrush.box(ChiselBrush.Shape.FLAT, 2, 2, 7, 11, 1));
    }

    @Test
    void tunnelGoesThroughTheWholeBlockAlongTheNormal() {
        assertArrayEquals(new int[]{0, 4, 8, 16, 8, 12}, ChiselBrush.box(ChiselBrush.Shape.TUNNEL, 4, 0, 15, 5, 9));
        assertArrayEquals(new int[]{0, 0, 8, 8, 16, 16}, ChiselBrush.box(ChiselBrush.Shape.TUNNEL, 8, 1, 3, 0, 12));
        assertArrayEquals(new int[]{7, 11, 0, 8, 12, 16}, ChiselBrush.box(ChiselBrush.Shape.TUNNEL, 1, 2, 7, 11, 1));
    }

    @Test
    void cubeIsTheGridAlignedCubeAsBefore() {
        for (int axis = 0; axis < 3; axis++)
            assertArrayEquals(new int[]{4, 8, 12, 8, 12, 16}, ChiselBrush.box(ChiselBrush.Shape.CUBE, 4, axis, 7, 9, 15));
    }

    @Test
    void onlyListedSizesAndKnownShapesAreValid() {
        for (int size : ChiselBrush.SIZES)
            for (int shape = 0; shape < 3; shape++) assertTrue(ChiselBrush.valid(size, shape));
        for (int size : new int[]{-1, 0, 3, 5, 16, 255}) assertFalse(ChiselBrush.valid(size, 0), "size " + size);
        for (int shape : new int[]{-1, 3, 127}) assertFalse(ChiselBrush.valid(4, shape), "shape " + shape);
        assertThrows(IllegalArgumentException.class, () -> ChiselBrush.box(ChiselBrush.Shape.CUBE, 3, 0, 0, 0, 0));
        assertEquals(ChiselBrush.Shape.TUNNEL, ChiselBrush.Shape.byId(2));
        assertNull(ChiselBrush.Shape.byId(3));
    }
}
