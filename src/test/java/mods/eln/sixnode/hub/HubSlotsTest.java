package mods.eln.sixnode.hub;

import com.google.gson.JsonParser;
import mods.eln.gui.layout.Layout;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.sixnode.hub.HubSlots.Place;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static mods.eln.misc.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The hub's screen against the world (todo #275): which cable slot and which link the player sees where, on every
 * face and for every way the player can face; and that the device's own slot -> world side table, which saved hubs
 * depend on, is what it was.
 */
class HubSlotsTest {
    static final Direction[] HORIZONTAL = {ZN, XP, ZP, XN};

    static int[] vec(Direction d) {
        int[] v = new int[3];
        d.applyTo(v, 1);
        return v;
    }

    /** a x b: with forward a and up b it is the viewer's right (x east, y up, z south is right-handed). */
    static int[] cross(int[] a, int[] b) {
        return new int[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    /** Saved data: the cable in inventory slot i of a hub on `side` connects toward this world direction. */
    @Test
    void slotToWorldSideOfThePlacedDeviceIsUnchanged() {
        Object[][] table = {
            // side, slot 0 (Left), slot 1 (Right), slot 2 (Down), slot 3 (Up)
            {YN, ZP, ZN, XN, XP},
            {YP, ZN, ZP, XN, XP},
            {XN, ZN, ZP, YN, YP},
            {XP, ZP, ZN, YN, YP},
            {ZN, XP, XN, YN, YP},
            {ZP, XN, XP, YN, YP},
        };
        for (Object[] row : table) {
            Direction side = (Direction) row[0];
            for (int slot = 0; slot < 4; slot++)
                assertEquals(row[1 + slot], side.applyLRDU(LRDU.fromInt(slot)), side + " slot " + slot);
        }
    }

    /** Saved data: connectionGrid<id> joins these two edges. */
    @Test
    void linkTableIsUnchanged() {
        LRDU[][] want = {{LRDU.Left, LRDU.Down}, {LRDU.Right, LRDU.Up}, {LRDU.Down, LRDU.Right}, {LRDU.Up, LRDU.Left},
            {LRDU.Left, LRDU.Right}, {LRDU.Down, LRDU.Up}};
        for (int id = 0; id < 6; id++) {
            assertArrayEquals(want[id], HubSlots.linkEdges(id), "link " + id);
            assertArrayEquals(want[id], HubElement.connectionIdToSide(id), "element link " + id);
        }
        assertNull(HubSlots.linkEdges(6));
    }

    /** The report: a hub on a floor, the player facing south (yaw 0). */
    @Test
    void floorHubSeenFacingSouth() {
        assertEquals(ZP, HubSlots.world(YN, ZP, Place.TOP));
        assertEquals(ZN, HubSlots.world(YN, ZP, Place.BOTTOM));
        assertEquals(XP, HubSlots.world(YN, ZP, Place.LEFT));
        assertEquals(XN, HubSlots.world(YN, ZP, Place.RIGHT));
        // ... which are these inventory slots (south 0, north 1, west 2, east 3 on a floor)
        assertEquals(0, HubSlots.slot(YN, ZP, Place.TOP));
        assertEquals(1, HubSlots.slot(YN, ZP, Place.BOTTOM));
        assertEquals(3, HubSlots.slot(YN, ZP, Place.LEFT));
        assertEquals(2, HubSlots.slot(YN, ZP, Place.RIGHT));
    }

    @Test
    void floorTopIsTheWayThePlayerFaces() {
        for (Direction f : HORIZONTAL) assertEquals(f, HubSlots.world(YN, f, Place.TOP), "facing " + f);
        // north up when facing north: the map picture
        assertEquals(ZN, HubSlots.world(YN, ZN, Place.TOP));
        assertEquals(XN, HubSlots.world(YN, ZN, Place.LEFT));
        assertEquals(XP, HubSlots.world(YN, ZN, Place.RIGHT));
        assertEquals(ZP, HubSlots.world(YN, ZN, Place.BOTTOM));
    }

    @Test
    void ceilingTopIsBehindThePlayer() {
        for (Direction f : HORIZONTAL) assertEquals(f.getInverse(), HubSlots.world(YP, f, Place.TOP), "facing " + f);
        // facing north and looking up: west is still on the left
        assertEquals(XN, HubSlots.world(YP, ZN, Place.LEFT));
        assertEquals(ZP, HubSlots.world(YP, ZN, Place.TOP));
    }

    @Test
    void wallTopIsUpWhateverThePlayerFaces() {
        for (Direction side : HORIZONTAL)
            for (Direction f : HORIZONTAL) {
                assertEquals(YP, HubSlots.world(side, f, Place.TOP), side + " facing " + f);
                assertEquals(YN, HubSlots.world(side, f, Place.BOTTOM), side + " facing " + f);
            }
        // on the north wall of a room (the player looks north at it): west on the left, east on the right
        assertEquals(XN, HubSlots.world(ZN, ZN, Place.LEFT));
        assertEquals(XP, HubSlots.world(ZN, ZN, Place.RIGHT));
    }

    /** Never mirrored: looking at the face the hub is on, with the top place up in the view, right is on the right. */
    @Test
    void thePictureIsTheViewOnEveryFace() {
        for (Direction side : Direction.values())
            for (Direction f : HORIZONTAL) {
                String what = side + " facing " + f;
                Direction top = HubSlots.world(side, f, Place.TOP), right = HubSlots.world(side, f, Place.RIGHT);
                assertArrayEquals(vec(right), cross(vec(side), vec(top)), what);
                assertEquals(top.getInverse(), HubSlots.world(side, f, Place.BOTTOM), what);
                assertEquals(right.getInverse(), HubSlots.world(side, f, Place.LEFT), what);
                // every place is an edge of the face, each slot is shown once, and place() is the inverse
                Set<Integer> slots = new HashSet<>();
                for (Place p : Place.values()) {
                    LRDU e = HubSlots.edge(side, f, p);
                    assertNotNull(e, what + " " + p);
                    assertEquals(HubSlots.world(side, f, p), side.applyLRDU(e), what + " " + p);
                    assertEquals(p, HubSlots.place(side, f, e), what + " " + p);
                    slots.add(HubSlots.slot(side, f, p));
                }
                assertEquals(Set.of(0, 1, 2, 3), slots, what);
            }
    }

    /** A link is drawn between the places of the two slots it joins. */
    @Test
    void linksAreDrawnBetweenTheirSlots() {
        Place[][] ends = new Place[6][];
        ends[HubSlots.RIGHT_BOTTOM] = new Place[]{Place.RIGHT, Place.BOTTOM};
        ends[HubSlots.LEFT_TOP] = new Place[]{Place.LEFT, Place.TOP};
        ends[HubSlots.LEFT_BOTTOM] = new Place[]{Place.LEFT, Place.BOTTOM};
        ends[HubSlots.RIGHT_TOP] = new Place[]{Place.RIGHT, Place.TOP};
        ends[HubSlots.LEFT_RIGHT] = new Place[]{Place.LEFT, Place.RIGHT};
        ends[HubSlots.TOP_BOTTOM] = new Place[]{Place.TOP, Place.BOTTOM};
        for (Direction side : Direction.values())
            for (Direction f : HORIZONTAL) {
                String what = side + " facing " + f;
                Set<Integer> shapes = new HashSet<>();
                for (int id = 0; id < 6; id++) {
                    int shape = HubSlots.shape(side, f, id);
                    shapes.add(shape);
                    LRDU[] e = HubSlots.linkEdges(id);
                    assertEquals(EnumSet.of(ends[shape][0], ends[shape][1]),
                        EnumSet.of(HubSlots.place(side, f, e[0]), HubSlots.place(side, f, e[1])), what + " link " + id);
                    assertEquals(id >= 4, shape >= 4, what + " link " + id + ": a straight stays a straight");
                }
                assertEquals(6, shapes.size(), what);
            }
    }

    /** Where the 1.12 picture was right it is kept: on walls, and on a floor seen facing east. */
    @Test
    void theOldPictureIsKeptWhereItWasRight() {
        for (int id = 0; id < 6; id++) {
            for (Direction side : HORIZONTAL) assertEquals(id, HubSlots.shape(side, ZN, id), side + " link " + id);
            assertEquals(id, HubSlots.shape(YN, XP, id), "floor facing east, link " + id);
        }
        for (Place p : Place.values()) {
            for (Direction side : HORIZONTAL) assertEquals(HubSlots.layoutId(p), HubSlots.slot(side, ZP, p), side + " " + p);
            assertEquals(HubSlots.layoutId(p), HubSlots.slot(YN, XP, p), "floor facing east " + p);
        }
    }

    /** The layout file's slot ids are where layoutId says: the screen takes each place's position from them. */
    @Test
    void layoutFileHasThePlaces() throws Exception {
        Path file = Path.of(System.getProperty("eln.guiDir", "src/main/resources/assets/eln/gui"), "hub.json");
        Layout.Result r = Layout.layout(JsonParser.parseString(Files.readString(file)).getAsJsonObject(), Layout.FIXED);
        Layout.Rect grid = r.get("canvas", "grid");
        Layout.Rect top = r.get("slot", "" + HubSlots.layoutId(Place.TOP)), right = r.get("slot", "" + HubSlots.layoutId(Place.RIGHT));
        Layout.Rect bottom = r.get("slot", "" + HubSlots.layoutId(Place.BOTTOM)), left = r.get("slot", "" + HubSlots.layoutId(Place.LEFT));
        assertTrue(top.y() + top.h() <= grid.y() && top.x() >= grid.x() && top.right() <= grid.right(), "top " + top);
        assertTrue(bottom.y() >= grid.y() + grid.h() && bottom.x() >= grid.x() && bottom.right() <= grid.right(), "bottom " + bottom);
        assertTrue(left.right() <= grid.x() && left.y() >= grid.y() && left.y() + left.h() <= grid.y() + grid.h(), "left " + left);
        assertTrue(right.x() >= grid.right() && right.y() >= grid.y() && right.y() + right.h() <= grid.y() + grid.h(), "right " + right);
    }

    @Test
    void worldNames() {
        assertEquals("north", HubSlots.worldName(ZN));
        assertEquals("south", HubSlots.worldName(ZP));
        assertEquals("west", HubSlots.worldName(XN));
        assertEquals("east", HubSlots.worldName(XP));
        assertEquals("up", HubSlots.worldName(YP));
        assertEquals("down", HubSlots.worldName(YN));
    }
}
