package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.v;
import static mods.eln.cad.models.OilKit.dial;
import static mods.eln.cad.models.OilKit.obox;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * Electric heater (50V / 200V): a resistor cabinet that pushes all its heat out of its back into a boiler or column.
 * Drawn by ElectricHeaterRender -> ElectricHeaterDescriptor.draw() (parts main and element, the coils, which glow when
 * hot), after front.glRotateXnRef(). Connections (ElectricHeaterElement): electrical power on the two side faces
 * (z = 0 and z = 16, at the floor), thermal on the back (x = 16), nothing on the front. Front XN (x = 0):
 * <ul>
 * <li>front: framed window with a grille over glowing coiled elements on ceramic end insulators, a dial, an amber
 * pilot lamp and a nameplate on the top rail (decoration: the model is static, the regulator lives in the GUI);</li>
 * <li>top: tier-painted cap with a louvred vent;</li>
 * <li>sides: floor terminal plates (copper studs, tier stripe) with a terminal box behind each;</li>
 * <li>back: copper heat pipes into a cast-iron backing plate and a full-height heat-tinted copper contact flange that
 * presses against the boiler (down to the floor, 11 px wide at the bottom middle).</li>
 * </ul>
 */
public final class ElectricHeater {
    private ElectricHeater() {
    }

    /**
     * fx: frame front x; bx: body front (window depth); z: cabinet z span; top: cabinet top; win: window y span; el:
     * element heights; er: element radius; fl: flange (z0, z1, top); pipes: heat pipe (y, z); bolts: (y, z).
     */
    record Tier(String name, String paint, double fx, double bx, double[] z, double top, double[] win, double[] el, double er, double[] fl,
                double[][] pipes, double[][] bolts) {
    }

    static final List<Tier> TIERS = List.of(
        new Tier("50v", OilKit.PAINT_50V, 1.5, 3.25, new double[]{3.0, 13.0}, 11.25, new double[]{2.75, 9.75}, new double[]{4.0, 6.25, 8.5}, 0.65,
            new double[]{2.75, 13.25, 11.75}, new double[][]{{3.5, 5.5}, {3.5, 10.5}, {8.0, 5.5}, {8.0, 10.5}},
            new double[][]{{1.0, 2.75}, {1.0, 13.25}, {11.5, 2.75}, {11.5, 13.25}, {6.25, 2.75}, {6.25, 13.25}}),
        new Tier("200v", OilKit.PAINT_200V, 1.0, 2.9, new double[]{2.0, 14.0}, 12.75, new double[]{2.75, 11.25}, new double[]{3.9, 6.0, 8.1, 10.2}, 0.7,
            new double[]{1.75, 14.25, 13.25}, new double[][]{{3.0, 4.75}, {3.0, 11.25}, {7.0, 4.75}, {7.0, 11.25}, {11.0, 4.75}, {11.0, 11.25}},
            new double[][]{{1.0, 1.75}, {1.0, 14.25}, {13.0, 1.75}, {13.0, 14.25}, {5.0, 1.75}, {5.0, 14.25}, {9.0, 1.75}, {9.0, 14.25}}));

