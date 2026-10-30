package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;

/**
 * The Signal Processor (Baughn 2026-10-30, board #45: "DIN rails in a cupboard with arithmetic modules, the number of
 * modules rendered depending on the number of chips inserted, maxing out at 3x6"). A six-node device: the face it is fixed
 * to is the plane x = 8 and it stands out along +x. An open cupboard (back plate, four walls), three DIN rails, and on
 * them up to eighteen modules (parts m0..m17, filled rail by rail from the bottom, each a little different: a lamp and a
 * label, screw terminals, a switch, a dial plate), the logger's small screen across the top with eight status lamps
 * beside it (parts led0..led7), and a terminal on each edge where the pins are, out to the block's edge over the cable's
 * end (CableEnds). Four-fold symmetric outside, as a wall-mounted processor is drawn without turning to its front.
 */
public final class SignalProcessor {
    private SignalProcessor() {
    }

    /** The screen: its face x, its y and z extents. */
    static final double SCREEN_X = 9.7, SY0 = 11.3, SY1 = 13.7, SZ0 = 2.3, SZ1 = 10.3;
    /** The rails' centres (y) and the modules' places along them (z of the first, pitch, width). */
    static final double[] RAIL_Y = {3.4, 6.2, 9.0};
    static final double MZ0 = 2.1, PITCH = 2.0, MW = 1.8, MH = 1.2;
    static final String[] BODY = {"white", "paint_plain", "darksteel", "paint_gen"};

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        // the cupboard: back plate and four walls
        m.add(box(8.0, 1.0, 1.0, 8.5, 15.0, 15.0, "darksteel"));
        m.add(box(8.5, 1.0, 1.0, 12.0, 1.6, 15.0, "paint"));
        m.add(box(8.5, 14.4, 1.0, 12.0, 15.0, 15.0, "paint"));
        m.add(box(8.5, 1.6, 1.0, 12.0, 14.4, 1.6, "paint"));
        m.add(box(8.5, 1.6, 14.4, 12.0, 14.4, 15.0, "paint"));
        // the DIN rails
        for (double y : RAIL_Y) m.add(box(8.5, y - 0.7, 1.9, 9.0, y + 0.7, 14.1, "steel"));
        // the logger's screen across the top
        m.add(box(8.5, 11.0, 2.0, 9.6, 14.0, 10.6, "darksteel"));
        m.add(box(9.6, SY0, SZ0, SCREEN_X, SY1, SZ1, "screen"));
        return m;
    }

    /** The terminals, a part of their own (the device's box leaves them out: it stays narrower than the face). */
    static List<Mesh> terminals() {
        List<Mesh> m = new ArrayList<>();
        // a terminal on each edge, from the walls out to the block's edge
        Mesh t;
        t = box(8.0, 0.0, 6.5, 9.6, 1.3, 9.5, "brass", "udsne");
        CableEnds.check("signalprocessor", t, 1, false, 2, 8);
        m.add(t);
        t = box(8.0, 14.7, 6.5, 9.6, 16.0, 9.5, "brass", "udsne");
        CableEnds.check("signalprocessor", t, 1, true, 2, 8);
        m.add(t);
        t = box(8.0, 6.5, 0.0, 9.6, 9.5, 1.3, "brass", "udsne");
        CableEnds.check("signalprocessor", t, 2, false, 1, 8);
        m.add(t);
        t = box(8.0, 6.5, 14.7, 9.6, 9.5, 16.0, "brass", "udsne");
        CableEnds.check("signalprocessor", t, 2, true, 1, 8);
        m.add(t);
        // (checked as part of the cupboard they hang from: drawn apart only to stay out of the device's box)
        for (Mesh x : m) x.body = "main";
        return m;
    }

    /** Module i: rail i / 6, place i % 6; its face one of four kinds. */
    static List<Mesh> module(int i) {
        double y = RAIL_Y[i / 6], z0 = MZ0 + (i % 6) * PITCH, z1 = z0 + MW;
        List<Mesh> m = new ArrayList<>();
        m.add(box(9.0, y - MH, z0, 11.0, y + MH, z1, BODY[(i * 7 + i / 6) % BODY.length]));
        switch ((i * 5 + i / 6) % 4) {
            case 0 -> {   // a lamp and a label
                m.add(box(11.0, y + 0.45, z0 + 0.6, 11.2, y + 0.9, z1 - 0.6, "lamp"));
                m.add(box(11.0, y - 0.9, z0 + 0.25, 11.1, y - 0.2, z1 - 0.25, "label"));
            }
            case 1 -> {   // screw terminals at top and bottom
                for (double dy : new double[]{0.65, -0.95})
                    for (double dz : new double[]{0.2, MW - 0.6}) m.add(box(11.0, y + dy, z0 + dz, 11.25, y + dy + 0.3, z0 + dz + 0.4, "brass"));
            }
            case 2 -> {   // a switch
                m.add(box(11.0, y - 0.6, z0 + 0.45, 11.15, y + 0.6, z1 - 0.45, "darksteel"));
                m.add(box(11.15, y - 0.1, z0 + 0.6, 11.6, y + 0.4, z1 - 0.6, "red"));
            }
            default -> {   // a dial plate
                m.add(box(11.0, y - 0.95, z0 + 0.2, 11.1, y + 0.95, z1 - 0.2, "dials"));
            }
        }
        return m;
    }

    /** Lamp i: two rows of four beside the screen. */
    static List<Mesh> led(int i) {
        double z = 11.1 + (i % 4) * 0.75, y = 12.8 - (i / 4) * 1.0;
        return List.of(box(8.5, y, z, 8.9, y + 0.5, z + 0.5, "white"));
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("signalprocessor", "paint_plain");
        n.part("main", main());
        n.part("terminals", terminals());
        for (int i = 0; i < 18; i++) n.part("m" + i, module(i));
        for (int i = 0; i < 8; i++) n.part("led" + i, led(i));
        // the logger screen, main's floats (centred block units, x up from the face): where the render draws the graph
        n.txt.add("o main");
        n.txt.add("f screenX " + Catalog.fmt((SCREEN_X - 8) / 16));
        n.txt.add("f screenY0 " + Catalog.fmt((SY0 - 8) / 16));
        n.txt.add("f screenY1 " + Catalog.fmt((SY1 - 8) / 16));
        n.txt.add("f screenZ0 " + Catalog.fmt((SZ0 - 8) / 16));
        n.txt.add("f screenZ1 " + Catalog.fmt((SZ1 - 8) / 16));
        return List.of(n);
    }
}
