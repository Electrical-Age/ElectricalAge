package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.OilKit.dial;
import static mods.eln.cad.models.OilKit.heatPad;
import static mods.eln.cad.models.OilKit.turned;
import static mods.eln.cad.models.ShaftKit.AX;
import static mods.eln.cad.models.ShaftKit.AY;
import static mods.eln.cad.models.ShaftKit.FLANGE_R;
import static mods.eln.cad.models.ShaftKit.halfCoupling;
import static mods.eln.cad.models.ShaftKit.pipeFlange;
import static mods.eln.cad.models.ShaftKit.shaft;

/**
 * Plant Steam Condenser: a big shell-and-tube surface condenser lying front-back on two saddles over a skid, the
 * hotwell slung under it between the saddles, a condensate pump on its front head and a circulating pump on its left
 * flank (both driven by a Shaft Motor set against that side: parts drive_front / drive_left, a half coupling and pump
 * shaft stub at the shaft line), a steam-jet air ejector on top. Ports (PlantCondenserElement): exhaust steam in at
 * large flanges on the top centre, back centre and right centre (z = 16 face at (8, 8)); condensate out: front LOW
 * small; makeup in: left HIGH small; cooling water in: left LOW small, out: right LOW small. The right LOW flange
 * overlaps the right large one: the large disc is recessed 0.1 px so the small one lies over it. Thermal pads at the
 * floor of the back (full) and both flanks (low, under the cooling-water flanges). Parts: main, drive_left,
 * drive_front (origin (8, 8, 8): on both drive axes). Front XN (x = 0), right z = 16, left z = 0.
 */
public final class PlantCondenser {
    private PlantCondenser() {
    }

    /** Shell radius (axis along x at (y 8, z 8)) and the cylindrical shell's span; heads beyond. */
    static final double C = 8.0, R = 4.75;
    static final double[] SX = {3.75, 14.0};

    static List<Mesh> shifted(List<Mesh> meshes, double dx, double dy, double dz) {
        List<Mesh> out = new ArrayList<>();
        for (Mesh m : meshes)
            out.add(m.mapped(p -> {
                p[0] += dx;
                p[1] += dy;
                p[2] += dz;
            }, false));
        return out;
    }

    /** A thermal floor pad for the x = 0 face, low enough to sit under a LOW port's flange (y 2.5): heat_pad's look without the bolts. */
    static List<Mesh> lowPad() {
        double top = 2.25, depth = 0.75;
        // (the checker: turned onto the flanks the backing stood 0.25 px clear of the skid and hung on nothing; a sill at
        // the floor joins it to the skid, in the 0.25 px gap behind it)
        return new ArrayList<>(List.of(box(0.1, 0, 4.0, depth, top, 12.0, "hotcopper"), box(depth, 0, 3.5, depth + 0.5, top + 0.25, 12.5, "castiron"),
            box(depth + 0.25, 0, 3.75, depth + 0.85, 1.0, 12.25, "darksteel", "nsewu")));
    }

    /**
     * pipeFlange with square bolt heads (5 faces: 8 triangles each rather than a hex prism's 16) on the flange's inner
     * side, to keep the many ports within the triangle budget.
     */
    static List<Mesh> flange(String face, double c1, double c2, String size, double stub, int bolts) {
        List<Mesh> ms = pipeFlange(face, c1, c2, size, stub, "steel", 0);
        double fr = FLANGE_R.get(size);
        char axis = face.charAt(0);
        boolean at0 = face.endsWith("0") && !face.endsWith("16");
        double a0 = at0 ? 1.0 : 14.75, a1 = at0 ? 1.25 : 15.0, h = size.equals("large") ? 0.25 : 0.18;
        String pair = axis == 'x' ? "we" : axis == 'y' ? "du" : "ns";
        char skip = pair.charAt(at0 ? 0 : 1);                  // the face against the disc
        String faces = "nsewud".replace(String.valueOf(skip), "");
        for (int k = 0; k < bolts; k++) {
            double ang = 2 * Math.PI * (k + 0.5) / bolts, p = c1 + (fr - 0.45) * Math.cos(ang), q = c2 + (fr - 0.45) * Math.sin(ang);
            ms.add(switch (axis) {
                case 'x' -> box(a0, p - h, q - h, a1, p + h, q + h, "brass", faces);
                case 'y' -> box(p - h, a0, q - h, p + h, a1, q + h, "brass", faces);
                default -> box(p - h, q - h, a0, p + h, q + h, a1, "brass", faces);
            });
        }
        return ms;
    }

