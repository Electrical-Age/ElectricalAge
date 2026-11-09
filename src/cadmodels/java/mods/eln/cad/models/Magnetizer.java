package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.OilKit.obox;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * The magnetizers (Baughn 2026-11-07, the four main machines restyled). Drawn by MagnetizerDescriptor: main, part rot
 * turned about +z through its .txt origin, the status LEDs, and the input item where rot's .txt says (inx iny inz,
 * block units from the centre). Power on every face but the front: floor terminal plates on the two z faces and the
 * back. Front XN (x = 0).
 * <ul>
 * <li>magnetizera (48V, green): the 1.12 shape modernised: a pedestal on a plinth, a U-bracket holding the coil drum
 * (rot: a winding of copper bands between flanges, spinning about the item at its centre), a capacitor can.</li>
 * <li>magnetizerb (230V, blue): a C-yoke electromagnet: cast-iron yoke, two pole pieces wound with heavy copper coils,
 * the item in the gap between the pole faces; capacitor cans in the front corners, a cooling fan (rot) in a cowl on
 * the yoke's z+ side.</li>
 * </ul>
 */
public final class Magnetizer {
    private Magnetizer() {
    }

    static void items(NodeModel n, double[] in, double size) {
        n.txt.addAll(List.of("o rot", "f inx " + Catalog.fmt(in[0] / 16 - 0.5), "f iny " + Catalog.fmt(in[1] / 16 - 0.5),
            "f inz " + Catalog.fmt(in[2] / 16 - 0.5), "f insize " + Catalog.fmt(size)));
    }

