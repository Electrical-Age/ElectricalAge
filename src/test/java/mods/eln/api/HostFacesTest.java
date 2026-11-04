package mods.eln.api;

import mods.eln.api.impl.Faces;
import mods.eln.api.impl.HostFaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A hosted block's faces: its facing property's direction is FRONT, no facing is north, and on a horizontal facing
 * UP and DOWN are the world's while LEFT and RIGHT are as seen by someone facing the front.
 */
public class HostFacesTest {
    static net.minecraft.core.Direction on(RelativeFace f, net.minecraft.core.Direction facing) {
        return Faces.resolve(f, HostFaces.front(facing)).toEnumFacing();
    }

    @Test
    void frontIsTheFacingAndBackItsOpposite() {
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            assertEquals(d, on(RelativeFace.FRONT, d));
            assertEquals(d.getOpposite(), on(RelativeFace.BACK, d));
        }
    }

    @Test
    void noFacingIsNorth() {
        assertEquals(mods.eln.misc.Direction.ZN, HostFaces.front((net.minecraft.core.Direction) null));
        assertEquals(net.minecraft.core.Direction.NORTH, on(RelativeFace.FRONT, null));
        assertEquals(net.minecraft.core.Direction.WEST, on(RelativeFace.RIGHT, null));
    }

    @Test
    void horizontalFacingsKeepUpAndSeeLeftAndRightFromTheFront() {
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            assertEquals(net.minecraft.core.Direction.UP, on(RelativeFace.UP, d));
            assertEquals(net.minecraft.core.Direction.DOWN, on(RelativeFace.DOWN, d));
            // facing the front means looking toward d's opposite: your right hand points d turned counter-clockwise
            assertEquals(d.getCounterClockWise(), on(RelativeFace.RIGHT, d));
            assertEquals(d.getClockWise(), on(RelativeFace.LEFT, d));
        }
    }

    @Test
    void verticalFacingsUseEasFrame() {
        assertEquals(net.minecraft.core.Direction.EAST, on(RelativeFace.UP, net.minecraft.core.Direction.UP));
        assertEquals(net.minecraft.core.Direction.WEST, on(RelativeFace.DOWN, net.minecraft.core.Direction.UP));
        assertEquals(net.minecraft.core.Direction.EAST, on(RelativeFace.UP, net.minecraft.core.Direction.DOWN));
    }
}
