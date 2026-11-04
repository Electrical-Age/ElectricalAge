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
 * the 200 V terminal plate, signal plates on the flanks (floor: inputs, high: outputs) and the status lamp in front
 * (part "lamp", tinted by the render).</li>
 * <li>elevatorcar (node model, drawn by the car's entity renderer; +x toward the rail): a 1 px tread plate (the car's
 * collider is that thick), a crosshead frame on the rail side with guide shoes on the rail's fin, and the call panel
 * (up button over down).</li>
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
        m.addAll(turned(signalPlate(), 1));                                      // inputs: floor plates on the flanks
        m.addAll(turned(signalPlate(), 3));
        for (int k : new int[]{1, 3}) m.addAll(turned(outputPlate(), k));        // outputs: high on the flanks
        return m;
    }

    /** A signal plate high on the x = 0 face, for a cable on the neighbour's ceiling. */
    static List<Mesh> outputPlate() {
        return new ArrayList<>(List.of(box(0.1, 10.5, 6.5, 0.85, 14, 9.5, "steel", "nsewd"), box(0.1, 10.1, 6.5, 0.85, 10.5, 9.5, "darksteel", "nsewd"),
            box(0.0, 11.9, 7.6, 0.1, 12.6, 8.4, "copper", "nsewd")));
    }

    static List<Mesh> lamp() {
        return new ArrayList<>(List.of(box(0.35, 8, 6, 0.85, 11, 10, "white")));
    }

    static List<Mesh> car() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(0, 0, 0, 16, 1, 16, "plate"));                                  // the tread plate
        m.add(box(14.5, 1, 1, 16, 26, 2.5, "paint"));                             // the crosshead's posts
        m.add(box(14.5, 1, 13.5, 16, 26, 15, "paint"));
        m.add(box(15, 1, 2.5, 16, 26, 13.5, "steel"));                            // its back plate between them
        m.add(box(14.5, 26, 1, 16, 27.5, 15, "paint"));                           // the top beam
        m.add(box(16, 3, 6.5, 17.5, 6, 9.5, "darksteel"));                        // guide shoes on the rail's fin
        m.add(box(16, 21, 6.5, 17.5, 24, 9.5, "darksteel"));
        m.add(box(14, 14, 5, 15, 23, 11, "darksteel"));                           // the call panel
        m.add(box(13.6, 19.5, 7, 14, 21.5, 9, "paint_green"));                    // up
        m.add(box(13.6, 15.5, 7, 14, 17.5, 9, "red"));                            // down
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
