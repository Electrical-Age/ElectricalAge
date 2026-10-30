package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.OilKit.boltRing;
import static mods.eln.cad.models.OilKit.mirroredX;
import static mods.eln.cad.models.ShaftKit.pipeFlange;

/**
 * Counterflow Heat Exchanger: a horizontal shell-and-tube exchanger along x. Blocks stack 2 x 2 across (y, z) and 1..8
 * long (x) into one exchanger; a lone block works too. Parts: shell, one block (front x = 0, back x = 16): the render
 * scales it over the whole stack box (x by N, y by 2, z by 2), so everything here is either at the ends (square channel
 * headers that carry the flanges, girth flanges, feet) or periodic along x (hoop bands every 4 px), and round things
 * are round in y/z (scaled equally). flange: the end connection on the x = 0 face of ONE block, unscaled, drawn on
 * every end-face block of a stack (and turned 180 deg about y for the back end): stream A = large flange at the face
 * centre (y 8, z 8), stream B = small flange at (y 12, z 8), 0.1 px proud of A's face (B's face at x 0.1, A's at 0.2,
 * so both stay inside the block), short nozzles into the header. Labels: a paint ring on A's face, a copper ring on
 * B's.
 */
public final class HeatExchanger {
    private HeatExchanger() {
    }

    /** Centre (y and z) of the shell and of the headers; shell radius and sides. */
    static final double C = 8.0, R = 6.0;
    static final int N_SH = 16;
    /**
     * Channel header plate (x range at the front; mirrored at the back). Front at 0.3 px: scaled by the stack length
     * (0.3 N <= 2.4 px) it stays behind the flange discs (front 0.1, 0.2) and the nozzles reach it (3.2).
     */
    static final double[] HDR = {0.3, 0.75};
    /** Girth flange (tube sheet ring) between header and shell; hoop bands, period 4 px (divides 16). */
    static final double[] GIRTH = {0.75, 1.25}, BANDS = {2.0, 6.0, 10.0, 14.0};

    /** Header, girth flange and foot at the front end (x = 0 side); mirrored for the back. */
    static List<Mesh> frontEnd() {
        List<Mesh> ms = new ArrayList<>();
        // square channel header plate: covers the face area so every flange of a 2 x 2 stack lands on it
        ms.add(box(HDR[0], 1.0, 1.0, HDR[1], 15.0, 15.0, "castiron", "nsewu"));
        // header rim band (darker edge) and its bolts along the rim
        ms.add(box(HDR[1], 1.0, 1.0, GIRTH[0] + 0.3, 15.0, 1.6, "darksteel", "nsewu"));
        ms.add(box(HDR[1], 1.0, 14.4, GIRTH[0] + 0.3, 15.0, 15.0, "darksteel", "nsewu"));
        ms.add(box(HDR[1], 14.4, 1.6, GIRTH[0] + 0.3, 15.0, 14.4, "darksteel", "nsewu"));
        // girth flange (tube sheet ring) and bolts
        ms.add(prism('x', C, C, R + 0.75, GIRTH[0], GIRTH[1], "steel", N_SH, false, true));
        ms.addAll(boltRing('x', C, C, R + 0.4, GIRTH[1], GIRTH[1] + 0.25, 8, "brass", 0.22, 0));
        // foot under the header, on the floor at the face
        ms.add(box(0.0, 0.0, 2.0, 1.75, 1.0, 14.0, "darksteel", "nsewu"));
        ms.add(box(1.75, 0.0, 3.0, 2.5, 0.6, 13.0, "darksteel", "nsewu"));
        return ms;
    }

    static List<Mesh> shell() {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('x', C, C, R, GIRTH[1], 16.0 - GIRTH[1], "paint", N_SH, false, false));
        for (double x : BANDS) m.add(tube('x', C, C, R - 0.1, R + 0.35, x - 0.2, x + 0.2, "darksteel", N_SH, Double.NaN, false));
        // mid saddle (one, between the bands at 6 and 10) with a skid rail each side, floor to the shell
        // (the checker: the saddle's foot ended at z 3 / 13 in the planes of the rails' ends; it is 0.1 px narrower each side)
        m.add(box(7.0, 0.0, 3.1, 9.0, 1.0, 12.9, "castiron", "nsewu"));
        m.add(box(7.25, 1.0, 4.5, 8.75, 2.75, 11.5, "castiron", "nsew"));
        for (double z0 : new double[]{3.0, 12.25}) m.add(box(2.5, 0.0, z0, 13.5, 0.5, z0 + 0.75, "darksteel", "nsu"));
        List<Mesh> fe = frontEnd();
        m.addAll(fe);
        m.addAll(mirroredX(fe, 8.0));
        return m;
    }

    static List<Mesh> shiftX(List<Mesh> ms, double dx) {
        List<Mesh> out = new ArrayList<>();
        for (Mesh m : ms) out.add(m.mapped(p -> p[0] += dx, false));
        return out;
    }

    static List<Mesh> flange() {
        List<Mesh> m = new ArrayList<>();
        // stream A: large flange at the face centre, recessed 0.2 px, nozzle into the header; label: a paint ring on its
        // face (0.1 px proud of it, inside the bolt-free middle, below B's disc)
        // (the checker: each label ring, listed after its flange, wasn't seen as joined to it, a tube with a bore can't
        // hold anything; listed before its flange it is)
        m.add(tube('x', C, C, 1.5, 2.4, 0.1, 0.2, "paint", 12, Double.NaN, true));
        m.addAll(shiftX(pipeFlange("x0", C, C, "large", 2.0, "steel", -1), 0.2));
        // stream B: small flange at (y 12, z 8), its face (x 0.1) 0.1 px proud of A's (x 0.2); label: a copper ring on it
        m.add(tube('x', 12.0, C, 0.75, 1.25, 0.0, 0.1, "copper", 10, Double.NaN, true));
        m.addAll(shiftX(pipeFlange("x0", 12.0, C, "small", 2.0, "steel", -1), 0.1));
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("heatexchanger", ShaftKit.PAINT_HX);
        n.part("shell", shell());
        n.part("flange", flange());
        n.hitbox(0, 0, 1, 16, 15, 15);
        return List.of(n);
    }
}
