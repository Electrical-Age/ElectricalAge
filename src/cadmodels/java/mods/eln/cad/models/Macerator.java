package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.Mc.v;
import static mods.eln.cad.models.OilKit.obox;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * The macerators (Baughn 2026-11-07, the four main machines restyled): two toothed crushing rolls turning against each
 * other under a hopper, the dust falling to a tray. Drawn by MaceratorDescriptor as it is: main, rot1 turned about -z
 * and rot2 about +z through their .txt origins by the same angle, the input item at the block centre ~12.5 px up (in the
 * hopper's throat, over the rolls) and the output item ~2.4 px up (on the tray). Power on the two z faces (powerLrdu:
 * not front or back): floor terminal plates there. Front XN (x = 0).
 * <ul>
 * <li>maceratora (48V, green): the 1.12 shape modernised: a cabinet of two round-topped cheeks with a hopper between
 * them, the rolls in its throat, a dust tray on the floor seen through the open front, a gear cover on one cheek and
 * the motor on the back.</li>
 * <li>maceratorb (230V, blue): an industrial roll crusher: cast-iron housings on a skid, a big painted hopper, larger
 * rolls, a spoked flywheel on one roll's shaft outside the z+ housing (it turns with the roll), belted to a motor low
 * at the back; a dust bin under a chute.</li>
 * </ul>
 */
public final class Macerator {
    private Macerator() {
    }

    /** Roll axes (px): x of rot1 and rot2, their height, root and tip radii, teeth, z span. */
    record Rolls(double x1, double x2, double y, double rRoot, double rTip, int teeth, double z0, double z1) {
    }

    static final Rolls ROLLS_A = new Rolls(6.55, 9.45, 11.9, 1.0, 1.3, 10, 4.6, 11.4);
    static final Rolls ROLLS_B = new Rolls(5.6, 10.4, 9.5, 1.85, 2.25, 12, 3.6, 11.4);

    /** One roll: a long toothed gear along z on its axle. which 0 = rot1, 1 = rot2 (half a tooth out of phase). */
    static List<Mesh> roll(Rolls r, int which, double axle0, double axle1, String mat) {
        double cx = which == 0 ? r.x1 : r.x2;
        double ph = which == 0 ? 0 : Math.PI / r.teeth;
        List<Mesh> m = new ArrayList<>();
        m.add(TarSandSeparator.gear(cx, r.y, r.z0, r.z1, r.rRoot, r.rTip, r.teeth, ph, mat));
        m.add(prism('z', cx, r.y, 0.45, axle0, axle1, "darksteel", 8));
        return m;
    }

    // ---- 48V

