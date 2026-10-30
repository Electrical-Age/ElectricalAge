package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.Mc.volute;
import static mods.eln.cad.models.OilKit.signalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * Small steam turbines (50V / 200V Steam Turbine, was tools/models/steamturbine.py): a scroll-cased steam turbine
 * coupled to a generator on a skid. Drawn by SteamGeneratorRender through Obj3D. The shaft runs front-back along x:
 * x = 0 (XN) FRONT: generator end, terminal plate at floor level (electric out, LRDU Down); x = 16 (XP) BACK: turbine
 * end, second terminal plate, fed by a bus duct along the z- skid rail. Top: steam inlet stub and flange on the scroll,
 * a riser to a flange centred on the top face (a boiler set on top feeds it). z+ side: exhaust vent stack, open at the
 * top: the renderer puffs steam from it (part "vent"); with a condenser against z+, part "exhaust" instead: a duct to a
 * flange on that face. The shaft and coupling are part "rotor", turned by the renderer about the .txt's origin.
 */
public final class SteamTurbine {
    private SteamTurbine() {
    }


    /** A tier: cy shaft height; rg, gx generator radius and x span; r0, r1, tx scroll radii and x span; generator
     *  cooling fins; bolts on the top flange. */
    record Tier(String name, String paint, double cy, double rg, double gx0, double gx1, double r0, double r1, double tx0, double tx1,
                double[] fins, int bolts) {
    }

    static final Tier[] TIERS = {
        new Tier("50v", OilKit.PAINT_50V, 6.75, 3.75, 2.25, 7.25, 2.75, 4.75, 9.25, 13.25, new double[]{3.0, 6.5}, 4),
        new Tier("200v", OilKit.PAINT_200V, 7.25, 4.5, 1.75, 7.75, 3.25, 5.5, 9.0, 13.75, new double[]{2.5, 3.75, 5.6, 7.0}, 6)};
    // (the checker, 200v: the third fin at x 5.75..6.25 lay in the planes of a generator foot's front (5.75), the exhaust
    // elbow's end cap (5.75) and the nameplate's end (6.25); it moved 0.15 px to the front, 5.6..6.1)
    static final double CZ = 8.0;
    /** An iron pipe's radius (PipePart.BORE[0] / 2 voxels). */
    static final double PIPE_R = 2.0;
    /** The exhaust duct into a condenser (condenser: the same, at the tier's shaft height). */
    static final double EXHAUST_R = 2.25;
    /** Scroll tongue angle (0 = up, 90 deg = +z); the radius grows from there round to it. */
    static final double START = Math.toRadians(40);

