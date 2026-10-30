package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.Mc.v;
import static mods.eln.cad.models.OilKit.handwheel;
import static mods.eln.cad.models.OilKit.obox;
import static mods.eln.cad.models.OilKit.signalPlate;
import static mods.eln.cad.models.ShaftKit.AX;
import static mods.eln.cad.models.ShaftKit.AY;
import static mods.eln.cad.models.ShaftKit.CENTRE_ORIGIN;
import static mods.eln.cad.models.ShaftKit.pipeFlange;
import static mods.eln.cad.models.ShaftKit.shaft;
import static mods.eln.cad.models.ShaftKit.shaftEnds;

/**
 * Gas Turbine: a compact industrial gas turbine lying across the shaft line, on a skid. Along the shaft (z): the
 * starter/accessory end and compressor casing on the left (z = 0: the starter motor couples there), the air intake
 * plenum in the middle with a louvred bellmouth grille on the front (the compressor wheel, in rotor, turns behind it),
 * the hot exhaust collector behind the plenum with two combustor cans on top, the turbine casing on the right. Ports:
 * hot exhaust at the back centre (large flange, the collector and duct heat-tinted); fuel at a small flange on the top
 * centre, piped through the fuel valve (valve: its stem and handwheel) to the cans. Signal floor terminal on the front
 * under the grille. Parts: main, rotor (turned about z through the shaft line), valve (lifted by the render with the
 * valve's opening). Front XN (x = 0), shaft along z at x = 8, y = 8.
 */
public final class ShaftGas {
    private ShaftGas() {
    }