    static List<Mesh> bodyA(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        Rolls r = ROLLS_A;
        m.add(box(2.0, 0, 1.0, 14.0, 1.0, 15.0, "darksteel"));                                         // plinth
        for (double[] z : new double[][]{{2.0, 4.0}, {12.0, 14.0}}) {                                   // the cheeks
            m.add(box(2.5, 1.0, z[0], 13.5, 10.0, z[1], "paint"));
            // (the checker: the round top's ends lay in the box's faces with other texture offsets; 0.1 px in)
            m.add(prism('z', 8.0, 10.0, 5.5, z[0] + 0.1, z[1] - 0.1, "paint", 20));
        }
        // the hopper between the cheeks: two side plates and two sloped walls down to the rolls' throat
        // (the checker: the plates' outer faces lay on the cheeks' round tops; they're sunk into the cheeks)
        for (double[] z : new double[][]{{3.8, 4.5}, {11.5, 12.2}}) m.add(box(3.0, 11.0, z[0], 13.0, 15.0, z[1], "steel"));
        double a = Math.atan2(3.6, 3.0);
        double[] up = v(0, 1, 0), zz = v(0, 0, 1);
        for (int s : new int[]{-1, 1}) {
            double[] ax = v(s * Math.cos(a), -Math.sin(a), 0);                        // along the wall, top to bottom
            double[] ay = v(Math.sin(a), s * Math.cos(a), 0);
            double cx = 8.0 - s * 3.6, cy = 13.6;
            m.add(obox(v(cx, cy, 8.0), ax, normalOf(ax), zz, 2.4, 0.25, 3.6, "steel"));
        }
        m.add(box(2.6, 15.0, 3.8, 13.4, 15.5, 4.5, "darksteel"));                                      // rims on the plates
        m.add(box(2.6, 15.0, 11.5, 13.4, 15.5, 12.2, "darksteel"));
        // the back panel and the dust chute under the rolls, the tray on the plinth (the output item sits on it)
        m.add(box(12.5, 1.0, 4.0, 13.5, 10.0, 12.0, "paint"));
        double ca = Math.atan2(4.0, 6.0);
        m.add(obox(v(8.6, 7.2, 8.0), v(-Math.cos(ca), -Math.sin(ca), 0), v(-Math.sin(ca), Math.cos(ca), 0), zz, 3.6, 0.2, 4.0, "steel"));
        m.add(box(4.0, 1.0, 5.0, 12.0, 1.4, 11.0, "darksteel"));
        m.add(box(4.0, 1.4, 5.0, 4.4, 2.2, 11.0, "darksteel"));
        // the gear cover over the rolls' ends on the z- cheek, and a bearing cap on the z+ one
        m.add(box(4.0, 9.5, 1.3, 12.0, 14.3, 2.1, "castiron"));   // (to the round top's end, 0.1 px in)
        for (double x : new double[]{r.x1, r.x2}) m.add(prism('z', x, r.y, 0.9, 13.9, 14.6, "darksteel", 8));
        // the motor on the back, its terminal box, a band of paint
        m.add(prism('z', 14.6, 6.0, 1.3, 5.0, 11.0, "steel", 12));
        m.add(prism('z', 14.6, 6.0, 1.45, 7.6, 8.4, "paint", 12));
        m.add(box(13.5, 4.5, 5.6, 14.2, 7.5, 6.4, "darksteel"));                                       // motor bracket
        m.add(box(13.5, 4.5, 9.6, 14.2, 7.5, 10.4, "darksteel"));
        // a brass nameplate on the front of the z- cheek
        m.add(box(2.4, 4.0, 2.4, 2.5, 6.0, 3.6, "brass", "w").free("a nameplate, a decal on the cheek"));
        // fiddly bits (Baughn: too clean): rivets round the cheeks' faces, a band across their feet, bolts on the gear
        // cover, the motor's fins, fan cowl and terminal box, a conduit from the z+ junction box up to it, the control
        // box with the status LEDs on the z+ cheek's front
        for (double[] z : new double[][]{{2.0, -1}, {14.0, 1}}) {
            MachineKit.rivetsZ(m, 3.3, 12.7, 5.4, z[0], (int) z[1], 6);   // (above the junction boxes)
            MachineKit.rivetsZ(m, 3.3, 12.7, 9.2, z[0], (int) z[1], 6);
        }
        m.add(box(2.4, 1.0, 1.9, 13.6, 1.5, 14.1, "darksteel", "nsewu"));
        for (double[] p : new double[][]{{4.5, 10.0}, {11.5, 10.0}, {4.5, 13.8}, {11.5, 13.8}})
            m.add(prism('z', p[0], p[1], 0.25, 1.15, 1.3, "brass", 6, true, false));
        for (double z : new double[]{6.2, 9.8}) m.add(prism('z', 14.6, 6.0, 1.45, z - 0.15, z + 0.15, "darksteel", 12));
        m.add(prism('z', 14.6, 6.0, 1.15, 11.0, 11.6, "darksteel", 12));
        m.add(box(14.0, 7.2, 6.8, 15.2, 8.2, 7.6, "darksteel"));
        m.add(box(13.5, 4.5, 13.0, 14.1, 8.0, 13.6, "darksteel"));
        leds.addAll(MachineKit.controlBox(m, 2.5, 7.0, 13.0));
        // power: floor terminals on the two z faces, a junction box behind each against its cheek
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        m.add(box(4.5, 1.0, 0.85, 11.5, 4.5, 2.0, "castiron"));
        m.add(box(4.5, 1.0, 14.0, 11.5, 4.5, 15.15, "castiron"));
        return m;
    }

