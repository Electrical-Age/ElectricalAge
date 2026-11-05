package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.OilKit.turned;

/**
 * The robotics computer (mods.eln.robotics.computer): computer (node model, front XN), a cabinet on a plinth with two
 * floppy drives side by side on its front, a vent grille under them, the 50 V terminal plate low on the back and the
 * RS485 bus pin on the right flank's floor edge (its right end, as on the servo drive; Baughn 2026-11-05: all pins on
 * the floor edges). Part "lamp" is the run lamp, tinted by the render.
 */
public final class ComputerParts {
    private ComputerParts() {
    }

    public static List<NodeModel> models() {
        NodeModel c = new NodeModel("computer", OilKit.PAINT_50V);
        c.part("main", cabinet());
        c.part("lamp", List.of(box(0.4, 12.5, 12.5, 1.0, 13.5, 13.5, "white")));
        return List.of(c);
    }

    static List<Mesh> cabinet() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(0.5, 0, 0.5, 15.5, 1, 15.5, "darksteel"));                     // the plinth
        m.add(box(1, 1, 1, 15, 15, 15, "paint"));                                // the cabinet
        m.add(box(1.5, 15, 1.5, 14.5, 15.4, 14.5, "darksteel"));                 // its lid
        for (double z0 : new double[]{2, 8.5}) {                                  // the drives: bezel, slot, eject button
            m.add(box(0.4, 8.5, z0, 1, 11.5, z0 + 5.5, "darksteel"));
            m.add(box(0.3, 9.8, z0 + 0.8, 0.4, 10.3, z0 + 4.7, "steel", "nsewu"));
            m.add(box(0.3, 8.9, z0 + 4.2, 0.4, 9.4, z0 + 4.9, "white", "nsewu"));
        }
        for (double y = 3; y < 7.5; y += 1.1) m.add(box(0.6, y, 3, 1, y + 0.5, 13, "darksteel"));   // the vent grille
        m.add(box(0.6, 12, 2, 1, 14, 11, "screen"));                             // the status strip
        m.addAll(turned(OilKit.terminalPlate(OilKit.PAINT_50V), 2));             // 50 V, low on the back
        m.addAll(turned(ServoParts.pinPlate(4, "paint_bus"), 1));                // the bus: right flank, its right end
        return m;
    }
}
