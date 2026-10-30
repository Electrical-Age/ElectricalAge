package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.Mc.volute;
import static mods.eln.cad.models.OilKit.boltRing;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * 50V Compact Steam Turbine: the early-game bootstrap generator (300 W, 50 V). Everything on one skid, smaller and
 * simpler than the 50V Steam Turbine: a little drum boiler fired through a heat plate, a scroll turbine coupled to a
 * green-banded generator, and a tiny finned air-cooled condenser with a vent stack. EA transparent node, drawn through
 * Obj3D; parts main and rotor (the shaft and coupling, origin on the shaft axis in the .txt, so the render can spin
 * it). Shaft along x:
 * <ul>
 * <li>x = 0 (XN) FRONT: generator end, terminal plate at the floor (electric out), the drum's sight glass;</li>
 * <li>x = 16 (XP) BACK: turbine end, second terminal plate;</li>
 * <li>z = 0 LEFT (seen from the front): heat plate flush with the face (a heat furnace touches it), the drum behind it;</li>
 * <li>z = 16 RIGHT: tiny finned air-cooled condenser, vent stack with rain cap on top (ventX/Y/Z in the .txt, part main);</li>
 * <li>top: water inlet, a laid pipe's flange (r 2.75 px, 4 bolts) centred on the top face, piped down into the drum.</li>
 * </ul>
 */
public final class CompactTurbine {
    private CompactTurbine() {
    }

    static final String NAME = "compactturbine50v";
    /** Shaft height and z (off centre: the drum takes the left, the condenser the right). */
    static final double CY = 5.5, CZ = 8.5;
    /** Generator radius, stator x span. */
    static final double RG = 2.4;
    static final double[] GX = {2.5, 6.5};
    /** Scroll radii, x span. */
    static final double R0 = 2.0, R1 = 3.25;
    static final double[] TX = {9.25, 12.25};
    /** Scroll tongue (0 = up, 90 = +z), as the steam turbine's. */
    static final double START = Math.toRadians(40);
    /** Drum axis height, z, radius, shell x span. */
    static final double DY = 7.0, DZ = 3.0, DR = 2.25;
    static final double[] DX = {2.75, 13.25};
    /** A laid pipe's bore (the top stub). */
    static final double PIPE_R = 2.0;
    /** The vent outlet (it faces up). */
    static final double VX = 10.75, VZ = 14.0, VTOP = 11.0;

