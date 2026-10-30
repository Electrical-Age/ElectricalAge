package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.OilKit.signalPlate;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * The 200V Inline Pump (Baughn 2026-10-29, the shaft redo): a centrifugal pump in a pipe line. Water in on its left
 * flank (z = 0: front.left()), out of its right (z = 16), both iron pipe flanges at the face centre; the motor behind
 * the volute; power on the front floor terminal, the speed signal on the back. Parts main, rotor (the coupling, turned
 * by InlinePumpRender about x through its origin). Front XN (x = 0).
 */
public final class InlinePump {
    private InlinePump() {
    }

    static final double C = 8.0;
    /** The line's height (InlinePumpElement.LINE_Y). */
    static final double LY = 4.0;

    static List<Mesh> mainPart() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(2.0, 0, 3.0, 14.0, 1.0, 13.0, "darksteel", "nsewu"));
        // the volute: a drum across the line, its axis along x
        // (the checker: the foot's back face, the drum's back cap and the steel band's end were one plane at x = 10 and
        // z-fought; the foot ends 0.1 px short of it (x 9.9) and the band 0.1 px past it (x 10.1))
        m.add(box(5.5, 1.0, 6.0, 9.9, 4.5, 10.0, "castiron"));   // (its foot)
        m.add(prism('x', C, C, 4.0, 5.0, 10.0, "paint", 14));
        m.add(tube('x', C, C, 3.9, 4.3, 9.5, 10.1, "steel", 14, Double.NaN, false));
        // the line, low (LINE_Y = 4 px: InlinePumpElement), into the volute's foot: pipe stubs to flanges on z = 0 and 16
        // (an alloy pipe's size: drawn 1.5 voxels, its flange 0.75 wider each way)
        m.add(prism('z', C, LY, 0.75, 0.9, 15.1, "steel", 8, false, false));
        m.add(prism('z', C, LY, 1.5, 0.0, 1.0, "steel", 10, true, false));
        m.add(prism('z', C, LY, 1.5, 15.0, 16.0, "steel", 10, false, true));
        // the motor behind, on a saddle
        m.add(box(11.5, 1.0, 5.5, 15.0, 5.5, 10.5, "castiron"));
        m.add(prism('x', C, C, 2.75, 11.0, 15.5, "steel", 12));
        for (double x : new double[]{12.0, 13.25, 14.5}) m.add(tube('x', C, C, 2.65, 3.1, x, x + 0.4, "darksteel", 12, Double.NaN, false));
        // (the checker: the power plate stood 1.15 px clear of the skid; a darksteel plinth under it, 1 px high like the
        // skid, joins them. The signal plate stood 0.15 px clear of the motor's saddle; a backing joins it, hidden behind
        // the plate)
        m.addAll(terminalPlate());
        m.add(box(0.85, 0, 4.5, 2.0, 1.0, 11.5, "darksteel", "nsu"));
        m.addAll(turned(signalPlate(), 2));
        m.addAll(turned(List.of(box(0.85, 0, 6.5, 1.0, 3.5, 9.5, "steel", "nsu")), 2));
        return m;
    }

    static List<Mesh> rotorPart() {
        return List.of(prism('x', C, C, 1.0, 10.0, 11.0, "brass", 8), box(10.2, C - 1.75, C - 0.3, 10.8, C + 1.75, C + 0.3, "darksteel"));
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("inlinepump200v", OilKit.PAINT_200V);
        n.part("main", mainPart());
        n.part("rotor", rotorPart());
        n.origin("rotor", 10.5, C, C);
        return List.of(n);
    }
}
