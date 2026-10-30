package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.Mc.v;
import static mods.eln.cad.models.OilKit.boltRing;
import static mods.eln.cad.models.OilKit.dial;
import static mods.eln.cad.models.OilKit.obox;
import static mods.eln.cad.models.OilKit.signalPlate;
import static mods.eln.cad.models.ShaftKit.AX;
import static mods.eln.cad.models.ShaftKit.AY;
import static mods.eln.cad.models.ShaftKit.CENTRE_ORIGIN;
import static mods.eln.cad.models.ShaftKit.pipeFlange;
import static mods.eln.cad.models.ShaftKit.COUPLING_DEPTH;
import static mods.eln.cad.models.ShaftKit.halfCoupling;
import static mods.eln.cad.models.ShaftKit.shaft;

/**
 * Shaft steam turbines (HP shaftsteamhp, LP shaftsteamlp): a steam turbine sitting across the shaft line (along z at
 * x = 8, y = 8 px), half couplings on both z faces: a cast-iron casing split at the shaft height (bolted horizontal
 * flange), bearing pedestals on a skid, a governor valve chest on top by the inlet with a pressure gauge facing the
 * front, a paint band, a cast exhaust duct/hood to a large flange at the back centre and the signal floor terminal on
 * the front. HP: compact barrel (r 5), small (alloy) inlet flange at the top centre with the valve chest just below it.
 * LP: near full block, double-flow (a central inlet belt, two flows widening to the ends), large inlet flange on top,
 * an exhaust hood across the back to the large exhaust flange; the valve chest on the front shoulder. Parts: main,
 * rotor (shaft, couplings, a turning gear and a bolted hub outside the casing; origin (8, 8, 8) px), valve (the
 * governor valve's stem and crosshead; the render lifts it by up to 1.5 px, so it stays under y = 14.5 px at rest).
 */
public final class ShaftTurbines {
    private ShaftTurbines() {
    }

    static final double C = 8.0;

    // ---------- rotor pieces ----------

    /** A spur turning gear on the shaft between z0 and z1: a disc with square teeth. */
    static List<Mesh> turningGear(double z0, double z1, double r, int teeth, String mat) {
        List<Mesh> ms = new ArrayList<>();
        ms.add(prism('z', AX, AY, r, z0, z1, mat, teeth));
        for (int k = 0; k < teeth; k++) {
            double a = 2 * Math.PI * k / teeth, ca = Math.cos(a), sa = Math.sin(a);
            ms.add(obox(v(AX + (r + 0.2) * ca, AY + (r + 0.2) * sa, (z0 + z1) / 2), v(ca, sa, 0), v(-sa, ca, 0), v(0, 0, 1), 0.3, 0.3, (z1 - z0) / 2, mat));
        }
        return ms;
    }

    /** A bolted coupling hub: steel disc, brass bolt heads on the face side (+1: towards z+, -1: z-). */
    static List<Mesh> boltedHub(double z0, double z1, double r, int bolts, int face) {
        List<Mesh> ms = new ArrayList<>();
        ms.add(prism('z', AX, AY, r, z0, z1, "steel", 8));
        double b0 = face > 0 ? z1 : z0 - 0.25, b1 = face > 0 ? z1 + 0.25 : z0;
        for (int k = 0; k < bolts; k++) {
            double a = 2 * Math.PI * (k + 0.5) / bolts;
            ms.add(prism('z', AX + (r - 0.5) * Math.cos(a), AY + (r - 0.5) * Math.sin(a), 0.25, b0, b1, "brass", 6, face < 0, face > 0));
        }
        return ms;
    }

    static List<Mesh> rotor(double g0, double g1, double h0, double h1) {
        // (one shaft through the block; it disappears into the bearings and casing. The checker: shaftEnds(0, 16) drew
        // the shaft's ends backwards inside the coupling discs, so no shaft ran from the couplings to the hub and the
        // gear and the couplings weren't joined; one shaft now runs from coupling to coupling, visible for 0.5 px
        // between the left coupling and the hub and 0.4 px between the gear and the right coupling)
        List<Mesh> m = new ArrayList<>(halfCoupling(0));
        m.addAll(halfCoupling(16));
        m.add(shaft(COUPLING_DEPTH, 16 - COUPLING_DEPTH));
        m.addAll(turningGear(g0, g1, 2.0, 10, "darksteel"));
        m.addAll(boltedHub(h0, h1, 1.8, 4, -1));
        return m;
    }

    // ---------- static pieces ----------

