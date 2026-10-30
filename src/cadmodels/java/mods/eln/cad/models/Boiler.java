package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.v;
import static mods.eln.cad.models.OilKit.boltRing;
import static mods.eln.cad.models.OilKit.dial;
import static mods.eln.cad.models.OilKit.handwheel;
import static mods.eln.cad.models.OilKit.heatPad;
import static mods.eln.cad.models.OilKit.obox;
import static mods.eln.cad.models.OilKit.signalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * Boiler: a riveted horizontal steam drum on saddles over a cast-iron heat-collector base. Drawn by BoilerRender ->
 * BoilerDescriptor.draw() (parts main, level, needle), after front.glRotateXnRef(). Connections (BoilerElement):
 * thermal on the back and both flanks at the floor, the phase signal on the front; pipe ports: the top flange
 * (steam), the feedwater flange on the right flank (through a check valve) and the dome port centred underneath.
 * Front XN (x = 0).
 */
public final class Boiler {
    private Boiler() {
    }

    static final double CY = 9.25, CZ = 8.0, R = 5.25;
    static final double[] DX = {2.25, 13.75};
    /** BoilerRender scales "level" in y about the column's foot and turns "needle" about x at the dial's centre. */
    static final double[] GAUGE = {0.9, 6.45, CZ - 2.4}, DIAL = {0.5, CY + 2.6, CZ + 1.6};

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        // base: heat collector, top plate, heat pads on three faces (the front is the phase signal's terminal)
        m.add(box(1.25, 0, 1.25, 14.75, 3.25, 14.75, "castiron", "nsewu"));
        m.add(box(1.5, 3.25, 1.5, 14.5, 3.6, 14.5, "darksteel", "nsewu"));
        for (int k : new int[]{1, 2, 3}) m.addAll(turned(heatPad(1.0, 4.0), k));
        m.addAll(turned(signalPlate(), 0));
        // (the checker: the signal plate stood 0.4 px clear of the base; a backing joins it, hidden behind the plate)
        m.add(box(0.85, 0, 6.5, 1.25, 3.5, 9.5, "steel", "nsu"));
        for (int k = 0; k < 4; k++) m.addAll(turned(List.of(box(1.0, 0, 1.0, 2.0, 3.0, 2.0, "darksteel", "nsewu")), k));
        // saddles
        for (double x : new double[]{3.25, 11.25}) {
            m.add(box(x, 3.6, 4.5, x + 1.5, 5.4, 11.5, "paint", "nsewu"));
            m.add(box(x - 0.25, 3.6, 4.25, x + 1.75, 4.1, 11.75, "paint", "nsewu"));
        }
        // drum shell, dished heads, seam bands
        m.add(prism('x', CY, CZ, R, DX[0], DX[1], "rivet", 16, false, false));
        m.add(prism('x', CY, CZ, R - 1.5, DX[0] - 1.0, DX[0], "rivet", 16, R, true, false, Double.NaN));
        m.add(prism('x', CY, CZ, R, DX[1], DX[1] + 1.0, "rivet", 16, R - 1.5, false, true, Double.NaN));
        for (double x : new double[]{5.6, 10.0}) m.add(prism('x', CY, CZ, R + 0.15, x, x + 0.4, "darksteel", 16, false, false));
        double hx = DX[0] - 1.0;
        // manhole on the back head
        m.add(prism('x', CY - 0.5, CZ, 1.6, DX[1] + 1.0, DX[1] + 1.4, "darksteel", 8));
        m.add(box(DX[1] + 1.4, CY - 0.75, CZ - 1.0, DX[1] + 1.7, CY - 0.25, CZ + 1.0, "darksteel"));
        // gauge glass on the front head between two brass cocks, guard rods, drain cock, the wire down to the terminal
        double gz = CZ - 2.4;
        for (double y : new double[]{5.9, 11.6}) m.add(box(0.35, y, gz - 0.55, hx + 1.3, y + 0.6, gz + 0.55, "brass"));
        m.add(box(0.3, 6.5, gz - 0.65, 0.45, 11.5, gz - 0.5, "darksteel"));
        m.add(box(0.3, 6.5, gz + 0.5, 0.45, 11.5, gz + 0.65, "darksteel"));
        m.add(prism('y', 0.9, gz, 0.2, 5.1, 5.9, "brass", 6));
        m.add(box(0.35, 4.5, gz - 0.25, 0.85, 5.1, gz + 0.25, "darksteel"));
        // pressure gauge on a siphon above the head (its needle: part needle)
        m.add(prism('x', CY + 2.6, CZ + 1.6, 0.2, 0.9, hx + 0.6, "brass", 6, false, false));
        m.addAll(dial(0.5, CY + 2.6, CZ + 1.6, 1.1, "xn", 0.4).subList(0, 2));
        // steam dome and the outlet flange on top (centred: the top port)
        double sx = 8.0, top = CY + R * Math.cos(Math.PI / 16);
        m.add(prism('y', sx, CZ, 1.7, top - 1.0, top + 0.5, "rivet", 10, false, true));
        m.add(prism('y', sx, CZ, 2.0, top + 0.5, 15.0, "steel", 12, false, false));
        m.add(prism('y', sx, CZ, 2.75, 15.0, 16.0, "steel", 12));
        // feedwater inlet on the z+ flank: pipe, check valve and handwheel, reducer, an iron pipe's flange at the face
        double fx = 8.0, fy = 8.0;
        m.add(prism('z', fx, fy, 0.7, CZ + 4.0, 12.9, "darksteel", 8, false, false));
        m.add(box(fx - 1.3, fy - 1.3, 12.9, fx + 1.3, fy + 1.3, 14.3, "castiron"));
        m.add(prism('y', fx, 13.6, 0.25, fy + 1.3, fy + 2.3, "darksteel", 6, false, false));
        m.addAll(handwheel(fx, fy + 2.4, 13.6, 0.9, 'y'));
        m.add(prism('z', fx, fy, 1.0, 14.3, 14.7, "steel", 8, 2.0, false, false, Double.NaN));
        m.add(prism('z', fx, fy, 2.0, 14.7, 15.0, "steel", 12, false, false));
        m.add(prism('z', fx, fy, 2.75, 15.0, 16.0, "steel", 12));
        // the dome pipe down the z- flank into the base and out of a flange on the underside (the dome port)
        double py = 14.4, pz = 1.9, pr = 0.6;
        m.add(prism('z', sx, py, pr, pz, CZ - 1.0, "steel", 8, false, false));
        m.add(prism('y', sx, pz, pr + 0.15, py - 0.75, py + 0.75, "darksteel", 8));
        m.add(prism('y', sx, pz, pr, 3.6, py - 0.75, "steel", 8, false, false));
        // (gland radius pr + 0.2, was + 0.35: its side lay 0.02 px off the heat pad's backing and z-fought)
        m.add(prism('y', sx, pz, pr + 0.2, 3.6, 4.1, "steel", 8));
        m.add(prism('y', 8.0, 8.0, 2.75, 0.0, 1.0, "steel", 12, true, false));
        m.addAll(boltRing('y', 8.0, 8.0, 1.5, 0.0, 0.05, 4, "brass", 0.22, 0));
        // nameplate on the z- flank of the base
        m.add(box(12.7, 1.0, 1.15, 13.9, 2.5, 1.25, "brass", "n").free("a nameplate decal, 0.1 px proud of the base's face"));
        return m;
    }

    static List<Mesh> level() {
        // half the glass's height, so it maps onto the water cell's blue half; the render stretches it (x 0..2)
        return List.of(prism('y', GAUGE[0], GAUGE[2], 0.4, GAUGE[1], GAUGE[1] + 2.575, "water", 6));
    }

    static List<Mesh> needle() {
        double x = DIAL[0], y = DIAL[1], z = DIAL[2];
        // (0.05 px further out than the Python's: the kit dial's glass is 0.1 px thick now)
        return List.of(obox(v(x - 0.15, y + 0.3, z), v(1, 0, 0), v(0, 1, 0), v(0, 0, 1), 0.04, 0.38, 0.07, "darksteel"),
            prism('x', y, z, 0.15, x - 0.2, x - 0.1, "darksteel", 6, true, false));
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("boiler", OilKit.PAINT_BOILER);
        n.part("main", main());
        n.part("level", level());
        n.part("needle", needle());
        n.origin("level", GAUGE[0], GAUGE[1], GAUGE[2]).origin("needle", DIAL[0], DIAL[1], DIAL[2]);
        return List.of(n);
    }
}