    static double scrollR(double ang) {
        double frac = (((ang - START) % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI) / (2 * Math.PI);
        return R0 + (R1 - R0) * frac;
    }

    static List<Mesh> mainPart() {
        List<Mesh> m = new ArrayList<>();
        // --- skid: cross members (the firebox and the condenser base are the rails)
        for (double x : new double[]{3.0, 10.0}) m.add(box(x, 0.2, 5.0, x + 1.25, 0.9, 12.0, "darksteel", "newu"));

        // --- LEFT flank (z = 0): the heat plate, where a heat furnace touches: lapped copper recessed behind vertical
        // copper ribs that reach the face, a heat-red rim, a cast-iron firebox backing; the drum sits on it
        m.add(box(2.5, 0.0, 0.6, 13.5, 6.5, 1.0, "castiron", "n"));
        for (int k = 0; k < 7; k++) {
            double x = 2.95 + 1.6 * k;
            m.add(box(x, 0.0, 0.0, x + 0.8, 6.25, 0.6, "hotcopper", "newu"));
        }
        m.add(box(2.0, 6.5, 0.1, 14.0, 7.0, 1.0, "red", "nsewu"));                       // rim: top
        m.add(box(2.0, 0.0, 0.1, 2.5, 6.5, 1.0, "red", "nwe"));
        m.add(box(13.5, 0.0, 0.1, 14.0, 6.5, 1.0, "red", "nwe"));
        // (the checker: the firebox's ends z-fought the drum heads' caps in the planes x = 2 and 14; it is 0.1 px shorter
        // at each end)
        m.add(box(2.1, 0.0, 1.0, 13.9, 6.0, 5.0, "castiron", "ewu"));                     // firebox
        m.add(box(1.75, 6.0, 1.0, 14.25, 6.4, 5.25, "darksteel", "sewu"));                // its top plate
        // drum: shell, dished heads, a seam band
        m.add(prism('x', DY, DZ, DR, DX[0], DX[1], "rivet", 10, false, false));
        m.add(prism('x', DY, DZ, DR - 1.0, DX[0] - 0.75, DX[0], "rivet", 10, DR, true, false, Double.NaN));
        m.add(prism('x', DY, DZ, DR, DX[1], DX[1] + 0.75, "rivet", 10, DR - 1.0, false, true, Double.NaN));
        m.add(prism('x', DY, DZ, DR + 0.15, 7.8, 8.2, "darksteel", 10, false, false));
        // sight glass on the front head, facing the front: brass cocks, the glass (water below, steam above), guards
        double hx = DX[0] - 0.75, gz = DZ;
        for (double y : new double[]{5.0, 8.75}) m.add(box(0.6, y, gz - 0.45, hx + 0.3, y + 0.5, gz + 0.45, "brass"));
        m.add(prism('y', 0.95, gz, 0.35, 5.5, 8.75, "water", 6, false, false));
        // (the checker: the guards met the cocks only along an edge; they stand 0.15 px further in, over the cocks)
        m.add(box(0.5, 5.5, gz - 0.45, 0.65, 8.75, gz - 0.3, "darksteel", "nsew"));
        m.add(box(0.5, 5.5, gz + 0.3, 0.65, 8.75, gz + 0.45, "darksteel", "nsew"));

        // --- MIDDLE: generator (green tier band, fins), scroll turbine
        double gx0 = GX[0], gx1 = GX[1];
        m.add(prism('x', CY, CZ, RG, gx0, gx1, "steel", 10, false, false));
        m.add(prism('x', CY, CZ, RG - 1.0, gx0 - 0.6, gx0, "steel", 10, RG, true, false, Double.NaN));
        m.add(prism('x', CY, CZ, RG, gx1, gx1 + 0.5, "steel", 10, RG - 0.9, false, true, Double.NaN));
        double fin = gx1 - 0.9;
        m.add(tube('x', CY, CZ, RG - 0.1, RG + 0.5, fin, fin + 0.5, "darksteel", 10, Double.NaN, false));
        double bx = (gx0 + gx1) / 2;
        m.add(tube('x', CY, CZ, RG - 0.1, RG + 0.35, bx - 0.6, bx + 0.6, "paint", 10, Double.NaN, false));
        m.add(prism('x', CY, CZ, 0.6, gx0 - 1.0, gx0 - 0.6, "darksteel", 8));                 // front bearing cap
        double foot = CY - RG + 0.5;
        // (the checker: the generator stood on its feet 0.1 px above the skid's front cross member; the feet reach down
        // to it, y 0.9)
        for (double x : new double[]{gx0 + 0.25, gx1 - 1.25}) m.add(box(x, 0.9, CZ - 2.0, x + 1.0, foot, CZ + 2.0, "steel"));
        // conduit from the generator down to the front terminal (on the skid's floor)
        // (the checker: it ended 0.15 px short of the terminal plate, which floated; it reaches the plate, x 0.85)
        m.add(box(0.85, 1.0, 7.25, gx0 + 0.25, 1.75, 8.75, "darksteel"));
        // scroll casing, covers, rear bearing, pedestal
        double tx0 = TX[0], tx1 = TX[1], tcx = (tx0 + tx1) / 2;
        m.add(volute('x', CY, CZ, R0, R1, tx0, tx1, "castiron", 12, START));
        m.add(prism('x', CY, CZ, R0 - 0.9, tx0 - 0.5, tx0, "castiron", 10, R0 - 0.3, true, false, Double.NaN));
        m.add(prism('x', CY, CZ, R0 - 0.2, tx1, tx1 + 0.6, "castiron", 10, R0 - 1.0, false, true, Double.NaN));
        m.add(prism('x', CY, CZ, 0.8, tx1 + 0.6, 14.0, "darksteel", 8));                       // rear bearing
        m.add(box(tx0 + 0.5, 1.0, CZ - 1.75, tx1 - 0.5, CY - scrollR(Math.PI) + 0.4, CZ + 1.75, "darksteel"));
        // steam from the drum to the scroll: a small pipe from the drum's top to the scroll's top, with a tier collar
        double st = CY + scrollR(0.0);
        m.add(prism('y', tcx, CZ, 0.6, st - 0.5, 10.0, "castiron", 8, false, false));
        m.add(prism('y', tcx, CZ, 0.85, st - 0.3, st + 0.4, "paint", 8));
        m.add(prism('z', tcx, 10.0, 0.6, DZ, CZ + 0.6, "castiron", 8, false, true));
        m.add(prism('y', tcx, DZ, 0.6, DY + DR - 0.3, 10.6, "castiron", 8, false, true));
        // exhaust: out of the scroll's z+ side into the condenser
        m.add(prism('z', tcx, CY - 0.5, 0.9, CZ + 1.0, 12.5, "castiron", 8, false, false));

        // --- RIGHT flank (z = 16): the tiny air-cooled condenser: header box, many thin fins, cap plate, vent stack
        double cx0 = 2.5, cx1 = 13.5;
        m.add(box(cx0, 1.0, 12.25, cx1, 7.5, 13.25, "steel", "ns"));                              // header
        int nf = 11;
        for (int k = 0; k < nf; k++) {
            double x = cx0 + 0.3 + (cx1 - cx0 - 0.6 - 0.25) * k / (nf - 1);
            m.add(box(x, 1.25, 13.25, x + 0.25, 7.25, 15.75, "copper", "sewu"));
        }
        m.add(box(cx0 - 0.25, 7.5, 12.0, cx1 + 0.25, 8.0, 15.9, "darksteel", "nsewu"));          // cap plate
        m.add(box(cx0 - 0.25, 1.0, 12.0, cx0, 7.5, 15.9, "darksteel", "nsew"));
        m.add(box(cx1, 1.0, 12.0, cx1 + 0.25, 7.5, 15.9, "darksteel", "nsew"));
        m.add(box(cx0 - 0.25, 0.0, 12.0, cx1 + 0.25, 1.0, 15.9, "darksteel", "nsewu"));          // base
        // condensate return to the drum along the skid (back end)
        m.add(prism('z', 14.25, 1.6, 0.4, DZ + 2.0, 12.0, "darksteel", 6, false, false));
        m.add(prism('y', 14.25, DZ + 2.0, 0.4, 1.2, DY - 1.0, "darksteel", 6, false, false));
        // vent stack with a rain cap on three posts
        m.add(prism('y', VX, VZ, 1.1, 8.0, 8.5, "darksteel", 8));                                 // base flange
        m.add(prism('y', VX, VZ, 0.75, 8.5, VTOP, "castiron", 8, false, false));
        // dark inside the bore (the checker: a solid plug's sides lay 0.05 px inside the stack's walls and z-fought them;
        // it is its top face alone)
        Mesh plug = prism('y', VX, VZ, 0.7, VTOP - 1.0, VTOP - 0.8, "rubber", 8, false, true);
        Mesh disc = new Mesh("rubber");
        disc.polys.add(plug.polys.get(plug.polys.size() - 1));   // (its top cap, facing up)
        m.add(disc);
        for (int k = 0; k < 3; k++) {
            double a = 2 * Math.PI * k / 3 + Math.PI / 6;
            m.add(box(VX + 0.8 * Math.cos(a) - 0.1, VTOP, VZ + 0.8 * Math.sin(a) - 0.1,
                VX + 0.8 * Math.cos(a) + 0.1, VTOP + 0.8, VZ + 0.8 * Math.sin(a) + 0.1, "darksteel", "nsew"));
        }
        m.add(prism('y', VX, VZ, 1.5, VTOP + 0.8, VTOP + 1.0, "darksteel", 8, true, false));
        m.add(prism('y', VX, VZ, 1.5, VTOP + 1.0, VTOP + 1.7, "darksteel", 8, 0.2, true, true, Double.NaN));   // rain cap

        // --- TOP: water inlet, a laid pipe's flange centred on the top face, down to the drum
        m.add(prism('y', 8.0, 8.0, PIPE_R + 0.75, 15.0, 16.0, "steel", 12));
        m.addAll(boltRing('y', 8.0, 8.0, PIPE_R + 0.4, 14.75, 15.0, 4, "brass", 0.25, 0));
        m.add(prism('y', 8.0, 8.0, PIPE_R, 14.0, 15.0, "steel", 12, false, false));
        m.add(prism('y', 8.0, 8.0, 0.7, 13.0, 14.0, "steel", 12, PIPE_R, true, false, Double.NaN));   // reducer
        m.add(prism('y', 8.0, 8.0, 0.5, 11.75, 13.0, "steel", 8, false, false));
        m.add(prism('z', 8.0, 11.75, 0.5, DZ - 0.5, 8.5, "steel", 8, true, true));
        m.add(prism('y', 8.0, DZ, 0.5, DY + DR - 0.3, 11.75, "steel", 8, false, false));
        m.add(box(7.4, 12.15, 5.0, 8.6, 13.35, 6.4, "castiron"));                                    // check valve
        m.add(prism('y', 8.0, 5.7, 0.2, 13.35, 13.8, "darksteel", 6, false, false));
        m.add(prism('y', 8.0, 5.7, 0.6, 13.8, 14.0, "red", 6));                                     // its handle

        // --- electric out: terminal plates flush with the front and back faces
        m.addAll(terminalPlate("paint"));
        m.addAll(turned(terminalPlate("paint"), 2));
        // (the checker: the back plate stood alone, 1.4 px clear of the scroll's pedestal; a conduit like the front
        // one runs from the pedestal to it along the floor)
        m.add(box(tx1 - 0.5, 1.0, 7.25, 15.15, 1.75, 8.75, "darksteel", "nsu"));
        return m;
    }

    static List<Mesh> rotorPart() {
        List<Mesh> r = new ArrayList<>();
        double x0 = GX[1] + 0.5, x1 = TX[0] - 0.5;
        r.add(prism('x', CY, CZ, 0.45, x0, x1, "darksteel", 8, false, false));
        double cx = (x0 + x1) / 2;
        r.add(prism('x', CY, CZ, 1.2, cx - 0.35, cx + 0.35, "copper", 8));
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 2 * k + Math.PI / 4;
            r.add(prism('x', CY + 0.85 * Math.cos(a), CZ + 0.85 * Math.sin(a), 0.22, cx - 0.5, cx + 0.5, "brass", 4));
        }
        // a spoked flywheel on the generator's front end (the coupling alone, 1 px across, didn't show it turning:
        // AdventurAgent): a rim, a hub and four spokes, painted so the spin reads
        double fx0 = 0.5, fx1 = 1.2;
        // (the checker: the flywheel and the coupling were two pieces in this part, joined only through the generator;
        // the flywheel's shaft runs on through the generator, hidden in it, to the coupling's)
        r.add(prism('x', CY, CZ, 0.3, fx1, x0, "darksteel", 8, false, false));
        r.add(tube('x', CY, CZ, 1.7, 2.3, fx0, fx1, "castiron", 12));
        r.add(prism('x', CY, CZ, 0.55, fx0, fx1, "brass", 8));
        r.add(box(fx0 + 0.1, CY - 1.75, CZ - 0.25, fx1 - 0.1, CY + 1.75, CZ + 0.25, "paint"));
        // (the checker: the two spokes crossed at the hub, their faces z-fighting there; the castiron one is two halves,
        // meeting the painted one inside the hub)
        r.add(box(fx0 + 0.1, CY - 0.25, CZ - 1.75, fx1 - 0.1, CY + 0.25, CZ - 0.25, "castiron"));
        r.add(box(fx0 + 0.1, CY - 0.25, CZ + 0.25, fx1 - 0.1, CY + 0.25, CZ + 1.75, "castiron"));
        return r;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel(NAME, OilKit.PAINT_50V);
        n.part("main", mainPart());
        n.part("rotor", rotorPart());
        // the rotor turns about the shaft (no originX: the render spins it about x); the vent outlet (it faces up)
        n.txt.addAll(List.of("o rotor", "f originY " + (CY / 16 - 0.5), "f originZ " + (CZ / 16 - 0.5), "o main",
            String.format(java.util.Locale.ROOT, "f ventX %.5f", VX / 16 - 0.5), String.format(java.util.Locale.ROOT, "f ventY %.5f", VTOP / 16 - 0.5),
            String.format(java.util.Locale.ROOT, "f ventZ %.5f", VZ / 16 - 0.5)));
        return List.of(n);
    }
}