    static List<Mesh> flange(String face, double c1, double c2, String size, double stub) {
        return flange(face, c1, c2, size, stub, 4);
    }

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        // skid, saddles, hotwell
        m.add(box(1.0, 0, 1.5, 15.0, 1.0, 14.5, "darksteel", "nsewu"));
        for (double x : new double[]{4.25, 11.0}) {
            m.add(box(x, 1.0, C - R + 0.75, x + 1.5, AY - R + 1.25, C + R - 0.75, "castiron"));
            m.add(box(x - 0.25, 1.0, C - R + 0.5, x + 1.75, 1.5, C + R - 0.5, "castiron"));
        }
        m.add(box(6.0, 1.0, 5.0, 10.75, AY - R + 0.75, 11.0, "steel"));                 // hotwell
        // the shell: cylinder, dished heads, tube-sheet flanges at the joints, a paint band
        m.add(prism('x', AY, C, R, SX[0], SX[1], "paint", 14, false, false));
        m.add(prism('x', AY, C, R - 1.75, SX[0] - 0.75, SX[0], "paint", 14, R, true, false, Double.NaN));
        m.add(prism('x', AY, C, R, SX[1], SX[1] + 0.75, "paint", 14, R - 1.5, false, true, Double.NaN));
        for (double x : new double[]{SX[0], SX[1] - 0.5}) m.add(tube('x', AY, C, R - 0.1, R + 0.4, x, x + 0.5, "darksteel", 12, Double.NaN, false));
        // exhaust inlets (large): top riser, back nozzle, right nozzle (its flange recessed 0.1 px under the small one)
        m.addAll(flange("y16", C, C, "large", 15.0 - (AY + R - 0.75)));
        m.addAll(flange("x16", AY, C, "large", 15.0 - SX[1]));
        m.addAll(shifted(flange("z16", C, AY, "large", 15.0 - (C + R - 0.75)), 0, 0, -0.1));
        // cooling water: in on the left LOW, out on the right LOW, pipes into the shell's lower flanks
        m.addAll(flange("z0", C, 4.0, "small", 5.25));
        m.addAll(flange("z16", C, 4.0, "small", 5.25));
        // makeup in: left HIGH, into the shell's upper flank
        m.addAll(flange("z0", C, 12.0, "small", 5.0));
        // circulating pump on the left flank (its shaft at the shaft line, z = 0 face): volute, bearing cap
        m.add(prism('z', C, AY, 2.1, 1.25, 3.0, "paint", 10));
        m.add(prism('z', C, AY, 1.0, 0.9, 1.25, "darksteel", 8, true, false));
        m.add(prism('z', C, AY, 1.2, 3.0, 4.0, "castiron", 8, false, false));    // discharge into the shell
        // condensate pump on the front head (shaft at (y 8, z 8), x = 0 face): volute, bearing cap
        m.add(prism('x', AY, C, 2.1, 1.25, 3.0, "paint", 10));
        m.add(prism('x', AY, C, 1.0, 0.9, 1.25, "darksteel", 8, true, false));
        // its suction from the hotwell, its discharge down to the condensate flange (front LOW)
        m.addAll(flange("x0", 4.0, C, "small", 1.25));
        m.add(prism('y', 2.0, C, 0.75, 3.25, AY - 1.5, "steel", 8, true, false));
        m.add(prism('x', 2.25, 10.75, 0.6, 2.4, 6.0, "steel", 8, true, false));
        m.add(prism('y', 2.4 + 0.6, 10.75, 0.6, 1.65, AY - 1.0, "steel", 8, true, false));
        // steam-jet air ejector on top, front-left: a nozzle and diffuser off the shell
        double ez = 4.25, ey = AY + 3.6;
        m.add(prism('x', ey, ez, 0.45, 3.0, 5.0, "steel", 8));
        m.add(prism('x', ey, ez, 0.45, 5.0, 7.25, "steel", 8, 0.8, true, true, Double.NaN));
        m.add(prism('y', 7.0, ez, 0.4, AY + 2.0, ey, "steel", 6, false, false));
        // vacuum gauge on the front head
        m.add(prism('x', AY + 3.0, C + 2.75, 0.25, SX[0] - 1.6, SX[0] - 0.4, "brass", 6, false, false));
        m.addAll(dial(SX[0] - 2.1, AY + 3.0, C + 2.75, 0.85, "xn", 0.5));
        // thermal pads: back full height (4 px under the back flange), flanks low (under the cooling-water flanges)
        m.addAll(turned(heatPad(0.75, 3.0), 2));
        m.addAll(turned(lowPad(), 1));
        m.addAll(turned(lowPad(), 3));
        return m;
    }

    static List<Mesh> driveLeft() {
        List<Mesh> m = new ArrayList<>(halfCoupling(0));
        m.add(shaft(0.5, 1.0, "darksteel", 0.8));
        return m;
    }

    static List<Mesh> driveFront() {
        List<Mesh> m = new ArrayList<>(halfCoupling(0));
        m.add(shaft(0.5, 1.0, "darksteel", 0.8));
        return turned(m, 1);    // the z = 0 face's coupling, turned onto x = 0
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("plantcondenser", ShaftKit.PAINT_CONDENSER);
        n.part("main", main());
        n.part("drive_left", driveLeft());
        n.part("drive_front", driveFront());
        n.origin("drive_left", AX, AY, 8.0).origin("drive_front", AX, AY, 8.0);
        n.hitbox(1.0, 0.0, 1.5, 15.0, 1.0, 14.5,          // skid
            2.75, 1.0, 3.0, 15.0, 13.0, 13.0,                 // shell, saddles, hotwell
            4.25, 12.5, 4.25, 11.75, 16.0, 11.75,             // top inlet
            14.0, 4.25, 4.25, 16.0, 11.75, 11.75,             // back inlet
            4.25, 2.5, 12.5, 11.75, 11.75, 16.0,              // right inlet, cooling water out
            5.75, 2.5, 0.0, 10.25, 13.5, 3.0,                 // left: pump and its coupling, makeup, cooling water in
            0.0, 2.5, 5.75, 3.0, 10.25, 10.25);               // front: pump and its coupling, condensate out
        return List.of(n);
    }
}
