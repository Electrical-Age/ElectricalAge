package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.v;
import static mods.eln.cad.models.OilKit.obox;

/**
 * The tilted terminal (Baughn 2026-10-30, board #45): a three-channel data logger whose screen leans back 45 degrees, so
 * that on a desk it faces a player standing in front of it, and hung from a ceiling (the same model, upside down) it faces
 * a player about three blocks below. A six-node device: the face it is fixed to is the plane x = 8, it stands out along
 * +x; front -z. A mounting plinth, a low painted body with the terminal strip on its back (inputs A, B, C at y 4, 8, 12
 * out to the block's edge, where the voxel pins are), a lamp (part "led"), and the head: a slab tilted back, its screen
 * on the face toward the front. The graph's place is in main's floats (ElectricalDataLoggerDescriptor).
 */
public final class TiltedTerminal {
    private TiltedTerminal() {
    }

    static final double TILT = Math.toRadians(45);
    /** The head: its centre, its axes (n out of the screen, w across, h up the screen), half sizes. */
    static final double[] C = {13.2, 8, 9.8};
    static final double[] N = {Math.sin(TILT), 0, -Math.cos(TILT)}, W = {0, 1, 0}, H = {Math.cos(TILT), 0, Math.sin(TILT)};
    static final double HN = 1.4, HW = 5.0, HH = 4.0, SW = 4.3, SH = 3.3;

    static double[] at(double n, double w, double h) {
        return v(C[0] + N[0] * n + W[0] * w + H[0] * h, C[1] + N[1] * n + W[1] * w + H[1] * h, C[2] + N[2] * n + W[2] * w + H[2] * h);
    }

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(8.0, 3.0, 4.0, 9.0, 13.0, 14.0, "darksteel"));                 // plinth
        m.add(box(9.0, 3.5, 6.0, 11.5, 12.5, 13.5, "paint"));                    // body
        // the head, tilted back, a bezel round its screen
        m.add(obox(C, N, W, H, HN, HW, HH, "paint"));
        m.add(obox(at(HN + 0.15, 0, 0), N, W, H, 0.15, SW + 0.4, SH + 0.4, "darksteel"));
        m.add(obox(at(HN + 0.35, 0, 0), N, W, H, 0.05, SW, SH, "screen"));
        // terminal strip on the back, the three inputs on it
        m.add(box(9.0, 3.0, 13.5, 11.0, 13.0, 14.4, "darksteel"));
        double[] ty = {12, 8, 4};
        String[] col = {"red", "blue", "paint_green"};
        for (int i = 0; i < 3; i++) m.add(box(9.3, ty[i] - 0.8, 14.4, 10.7, ty[i] + 0.8, 16.0, col[i]));
        return m;
    }

    static List<Mesh> led() {
        return List.of(box(10.0, 11.2, 5.7, 10.7, 11.9, 6.0, "white"));
    }

    static String f(double px) {
        return Catalog.fmt((px - 8) / 16);
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("tiltedterminal", "paint_plain");
        n.part("main", main());
        n.part("led", led());
        // the graph: drawn y down in mx by my, scaled by s, turned (ra2 about z, then ra about y) so its x runs across
        // the screen (+y), its y down the screen (-h), its face out of it (n); its top-left corner 0.1 px over the glass
        double s = 0.2, inset = 0.3;
        double[] t = at(HN + 0.5, -SW + inset, SH - inset);
        n.txt.add("o main");
        for (String[] e : new String[][]{{"sx", "" + s}, {"sy", "" + s}, {"sz", "1"}, {"tx", f(t[0])}, {"ty", f(t[1])}, {"tz", f(t[2])},
            {"rx", "0"}, {"ry", "1"}, {"rz", "0"}, {"ra", "-45"}, {"rx2", "0"}, {"ry2", "0"}, {"rz2", "1"}, {"ra2", "90"},
            {"mx", "2.0"}, {"my", "1.5"}, {"reflc", "0"},
            // hung from a ceiling the model is upside down: the graph turns half round within the screen (its size here)
            {"flipW", Catalog.fmt((2 * (SW - inset)) / 16 / s)}, {"flipH", Catalog.fmt((2 * (SH - inset)) / 16 / s)}})
            n.txt.add("f " + e[0] + " " + e[1]);
        return List.of(n);
    }
}