    /** Intake plenum spans (x: front open, grille; back: the collector), plate thickness. */
    static final double[] PX = {1.0, AX}, PY = {4.25, 12.75}, PZ = {5.5, 10.5};
    static final double T = 0.5, FAN_R = 3.4, FUEL_Y = 13.0, VX = 9.9;
    static final double[][] CANS = {{11.5, 6.5}, {11.5, 9.5}};

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        // skid, with feet out to the back face at the corners (the floor middle stays clear)
        m.add(box(1.0, 0, 1.25, 15.0, 1.0, 14.75, "darksteel", "nsewu"));
        for (double z0 : new double[]{1.5, 12.5}) m.add(box(15.0, 0, z0, 16.0, 0.75, z0 + 2.0, "darksteel", "nsewu"));
        // compressor casing (left), its saddle; the starter end bell with a bolted pad
        m.add(prism('z', AX, AY, 4.25, 1.75, PZ[0] + 0.25, "paint", 12, false, true));
        m.add(prism('z', AX, AY, 2.6, 1.0, 1.75, "castiron", 12, 4.25, true, false, Double.NaN));
        m.add(tube('z', AX, AY, 4.15, 4.6, 4.75, 5.15, "darksteel", 12, Double.NaN, false));
        m.add(box(4.25, 1.0, 2.25, 11.75, AY - 3.5, 4.25, "castiron"));
        // intake plenum: four plates round an open front (the grille) and the collector behind
        m.add(box(PX[0], PY[1] - T, PZ[0], PX[1], PY[1], PZ[1], "paint"));             // roof
        m.add(box(PX[0], PY[0], PZ[0], PX[1], PY[0] + T, PZ[1], "paint"));             // floor
        m.add(box(PX[0], PY[0] + T, PZ[0], PX[1], PY[1] - T, PZ[0] + T, "paint"));     // side walls
        m.add(box(PX[0], PY[0] + T, PZ[1] - T, PX[1], PY[1] - T, PZ[1], "paint"));
        m.add(box(PX[0] + 0.5, 1.0, 6.0, PX[1], PY[0], 10.0, "castiron"));              // its plinth
        // bellmouth: a steel frame round the opening, standing 0.5 px proud of the plenum
        double fx0 = PX[0] - 0.5, fx1 = PX[0] + 0.25;
        m.add(box(fx0, PY[1] - 0.75, PZ[0] - 0.25, fx1, PY[1] + 0.25, PZ[1] + 0.25, "steel"));
        m.add(box(fx0, PY[0] - 0.25, PZ[0] - 0.25, fx1, PY[0] + 0.75, PZ[1] + 0.25, "steel"));
        m.add(box(fx0, PY[0] + 0.75, PZ[0] - 0.25, fx1, PY[1] - 0.75, PZ[0] + 0.75, "steel"));
        m.add(box(fx0, PY[0] + 0.75, PZ[1] - 0.75, fx1, PY[1] - 0.75, PZ[1] + 0.25, "steel"));
        // louvres: tilted slats across the opening (the wheel shows between them)
        // (the checker: the slats ended 0.25 px short of the side walls, held only by the frame's edge; 0.35 px longer
        // at each end they reach into the walls, behind the frame)
        for (double y = PY[0] + 1.4; y < PY[1] - 1.0; y += 1.2)
            m.add(obox(v(PX[0] + 0.6, y, 8.0), v(0.8, 0.6, 0), v(-0.6, 0.8, 0), v(0, 0, 1), 0.55, 0.08, (PZ[1] - PZ[0]) / 2 - 0.4, "louvre"));
        // mullion (the checker: it stood between the frame bars, barely touching them; it now runs 0.5 px further up and
        // down into the roof and floor plates, behind the frame)
        m.add(box(PX[0] + 0.2, PY[0] + 0.25, 7.85, PX[0] + 0.5, PY[1] - 0.25, 8.15, "darksteel"));
        // exhaust collector behind the plenum (heat-tinted), on a pedestal, the duct to the back flange
        m.add(box(PX[1] + 0.5, 1.0, 5.0, 14.5, 3.0, 11.0, "darksteel"));
        m.add(box(PX[1], 3.0, 5.0, 14.5, 11.0, 11.0, "hotcopper"));
        // stiffener bands (the checker: the first band's face at x 10.5 lay 0.016 px off the cans' front flats; it moved
        // 0.2 px to the front)
        for (double x : new double[]{10.3, 13.0}) m.add(box(x, 2.9, 4.9, x + 0.4, 11.1, 11.1, "darksteel", "nsewu"));
        m.add(prism('x', AY, 8.0, 3.25, 14.5, 15.0, "hotcopper", 14, false, false));
        m.addAll(pipeFlange("x16", AY, 8.0, "large", 0.5, "steel", -1));
        // turbine casing (right): flanged, a cone down to the shaft
        m.add(prism('z', AX, AY, 3.75, 11.0, 14.25, "castiron", 14, true, false));
        m.add(prism('z', AX, AY, 3.75, 14.25, 15.0, "castiron", 14, 2.6, false, true, Double.NaN));
        m.add(tube('z', AX, AY, 3.65, 4.15, 13.0, 13.4, "darksteel", 14, Double.NaN, false));
        m.add(box(4.5, 1.0, 12.0, 11.5, AY - 3.25, 13.5, "castiron"));
        // combustor cans on the collector, the fuel manifold and line to the top flange through the valve
        for (double[] c : CANS) {
            double x = c[0], z = c[1];
            m.add(prism('y', x, z, 1.1, 10.75, 12.25, "steel", 8, false, false));
            m.add(prism('y', x, z, 1.1, 12.25, 12.75, "steel", 8, 0.5, false, true, Double.NaN));
            m.add(tube('y', x, z, 1.0, 1.35, 11.25, 11.6, "darksteel", 8, Double.NaN, false));
            // injector (the checker: r 0.3, its flats 0.04 px inside the manifold's; now r 0.2)
            m.add(prism('y', x, z, 0.2, 12.75, FUEL_Y, "brass", 6, false, false));
        }
        m.add(prism('z', CANS[0][0], FUEL_Y, 0.35, CANS[0][1] - 0.35, CANS[1][1] + 0.35, "copper", 6));
        m.add(prism('x', FUEL_Y, 8.0, 0.35, 8.0, CANS[0][0], "copper", 6, false, false));
        // (the checker: the flange stub's bottom cap at y 12.65 lay 0.05 px under the fuel line's underside; the stub runs
        // 0.15 px deeper, inside the riser)
        m.addAll(pipeFlange("y16", AX, 8.0, "small", 15.0 - FUEL_Y + 0.5, "steel", -1));
        m.add(prism('y', AX, 8.0, 0.75, FUEL_Y - 0.6, 13.5, "steel", 8, true, false));
        m.add(box(VX - 0.55, FUEL_Y - 0.6, 7.45, VX + 0.55, FUEL_Y + 0.6, 8.55, "castiron"));      // valve body
        m.add(prism('y', VX, 8.0, 0.45, FUEL_Y + 0.6, FUEL_Y + 1.0, "steel", 8));               // bonnet
        // signal terminal on the front, a conduit back into the skid
        m.addAll(signalPlate());
        m.add(box(0.85, 0, 7.25, 1.75, 1.25, 8.75, "darksteel", "nsewu"));
        return m;
    }

    static List<Mesh> rotor() {
        // (the checker: the shaft was drawn only from the couplings to the casings and through the plenum, so the
        // couplings weren't joined to the wheel; one shaft now runs through, hidden in the casings)
        List<Mesh> m = new ArrayList<>(shaftEnds(PZ[0], PZ[1]));
        m.add(shaft(PZ[0], PZ[1]));
        // compressor wheel: hub with a nose cone and pitched blades
        m.add(prism('z', AX, AY, 1.3, 7.0, 9.0, "darksteel", 10));
        int nb = 8;
        double p = Math.toRadians(40), rc = (1.2 + FAN_R) / 2;
        for (int k = 0; k < nb; k++) {
            double a = 2 * Math.PI * (k + 0.5) / nb;
            double[] u = {Math.cos(a), Math.sin(a), 0}, t = {-u[1], u[0], 0};
            double[] bt = {Math.cos(p) * t[0], Math.cos(p) * t[1], Math.sin(p)}, bn = {-Math.sin(p) * t[0], -Math.sin(p) * t[1], Math.cos(p)};
            m.add(obox(v(AX + rc * u[0], AY + rc * u[1], 8.0), u, bt, bn, (FAN_R - 1.2) / 2, 1.0, 0.08, "steel"));
        }
        return m;
    }

    static List<Mesh> valve() {
        double top = FUEL_Y + 1.0;
        List<Mesh> m = new ArrayList<>();
        // (the checker: the stem's flats lay 0.04 px off the handwheel's spoke; it ends 0.15 px lower, in the hub)
        m.add(prism('y', VX, 8.0, 0.18, top, top + 0.1, "steel", 6, false, true));
        m.addAll(handwheel(VX, top + 0.25, 8.0, 0.8, 'y'));
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("shaftgas", ShaftKit.PAINT_GAS);
        n.part("main", main());
        n.part("rotor", rotor());
        n.part("valve", valve());
        n.origin("rotor", CENTRE_ORIGIN[0], CENTRE_ORIGIN[1], CENTRE_ORIGIN[2]).origin("valve", VX, FUEL_Y + 1.0, 8.0);
        n.hitbox(1.0, 0.0, 1.25, 16.0, 1.0, 14.75,        // skid and feet
            3.75, 1.0, 1.0, 12.25, 12.25, 5.5,                // compressor casing
            0.5, 1.0, 5.0, 14.5, 12.75, 11.0,                 // plenum, collector
            4.25, 1.0, 11.0, 11.75, 12.25, 15.0,              // turbine casing
            14.5, 4.25, 4.25, 16.0, 11.75, 11.75,             // exhaust duct and flange
            7.25, 10.75, 5.25, 12.75, 16.0, 10.75,            // cans, fuel line, valve, fuel flange
            0.0, 0.0, 6.5, 1.0, 4.0, 9.5,                     // signal terminal
            5.75, 5.75, 0.0, 10.25, 10.25, 16.0);             // shaft and couplings
        return List.of(n);
    }
}
