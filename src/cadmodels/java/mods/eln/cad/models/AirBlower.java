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
 * The 230V Air Blower (todo #471; its own model, todo #519, Baughn 2026-11-18: one of its two pipes is the air intake): the
 * Inline Pump's centrifugal machine with its left flank's pipe replaced by an intake, a short bell-mouthed duct into the
 * volute's side behind a grille (z = 0, front.left()); the air goes out of the iron pipe flange on its right flank
 * (z = 16, at the pipes' low line: AirBlowerElement's port). The motor behind the volute; power on the front floor
 * terminal, the speed signal on the back. Parts main, rotor (as the Inline Pump's, turned by its render). Front XN.
 */
public final class AirBlower {
    private AirBlower() {
    }

    static final double C = 8.0;
    /** The outlet's height (InlinePumpElement.LINE_Y) and the intake's centre (x, y) and radius. */
    static final double LY = 4.0, IX = 7.5, IY = 8.0, IR = 2.2;

    static List<Mesh> mainPart() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(2.0, 0, 3.0, 14.0, 1.0, 13.0, "darksteel", "nsewu"));
        // the volute: a drum across the line, its axis along x
        // (the checker: the foot's back face, the drum's back cap and the steel band's end were one plane at x = 10 and
        // z-fought; the foot ends 0.1 px short of it (x 9.9) and the band 0.1 px past it (x 10.1))
        m.add(box(5.5, 1.0, 6.0, 9.9, 4.5, 10.0, "castiron"));   // (its foot)
        m.add(prism('x', C, C, 4.0, 5.0, 10.0, "paint", 14));
        m.add(tube('x', C, C, 3.9, 4.3, 9.5, 10.1, "steel", 14, Double.NaN, false));
        // the outlet, low (LINE_Y = 4 px: InlinePumpElement), out of the volute's foot: a pipe stub to a flange on z = 16
        // (an alloy pipe's size: drawn 1.5 voxels, its flange 0.75 wider each way)
        m.add(prism('z', C, LY, 0.75, 8.0, 15.1, "steel", 8, false, false));
        m.add(prism('z', C, LY, 1.5, 15.0, 16.0, "steel", 10, false, true));
        // the intake on the left flank: a cast duct into the volute's side, a steel ring round its mouth, the grille's
        // louvres behind three bars
        m.add(prism('z', IX, IY, IR, 1.0, 5.0, "castiron", 14, false, false));
        m.add(tube('z', IX, IY, IR + 0.4, IR + 0.8, 0.6, 1.4, "steel", 14, Double.NaN, false));
        m.add(prism('z', IX, IY, IR + 0.4, 0.8, 1.0, "louvre", 14, true, false));
        // (bars within the ring's bore, 0.1 px behind its face: at 0.62 they z-fought it)
        for (double y : new double[]{IY - 1.2, IY, IY + 1.2}) {
            double h = Math.sqrt((IR + 0.4) * (IR + 0.4) - (Math.abs(y - IY) + 0.12) * (Math.abs(y - IY) + 0.12)) - 0.05;
            m.add(box(IX - h, y - 0.12, 0.7, IX + h, y + 0.12, 0.8, "darksteel"));
        }
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
        NodeModel n = new NodeModel("airblower200v", OilKit.PAINT_200V);
        n.part("main", mainPart());
        n.part("rotor", rotorPart());
        n.origin("rotor", 10.5, C, C);
        return List.of(n);
    }
}
