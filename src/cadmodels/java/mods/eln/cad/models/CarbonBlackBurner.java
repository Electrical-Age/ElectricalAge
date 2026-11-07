package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;

/**
 * The Carbon Black Burner (the rubber chain's oil route, 2026-11-07): a firebrick box bound in steel bands on a skid, a
 * stack on top (the black smoke comes out of it), a soot drawer at the front, the fire seen through a sight port (part
 * fire, lit while it burns) and the blue IN flange for its fuel on the back (4 px up, centred), its label in miniature.
 * Drawn by CarbonBlackBurnerRender. Front XN (x = 0).
 */
public final class CarbonBlackBurner {
    private CarbonBlackBurner() {
    }

    static List<Mesh> body() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.0, 0, 2.0, 15.0, 1.0, 14.0, "darksteel"));                                         // skid
        m.add(box(3.0, 1.0, 3.0, 13.0, 9.0, 13.0, "firebrick"));                                       // the firebox
        for (double y : new double[]{3.0, 7.0})                                                         // steel bands
            m.add(box(2.85, y - 0.3, 2.85, 13.15, y + 0.3, 13.15, "darksteel", "nsewu"));
        m.add(box(2.8, 9.0, 2.8, 13.2, 9.6, 13.2, "darksteel"));                                       // the roof plate
        MachineKit.rivetsX(m, 3.6, 12.4, 9.3, 2.8, -1, 5);
        // the stack: a tapering flue on the roof, a soot-black lip
        m.add(prism('y', 9.0, 8.0, 1.6, 9.6, 11.0, "castiron", 12, 1.3, true, false, Double.NaN));
        m.add(prism('y', 9.0, 8.0, 1.2, 11.0, 15.4, "darksteel", 12, false, false));
        m.add(Mc.tube('y', 9.0, 8.0, 1.2, 1.5, 15.4, 16.0, "soot", 12));
        m.add(Mc.tube('y', 9.0, 8.0, 1.2, 1.4, 12.8, 13.2, "steel", 12));                              // a band on it
        // the soot drawer at the front, its handle, soot heaped in it
        m.add(box(1.0, 1.0, 5.0, 3.0, 4.0, 11.0, "darksteel"));
        m.add(box(1.2, 4.0, 5.2, 2.8, 4.2, 10.8, "soot").free("soot heaped on the drawer"));
        m.add(box(0.5, 2.2, 7.2, 1.0, 2.6, 8.8, "steel"));
        // the fuel line: the blue IN flange on the back (4 px up, centred) and its pipe into the firebox
        m.add(prism('x', 4.0, 8.0, 0.45, 13.0, 15.5, "darksteel", 8, false, false));
        m.add(prism('x', 4.0, 8.0, 1.0, 15.5, 15.7, "paint_blue", 12));
        m.add(prism('x', 4.0, 8.0, 1.45, 15.7, 15.9, "steel", 12));
        m.addAll(OilKit.boltRing('x', 4.0, 8.0, 1.25, 15.9, 15.98, 4, "darksteel", 0.17, Math.PI / 4));
        m.add(MachineKit.flangeLabel(15.9, 4.0, 8.0, 0.7, "label_in"));
        m.add(box(13.0, 3.0, 7.2, 15.0, 3.4, 8.8, "darksteel"));                                       // a pipe bracket
        m.add(box(2.9, 3.6, 10.2, 3.0, 4.8, 12.0, "brass", "w").free("a nameplate on the firebox"));
        return m;
    }

    static List<Mesh> fire() {
        return List.of(box(2.85, 5.0, 6.8, 3.0, 6.5, 9.2, "fire", "w").free("the fire through the sight port"));
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("carbonblackburner", OilKit.PAINT_200V);
        n.part("main", body());
        n.part("fire", fire());
        return List.of(n);
    }
}
