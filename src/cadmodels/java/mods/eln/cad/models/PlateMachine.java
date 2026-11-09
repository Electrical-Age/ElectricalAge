package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.OilKit.handwheel;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * The plate machines (Baughn 2026-11-07, the four main machines restyled): rolling mills. Drawn by
 * PlateMachineDescriptor: main, the lower work roll rot1 turned about -z and the upper rot2 about +z through their .txt
 * origins; on the 230V also the backup rolls, rot3 (under rot1, turning as rot2) and rot4 (over rot2, turning as rot1),
 * at the work rolls' surface speed (their .txt: ratio). The items lie on the entry table (input, front) and the exit
 * table (output, back): positions in rot1's .txt (inx.. outz). Power on the two z faces. Front XN (x = 0).
 * <ul>
 * <li>platemachinea (48V, green): a two-high mill: two painted housings with windows, the work rolls in chocks,
 * screw-down handwheels on top, entry and exit tables with small rollers, a gearbox and motor under the exit table.</li>
 * <li>platemachineb (230V, blue): a four-high mill stand: tall cast housings, backup rolls over and under the work
 * rolls, a screw-down gear on the crown, spindles from a pinion box on the z- side, a motor at the back, roller tables.</li>
 * </ul>
 */
public final class PlateMachine {
    private PlateMachine() {
    }