    /** A unit normal to ax in the xy plane. */
    static double[] normalOf(double[] ax) {
        return v(-ax[1], ax[0], 0);
    }

    // ---- 230V

    /** Flywheel on rot1's shaft, outside the z+ housing: rim, four spokes, hub. */
    static final double FW_R = 3.3, FW_Z0 = 14.2, FW_Z1 = 15.1;
    /** The motor's pulley (px): x, y, radius. */
    static final double PX = 12.9, PY = 3.4, PR = 0.9;

    static List<Mesh> flywheel(Rolls r) {
        List<Mesh> m = new ArrayList<>();
        double cx = r.x1, cy = r.y;
        m.add(tube('z', cx, cy, FW_R - 0.7, FW_R, FW_Z0, FW_Z1, "castiron", 20));
        m.add(prism('z', cx, cy, 0.9, FW_Z0 - 0.2, FW_Z1 + 0.2, "darksteel", 10));
        for (int k = 0; k < 4; k++) {
            double ang = Math.PI / 4 + k * Math.PI / 2;
            double[] d = v(Math.cos(ang), Math.sin(ang), 0);
            double mid = (0.9 + FW_R - 0.7) / 2;
            m.add(obox(v(cx + d[0] * mid, cy + d[1] * mid, (FW_Z0 + FW_Z1) / 2), d, normalOf(d), v(0, 0, 1),
                (FW_R - 0.7 - 0.9) / 2 + 0.3, 0.3, 0.3, "castiron"));
        }
        return m;
    }

