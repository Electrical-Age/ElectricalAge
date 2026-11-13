package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.OilKit.handwheel;
import static mods.eln.cad.models.OilKit.signalPlate;

/**
 * The Pipe Mixer (Baughn 2026-11-11): a pipe T of two steerable check valves. Inlet A on its left flank (z = 0:
 * front.left()), inlet B on its right (z = 16), the outlet out of its back (x = 16), all at the line's height
 * (PipeMixerElement.LINE_Y), iron pipe flanges at the face centre; a cast body over the junction with a valve bonnet
 * and handwheel over each inlet (A's red, B's blue); the signal on the front floor terminal. Part main. Front XN (x = 0).
 */
public final class PipeMixer {
    private PipeMixer() {
    }

    static final double C = 8.0;
    /** The line's height (PipeMixerElement.LINE_Y). */
    static final double LY = 4.0;

    static List<Mesh> mainPart() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(2.0, 0, 2.0, 14.0, 1.0, 14.0, "darksteel", "nsewu"));
        // the body over the junction, painted, on the skid
        m.add(box(5.0, 1.0, 4.0, 11.0, 6.5, 12.0, "paint"));
        // the line across (A to B) and the outlet to the back, each with its flange
        m.add(prism('z', C, LY, 0.75, 0.9, 15.1, "steel", 8, false, false));
        m.add(prism('z', C, LY, 1.5, 0.0, 1.0, "steel", 10, true, false));
        m.add(prism('z', C, LY, 1.5, 15.0, 16.0, "steel", 10, false, true));
        m.add(prism('x', LY, C, 0.75, 10.5, 15.1, "steel", 8, false, false));
        m.add(prism('x', LY, C, 1.5, 15.0, 16.0, "steel", 10, false, true));
        // a bonnet and a handwheel over each inlet's valve: A's wheel red, B's blue (a client check: the inlets looked alike)
        for (double z : new double[]{5.75, 10.25}) {
            m.add(prism('y', C, z, 0.9, 6.4, 8.5, "steel", 8, false, true));
            m.addAll(handwheel(C, 8.7, z, 1.1, 'y', z < C ? "red" : "blue"));   // (its hub on the bonnet's top)
        }
        // the signal terminal on the front, its backing joining it to the skid
        m.addAll(signalPlate());
        m.add(box(0.85, 0, 6.5, 2.0, 3.5, 9.5, "steel", "nsu"));
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("pipemixer", OilKit.PAINT_50V);
        n.part("main", mainPart());
        return List.of(n);
    }
}
