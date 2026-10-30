package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;

/**
 * The three-channel data logger (Baughn 2026-10-30, board #45): a small field logger standing on the floor, a six-node
 * device, so the floor is the plane x = 8 and it stands up along +x. A painted case on a dark skid, a carrying handle on
 * top, the screen on its front (-z) where the render draws the graph (main's floats, as the 1.12 loggers' models), a
 * lamp (part "led"), and on the back (+z) a terminal strip with the three inputs at y 4, 8 and 12, where the voxel
 * system's three pins on that edge are, coloured A red, B blue, C green.
 */
public final class FieldLogger {
    private FieldLogger() {
    }

    static final double SX0 = 10.2, SX1 = 14.3, SY0 = 4.5, SY1 = 11.5, SZ = 2.9;
    /** The terminals' places along the back edge (px) and colours, A, B, C. */
    static final double[] TERMINAL_Y = {12, 8, 4};
    static final String[] TERMINAL = {"red", "blue", "paint_green"};

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(8.0, 2.5, 3.0, 8.8, 13.5, 13.0, "darksteel"));                 // skid
        m.add(box(8.8, 3.0, 3.5, 15.5, 13.0, 12.5, "paint"));                    // case
        m.add(box(9.5, 3.8, 3.0, 15.0, 12.2, 3.5, "darksteel"));                 // bezel on the front
        m.add(box(SX0, SY0, SZ, SX1, SY1, 3.0, "screen"));                        // the screen
        // carrying handle: two posts and a grip
        m.add(box(15.5, 4.5, 7.5, 16.5, 5.5, 8.5, "darksteel"));
        m.add(box(15.5, 10.5, 7.5, 16.5, 11.5, 8.5, "darksteel"));
        m.add(box(16.5, 4.2, 7.3, 17.1, 11.8, 8.7, "rubber"));
        // terminal strip on the back, the three inputs on it
        m.add(box(9.0, 3.0, 12.5, 11.0, 13.0, 13.4, "darksteel"));
        for (int i = 0; i < 3; i++) {
            double y = TERMINAL_Y[i];
            // (out to the block's edge, where the cables end)
            m.add(box(9.3, y - 0.8, 13.4, 10.7, y + 0.8, 16.0, TERMINAL[i]));
        }
        return m;
    }

    static List<Mesh> led() {
        return List.of(box(13.6, 12.3, 3.0, 14.4, 12.9, 3.5, "white"));
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("fieldlogger", "paint_condenser");
        n.part("main", main());
        n.part("led", led());
        // the graph's place (ElectricalDataLoggerDescriptor: translate, rotate ra about (rx, ry, rz), scale; a graph of
        // mx by my drawn y down): along y across the screen, down its height, 0.1 px in front of it
        double s = 0.2;
        n.txt.add("o main");
        for (String[] f : new String[][]{{"sx", "" + s}, {"sy", "" + s}, {"sz", "1"}, {"tx", Catalog.fmt((SX1 - 8) / 16 - 0.01)},
            {"ty", Catalog.fmt((SY0 - 8) / 16 + 0.01)}, {"tz", Catalog.fmt((SZ - 0.1 - 8) / 16)}, {"rx", "0"}, {"ry", "0"}, {"rz", "1"}, {"ra", "90"},
            {"mx", "1.6"}, {"my", "1.0"}, {"reflc", "0"}})
            n.txt.add("f " + f[0] + " " + f[1]);
        return List.of(n);
    }
}
