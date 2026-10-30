package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;

/**
 * The Signal Processor (Baughn 2026-10-30, board #45: restyled to match the new blocks, with a tiny data logger): a
 * six-node device, so its mounting face is the plane x = 8 and it stands up from it along +x (Obj3D: x = 0 is the face
 * it sits on). A dark mounting plate, a painted case with a terminal on each side (the render draws the coloured pins:
 * A red, B blue, C green, the output), and on top a bezel holding the logger's screen, where the render draws the
 * A/B/C/out graph, and a column of eight status lamps (parts led0..led7). Four-fold symmetric about the face's normal,
 * as a wall-mounted processor is drawn without turning to its front.
 */
public final class SignalProcessor {
    private SignalProcessor() {
    }

    /** The case's top (px above the plane x = 8 is TOP - 8) and the screen: its face x, its y and z extents. */
    static final double TOP = 12.5, SCREEN_X = 13.2, SY0 = 4.5, SY1 = 11.5, SZ0 = 4.5, SZ1 = 9.5;

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(8.0, 2.0, 2.0, 8.6, 14.0, 14.0, "darksteel"));                 // mounting plate
        m.add(box(8.6, 3.0, 3.0, TOP, 13.0, 13.0, "paint"));                     // the case
        // a terminal block on each side, where the pins meet it
        m.add(box(9.0, 2.4, 6.5, 11.0, 3.0, 9.5, "brass"));
        m.add(box(9.0, 13.0, 6.5, 11.0, 13.6, 9.5, "brass"));
        m.add(box(9.0, 6.5, 2.4, 11.0, 9.5, 3.0, "brass"));
        m.add(box(9.0, 6.5, 13.0, 11.0, 9.5, 13.6, "brass"));
        // the bezel on top, the screen set in it, a strip for the lamps beside it
        m.add(box(TOP, 3.5, 3.5, 13.0, 12.5, 12.5, "darksteel"));
        m.add(box(13.0, SY0, SZ0, SCREEN_X, SY1, SZ1, "screen"));
        m.add(box(13.0, SY0, 10.3, 13.1, SY1, 11.7, "steel"));
        return m;
    }

    /** Lamp i on the strip (a column along y). */
    static List<Mesh> led(int i) {
        double y = SY0 + 0.25 + i * 0.85;
        return List.of(box(13.1, y, 10.75, 13.4, y + 0.5, 11.25, "white"));
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("signalprocessor", "paint_plain");
        n.part("main", main());
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
