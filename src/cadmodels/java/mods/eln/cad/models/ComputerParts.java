package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.OilKit.turned;

/**
 * The robotics computer (mods.eln.robotics.computer): computer (node model, front XN), two blocks tall (Baughn
 * 2026-11-18): a desktop case on a skid, its two floppy drives side by side on its front over a vent grille, the 50 V
 * terminal plate low on the back and the RS485 bus pin on the right flank's floor edge (its right end, as on the servo
 * drive; Baughn 2026-11-05: all pins on the floor edges); and on top of it, in the block above, a CRT monitor on a
 * swivel foot, its tube tapering back. Painted as the three-channel logger (FieldLogger: its case colour, dark steel
 * trim, rubber). Part "lamp" is the case's run lamp and "lamp2" the monitor's power lamp, both tinted by the render;
 * the render draws the screen's picture over the glass (SCREEN_*).
 */
public final class ComputerParts {
    private ComputerParts() {
    }

    /** The CRT's glass on the monitor's front, px: x of its face, its rows (y) and columns (z), 4:3. */
    /** (shaped as the 80 x 25 screen of 6 x 10 px cells, 1.92:1: Baughn; the 120 x 40 mode gets bars at its sides) */
    public static final double SCREEN_X = 0.5, SCREEN_Y0 = 20.0, SCREEN_Y1 = 20.0 + 12.8 / 1.92, SCREEN_Z0 = 1.6, SCREEN_Z1 = 14.4;
    /** The monitor's front face (its bezel), px. */
    static final double BEZEL_X = 0.9;

    public static List<NodeModel> models() {
        NodeModel c = new NodeModel("computer", "paint_condenser");
        c.part("main", main());
        c.part("lamp", List.of(box(0.4, 12.5, 12.5, 1.0, 13.5, 13.5, "white")));
        c.part("lamp2", List.of(box(BEZEL_X - 0.4, 18.3, 12.4, BEZEL_X, 18.9, 13.0, "white")));
        return List.of(c);
    }

    static List<Mesh> main() {
        List<Mesh> m = cabinet();
        m.addAll(monitor());
        return m;
    }

    static List<Mesh> cabinet() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(0.5, 0, 0.5, 15.5, 1, 15.5, "darksteel"));                     // the skid
        m.add(box(1, 1, 1, 15, 15, 15, "paint"));                                // the case
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

    /** The monitor: foot and neck on the case's lid, the front shell round the glass, the tube's back, its vents. */
    static List<Mesh> monitor() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(4.0, 15.4, 4.0, 12.0, 16.2, 12.0, "darksteel"));               // the swivel foot on the lid
        m.add(box(5.5, 16.2, 6.0, 10.5, 17.6, 10.0, "darksteel"));               // its neck
        m.add(box(BEZEL_X, 17.5, 0.6, 9.0, 28.2, 15.4, "paint"));                // the front shell
        // the tube's back, a square frustum tapering to the rear (its corners on the diagonals: phase pi / 4)
        double cy = 22.85, cz = 8.0, half = 5.0, halfEnd = 2.8;
        m.add(prism('x', cy, cz, half * Math.sqrt(2), 9.0, 14.8, "paint", 4, halfEnd * Math.sqrt(2), true, true, Math.PI / 4));
        m.add(box(14.6, cy - 2.6, cz - 2.6, 15.6, cy + 2.6, cz + 2.6, "darksteel"));   // the cap at the tube's end
        for (double y = cy - 1.8; y < cy + 2.0; y += 1.2) m.add(box(15.6, y, cz - 1.8, 15.9, y + 0.5, cz + 1.8, "darksteel", "nsewu"));   // its vents
        // the glass, sunk in a dark surround, and the strip under it: brightness and contrast knobs
        m.add(box(BEZEL_X - 0.2, SCREEN_Y0 - 0.6, SCREEN_Z0 - 0.6, BEZEL_X, SCREEN_Y1 + 0.6, SCREEN_Z1 + 0.6, "darksteel"));
        m.add(box(SCREEN_X, SCREEN_Y0, SCREEN_Z0, BEZEL_X - 0.2, SCREEN_Y1, SCREEN_Z1, "screen"));
        for (double z : new double[]{3.0, 5.0})
            m.add(prism('x', 18.6, z, 0.55, BEZEL_X - 0.5, BEZEL_X, "darksteel", 8));
        return m;
    }
}
