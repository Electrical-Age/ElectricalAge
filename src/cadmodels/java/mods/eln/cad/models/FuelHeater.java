package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;

/**
 * The Fuel Heat Furnace as a small oil-fired heater (Baughn 2026-11-03, was the 1.12 OBJ "FuelHeater"): a painted steel
 * housing and lid on a dark steel skid, with a cast-iron front plate. On the front (XN, x = 0): a sight
 * port with a glass pane on the left showing the flame, the installed oil burner's mounting flange on the right (the
 * burner gun bolts on there), the power and heat lamps on a small panel above it, and low in the middle an iron pipe's
 * flange for the fuel (ShaftKit.pipeFlange, centre y 4, z 8, its face on x = 0; where a pipe will bring the oil), whose
 * stub runs into the front plate with a copper fuel line up to the burner's mount. On top a short flue to an iron
 * pipe's flange exactly at the top face's centre (x 8, z 8, top at y 16). The thermal contact is a copper pad on the
 * back at the floor, the control signal inputs are plates low on both flanks.
 * <p>
 * Parts: main; burnerbig, burnermedium, burnersmall (the burner gun of each size: a red blower housing, a grille on its
 * intake, a motor on its right, an ignition box on top; FuelHeatFurnaceDescriptor draws the installed one); powerled,
 * heatled (white lamps, tinted as drawn); glow (the flame behind the sight port, white, its whole height: scaled about
 * its foot by the fire's level and tinted by the power, unlit); glass (the pane, drawn see-through over the glow).
 */
public final class FuelHeater {
    private FuelHeater() {
    }

