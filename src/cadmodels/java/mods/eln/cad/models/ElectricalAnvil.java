package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;

/**
 * Electrical anvil (Baughn 2026-10-28, was tools/models/electricalanvil.py): a workshop desk. A steel-topped bench on
 * four legs with a shelf, a pegboard at the back with tools on it, a vise at the front left, an arc welder (the 800 V
 * set) on the right with its cable and torch, and an angled lamp over the work. The repaired tool lies on the bench in
 * front of the vise (the renderer draws it at the .txt's toolX/Y/Z) and the sparks fly from the torch tip
 * (sparkX/Y/Z). The 800 V terminal plate sits low on the back face. Drawn by ElectricalAnvilRender through Obj3D.
 * Front XN (x = 0). (The Python also wrote the item icon textures/item/electrical_anvil.png; not ported here.)
 */
public final class ElectricalAnvil {
    private ElectricalAnvil() {
    }

    static int[] all(int k) {
        return k == 0 ? null : new int[]{k, k, k};
    }


    /** The bench top's surface (px). */
    static final double TOP = 11.0;
    /** Where the tool lies and the torch tip (px). */
    static final double[] TOOL = {4.5, TOP + 0.05, 9.0}, SPARK = {7.7, TOP + 0.5, 4.8};

    static List<Mesh> desk() {
        List<Mesh> m = new ArrayList<>();
        // legs and the lower shelf
        for (double[] xz : new double[][]{{1, 1}, {13.5, 1}, {1, 13.5}, {13.5, 13.5}})
            m.add(box(xz[0], 0, xz[1], xz[0] + 1.5, TOP - 1.5, xz[1] + 1.5, "darksteel"));
        // (the checker: the shelf held each leg by a 1 x 1 px corner, 44..49 %; it is 0.1 px bigger each side, 1.4..14.6)
        m.add(box(1.4, 2.5, 1.4, 14.6, 3.2, 14.6, "wood"));                        // the shelf
        m.add(box(2.5, 3.2, 9.5, 7.0, 5.5, 13.5, "plate", "nsewu"));               // a box of offcuts on it
        // the bench top: a wooden slab with a steel work plate let into the front half
        m.add(box(0.5, TOP - 1.5, 0.5, 15.5, TOP - 0.3, 15.5, "wood"));
        m.add(box(0.5, TOP - 0.3, 0.5, 8.5, TOP, 15.5, "plate", "nsewu"));
        m.add(box(8.5, TOP - 0.3, 0.5, 15.5, TOP, 15.5, "wood", "nsewu"));
        m.add(box(0.3, TOP - 1.7, 0.3, 15.7, TOP - 1.5, 15.7, "darksteel", "nsewd"));   // the apron's edge
        // the pegboard at the back, with a hammer, a spanner and a file hanging on it
        m.add(box(14.6, TOP, 0.8, 15.6, 16, 15.2, "peg"));
        // (the checker: the hammer's handle ran on into the head, its sides in the head's planes; it stops at the head, z 5.4)
        m.add(box(14.1, 13.6, 2.0, 14.6, 14.1, 5.4, "wood"));                      // hammer
        m.add(box(14.1, 13.2, 5.4, 14.6, 14.6, 6.6, "darksteel"));
        m.add(box(14.2, 12.4, 7.5, 14.6, 15.2, 8.2, "steel"));                     // spanner
        m.add(box(14.2, 15.0, 7.2, 14.6, 15.6, 8.5, "steel"));
        m.add(box(14.2, 12.2, 9.6, 14.6, 15.4, 10.1, "darksteel"));                // file
        m.add(box(14.2, 11.6, 9.5, 14.6, 12.2, 10.2, "wood"));
        // the vise, front left (jaws open along z)
        m.add(box(1.0, TOP, 1.0, 5.0, TOP + 0.8, 5.0, "darksteel"));              // its turntable base
        m.add(box(1.5, TOP + 0.8, 1.5, 4.5, TOP + 3.0, 2.6, "welder"));           // fixed jaw (painted)
        m.add(box(1.5, TOP + 0.8, 3.6, 4.5, TOP + 3.0, 4.6, "welder"));           // moving jaw
        m.add(box(1.4, TOP + 2.2, 2.6, 4.6, TOP + 3.0, 2.8, "steel"));            // jaw plates
        m.add(box(1.4, TOP + 2.2, 3.4, 4.6, TOP + 3.0, 3.6, "steel"));
        m.add(prism('z', 3.0, TOP + 1.6, 0.35, 0.2, 7.2, "steel", 8));            // the screw
        m.add(box(2.9, TOP + 0.9, 6.6, 3.1, TOP + 2.3, 7.4, "steel"));            // its handle (a T)
        m.add(prism('x', TOP + 1.6, 7.0, 0.3, 0.8, 5.2, "steel", 6));
        // the arc welder, back right: a painted case with dials, a cable to a torch lying on the plate
        m.add(box(9.0, TOP, 10.5, 14.4, TOP + 4.5, 15.2, "welder"));
        m.add(box(8.9, TOP + 1.0, 11.0, 9.0, TOP + 4.0, 14.7, "dials", "w").free("the dial panel, a decal 0.1 px proud of the case"));
        m.add(box(9.5, TOP + 4.5, 12.0, 13.5, TOP + 4.9, 12.6, "darksteel"));     // carry handle
        m.add(box(9.5, TOP + 4.5, 11.6, 9.9, TOP + 4.9, 13.0, "darksteel"));
        m.add(box(13.1, TOP + 4.5, 11.6, 13.5, TOP + 4.9, 13.0, "darksteel"));
        m.add(prism('x', TOP + 0.9, 12.0, 0.4, 7.5, 9.0, "rubber", 6));           // the torch cable, out of the case...
        m.add(prism('z', 7.7, TOP + 0.5, 0.4, 7.5, 12.0, "rubber", 6));           // ...along the bench
        // (the checker: the cable along the bench only grazed the one out of the case, 0.4 px higher; a bend joins them at
        // the corner, y TOP + 0.5..0.9, a small visible knuckle)
        m.add(prism('y', 7.7, 12.0, 0.4, TOP + 0.5, TOP + 0.9, "rubber", 6, Double.NaN, true, true, 0));
        m.add(prism('z', 7.7, TOP + 0.5, 0.55, 5.6, 7.5, "darksteel", 8));        // the torch's grip
        m.add(prism('z', 7.7, TOP + 0.5, 0.35, 4.8, 5.6, "brass", 8, 0.2, true, true, Double.NaN));   // its nozzle
        // (the checker: the nozzle met the grip only with its narrow end; a hidden stem inside both, r 0.15, z 5.0..6.0, joins them)
        m.add(prism('z', 7.7, TOP + 0.5, 0.15, 5.0, 6.0, "brass", 8));
        m.add(prism('x', TOP + 0.6, 14.0, 0.3, 7.6, 9.0, "copper", 6));           // earth clamp lead
        // the lamp: a post on the back right corner, an arm over the bench, a shade with a bulb under it
        // (the checker: the post's foot lay in the plane of the pegboard's underside; the foot, on the bench, is dropped. The
        // arm's end lay 0.016 px off the post's side; it ends inside the post, x 14.4, was 14.6)
        m.add(prism('y', 14.2, 1.8, 0.45, TOP, 15.4, "darksteel", 8, false, true));
        m.add(box(5.6, 15.0, 1.5, 14.4, 15.5, 2.1, "steel"));                     // the arm
        m.add(prism('y', 6.0, 1.8, 1.6, 13.4, 15.0, "darksteel", 10, 0.7, true, true, Double.NaN));   // the shade
        // (the checker: the shade hung from the arm by its 0.7 px top only; a hidden stem inside both, r 0.25, y 14.5..15.4)
        m.add(prism('y', 6.0, 1.8, 0.25, 14.5, 15.4, "darksteel", 8));
        m.add(prism('y', 6.0, 1.8, 0.9, 13.1, 13.4, "bulb", 10));                 // the bulb's face
        // the 800 V terminal plate, low on the back face (the 800 V tier colour)
        // (the checker: the plate's back face lay in the studs' plane, x 16; the plate is recessed 0.1 px (x 15.6..15.9) as
        // the other terminal plates, the studs stand 0.1 px proud. It also stood clear of everything: a dark bracket from
        // the shelf's edge, x 14.6..15.6, y 2.6..3.1, z 6.5..9.5, holds it, low under the bench, a little visible)
        m.add(box(15.6, 0.5, 4.5, 15.9, 5.5, 11.5, "steel"));
        m.add(box(15.6, 5.5, 4.5, 15.9, 6.0, 11.5, "hvtier"));
        m.add(box(14.6, 2.6, 6.5, 15.6, 3.1, 9.5, "darksteel"));
        for (double z : new double[]{5.75, 8.0, 10.25}) m.add(box(15.5, 2.0, z - 0.5, 16.0, 3.0, z + 0.5, "copper"));
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("electricalanvil", "welder");   // (no "paint" material)
        n.part("main", desk());
        n.txt.add("o main");
        String[] k = {"toolX", "toolY", "toolZ", "sparkX", "sparkY", "sparkZ"};
        double[] v = {TOOL[0], TOOL[1], TOOL[2], SPARK[0], SPARK[1], SPARK[2]};
        for (int i = 0; i < 6; i++) n.txt.add("f " + k[i] + " " + (v[i] / 16 - 0.5));   // block units, centred
        return List.of(n);
    }
}