    /** A roll along z: body with a few grooves of darker steel, necks, at (x, y). */
    static List<Mesh> roll(double x, double y, double r, double z0, double z1, double n0, double n1) {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('z', x, y, r, z0, z1, "steel", 16));
        m.add(prism('z', x, y, r * 0.55, n0, z0, "darksteel", 10));
        m.add(prism('z', x, y, r * 0.55, z1, n1, "darksteel", 10));
        m.add(box(x - 0.15, y + r - 0.05, z0 + 0.2, x + 0.15, y + r + 0.05, z1 - 0.2, "darksteel").free("a scribe line on the roll, to see it turn"));
        return m;
    }

    static List<Mesh> table(double x0, double x1, double y, double z0, double z1) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(x0, y - 0.6, z0, x1, y - 0.3, z1, "darksteel"));
        for (double x = x0 + 0.5; x < x1 - 0.2; x += 1.0) m.add(prism('z', x, y - 0.25, 0.3, z0 + 0.2, z1 - 0.2, "steel", 8));
        return m;
    }

    static void items(NodeModel n, double[] in, double[] out) {
        n.txt.addAll(List.of("o rot1", "f inx " + Catalog.fmt(in[0] / 16 - 0.5), "f iny " + Catalog.fmt(in[1] / 16 - 0.5),
            "f inz " + Catalog.fmt(in[2] / 16 - 0.5), "f outx " + Catalog.fmt(out[0] / 16 - 0.5), "f outy " + Catalog.fmt(out[1] / 16 - 0.5),
            "f outz " + Catalog.fmt(out[2] / 16 - 0.5)));
    }

    // ---- 48V: work rolls
    static final double AX = 8.0, AY1 = 7.6, AY2 = 11.4, AR = 1.8;

    static List<Mesh> bodyA(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.0, 0, 1.5, 15.0, 1.0, 14.5, "darksteel"));                                          // bed plate
        for (double[] z : new double[][]{{2.0, 4.0}, {12.0, 14.0}}) {
            // housing: two posts, a sill and a cap round the window the chocks sit in
            m.add(box(4.5, 1.0, z[0], 6.0, 14.5, z[1], "paint"));
            m.add(box(10.0, 1.0, z[0], 11.5, 14.5, z[1], "paint"));
            m.add(box(6.0, 1.0, z[0], 10.0, 5.2, z[1], "paint"));
            m.add(box(4.2, 14.5, z[0] - 0.2, 11.8, 15.3, z[1] + 0.2, "darksteel"));
            m.add(box(6.0, 5.2, z[0] + 0.3, 10.0, AY1 + 1.2, z[1] - 0.3, "castiron"));                // lower chock
            m.add(box(6.0, AY2 - 1.2, z[0] + 0.3, 10.0, 14.5, z[1] - 0.3, "castiron"));                // upper chock
            m.addAll(handwheel(AX, 15.9, (z[0] + z[1]) / 2, 0.9, 'y'));
            m.add(prism('y', AX, (z[0] + z[1]) / 2, 0.3, 15.3, 15.9, "darksteel", 6));
            MachineKit.rivetsZ(m, 4.9, 11.1, 13.6, z[0] < 8 ? z[0] : z[1], z[0] < 8 ? -1 : 1, 2);
        }
        m.addAll(table(1.0, 4.5, AY1 + AR, 4.0, 12.0));                                               // entry
        m.addAll(table(11.5, 15.0, AY1 + AR, 4.0, 12.0));                                             // exit
        for (double[] p : new double[][]{{1.4, 4.4}, {14.6, 4.4}, {1.4, 11.6}, {14.6, 11.6}})
            m.add(box(p[0] - 0.3, 1.0, p[1] - 0.3, p[0] + 0.3, AY1 + AR - 0.6, p[1] + 0.3, "darksteel"));   // table legs
        // gearbox and motor under the exit table, a coupling into the z- housing
        m.add(box(11.8, 1.0, 5.0, 14.6, 5.0, 11.0, "castiron"));
        m.add(prism('x', 3.0, 8.0, 1.2, 11.5, 11.8, "darksteel", 12));                                // coupling
        m.add(box(12.4, 5.0, 5.8, 14.0, 5.4, 10.2, "darksteel"));                                      // motor feet
        m.add(prism('z', 13.2, 6.3, 1.0, 5.5, 10.5, "steel", 12));
        m.add(prism('z', 13.2, 6.3, 1.12, 7.6, 8.4, "paint", 12));
        leds.addAll(MachineKit.controlBox(m, 4.5, 3.4, 3.0));
        m.add(box(4.4, 6.0, 2.4, 4.5, 8.0, 3.6, "brass", "w").free("a nameplate on the housing"));
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        m.add(box(4.5, 1.0, 0.85, 11.5, 4.5, 2.0, "castiron"));
        m.add(box(4.5, 1.0, 14.0, 11.5, 4.5, 15.15, "castiron"));
        return m;
    }

    // ---- 230V: work rolls and backups
    static final double BX = 8.0, BY1 = 7.0, BY2 = 9.6, BR = 1.2, BY3 = 3.9, BY4 = 12.7, BBR = 1.85;

    static List<Mesh> bodyB(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(0.85, 0, 0.85, 15.15, 1.0, 15.15, "darksteel"));
        for (double[] z : new double[][]{{2.6, 4.6}, {11.4, 13.4}}) {
            m.add(box(4.0, 1.0, z[0], 5.8, 15.0, z[1], "castiron"));                                    // posts
            m.add(box(10.2, 1.0, z[0], 12.0, 15.0, z[1], "castiron"));
            m.add(box(5.8, 1.0, z[0], 10.2, 1.8, z[1], "castiron"));                                    // sill
            m.add(box(3.7, 15.0, z[0] - 0.2, 12.3, 15.6, z[1] + 0.2, "darksteel"));                      // cap
            for (double[] y : new double[][]{{1.8, BY3 + 1.6}, {BY3 + 1.6, BY1 + 1.0}, {BY2 - 1.0, BY4 - 1.6}, {BY4 - 1.6, 15.0}})
                m.add(box(5.8, y[0], z[0] + 0.3, 10.2, y[1], z[1] - 0.3, "steel"));                     // chocks
            for (double y : new double[]{BY3 + 1.6, BY1 + 1.0, BY2 - 1.0, BY4 - 1.6})
                m.add(box(5.7, y - 0.1, z[0] + 0.2, 10.3, y + 0.1, z[1] - 0.2, "darksteel"));            // chock seams
            MachineKit.rivetsZ(m, 4.9, 11.1, 14.2, z[0] < 8 ? z[0] : z[1], z[0] < 8 ? -1 : 1, 2);   // (on the posts)
            MachineKit.rivetsZ(m, 4.9, 11.1, 2.4, z[0] < 8 ? z[0] : z[1], z[0] < 8 ? -1 : 1, 2);
        }
        // screw-down gear on the crown: a worm box across the caps, its motor
        m.add(box(5.5, 15.0, 4.8, 10.5, 15.6, 11.2, "castiron"));                                      // crown beam
        m.add(box(5.5, 15.6, 3.0, 10.5, 16.0, 13.0, "castiron"));
        m.add(prism('z', 8.0, 15.8, 0.15, 13.0, 14.4, "darksteel", 6).free("the screw-down shaft"));
        // pinion box on the z- side and spindles into the work rolls' necks
        m.add(box(5.5, 5.5, 0.95, 10.5, 11.0, 2.4, "castiron"));
        m.add(prism('z', BX, BY1, 0.5, 2.4, 2.9, "darksteel", 8));
        m.add(prism('z', BX, BY2, 0.5, 2.4, 2.9, "darksteel", 8));
        // roller tables in and out, the motor at the back under the exit table
        m.addAll(table(0.9, 4.0, BY1 + BR, 4.6, 11.4));
        m.addAll(table(12.0, 15.1, BY1 + BR, 4.6, 11.4));
        m.add(box(12.6, 1.0, 5.2, 14.8, BY1 + BR - 0.6, 5.8, "darksteel"));
        m.add(box(1.2, 1.0, 5.2, 3.4, BY1 + BR - 0.6, 5.8, "darksteel"));
        m.add(prism('z', 13.6, 2.6, 1.7, 6.0, 11.2, "steel", 14));   // (sunk a little into the bed)
        for (double z : new double[]{7.0, 8.3, 9.6}) m.add(prism('z', 13.6, 2.6, 1.9, z - 0.2, z + 0.2, "darksteel", 14));
        m.add(prism('z', 13.6, 2.6, 1.85, 10.4, 11.0, "paint", 14));
        leds.addAll(MachineKit.controlBox(m, 4.0, 11.0, 3.6));
        m.add(box(3.9, 4.0, 2.9, 4.0, 6.0, 4.3, "brass", "w").free("a nameplate on the housing"));
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        m.add(box(4.5, 1.0, 13.4, 11.5, 4.5, 15.15, "castiron"));
        return m;
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        NodeModel a = new NodeModel("platemachinea", OilKit.PAINT_50V);
        List<List<Mesh>> la = new ArrayList<>();
        a.part("main", bodyA(la));
        a.part("rot1", roll(AX, AY1, AR, 4.3, 11.7, 2.4, 13.6));
        a.part("rot2", roll(AX, AY2, AR, 4.3, 11.7, 2.4, 13.6));
        a.part("ledpower", la.get(0));
        a.part("ledrun", la.get(1));
        a.part("toggle", la.get(2));
        a.origin("rot1", AX, AY1, 8.0).origin("rot2", AX, AY2, 8.0).spin("rot1", 'z').spin("rot2", 'z');
        items(a, new double[]{2.6, AY1 + AR, 8.0}, new double[]{13.4, AY1 + AR, 8.0});
        out.add(a);
        NodeModel b = new NodeModel("platemachineb", OilKit.PAINT_200V);
        List<List<Mesh>> lb = new ArrayList<>();
        b.part("main", bodyB(lb));
        b.part("rot1", roll(BX, BY1, BR, 4.9, 11.1, 3.0, 12.8));   // (necks end inside the chocks)
        b.part("rot2", roll(BX, BY2, BR, 4.9, 11.1, 3.0, 12.8));
        b.part("rot3", roll(BX, BY3, BBR, 4.9, 11.1, 3.2, 12.8));
        b.part("rot4", roll(BX, BY4, BBR, 4.9, 11.1, 3.2, 12.8));
        b.part("ledpower", lb.get(0));
        b.part("ledrun", lb.get(1));
        b.part("toggle", lb.get(2));
        b.origin("rot1", BX, BY1, 8.0).origin("rot2", BX, BY2, 8.0).origin("rot3", BX, BY3, 8.0).origin("rot4", BX, BY4, 8.0)
            .spin("rot1", 'z').spin("rot2", 'z').spin("rot3", 'z').spin("rot4", 'z');
        items(b, new double[]{2.4, BY1 + BR, 8.0}, new double[]{13.6, BY1 + BR, 8.0});
        b.txt.addAll(List.of("o rot3", "f ratio " + Catalog.fmt(BR / BBR), "o rot4", "f ratio " + Catalog.fmt(BR / BBR)));
        out.add(b);
        return out;
    }
}
