package mods.eln.cad.models;

import org.erisia.cad.Mesh;
import org.erisia.cad.Model;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.OilKit.signalPlate;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * The servo layer's models (mods.eln.robotics, Baughn 2026-11-02): the Linear Servo Drive, its elevator car, the Servo
 * Rail and the Station Marker.
 * <ul>
 * <li>servodrive (node model, front XN): a painted cabinet with the rail's foot on top, a motor bell on the back over
 * the 200 V terminal plate, three pin plates on each flank's floor edge, a quarter block apart (Baughn 2026-11-05:
 * several pins per edge; the right flank's right one violet, the bus) and the status lamp in front (part "lamp",
 * tinted by the render).</li>
 * <li>elevatorcar (node model, drawn by the car's entity renderer; +x toward the rail, centred on the middle of its
 * 3 x 3 blocks): the cabin, 3 x 3 x 3 outside (Baughn 2026-11-05): a 1 px tread plate (the car's collider is that
 * thick), 1 px walls on the rail side and both flanks, a 1 px roof, the front open; corner posts, a crosshead on the
 * rail side with guide shoes on the rail's fin, and the call panel inside the back wall.</li>
 * <li>servo_rail (block model, pointing up): a square post with four guide fins.</li>
 * <li>station_marker (block model, pointing north, its back on the south face): a call box with one button.</li>
 * </ul>
 */
public final class ServoParts {
    private ServoParts() {
    }

    static List<Mesh> drive() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(0.5, 0, 0.5, 15.5, 1.5, 15.5, "darksteel"));                  // the plinth
        m.add(box(0.85, 1.5, 0.85, 15.15, 14, 15.15, "paint"));                  // the cabinet
        m.add(box(1.5, 14, 1.5, 14.5, 14.6, 14.5, "darksteel"));                 // its lid
        m.add(box(6, 14.6, 6, 10, 16, 10, "steel"));                             // the rail's foot (the rail's post above)
        m.add(prism('x', 9.5, 8, 3.5, 14.5, 15.9, "steel", 16));                 // the motor's end bell, on the back
        m.add(prism('x', 9.5, 8, 1.2, 15.9, 16.0, "darksteel", 12));             // its bearing cap
        m.addAll(turned(terminalPlate(OilKit.PAINT_200V), 2));                   // 200 V, low on the back
        // three pins on each flank's floor edge, at z 4, 8, 12 on the x = 0 face before the turn (LinearServoDriveDescriptor.spec)
        for (int k : new int[]{1, 3})
            for (double dz : new double[]{-4, 0, 4})
                m.addAll(turned(pinPlate(dz, k == 1 && dz == 4 ? "paint_bus" : "darksteel"), k));   // (k 1: the right flank, z = 16; +dz its right seen from outside)
        return m;
    }

    /** A pin plate on the x = 0 face's floor edge, dz from the middle: the signal plate, its cap in `cap`. */
    static List<Mesh> pinPlate(double dz, String cap) {
        return new ArrayList<>(List.of(box(0.1, 0, 6.5 + dz, 0.85, 3.5, 9.5 + dz, "steel", "nsewu"), box(0.1, 3.5, 6.5 + dz, 0.85, 3.9, 9.5 + dz, cap, "nsewu"),
            box(0.0, 1.9, 7.6 + dz, 0.1, 2.6, 8.4 + dz, "copper", "nsewu")));
    }

    static List<Mesh> lamp() {
        return new ArrayList<>(List.of(box(0.35, 8, 6, 0.85, 11, 10, "white")));
    }

    /** The cabin (px, 3 x 3 blocks centred on x, z = 8; +x toward the rail; ElevatorCar.CABIN, WALL, THICK). */
    static List<Mesh> car() {
        double lo = -16, hi = 32, top = 48;
        List<Mesh> m = new ArrayList<>();
        m.add(box(lo, 0, lo, hi, 1, hi, "plate"));                                // the tread plate
        m.add(box(hi - 1, 1, lo + 1, hi, top - 1, hi - 1, "steel"));              // the back wall, on the rail side
        m.add(box(lo, 1, lo, hi - 1, top - 1, lo + 1, "steel"));                  // the flank walls
        m.add(box(lo, 1, hi - 1, hi - 1, top - 1, hi, "steel"));
        m.add(box(lo, top - 1, lo, hi, top, hi, "steel"));                        // the roof
        m.add(box(lo, 1, lo + 1, lo + 1.5, top - 1, lo + 2.5, "paint"));        // posts at the open front's corners
        m.add(box(lo, 1, hi - 2.5, lo + 1.5, top - 1, hi - 1, "paint"));
        m.add(box(lo, top - 2.5, lo + 2.5, lo + 1.5, top - 1, hi - 2.5, "paint")); // the lintel over the opening
        m.add(box(hi, 3, 6.5, hi + 1.5, 6, 9.5, "darksteel"));                    // guide shoes on the rail's fin
        m.add(box(hi, top - 6, 6.5, hi + 1.5, top - 3, 9.5, "darksteel"));
        m.add(box(hi - 2, 14, 5, hi - 1, 23, 11, "darksteel"));                   // the call panel, inside the back wall
        m.add(box(hi - 2.4, 19.5, 7, hi - 2, 21.5, 9, "paint_green"));
        m.add(box(hi - 2.4, 15.5, 7, hi - 2, 17.5, 9, "red"));
        return m;
    }

    static Model rail() {
        Model r = new Model();
        r.add(box(6, 0, 6, 10, 16, 10, "darksteel"));                             // the post
        r.add(box(1, 0, 7.5, 6, 16, 8.5, "steel"));                               // four guide fins
        r.add(box(10, 0, 7.5, 15, 16, 8.5, "steel"));
        r.add(box(7.5, 0, 1, 8.5, 16, 6, "steel"));
        r.add(box(7.5, 0, 10, 8.5, 16, 15, "steel"));
        return r;
    }

    static Model marker() {
        Model r = new Model();
        r.add(box(3, 3, 15, 13, 13, 16, "darksteel"));                            // the back plate on the rail
        r.add(box(4, 4, 14.5, 12, 12, 15, "steel"));                              // the face
        r.add(prism('z', 8, 8, 1.6, 13.8, 14.5, "red", 12));                      // the call button
        return r;
    }

    public static List<NodeModel> models() {
        NodeModel d = new NodeModel("servodrive", OilKit.PAINT_200V);
        d.part("main", drive());
        d.part("lamp", lamp());
        NodeModel c = new NodeModel("elevatorcar", OilKit.PAINT_200V);
        c.part("main", car());
        return List.of(d, c);
    }

    public static List<BlockModel> blocks() {
        return List.of(new BlockModel("servo_rail", rail(), "servo", "steel"), new BlockModel("station_marker", marker(), "servo", "steel"));
    }
}