    /** A bearing pedestal under the shaft: cast block from bottom up to the shaft, a split bearing cap round it. */
    static List<Mesh> pedestal(double z0, double z1, double bottom, double halfW) {
        return List.of(box(C - halfW, bottom, z0, C + halfW, AY - 0.5, z1, "castiron"),
            box(C - halfW - 0.4, bottom, z0 - 0.2, C + halfW + 0.4, bottom + 0.5, z1 + 0.2, "castiron"),
            prism('z', AX, AY, 1.7, z0 + 0.1, z1 - 0.1, "darksteel", 8),
            box(C - 2.1, AY - 0.3, z0 + 0.15, C + 2.1, AY + 0.3, z1 - 0.15, "darksteel"));  // (cap's split lugs)
    }

    /**
     * The horizontal split-line flange on one flank of a casing of radius r about (xc, 8): a lip at the shaft height
     * along z, brass bolt heads on top. side -1 = the front flank (x-), +1 = the back.
     */
    static List<Mesh> splitFlange(double xc, double r, double z0, double z1, int side, int bolts) {
        double xin = xc + side * (r - 0.6), xout = xc + side * (r + 0.8);
        List<Mesh> ms = new ArrayList<>();
        ms.add(box(Math.min(xin, xout), AY - 0.4, z0, Math.max(xin, xout), AY + 0.4, z1, "castiron"));
        double xb = xc + side * (r + 0.35);
        for (int k = 0; k < bolts; k++) {
            double z = z0 + (z1 - z0) * (k + 0.5) / bolts;
            ms.add(prism('y', xb, z, 0.25, AY + 0.4, AY + 0.7, "brass", 6, false, true));
        }
        return ms;
    }

    /**
     * Governor valve stem and crosshead, rising out of the bonnet (top at top, y); at rest it ends 0.5 px above it, so
     * lifted by 1.5 px it stays in the block for top <= 14.
     */
    static List<Mesh> valve(double vx, double vz, double top) {
        return List.of(prism('y', vx, vz, 0.3, top - 0.3, top + 0.2, "steel", 6, false, false),
            box(vx - 0.35, top + 0.2, vz - 0.9, vx + 0.35, top + 0.45, vz + 0.9, "darksteel"),
            prism('y', vx, vz, 0.3, top + 0.45, top + 0.5, "brass", 6, false, true));
    }

    /** Valve bonnet on the chest (static, top at top) and the two yoke posts guiding the stem's crosshead. */
    static List<Mesh> bonnet(double vx, double vz, double y0, double top) {
        List<Mesh> ms = new ArrayList<>(List.of(prism('y', vx, vz, 0.9, y0, top - 0.25, "steel", 8, false, true),
            prism('y', vx, vz, 0.5, top - 0.25, top, "darksteel", 8, false, true)));
        // (the checker: the post at vz - 1.2 .. vz - 0.95 was given z0 > z1, a box turned inside out; now in order)
        for (int s : new int[]{-1, 1})
            ms.add(box(vx - 0.2, y0, Math.min(vz + s * 0.95, vz + s * 1.2), vx + 0.2, 15.75, Math.max(vz + s * 0.95, vz + s * 1.2), "darksteel", "nsewu"));
        return ms;
    }

    /** Signal floor terminal on the front, and the junction box behind it (on the skid line). */
    static List<Mesh> frontTerminal() {
        List<Mesh> ms = signalPlate();
        ms.add(box(0.85, 0.0, 6.75, 1.75, 3.25, 9.25, "darksteel", "nsewu"));
        return ms;
    }

    /** Pressure gauge facing the front with its dial face at x. */
    static List<Mesh> gauge(double x, double y, double z) {
        return dial(x, y, z, 0.9, "xn", 0.5);
    }

    static NodeModel finish(String name, String paint, List<Mesh> m, List<Mesh> rotor, List<Mesh> valve, double vx, double vtop, double vz,
                            double... hitbox) {
        NodeModel n = new NodeModel(name, paint);
        n.part("main", m);
        n.part("rotor", rotor);
        n.part("valve", valve);
        n.origin("rotor", CENTRE_ORIGIN[0], CENTRE_ORIGIN[1], CENTRE_ORIGIN[2]).origin("valve", vx, vtop, vz);
        n.hitbox(hitbox);
        return n;
    }

    // ---------- HP ----------

