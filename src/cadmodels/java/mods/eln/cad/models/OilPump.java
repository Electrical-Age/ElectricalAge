package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.OilKit.boltRing;
import static mods.eln.cad.models.OilKit.dial;
import static mods.eln.cad.models.OilKit.handwheel;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * Oil pump (50V / 200V): an electric well-head pump (progressing-cavity style drive head) on top of the well casing.
 * Drawn by OilPumpRender -> OilPumpDescriptor.draw() (part main), after front.glRotateXnRef(). Connections
 * (OilPumpElement): electrical power on all four horizontal faces at the floor. The block below is the well casing, so
 * the wellhead stands at the bottom centre. Crude/water leaves through the fluid capability (pipes on any side): the
 * discharge line is decoration. Front XN (x = 0):
 * <ul>
 * <li>floor: skid with a floor terminal plate and junction box on every face; casing-head flange with bolts at the centre;</li>
 * <li>stack: tubing head, flanged flow tee (pressure gauge to the front, discharge line with gate valve and red
 * handwheel to z+), brass stuffing box, polished rod, drive-head frame and cast-iron gearbox;</li>
 * <li>drive: vertical finned motor at the back (x+) on a pedestal, tier paint band, terminal box with conduit, belt
 * guard in tier paint over gearbox and motor.</li>
 * </ul>
 */
public final class OilPump {
    private OilPump() {
    }

    /** fr: casing flange radius; mx/mr: motor axis x and radius; fins: motor fin heights; gb: gearbox x span; gtop: gearbox top; hw: handwheel radius. */
    record Tier(String name, String paint, double fr, double mx, double mr, double[] fins, double[] gb, double gtop, double hw) {
    }

    static final List<Tier> TIERS = List.of(
        new Tier("50v", OilKit.PAINT_50V, 3.0, 12.75, 2.25, new double[]{6.0, 8.0, 10.0}, new double[]{5.75, 10.25}, 12.5, 1.1),
        new Tier("200v", OilKit.PAINT_200V, 3.4, 12.85, 2.6, new double[]{5.75, 7.25, 8.75, 10.25}, new double[]{5.6, 10.15}, 12.75, 1.3));
    static final double C = 8.0;

