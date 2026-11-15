package mods.eln.sixnode.hub;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;

/**
 * Where the hub's four cable slots and six links are on its screen (todo #275: the 1.12 picture was fixed, so on a
 * floor it was turned a quarter against the world for a player facing south).
 * <p>
 * The device is not touched: cable slot i is the edge {@code LRDU} i of the hub's face, which lies toward
 * {@code side.applyLRDU(edge)} in the world, and link i joins the two edges of {@link #linkEdges}. Only the picture
 * turns. The screen shows the hub as the player sees it:
 * <ul>
 * <li>on a wall: top = up, left = the player's left as they look at the wall (the 1.12 picture, which was right
 * there);</li>
 * <li>on a floor: top = the way the player faces (the far side), left = the player's left;</li>
 * <li>on a ceiling: top = behind the player (looking up, the far side is at the bottom of the view), left = the
 * player's left.</li>
 * </ul>
 * Pure, so the server-side tests can ask it which slot a player sees where.
 */
public final class HubSlots {
    private HubSlots() {
    }

    /** The places of the four cable slots round the grid, clockwise from the top. */
    public enum Place {
        TOP, RIGHT, BOTTOM, LEFT;

        public Place opposite() {
            return values()[(ordinal() + 2) % 4];
        }
    }

    /** The two edges that link `id` of the connection grid joins (saved as connectionGrid&lt;id&gt;), or null. */
    public static LRDU[] linkEdges(int id) {
        return switch (id) {
            case 0 -> new LRDU[]{LRDU.Left, LRDU.Down};
            case 1 -> new LRDU[]{LRDU.Right, LRDU.Up};
            case 2 -> new LRDU[]{LRDU.Down, LRDU.Right};
            case 3 -> new LRDU[]{LRDU.Up, LRDU.Left};
            case 4 -> new LRDU[]{LRDU.Left, LRDU.Right};
            case 5 -> new LRDU[]{LRDU.Down, LRDU.Up};
            default -> null;
        };
    }

    /** The horizontal direction on the right hand of someone facing `facing` (horizontal). */
    public static Direction rightOf(Direction facing) {
        return switch (facing) {
            case ZN -> Direction.XP;   // north: east
            case XP -> Direction.ZP;   // east: south
            case ZP -> Direction.XN;   // south: west
            case XN -> Direction.ZN;   // west: north
            default -> throw new IllegalArgumentException("not horizontal: " + facing);
        };
    }

    /**
     * The world direction of the hub's edge shown at `place`, for a hub on face `side` of its block (YN: on the floor)
     * and a player facing `facing` (horizontal; not used on a wall).
     */
    public static Direction world(Direction side, Direction facing, Place place) {
        Direction top, right;
        if (side == Direction.YN) {
            top = facing;
            right = rightOf(facing);
        } else if (side == Direction.YP) {
            top = facing.getInverse();
            right = rightOf(facing);
        } else {
            top = Direction.YP;
            right = rightOf(side);   // (the player looks at the wall: toward `side`)
        }
        return switch (place) {
            case TOP -> top;
            case RIGHT -> right;
            case BOTTOM -> top.getInverse();
            case LEFT -> right.getInverse();
        };
    }

    /** The hub's edge shown at `place`. */
    public static LRDU edge(Direction side, Direction facing, Place place) {
        return side.getLRDUGoingTo(world(side, facing, place));
    }

    /** The cable slot (index in the hub's inventory) shown at `place`. */
    public static int slot(Direction side, Direction facing, Place place) {
        return HubContainer.cableSlotId + edge(side, facing, place).toInt();
    }

    /** Where the hub's `edge` is shown. */
    public static Place place(Direction side, Direction facing, LRDU edge) {
        for (Place p : Place.values()) if (edge(side, facing, p) == edge) return p;
        throw new IllegalStateException("edge " + edge + " of " + side + " has no place");
    }

    /** The shapes a link is drawn in, by the two places it joins. */
    public static final int RIGHT_BOTTOM = 0, LEFT_TOP = 1, LEFT_BOTTOM = 2, RIGHT_TOP = 3, LEFT_RIGHT = 4, TOP_BOTTOM = 5;

    /** The shape joining two places. */
    public static int shape(Place a, Place b) {
        return switch (1 << a.ordinal() | 1 << b.ordinal()) {   // TOP 1, RIGHT 2, BOTTOM 4, LEFT 8
            case 2 | 4 -> RIGHT_BOTTOM;
            case 8 | 1 -> LEFT_TOP;
            case 8 | 4 -> LEFT_BOTTOM;
            case 2 | 1 -> RIGHT_TOP;
            case 8 | 2 -> LEFT_RIGHT;
            case 1 | 4 -> TOP_BOTTOM;
            default -> throw new IllegalArgumentException(a + " and " + b);
        };
    }

    /** The shape link `id` is drawn in: between the places of its two edges. */
    public static int shape(Direction side, Direction facing, int id) {
        LRDU[] e = linkEdges(id);
        return shape(place(side, facing, e[0]), place(side, facing, e[1]));
    }

    /**
     * The slot id in the layout file (gui/hub.json) that marks `place`. The file keeps the 1.12 picture (slot 3 on
     * top, 0 right, 2 bottom, 1 left), which is how the server's MenuSpec has them; the screen moves each slot to the
     * place its edge is seen at.
     */
    public static int layoutId(Place place) {
        return switch (place) {
            case TOP -> 3;
            case RIGHT -> 0;
            case BOTTOM -> 2;
            case LEFT -> 1;
        };
    }

    /** "north", "up"...: the world side for a slot's tooltip. */
    public static String worldName(Direction d) {
        return switch (d) {
            case XN -> "west";
            case XP -> "east";
            case YN -> "down";
            case YP -> "up";
            case ZN -> "north";
            case ZP -> "south";
        };
    }
}