    static List<Mesh> bodyB(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        Rolls r = ROLLS_B;
        double[] zz = v(0, 0, 1);
        m.add(box(1.0, 0, 0.85, 15.0, 1.5, 15.15, "darksteel"));                                     // skid
        // cast housings either side of the rolls, a rib down each, a darker cap
        for (double[] z : new double[][]{{0.85, 3.6}, {11.4, 13.9}}) {
            m.add(box(1.5, 1.5, z[0], 14.5, 12.0, z[1], "castiron"));
            m.add(box(1.2, 12.0, z[0] - 0.1, 14.8, 12.6, z[1] + 0.1, "darksteel"));   // (cap)
        }
        for (double x : new double[]{r.x1, r.x2}) m.add(prism('z', x, r.y, 1.3, 0.5, 0.85, "darksteel", 8));   // bearing caps
        // the hopper: painted, flared, over the housings
        for (double[] z : new double[][]{{3.1, 3.6}, {11.4, 11.9}}) m.add(box(1.5, 12.6, z[0], 14.5, 15.8, z[1], "paint"));
        double a = Math.atan2(3.2, 3.6);
        for (int s : new int[]{-1, 1}) {
            double[] ax = v(s * Math.cos(a), -Math.sin(a), 0);
            m.add(obox(v(8.0 - s * 4.6, 14.2, 7.5), ax, normalOf(ax), zz, 2.45, 0.3, 4.4, "paint"));
        }
        // (the checker: the rims' inner faces lay in the side plates'; 0.1 px back into them)
        m.add(box(1.2, 15.5, 3.0, 14.8, 15.9, 3.5, "darksteel"));                                     // rim
        m.add(box(1.2, 15.5, 11.5, 14.8, 15.9, 12.0, "darksteel"));
        // front cross beam under the hopper, back plate
        // (the checker: their tops lay in the housings' caps' and their feet in the motor feet's plane; tops sunk into the
        // caps, no bottom faces on the skid)
        m.add(box(1.5, 10.6, 3.6, 2.6, 12.4, 11.4, "castiron"));
        m.add(box(13.4, 1.5, 3.6, 14.5, 12.4, 11.4, "castiron", "nsewu"));
        // chute under the rolls and the dust bin (the output item sits in it)
        double ca = Math.atan2(3.6, 5.5);
        m.add(obox(v(8.8, 5.4, 7.5), v(-Math.cos(ca), -Math.sin(ca), 0), v(-Math.sin(ca), Math.cos(ca), 0), zz, 3.4, 0.2, 3.9, "steel"));
        m.add(box(4.0, 1.5, 4.3, 12.0, 1.9, 10.9, "steel", "nsewu"));
        m.add(box(4.0, 1.9, 4.3, 4.4, 3.2, 10.9, "steel"));
        m.add(box(4.4, 1.9, 4.3, 12.0, 3.2, 4.7, "steel"));
        m.add(box(4.4, 1.9, 10.5, 12.0, 3.2, 10.9, "steel"));
        // the motor, low at the back, its pulley outside the z+ housing, the belt up to the flywheel
        m.add(prism('z', PX, PY + 0.4, 1.9, 5.0, 13.7, "steel", 14));   // (ends inside the housing)
        for (double z : new double[]{6.5, 8.5, 10.5}) m.add(prism('z', PX, PY + 0.4, 2.15, z - 0.25, z + 0.25, "darksteel", 14));
        m.add(prism('z', PX, PY + 0.4, 2.05, 12.0, 12.8, "paint", 14));
        m.add(box(11.0, 1.5, 7.6, 14.8, 2.0, 9.4, "darksteel", "nsewu"));                              // motor feet
        m.add(prism('z', PX, PY + 0.4, 0.4, 13.9, FW_Z1, "darksteel", 8));                              // motor shaft
        m.add(prism('z', PX, PY + 0.4, PR, FW_Z0 + 0.1, FW_Z1 - 0.1, "steel", 12));                     // pulley
        // (the belt is drawn by mods.eln.anim.Belt from part beltlink, wrapping the flywheel and the pulley, running)
        // a brass nameplate on the front of the z- housing
        m.add(box(1.4, 6.0, 1.3, 1.5, 8.0, 3.1, "brass", "w").free("a nameplate, a decal on the housing"));
        // fiddly bits: ribs and rivet rows on the housings' outer faces, bolt rings and grease nipples on the bearing
        // caps, lifting eyes on the caps, hopper rim bolts, the motor's terminal box and conduit, the control box
        for (double x : new double[]{2.6, 13.4}) m.add(box(x - 0.4, 1.5, 0.55, x + 0.4, 12.0, 0.85, "castiron", "nwue"));
        MachineKit.rivetsZ(m, 2.2, 13.8, 2.2, 0.85, -1, 7);
        MachineKit.rivetsZ(m, 2.2, 13.8, 11.4, 0.85, -1, 7);
        MachineKit.rivetsZ(m, 2.2, 13.8, 11.4, 13.9, 1, 7);
        for (double x : new double[]{r.x1, r.x2}) {
            m.addAll(OilKit.boltRing('z', x, r.y, 1.0, 0.35, 0.5, 6, "brass", 0.15, 0));
            m.add(prism('z', x, r.y, 0.35, 0.3, 0.5, "darksteel", 6));
        }
        m.add(prism('y', r.x2, r.y + 1.0, 0.2, 10.8, 11.6, "brass", 6).free("a grease nipple on the bearing"));
        for (double[] p : new double[][]{{2.2, 2.2}, {13.8, 2.2}, {2.2, 12.6}, {13.8, 12.6}})
            m.add(Mc.tube('x', 13.0, p[1], 0.3, 0.6, p[0] - 0.25, p[0] + 0.25, "darksteel", 8));
        for (double x : new double[]{3.0, 8.0, 13.0}) {
            m.add(prism('y', x, 3.25, 0.18, 15.85, 15.98, "brass", 6, false, true));
            m.add(prism('y', x, 11.75, 0.18, 15.85, 15.98, "brass", 6, false, true));
        }
        m.add(box(PX - 0.8, PY + 2.2, 9.0, PX + 0.8, PY + 3.2, 10.6, "darksteel"));                    // motor terminal box
        leds.addAll(MachineKit.controlBox(m, 1.5, 9.0, 2.2));
        // power: floor terminals on the two z faces, junction boxes against the housings
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        // (Baughn: the belt ran through this junction box; it is low now, under the belt's lower run with 0.6 px to spare;
        // the skid carries the terminal plate with it)
        m.add(box(4.5, 1.5, 13.9, 11.5, 2.6, 15.15, "castiron"));
        return m;
    }

