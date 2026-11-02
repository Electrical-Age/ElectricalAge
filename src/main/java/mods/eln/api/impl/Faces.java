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

    /** The cable position on a transparent node's face in EA's terms: FLOOR is LRDU.Down, CEILING LRDU.Up; ANY null. */
    public static LRDU lrdu(CablePosition position) {
        return switch (position) {
            case ANY -> null;
            case FLOOR -> LRDU.Down;
            case CEILING -> LRDU.Up;
        };
    }

    /** Does a cable at lrdu meet position? (Any cable meets ANY.) */
    public static boolean at(CablePosition position, LRDU lrdu) {
        return position == CablePosition.ANY || lrdu(position) == lrdu;
    }

    /** The relative face side is on, for an element facing front. */
    public static RelativeFace relative(Direction side, Direction front) {
        for (RelativeFace f : RelativeFace.values()) if (resolve(f, front) == side) return f;
        throw new IllegalStateException("no relative face for " + side + " facing " + front); // (resolve is one-to-one)
    }
}