    static void power(List<Mesh> m, double y0) {
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 2));
        m.addAll(turned(terminalPlate(), 3));
        m.add(box(4.5, y0, 0.85, 11.5, 4.5, 2.0, "castiron"));
        m.add(box(4.5, y0, 14.0, 11.5, 4.5, 15.15, "castiron"));
        m.add(box(14.0, y0, 4.5, 15.15, 4.5, 11.5, "castiron"));
    }

    // ---- 48V: the drum's axis
    static final double AY = 11.5;

    static List<Mesh> bodyA(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.0, 0, 1.0, 15.0, 1.5, 15.0, "darksteel"));                                         // plinth
        m.add(box(5.5, 1.5, 5.5, 10.5, 7.0, 10.5, "paint"));                                           // pedestal
        m.add(box(5.2, 1.5, 5.2, 10.8, 2.0, 10.8, "darksteel", "nsewu"));                             // its foot band
        m.add(box(4.5, 7.0, 2.5, 11.5, 8.0, 13.5, "paint"));                                           // the bracket's bridge
        for (double[] z : new double[][]{{2.5, 3.5}, {12.5, 13.5}}) {                                    // its arms, bearings
            m.add(box(6.0, 8.0, z[0], 10.0, AY, z[1], "paint"));
            m.add(prism('z', 8.0, AY, 2.0, z[0], z[1], "paint", 16));
            m.add(prism('z', 8.0, AY, 0.9, z[0] - 0.3, z[1] + 0.3, "darksteel", 8));
        }
        m.add(prism('y', 12.5, 3.0, 1.1, 1.5, 5.6, "paint_blue", 12));                                // capacitor can
        m.add(prism('y', 12.5, 3.0, 1.2, 5.6, 6.0, "darksteel", 12));
        m.add(prism('y', 12.5, 3.0, 0.3, 6.0, 6.5, "copper", 6));
        // fiddly bits: rivets on the bridge, bolts on the plinth, a conduit up the pedestal, the control box
        MachineKit.rivetsX(m, 3.5, 12.5, 7.5, 4.5, -1, 6);
        for (double[] p : new double[][]{{2.0, 2.0}, {14.0, 2.0}, {2.0, 14.0}, {14.0, 14.0}})
            m.add(prism('y', p[0], p[1], 0.3, 1.5, 1.7, "brass", 6, false, true));
        m.add(box(10.5, 1.5, 7.6, 11.1, 7.0, 8.4, "darksteel"));
        leds.addAll(MachineKit.controlBox(m, 5.5, 4.5, 8.0));
        m.add(box(5.4, 2.4, 6.0, 5.5, 3.2, 7.4, "brass", "w").free("a nameplate, a decal on the pedestal"));
        power(m, 1.5);
        return m;
    }

    static List<Mesh> drumA() {
        List<Mesh> m = new ArrayList<>();
        m.add(tube('z', 8.0, AY, 2.0, 2.8, 4.0, 12.0, "darksteel", 16));                                // the former
        // (the checker: bands and flanges with their bores at or near the former's skins z-fought it and didn't join; their
        // bores are sunk into its wall)
        // (the checker: bands and flanges sunk into the former's wall didn't count as joined; the bands sit on its skin,
        // the flanges butt against its ends, and a spider of two bars carries each stub axle)
        // (coaxial joints, a band on the former's skin and a flange butted to its end, are the checker's Mesh.coax rule)
        for (double z = 4.6; z < 11.6; z += 1.4)
            m.add(tube('z', 8.0, AY, 2.8, 3.1, z, z + 0.9, "copper", 16));
        m.add(tube('z', 8.0, AY, 2.0, 3.5, 3.4, 4.0, "steel", 16));
        m.add(tube('z', 8.0, AY, 2.0, 3.5, 12.0, 12.6, "steel", 16));
        for (double[] z : new double[][]{{3.4, 4.0}, {12.0, 12.6}}) {
            m.add(box(6.0, AY - 0.25, z[0] + 0.1, 10.0, AY + 0.25, z[1] - 0.1, "darksteel").free("a spider bar across the flange's bore"));
            m.add(box(8.0 - 0.25, AY - 2.0, z[0] + 0.1, 8.0 + 0.25, AY + 2.0, z[1] - 0.1, "darksteel").free("a spider bar across the flange's bore"));
        }
        m.add(prism('z', 8.0, AY, 0.5, 2.2, 3.5, "darksteel", 8).free("the stub axle, on the spider"));
        m.add(prism('z', 8.0, AY, 0.5, 12.5, 13.8, "darksteel", 8).free("the stub axle, on the spider"));
        return m;
    }

    // ---- 230V
    static final double PX = 6.0, PZ = 8.0, GAP0 = 6.4, GAP1 = 10.0;
    static final double FX = 12.0, FY = 7.5, FZ = 13.4;

    static List<Mesh> bodyB(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(0.85, 0, 0.85, 15.15, 1.5, 15.15, "darksteel"));                                    // skid
        // the C-yoke: back leg, bottom and top arms; bolts on its flanks
        m.add(box(11.0, 1.5, 4.5, 14.0, 14.5, 11.5, "castiron", "nsewu"));
        m.add(box(2.5, 1.5, 4.5, 11.0, 3.5, 11.5, "castiron", "nsewu"));
        m.add(box(2.5, 12.5, 4.5, 11.0, 14.5, 11.5, "castiron"));
        for (double z : new double[]{4.5, 11.5})
            for (double[] p : new double[][]{{4.0, 2.5}, {8.0, 2.5}, {4.0, 13.5}, {8.0, 13.5}, {12.5, 5.0}, {12.5, 8.0}, {12.5, 11.0}})
                m.add(prism('z', p[0], p[1], 0.25, z < 8 ? z - 0.12 : z, z < 8 ? z : z + 0.12, "brass", 6, z < 8, z > 8));
        // pole pieces and their coils
        m.add(prism('y', PX, PZ, 1.6, 3.5, GAP0 - 0.1, "steel", 16));   // (ends inside the pole face)
        m.add(prism('y', PX, PZ, 1.6, GAP1 + 0.1, 12.5, "steel", 16));
        m.add(prism('y', PX, PZ, 1.9, GAP0 - 0.3, GAP0, "darksteel", 16));                            // pole faces
        m.add(prism('y', PX, PZ, 1.9, GAP1, GAP1 + 0.3, "darksteel", 16));
        m.add(tube('y', PX, PZ, 1.6, 3.0, 3.6, 5.8, "copper", 16));
        m.add(tube('y', PX, PZ, 1.6, 3.0, 10.6, 12.4, "copper", 16));
        for (double y : new double[]{4.7, 11.5}) m.add(tube('y', PX, PZ, 2.95, 3.15, y - 0.15, y + 0.15, "darksteel", 16));   // binding
        // capacitor cans in the front corners, a bus bar across their tops
        for (double z : new double[]{2.2, 13.8}) {
            m.add(prism('y', 2.4, z, 1.1, 1.5, 7.1, "paint", 12));
            m.add(prism('y', 2.4, z, 1.2, 7.1, 7.5, "darksteel", 12));
            m.add(prism('y', 2.4, z, 0.3, 7.5, 8.0, "copper", 6));
        }
        // the fan cowl on the yoke's z+ side (the fan is rot), its guard bars
        m.add(tube('z', FX, FY, 2.0, 2.4, 11.5, 14.4, "paint", 16));
        for (double y : new double[]{FY - 1.2, FY, FY + 1.2}) m.add(box(FX - 2.0, y - 0.1, 14.4, FX + 2.0, y + 0.1, 14.6, "darksteel"));
        m.add(prism('z', FX, FY, 0.5, 11.5, 12.6, "darksteel", 8));                                   // fan motor hub
        // the cable from the coils to the capacitors, the control box, a nameplate
        m.add(box(3.0, 3.5, 7.7, 3.6, 7.5, 8.3, "darksteel"));
        leds.addAll(MachineKit.controlBox(m, 2.5, 2.4, 8.0));
        m.add(box(10.9, 6.0, 6.5, 11.0, 7.4, 9.5, "brass", "w").free("a nameplate on the yoke's back leg"));
        power(m, 1.5);
        return m;
    }

    static List<Mesh> fanB() {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('z', FX, FY, 0.6, 12.6, 13.6, "steel", 8));
        for (int k = 0; k < 5; k++) {
            double a = 2 * Math.PI * k / 5;
            double[] d = Mc.v(Math.cos(a), Math.sin(a), 0);
            double p = Math.toRadians(30);
            double[] az = Mc.v(-d[1] * Math.sin(p), d[0] * Math.sin(p), Math.cos(p)), ay = Mc.v(-d[1] * Math.cos(p), d[0] * Math.cos(p), -Math.sin(p));
            m.add(obox(Mc.v(FX + d[0] * 1.2, FY + d[1] * 1.2, 13.1), d, ay, az, 0.75, 0.45, 0.08, "steel"));
        }
        return m;
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        NodeModel a = new NodeModel("magnetizera", OilKit.PAINT_50V);
        List<List<Mesh>> la = new ArrayList<>();
        a.part("main", bodyA(la));
        a.part("rot", drumA());
        a.part("ledpower", la.get(0));
        a.part("ledrun", la.get(1));
        a.part("toggle", la.get(2));
        a.origin("rot", 8.0, AY, 8.0).spin("rot", 'z');
        // (the item hovers in front of the coil, in its field: in the bore, behind the spider and axle, it couldn't be seen;
        // over the drum it was too small, or poked out of the block, AdventurAgent)
        items(a, new double[]{2.8, AY - 1.0, 8.0}, 1);
        out.add(a);
        NodeModel b = new NodeModel("magnetizerb", OilKit.PAINT_200V);
        List<List<Mesh>> lb = new ArrayList<>();
        b.part("main", bodyB(lb));
        b.part("rot", fanB());
        b.part("ledpower", lb.get(0));
        b.part("ledrun", lb.get(1));
        b.part("toggle", lb.get(2));
        b.origin("rot", FX, FY, FZ).spin("rot", 'z');
        items(b, new double[]{PX, (GAP0 + GAP1) / 2 - 1.0, PZ}, 1);
        out.add(b);
        return out;
    }
}
