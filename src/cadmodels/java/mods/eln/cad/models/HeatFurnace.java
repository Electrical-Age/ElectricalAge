package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;

/**
 * The Stone Heat Furnace as a coal oven (Baughn 2026-11-03, was the 1.12 OBJ with an opening drawer): a squat firebrick
 * body on a cast-iron plinth, held by cast-iron corner posts under a cast-iron top plate, a firebrick crown, and a short
 * flue rising from the crown to an iron pipe's flange exactly at the top face's centre (x 8, z 8, top at y 16; where a
 * pipe will take the flue gas). On the front (XN, x = 0) a cast-iron firebox door with a glass window, hinged on its
 * left, with a latch on its right, and a louvred ash door below it. The thermal contact is a copper pad on the back at
 * the floor, the control signal inputs are plates low on both flanks.
 * <p>
 * Parts: main; glow (the fire bed behind the window, white, its whole height: HeatFurnaceRender scales it about its foot
 * by the fire's level and tints it by the power, unlit); glass (the window's pane, drawn see-through over the glow).
 */
public final class HeatFurnace {
    private HeatFurnace() {
    }

    /** The firebox window: its y and z extents (px; HeatFurnaceDescriptor scales the glow about WIN_Y0). */
    static final double WIN_Y0 = 5.25, WIN_Y1 = 8.75, WIN_Z0 = 5.25, WIN_Z1 = 10.75;
    /** The firebox door: front face x, its back on the brick at x = 1.5, its y and z extents. */
    static final double DOOR_X = 0.8, BRICK_X = 1.5, DOOR_Y0 = 4.25, DOOR_Y1 = 9.75, DOOR_Z0 = 4.0, DOOR_Z1 = 12.0;

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        // the body: a cast-iron plinth (its bottom on the floor, not drawn), firebrick walls, cast-iron corner posts and
        // top plate, a firebrick crown
        m.add(box(1, 0, 1, 15, 1, 15, "castiron", "nsewu"));
        m.add(box(BRICK_X, 1, BRICK_X, 16 - BRICK_X, 10.5, 16 - BRICK_X, "firebrick", "nsew"));
        for (double[] xz : new double[][]{{1, 1}, {13.8, 1}, {1, 13.8}, {13.8, 13.8}})
            m.add(box(xz[0], 1, xz[1], xz[0] + 1.2, 10.5, xz[1] + 1.2, "castiron", "nsew"));
        m.add(box(1, 10.5, 1, 15, 11.5, 15, "castiron"));
        // tie rods between the posts along the flanks and the back, nuts on the posts' outer faces (a brick oven's buckstays)
        for (double z : new double[]{1.3, 14.7})
            for (double y : new double[]{2.75, 8.75}) {
                m.add(prism('x', y, z, 0.22, 2.2, 13.8, "darksteel", 6, false, false));
                m.add(prism('x', y, z, 0.4, 0.6, 1.0, "darksteel", 6, true, false));
                m.add(prism('x', y, z, 0.4, 15.0, 15.4, "darksteel", 6, false, true));
            }
        m.add(prism('z', 14.7, 7.75, 0.22, 2.2, 13.8, "darksteel", 6, false, false));
        m.add(prism('z', 14.7, 7.75, 0.4, 0.6, 1.0, "darksteel", 6, true, false));
        m.add(prism('z', 14.7, 7.75, 0.4, 15.0, 15.4, "darksteel", 6, false, true));
        m.add(box(3, 11.5, 3, 13, 12.25, 13, "firebrick", "nsewu"));
        // the flue: a cast-iron hood on the crown tapering into a short stack, a band round it, and an iron pipe's flange
        // at the top face's centre (ShaftKit.pipeFlange: r 1.5 at y 15..16, as the pipe renderer draws an iron pipe's end)
        m.add(prism('y', 8, 8, 3.2, 12.25, 12.5, "castiron", 16, false, true));
        m.addAll(OilKit.boltRing('y', 8, 8, 3.6, 12.25, 12.5, 8, "brass", 0.22, 0));
        m.add(prism('y', 8, 8, 2.6, 12.5, 13.25, "castiron", 16, 1.3, false, true, Double.NaN));
        m.add(prism('y', 8, 8, 1.1, 13.0, 15.0, "castiron", 12, false, false));
        m.add(tube('y', 8, 8, 1.0, 1.3, 13.9, 14.2, "darksteel", 12, Double.NaN, false));
        m.addAll(ShaftKit.pipeFlange("y16", 8, 8, "small", 0.5, "steel", -1));
        // the firebox door: a cast-iron frame round the window (bars meeting edge to edge), two glazing bars across it,
        // the soot-black firebox behind (seen when the fire is out), hinges on the left, a latch on the right, four bolts
        m.add(box(DOOR_X, DOOR_Y0, DOOR_Z0, BRICK_X, WIN_Y0, DOOR_Z1, "castiron", "nswud"));
        m.add(box(DOOR_X, WIN_Y1, DOOR_Z0, BRICK_X, DOOR_Y1, DOOR_Z1, "castiron", "nswud"));
        m.add(box(DOOR_X, WIN_Y0, DOOR_Z0, BRICK_X, WIN_Y1, WIN_Z0, "castiron", "nsw"));
        m.add(box(DOOR_X, WIN_Y0, WIN_Z1, BRICK_X, WIN_Y1, DOOR_Z1, "castiron", "nsw"));
        for (double z : new double[]{7.05, 8.95})
            m.add(box(0.9, WIN_Y0, z - 0.15, 1.2, WIN_Y1, z + 0.15, "castiron", "nsw"));
        m.add(box(1.35, WIN_Y0, WIN_Z0, BRICK_X, WIN_Y1, WIN_Z1, "soot", "w"));
        for (double y : new double[]{4.75, 8.25})
            m.add(prism('y', 1.0, DOOR_Z0 - 0.3, 0.4, y, y + 1.0, "darksteel", 8));
        m.add(box(0.5, 6.1, 11.05, DOOR_X, 6.6, 11.55, "steel"));
        m.add(box(0.5, 7.9, 11.05, DOOR_X, 8.4, 11.55, "steel"));
        m.add(box(0.2, 5.9, 11.0, 0.5, 8.6, 11.6, "steel"));
        for (double[] yz : new double[][]{{4.75, 4.5}, {4.75, 11.5}, {9.25, 4.5}, {9.25, 11.5}})
            m.add(prism('x', yz[0], yz[1], 0.25, 0.6, DOOR_X, "rivet", 6, true, false));
        // the ash door below: cast iron with a louvred vent and a brass knob
        m.add(box(1.0, 1.75, 5.0, BRICK_X, 3.75, 11.0, "castiron", "nswud"));
        m.add(box(0.9, 2.25, 6.0, 1.0, 3.25, 10.0, "louvre", "nswud"));
        m.add(prism('x', 2.75, 10.5, 0.3, 0.5, 1.0, "brass", 8, true, false));
        // the back: the thermal contact at the floor (OilKit.heatPad, turned to the x = 16 face)
        m.addAll(OilKit.turned(OilKit.heatPad(), 2));
        // the flanks: the control signal inputs, on cast-iron blocks set into the brick
        m.add(box(6, 0, 14.0, 10, 4.2, 15.2, "castiron", "nsewu"));
        m.add(box(6, 0, 0.8, 10, 4.2, 2.0, "castiron", "nsewu"));
        m.addAll(OilKit.turned(OilKit.signalPlate(), 1));
        m.addAll(OilKit.turned(OilKit.signalPlate(), 3));
        return m;
    }

    /** The fire bed behind the window, its whole height (white: tinted as drawn; scaled about WIN_Y0 by the fire's level). */
    static List<Mesh> glow() {
        return List.of(box(1.25, WIN_Y0, WIN_Z0, 1.35, WIN_Y1, WIN_Z1, "glow", "w"));
    }

    /** The window's pane, in front of the glow (drawn see-through). */
    static List<Mesh> glass() {
        return List.of(box(1.05, WIN_Y0, WIN_Z0, 1.15, WIN_Y1, WIN_Z1, "glass", "w"));
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("stonefurnace", "paint_plain");
        n.part("main", main());
        n.part("glow", glow());
        n.part("glass", glass());
        n.hitbox(0.5, 0, 1, 16, 11.5, 15,     // the body, its doors and the back pad
            3, 11.5, 3, 13, 12.25, 13,          // the crown
            6, 12.25, 6, 10, 16, 10);           // the flue
        return List.of(n);
    }
}