    static NodeModel hp() {
        List<Mesh> m = new ArrayList<>();
        double R = 5.0;
        // skid with two cross sills, bearing pedestals, casing saddles
        m.add(box(1.75, 0, 1.5, 14.25, 0.75, 14.5, "darksteel", "nsewu"));
        for (double[] zz : new double[][]{{1.75, 3.25}, {12.75, 14.25}}) m.addAll(pedestal(zz[0], zz[1], 0.75, 2.0));
        for (double z : new double[]{4.5, 10.75}) m.add(box(4.25, 0.75, z, 11.75, 4.0, z + 0.75, "castiron"));
        // casing: barrel with end covers, a paint band at each end of the barrel
        m.add(prism('z', AX, AY, R, 4.5, 11.5, "castiron", 12, false, false));
        m.add(prism('z', AX, AY, 2.25, 3.5, 4.5, "castiron", 12, R, true, false, Double.NaN));
        m.add(prism('z', AX, AY, R, 11.5, 12.5, "castiron", 12, 2.25, false, true, Double.NaN));
        for (double z : new double[]{4.6, 10.65}) m.add(tube('z', AX, AY, R - 0.1, R + 0.3, z, z + 0.75, "paint", 12, Double.NaN, false));
        // split-line flange: front full length, back either side of the exhaust duct
        m.addAll(splitFlange(C, R, 4.5, 11.5, -1, 4));
        // (the checker: the back lips' inner ends at z 5.4 / 10.6 lay 0.05 px off the paint bands' faces; 0.1 px longer)
        m.addAll(splitFlange(C, R, 4.5, 5.5, 1, 1));
        m.addAll(splitFlange(C, R, 10.5, 11.5, 1, 1));
        // exhaust: cast duct from the barrel to the large flange at the back centre, a stand under it to the back floor
        m.add(prism('x', AY, C, 3.25, 11.0, 14.5, "castiron", 12, false, false));
        m.addAll(pipeFlange("x16", AY, C, "large", 0.5, "steel", -1));
        m.add(box(12.25, 0.75, 6.5, 14.5, 5.0, 9.5, "castiron"));
        m.add(box(12.0, 0, 5.75, 15.9, 0.75, 10.25, "darksteel", "nsewu"));
        // governor valve chest on top, the small inlet flange above its middle, the valve on its front end
        m.add(box(3.75, 10.0, 5.75, 10.25, 12.75, 10.25, "castiron"));
        m.add(box(3.5, 12.75, 5.5, 10.5, 13.1, 10.5, "castiron"));          // chest cover
        m.addAll(boltRing('y', C, C, 1.9, 13.1, 13.3, 4, "brass", 0.2, 0));
        m.add(prism('y', C, C, 1.1, 13.1, 15.0, "castiron", 10, false, false));   // inlet neck
        m.addAll(pipeFlange("y16", C, C, "small", 0.5, "steel", -1));
        double vx = 5.0, vz = 8.0, vtop = 14.0;
        m.addAll(bonnet(vx, vz, 13.1, vtop));
        // pressure gauge on the chest's front
        m.add(prism('x', 11.5, 9.0, 0.2, 3.0, 3.75, "brass", 6, false, false));
        m.addAll(gauge(2.5, 11.5, 9.0));
        // signal terminal on the front; a conduit up to the governor
        m.addAll(frontTerminal());
        m.add(prism('y', 1.3, 9.5, 0.3, 3.25, 7.6, "darksteel", 6, false, false));
        // (the checker: this box's underside lay in the plane of the split flange's; it sits 0.1 px lower)
        m.add(box(1.0, 7.5, 9.2, 2.6, 7.5 + 0.6, 9.8, "darksteel"));
        return finish("shaftsteamhp", ShaftKit.PAINT_HP, m, rotor(14.5, 15.1, 1.0, 1.5), valve(vx, vz, vtop), vx, vtop, vz,
            1.75, 0, 1.5, 14.25, 3.0, 14.5,           // skid, pedestals, saddles
            2.25, 3.0, 3.5, 13.75, 13.0, 12.5,        // casing and split flange
            3.5, 10.0, 5.5, 10.5, 16.0, 10.5,         // valve chest, inlet flange, valve
            12.0, 4.25, 4.25, 16.0, 11.75, 11.75,     // exhaust duct and flange
            5.75, 5.75, 0.0, 10.25, 10.25, 16.0,      // shaft, couplings, gear, hub
            0.0, 0.0, 6.5, 1.75, 4.0, 9.5);           // signal terminal
    }

    // ---------- LP ----------

