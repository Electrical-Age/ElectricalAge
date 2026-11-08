package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;

/**
 * Equipment fitted to a utility pole (Baughn 2026-11-08), drawn by ElectricalPoleRender over the pole's own model:
 * <ul>
 * <li>"lamp": a streetlight, a clamp round the pole (its axis at x = z = 8), an arm out towards -x and a lamp head at
 * its end; "lens" under the head, drawn bright when lit. The render turns it to face its side and lifts it to the
 * pole's top.</li>
 * <li>"anchor": the stasis anchor, rift-crystal rings round the cross-arm's two insulators and a crystal on the pole's
 * top, in the pole model's own coordinates (floor at y = 0, insulators at x = 4, z = 8 -+ 11.2).</li>
 * </ul>
 */
public final class PoleGear {
    private PoleGear() {
    }

    static List<Mesh> lamp() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(5.3, 0, 5.3, 10.7, 3.0, 10.7, "darksteel"));                 // the clamp round the pole
        m.add(box(-20, 1.6, 7.4, 6.0, 2.8, 8.6, "darksteel"));                 // the arm
        m.add(box(-8, 0.4, 7.6, 6.0, 1.7, 8.4, "darksteel"));                  // its brace under it
        m.add(box(-26, 1.0, 5.5, -16, 3.6, 10.5, "steel"));                    // the head
        m.add(box(-26.4, 3.5, 5.1, -15.6, 4.2, 10.9, "darksteel"));            // its cap
        return m;
    }

    static List<Mesh> lens() {
        return List.of(box(-25, 0.9, 6.2, -17, 1.0, 9.8, "lamp", "d").free("the lens, a pane under the head"));
    }

    static List<Mesh> anchor() {
        List<Mesh> m = new ArrayList<>();
        for (double z : new double[]{8 - 11.2, 8 + 11.2}) {
            m.add(tube('y', 4, z, 1.1, 1.5, 61.0, 62.0, "crystal", 10).free("a ring round the insulator, held by the field"));
            m.add(tube('y', 4, z, 1.1, 1.5, 63.0, 64.0, "crystal", 10).free("a ring round the insulator, held by the field"));
        }
        m.add(box(6.4, 69.9, 6.4, 9.6, 70.9, 9.6, "darksteel"));               // a socket on the pole's top
        m.add(prism('y', 8, 8, 1.0, 70.5, 74.5, "crystal", 6));
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("polegear", "darksteel");
        n.part("lamp", lamp());
        n.part("lens", lens());
        n.part("anchor", anchor());
        return List.of(n);
    }
}
