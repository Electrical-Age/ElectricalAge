package mods.eln.api.impl;

import mods.eln.api.CablePosition;
import mods.eln.api.RelativeFace;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;

/**
 * RelativeFace against an element's front, in EA's own terms: FRONT is front, BACK front.back(), LEFT front.left(),
 * RIGHT front.right(), UP front.up(), DOWN front.down(). A one-to-one map for each of the six fronts (for a front
 * of YP or YN, EA's up() is XP).
 */
public final class Faces {
    private Faces() {
    }

    /** The block face a relative face is on, for an element facing front. */
    public static Direction resolve(RelativeFace face, Direction front) {
        return switch (face) {
            case FRONT -> front;
            case BACK -> front.back();
            case LEFT -> front.left();
            case RIGHT -> front.right();
            case UP -> front.up();
            case DOWN -> front.down();
        };
    }

    /** The edge of a transparent node's face a position is on, in EA's terms: the floor is LRDU.Down, the ceiling
     *  LRDU.Up; ANY null. */
    public static LRDU lrdu(CablePosition position) {
        if (position.floor()) return LRDU.Down;
        if (position.ceiling()) return LRDU.Up;
        return null;
    }

    /** A pin's place along its edge, voxels toward the left as seen facing the face from outside: 4, 0 or -4. */
    public static int leftward(CablePosition position) {
        return switch (position) {
            case FLOOR_LEFT, CEILING_LEFT -> 4;
            case FLOOR_RIGHT, CEILING_RIGHT -> -4;
            default -> 0;
        };
    }

    /** Does a plain cable at lrdu meet position? (Any cable meets ANY; none meets a side pin.) */
    public static boolean at(CablePosition position, LRDU lrdu) {
        return position == CablePosition.ANY || !position.side() && lrdu(position) == lrdu;
    }

    /** The relative face side is on, for an element facing front. */
    public static RelativeFace relative(Direction side, Direction front) {
        for (RelativeFace f : RelativeFace.values()) if (resolve(f, front) == side) return f;
        throw new IllegalStateException("no relative face for " + side + " facing " + front); // (resolve is one-to-one)
    }
}
