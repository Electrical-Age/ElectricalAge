package mods.eln.api.impl;

import mods.eln.api.RelativeFace;
import mods.eln.misc.Direction;

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

    /** The relative face side is on, for an element facing front. */
    public static RelativeFace relative(Direction side, Direction front) {
        for (RelativeFace f : RelativeFace.values()) if (resolve(f, front) == side) return f;
        throw new IllegalStateException("no relative face for " + side + " facing " + front); // (resolve is one-to-one)
    }
}