    static List<Mesh> pump(Tier t) {
        List<Mesh> m = new ArrayList<>();
        // skid, four floor terminals with junction boxes
        m.add(box(1.0, 0, 1.0, 15.0, 0.75, 15.0, "darksteel", "nsewu"));
        List<Mesh> jb = List.of(box(0.85, 0.75, 5.5, 2.0, 3.0, 10.5, "steel", "nsewu"), box(0.85, 3.0, 5.35, 2.15, 3.3, 10.65, "darksteel", "nsewu"));
        for (int k = 0; k < 4; k++) {
            m.addAll(turned(terminalPlate(), k));
            m.addAll(turned(jb, k));
        }
        // casing head flange (meets the well casing below), tubing head, flanged flow tee
        double fr = t.fr;
        m.add(prism('y', C, C, fr, 0, 1.5, "castiron", 12, false, true));
        m.addAll(boltRing('y', C, C, fr - 0.55, 1.5, 1.85, 6, "brass", 0.3, 0));
        m.add(prism('y', C, C, 2.0, 1.5, 3.5, "castiron", 10, false, false));
        m.add(prism('y', C, C, 2.6, 3.5, 4.1, "steel", 12));
        m.addAll(boltRing('y', C, C, 2.25, 4.1, 4.35, 6, "brass", 0.22, 0));
        m.add(prism('y', C, C, 1.4, 4.1, 7.0, "steel", 8, false, false));
        m.add(prism('y', C, C, 1.8, 7.0, 7.4, "steel", 10));
        // discharge line to z+: pipe, gate valve with bonnet and handwheel, flange at the face
        double dy = 6.0;
        m.add(prism('z', C, dy, 0.75, C + 1.2, 15.3, "darksteel", 6, false, false));
        m.add(box(C - 1.0, dy - 1.1, 10.75, C + 1.0, dy + 1.1, 12.5, "castiron"));
        m.add(prism('y', C, 11.6, 0.5, dy + 1.1, dy + 2.4, "castiron", 6, false, true));
        m.add(prism('y', C, 11.6, 0.2, dy + 2.4, dy + 3.0, "darksteel", 6, false, false));
        m.addAll(handwheel(C, dy + 3.1, 11.6, t.hw, 'y'));
        m.add(prism('z', C, dy, 1.2, 15.3, 15.85, "steel", 8));
        // pressure gauge on the tee, facing the front
        m.add(prism('x', 5.6, C, 0.2, C - 2.6, C - 1.3, "brass", 6, false, false));
        m.addAll(dial(C - 3.0, 5.6, C, 0.8, "xn", 0.4));
        // stuffing box, polished rod, drive-head frame, gearbox
        m.add(prism('y', C, C, 1.0, 7.4, 8.6, "brass", 8, false, true));
        m.addAll(boltRing('y', C, C, 0.8, 8.6, 8.8, 2, "brass", 0.2, Math.PI / 2));
        m.add(prism('y', C, C, 0.35, 8.6, 10.0, "steel", 6, false, false));
        for (double z : new double[]{C - 1.9, C + 1.4}) m.add(box(C - 0.5, 7.4, z, C + 0.5, 10.0, z + 0.5, "darksteel"));
        double g0 = t.gb[0], g1 = t.gb[1];
        // (the checker: the gearbox's bottom z-fought its flange plate's; dropped, the plate covers it)
        m.add(box(g0, 10.0, C - 2.25, g1, t.gtop, C + 2.25, "castiron", "nsewu"));
        m.add(box(g0 - 0.15, 10.0, C - 2.4, g1 + 0.15, 10.4, C + 2.4, "darksteel"));
        m.add(prism('x', 11.25, C, 0.6, g0 - 0.3, g0, "darksteel", 6, true, false));   // inspection plug
        // motor: pedestal, bottom bell, finned frame, tier band, adapter into the belt guard
        double mx = t.mx, mr = t.mr;
        m.add(box(mx - 1.75, 0.75, C - 2.0, mx + 1.75, 3.5, C + 2.0, "darksteel"));
        m.add(prism('y', mx, C, mr - 0.5, 3.5, 4.25, "darksteel", 10, false, false));
        m.add(prism('y', mx, C, mr, 4.25, 12.0, "steel", 10, false, false));
        for (double y : t.fins) m.add(prism('y', mx, C, mr + 0.35, y - 0.2, y + 0.2, "darksteel", 10));
        m.add(prism('y', mx, C, mr + 0.3, 10.9, 11.6, "paint", 10));
        m.add(prism('y', mx, C, 1.5, 12.0, t.gtop + 0.1, "darksteel", 8, false, false));
        // terminal box on the motor's z- side, conduit to the skid
        // (the checker: the box's top, under its lid, z-fought the 200V's third motor fin, 0.05 px below it; dropped, the
        // lid covers it)
        m.add(box(mx - 1.0, 6.5, C - mr - 0.9, mx + 1.0, 9.0, C - mr + 0.3, "steel", "nsewd"));
        m.add(box(mx - 1.15, 9.0, C - mr - 1.05, mx + 1.15, 9.3, C - mr + 0.3, "darksteel"));
        m.add(box(mx - 0.35, 0.75, C - mr - 0.75, mx + 0.35, 6.5, C - mr - 0.15, "darksteel"));
        // belt guard (tier paint): rounded ends over the gearbox and the motor, lid
        double gy0 = t.gtop + 0.1, gy1 = t.gtop + 1.9;
        m.add(prism('y', C, C, 2.0, gy0, gy1, "paint", 10));
        m.add(prism('y', mx, C, 1.7, gy0, gy1, "paint", 10));
        m.add(box(C, gy0, C - 1.7, mx, gy1, C + 1.7, "paint", "nsud"));
        m.add(box(C - 0.5, gy1, C - 0.6, mx + 0.5, gy1 + 0.3, C + 0.6, "darksteel"));   // lid seam / lifting bar
        return m;
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        for (Tier t : TIERS) {
            NodeModel n = new NodeModel("oilpump" + t.name, t.paint);
            n.part("main", pump(t));
            out.add(n);
        }
        return out;
    }
}
