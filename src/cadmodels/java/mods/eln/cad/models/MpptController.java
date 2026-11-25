package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;

/**
 * The MPPT charge controller (Baughn 2026-11-25, board #79: "a dedicated MPPT block, like a heavy duty Victron"): a
 * wall-mounted unit, a six-node device, so the face it hangs on is the plane x = 8 and it stands out along +x; on a
 * wall its top is +y. A painted case on a back plate, rubber bumpers on its four corners, a finned heat sink across
 * the bottom, a stripe and a label, the display (part "lcd", lit by the render while it converts), and a terminal on
 * each edge where the pins are, out to the block's edge over the cable's end (CableEnds), as the signal processor's.
 */
public final class MpptController {
    private MpptController() {
    }

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(8.0, 1.0, 2.5, 8.5, 15.0, 13.5, "darksteel"));                  // back plate
        m.add(box(8.5, 4.4, 3.0, 12.4, 13.6, 13.0, "paint", "udsne"));            // the case (no back: on the plate)
        m.add(box(8.5, 1.4, 3.0, 12.0, 4.4, 13.0, "darksteel", "udsne"));       // heat sink across the bottom
        for (int i = 0; i < 6; i++) {                                              // its fins
            double z = 3.6 + i * 1.6;
            m.add(box(12.0, 1.6, z, 12.6, 4.2, z + 0.6, "darksteel", "udsne"));
        }
        // rubber bumpers on the corners (wrapping the case's corners)
        for (double y0 : new double[]{1.0, 12.8})
            for (double z0 : new double[]{2.5, 11.7})
                m.add(box(8.5, y0, z0, 12.8, y0 + 2.2, z0 + 1.8, "rubber"));
        m.add(box(12.4, 5.0, 7.6, 12.5, 6.2, 11.4, "label", "udsne"));            // rating label
        m.add(box(12.4, 6.9, 4.9, 12.7, 9.9, 11.1, "darksteel", "udsne"));        // the display's bezel
        return m;
    }

    /** The stripe across the front: drawn tinted by the tier (orange, gold for the advanced one). */
    static List<Mesh> stripe() {
        return List.of(box(12.4, 10.9, 4.4, 12.5, 11.5, 11.6, "white", "udsne"));
    }

    /** The display's glass: drawn lit (full bright, tinted) by the render while the controller converts. */
    static List<Mesh> lcd() {
        return List.of(box(12.7, 7.3, 5.4, 12.8, 9.5, 10.6, "white", "udsne"));
    }

    /** A terminal on each edge, from the case out to the block's edge (the signal processor's places). */
    static List<Mesh> terminals() {
        List<Mesh> m = new ArrayList<>();
        Mesh t;
        t = box(8.0, -CableEnds.PAST, 6.5, 9.6, 1.9, 9.5, "brass", "udsne");   // (into the heat sink)
        CableEnds.check("mpptcontroller", t, 1, false, 2, 8);
        m.add(t);
        t = box(8.0, 13.2, 6.5, 9.6, 16.0 + CableEnds.PAST, 9.5, "brass", "udsne");
        CableEnds.check("mpptcontroller", t, 1, true, 2, 8);
        m.add(t);
        t = box(8.0, 6.5, -CableEnds.PAST, 9.6, 9.5, 3.4, "brass", "udsne");
        CableEnds.check("mpptcontroller", t, 2, false, 1, 8);
        m.add(t);
        t = box(8.0, 6.5, 12.6, 9.6, 9.5, 16.0 + CableEnds.PAST, "brass", "udsne");
        CableEnds.check("mpptcontroller", t, 2, true, 1, 8);
        m.add(t);
        for (Mesh x : m) x.body = "main";
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("mpptcontroller", "paint_blue");
        n.part("main", main());
        n.part("terminals", terminals());
        n.part("lcd", lcd());
        n.part("stripe", stripe());
        return List.of(n);
    }
}