    /** The sight port: its y and z extents (px; FuelHeatFurnaceDescriptor scales the glow about WIN_Y0). */
    static final double WIN_Y0 = 6.5, WIN_Y1 = 10.0, WIN_Z0 = 3.75, WIN_Z1 = 7.25;
    /** The front plate's face (its back on the housing at x = 3.5). */
    static final double PLATE_X = 3.0, BODY_X = 3.5;
    /** The burner's axis (y, z) and the mount flange's face. */
    static final double BY = 8.0, BZ = 10.5, MOUNT_X = 2.6;
    /** The fuel flange's centre on the front face (Baughn: low, centred across). */
    static final double FUEL_Y = 4.0, FUEL_Z = 8.0;

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        // the skid (its bottom on the floor, not drawn), the painted housing and its lid, the cast-iron front plate
        m.add(box(2.5, 0, 1.5, 14.8, 1, 14.5, "darksteel", "nsewu"));
        m.add(box(BODY_X, 1, 2, 14.5, 12.5, 14, "paint", "nsew"));
        m.add(box(3.2, 12.5, 1.7, 14.8, 13.0, 14.3, "paint"));
        m.add(box(PLATE_X, 1, 2.5, BODY_X, 12.0, 13.5, "castiron", "nswu"));
        // a darksteel band round the housing at mid height, and bolts in the front plate's corners
        m.add(box(BODY_X - 0.2, 5.0, 1.8, 14.7, 5.5, 14.2, "darksteel"));
        for (double[] yz : new double[][]{{1.6, 3.1}, {1.6, 12.9}, {11.4, 3.1}, {11.4, 12.9}})
            m.add(prism('x', yz[0], yz[1], 0.25, PLATE_X - 0.2, PLATE_X, "rivet", 6, true, false));
        // the flue: a cast-iron hood on the lid tapering into a short stack, a band round it, and an iron pipe's flange
        // at the top face's centre (ShaftKit.pipeFlange: r 1.5 at y 15..16, as the pipe renderer draws an iron pipe's end)
        m.add(prism('y', 8, 8, 2.8, 13.0, 13.25, "castiron", 16, false, true));
        m.addAll(OilKit.boltRing('y', 8, 8, 3.2, 13.0, 13.25, 8, "brass", 0.2, 0));
        m.add(prism('y', 8, 8, 2.3, 13.25, 13.9, "castiron", 16, 1.3, false, true, Double.NaN));
        m.add(prism('y', 8, 8, 1.1, 13.7, 15.0, "castiron", 12, false, false));
        m.add(tube('y', 8, 8, 1.0, 1.3, 14.2, 14.5, "darksteel", 12, Double.NaN, false));
        m.addAll(ShaftKit.pipeFlange("y16", 8, 8, "small", 0.5, "steel", -1));
        // the fuel: an iron pipe's flange low on the front, its stub into the front plate, and a copper line from the
        // stub up into the burner's mounting flange
        m.addAll(ShaftKit.pipeFlange("x0", FUEL_Y, FUEL_Z, "small", 2.1, "steel", -1));
        m.add(prism('y', 2.7, 8.4, 0.25, 4.2, 7.3, "copper", 8, false, false));
        // the sight port: a cast-iron frame round the window (bars meeting edge to edge), the soot-black firebox behind
        // (seen when the burner is out), two bolts top and bottom
        m.add(box(2.2, WIN_Y0 - 0.75, WIN_Z0 - 0.75, PLATE_X, WIN_Y0, WIN_Z1 + 0.75, "castiron", "nswud"));
        m.add(box(2.2, WIN_Y1, WIN_Z0 - 0.75, PLATE_X, WIN_Y1 + 0.75, WIN_Z1 + 0.75, "castiron", "nswud"));
        m.add(box(2.2, WIN_Y0, WIN_Z0 - 0.75, PLATE_X, WIN_Y1, WIN_Z0, "castiron", "nsw"));
        m.add(box(2.2, WIN_Y0, WIN_Z1, PLATE_X, WIN_Y1, WIN_Z1 + 0.75, "castiron", "nsw"));
        m.add(box(2.8, WIN_Y0, WIN_Z0, PLATE_X, WIN_Y1, WIN_Z1, "soot", "w"));
        for (double y : new double[]{WIN_Y0 - 0.375, WIN_Y1 + 0.375})
            for (double z : new double[]{WIN_Z0 - 0.375, WIN_Z1 + 0.375})
                m.add(prism('x', y, z, 0.2, 2.0, 2.2, "brass", 6, true, false));
        // the burner's mounting flange (with no burner in, a blank plate with four bolts)
        m.add(prism('x', BY, BZ, 2.6, MOUNT_X, PLATE_X, "castiron", 16, true, false));
        m.addAll(OilKit.boltRing('x', BY, BZ, 2.35, MOUNT_X - 0.2, MOUNT_X, 4, "brass", 0.15, Math.PI / 4));
        // the lamp panel above the burner
        m.add(box(2.7, 10.9, 8.8, PLATE_X, 12.0, 12.8, "darksteel", "nswud"));
        for (double z : new double[]{9.8, 11.8})
            m.add(prism('x', 11.45, z, 0.5, 2.6, 2.7, "steel", 8, true, false));
        // the back: the thermal contact at the floor (OilKit.heatPad, turned to the x = 16 face)
        m.addAll(OilKit.turned(OilKit.heatPad(), 2));
        // the flanks: the control signal inputs, on cast-iron blocks on the housing
        m.add(box(6, 0, 14.0, 10, 4.2, 15.2, "castiron", "nsewu"));
        m.add(box(6, 0, 0.8, 10, 4.2, 2.0, "castiron", "nsewu"));
        m.addAll(OilKit.turned(OilKit.signalPlate(), 1));
        m.addAll(OilKit.turned(OilKit.signalPlate(), 3));
        return m;
    }

    /**
     * A burner gun of radius r reaching out to x0 (bigger burners are wider and longer): its own flange on the mount, a
     * red blower housing, a grille on the intake, a motor out to its right with a fan cover, an ignition box on top.
     */
    static List<Mesh> burner(double r, double x0, int k) {
        // (the checker reads the three burners together though only one is drawn: each size's round pieces are turned by a
        // third of a facet from the others', so no two sizes have faces in one plane)
        double ph = Math.PI / 16 + k * (2 * Math.PI / 16) / 3, pm = Math.PI / 10 + k * (2 * Math.PI / 10) / 3;
        List<Mesh> m = new ArrayList<>();
        m.add(prism('x', BY, BZ, r + 0.2, MOUNT_X - 0.35, MOUNT_X, "darksteel", 16, Double.NaN, true, false, ph));
        m.add(prism('x', BY, BZ, r, x0 + 0.3, MOUNT_X - 0.35, "red", 16, Double.NaN, true, false, ph));
        m.add(prism('x', BY, BZ, r - 0.4, x0, x0 + 0.3, "louvre", 16, Double.NaN, true, false, ph));
        double h0 = x0 + 0.3, h1 = MOUNT_X - 0.35, rm = Math.min(0.45 * r, (h1 - h0) / 2 - 0.05), xm = (h0 + h1) / 2;
        double z0 = BZ + r - 0.3, z1 = BZ + 1.7 * r;
        m.add(prism('z', xm, BY - 0.2, rm, z0, z1, "darksteel", 10, Double.NaN, false, true, pm));
        m.add(prism('z', xm, BY - 0.2, rm + 0.15, z1, z1 + 0.3, "steel", 10, Double.NaN, true, true, pm));
        m.add(box(x0 + 0.5, BY + r - 0.3, BZ - 0.6 * r, MOUNT_X - 0.6 - 0.1 * k, BY + r + 0.45, BZ + 0.4 * r, "darksteel", "nsewu"));
        return m;
    }

    static List<Mesh> led(double z) {
        return List.of(prism('x', 11.45, z, 0.35, 2.4, 2.6, "white", 8, true, false));
    }

    /** The flame behind the sight port, its whole height (white: tinted as drawn; scaled about WIN_Y0 by the fire's level). */
    static List<Mesh> glow() {
        return List.of(box(2.6, WIN_Y0, WIN_Z0, 2.7, WIN_Y1, WIN_Z1, "glow", "w"));
    }

    /** The sight port's pane, in front of the glow (drawn see-through). */
    static List<Mesh> glass() {
        return List.of(box(2.35, WIN_Y0, WIN_Z0, 2.45, WIN_Y1, WIN_Z1, "glass", "w"));
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("fuelheater", "paint_heater");
        n.part("main", main());
        n.part("burnerbig", burner(1.95, 0.3, 0));
        n.part("burnermedium", burner(1.6, 0.7, 1));
        n.part("burnersmall", burner(1.25, 1.1, 2));
        n.part("powerled", led(9.8));
        n.part("heatled", led(11.8));
        n.part("glow", glow());
        n.part("glass", glass());
        n.hitbox(2.0, 0, 0.8, 16, 13, 15.2,     // the housing, its front, the back pad and the flank plates
            0, 2.5, 6.5, 2.0, 5.5, 9.5,          // the fuel flange
            0.3, 5.6, 8.1, 2.6, 10.6, 14.2,      // the burner (the big one)
            5, 13, 5, 11, 16, 11);               // the flue
        return List.of(n);
    }
}