    static NodeModel lp() {
        List<Mesh> m = new ArrayList<>();
        double RB = 6.0, RE = 6.75;          // belt radius, flow end radius
        // skid, pedestals
        m.add(box(1.75, 0, 1.25, 14.25, 0.75, 14.75, "darksteel", "nsewu"));
        for (double[] zz : new double[][]{{1.75, 3.25}, {12.75, 14.25}}) m.addAll(pedestal(zz[0], zz[1], 0.75, 1.75));
        // casing: central inlet belt, two flows widening to the ends, end walls
        m.add(prism('z', AX, AY, RB, 6.0, 10.0, "paint", 14, false, false));      // (the painted inlet belt)
        m.add(prism('z', AX, AY, RE, 4.0, 6.0, "castiron", 14, RB, false, false, Double.NaN));
        m.add(prism('z', AX, AY, RB, 10.0, 12.0, "castiron", 14, RE, false, false, Double.NaN));
        m.add(prism('z', AX, AY, 2.5, 3.4, 4.0, "castiron", 14, RE, true, false, Double.NaN));
        m.add(prism('z', AX, AY, RE, 12.0, 12.6, "castiron", 14, 2.5, false, true, Double.NaN));
        // feet under the flows (on the skid)
        for (double z : new double[]{4.5, 10.75}) m.add(box(3.0, 0.75, z, 11.0, 2.25, z + 0.75, "castiron"));
        // split-line flange on the front flank (the hood covers the back)
        m.addAll(splitFlange(C, RB, 6.0, 10.0, -1, 3));
        m.addAll(splitFlange(C, RE - 0.35, 4.0, 5.6, -1, 1));
        m.addAll(splitFlange(C, RE - 0.35, 10.4, 12.0, -1, 1));
        // exhaust hood across the back (both flows), the large flange at the back centre, a foot to the back floor
        m.add(box(11.5, 0.75, 3.75, 14.75, 12.0, 12.25, "castiron"));
        m.add(box(11.25, 12.0, 3.5, 15.0, 12.4, 12.5, "castiron"));               // hood cover lip
        for (double z : new double[]{5.5, 10.5}) m.add(box(14.75, 1.0, z - 0.3, 15.25, 11.5, z + 0.3, "castiron"));   // ribs
        // (the checker: with a 0.25 px stub the stub's end lay in the plane of the ribs' ends at x 14.75; 0.5 px, the
        // end inside the hood)
        m.addAll(pipeFlange("x16", AY, C, "large", 0.5, "steel", -1));
        m.add(box(11.25, 0, 4.0, 15.9, 0.75, 12.0, "darksteel", "nsewu"));
        // inlet: neck from the belt to the large flange at the top centre
        m.add(prism('y', C, C, 3.0, 13.0, 14.75, "castiron", 12, false, false));   // (the flange stub carries on)
        m.add(tube('y', C, C, 2.95, 3.4, 13.5, 14.1, "castiron", 12, Double.NaN, false));
        m.addAll(pipeFlange("y16", C, C, "large", 0.25, "steel", -1));
        // governor valve chest on the front shoulder, feeding the neck
        m.add(box(2.0, 9.5, 6.0, 6.5, 12.75, 10.0, "castiron"));
        m.add(box(1.75, 12.75, 5.75, 6.0, 13.1, 10.25, "castiron"));               // chest cover
        double vx = 3.25, vz = 8.0, vtop = 14.0;
        m.addAll(bonnet(vx, vz, 13.1, vtop));
        for (double z : new double[]{6.25, 9.75}) m.add(prism('y', 5.4, z, 0.2, 13.1, 13.3, "brass", 6));
        // pressure gauge on the chest's front
        m.add(prism('x', 11.5, 9.0, 0.2, 1.5, 2.0, "brass", 6, false, false));
        m.addAll(gauge(1.0, 11.5, 9.0));
        // signal terminal on the front, conduit up to the governor
        m.addAll(frontTerminal());
        // (the checker: at z 9.6 the conduit missed the junction box, which then hung on the skid's edge; at z 9.5, as
        // the HP's, it rises out of the box)
        m.add(prism('y', 1.3, 9.5, 0.3, 3.25, 7.6, "darksteel", 6, false, false));
        return finish("shaftsteamlp", ShaftKit.PAINT_LP, m, rotor(14.6, 15.2, 1.0, 1.5), valve(vx, vz, vtop), vx, vtop, vz,
            1.75, 0, 1.25, 14.25, 2.0, 14.75,         // skid, feet
            1.0, 1.25, 3.4, 15.0, 15.0, 12.6,         // casing, hood
            3.75, 13.0, 3.75, 12.25, 16.0, 12.25,     // inlet neck and flange
            1.75, 9.5, 5.75, 6.5, 16.0, 10.25,        // valve chest and valve
            14.0, 4.25, 4.25, 16.0, 11.75, 11.75,     // exhaust flange
            5.75, 5.75, 0.0, 10.25, 10.25, 16.0,      // shaft, couplings, gear, hub
            0.0, 0.0, 6.5, 1.75, 4.0, 9.5);           // signal terminal
    }

    public static List<NodeModel> models() {
        return List.of(hp(), lp());
    }
}
