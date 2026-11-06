package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;

/**
 * Wire-drawing machines (Baughn 2026-11-06, Wires stage 4): a painted cabinet with a control panel on its front; on
 * top, a copper rod spool at the back on two uprights, the drawing die block in the middle, and at the front a capstan
 * wound with finished (rubber-sheathed) wire. The wire runs from the spool through the die to the capstan. The power
 * terminal plate sits low on the back face. Two models, alike but for the paint: wiredrawer48 (green) and
 * wiredrawer230 (blue). Drawn by WireDrawingRender through Obj3D. Front XN (x = 0).
 */
public final class WireDrawer {
    private WireDrawer() {
    }

    /** The cabinet's top (px). */
    static final double TOP = 6.0;
    /** The wire's height over the top, and the spool's axis. */
    static final double WIRE_Y = TOP + 2.2, SPOOL_X = 12.0, SPOOL_Y = TOP + 4.6;

    static List<Mesh> machine() {
        List<Mesh> m = new ArrayList<>();
        // base and cabinet
        m.add(box(0.5, 0, 0.5, 15.5, 1.0, 15.5, "darksteel"));
        m.add(box(1.0, 1.0, 1.0, 15.0, TOP, 15.0, "paint"));
        m.add(box(0.9, 2.0, 4.0, 1.0, 5.0, 12.0, "dials", "w").free("the control panel, a decal 0.1 px proud of the cabinet"));
        // the rod spool at the back: two uprights, an axle, a copper drum between steel flanges
        for (double z0 : new double[]{1.5, 13.5}) m.add(box(SPOOL_X - 0.6, TOP, z0, SPOOL_X + 0.6, SPOOL_Y + 0.7, z0 + 1.0, "darksteel"));
        m.add(prism('z', SPOOL_X, SPOOL_Y, 0.45, 1.3, 14.7, "steel", 8));
        m.add(prism('z', SPOOL_X, SPOOL_Y, 3.6, 3.0, 3.5, "steel", 16));
        m.add(prism('z', SPOOL_X, SPOOL_Y, 3.6, 12.5, 13.0, "steel", 16));
        m.add(prism('z', SPOOL_X, SPOOL_Y, 2.9, 3.5, 12.5, "copper", 16));
        // the die block in the middle, with the die's brass ring on its back face
        m.add(box(6.5, TOP, 6.0, 8.5, TOP + 3.5, 10.0, "steel"));
        m.add(tube('x', WIRE_Y, 8.0, 0.4, 1.0, 8.5, 8.8, "brass", 10, Double.NaN, true));
        // the capstan at the front, wound with finished wire
        m.add(prism('y', 3.5, 8.0, 2.2, TOP, TOP + 3.6, "steel", 16));
        m.add(tube('y', 3.5, 8.0, 2.0, 2.7, TOP + 1.2, TOP + 3.0, "rubber", 16, Double.NaN, false));
        m.add(prism('y', 3.5, 8.0, 2.6, TOP + 3.6, TOP + 3.9, "darksteel", 16));
        // the wire: bare copper from the spool's underside to the die, sheathed from the die to the capstan
        m.add(box(8.5, WIRE_Y - 0.25, 7.75, SPOOL_X, WIRE_Y + 0.25, 8.25, "copper"));
        m.add(box(5.4, WIRE_Y - 0.3, 7.7, 6.5, WIRE_Y + 0.3, 8.3, "rubber"));
        // the terminal plate, low on the back face, recessed 0.1 px; its studs stand 0.1 px proud
        m.add(box(15.0, 1.0, 4.5, 15.9, 5.0, 11.5, "steel"));
        m.add(box(15.0, 5.0, 4.5, 15.9, 5.5, 11.5, "darksteel"));
        for (double z : new double[]{5.75, 8.0, 10.25}) m.add(box(15.5, 2.0, z - 0.5, 16.0, 3.0, z + 0.5, "copper"));
        return m;
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        for (String[] v : new String[][]{{"wiredrawer48", "paint_green"}, {"wiredrawer230", "paint_blue"}}) {
            NodeModel n = new NodeModel(v[0], v[1]);
            n.part("main", machine());
            out.add(n);
        }
        return out;
    }
}
