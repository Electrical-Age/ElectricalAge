package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.Mc.v;
import static mods.eln.cad.models.OilKit.boltRing;
import static mods.eln.cad.models.OilKit.obox;
import static mods.eln.cad.models.OilKit.signalPlate;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;
import static mods.eln.cad.models.ShaftKit.AX;
import static mods.eln.cad.models.ShaftKit.AY;
import static mods.eln.cad.models.ShaftKit.CENTRE_ORIGIN;
import static mods.eln.cad.models.ShaftKit.shaft;
import static mods.eln.cad.models.ShaftKit.shaftEnds;

/**
 * 200V Shaft Motor: a totally enclosed fan-cooled industrial motor lying across the shaft line, on a skid. Ribbed frame
 * (axial cooling fins), drive-end bell on the left (z = 0), a fan cowl with a grille on the right (z = 16) and the
 * external cooling fan behind it, terminal box on the front flank with a conduit down to the power floor terminal;
 * signal terminal on the back. The shaft comes out of both sides to half couplings. Parts: main, rotor (shaft,
 * couplings, cooling fan; turned about z through the shaft line). Front XN (x = 0), shaft along z at x = 8, y = 8.
 */
public final class ShaftMotor {
    private ShaftMotor() {
    }

    /** Frame radius, ribbed frame span, fan cowl span, fan blades (inside the cowl). */
    static final double R = 4.6, Z0 = 2.75, Z1 = 12.25;
    static final double[] COWL = {12.25, 15.0}, FAN = {13.1, 13.9};

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        // skid and feet
        m.add(box(2.0, 0, 1.75, 14.0, 1.0, 14.25, "darksteel", "nsewu"));
        for (double z : new double[]{3.5, 10.0}) {
            m.add(box(3.25, 1.0, z, 12.75, 1.6, z + 2.0, "castiron"));
            m.add(box(4.25, 1.6, z + 0.25, 11.75, AY - R + 1.25, z + 1.75, "paint"));
            for (double x : new double[]{3.75, 12.25}) m.add(prism('y', x, z + 1.0, 0.3, 1.6, 1.9, "brass", 6, false, true));
        }
        // ribbed frame: the drum and axial cooling fins
        m.add(prism('z', AX, AY, R, Z0, Z1, "paint", 14, false, false));
        int nf = 14;
        for (int k = 0; k < nf; k++) {
            double a = 2 * Math.PI * (k + 0.5) / nf;
            if (Math.sin(a) < -0.75) continue;            // (none under it, over the feet)
            double[] u = {Math.cos(a), Math.sin(a), 0};
            // (the checker: the fins ended at z 12.25 in the plane of the end ring's face; they end 0.1 px short of it)
            m.add(obox(v(AX + (R + 0.3) * u[0], AY + (R + 0.3) * u[1], (Z0 + Z1) / 2 + 0.2), u, v(-u[1], u[0], 0), v(0, 0, 1), 0.45, 0.17,
                (Z1 - Z0) / 2 - 0.3, "paint"));
        }
        // drive-end bell (left) and bearing cap, non-drive end shield (right)
        m.add(prism('z', AX, AY, R - 1.4, 1.5, Z0, "castiron", 14, R + 0.2, true, false, Double.NaN));
        m.add(tube('z', AX, AY, R - 0.1, R + 0.3, Z0, Z0 + 0.4, "darksteel", 14, Double.NaN, false));
        m.add(prism('z', AX, AY, 1.6, 1.0, 1.5, "darksteel", 10, true, false));
        m.addAll(boltRing('z', AX, AY, R - 0.6, 1.95, 2.2, 6, "brass", 0.2, 0));
        m.add(tube('z', AX, AY, R - 0.1, R + 0.3, Z1 - 0.4, Z1, "darksteel", 14, Double.NaN, false));
        // fan cowl: a sheet-steel shroud, open (dark) inside, a grille over its end; the fan turns behind it
        // (the checker: the cowl, the rim, the grille bars and the grille hub all ended in the plane z 15 and z-fought;
        // the cowl now ends 0.1 px inside the rim, the horizontal bars 0.2 px short of the rim's face, the hub starts
        // 0.1 px deeper than the vertical bars)
        m.add(tube('z', AX, AY, R - 0.2, R + 0.15, COWL[0], COWL[1] - 0.1, "steel", 14));
        m.add(prism('z', AX, AY, R - 0.15, Z1 - 0.01, Z1 + 0.2, "darksteel", 14, false, true));   // end shield face (dark)
        double gz = COWL[1] - 0.35;
        for (double y : new double[]{-3.0, -1.5, 1.5, 3.0}) {                     // horizontal grille bars (the hub bar carries the bearing)
            double w = Math.sqrt(Math.max(0, (R - 0.2) * (R - 0.2) - y * y));
            m.add(box(AX - w, AY + y - 0.25, gz, AX + w, AY + y + 0.25, COWL[1] - 0.2, "louvre"));
        }
        for (double x : new double[]{-3.0, -1.5, 1.5, 3.0}) {
            double w = Math.sqrt(Math.max(0, (R - 0.2) * (R - 0.2) - x * x));
            m.add(box(AX + x - 0.2, AY - w, gz - 0.2, AX + x + 0.2, AY + w, gz, "louvre"));
        }
        m.add(tube('z', AX, AY, 1.2, 1.9, gz - 0.3, COWL[1], "darksteel", 10));   // grille hub (the shaft through it)
        m.add(tube('z', AX, AY, R - 0.2, R + 0.35, COWL[1] - 0.45, COWL[1], "darksteel", 14, Double.NaN, false));   // rim
        // (the checker: the rim's inner face at z 14.6 lay 0.05 px off the bars' ends; the rim starts 0.05 px lower)
        // terminal box on the front flank, conduit down to the power terminal
        double tx0 = AX - R - 1.0;
        m.add(box(tx0, 6.0, 5.5, AX - R + 1.0, 10.0, 10.5, "paint"));
        m.add(box(tx0 - 0.3, 5.75, 5.25, tx0, 10.25, 10.75, "darksteel"));                 // lid
        for (double[] yz : new double[][]{{6.25, 5.5}, {6.25, 10.5}, {9.75, 5.5}, {9.75, 10.5}})
            m.add(prism('x', yz[0], yz[1], 0.2, tx0 - 0.5, tx0 - 0.3, "brass", 6, true, false));
        m.add(box(tx0 + 0.25, 1.0, 7.0, tx0 + 1.5, 6.0, 9.0, "darksteel"));                // conduit
        m.add(box(0.85, 0.0, 6.5, tx0 + 1.5, 1.4, 9.5, "darksteel", "nsewu"));            // into the plate
        m.addAll(terminalPlate());
        // signal: a small junction box on the back flank, conduit down to the back terminal
        double sx = AX + R + 0.5;
        m.add(box(sx - 0.5, 6.5, 6.75, sx + 1.0, 8.5, 9.25, "steel"));
        m.add(box(sx, 1.0, 7.5, sx + 0.75, 6.5, 8.5, "darksteel"));
        m.add(box(sx, 0.0, 7.25, 15.15, 1.2, 8.75, "darksteel", "nsewu"));
        m.addAll(turned(signalPlate(), 2));
        // lifting eye
        // (the checker: drawn after its stem the eye counted as unjoined, a tube with a bore can't hold anything; drawn
        // first, its foot is seen inside the stem)
        m.add(tube('x', AY + R + 1.6, 8.0, 0.4, 0.9, AX - 0.25, AX + 0.25, "steel", 8));
        m.add(box(AX - 0.25, AY + R + 0.4, 7.25, AX + 0.25, AY + R + 1.6, 8.75, "steel"));
        return m;
    }

    static List<Mesh> rotor() {
        List<Mesh> m = new ArrayList<>(shaftEnds(1.0, COWL[1] - 0.4));
        m.add(shaft(COWL[0], COWL[1] - 0.4));
        // (the checker: the shaft wasn't drawn inside the motor, so the drive-end coupling wasn't joined to the rest; it
        // now runs through, hidden in the frame)
        m.add(shaft(1.0, COWL[0]));
        // external cooling fan: hub and pitched radial blades
        m.add(prism('z', AX, AY, 1.4, FAN[0] - 0.3, FAN[1] + 0.1, "darksteel", 10));
        int nb = 8;
        double p = Math.toRadians(35), rc = (1.3 + R - 0.5) / 2;               // blade pitch
        for (int k = 0; k < nb; k++) {
            double a = 2 * Math.PI * k / nb;
            double[] u = {Math.cos(a), Math.sin(a), 0}, t = {-u[1], u[0], 0};
            double[] bt = {Math.cos(p) * t[0], Math.cos(p) * t[1], Math.sin(p)}, bn = {-Math.sin(p) * t[0], -Math.sin(p) * t[1], Math.cos(p)};
            m.add(obox(v(AX + rc * u[0], AY + rc * u[1], (FAN[0] + FAN[1]) / 2), u, bt, bn, (R - 0.5 - 1.3) / 2, 0.75, 0.08, "steel"));
        }
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("shaftmotor", ShaftKit.PAINT_MOTOR);
        n.part("main", main());
        n.part("rotor", rotor());
        n.origin("rotor", CENTRE_ORIGIN[0], CENTRE_ORIGIN[1], CENTRE_ORIGIN[2]);
        n.hitbox(2.0, 0.0, 1.75, 14.0, 1.0, 14.25,                              // skid
            AX - R - 0.6, 1.0, 1.0, AX + R + 0.6, AY + R + 0.6, COWL[1],             // feet, frame, fins, cowl
            AX - R - 1.3, 1.0, 5.25, AX - R + 1.0, 10.25, 10.75,                     // terminal box and conduit
            0.0, 0.0, 4.25, 1.0, 4.5, 11.75,                                         // power terminal
            15.0, 0.0, 6.5, 16.0, 8.5, 9.5,                                          // signal box and terminal
            AX - 2.25, AY - 2.25, 0.0, AX + 2.25, AY + 2.25, 16.0);                  // shaft and couplings
        return List.of(n);
    }
}