    /** The cabinet (part main) and the coils (part element, drawn again glowing when hot: Incandescence). */
    static List<List<Mesh>> heater(Tier t) {
        List<Mesh> m = new ArrayList<>(), hot = new ArrayList<>();
        double fx = t.fx, bx = t.bx, z0 = t.z[0], z1 = t.z[1], top = t.top, w0 = t.win[0], w1 = t.win[1];
        double zi0 = z0 + 1.25, zi1 = z1 - 1.25;   // window z span
        double back = 12.5;                         // cabinet back
        double bp = 13.6;                           // backing plate front
        // plinth (reaches the front face at the floor)
        m.add(box(0.1, 0, z0 - 0.25, bp, 1.25, z1 + 0.25, "darksteel", "nsewu"));
        // cabinet body behind the window, dark inner wall, framed front
        m.add(box(bx, 1.25, z0, back, top, z1, "steel", "nseu"));
        m.add(box(bx - 0.1, w0, zi0, bx, w1, zi1, "darksteel", "w"));
        m.add(box(fx, 1.25, z0, bx, top, zi0, "steel", "nswu"));
        m.add(box(fx, 1.25, zi1, bx, top, z1, "steel", "nswu"));
        m.add(box(fx, w1, zi0, bx, top, zi1, "steel", "wud"));
        m.add(box(fx, 1.25, zi0, bx, w0, zi1, "steel", "wu"));
        // heating elements: coil on a rod between ceramic end insulators
        double ex = (fx + bx) / 2 + 0.25;
        for (double y : t.el) {
            // (the checker: part element is checked on its own, where the coils don't touch; each hangs between its
            // insulators in part main)
            hot.add(prism('z', ex, y, t.er, zi0 + 0.75, zi1 - 0.75, "coil", 6, false, false).free("a coil, held by its ceramic insulators in part main"));
            m.add(prism('z', ex, y, t.er + 0.25, zi0 + 0.05, zi0 + 0.75, "ceramic", 6, false, true));
            m.add(prism('z', ex, y, t.er + 0.25, zi1 - 0.75, zi1 - 0.05, "ceramic", 6, true, false));
        }
        // grille: vertical bars and one cross bar in front of the elements
        int nb = (int) ((zi1 - zi0) / 1.25);
        for (int k = 1; k < nb; k++) {
            double z = zi0 + (zi1 - zi0) * k / nb;
            m.add(box(fx + 0.05, w0, z - 0.15, fx + 0.35, w1, z + 0.15, "darksteel", "nsw"));
        }
        m.add(box(fx - 0.05, (w0 + w1) / 2 - 0.15, zi0, fx + 0.05, (w0 + w1) / 2 + 0.15, zi1, "darksteel", "wud"));
        // top rail: dial, pilot lamp, nameplate
        double ry = (w1 + top) / 2, rr = Math.min(0.7, (top - w1) / 2 - 0.1);
        // (the checker: the dial's case was flush with the rail's face and z-fought it; the dial stands 0.1 px proud)
        double dx = fx - 0.1, dz = zi1 - 1.2;
        m.addAll(dial(dx, ry, dz, rr, "xn", 0.3));
        m.add(prism('x', ry, zi0 + 1.0, rr * 0.6, fx - 0.3, fx, "lamp", 6, true, false));
        m.add(prism('x', ry, zi0 + 1.0, rr * 0.85, fx - 0.15, fx, "darksteel", 6, true, false));
        m.add(box(fx - 0.1, ry - rr * 0.6, zi0 + 2.2, fx, ry + rr * 0.6, zi1 - 2.4, "brass", "w").free("a nameplate decal, 0.1 px proud of the rail"));
        // tier cap and louvred vent hood
        m.add(box(fx - 0.15, top, z0 - 0.15, back + 0.15, top + 0.75, z1 + 0.15, "paint"));
        m.add(box(fx + 2.0, top + 0.75, z0 + 1.0, back - 1.25, top + 1.2, z1 - 1.0, "louvre", "nsewu"));
        // side terminals: floor plates on both z faces, terminal boxes behind them, conduit into the cabinet
        List<Mesh> side = List.of(box(5.0, 1.25, 0.85, 11.0, 5.5, z0, "steel", "nweu"),
            box(4.85, 5.5, 0.7, 11.15, 5.85, z0, "darksteel", "nweud"),
            prism('y', 8.0, (0.85 + z0) / 2, 0.45, 5.85, 6.5, "brass", 6, false, true));
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        m.addAll(side);
        m.addAll(turned(side, 2));
        // heat path: copper heat pipes into the backing plate, bolts, hot contact flange on the back face
        double fz0 = t.fl[0], fz1 = t.fl[1], ftop = t.fl[2];
        for (double[] p : t.pipes) m.add(prism('x', p[0], p[1], 0.6, back, bp, "copper", 6, false, false));
        m.add(box(bp, 0, fz0 - 0.5, 14.1, ftop + 0.5, fz1 + 0.5, "castiron", "nsewu"));
        m.add(box(14.1, 0, fz0, 15.9, ftop, fz1, "hotcopper", "nseu"));
        for (double[] b : t.bolts) {
            double zz = b[1] < 8 ? b[1] - 0.25 : b[1] + 0.25;
            m.add(prism('x', b[0], zz, 0.3, bp - 0.3, bp, "brass", 6, true, false));
        }
        return List.of(m, hot);
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        for (Tier t : TIERS) {
            NodeModel n = new NodeModel("electricheater" + t.name, t.paint);
            List<List<Mesh>> h = heater(t);
            n.part("main", h.get(0));
            n.part("element", h.get(1));
            out.add(n);
        }
        return out;
    }
}
