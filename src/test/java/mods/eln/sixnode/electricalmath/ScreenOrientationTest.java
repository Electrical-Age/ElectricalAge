package mods.eln.sixnode.electricalmath;

import mods.eln.gl.GL11;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The in-world graphs read the right way on every face and turn (AdventurAgent 2026-10-31: on a wall the processor's
 * tiny screen had its newest points on the left). The transforms are the renders' own: SixNodeRender's face turn, then
 * the device's (the processor: front.left() on a floor or ceiling; the loggers: front on a floor, ceiling or wall).
 */
class ScreenOrientationTest {
    /** The matrix a device on side with front draws with (world-aligned base, as a block's draw). */
    static Matrix4f device(Direction side, Runnable turn) {
        GlFrame.open(new Matrix4f(), null);
        try {
            GL11.glTranslatef(.5f, .5f, .5f);
            side.glRotateXnRef();
            GL11.glTranslatef(-0.5f, 0f, 0f);
            turn.run();
            return GL11.currentMatrix();
        } finally {
            GlFrame.close();
        }
    }

    static Vector3f dir(Matrix4f m, float x, float y, float z) {
        return m.transformDirection(new Vector3f(x, y, z)).normalize();
    }

    @Test
    void theProcessorsScreenReadsNewestOnTheRightEverywhere() {
        for (Direction side : Direction.values())
            for (LRDU front : LRDU.values()) {
                Matrix4f m = device(side, () -> {
                    if (side.isY()) front.left().glRotateOnX();
                });
                boolean[] o = ElectricalMathDescriptor.screenOrientation(m);
                // where the texture's right (u up) and down (v up) point in the world, as drawGraph lays its corners
                Vector3f right = dir(m, 0, 0, o[0] ? 1 : -1), down = dir(m, 0, o[1] ? -1 : 1, 0), normal = dir(m, 1, 0, 0);
                Vector3f up = new Vector3f(down).negate();
                String where = side + " " + front;
                // seen from in front (looking against the normal), right x up points at the viewer: not mirrored
                assertTrue(new Vector3f(right).cross(up).dot(normal) > 0.99f, where + ": mirrored");
                // on a wall, upright, newest on the right
                if (!side.isY()) assertTrue(up.y > 0.99f, where + ": not upright (up " + up + ")");
            }
    }

    static Vector3f vec(Direction d) {
        int[] v = new int[3];
        d.applyTo(v, 1);
        return new Vector3f(v[0], v[1], v[2]);
    }

    /**
     * On a floor or ceiling, as seen by a player standing at the device's front (where its lamps and labels read from:
     * the player who placed it, looking the way they looked then): the screen's right is their right, its top away from
     * them on a floor and toward them on a ceiling, newest on the right (AdventurAgent 2026-10-31: on a floor it was
     * turned 180°). The front is placed as SixNodeDescriptor.getFrontFromPlace does it; all four fronts.
     */
    @Test
    void onAFloorOrCeilingTheProcessorsScreenReadsFromItsFront() {
        StringBuilder bad = new StringBuilder();
        for (Direction side : new Direction[]{Direction.YN, Direction.YP})
            for (Direction looking : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP}) {
                LRDU front = side.getLRDUGoingTo(looking);
                if (side == Direction.YP) front = front.inverse();
                Matrix4f m = device(side, front.left()::glRotateOnX);
                boolean[] o = ElectricalMathDescriptor.screenOrientation(m);
                Vector3f right = dir(m, 0, 0, o[0] ? 1 : -1), up = dir(m, 0, o[1] ? 1 : -1, 0), normal = dir(m, 1, 0, 0);
                Vector3f away = vec(looking), viewersRight = new Vector3f(away).cross(0, 1, 0);
                Vector3f wantUp = side == Direction.YN ? away : new Vector3f(away).negate();
                // the lamps (model +z of the screen) sit to its left
                Vector3f lamps = dir(m, 0, 0, 1);
                if (right.dot(viewersRight) < 0.99f || up.dot(wantUp) < 0.99f || lamps.dot(viewersRight) > -0.99f
                    || new Vector3f(right).cross(up).dot(normal) < 0.99f)
                    bad.append(side).append(" front ").append(front).append(", looking ").append(looking).append(": right ")
                        .append(right).append(" up ").append(up).append('\n');
            }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theLoggersFramesAreNeverMirrored() {
        // a logger's graph is drawn in its model's frame; a turn (no mirror) keeps it reading as on the floor
        for (Direction side : Direction.values())
            for (LRDU front : LRDU.values()) {
                Matrix4f m = device(side, front::glRotateOnX);
                assertTrue(m.determinant3x3() > 0, side + " " + front + ": mirrored");
            }
    }
}