    /** Scroll radius at angle ang (radians, 0 = up, towards +z). */
    static double scrollR(Tier t, double ang) {
        double frac = (((ang - START) % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI) / (2 * Math.PI);
        return t.r0 + (t.r1 - t.r0) * frac;
    }

    /** Steel plate flush with the face (recessed 0.1 px), down to the floor, copper studs, tier stripe on top. */
    static void terminalPlate(List<Mesh> m, double x0, double x1, double faceX, double studOut) {
        m.add(box(x0, 0, 4.5, x1, 4.0, 11.5, "steel"));
        m.add(box(x0, 4.0, 4.5, x1, 4.5, 11.5, "paint"));
        for (double z : new double[]{5.75, 8.0, 10.25})
            m.add(box(Math.min(faceX, studOut), 1.75, z - 0.5, Math.max(faceX, studOut), 2.75, z + 0.5, "copper"));
        // gasket edges (the checker: the back plate's z- gasket's face against the plate lay in the plane of the bus duct's
        // side; the gaskets' faces against the plate, hidden, are dropped)
        m.add(box(x0, 0.5, 4.25, x1, 3.5, 4.5, "rubber", "newud"));
        m.add(box(x0, 0.5, 11.5, x1, 3.5, 11.75, "rubber", "sewud"));
    }

    static NodeModel turbine(Tier t) {
        double cy = t.cy, rg = t.rg, gx0 = t.gx0, gx1 = t.gx1, tx0 = t.tx0, tx1 = t.tx1;
        double tcx = (tx0 + tx1) / 2;
        List<Mesh> m = new ArrayList<>();
        // --- skid: two rails along x, cross members
        m.add(box(1.0, 0, 2.0, 15.0, 1.5, 4.0, "darksteel"));
        m.add(box(1.0, 0, 12.0, 15.0, 1.5, 14.0, "darksteel"));
        double[] feet = {gx0 + 0.5, gx1 - 2.0};
        for (double x : new double[]{feet[0], feet[1], tcx - 0.75}) m.add(box(x, 0.25, 4.0, x + 1.5, 1.25, 12.0, "darksteel"));   // cross members
        // --- generator: ribbed stator, end bells, tier band, cradle feet, terminal box
        m.add(prism('x', cy, CZ, rg, gx0, gx1, "steel", 12, false, false));
        m.add(prism('x', cy, CZ, rg - 1.25, gx0 - 0.75, gx0, "steel", 12, rg, true, false, Double.NaN));
        m.add(prism('x', cy, CZ, rg, gx1, gx1 + 0.6, "steel", 12, rg - 1.0, false, true, Double.NaN));
        for (double x : t.fins) m.add(tube('x', cy, CZ, rg - 0.1, rg + 0.6, x, x + 0.5, "darksteel", 12, Double.NaN, false));
        double bx = (gx0 + gx1) / 2;
        m.add(tube('x', cy, CZ, rg - 0.1, rg + 0.4, bx - 0.75, bx + 0.75, "paint", 12, Double.NaN, false));
        double foot = cy - rg + 0.6;
        for (double x : feet) {
            m.add(box(x, 1.25, 4.75, x + 1.5, foot, 6.5, "steel"));
            m.add(box(x, 1.25, 9.5, x + 1.5, foot, 11.25, "steel"));
        }
        m.add(prism('x', cy, CZ, 0.8, gx0 - 1.25, gx0 - 0.75, "darksteel", 8));                 // front bearing cap
        // terminal box on the z- flank, conduit down to the bus duct on the skid
        double tz0 = CZ - rg - 1.25;
        m.add(box(bx - 1.6, cy - 1.75, tz0, bx + 1.6, cy + 1.25, CZ - rg + 0.75, "steel"));
        m.add(box(bx - 1.85, cy + 1.25, tz0 - 0.25, bx + 1.85, cy + 1.6, CZ - rg + 0.75, "darksteel"));   // lid
        // (the checker: the conduit's sides lay in the planes of the tier band's rims, bx -+ 0.75; it is 0.1 px narrower
        // each side, 1.3 px wide)
        m.add(box(bx - 0.65, 1.5, tz0 + 0.25, bx + 0.65, cy - 1.75, tz0 + 1.75, "darksteel"));      // conduit
        m.add(box(bx - 1.0, 1.5, 2.5, 15.25, 3.0, 4.5, "darksteel"));                               // bus duct (on the rail) to the back plate
        // nameplate on the z+ flank
        m.add(box(bx - 1.5, cy - 0.75, CZ + rg + 0.0, bx + 1.5, cy + 0.75, CZ + rg + 0.1, "brass"));
        // --- shaft and coupling: a part of their own, which the renderer turns with the shaft's speed (Baughn); the
        // coupling's bolts show it turning
        List<Mesh> rotor = new ArrayList<>();
        rotor.add(prism('x', cy, CZ, 0.6, gx1 + 0.6, tx0 - 0.5, "darksteel", 8, false, false));
        double cx = (gx1 + 0.6 + tx0 - 0.5) / 2;
        rotor.add(prism('x', cy, CZ, 1.5, cx - 0.4, cx + 0.4, "copper", 8));
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 2 * k + Math.PI / 4;
            rotor.add(prism('x', cy + 1.45 * Math.cos(a), CZ + 1.45 * Math.sin(a), 0.3, cx - 0.55, cx + 0.55, "brass", 6));
        }
        // --- turbine: scroll casing, end covers, pedestal
        m.add(volute('x', cy, CZ, t.r0, t.r1, tx0, tx1, "castiron", 20, START));
        m.add(prism('x', cy, CZ, t.r0 - 1.25, tx0 - 0.5, tx0, "castiron", 10, t.r0 - 0.5, true, false, Double.NaN));
        m.add(prism('x', cy, CZ, t.r0 - 0.25, tx1, tx1 + 0.75, "castiron", 10, t.r0 - 1.25, false, true, Double.NaN));
        m.add(prism('x', cy, CZ, 1.1, tx1 + 0.75, 15.0, "darksteel", 8));                          // rear bearing
        double bottom = cy - scrollR(t, Math.PI) + 0.5;
        m.add(box(tx0 + 0.5, 1.25, 5.75, tx1 - 0.5, bottom, 10.25, "darksteel"));                  // pedestal
        // governor housing on the back of the casing
        m.add(box(tx1 - 0.25, cy + 1.25, CZ - 1.5, tx1 + 1.5, cy + 3.25, CZ + 1.5, "steel"));
        m.add(box(tx1 - 0.25, cy + 3.25, CZ - 1.6, tx1 + 1.6, cy + 3.5, CZ + 1.6, "paint"));
        // --- steam inlet: an iron pipe's bore (4 px, Baughn: the standard for this tier) the whole way, as a laid pipe
        // looks: up out of the scroll, across to the middle, up to a pipe's flange on the top face (a boiler set on top,
        // or a pipe laid to it, couples there)
        double top = cy + scrollR(t, 0.0);
        double yh = Math.min(15.75 - PIPE_R, top + PIPE_R);   // (the run across: clear of the scroll where there's room, under the top flange's face)
        m.add(prism('y', tcx, CZ, PIPE_R, top - 1.0, yh + PIPE_R, "castiron", 12, false, true));
        double c0 = top - 0.5, c1 = Math.min(top + 0.25, yh - PIPE_R + 0.25);
        Mesh collar = prism('y', tcx, CZ, PIPE_R + 0.25, c0, c1, "paint", 12, false, true);   // tier collar
        // (the checker, 200v: there c1 == c0 (12.0) and the collar's sides had no height (a Python slip); only its top
        // ring, all that was drawn, is kept)
        m.add(c1 - c0 < 1e-9 ? capOnly(collar) : collar);
        m.add(prism('x', yh, CZ, PIPE_R, 8.0, tcx, "castiron", 12, false, false));
        m.add(prism('y', 8.0, CZ, PIPE_R, yh - PIPE_R, 15.0, "castiron", 12, true, false));
        m.add(prism('y', 8.0, CZ, PIPE_R + 0.75, 15.0, 16.0, "steel", 12));                       // top flange, a laid pipe's
        double br = PIPE_R + 0.4;
        for (int k = 0; k < t.bolts; k++) {
            double a = 2 * Math.PI * (k + 0.5) / t.bolts, x = 8.0 + br * Math.cos(a), z = CZ + br * Math.sin(a);
            // (the checker, 200v: the two bolts at x 10.08 sat inside the inlet riser, a side 0.03 px under the riser's
            // wall; buried (they showed at most 0.08 px), they are left out)
            if (Math.hypot(x - tcx, z - CZ) + 0.25 < PIPE_R + 0.1) continue;
            m.add(prism('y', x, z, 0.25, 14.75, 15.0, "brass", 6));
        }
        // pressure gauge on the bend over the generator, facing the front
        double gx = 8.0 - PIPE_R;
        m.add(prism('x', yh, CZ, 0.25, gx - 0.5, gx + 0.2, "brass", 6, false, false));
        m.add(prism('x', yh, CZ, 0.9, gx - 1.0, gx - 0.5, "brass", 10));
        // (the checker: the 0.05 px glass's face lay 0.05 px off the case's front and z-fought; it is 0.1 px thick, gx - 1.1)
        m.add(prism('x', yh, CZ, 0.7, gx - 1.1, gx - 1.0, "dialglass", 10, true, false));
        // --- exhaust vent stack on the z+ side (vents to air): a part of its own ("vent"), drawn unless a condenser
        // stands against that side; then the "exhaust" part, a duct out to a flange on the z+ face, joins the condenser's
        double ez = CZ + scrollR(t, Math.PI / 2) + 1.0;
        List<Mesh> vent = new ArrayList<>();
        vent.add(prism('z', tcx, cy - 0.25, 0.9, CZ + 1.0, ez + 0.9, "castiron", 8, false, true));
        // straight up to an open outlet: the steam leaves there (Baughn). (A boiler set on top would hide it, but nobody
        // builds like that, Baughn: pipes are cheap.)
        // (the checker: the outlet "touched nothing": it judges a joint by the part of the first piece inside the second's
        // hull, and a tube with an inner wall has none; the outlet is listed before the riser it stands on, no change)
        vent.add(tube('y', tcx, ez, 0.8, 1.25, 12.75, 13.75, "darksteel", 8));                    // open outlet
        vent.add(prism('y', tcx, ez, 0.9, cy - 0.25, 12.75, "castiron", 8, true, false));
        // (dark inside the bore; the checker: its 0.5 px sides lay 0.05 px inside the riser's and z-fought, only its face is kept)
        vent.add(capOnly(prism('y', tcx, ez, 0.85, 12.0, 12.5, "rubber", 8, false, true)));
        // (out of the scroll along z, an elbow, along x to the face's middle, out to the flange: the condenser's duct
        // meets it at the middle of its face whichever way it faces)
        List<Mesh> ex = new ArrayList<>();
        double zr = 13.0;
        ex.add(prism('z', tcx, cy, EXHAUST_R, CZ + 1.0, zr + EXHAUST_R, "castiron", 12, false, true));
        ex.add(prism('x', cy, zr, EXHAUST_R, 8.0 - EXHAUST_R, tcx + EXHAUST_R, "castiron", 12, true, true));
        ex.add(prism('z', 8.0, cy, EXHAUST_R, zr, 15.0, "castiron", 12, false, false));
        ex.add(prism('z', 8.0, cy, EXHAUST_R + 0.75, 15.0, 16.0, "steel", 12, true, false));      // flange on the face
        for (int k = 0; k < t.bolts; k++) {
            double a = 2 * Math.PI * (k + 0.5) / t.bolts, b = EXHAUST_R + 0.4;
            ex.add(prism('z', 8.0 + b * Math.cos(a), cy + b * Math.sin(a), 0.25, 14.75, 15.0, "brass", 6));
        }
        // --- terminal plates (electric out, front and back, floor level)
        terminalPlate(m, 0.1, 1.0, 0.1, 0.0);
        // (the checker: the front plate stood clear of everything; a front cross member of the skid between the rails, as
        // the others (y 0.25..1.25), and the plate 0.15 px deeper (x 0.1..1.0, was ..0.85) join it. Visible: the
        // member, low behind the plate, and the plate's thicker edge)
        m.add(box(1.0, 0.25, 4.0, 2.0, 1.25, 12.0, "darksteel"));
        terminalPlate(m, 15.15, 15.9, 15.9, 16.0);
        // --- signal plate on the z- flank (left, seen from the front): external control of the throttle (Baughn 2026-10-26)
        m.addAll(turned(signalPlate(), 3));
        // (the checker: the signal plate stood 1.15 px clear of the z- rail; a bracket behind it, 2 px wide, 1 px high,
        // z 0.85..2.0, joins it to the rail. Visible from the side, low)
        m.add(box(7.0, 0.25, 0.85, 9.0, 1.25, 2.0, "darksteel"));

        NodeModel n = new NodeModel("steamturbine" + t.name, t.paint);
        n.part("main", m);
        n.part("rotor", rotor);
        n.part("vent", vent);
        n.part("exhaust", ex);
        // the rotor turns about the shaft (Obj3D part origin, y and z only); the vent outlet's centre (it faces up)
        n.txt.addAll(List.of("o rotor", "f originY " + (cy / 16 - 0.5), "f originZ " + (CZ / 16 - 0.5), "o main",
            "f ventX " + (tcx / 16 - 0.5), "f ventY " + (13.75 / 16 - 0.5), "f ventZ " + (ez / 16 - 0.5)));
        return n;
    }

    /** Only the last polygon (a prism's far cap): a disc without its side walls. */
    static Mesh capOnly(Mesh m) {
        Mesh d = new Mesh((String) m.material);
        d.polys.add(m.polys.get(m.polys.size() - 1));
        return d;
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        for (Tier t : TIERS) out.add(turbine(t));
        return out;
    }
}
