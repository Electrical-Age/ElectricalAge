package mods.eln.robotics.motion;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** StraightPath: the old axis arithmetic (origin + axis x s), its grade, and along() as its inverse. */
class StraightPathTest {
    final Vec3 o = new Vec3(10.5, 64, -3.5);

    @Test
    void placesAlongItsAxisAsTheCarDid() {
        for (Direction d : Direction.values()) {
            var p = new StraightPath(o, d, 12);
            Vec3 want = o.add(d.getStepX() * 7.25, d.getStepY() * 7.25, d.getStepZ() * 7.25);
            assertEquals(0, p.at(7.25).distanceTo(want), 1e-12, d.toString());
            assertEquals(1, p.tangent(3).length(), 1e-12);
            assertTrue(p.axisAligned());
        }
    }

    @Test
    void climbsOnlyUpOrDown() {
        assertEquals(1, new StraightPath(o, Direction.UP, 5).grade(2));
        assertEquals(-1, new StraightPath(o, Direction.DOWN, 5).grade(2));
        assertEquals(0, new StraightPath(o, Direction.EAST, 5).grade(2));
        assertEquals(0, new StraightPath(o, Direction.NORTH, 5).grade(2));
    }

    @Test
    void alongIsAtsInverseWhereverItIsMeasured() {
        for (Direction d : Direction.values()) {
            var p = new StraightPath(o, d, 32);
            for (double s : new double[]{0, 1.5, 17, 31.99})
                for (double near : new double[]{0, 5, 30})
                    assertEquals(s, p.along(p.at(s), near), 1e-9, d + " s " + s + " near " + near);
            // (off the path sideways, along() is the offset along it alone)
            Vec3 side = d.getAxis() == Direction.Axis.Y ? new Vec3(0.3, 0, 0.2) : new Vec3(0, 0.4, 0);
            assertEquals(4, p.along(p.at(4).add(side), 0), 1e-9);
        }
    }
}