    /** The two straight runs of a belt between two pulleys (centres a, b; radii ra, rb), in the flywheel's plane. */
    static List<Mesh> belt(double ax, double ay, double ra, double bx, double by, double rb) {
        List<Mesh> m = new ArrayList<>();
        double dx = bx - ax, dy = by - ay, d = Math.hypot(dx, dy), base = Math.atan2(dy, dx), off = Math.acos((ra - rb) / d);
        for (int s : new int[]{-1, 1}) {
            double ang = base + s * off;
            double[] n = v(Math.cos(ang), Math.sin(ang), 0);
            double[] p0 = v(ax + ra * n[0], ay + ra * n[1], 0), p1 = v(bx + rb * n[0], by + rb * n[1], 0);
            double len = Math.hypot(p1[0] - p0[0], p1[1] - p0[1]);
            double[] t = v((p1[0] - p0[0]) / len, (p1[1] - p0[1]) / len, 0);
            m.add(obox(v((p0[0] + p1[0]) / 2 + n[0] * 0.15, (p0[1] + p1[1]) / 2 + n[1] * 0.15, (FW_Z0 + FW_Z1) / 2), t, n, v(0, 0, 1),
                len / 2, 0.15, 0.3, "rubber").free("a belt run, tangent to the pulley and the flywheel"));
        }
        return m;
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        NodeModel a = new NodeModel("maceratora", OilKit.PAINT_50V);
        List<List<Mesh>> la = new ArrayList<>();
        a.part("main", bodyA(la));
        a.part("ledpower", la.get(0));
        a.part("ledrun", la.get(1));
        a.part("rot1", roll(ROLLS_A, 0, 2.6, 14.0, "steel"));
        a.part("rot2", roll(ROLLS_A, 1, 2.6, 14.0, "steel"));
        a.origin("rot1", ROLLS_A.x1, ROLLS_A.y, 8.0).origin("rot2", ROLLS_A.x2, ROLLS_A.y, 8.0);
        out.add(a);
        NodeModel b = new NodeModel("maceratorb", OilKit.PAINT_200V);
        List<List<Mesh>> lb = new ArrayList<>();
        b.part("main", bodyB(lb));
        b.part("ledpower", lb.get(0));
        b.part("ledrun", lb.get(1));
        // one belt link in the flywheel's plane; mods.eln.anim.Belt repeats it
        // (built above the block, clear of the model, where it is never drawn: Belt moves it from (lx, ly))
        b.part("beltlink", List.of(box(7.6, 16.85, FW_Z0 + 0.25, 8.4, 17.15, FW_Z1 - 0.25, "rubber")));
        b.part("beltmark", List.of(box(7.5, 16.75, FW_Z0 + 0.15, 8.5, 17.25, FW_Z1 - 0.15, "steel")));   // (a lacing clip: shows it run)
        b.txt.addAll(List.of("o beltlink", "f ax " + Catalog.fmt(ROLLS_B.x1), "f ay " + Catalog.fmt(ROLLS_B.y), "f ar " + Catalog.fmt(FW_R + 0.15),
            "f bx " + Catalog.fmt(PX), "f by " + Catalog.fmt(PY + 0.4), "f br " + Catalog.fmt(PR + 0.15), "f pitch 0.8",
            "f lx 8.0", "f ly 17.0"));
        List<Mesh> r1 = roll(ROLLS_B, 0, 1.2, FW_Z1 + 0.2, "steel");
        r1.addAll(flywheel(ROLLS_B));
        b.part("rot1", r1);
        b.part("rot2", roll(ROLLS_B, 1, 1.2, 13.4, "steel"));
        b.origin("rot1", ROLLS_B.x1, ROLLS_B.y, 8.0).origin("rot2", ROLLS_B.x2, ROLLS_B.y, 8.0);
        out.add(b);
        return out;
    }
}
